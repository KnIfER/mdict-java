/******************************************************************************
 *                                                                            *
 * Copyright (c) 1999-2003 Wimba S.A., All Rights Reserved.                   *
 *                                                                            *
 * COPYRIGHT:                                                                 *
 *      This software is the property of Wimba S.A.                           *
 *      This software is redistributed under the Xiph.org variant of          *
 *      the BSD license.                                                      *
 *      Redistribution and use in source and binary forms, with or without    *
 *      modification, are permitted provided that the following conditions    *
 *      are met:                                                              *
 *      - Redistributions of source code must retain the above copyright      *
 *      notice, this list of conditions and the following disclaimer.         *
 *      - Redistributions in binary form must reproduce the above copyright   *
 *      notice, this list of conditions and the following disclaimer in the   *
 *      documentation and/or other materials provided with the distribution.  *
 *      - Neither the name of Wimba, the Xiph.org Foundation nor the names of *
 *      its contributors may be used to endorse or promote products derived   *
 *      from this software without specific prior written permission.         *
 *                                                                            *
 * WARRANTIES:                                                                *
 *      This software is made available by the authors in the hope            *
 *      that it will be useful, but without any warranty.                     *
 *      Wimba S.A. is not liable for any consequence related to the           *
 *      use of the provided software.                                         *
 *                                                                            *
 * Class: LU.java                                                             *
 *                                                                            *
 * Author: James LAWRENCE                                                     *
 * Modified by: Marc GIMPEL                                                   *
 * Based on code by: Jean-Marc VALIN                                          *
 *                                                                            *
 * Date: March 2003                                                           *
 *                                                                            *
 ******************************************************************************/

/* $Id: LspQuant.java,v 1.2 2004/10/21 16:21:57 mgimpel Exp $ */

/* Copyright (C) 2002 Jean-Marc Valin 

   Redistribution and use in source and binary forms, with or without
   modification, are permitted provided that the following conditions
   are met:
   
   - Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
   
   - Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.
   
   - Neither the name of the Xiph.org Foundation nor the names of its
   contributors may be used to endorse or promote products derived from
   this software without specific prior written permission.
   
   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
   ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
   A PARTICULAR PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE FOUNDATION OR
   CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
   EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
   PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
   PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
   LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
   NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
   SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package org.xiph.speex;

/**
 * Abstract class that is the base for the various LSP Quantisation and
 * Unquantisation methods.
 * 
 * @author Jim Lawrence, helloNetwork.com
 * @author Marc Gimpel, Wimba S.A. (mgimpel@horizonwimba.com)
 * @version $Revision: 1.2 $
 */
public abstract class LspQuant
  implements Codebook
{
  /** */
  public static final int MAX_LSP_SIZE       = 20;

  /**
   * Constructor
   */
  protected LspQuant()
  {
  }

  /**
   * Line Spectral Pair Quantification.
   * @param lsp - Line Spectral Pairs table.
   * @param qlsp - Quantified Line Spectral Pairs table.
   * @param order
   * @param bits - Speex bits buffer.
   */
  public abstract void quant(final float[] lsp,
                             final float[] qlsp,
                             final int order,
                             final Bits bits); 
  
  /**
   * Line Spectral Pair Unquantification.
   * @param lsp - Line Spectral Pairs table.
   * @param order
   * @param bits - Speex bits buffer.
   */
  public abstract void unquant(float[] lsp, int order, Bits bits); 
  
  /**
   * Read the next 6 bits from the buffer, and using the value read and the
   * given codebook, rebuild LSP table.
   * @param lsp
   * @param tab
   * @param bits - Speex bits buffer.
   * @param k
   * @param ti
   * @param li
   */
  protected void unpackPlus(final float[] lsp,
                            final int[] tab,
                            final Bits bits,
                            final float k,
                            final int ti,
                            final int li)
  {
    int id=bits.unpack(6);
    for (int i=0;i<ti;i++)
      lsp[i+li] += k * (float)tab[id*ti+i];
  }
  
  /**
   * LSP quantification
   * Note: x is modified
   * @param x
   * @param xs
   * @param cdbk
   * @param nbVec
   * @param nbDim
   * @return the index of the best match in the codebook
   * (NB x is also modified).
   */
  protected static int lsp_quant(final float[] x,
                                 final int xs,
                                 final int[] cdbk,
                                 final int nbVec,
                                 final int nbDim)
  {
    int i, j;
    float dist, tmp;
    float best_dist=0;
    int best_id=0;
    int ptr=0;
    for (i=0; i<nbVec; i++) {
      dist=0;
      for (j=0; j<nbDim; j++) {
        tmp=(x[xs+j]-cdbk[ptr++]);
        dist+=tmp*tmp;
      }
      if (dist<best_dist || i==0) {
        best_dist=dist;
        best_id=i;
      }
    }

    for (j=0; j<nbDim; j++)
      x[xs+j] -= cdbk[best_id*nbDim+j];
    
    return best_id;
  }

  /**
   * LSP weighted quantification
   * Note: x is modified
   * @param x
   * @param xs
   * @param weight
   * @param ws
   * @param cdbk
   * @param nbVec
   * @param nbDim
   * @return the index of the best match in the codebook
   * (NB x is also modified).
   */
  protected static int lsp_weight_quant(final float[] x,
                                        final int xs,
                                        final float[] weight,
                                        final int ws,
                                        final int[] cdbk,
                                        final int nbVec,
                                        final int nbDim)
  {
    int i,j;
    float dist, tmp;
    float best_dist=0;
    int best_id=0;
    int ptr=0;
    for (i=0; i<nbVec; i++) {
      dist=0;
      for (j=0; j<nbDim; j++) {
        tmp=(x[xs+j]-cdbk[ptr++]);
        dist+=weight[ws+j]*tmp*tmp;
      }
      if (dist<best_dist || i==0) {
        best_dist=dist;
        best_id=i;
      }
    }
    for (j=0; j<nbDim; j++)
      x[xs+j] -= cdbk[best_id*nbDim+j];
    return best_id;
  }
}

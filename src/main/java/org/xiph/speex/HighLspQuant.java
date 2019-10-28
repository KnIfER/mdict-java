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
 * Class: HighLu.java                                                         *
 *                                                                            *
 * Author: James LAWRENCE                                                     *
 * Modified by: Marc GIMPEL                                                   *
 * Based on code by: Jean-Marc VALIN                                          *
 *                                                                            *
 * Date: March 2003                                                           *
 *                                                                            *
 ******************************************************************************/

/* $Id: HighLspQuant.java,v 1.2 2004/10/21 16:21:57 mgimpel Exp $ */

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
 * LSP Quantisation and Unquantisation (high)
 * 
 * @author Jim Lawrence, helloNetwork.com
 * @author Marc Gimpel, Wimba S.A. (mgimpel@horizonwimba.com)
 * @version $Revision: 1.2 $
 */
public class HighLspQuant
  extends LspQuant
{
  /**
   * Line Spectral Pair Quantification (high).
   * @param lsp - Line Spectral Pairs table.
   * @param qlsp - Quantified Line Spectral Pairs table.
   * @param order
   * @param bits - Speex bits buffer.
   */
  public final void quant(final float[] lsp,
                          final float[] qlsp,
                          final int order,
                          final Bits bits)
  {
    int i;
    float tmp1, tmp2;
    int id;
    float[] quant_weight = new float[MAX_LSP_SIZE];

    for (i=0;i<order;i++)
      qlsp[i]=lsp[i];
    quant_weight[0] = 1/(qlsp[1]-qlsp[0]);
    quant_weight[order-1] = 1/(qlsp[order-1]-qlsp[order-2]);
    for (i=1;i<order-1;i++)
    {
      tmp1 = 1/(qlsp[i]-qlsp[i-1]);
      tmp2 = 1/(qlsp[i+1]-qlsp[i]);
      quant_weight[i] = tmp1 > tmp2 ? tmp1 : tmp2;
    }

    for (i=0;i<order;i++)
      qlsp[i]-=(.3125*i+.75);
    for (i=0;i<order;i++)
      qlsp[i]*=256;
    id = lsp_quant(qlsp, 0, high_lsp_cdbk, 64, order);
    bits.pack(id, 6);

    for (i=0;i<order;i++)
      qlsp[i]*=2;
    id = lsp_weight_quant(qlsp, 0, quant_weight, 0, high_lsp_cdbk2, 64, order);
    bits.pack(id, 6);

    for (i=0;i<order;i++)
      qlsp[i]*=0.0019531;
    for (i=0;i<order;i++)
      qlsp[i]=lsp[i]-qlsp[i];
  }

  /**
   * Line Spectral Pair Unquantification (high).
   * @param lsp - Line Spectral Pairs table.
   * @param order
   * @param bits - Speex bits buffer.
   */
  public final void unquant(final float[] lsp,
                            final int order,
                            final Bits bits)
  {
    for (int i=0;i<order;i++) {
      lsp[i]=.3125f*i+.75f;
    }
    unpackPlus(lsp, high_lsp_cdbk, bits, 0.0039062f, order, 0);
    unpackPlus(lsp, high_lsp_cdbk2, bits, 0.0019531f, order, 0);
  } 
}

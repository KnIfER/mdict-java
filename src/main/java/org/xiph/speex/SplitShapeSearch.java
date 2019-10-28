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
 * Class: SplitShapeSearch.java                                               *
 *                                                                            *
 * Author: James LAWRENCE                                                     *
 * Modified by: Marc GIMPEL                                                   *
 * Based on code by: Jean-Marc VALIN                                          *
 *                                                                            *
 * Date: March 2003                                                           *
 *                                                                            *
 ******************************************************************************/

/* $Id: SplitShapeSearch.java,v 1.2 2004/10/21 16:21:57 mgimpel Exp $ */

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
 * Split shape codebook search
 * 
 * @author Jim Lawrence, helloNetwork.com
 * @author Marc Gimpel, Wimba S.A. (mgimpel@horizonwimba.com)
 * @version $Revision: 1.2 $
 */
public class SplitShapeSearch
  extends CbSearch
{
  /** */
  public static final int MAX_COMPLEXITY = 10;
  
  private int   subframesize;
  private int   subvect_size;
  private int   nb_subvect;
  private int[] shape_cb;
  private int   shape_cb_size;
  private int   shape_bits;
  private int   have_sign;
  private int[] ind; 
  private int[] signs;
  // Varibles used by the encoder
  private float[]   t, e, E, r2;
  private float[][] ot, nt;
  private int[][]   nind, oind;
   
  /**
   * Constructor
   * @param subframesize
   * @param subvect_size
   * @param nb_subvect
   * @param shape_cb
   * @param shape_bits
   * @param have_sign
   */
  public SplitShapeSearch(final int   subframesize,
                          final int   subvect_size,
                          final int   nb_subvect,
                          final int[] shape_cb,
                          final int   shape_bits,
                          final int   have_sign)
  {
    this.subframesize = subframesize;
    this.subvect_size = subvect_size;
    this.nb_subvect   = nb_subvect;
    this.shape_cb     = shape_cb;
    this.shape_bits   = shape_bits;
    this.have_sign    = have_sign;  
    this.ind          = new int[nb_subvect]; 
    this.signs        = new int[nb_subvect];
    shape_cb_size = 1<<shape_bits;
    ot=new float[MAX_COMPLEXITY][subframesize];
    nt=new float[MAX_COMPLEXITY][subframesize];
    oind=new int[MAX_COMPLEXITY][nb_subvect];
    nind=new int[MAX_COMPLEXITY][nb_subvect];
    t = new float[subframesize];
    e = new float[subframesize];
    r2 = new float[subframesize];
    E = new float[shape_cb_size];
  }

  /**
   * Codebook Search Quantification (Split Shape).
   * @param target   target vector
   * @param ak       LPCs for this subframe
   * @param awk1     Weighted LPCs for this subframe
   * @param awk2     Weighted LPCs for this subframe
   * @param p        number of LPC coeffs
   * @param nsf      number of samples in subframe
   * @param exc      excitation array.
   * @param es       position in excitation array.
   * @param r
   * @param bits     Speex bits buffer.
   * @param complexity
   */
  public final void quant(float[] target, float[] ak, float[] awk1, float[] awk2,
                          int p, int nsf, float[] exc, int es, float[] r,
                          Bits bits, int complexity)
  {
    int i,j,k,m,n,q;
    float[] resp;
    float[] ndist, odist;
    int[] best_index;
    float[] best_dist;

    int N=complexity;
    if (N>10)
      N=10;

    resp = new float[shape_cb_size*subvect_size];

    best_index = new int[N];
    best_dist = new float[N];
    ndist = new float[N];
    odist = new float[N];
    
    for (i=0;i<N;i++) {
      for (j=0;j<nb_subvect;j++)
        nind[i][j]=oind[i][j]=-1;
    }

    for (j=0;j<N;j++)
      for (i=0;i<nsf;i++)
        ot[j][i]=target[i];

//    System.arraycopy(target, 0, t, 0, nsf);

    /* Pre-compute codewords response and energy */
    for (i=0; i<shape_cb_size; i++) {
      int res;
      int shape;

      res = i*subvect_size;
      shape = i*subvect_size;

      /* Compute codeword response using convolution with impulse response */
      for (j=0; j<subvect_size; j++) {
        resp[res+j]=0;
        for (k=0;k<=j;k++)
          resp[res+j] += 0.03125*shape_cb[shape+k]*r[j-k];
      }
      
      /* Compute codeword energy */
      E[i]=0;
      for (j=0; j<subvect_size; j++)
        E[i]+=resp[res+j]*resp[res+j];
    }

    for (j=0; j<N; j++)
      odist[j]=0;
    /*For all subvectors*/
    for (i=0; i<nb_subvect; i++) {
      int offset = i*subvect_size;
      /*"erase" nbest list*/
      for (j=0; j<N; j++)
        ndist[j]=-1;

      /*For all n-bests of previous subvector*/
      for (j=0; j<N; j++) {
        /*Find new n-best based on previous n-best j*/
        if (have_sign != 0)
          VQ.nbest_sign(ot[j], offset, resp, subvect_size, shape_cb_size, E, N, best_index, best_dist);
        else
          VQ.nbest(ot[j], offset, resp, subvect_size, shape_cb_size, E, N, best_index, best_dist);

        /*For all new n-bests*/
        for (k=0; k<N; k++) {
          float[] ct;
          float err=0;
          ct = ot[j];
          /*update target*/

          /*previous target*/
          for (m=offset; m<offset+subvect_size; m++)
            t[m]=ct[m];

          /* New code: update only enough of the target to calculate error*/
          {
            int rind;
            int res;
            float sign=1;
            rind = best_index[k];
            if (rind>=shape_cb_size) {
              sign = -1;
              rind -= shape_cb_size;
            }
            res = rind*subvect_size;
            if (sign>0)
              for (m=0;m<subvect_size;m++)
                t[offset+m] -= resp[res+m];
            else
              for (m=0;m<subvect_size;m++)
                t[offset+m] += resp[res+m];
          }
          
          /*compute error (distance)*/
          err=odist[j];
          for (m=offset;m<offset+subvect_size;m++)
            err += t[m]*t[m];
          /*update n-best list*/
          if (err<ndist[N-1] || ndist[N-1]<-.5) {

            /*previous target (we don't care what happened before*/
            for (m=offset+subvect_size; m<nsf; m++)
              t[m] = ct[m];
            /* New code: update the rest of the target only if it's worth it */
            for (m=0; m<subvect_size; m++) {
              float g;
              int rind;
              float sign = 1;
              rind = best_index[k];
              if (rind>=shape_cb_size) {
                sign = -1;
                rind -= shape_cb_size;
              }

              g = sign*0.03125f*shape_cb[rind*subvect_size+m];
              q = subvect_size-m;
              for (n=offset+subvect_size; n<nsf; n++, q++)
                t[n] -= g*r[q];
            }


            for (m=0; m<N; m++) {
              if (err < ndist[m] || ndist[m]<-.5) {
                for (n=N-1; n>m; n--) {
                  for (q=offset+subvect_size; q<nsf; q++)
                    nt[n][q] = nt[n-1][q];
                  for (q=0; q<nb_subvect; q++)
                    nind[n][q] = nind[n-1][q];
                  ndist[n] = ndist[n-1];
                }
                for (q=offset+subvect_size; q<nsf; q++)
                  nt[m][q] = t[q];
                for (q=0; q<nb_subvect; q++)
                  nind[m][q] = oind[j][q];
                nind[m][i] = best_index[k];
                ndist[m] = err;
                break;
              }
            }
          }
        }
        if (i==0)
          break;
      }

      /*update old-new data*/
      /* just swap pointers instead of a long copy */
      {
        float[][] tmp2;
        tmp2=ot;
        ot=nt;
        nt=tmp2;
      }
      for (j=0; j<N; j++)
        for (m=0; m<nb_subvect; m++)
          oind[j][m] = nind[j][m];
      for (j=0; j<N; j++)
        odist[j] = ndist[j];
    }

    /*save indices*/
    for (i=0; i<nb_subvect; i++) {
      ind[i] = nind[0][i];
      bits.pack(ind[i], shape_bits+have_sign);
    }
    
    /* Put everything back together */
    for (i=0; i<nb_subvect; i++) {
      int rind;
      float sign = 1;
      rind = ind[i];
      if (rind >= shape_cb_size) {
        sign = -1;
        rind -= shape_cb_size;
      }

      for (j=0; j<subvect_size; j++)
        e[subvect_size*i+j] = sign*0.03125f*shape_cb[rind*subvect_size+j];
    }   
    /* Update excitation */
    for (j=0; j<nsf; j++)
      exc[es+j] += e[j];
    
    /* Update target */
    Filters.syn_percep_zero(e, 0, ak, awk1, awk2, r2, nsf, p);
    for (j=0; j<nsf; j++)
      target[j] -= r2[j];
  }

  /**
   * Codebook Search Unquantification (Split Shape).
   * @param exc - excitation array.
   * @param es - position in excitation array.
   * @param nsf - number of samples in subframe.
   * @param bits - Speex bits buffer.
   */
  public final void unquant(float[] exc, int es, int nsf, Bits bits)
  {
    int i,j;

    /* Decode codewords and gains */
    for (i=0; i<nb_subvect; i++) {
      if (have_sign!=0)
        signs[i] = bits.unpack(1);
      else
        signs[i] = 0;
      ind[i] = bits.unpack(shape_bits);
    }

    /* Compute decoded excitation */
    for (i=0; i<nb_subvect; i++) {
      float s=1.0f;
      if (signs[i]!=0)
        s=-1.0f;
      for (j=0; j<subvect_size; j++){
        exc[es+subvect_size*i+j]+=s*0.03125f*(float)shape_cb[ind[i]*subvect_size+j]; 
      }
    }
  }
}

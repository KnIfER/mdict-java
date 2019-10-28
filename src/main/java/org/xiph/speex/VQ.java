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
 * Class: VQ.java                                                             *
 *                                                                            *
 * Author: Marc GIMPEL                                                        *
 * Based on code by: Jean-Marc VALIN                                          *
 *                                                                            *
 * Date: 15th April 2003                                                      *
 *                                                                            *
 ******************************************************************************/

/* $Id: VQ.java,v 1.2 2004/10/21 16:21:57 mgimpel Exp $ */

/* Copyright (C) 2002 Jean-Marc Valin
   File: vq.c
   Vector quantization

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
 * Vector Quantization.
 * 
 * @author Marc Gimpel, Wimba S.A. (mgimpel@horizonwimba.com)
 * @version $Revision: 1.2 $
 */
public class VQ
{
  /**
   * Finds the index of the entry in a codebook that best matches the input.
   * @param in - the value to compare.
   * @param codebook - the list of values to search through for the best match.
   * @param entries - the size of the codebook.
   * @return the index of the entry in a codebook that best matches the input.
   */
  public static final int index(final float in,
                                final float[] codebook,
                                final int entries)
  {
    int i;
    float min_dist=0;
    int best_index=0;
    for (i=0;i<entries;i++)
    {
      float dist = in-codebook[i];
      dist = dist*dist;
      if (i==0 || dist<min_dist)
      {
        min_dist=dist;
        best_index=i;
      }
    }
    return best_index;
  }

  /**
   * Finds the index of the entry in a codebook that best matches the input.
   * @param in - the vector to compare.
   * @param codebook - the list of values to search through for the best match.
   * @param len - the size of the vector.
   * @param entries - the size of the codebook.
   * @return the index of the entry in a codebook that best matches the input.
   */
  public static final int index(final float[] in,
                                final float[] codebook,
                                final int len,
                                final int entries)
  {
    int i,j,k=0;
    float min_dist=0;
    int best_index=0;
    for (i=0;i<entries;i++)
    {
      float dist=0;
      for (j=0;j<len;j++)
      {
        float tmp = in[j]-codebook[k++];
        dist += tmp*tmp;
      }
      if (i==0 || dist<min_dist)
      {
        min_dist=dist;
        best_index=i;
      }
    }
    return best_index;
  }


  /**
   * Finds the indices of the n-best entries in a codebook
   * @param in
   * @param offset
   * @param codebook
   * @param len
   * @param entries
   * @param E
   * @param N
   * @param nbest
   * @param best_dist
   */
  public static final void nbest(final float[] in,
                                 final int offset,
                                 final float[] codebook,
                                 final int len,
                                 final int entries,
                                 final float[] E,
                                 final int N,
                                 final int[] nbest,
                                 final float[] best_dist)
  {
    int i, j, k, l=0, used=0;
    for (i=0;i<entries;i++)
    {
      float dist=.5f*E[i];
      for (j=0;j<len;j++)
        dist -= in[offset+j]*codebook[l++];
      if (i<N || dist<best_dist[N-1]) {
        for (k=N-1; (k >= 1) && (k > used || dist < best_dist[k-1]); k--) {
          best_dist[k] = best_dist[k-1];
          nbest[k] = nbest[k-1];
        }
        best_dist[k]=dist;
        nbest[k]=i;
        used++;
      }
    }
  }

  /**
   * Finds the indices of the n-best entries in a codebook with sign
   * @param in
   * @param offset
   * @param codebook
   * @param len
   * @param entries
   * @param E
   * @param N
   * @param nbest
   * @param best_dist
   */
  public static final void nbest_sign(final float[] in,
                                      final int offset,
                                      final float[] codebook,
                                      final int len,
                                      final int entries,
                                      final float[] E,
                                      final int N,
                                      final int[] nbest,
                                      final float[] best_dist)
  {
    int i, j, k, l=0, sign, used=0;
    for (i=0;i<entries;i++) {
      float dist=0;
      for (j=0;j<len;j++)
        dist -= in[offset+j]*codebook[l++];
      if (dist>0) {
        sign=1;
        dist=-dist;
      }
      else {
        sign=0;
      }
      dist += .5*E[i];
      if (i<N || dist<best_dist[N-1]) {
        for (k=N-1; (k >= 1) && (k > used || dist < best_dist[k-1]); k--)
        {
          best_dist[k]=best_dist[k-1];
          nbest[k] = nbest[k-1];
        }
        best_dist[k]=dist;
        nbest[k]=i;
        used++;
        if (sign != 0)
          nbest[k]+=entries;
      }
    }
  }
}

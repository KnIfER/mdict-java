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
 * Class: Stereo.java                                                         *
 *                                                                            *
 * Author: Marc GIMPEL                                                        *
 * Based on code by: Jean-Marc VALIN                                          *
 *                                                                            *
 * Date: 22nd April 2003                                                      *
 *                                                                            *
 ******************************************************************************/

/* $Id: Stereo.java,v 1.2 2004/10/21 16:21:57 mgimpel Exp $ */

/* Copyright (C) 2002 Jean-Marc Valin 
   File: stereo.c

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
 * Stereo
 * 
 * @author Marc Gimpel, Wimba S.A. (mgimpel@horizonwimba.com)
 * @version $Revision: 1.2 $
 */
public class Stereo
{
  /** Inband code number for Stereo */
  public static final int SPEEX_INBAND_STEREO = 9;
  /** */
  public static final float[] e_ratio_quant = {.25f, .315f, .397f, .5f};

  private float balance      = 1f;   /** Left/right balance info */
  private float e_ratio      = 0.5f; /** Ratio of energies: E(left+right)/[E(left)+E(right)]  */
  private float smooth_left  = 1f;   /** Smoothed left channel gain */
  private float smooth_right = 1f;   /** Smoothed right channel gain */
//  private float reserved1;           /** Reserved for future use */
//  private float reserved2;           /** Reserved for future use */

  /**
   * Transforms a stereo frame into a mono frame and stores intensity stereo
   * info in 'bits'.
   * @param bits - Speex bits buffer.
   * @param data
   * @param frameSize
   */
  public static void encode(final Bits bits,
                            final float[] data,
                            final int frameSize)
  {
    int i, tmp;
    float e_left=0, e_right=0, e_tot=0;
    float balance, e_ratio;
    for (i=0;i<frameSize;i++) {
      e_left  += data[2*i]*data[2*i];
      e_right += data[2*i+1]*data[2*i+1];
      data[i] =  .5f*(data[2*i]+data[2*i+1]);
      e_tot   += data[i]*data[i];
    }
    balance=(e_left+1)/(e_right+1);
    e_ratio = e_tot/(1+e_left+e_right);
    /*Quantization*/
    bits.pack(14, 5);
    bits.pack(SPEEX_INBAND_STEREO, 4);
    balance=(float)(4*Math.log(balance));

    /*Pack balance*/
    if (balance>0)
      bits.pack(0, 1);
    else
      bits.pack(1, 1);
    balance=(float) Math.floor(.5f+Math.abs(balance));
    if (balance>30)
      balance=31;
    bits.pack((int)balance, 5);
    
    /*Quantize energy ratio*/
    tmp=VQ.index(e_ratio, e_ratio_quant, 4);
    bits.pack(tmp, 2);
  }

  /**
   * Transforms a mono frame into a stereo frame using intensity stereo info.
   * @param data - float array of size 2*frameSize, that contains the mono
   * audio samples in the first half. When the function has completed, the
   * array will contain the interlaced stereo audio samples.
   * @param frameSize - the size of a frame of mono audio samples.
   */
  public void decode(final float[] data, final int frameSize)
  {
    int i;
    float e_tot=0, e_left, e_right, e_sum;

    for (i=frameSize-1; i>=0; i--) {
      e_tot += data[i]*data[i];
    }
    e_sum=e_tot/e_ratio;
    e_left  = e_sum*balance / (1+balance);
    e_right = e_sum-e_left;
    e_left  = (float)Math.sqrt(e_left/(e_tot+.01f));
    e_right = (float)Math.sqrt(e_right/(e_tot+.01f));

    for (i=frameSize-1;i>=0;i--) {
      float ftmp=data[i];
      smooth_left  = .98f*smooth_left  + .02f*e_left;
      smooth_right = .98f*smooth_right + .02f*e_right;
      data[2*i] = smooth_left*ftmp;
      data[2*i+1] = smooth_right*ftmp;
    }
  }

  /**
   * Callback handler for intensity stereo info
   * @param bits - Speex bits buffer.
   */
  public void init(Bits bits)
  {
    float sign=1;
    int tmp;
    if (bits.unpack(1) != 0)
      sign=-1;
    tmp = bits.unpack(5);
    balance = (float) Math.exp(sign*.25*tmp);
    tmp = bits.unpack(2);
    e_ratio = e_ratio_quant[tmp];
  }
}

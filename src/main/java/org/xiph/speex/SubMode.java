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
 * Class: SubMode.java                                                        *
 *                                                                            *
 * Author: James LAWRENCE                                                     *
 * Modified by: Marc GIMPEL                                                   *
 * Based on code by: Jean-Marc VALIN                                          *
 *                                                                            *
 * Date: March 2003                                                           *
 *                                                                            *
 ******************************************************************************/

/* $Id: SubMode.java,v 1.2 2004/10/21 16:21:57 mgimpel Exp $ */

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
 * Speex SubMode
 * 
 * @author Jim Lawrence, helloNetwork.com
 * @author Marc Gimpel, Wimba S.A. (mgimpel@horizonwimba.com)
 * @version $Revision: 1.2 $
 */
public class SubMode
{
  /** Set to -1 for "normal" modes, otherwise encode pitch using a global pitch and allowing a +- lbr_pitch variation (for low not-rates)*/
  public int      lbr_pitch;
  /** Use the same (forced) pitch gain for all sub-frames */
  public int      forced_pitch_gain;
  /** Number of bits to use as sub-frame innovation gain */
  public int      have_subframe_gain;
  /** Apply innovation quantization twice for higher quality (and higher bit-rate)*/
  public int      double_codebook;
  /** LSP quantization/unquantization function */
  public LspQuant lsqQuant;
  /** Long-term predictor (pitch) un-quantizer */
  public Ltp      ltp;
  /** Codebook Search un-quantizer*/
  public CbSearch innovation;
  /** Enhancer constant */
  public float    lpc_enh_k1;
  /** Enhancer constant */
  public float    lpc_enh_k2;
  /** Gain of enhancer comb filter */
  public float    comb_gain;
  /** Number of bits per frame after encoding*/
  public int      bits_per_frame;
   
  /**
   * Constructor
   */
  public SubMode(final int      lbr_pitch,
                 final int      forced_pitch_gain, 
                 final int      have_subframe_gain, 
                 final int      double_codebook,   
                 final LspQuant lspQuant,    
                 final Ltp      ltp,         
                 final CbSearch innovation,
                 final float    lpc_enh_k1,
                 final float    lpc_enh_k2,
                 final float    comb_gain,
                 final int      bits_per_frame)
  {
    this.lbr_pitch          = lbr_pitch;
    this.forced_pitch_gain  = forced_pitch_gain;
    this.have_subframe_gain = have_subframe_gain;
    this.double_codebook    = double_codebook;
    this.lsqQuant           = lspQuant;
    this.ltp                = ltp;
    this.innovation         = innovation;
    this.lpc_enh_k1         = lpc_enh_k1;
    this.lpc_enh_k2         = lpc_enh_k2;
    this.comb_gain          = comb_gain;
    this.bits_per_frame     = bits_per_frame;
  }
}

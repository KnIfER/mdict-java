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
 * Class: Decoder.java                                                        *
 *                                                                            *
 * Author: James LAWRENCE                                                     *
 * Modified by: Marc GIMPEL                                                   *
 * Based on code by: Jean-Marc VALIN                                          *
 *                                                                            *
 * Date: March 2003                                                           *
 *                                                                            *
 ******************************************************************************/

/* $Id: Decoder.java,v 1.2 2004/10/21 16:21:57 mgimpel Exp $ */

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

import java.io.StreamCorruptedException;

/**
 * Speex Decoder inteface, used as a base for the Narrowband and sideband
 * decoders.
 * 
 * @author Jim Lawrence, helloNetwork.com
 * @author Marc Gimpel, Wimba S.A. (mgimpel@horizonwimba.com)
 * @version $Revision: 1.2 $
 */
public interface Decoder
{
  /**
   * Decode the given input bits.
   * @param bits - Speex bits buffer.
   * @param out - the decoded mono audio frame.
   * @return 1 if a terminator was found, 0 if not.
   * @throws StreamCorruptedException If there is an error detected in the
   * data stream.
   */
  public int decode(Bits bits, float[] out)
    throws StreamCorruptedException;
  
  /**
   * Decode the given bits to stereo.
   * @param data - float array of size 2*frameSize, that contains the mono
   * audio samples in the first half. When the function has completed, the
   * array will contain the interlaced stereo audio samples.
   * @param frameSize - the size of a frame of mono audio samples.
   */
  public void decodeStereo(float[] data, int frameSize);

  /**
   * Enables or disables perceptual enhancement.
   * @param enhanced
   */
  public void setPerceptualEnhancement(boolean enhanced);
  
  /**
   * Returns whether perceptual enhancement is enabled or disabled.
   * @return whether perceptual enhancement is enabled or disabled.
   */
  public boolean getPerceptualEnhancement();

  /**
   * Returns the size of a frame.
   * @return the size of a frame.
   */
  public int  getFrameSize();

  /**
   * Returns whether or not we are using Discontinuous Transmission encoding.
   * @return whether or not we are using Discontinuous Transmission encoding.
   */
  public boolean getDtx();

  /**
   * Returns the Pitch Gain array.
   * @return the Pitch Gain array.
   */
  public float[] getPiGain();

  /**
   * Returns the excitation array.
   * @return the excitation array.
   */
  public float[] getExc();
  
  /**
   * Returns the innovation array.
   * @return the innovation array.
   */
  public float[] getInnov();
}

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
 * Class: Inband.java                                                         *
 *                                                                            *
 * Author: Marc GIMPEL                                                        *
 *                                                                            *
 * Date: 14th July 2003                                                       *
 *                                                                            *
 ******************************************************************************/

/* $Id: Inband.java,v 1.2 2004/10/21 16:21:57 mgimpel Exp $ */

package org.xiph.speex;

import java.io.StreamCorruptedException;

/**
 * Speex in-band and User in-band controls.
 * 
 * @author Marc Gimpel, Wimba S.A. (mgimpel@horizonwimba.com)
 * @version $Revision: 1.2 $
 */
public class Inband
{
  private Stereo stereo;

  /**
   * Constructor.
   * @param stereo
   */
  public Inband (final Stereo stereo)
  {
    this.stereo = stereo;
  }

  /**
   * Speex in-band request (submode=14).
   * @param bits - Speex bits buffer.
   * @throws StreamCorruptedException If stream seems corrupted.
   */
  public void speexInbandRequest(final Bits bits)
    throws StreamCorruptedException
  {
    int code = bits.unpack(4);
    switch (code) {
      case 0: // asks the decoder to set perceptual enhancment off (0) or on (1)
        bits.advance(1);
        break;
      case 1: // asks (if 1) the encoder to be less "aggressive" due to high packet loss
        bits.advance(1);
        break;
      case 2: // asks the encoder to switch to mode N
        bits.advance(4);
        break;
      case 3: // asks the encoder to switch to mode N for low-band
        bits.advance(4);
        break;
      case 4: // asks the encoder to switch to mode N for high-band
        bits.advance(4);
        break;
      case 5: // asks the encoder to switch to quality N for VBR
        bits.advance(4);
        break;
      case 6: // request acknowledgement (0=no, 1=all, 2=only for inband data)
        bits.advance(4);
        break;
      case 7: // asks the encoder to set CBR(0), VAD(1), DTX(3), VBR(5), VBR+DTX(7)
        bits.advance(4);
        break;
      case 8: // transmit (8-bit) character to the other end
        bits.advance(8);
        break;
      case 9: // intensity stereo information
        // setup the stereo decoder; to skip: tmp = bits.unpack(8); break;
        stereo.init(bits); // read 8 bits
        break;
      case 10: // announce maximum bit-rate acceptable (N in byets/second)
        bits.advance(16);
        break;
      case 11: // reserved
        bits.advance(16);
        break;
      case 12: // Acknowledge receiving packet N
        bits.advance(32);
        break;
      case 13: // reserved
        bits.advance(32);
        break;
      case 14: // reserved
        bits.advance(64);
        break;
      case 15: // reserved
        bits.advance(64);
        break;
      default:
    }
  }
  
  /**
   * User in-band request (submode=13).
   * @param bits - Speex bits buffer.
   * @throws StreamCorruptedException If stream seems corrupted.
   */
  public void userInbandRequest(final Bits bits)
    throws StreamCorruptedException
  {
    int req_size = bits.unpack(4);
    bits.advance(5+8*req_size);
  }
}

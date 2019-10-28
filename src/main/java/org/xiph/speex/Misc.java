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
 * Class: Misc.java                                                           *
 *                                                                            *
 * Author: Marc GIMPEL                                                        *
 *                                                                            *
 * Date: 14th July 2003                                                       *
 *                                                                            *
 ******************************************************************************/

/* $Id: Misc.java,v 1.2 2004/10/21 16:21:57 mgimpel Exp $ */

package org.xiph.speex;

/**
 * Miscellaneous functions
 * 
 * @author Marc Gimpel, Wimba S.A. (mgimpel@horizonwimba.com)
 * @version $Revision: 1.2 $
 */
public class Misc
{
  /**
   * Builds an Asymmetric "pseudo-Hamming" window.
   * @param windowSize
   * @param subFrameSize
   * @return an Asymmetric "pseudo-Hamming" window.
   */
  public static float[] window(final int windowSize, final int subFrameSize)
  {
    int i;
    int part1 = subFrameSize * 7 / 2;
    int part2 = subFrameSize * 5 / 2;
    float[] window = new float[windowSize];
    for (i=0; i<part1; i++)
      window[i]=(float) (0.54 - 0.46 * Math.cos(Math.PI * i / part1));
    for (i=0; i<part2; i++)
      window[part1+i]=(float) (0.54 + 0.46 * Math.cos(Math.PI * i / part2));
    return window;
  }
  
  /**
   * Create the window for autocorrelation (lag-windowing).
   * @param lpcSize
   * @param lagFactor
   * @return the window for autocorrelation.
   */
  public static float[] lagWindow(final int lpcSize, final float lagFactor)
  {
    float[] lagWindow = new float[lpcSize+1];
    for (int i=0; i<lpcSize+1; i++)
      lagWindow[i]=(float) Math.exp(-0.5 * (2*Math.PI*lagFactor*i) *
                                           (2*Math.PI*lagFactor*i));
    return lagWindow;
  }
}

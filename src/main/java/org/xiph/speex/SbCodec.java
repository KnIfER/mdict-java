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
 * Class: SbCodec.java                                                        *
 *                                                                            *
 * Author: Marc GIMPEL                                                        *
 * Based on code by: Jean-Marc VALIN                                          *
 *                                                                            *
 * Date: 14th July 2003                                                       *
 *                                                                            *
 ******************************************************************************/

/* $Id: SbCodec.java,v 1.2 2004/10/21 16:21:57 mgimpel Exp $ */

package org.xiph.speex;

/**
 * Sideband Codec.
 * This class contains all the basic structures needed by the Sideband
 * encoder and decoder.
 * 
 * @author Marc Gimpel, Wimba S.A. (mgimpel@horizonwimba.com)
 * @version $Revision: 1.2 $
 */
public class SbCodec
  extends NbCodec
{
  //---------------------------------------------------------------------------
  // Constants
  //---------------------------------------------------------------------------
  /** The Sideband Frame Size gives the size in bits of a Sideband frame for a given sideband submode. */
  public static final int[] SB_FRAME_SIZE = {4, 36, 112, 192, 352, -1, -1, -1};
  /** The Sideband Submodes gives the number of submodes possible for the Sideband codec. */
  public static final int SB_SUBMODES     = 8;
  /** The Sideband Submodes Bits gives the number bits used to encode the Sideband Submode*/
  public static final int SB_SUBMODE_BITS = 3;
  /** Quadratic Mirror Filter Order */
  public static final int QMF_ORDER = 64;

  //---------------------------------------------------------------------------
  // Parameters
  //---------------------------------------------------------------------------
  /** */
  protected int   fullFrameSize;
  /** */
  protected float foldingGain;

  //---------------------------------------------------------------------------
  // Variables
  //---------------------------------------------------------------------------
  /** */
  protected float[] high;
  /** */
  protected float[] y0, y1;
  /** */
  protected float[] x0d;
  /** */
  protected float[] g0_mem, g1_mem;

  /**
   * Wideband initialisation
   */
  public void wbinit()
  {
    // Initialize SubModes
    submodes     = buildWbSubModes();
    submodeID    = 3;
    // Initialize narrwoband parameters and variables
    //init(160, 40, 8, 640, .9f);
  }

  /**
   * Ultra-wideband initialisation
   */
  public void uwbinit()
  {
    // Initialize SubModes
    submodes     = buildUwbSubModes();
    submodeID    = 1;
    // Initialize narrwoband parameters and variables
    //init(320, 80, 8, 1280, .7f);
  }

  /**
   * Initialisation
   * @param frameSize
   * @param subframeSize
   * @param lpcSize
   * @param bufSize
   * @param foldingGain
   */
  protected void init(final int frameSize,
                      final int subframeSize,
                      final int lpcSize,
                      final int bufSize,
                      final float foldingGain)
  {
    super.init(frameSize, subframeSize, lpcSize, bufSize);
    this.fullFrameSize = 2*frameSize;
    this.foldingGain   = foldingGain;

    lag_factor  = 0.002f;

    high        = new float[fullFrameSize];
    y0          = new float[fullFrameSize];
    y1          = new float[fullFrameSize];
    x0d         = new float[frameSize];
    g0_mem      = new float[QMF_ORDER];
    g1_mem      = new float[QMF_ORDER];
}
  
  /**
   * Build wideband submodes.
   * @return the wideband submodes.
   */
  protected static SubMode[] buildWbSubModes()
  {
    /* Initialize Long Term Predictions */
    HighLspQuant highLU = new HighLspQuant();
    /* Initialize Codebook Searches */
    SplitShapeSearch ssCbHighLbrSearch = new SplitShapeSearch(40, 10, 4, hexc_10_32_table, 5, 0);
    SplitShapeSearch ssCbHighSearch    = new SplitShapeSearch(40, 8, 5, hexc_table, 7, 1);
    /* Initialize wide-band modes */
    SubMode[] wbSubModes = new SubMode[SB_SUBMODES];
    wbSubModes[1] = new SubMode(0, 0, 1, 0, highLU, null, null, .75f, .75f, -1, 36);
    wbSubModes[2] = new SubMode(0, 0, 1, 0, highLU, null, ssCbHighLbrSearch, .85f, .6f, -1, 112);
    wbSubModes[3] = new SubMode(0, 0, 1, 0, highLU, null, ssCbHighSearch, .75f, .7f, -1, 192);
    wbSubModes[4] = new SubMode(0, 0, 1, 1, highLU, null, ssCbHighSearch, .75f, .75f, -1, 352);
    return wbSubModes;
  }

  /**
   * Build ultra-wideband submodes.
   * @return the ultra-wideband submodes.
   */
  protected static SubMode[] buildUwbSubModes()
  {
    /* Initialize Long Term Predictions */
    HighLspQuant highLU = new HighLspQuant();
    SubMode[] uwbSubModes = new SubMode[SB_SUBMODES];
    uwbSubModes[1] = new SubMode(0, 0, 1, 0, highLU, null, null, .75f, .75f, -1, 2);
    return uwbSubModes;
  }

  /**
   * Returns the size of a frame (ex: 160 samples for a narrowband frame,
   * 320 for wideband and 640 for ultra-wideband).
   * @return the size of a frame (number of audio samples in a frame).
   */
  public int  getFrameSize()
  {
    return fullFrameSize;
  }

  /**
   * Returns whether or not we are using Discontinuous Transmission encoding.
   * @return whether or not we are using Discontinuous Transmission encoding.
   */
  public boolean getDtx()
  {
    // TODO - should return DTX for the NbCodec
    return dtx_enabled != 0;
  }

  /**
   * Returns the excitation array.
   * @return the excitation array.
   */
  public float[] getExc()
  {
    int i;
    float[] excTmp = new float[fullFrameSize];
    for (i=0;i<frameSize;i++)
      excTmp[2*i]=2*excBuf[excIdx+i];
    return excTmp;
  }

  /**
   * Returns the innovation array.
   * @return the innovation array.
   */
  public float[] getInnov()
  {
    return getExc();
  }
}

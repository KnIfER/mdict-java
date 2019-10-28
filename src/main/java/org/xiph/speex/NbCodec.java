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
 * Class: NbCodec.java                                                        *
 *                                                                            *
 * Author: Marc GIMPEL                                                        *
 * Based on code by: Jean-Marc VALIN                                          *
 *                                                                            *
 * Date: 14th July 2003                                                       *
 *                                                                            *
 ******************************************************************************/

/* $Id: NbCodec.java,v 1.2 2004/10/21 16:21:57 mgimpel Exp $ */

package org.xiph.speex;

/**
 * Narrowband Codec.
 * This class contains all the basic structures needed by the Narrowband
 * encoder and decoder.
 * 
 * @author Marc Gimpel, Wimba S.A. (mgimpel@horizonwimba.com)
 * @version $Revision: 1.2 $
 */
public class NbCodec
  implements Codebook
{
  //---------------------------------------------------------------------------
  // Constants
  //---------------------------------------------------------------------------
  /** Very small initial value for some of the buffers. */
  public static final float VERY_SMALL = (float) 0e-30;
  /** The Narrowband Frame Size gives the size in bits of a Narrowband frame for a given narrowband submode. */
  public static final int[] NB_FRAME_SIZE = {5, 43, 119, 160, 220, 300, 364, 492, 79, 1, 1, 1, 1, 1, 1, 1};
  /** The Narrowband Submodes gives the number of submodes possible for the Narrowband codec. */
  public static final int NB_SUBMODES     = 16;
  /** The Narrowband Submodes Bits gives the number bits used to encode the Narrowband Submode*/
  public static final int NB_SUBMODE_BITS = 4;
  /** */
  public static final float[] exc_gain_quant_scal1 = {-0.35f, 0.05f};
  /** */
  public static final float[] exc_gain_quant_scal3 = {-2.794750f, -1.810660f,
                                                      -1.169850f, -0.848119f, 
                                                      -0.587190f, -0.329818f,
                                                      -0.063266f, 0.282826f};
  
  //---------------------------------------------------------------------------
  // Tools
  //---------------------------------------------------------------------------
  /** */
  protected Lsp      m_lsp;
  /** */
  protected Filters  filters;

  //---------------------------------------------------------------------------
  // Parameters
  //---------------------------------------------------------------------------
  protected SubMode[] submodes;  /** Sub-mode data */
  protected int       submodeID; /** Activated sub-mode */

  protected int    first;        /** Is this the first frame? */
  protected int    frameSize;    /** Size of frames */
  protected int    subframeSize; /** Size of sub-frames */
  protected int    nbSubframes;  /** Number of sub-frames */
  protected int    windowSize;   /** Analysis (LPC) window length */
  protected int    lpcSize;      /** LPC order */
  protected int    bufSize;      /** Buffer size */
  protected int    min_pitch;    /** Minimum pitch value allowed */
  protected int    max_pitch;    /** Maximum pitch value allowed */
  protected float  gamma1;       /** Perceptual filter: A(z/gamma1) */
  protected float  gamma2;       /** Perceptual filter: A(z/gamma2) */
  protected float  lag_factor;   /** Lag windowing Gaussian width */
  protected float  lpc_floor;    /** Noise floor multiplier for A[0] in LPC analysis*/
  protected float  preemph;      /** Pre-emphasis: P(z) = 1 - a*z^-1*/
  protected float  pre_mem;      /** 1-element memory for pre-emphasis */

  //---------------------------------------------------------------------------
  // Variables
  //---------------------------------------------------------------------------
  protected float[] frmBuf;      /** Input buffer (original signal) */
  protected int     frmIdx;
  protected float[] excBuf;      /** Excitation buffer */
  protected int     excIdx;      /** Start of excitation frame */
  protected float[] innov;       /** Innovation for the frame */
  protected float[] lpc;         /** LPCs for current frame */
  protected float[] qlsp;        /** Quantized LSPs for current frame */
  protected float[] old_qlsp;    /** Quantized LSPs for previous frame */
  protected float[] interp_qlsp; /** Interpolated quantized LSPs */
  protected float[] interp_qlpc; /** Interpolated quantized LPCs */
  protected float[] mem_sp;      /** Filter memory for synthesis signal */
  protected float[] pi_gain;     /** Gain of LPC filter at theta=pi (fe/2) */
  protected float[] awk1, awk2, awk3;
  // Vocoder data
  protected float voc_m1;
  protected float voc_m2;
  protected float voc_mean;
  protected int   voc_offset;

  protected int dtx_enabled;    /** 1 for enabling DTX, 0 otherwise */

  /**
   * Constructor.
   */
  public NbCodec()
  {
    m_lsp   = new Lsp();
    filters = new Filters();
  }

  /**
   * Narrowband initialisation.
   */
  public void nbinit()
  {
    // Initialize SubModes
    submodes     = buildNbSubModes();
    submodeID    = 5;
    // Initialize narrwoband parameters and variables
    init(160, 40, 10, 640);
  }

  /**
   * Initialisation.
   * @param frameSize
   * @param subframeSize
   * @param lpcSize
   * @param bufSize
   */
  protected void init(final int frameSize,
                      final int subframeSize,
                      final int lpcSize,
                      final int bufSize)
  {
    first = 1;
    // Codec parameters, should eventually have several "modes"
    this.frameSize    = frameSize;
    this.windowSize   = frameSize*3/2;
    this.subframeSize = subframeSize;
    this.nbSubframes  = frameSize/subframeSize;
    this.lpcSize      = lpcSize;
    this.bufSize      = bufSize;
    min_pitch  = 17;
    max_pitch  = 144;
    preemph    = 0.0f;
    pre_mem    = 0.0f;
    gamma1     = 0.9f;
    gamma2     = 0.6f;
    lag_factor = .01f;
    lpc_floor  = 1.0001f;

    frmBuf = new float[bufSize];
    frmIdx = bufSize - windowSize;
    excBuf = new float[bufSize];
    excIdx = bufSize - windowSize;
    innov  = new float[frameSize];

    lpc         = new float[lpcSize+1];
    qlsp        = new float[lpcSize];
    old_qlsp    = new float[lpcSize];
    interp_qlsp = new float[lpcSize];
    interp_qlpc = new float[lpcSize+1];
    mem_sp      = new float[5*lpcSize]; // TODO - check why 5 (why not 2 or 1)
    pi_gain     = new float[nbSubframes];

    awk1 = new float[lpcSize+1];
    awk2 = new float[lpcSize+1];
    awk3 = new float[lpcSize+1];

    voc_m1 = voc_m2 = voc_mean = 0;
    voc_offset = 0;    
    dtx_enabled = 0; // disabled by default
  }

  /**
   * Build narrowband submodes
   */
  private static SubMode[] buildNbSubModes()
  {
    /* Initialize Long Term Predictions */
    Ltp3Tap ltpNb   = new Ltp3Tap(gain_cdbk_nb,  7, 7);
    Ltp3Tap ltpVlbr = new Ltp3Tap(gain_cdbk_lbr, 5, 0);
    Ltp3Tap ltpLbr  = new Ltp3Tap(gain_cdbk_lbr, 5, 7);
    Ltp3Tap ltpMed  = new Ltp3Tap(gain_cdbk_lbr, 5, 7);
    LtpForcedPitch ltpFP = new LtpForcedPitch();
    /* Initialize Codebook Searches */
    NoiseSearch noiseSearch = new NoiseSearch();
    SplitShapeSearch ssNbVlbrSearch = new SplitShapeSearch(40, 10, 4, exc_10_16_table, 4, 0);
    SplitShapeSearch ssNbLbrSearch  = new SplitShapeSearch(40, 10, 4, exc_10_32_table, 5, 0);
    SplitShapeSearch ssNbSearch     = new SplitShapeSearch(40,  5, 8, exc_5_64_table,  6, 0);
    SplitShapeSearch ssNbMedSearch  = new SplitShapeSearch(40,  8, 5, exc_8_128_table, 7, 0);
    SplitShapeSearch ssSbSearch     = new SplitShapeSearch(40,  5, 8, exc_5_256_table, 8, 0);
    SplitShapeSearch ssNbUlbrSearch = new SplitShapeSearch(40, 20, 2, exc_20_32_table, 5, 0);
    /* Initialize Line Spectral Pair Quantizers */
    NbLspQuant nbLspQuant   = new NbLspQuant();
    LbrLspQuant lbrLspQuant = new LbrLspQuant();
    /* Initialize narrow-band modes */
    SubMode[] nbSubModes = new SubMode[NB_SUBMODES];
    /* 2150 bps "vocoder-like" mode for comfort noise */
    nbSubModes[1] = new SubMode(0, 1, 0, 0, lbrLspQuant, ltpFP, noiseSearch, .7f, .7f, -1, 43);
    /* 5.95 kbps very low bit-rate mode */
    nbSubModes[2] = new SubMode(0, 0, 0, 0, lbrLspQuant, ltpVlbr, ssNbVlbrSearch, 0.7f, 0.5f, .55f, 119);
    /* 8 kbps low bit-rate mode */
    nbSubModes[3] = new SubMode(-1, 0, 1, 0, lbrLspQuant, ltpLbr, ssNbLbrSearch, 0.7f, 0.55f, .45f, 160);
    /* 11 kbps medium bit-rate mode */
    nbSubModes[4] = new SubMode(-1, 0, 1, 0, lbrLspQuant, ltpMed, ssNbMedSearch, 0.7f, 0.63f, .35f, 220);
    /* 15 kbps high bit-rate mode */
    nbSubModes[5] = new SubMode(-1, 0, 3, 0, nbLspQuant, ltpNb, ssNbSearch, 0.7f, 0.65f, .25f, 300);
    /* 18.2 high bit-rate mode */
    nbSubModes[6] = new SubMode(-1, 0, 3, 0, nbLspQuant, ltpNb, ssSbSearch, 0.68f, 0.65f, .1f, 364);
    /* 24.6 kbps high bit-rate mode */
    nbSubModes[7] = new SubMode(-1, 0, 3, 1, nbLspQuant, ltpNb, ssNbSearch, 0.65f, 0.65f, -1, 492);
    /* 3.95 kbps very low bit-rate mode */
    nbSubModes[8] = new SubMode(0, 1, 0, 0, lbrLspQuant, ltpFP, ssNbUlbrSearch, .7f, .5f, .65f, 79);
    /* Return the Narrowband SubModes*/
    return nbSubModes;
  }

  /**
   * Returns the size of a frame (ex: 160 samples for a narrowband frame,
   * 320 for wideband and 640 for ultra-wideband).
   * @return the size of a frame (number of audio samples in a frame).
   */
  public int  getFrameSize()
  {
    return frameSize;
  }

  /**
   * Returns whether or not we are using Discontinuous Transmission encoding.
   * @return whether or not we are using Discontinuous Transmission encoding.
   */
  public boolean getDtx()
  {
    return dtx_enabled != 0;
  }

  /**
   * Returns the Pitch Gain array.
   * @return the Pitch Gain array.
   */
  public float[] getPiGain()
  {
    return pi_gain;
  }
  
  /**
   * Returns the excitation array.
   * @return the excitation array.
   */
  public float[] getExc()
  {
    float[] excTmp = new float[frameSize];
    System.arraycopy(excBuf, excIdx, excTmp, 0, frameSize);
    return excTmp;
  }
  
  /**
   * Returns the innovation array.
   * @return the innovation array.
   */
  public float[] getInnov()
  {
    return innov;
  }
}

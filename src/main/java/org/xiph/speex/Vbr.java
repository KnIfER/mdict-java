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
 * Class: Vbr.java                                                            *
 *                                                                            *
 * Author: Marc GIMPEL                                                        *
 * Based on code by: Jean-Marc VALIN                                          *
 *                                                                            *
 * Date: 19th April 2003                                                      *
 *                                                                            *
 ******************************************************************************/

/* $Id: Vbr.java,v 1.2 2004/10/21 16:21:57 mgimpel Exp $ */

/* Copyright (C) 2002 Jean-Marc Valin 
   File: vbr.c

   VBR-related routines

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
 * This class analyses the signal to help determine what bitrate to use when
 * the Varible BitRate option has been selected.
 * 
 * @author Marc Gimpel, Wimba S.A. (mgimpel@horizonwimba.com)
 * @version $Revision: 1.2 $
 */
public class Vbr
{
  /** */
  public static final int   VBR_MEMORY_SIZE = 5;
  /** */
  public static final int   MIN_ENERGY = 6000;
  /** */
  public static final float NOISE_POW  = 0.3f;

  /**
   * Narrowband threshhold table.
   */
  public static final float[][] nb_thresh = { //[9][11]
    {-1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f}, /*   CNG   */
    { 3.5f,  2.5f,  2.0f,  1.2f,  0.5f,  0.0f, -0.5f, -0.7f, -0.8f, -0.9f, -1.0f}, /*  2 kbps */
    {10.0f,  6.5f,  5.2f,  4.5f,  3.9f,  3.5f,  3.0f,  2.5f,  2.3f,  1.8f,  1.0f}, /*  6 kbps */
    {11.0f,  8.8f,  7.5f,  6.5f,  5.0f,  3.9f,  3.9f,  3.9f,  3.5f,  3.0f,  1.0f}, /*  8 kbps */
    {11.0f, 11.0f,  9.9f,  9.0f,  8.0f,  7.0f,  6.5f,  6.0f,  5.0f,  4.0f,  2.0f}, /* 11 kbps */
    {11.0f, 11.0f, 11.0f, 11.0f,  9.5f,  9.0f,  8.0f,  7.0f,  6.5f,  5.0f,  3.0f}, /* 15 kbps */
    {11.0f, 11.0f, 11.0f, 11.0f, 11.0f, 11.0f,  9.5f,  8.5f,  8.0f,  6.5f,  4.0f}, /* 18 kbps */
    {11.0f, 11.0f, 11.0f, 11.0f, 11.0f, 11.0f, 11.0f, 11.0f,  9.8f,  7.5f,  5.5f}, /* 24 kbps */ 
    { 8.0f,  5.0f,  3.7f,  3.0f,  2.5f,  2.0f,  1.8f,  1.5f,  1.0f,  0.0f,  0.0f}  /*  4 kbps */
  };

  /**
   * Wideband threshhold table.
   */
  public static final float[][] hb_thresh = { //[5][11]
    {-1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f}, /* silence */
    {-1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f}, /*  2 kbps */
    {11.0f, 11.0f,  9.5f,  8.5f,  7.5f,  6.0f,  5.0f,  3.9f,  3.0f,  2.0f,  1.0f}, /*  6 kbps */
    {11.0f, 11.0f, 11.0f, 11.0f, 11.0f,  9.5f,  8.7f,  7.8f,  7.0f,  6.5f,  4.0f}, /* 10 kbps */
    {11.0f, 11.0f, 11.0f, 11.0f, 11.0f, 11.0f, 11.0f, 11.0f,  9.8f,  7.5f,  5.5f}  /* 18 kbps */ 
  };

  /**
   * Ultra-wideband threshhold table.
   */
  public static final float[][] uhb_thresh = { // [2][11]
    {-1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f}, /* silence */
    { 3.9f,  2.5f,  0.0f,  0.0f,  0.0f,  0.0f,  0.0f,  0.0f,  0.0f,  0.0f, -1.0f}  /*  2 kbps */
  };

  private float energy_alpha;
  private float average_energy;
  private float last_energy;
  private float[] last_log_energy;
  private float accum_sum;
  private float last_pitch_coef;
  private float soft_pitch;
  private float last_quality;
  private float noise_level;
  private float noise_accum;
  private float noise_accum_count;
  private int   consec_noise;

  /**
   * Constructor
   */
  public Vbr()
  {
    average_energy  = 0;
    last_energy     = 1;
    accum_sum       = 0;
    energy_alpha    = .1f;
    soft_pitch      = 0;
    last_pitch_coef = 0;
    last_quality    = 0;

    noise_accum       = (float) (.05*Math.pow(MIN_ENERGY, NOISE_POW));
    noise_accum_count = .05f;
    noise_level       = noise_accum/noise_accum_count;
    consec_noise      = 0;

    last_log_energy = new float[VBR_MEMORY_SIZE];
    for (int i=0; i<VBR_MEMORY_SIZE; i++)
      last_log_energy[i] = (float) Math.log(MIN_ENERGY);
  }

  /**
   * This function should analyse the signal and decide how critical the
   * coding error will be perceptually. The following factors should be
   * taken into account:
   * <ul>
   * <li>Attacks (positive energy derivative) should be coded with more bits
   * <li>Stationary voiced segments should receive more bits
   * <li>Segments with (very) low absolute energy should receive less bits
   *     (maybe only shaped noise?)
   * <li>DTX for near-zero energy?
   * <li>Stationary fricative segments should have less bits
   * <li>Temporal masking: when energy slope is decreasing, decrease the bit-rate
   * <li>Decrease bit-rate for males (low pitch)?
   * <li>(wideband only) less bits in the high-band when signal is very 
   *     non-stationary (harder to notice high-frequency noise)???
   * </ul>
   * @param sig - signal.
   * @param len - signal length.
   * @param pitch - signal pitch.
   * @param pitch_coef - pitch coefficient.
   * @return quality
   */
  public float analysis(final float[] sig,
                        final int len,
                        final int pitch,
                        final float pitch_coef)
  {
    int i;
    float ener=0, ener1=0, ener2=0;
    float qual=7;
    int va;
    float log_energy;
    float non_st=0;
    float voicing;
    float pow_ener;

    for (i=0; i<len>>1; i++)
      ener1 += sig[i]*sig[i];
    for (i=len>>1; i<len; i++)
      ener2 += sig[i]*sig[i];
    ener=ener1+ener2;

    log_energy = (float) Math.log(ener+MIN_ENERGY);
    for (i=0; i<VBR_MEMORY_SIZE; i++)
      non_st += (log_energy-last_log_energy[i])*(log_energy-last_log_energy[i]);
    non_st = non_st/(30*VBR_MEMORY_SIZE);
    if (non_st>1)
      non_st=1;

    voicing = 3*(pitch_coef-.4f)*Math.abs(pitch_coef-.4f);
    average_energy = (1-energy_alpha)*average_energy + energy_alpha*ener;
    noise_level=noise_accum/noise_accum_count;
    pow_ener = (float) Math.pow(ener,NOISE_POW);
    if (noise_accum_count<.06f && ener>MIN_ENERGY)
      noise_accum = .05f*pow_ener;

    if ((voicing<.3f && non_st < .2f && pow_ener < 1.2f*noise_level)
       || (voicing<.3f && non_st < .05f && pow_ener < 1.5f*noise_level)
       || (voicing<.4f && non_st < .05f && pow_ener < 1.2f*noise_level)
       || (voicing<0 && non_st < .05f))
    {
      float tmp;
      va = 0;
      consec_noise++;
      if (pow_ener > 3*noise_level)
        tmp = 3*noise_level;
      else 
        tmp = pow_ener;
      if (consec_noise>=4) {
         noise_accum = .95f*noise_accum + .05f*tmp;
         noise_accum_count = .95f*noise_accum_count + .05f;
      }
    } else {
      va = 1;
      consec_noise=0;
    }

    if (pow_ener < noise_level && ener>MIN_ENERGY) {
      noise_accum = .95f*noise_accum + .05f*pow_ener;
      noise_accum_count = .95f*noise_accum_count + .05f;
    }

    /* Checking for very low absolute energy */
    if (ener < 30000)
    {
      qual -= .7f;
      if (ener < 10000)
        qual-=.7f;
      if (ener < 3000)
        qual-=.7f;
    } else {
      float short_diff, long_diff;
      short_diff = (float) Math.log((ener+1)/(1+last_energy));
      long_diff = (float) Math.log((ener+1)/(1+average_energy));
      /*fprintf (stderr, "%f %f\n", short_diff, long_diff);*/

      if (long_diff<-5)
        long_diff=-5;
      if (long_diff>2)
        long_diff=2;

      if (long_diff>0)
        qual += .6f*long_diff;
      if (long_diff<0)
        qual += .5f*long_diff;
      if (short_diff>0)
      {
        if (short_diff>5)
          short_diff=5;
        qual += .5f*short_diff;
      }
      /* Checking for energy increases */
      if (ener2 > 1.6f*ener1)
         qual += .5f;
    }
    last_energy = ener;
    soft_pitch = .6f*soft_pitch + .4f*pitch_coef;
    qual += 2.2f*((pitch_coef-.4) + (soft_pitch-.4));

    if (qual < last_quality)
      qual = .5f*qual + .5f*last_quality;
    if (qual<4)
      qual=4;
    if (qual>10)
      qual=10;
   
    /*
    if (consec_noise>=2)
       qual-=1.3f;
    if (consec_noise>=5)
      qual-=1.3f;
    if (consec_noise>=12)
       qual-=1.3f;
    */
    if (consec_noise>=3)
      qual=4;

    if (consec_noise != 0)
      qual -= (float)(1.0 * (Math.log(3.0 + consec_noise)-Math.log(3)));
    if (qual<0)
      qual=0;
   
    if (ener<60000)
    {
      if (consec_noise>2)
        qual-=(float)(0.5*(Math.log(3.0 + consec_noise)-Math.log(3)));
      if (ener<10000&&consec_noise>2)
        qual-=(float)(0.5*(Math.log(3.0 + consec_noise)-Math.log(3)));
      if (qual<0)
        qual=0;
      qual += (float)(.3*Math.log(ener/60000.0));
    }
    if (qual<-1)
      qual=-1;

    last_pitch_coef = pitch_coef;
    last_quality = qual;

    for (i=VBR_MEMORY_SIZE-1; i>0; i--)
      last_log_energy[i] = last_log_energy[i-1];
    last_log_energy[0] = log_energy;

    return qual;
  }
}

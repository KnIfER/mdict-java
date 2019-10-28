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
 * Class: NbDecoder.java                                                      *
 *                                                                            *
 * Author: James LAWRENCE                                                     *
 * Modified by: Marc GIMPEL                                                   *
 * Based on code by: Jean-Marc VALIN                                          *
 *                                                                            *
 * Date: March 2003                                                           *
 *                                                                            *
 ******************************************************************************/

/* $Id: NbDecoder.java,v 1.3 2005/05/27 13:16:27 mgimpel Exp $ */

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
import java.util.Random;

/**
 * Narrowband Speex Decoder
 * 
 * @author Jim Lawrence, helloNetwork.com
 * @author Marc Gimpel, Wimba S.A. (mgimpel@horizonwimba.com)
 * @version $Revision: 1.3 $
 */
public class NbDecoder
  extends NbCodec
  implements Decoder
{
  private float[]  innov2;
  /*Packet loss*/
  private int     count_lost;
  private int     last_pitch;         /** Pitch of last correctly decoded frame */
  private float   last_pitch_gain;    /** Pitch gain of last correctly decoded frame */
  private float[] pitch_gain_buf;     /** Pitch gain of last decoded frames */
  private int     pitch_gain_buf_idx; /** Tail of the buffer */
  private float   last_ol_gain;       /** Open-loop gain for previous frame */

  /** */
  protected Random random = new Random();
  /** */
  protected Stereo  stereo;
  /** */
  protected Inband  inband;
  /** */
  protected boolean enhanced;

  /**
   * Constructor
   */
  public NbDecoder()
  {
    stereo   = new Stereo();
    inband   = new Inband(stereo);
    enhanced = true;
  }

  /**
   * Initialise
   * @param frameSize
   * @param subframeSize
   * @param lpcSize
   * @param bufSize
   */
  public void init(final int frameSize,
                   final int subframeSize,
                   final int lpcSize,
                   final int bufSize)
  {
    super.init(frameSize, subframeSize, lpcSize, bufSize);
    filters.init ();
    innov2 =  new float[40];

    count_lost         = 0;
    last_pitch         = 40;
    last_pitch_gain    = 0;
    pitch_gain_buf     = new float[3];
    pitch_gain_buf_idx = 0;
    last_ol_gain       = 0;
  }

  /**
   * Decode the given input bits.
   * @param bits - Speex bits buffer.
   * @param out - the decoded mono audio frame.
   * @return 1 if a terminator was found, 0 if not.
   * @throws StreamCorruptedException If there is an error detected in the
   * data stream.
   */
  public int decode(final Bits bits, final float[] out)
    throws StreamCorruptedException
  {
    int i, sub, pitch, ol_pitch=0, m;
    float[] pitch_gain = new float[3];
    float ol_gain=0.0f, ol_pitch_coef=0.0f;
    int best_pitch=40;
    float best_pitch_gain=0;
    float pitch_average=0;
    
    if (bits == null && dtx_enabled!=0) {
      submodeID = 0;
    }
    else {
      /* If bits is NULL, consider the packet to be lost (what could we do anyway) */
      if (bits == null) {
        decodeLost(out);
        return 0;
      }
      /* Search for next narrowband block (handle requests, skip wideband blocks) */
      do {
        if (bits.unpack(1)!=0) { /* Skip wideband block (for compatibility) */
//Wideband
          /* Get the sub-mode that was used */
          m = bits.unpack(SbCodec.SB_SUBMODE_BITS);
          int advance = SbCodec.SB_FRAME_SIZE[m];
          if (advance < 0) {
            throw new StreamCorruptedException("Invalid sideband mode encountered (1st sideband): " + m);
            //return -2;
          } 
          advance -= (SbCodec.SB_SUBMODE_BITS+1);
          bits.advance(advance);
          if (bits.unpack(1)!=0) { /* Skip ultra-wideband block (for compatibility) */
            /* Get the sub-mode that was used */
            m = bits.unpack(SbCodec.SB_SUBMODE_BITS);
            advance = SbCodec.SB_FRAME_SIZE[m];
            if (advance < 0) {
              throw new StreamCorruptedException("Invalid sideband mode encountered. (2nd sideband): " + m);
              //return -2;
            } 
            advance -= (SbCodec.SB_SUBMODE_BITS+1);
            bits.advance(advance);
            if (bits.unpack(1)!=0) { /* Sanity check */
              throw new StreamCorruptedException("More than two sideband layers found");
              //return -2;
            }
          }
//*/
        }

        /* Get the sub-mode that was used */
        m = bits.unpack(NB_SUBMODE_BITS);
        if (m==15) { /* We found a terminator */
          return 1;
        }
        else if (m==14) { /* Speex in-band request */
          inband.speexInbandRequest(bits);
        }
        else if (m==13) { /* User in-band request */
          inband.userInbandRequest(bits);
        }
        else if (m>8) { /* Invalid mode */
          throw new StreamCorruptedException("Invalid mode encountered: " + m);
          //return -2;
        }
      }
      while (m>8);
      submodeID = m;
    }

    /* Shift all buffers by one frame */
    System.arraycopy(frmBuf, frameSize, frmBuf, 0, bufSize-frameSize);
    System.arraycopy(excBuf, frameSize, excBuf, 0, bufSize-frameSize);

    /* If null mode (no transmission), just set a couple things to zero*/
    if (submodes[submodeID] == null) {
      Filters.bw_lpc(.93f, interp_qlpc, lpc, 10);

      float innov_gain=0;
      for (i=0;i<frameSize;i++)
        innov_gain += innov[i]*innov[i];
      innov_gain=(float)Math.sqrt(innov_gain/frameSize);
      for (i=excIdx;i<excIdx+frameSize;i++) {
        excBuf[i]=3*innov_gain*(random.nextFloat() - .5f);
      }
      first=1;

      /* Final signal synthesis from excitation */
      Filters.iir_mem2(excBuf, excIdx, lpc, frmBuf, frmIdx, frameSize, lpcSize, mem_sp);

      out[0] = frmBuf[frmIdx] + preemph*pre_mem;
      for (i=1;i<frameSize;i++)
        out[i]=frmBuf[frmIdx+i] + preemph*out[i-1];
      pre_mem=out[frameSize-1];
      count_lost=0;
      return 0;
    }

    /* Unquantize LSPs */
    submodes[submodeID].lsqQuant.unquant(qlsp, lpcSize, bits);

    /*Damp memory if a frame was lost and the LSP changed too much*/
    if (count_lost != 0)
    {
      float lsp_dist=0, fact;
      for (i=0;i<lpcSize;i++)
        lsp_dist += Math.abs(old_qlsp[i] - qlsp[i]);
      fact = (float) (.6*Math.exp(-.2*lsp_dist));
      for (i=0;i<2*lpcSize;i++)
        mem_sp[i] *= fact;
    }

    /* Handle first frame and lost-packet case */
    if (first!=0 || count_lost != 0) {
      for (i=0;i<lpcSize;i++)
        old_qlsp[i] = qlsp[i];
    }

    /* Get open-loop pitch estimation for low bit-rate pitch coding */
    if (submodes[submodeID].lbr_pitch!=-1) {
      ol_pitch = min_pitch + bits.unpack(7);
    }  

    if (submodes[submodeID].forced_pitch_gain!=0) {
      int quant= bits.unpack(4);
      ol_pitch_coef=0.066667f*quant;
    }

    /* Get global excitation gain */
    int qe  = bits.unpack(5);
    ol_gain = (float)Math.exp(qe/3.5);

    /* unpacks unused dtx bits */
    if (submodeID==1) {
      int extra = bits.unpack(4);
      if (extra == 15)
        dtx_enabled=1;
      else
        dtx_enabled=0;
    }
    if (submodeID>1)
      dtx_enabled=0;

    /*Loop on subframes */
    for (sub=0;sub<nbSubframes;sub++) {
      int offset, spIdx, extIdx;
      float tmp;
      /* Offset relative to start of frame */
      offset = subframeSize*sub;
      /* Original signal */
      spIdx  = frmIdx + offset;
      /* Excitation */
      extIdx = excIdx+offset;

      /* LSP interpolation (quantized and unquantized) */
      tmp = (1.0f + sub)/nbSubframes;
      for (i=0;i<lpcSize;i++)
        interp_qlsp[i] = (1-tmp)*old_qlsp[i] + tmp*qlsp[i];

      /* Make sure the LSP's are stable */
      Lsp.enforce_margin(interp_qlsp, lpcSize, .002f);

      /* Compute interpolated LPCs (unquantized) */
      for (i=0;i<lpcSize;i++)
        interp_qlsp[i] = (float)Math.cos(interp_qlsp[i]);
      m_lsp.lsp2lpc(interp_qlsp, interp_qlpc, lpcSize);

      /* Compute enhanced synthesis filter */
      if (enhanced) {
        float r=.9f;   
        float k1,k2,k3;
        
        k1=submodes[submodeID].lpc_enh_k1;
        k2=submodes[submodeID].lpc_enh_k2;
        k3=(1-(1-r*k1)/(1-r*k2))/r;
        Filters.bw_lpc(k1, interp_qlpc, awk1, lpcSize);
        Filters.bw_lpc(k2, interp_qlpc, awk2, lpcSize);
        Filters.bw_lpc(k3, interp_qlpc, awk3, lpcSize);
      }
      
      /* Compute analysis filter at w=pi */
      tmp=1;
      pi_gain[sub]=0;
      for (i=0;i<=lpcSize;i++) {
        pi_gain[sub] += tmp*interp_qlpc[i];
        tmp = -tmp;
      }

      /* Reset excitation */
      for (i=0;i<subframeSize;i++)
        excBuf[extIdx+i]=0;

      /*Adaptive codebook contribution*/
      int pit_min, pit_max;
      
      /* Handle pitch constraints if any */
      if (submodes[submodeID].lbr_pitch != -1) {
        int margin= submodes[submodeID].lbr_pitch;
        if (margin!=0) {
          pit_min = ol_pitch-margin+1;
          if (pit_min < min_pitch)
            pit_min = min_pitch;
          pit_max = ol_pitch+margin;
          if (pit_max > max_pitch)
            pit_max = max_pitch;
        } 
        else {
          pit_min = pit_max = ol_pitch;
        }
      } 
      else {
        pit_min = min_pitch;
        pit_max = max_pitch;
      }

      /* Pitch synthesis */
      pitch = submodes[submodeID].ltp.unquant(excBuf, extIdx, pit_min, ol_pitch_coef,  
                                              subframeSize, pitch_gain, bits,
                                              count_lost, offset, last_pitch_gain);

      /* If we had lost frames, check energy of last received frame */
      if (count_lost != 0 && ol_gain < last_ol_gain) {
        float fact = ol_gain/(last_ol_gain+1);
        for (i=0;i<subframeSize;i++)
          excBuf[excIdx+i]*=fact;
      }

      tmp = Math.abs(pitch_gain[0]+pitch_gain[1]+pitch_gain[2]);
      tmp = Math.abs(pitch_gain[1]);
      if (pitch_gain[0]>0)
        tmp += pitch_gain[0];
      else
        tmp -= .5*pitch_gain[0];
      if (pitch_gain[2]>0)
        tmp += pitch_gain[2];
      else
        tmp -= .5*pitch_gain[0];

      pitch_average += tmp;
      if (tmp>best_pitch_gain)
      {
        best_pitch = pitch;
        best_pitch_gain = tmp;
      }
      
      /* Unquantize the innovation */
      int q_energy, ivi=sub*subframeSize;
      float ener;
      
      for (i=ivi;i<ivi+subframeSize;i++)
        innov[i]=0.0f;

      /* Decode sub-frame gain correction */
      if (submodes[submodeID].have_subframe_gain==3) {
        q_energy = bits.unpack(3);
        ener     = (float) (ol_gain*Math.exp(exc_gain_quant_scal3[q_energy]));
      } 
      else if (submodes[submodeID].have_subframe_gain==1) {
        q_energy = bits.unpack(1);
        ener     = (float) (ol_gain*Math.exp(exc_gain_quant_scal1[q_energy]));
      } 
      else {
        ener     = ol_gain;
      }
      
      if (submodes[submodeID].innovation!=null) {
        /* Fixed codebook contribution */
        submodes[submodeID].innovation.unquant(innov, ivi, subframeSize, bits);
      } 

      /* De-normalize innovation and update excitation */
      for (i=ivi;i<ivi+subframeSize;i++)
        innov[i]*=ener;

      /*  Vocoder mode */
      if (submodeID==1) {
        float g=ol_pitch_coef;
        
        for (i=0;i<subframeSize;i++)
          excBuf[extIdx+i]=0;
        while (voc_offset<subframeSize) {
          if (voc_offset>=0)
            excBuf[extIdx+voc_offset]=(float)Math.sqrt(1.0f*ol_pitch);
          voc_offset+=ol_pitch;
        }
        voc_offset -= subframeSize;

        g=.5f+2*(g-.6f);
        if (g<0)
          g=0;
        if (g>1)
          g=1;
        for (i=0;i<subframeSize;i++)
        {
          float itmp=excBuf[extIdx+i];
          excBuf[extIdx+i]=.8f*g*excBuf[extIdx+i]*ol_gain + .6f*g*voc_m1*ol_gain + .5f*g*innov[ivi+i] - .5f*g*voc_m2 + (1-g)*innov[ivi+i];
          voc_m1 = itmp;
          voc_m2 = innov[ivi+i];
          voc_mean = .95f*voc_mean + .05f*excBuf[extIdx+i];
          excBuf[extIdx+i]-=voc_mean;
        }
      } 
      else {
        for (i=0;i<subframeSize;i++)
          excBuf[extIdx+i]+=innov[ivi+i];
      }
      
      /* Decode second codebook (only for some modes) */
      if (submodes[submodeID].double_codebook!=0) {
        for (i=0;i<subframeSize;i++)
          innov2[i]=0;
        submodes[submodeID].innovation.unquant(innov2, 0, subframeSize, bits);
        for (i=0;i<subframeSize;i++)
          innov2[i]*=ener*(1/2.2);
        for (i=0;i<subframeSize;i++)
          excBuf[extIdx+i] += innov2[i];
      }
      
      for (i=0;i<subframeSize;i++)
        frmBuf[spIdx+i]=excBuf[extIdx+i];

      /* Signal synthesis */
      if (enhanced && submodes[submodeID].comb_gain>0) {
        filters.comb_filter(excBuf, extIdx, frmBuf, spIdx, subframeSize,
                            pitch, pitch_gain, submodes[submodeID].comb_gain);
      }

      if (enhanced) {
        /* Use enhanced LPC filter */
        Filters.filter_mem2(frmBuf, spIdx, awk2, awk1, subframeSize, lpcSize, mem_sp, lpcSize);
        Filters.filter_mem2(frmBuf, spIdx, awk3, interp_qlpc, subframeSize, lpcSize, mem_sp, 0);
      }
      else {
        /* Use regular filter */
        for (i=0;i<lpcSize;i++)
          mem_sp[lpcSize+i] = 0;
        Filters.iir_mem2(frmBuf, spIdx, interp_qlpc, frmBuf, spIdx, subframeSize, lpcSize, mem_sp);
      }
    }
    
    /*Copy output signal*/
    out[0] = frmBuf[frmIdx] + preemph * pre_mem;
    for (i=1;i<frameSize;i++)
      out[i] = frmBuf[frmIdx+i] + preemph * out[i-1];
    pre_mem = out[frameSize-1];
    
    /* Store the LSPs for interpolation in the next frame */
    for (i=0;i<lpcSize;i++)
      old_qlsp[i] = qlsp[i];

    /* The next frame will not be the first (Duh!) */
    first = 0;
    count_lost=0;
    last_pitch = best_pitch;
    last_pitch_gain = .25f*pitch_average;
    pitch_gain_buf[pitch_gain_buf_idx++] = last_pitch_gain;
    if (pitch_gain_buf_idx > 2) /* rollover */
      pitch_gain_buf_idx = 0;
    last_ol_gain = ol_gain;
    
    return 0;
  }

  /**
   * Decode when packets are lost.
   * @param out - the generated mono audio frame.
   * @return 0 if successful.
   */
  public int decodeLost(final float[] out)
  {
    int i;
    float pitch_gain, fact, gain_med;

    fact = (float) Math.exp(-.04*count_lost*count_lost);
    // median3(a, b, c) = (a<b ? (b<c ? b : (a<c ? c : a))
    //                         : (c<b ? b : (c<a ? c : a)))
    gain_med = (pitch_gain_buf[0] < pitch_gain_buf[1] ? (pitch_gain_buf[1] < pitch_gain_buf[2] ? pitch_gain_buf[1] : (pitch_gain_buf[0] < pitch_gain_buf[2] ? pitch_gain_buf[2] : pitch_gain_buf[0]))
                                                      : (pitch_gain_buf[2] < pitch_gain_buf[1] ? pitch_gain_buf[1] : (pitch_gain_buf[2] < pitch_gain_buf[0] ? pitch_gain_buf[2] : pitch_gain_buf[0])));
    if (gain_med < last_pitch_gain)
      last_pitch_gain = gain_med;

    pitch_gain = last_pitch_gain;
    if (pitch_gain>.95f)
      pitch_gain=.95f;

    pitch_gain *= fact;
   
    /* Shift all buffers by one frame */
    System.arraycopy(frmBuf, frameSize, frmBuf, 0, bufSize-frameSize);
    System.arraycopy(excBuf, frameSize, excBuf, 0, bufSize-frameSize);

    for (int sub=0; sub<nbSubframes; sub++)
    {
      int offset;
      int spIdx, extIdx;
      /* Offset relative to start of frame */
      offset = subframeSize*sub;
      /* Original signal */
      spIdx  = frmIdx+offset;
      /* Excitation */
      extIdx = excIdx+offset;
      /* Excitation after post-filter*/

      /* Calculate perceptually enhanced LPC filter */
      if (enhanced) {
        float r=.9f;   
        float k1,k2,k3;
        if (submodes[submodeID] != null) {
          k1=submodes[submodeID].lpc_enh_k1;
          k2=submodes[submodeID].lpc_enh_k2;
        }
        else {
          k1 = k2 = 0.7f;
        }
        k3=(1-(1-r*k1)/(1-r*k2))/r;
        Filters.bw_lpc(k1, interp_qlpc, awk1, lpcSize);
        Filters.bw_lpc(k2, interp_qlpc, awk2, lpcSize);
        Filters.bw_lpc(k3, interp_qlpc, awk3, lpcSize);
      }
      /* Make up a plausible excitation */
      /* THIS CAN BE IMPROVED */
      /*if (pitch_gain>.95)
        pitch_gain=.95;*/
      {
        float innov_gain=0;
        for (i=0; i<frameSize; i++)
          innov_gain += innov[i]*innov[i];
        innov_gain = (float) Math.sqrt(innov_gain/frameSize);
        for (i=0; i<subframeSize; i++)
        {
//#if 0
//          excBuf[extIdx+i] = pitch_gain*excBuf[extIdx+i-last_pitch] + fact*((float)Math.sqrt(1-pitch_gain))*innov[i+offset];
//          /*Just so it give the same lost packets as with if 0*/
//          /*rand();*/
//#else
          /*excBuf[extIdx+i] = pitch_gain*excBuf[extIdx+i-last_pitch] + fact*innov[i+offset];*/
          excBuf[extIdx+i] = pitch_gain*excBuf[extIdx+i-last_pitch] + fact*((float)Math.sqrt(1-pitch_gain))*3*innov_gain*((random.nextFloat())-0.5f);
//#endif
        }
      }
      for (i=0;i<subframeSize;i++)
        frmBuf[spIdx+i]=excBuf[extIdx+i];

      /* Signal synthesis */
      if (enhanced) {
        /* Use enhanced LPC filter */
        Filters.filter_mem2(frmBuf, spIdx, awk2, awk1, subframeSize, lpcSize, mem_sp, lpcSize);
        Filters.filter_mem2(frmBuf, spIdx, awk3, interp_qlpc, subframeSize, lpcSize, mem_sp, 0);
      }
      else {
        /* Use regular filter */
        for (i=0;i<lpcSize;i++)
          mem_sp[lpcSize+i] = 0;
        Filters.iir_mem2(frmBuf, spIdx, interp_qlpc, frmBuf, spIdx, subframeSize, lpcSize, mem_sp);
      }
    }

    out[0] = frmBuf[0] + preemph*pre_mem;
    for (i=1;i<frameSize;i++)
      out[i] = frmBuf[i] + preemph*out[i-1];
    pre_mem=out[frameSize-1];
    first = 0;
    count_lost++;
    pitch_gain_buf[pitch_gain_buf_idx++] = pitch_gain;
    if (pitch_gain_buf_idx > 2) /* rollover */
      pitch_gain_buf_idx = 0;

    return 0;
  }

  /**
   * Decode the given bits to stereo.
   * @param data - float array of size 2*frameSize, that contains the mono
   * audio samples in the first half. When the function has completed, the
   * array will contain the interlaced stereo audio samples.
   * @param frameSize - the size of a frame of mono audio samples.
   */
  public void decodeStereo(final float[] data, final int frameSize)
  {
    stereo.decode(data, frameSize);
  }

  /**
   * Enables or disables perceptual enhancement.
   * @param enhanced
   */
  public void setPerceptualEnhancement(final boolean enhanced)
  {
    this.enhanced = enhanced;
  }
  
  /**
   * Returns whether perceptual enhancement is enabled or disabled.
   * @return whether perceptual enhancement is enabled or disabled.
   */
  public boolean getPerceptualEnhancement()
  {
    return enhanced;
  }
}

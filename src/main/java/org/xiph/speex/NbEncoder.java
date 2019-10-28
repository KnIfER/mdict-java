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
 * Class: NbEncoder.java                                                      *
 *                                                                            *
 * Author: Marc GIMPEL                                                        *
 * Based on code by: Jean-Marc VALIN                                          *
 *                                                                            *
 * Date: 9th April 2003                                                       *
 *                                                                            *
 ******************************************************************************/

/* $Id: NbEncoder.java,v 1.2 2004/10/21 16:21:57 mgimpel Exp $ */

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
 * Narrowband Speex Encoder
 * 
 * @author Marc Gimpel, Wimba S.A. (mgimpel@horizonwimba.com)
 * @version $Revision: 1.2 $
 */
public class NbEncoder
  extends NbCodec
  implements Encoder
{
  /** The Narrowband Quality map indicates which narrowband submode to use for the given narrowband quality setting */
  public static final int[] NB_QUALITY_MAP = {1, 8, 2, 3, 3, 4, 4, 5, 5, 6, 7};
  
  private int     bounded_pitch; /** Next frame should not rely on previous frames for pitch */
  private int[]   pitch;         /** */
  private float   pre_mem2;      /** 1-element memory for pre-emphasis */
  private float[] exc2Buf;       /** "Pitch enhanced" excitation */
  private int     exc2Idx;       /** "Pitch enhanced" excitation */
  private float[] swBuf;         /** Weighted signal buffer */
  private int     swIdx;         /** Start of weighted signal frame */
  private float[] window;       /** Temporary (Hanning) window */
  private float[] buf2;         /** 2nd temporary buffer */
  private float[] autocorr;     /** auto-correlation */
  private float[] lagWindow;    /** Window applied to auto-correlation */
  private float[] lsp;          /** LSPs for current frame */
  private float[] old_lsp;      /** LSPs for previous frame */
  private float[] interp_lsp;   /** Interpolated LSPs */
  private float[] interp_lpc;   /** Interpolated LPCs */
  private float[] bw_lpc1;      /** LPCs after bandwidth expansion by gamma1 for perceptual weighting*/
  private float[] bw_lpc2;      /** LPCs after bandwidth expansion by gamma2 for perceptual weighting*/
  private float[] rc;           /** Reflection coefficients */
  private float[] mem_sw;       /** Filter memory for perceptually-weighted signal */
  private float[] mem_sw_whole; /** Filter memory for perceptually-weighted signal (whole frame)*/
  private float[] mem_exc;      /** Filter memory for excitation (whole frame) */

  private Vbr    vbr;            /** State of the VBR data */
  private int    dtx_count;      /** Number of consecutive DTX frames */

  private float[]  innov2;

  protected int complexity;     /** Complexity setting (0-10 from least complex to most complex) */
  protected int vbr_enabled;    /** 1 for enabling VBR, 0 otherwise */
  protected int vad_enabled;    /** 1 for enabling VAD, 0 otherwise */
  protected int abr_enabled;    /** ABR setting (in bps), 0 if off */
  protected float vbr_quality;      /** Quality setting for VBR encoding */
  protected float relative_quality; /** Relative quality that will be needed by VBR */
  protected float abr_drift;
  protected float abr_drift2;
  protected float abr_count;
  protected int sampling_rate;

  protected int     submodeSelect;  /** Mode chosen by the user (may differ from submodeID if VAD is on) */

  /**
   * Initialisation
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

    complexity  = 3; // in C it's 2 here, but set to 3 automatically by the encoder
    vbr_enabled = 0; // disabled by default
    vad_enabled = 0; // disabled by default
    abr_enabled = 0; // disabled by default
    vbr_quality = 8;

    submodeSelect = 5;
    pre_mem2      = 0;
    bounded_pitch = 1;

    exc2Buf  = new float[bufSize];
    exc2Idx  = bufSize - windowSize;
    swBuf    = new float[bufSize];
    swIdx    = bufSize - windowSize;

    window = Misc.window(windowSize, subframeSize);
    lagWindow = Misc.lagWindow(lpcSize, lag_factor);

    autocorr = new float[lpcSize+1];
    buf2     = new float[windowSize];

    interp_lpc  = new float[lpcSize+1];
    interp_qlpc = new float[lpcSize+1];
    bw_lpc1     = new float[lpcSize+1];
    bw_lpc2     = new float[lpcSize+1];
    lsp         = new float[lpcSize];
    qlsp        = new float[lpcSize];
    old_lsp     = new float[lpcSize];
    old_qlsp    = new float[lpcSize];
    interp_lsp  = new float[lpcSize];
    interp_qlsp = new float[lpcSize];

    rc           = new float[lpcSize];
    mem_sp       = new float[lpcSize]; // why was there a *5 before ?!?
    mem_sw       = new float[lpcSize];
    mem_sw_whole = new float[lpcSize];
    mem_exc      = new float[lpcSize];

    vbr = new Vbr();
    dtx_count   = 0;
    abr_count   = 0;
    sampling_rate = 8000;

    awk1   =  new float[lpcSize+1];
    awk2   =  new float[lpcSize+1];
    awk3   =  new float[lpcSize+1];
    innov2 =  new float[40];

    filters.init ();

    pitch = new int[nbSubframes];
  }

  /**
   * Encode the given input signal.
   * @param bits - Speex bits buffer.
   * @param in - the raw mono audio frame to encode.
   * @return return 1 if successful.
   */
  public int encode(final Bits bits, final float[] in)
  {
    int i;
    float[] res, target, mem;
    float[] syn_resp;
    float[] orig;

    /* Copy new data in input buffer */
    System.arraycopy(frmBuf, frameSize, frmBuf, 0, bufSize-frameSize);
    frmBuf[bufSize-frameSize] = in[0] - preemph*pre_mem;
    for (i=1; i<frameSize; i++)
      frmBuf[bufSize-frameSize+i] = in[i] - preemph*in[i-1];
    pre_mem = in[frameSize-1];

    /* Move signals 1 frame towards the past */
    System.arraycopy(exc2Buf, frameSize, exc2Buf, 0, bufSize-frameSize);
    System.arraycopy(excBuf, frameSize, excBuf, 0, bufSize-frameSize);
    System.arraycopy(swBuf, frameSize, swBuf, 0, bufSize-frameSize);

    /* Window for analysis */
    for (i=0; i<windowSize; i++)
       buf2[i] = frmBuf[i+frmIdx] * window[i];

    /* Compute auto-correlation */
    Lpc.autocorr(buf2, autocorr, lpcSize+1, windowSize);

    autocorr[0] += 10;        /* prevents NANs */
    autocorr[0] *= lpc_floor; /* Noise floor in auto-correlation domain */

    /* Lag windowing: equivalent to filtering in the power-spectrum domain */
    for (i=0; i<lpcSize+1; i++)
       autocorr[i] *= lagWindow[i];

    /* Levinson-Durbin */
    Lpc.wld(lpc, autocorr, rc, lpcSize); // tmperr  
    System.arraycopy(lpc, 0, lpc, 1, lpcSize);
    lpc[0]=1;
    
    /* LPC to LSPs (x-domain) transform */
    int roots=Lsp.lpc2lsp (lpc, lpcSize, lsp, 15, 0.2f);
    /* Check if we found all the roots */
    if (roots==lpcSize)
    {
       /* LSP x-domain to angle domain*/
       for (i=0;i<lpcSize;i++)
          lsp[i] = (float)Math.acos(lsp[i]);
    } else {
       /* Search again if we can afford it */
       if (complexity>1)
          roots = Lsp.lpc2lsp (lpc, lpcSize, lsp, 11, 0.05f);
       if (roots==lpcSize) 
       {
          /* LSP x-domain to angle domain*/
          for (i=0;i<lpcSize;i++)
             lsp[i] = (float)Math.acos(lsp[i]);
       } else {
          /*If we can't find all LSP's, do some damage control and use previous filter*/
          for (i=0;i<lpcSize;i++)
          {
             lsp[i]=old_lsp[i];
          }
       }
    }

    float lsp_dist=0;
    for (i=0;i<lpcSize;i++)
       lsp_dist += (old_lsp[i] - lsp[i])*(old_lsp[i] - lsp[i]);

    /* Whole frame analysis (open-loop estimation of pitch and excitation gain) */
    float ol_gain;
    int ol_pitch;
    float ol_pitch_coef;
    {
      if (first != 0)
        for (i=0; i<lpcSize;i++)
          interp_lsp[i] = lsp[i];
      else
        for (i=0;i<lpcSize;i++)
          interp_lsp[i] = .375f*old_lsp[i] + .625f*lsp[i];

      Lsp.enforce_margin(interp_lsp, lpcSize, .002f);

      /* Compute interpolated LPCs (unquantized) for whole frame*/
      for (i=0; i<lpcSize; i++)
        interp_lsp[i] = (float)Math.cos(interp_lsp[i]);
      m_lsp.lsp2lpc(interp_lsp, interp_lpc, lpcSize);

      /*Open-loop pitch*/
      if (submodes[submodeID] == null ||
          vbr_enabled != 0 || vad_enabled != 0 ||
          submodes[submodeID].forced_pitch_gain != 0 ||
          submodes[submodeID].lbr_pitch != -1)
      {
        int[] nol_pitch = new int[6];
        float[] nol_pitch_coef = new float[6];
        
        Filters.bw_lpc(gamma1, interp_lpc, bw_lpc1, lpcSize);
        Filters.bw_lpc(gamma2, interp_lpc, bw_lpc2, lpcSize);
        
        Filters.filter_mem2(frmBuf, frmIdx, bw_lpc1, bw_lpc2, swBuf, swIdx,
                            frameSize, lpcSize, mem_sw_whole, 0);

        Ltp.open_loop_nbest_pitch(swBuf, swIdx, min_pitch, max_pitch, frameSize, 
                                  nol_pitch, nol_pitch_coef, 6);
        ol_pitch=nol_pitch[0];
        ol_pitch_coef = nol_pitch_coef[0];
        /*Try to remove pitch multiples*/
        for (i=1;i<6;i++)
        {
          if ((nol_pitch_coef[i]>.85*ol_pitch_coef) && 
              (Math.abs(nol_pitch[i]-ol_pitch/2.0)<=1 ||
               Math.abs(nol_pitch[i]-ol_pitch/3.0)<=1 || 
               Math.abs(nol_pitch[i]-ol_pitch/4.0)<=1 ||
               Math.abs(nol_pitch[i]-ol_pitch/5.0)<=1))
          {
            /*ol_pitch_coef=nol_pitch_coef[i];*/
            ol_pitch = nol_pitch[i];
          }
        }
        /*if (ol_pitch>50)
        ol_pitch/=2;*/
        /*ol_pitch_coef = sqrt(ol_pitch_coef);*/
      } else {
        ol_pitch=0;
        ol_pitch_coef=0;
      }
      /*Compute "real" excitation*/
      Filters.fir_mem2(frmBuf, frmIdx, interp_lpc, excBuf, excIdx, frameSize, lpcSize, mem_exc);

      /* Compute open-loop excitation gain */
      ol_gain=0;
      for (i=0;i<frameSize;i++)
        ol_gain += excBuf[excIdx+i]*excBuf[excIdx+i];
      
      ol_gain=(float)Math.sqrt(1+ol_gain/frameSize);
    }

    /*VBR stuff*/
    if (vbr != null && (vbr_enabled != 0 || vad_enabled != 0)) {
      if (abr_enabled != 0)
      {
        float qual_change=0;
        if (abr_drift2 * abr_drift > 0)
        {
          /* Only adapt if long-term and short-term drift are the same sign */
          qual_change = -.00001f*abr_drift/(1+abr_count);
          if (qual_change>.05f)
            qual_change=.05f;
          if (qual_change<-.05f)
            qual_change=-.05f;
        }
        vbr_quality += qual_change;
        if (vbr_quality>10)
          vbr_quality=10;
        if (vbr_quality<0)
          vbr_quality=0;
      }
      relative_quality = vbr.analysis(in, frameSize, ol_pitch, ol_pitch_coef);
      /*if (delta_qual<0)*/
      /*  delta_qual*=.1*(3+st->vbr_quality);*/
      if (vbr_enabled != 0) {
        int mode;
        int choice=0;
        float min_diff=100;
        mode = 8;
        while (mode > 0)
        {
          int v1;
          float thresh;
          v1=(int)Math.floor(vbr_quality);
          if (v1==10)
            thresh = Vbr.nb_thresh[mode][v1];
          else
            thresh = (vbr_quality-v1)*Vbr.nb_thresh[mode][v1+1] +
                     (1+v1-vbr_quality)*Vbr.nb_thresh[mode][v1];
          if (relative_quality > thresh && 
              relative_quality-thresh<min_diff)
          {
            choice = mode;
            min_diff = relative_quality-thresh;
          }
          mode--;
        }
        mode=choice;
        if (mode==0)
        {
          if (dtx_count==0 || lsp_dist>.05 || dtx_enabled==0 || dtx_count>20)
          {
            mode=1;
            dtx_count=1;
          } else {
            mode=0;
            dtx_count++;
          }
        } else {
          dtx_count=0;
        }
        setMode(mode);

        if (abr_enabled != 0)
        {
          int bitrate;
          bitrate = getBitRate();
          abr_drift+=(bitrate-abr_enabled);
          abr_drift2 = .95f*abr_drift2 + .05f*(bitrate-abr_enabled);
          abr_count += 1.0;
        }

      } else {
         /*VAD only case*/
         int mode;
         if (relative_quality<2)
         {
            if (dtx_count==0 || lsp_dist>.05 || dtx_enabled == 0 || dtx_count>20)
            {
               dtx_count=1;
               mode=1;
            } else {
               mode=0;
               dtx_count++;
            }
         } else {
            dtx_count = 0;
            mode=submodeSelect;
         }
         /*speex_encoder_ctl(state, SPEEX_SET_MODE, &mode);*/
         submodeID=mode;
      }
    } else {
      relative_quality = -1;
    }

    /* First, transmit a zero for narrowband */
    bits.pack(0, 1);

    /* Transmit the sub-mode we use for this frame */
    bits.pack(submodeID, NB_SUBMODE_BITS);

    /* If null mode (no transmission), just set a couple things to zero*/
    if (submodes[submodeID] == null)
    {
       for (i=0;i<frameSize;i++)
          excBuf[excIdx+i]=exc2Buf[exc2Idx+i]=swBuf[swIdx+i]=VERY_SMALL;

       for (i=0;i<lpcSize;i++)
          mem_sw[i]=0;
       first=1;
       bounded_pitch = 1;

       /* Final signal synthesis from excitation */
       Filters.iir_mem2(excBuf, excIdx, interp_qlpc, frmBuf, frmIdx, frameSize, lpcSize, mem_sp);

       in[0] = frmBuf[frmIdx] + preemph*pre_mem2;
       for (i=1;i<frameSize;i++)
          in[i]=frmBuf[frmIdx=i] + preemph*in[i-1];
       pre_mem2=in[frameSize-1];

       return 0;
    }

    /* LSP Quantization */
    if (first != 0)
    {
       for (i=0; i<lpcSize;i++)
          old_lsp[i] = lsp[i];
    }

    /*Quantize LSPs*/
//#if 1 /*0 for unquantized*/
     submodes[submodeID].lsqQuant.quant(lsp, qlsp, lpcSize, bits);
//#else
//     for (i=0;i<lpcSize;i++)
//       qlsp[i]=lsp[i];
//#endif

    /*If we use low bit-rate pitch mode, transmit open-loop pitch*/
    if (submodes[submodeID].lbr_pitch!=-1)
    {
      bits.pack(ol_pitch-min_pitch, 7);
    } 

    if (submodes[submodeID].forced_pitch_gain != 0)
    {
      int quant;
      quant = (int)Math.floor(.5+15*ol_pitch_coef);
      if (quant>15)
        quant=15;
      if (quant<0)
        quant=0;
      bits.pack(quant, 4);
      ol_pitch_coef=(float) 0.066667*quant;
    }

    /*Quantize and transmit open-loop excitation gain*/
    {
      int qe = (int)(Math.floor(0.5+3.5*Math.log(ol_gain)));
      if (qe<0)
        qe=0;
      if (qe>31)
        qe=31;
      ol_gain = (float) Math.exp(qe/3.5);
      bits.pack(qe, 5);
    }

    /* Special case for first frame */
    if (first != 0)
    {
      for (i=0;i<lpcSize;i++)
        old_qlsp[i] = qlsp[i];
    }

    /* Filter response */
    res = new float[subframeSize];
    /* Target signal */
    target = new float[subframeSize];
    syn_resp = new float[subframeSize];
    mem = new float[lpcSize];
    orig = new float[frameSize];
    for (i=0;i<frameSize;i++)
      orig[i]=frmBuf[frmIdx+i];

    /* Loop on sub-frames */
    for (int sub=0;sub<nbSubframes;sub++)
    {
      float tmp;
      int offset;
      int sp, sw, exc, exc2;
      int pitchval;

      /* Offset relative to start of frame */
      offset = subframeSize*sub;
      /* Original signal */
      sp=frmIdx+offset;
      /* Excitation */
      exc=excIdx+offset;
      /* Weighted signal */
      sw=swIdx+offset;

      exc2=exc2Idx+offset;

      /* LSP interpolation (quantized and unquantized) */
      tmp = (float) (1.0 + sub)/nbSubframes;
      for (i=0;i<lpcSize;i++)
        interp_lsp[i] = (1-tmp)*old_lsp[i] + tmp*lsp[i];
      for (i=0;i<lpcSize;i++)
        interp_qlsp[i] = (1-tmp)*old_qlsp[i] + tmp*qlsp[i];

      /* Make sure the filters are stable */
      Lsp.enforce_margin(interp_lsp, lpcSize, .002f);
      Lsp.enforce_margin(interp_qlsp, lpcSize, .002f);

      /* Compute interpolated LPCs (quantized and unquantized) */
      for (i=0;i<lpcSize;i++)
        interp_lsp[i] = (float) Math.cos(interp_lsp[i]);
      m_lsp.lsp2lpc(interp_lsp, interp_lpc, lpcSize);

      for (i=0;i<lpcSize;i++)
        interp_qlsp[i] = (float) Math.cos(interp_qlsp[i]);
      m_lsp.lsp2lpc(interp_qlsp, interp_qlpc, lpcSize);

      /* Compute analysis filter gain at w=pi (for use in SB-CELP) */
      tmp=1;
      pi_gain[sub]=0;
      for (i=0;i<=lpcSize;i++)
      {
        pi_gain[sub] += tmp*interp_qlpc[i];
        tmp = -tmp;
      }

      /* Compute bandwidth-expanded (unquantized) LPCs for perceptual weighting */
      Filters.bw_lpc(gamma1, interp_lpc, bw_lpc1, lpcSize);
      if (gamma2>=0)
        Filters.bw_lpc(gamma2, interp_lpc, bw_lpc2, lpcSize);
      else
      {
        bw_lpc2[0]=1;
        bw_lpc2[1]=-preemph;
        for (i=2;i<=lpcSize;i++)
          bw_lpc2[i]=0;
      }

      /* Compute impulse response of A(z/g1) / ( A(z)*A(z/g2) )*/
      for (i=0;i<subframeSize;i++)
        excBuf[exc+i]=0;
      excBuf[exc]=1;
      Filters.syn_percep_zero(excBuf, exc, interp_qlpc, bw_lpc1, bw_lpc2, syn_resp, subframeSize, lpcSize);

      /* Reset excitation */
      for (i=0;i<subframeSize;i++)
        excBuf[exc+i]=0;
      for (i=0;i<subframeSize;i++)
        exc2Buf[exc2+i]=0;

      /* Compute zero response of A(z/g1) / ( A(z/g2) * A(z) ) */
      for (i=0;i<lpcSize;i++)
        mem[i]=mem_sp[i];
      Filters.iir_mem2(excBuf, exc, interp_qlpc, excBuf, exc, subframeSize, lpcSize, mem);

      for (i=0;i<lpcSize;i++)
        mem[i]=mem_sw[i];
      Filters.filter_mem2(excBuf, exc, bw_lpc1, bw_lpc2, res, 0, subframeSize, lpcSize, mem, 0);

      /* Compute weighted signal */
      for (i=0;i<lpcSize;i++)
        mem[i]=mem_sw[i];
      Filters.filter_mem2(frmBuf, sp, bw_lpc1, bw_lpc2, swBuf, sw, subframeSize, lpcSize, mem, 0);
    
      /* Compute target signal */
      for (i=0;i<subframeSize;i++)
        target[i]=swBuf[sw+i]-res[i];

      for (i=0;i<subframeSize;i++)
        excBuf[exc+i]=exc2Buf[exc2+i]=0;

      /* If we have a long-term predictor (otherwise, something's wrong) */
//    if (submodes[submodeID].ltp.quant)
//    {
      int pit_min, pit_max;
      /* Long-term prediction */
      if (submodes[submodeID].lbr_pitch != -1)
      {
        /* Low bit-rate pitch handling */
        int margin;
        margin = submodes[submodeID].lbr_pitch;
        if (margin != 0)
        {
          if (ol_pitch < min_pitch+margin-1)
            ol_pitch=min_pitch+margin-1;
          if (ol_pitch > max_pitch-margin)
            ol_pitch=max_pitch-margin;
          pit_min = ol_pitch-margin+1;
          pit_max = ol_pitch+margin;
        } else {
          pit_min=pit_max=ol_pitch;
        }
      } else {
        pit_min = min_pitch;
        pit_max = max_pitch;
      }
    
      /* Force pitch to use only the current frame if needed */
      if (bounded_pitch != 0 && pit_max>offset)
        pit_max=offset;

      /* Perform pitch search */
      pitchval = submodes[submodeID].ltp.quant(target, swBuf, sw, interp_qlpc, bw_lpc1, bw_lpc2,
                                               excBuf, exc, pit_min, pit_max, ol_pitch_coef, lpcSize,
                                               subframeSize, bits, exc2Buf, exc2, syn_resp, complexity);

      pitch[sub]=pitchval;

//    } else {
//      speex_error ("No pitch prediction, what's wrong");
//    }
   
      /* Update target for adaptive codebook contribution */
      Filters.syn_percep_zero(excBuf, exc, interp_qlpc, bw_lpc1, bw_lpc2, res, subframeSize, lpcSize);
      for (i=0;i<subframeSize;i++)
        target[i]-=res[i];

      /* Quantization of innovation */
      {
        int innovptr;
        float ener=0, ener_1;

        innovptr = sub*subframeSize;
        for (i=0;i<subframeSize;i++)
          innov[innovptr+i]=0;

        Filters.residue_percep_zero(target, 0, interp_qlpc, bw_lpc1, bw_lpc2, buf2, subframeSize, lpcSize);
        for (i=0;i<subframeSize;i++)
          ener+=buf2[i]*buf2[i];
        ener=(float)Math.sqrt(.1f+ener/subframeSize);
        /*for (i=0;i<subframeSize;i++)
        System.out.print(buf2[i]/ener + "\t");
        */
        
        ener /= ol_gain;

        /* Calculate gain correction for the sub-frame (if any) */
        if (submodes[submodeID].have_subframe_gain != 0) {
          int qe;
          ener=(float)Math.log(ener);
          if (submodes[submodeID].have_subframe_gain==3) {
            qe = VQ.index(ener, exc_gain_quant_scal3, 8);
            bits.pack(qe, 3);
            ener=exc_gain_quant_scal3[qe];
          }
          else {
            qe = VQ.index(ener, exc_gain_quant_scal1, 2);
            bits.pack(qe, 1);
            ener=exc_gain_quant_scal1[qe];               
          }
          ener=(float)Math.exp(ener);
        }
        else {
          ener=1;
        }

        ener*=ol_gain;

        /*System.out.println(ener + " " + ol_gain);*/

        ener_1 = 1/ener;

        /* Normalize innovation */
        for (i=0;i<subframeSize;i++)
          target[i]*=ener_1;

        /* Quantize innovation */
//      if (submodes[submodeID].innovation != null)
//      {
        /* Codebook search */
        submodes[submodeID].innovation.quant(target, interp_qlpc, bw_lpc1, bw_lpc2,
                                             lpcSize, subframeSize, innov,
                                             innovptr, syn_resp, bits, complexity);

        /* De-normalize innovation and update excitation */
        for (i=0;i<subframeSize;i++)
          innov[innovptr+i]*=ener;
        for (i=0;i<subframeSize;i++)
          excBuf[exc+i] += innov[innovptr+i];
//      } else {
//        speex_error("No fixed codebook");
//      }

        /* In some (rare) modes, we do a second search (more bits) to reduce noise even more */
        if (submodes[submodeID].double_codebook != 0) {
          float[] innov2 = new float[subframeSize];
//          for (i=0;i<subframeSize;i++)
//            innov2[i]=0;
          for (i=0;i<subframeSize;i++)
            target[i]*=2.2;
          submodes[submodeID].innovation.quant(target, interp_qlpc, bw_lpc1, bw_lpc2, 
                                               lpcSize, subframeSize, innov2, 0,
                                               syn_resp, bits, complexity);
          for (i=0;i<subframeSize;i++)
            innov2[i]*=ener*(1/2.2);
          for (i=0;i<subframeSize;i++)
            excBuf[exc+i] += innov2[i];
        }

        for (i=0;i<subframeSize;i++)
          target[i]*=ener;
      }

      /*Keep the previous memory*/
      for (i=0;i<lpcSize;i++)
        mem[i]=mem_sp[i];
      /* Final signal synthesis from excitation */
      Filters.iir_mem2(excBuf, exc, interp_qlpc, frmBuf, sp, subframeSize, lpcSize, mem_sp);

      /* Compute weighted signal again, from synthesized speech (not sure it's the right thing) */
      Filters.filter_mem2(frmBuf, sp, bw_lpc1, bw_lpc2, swBuf, sw, subframeSize, lpcSize, mem_sw, 0);
      for (i=0;i<subframeSize;i++)
        exc2Buf[exc2+i]=excBuf[exc+i];
    }

    /* Store the LSPs for interpolation in the next frame */
    if (submodeID>=1)
    {
      for (i=0;i<lpcSize;i++)
        old_lsp[i] = lsp[i];
      for (i=0;i<lpcSize;i++)
        old_qlsp[i] = qlsp[i];
    }

    if (submodeID==1)
    {
      if (dtx_count != 0) {
        bits.pack(15, 4);
      }
      else {
        bits.pack(0, 4);
      }
    }

    /* The next frame will not be the first (Duh!) */
    first = 0;

    {
      float ener=0, err=0;
      float snr;
      for (i=0;i<frameSize;i++)
      {
        ener+=frmBuf[frmIdx+i]*frmBuf[frmIdx+i];
        err += (frmBuf[frmIdx+i]-orig[i])*(frmBuf[frmIdx+i]-orig[i]);
      }
      snr = (float) (10*Math.log((ener+1)/(err+1)));
      /*System.out.println("Frame result: SNR="+snr+" E="+ener+" Err="+err+"\r\n");*/
    }
  
    /* Replace input by synthesized speech */
    in[0] = frmBuf[frmIdx] + preemph*pre_mem2;
    for (i=1;i<frameSize;i++)
      in[i]=frmBuf[frmIdx+i] + preemph*in[i-1];
    pre_mem2=in[frameSize-1];

    if (submodes[submodeID].innovation instanceof NoiseSearch || submodeID==0)
      bounded_pitch = 1;
    else
      bounded_pitch = 0;

    return 1;
  }
    
  /**
   * Returns the size in bits of an audio frame encoded with the current mode.
   * @return the size in bits of an audio frame encoded with the current mode.
   */
  public int getEncodedFrameSize()
  {
    return NB_FRAME_SIZE[submodeID];
  }

  //---------------------------------------------------------------------------
  // Speex Control Functions
  //---------------------------------------------------------------------------

  /**
   * Sets the Quality
   * @param quality
   */
  public void setQuality(int quality)
  {
    if (quality < 0) {
      quality = 0;
    }
    if (quality > 10) {
      quality = 10;
    }
    submodeID = submodeSelect = NB_QUALITY_MAP[quality];
  }
  
  /**
   * Gets the bitrate.
   * @return the bitrate.
   */
  public int getBitRate()
  {
    if (submodes[submodeID] != null)
      return sampling_rate*submodes[submodeID].bits_per_frame/frameSize;
    else
      return sampling_rate*(NB_SUBMODE_BITS+1)/frameSize;
  }
  
  /**
   * 
   */
//  public void    resetState()
//  {
//  }
  
  //---------------------------------------------------------------------------
  //---------------------------------------------------------------------------

  /**
   * Sets the encoding submode.
   * @param mode
   */
  public void setMode(int mode)
  {
    if (mode < 0) {
      mode = 0;
    }
    submodeID = submodeSelect = mode;
  }
  
  /**
   * Returns the encoding submode currently in use.
   * @return the encoding submode currently in use.
   */
  public int getMode()
  {
    return submodeID;
  }
  
  /**
   * Sets the bitrate.
   * @param bitrate
   */
  public void setBitRate(final int bitrate)
  {
    for (int i=10; i>=0; i--) {
      setQuality(i);
      if (getBitRate() <= bitrate)
        return;
    }
  }
  
  /**
   * Sets whether or not to use Variable Bit Rate encoding.
   * @param vbr
   */
  public void setVbr(final boolean vbr)
  {
    vbr_enabled = vbr ? 1 : 0;
  }
  
  /**
   * Returns whether or not we are using Variable Bit Rate encoding.
   * @return whether or not we are using Variable Bit Rate encoding.
   */
  public boolean getVbr()
  {
    return vbr_enabled != 0;
  }
  
  /**
   * Sets whether or not to use Voice Activity Detection encoding.
   * @param vad
   */
  public void setVad(final boolean vad)
  {
    vad_enabled = vad ? 1 : 0;
  }
  
  /**
   * Returns whether or not we are using Voice Activity Detection encoding.
   * @return whether or not we are using Voice Activity Detection encoding.
   */
  public boolean getVad()
  {
    return vad_enabled != 0;
  }
  
  /**
   * Sets whether or not to use Discontinuous Transmission encoding.
   * @param dtx
   */
  public void setDtx(final boolean dtx)
  {
    dtx_enabled = dtx ? 1 : 0;
  }
  
  /**
   * Returns the Average Bit Rate used (0 if ABR is not turned on).
   * @return the Average Bit Rate used (0 if ABR is not turned on).
   */
  public int getAbr()
  {
    return abr_enabled;
  }
  
  /**
   * Sets the Average Bit Rate.
   * @param abr
   */
  public void setAbr(final int abr)
  {
    abr_enabled = (abr!=0) ? 1 : 0;
    vbr_enabled = 1;
    {
      int i=10, rate, target;
      float vbr_qual;
      target = abr;
      while (i>=0)
      {
        setQuality(i);
        rate = getBitRate();
        if (rate <= target)
          break;
        i--;
      }
      vbr_qual=i;
      if (vbr_qual<0)
        vbr_qual=0;
      setVbrQuality(vbr_qual);
      abr_count=0;
      abr_drift=0;
      abr_drift2=0;
    }
  }

  /**
   * Sets the Varible Bit Rate Quality.
   * @param quality
   */
  public void setVbrQuality(float quality)
  {
    if (quality < 0f)
      quality = 0f;
    if (quality > 10f)
      quality = 10f;
    vbr_quality = quality;
  }
  
  /**
   * Returns the Varible Bit Rate Quality.
   * @return the Varible Bit Rate Quality.
   */
  public float getVbrQuality()
  {
    return vbr_quality;
  }
  
  /**
   * Sets the algorthmic complexity.
   * @param complexity
   */
  public void setComplexity(int complexity)
  {
    if (complexity < 0)
      complexity = 0;
    if (complexity > 10)
      complexity = 10;
    this.complexity = complexity;
  }
  
  /**
   * Returns the algorthmic complexity.
   * @return the algorthmic complexity.
   */
  public int getComplexity()
  {
    return complexity;
  }
  
  /**
   * Sets the sampling rate.
   * @param rate
   */
  public void setSamplingRate(final int rate)
  {
    sampling_rate = rate;
  }
    
  /**
   * Returns the sampling rate.
   * @return the sampling rate.
   */
  public int getSamplingRate()
  {
    return sampling_rate;
  }

  /**
   * Return LookAhead.
   * @return LookAhead.
   */
  public int getLookAhead()
  {
    return windowSize - frameSize;
  }
  
  /**
   * Returns the relative quality.
   * @return the relative quality.
   */
  public float getRelativeQuality()
  {
    return relative_quality;
  }
}

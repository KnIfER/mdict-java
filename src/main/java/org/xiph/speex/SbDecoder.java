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
 * Class: SbDecoder.java                                                      *
 *                                                                            *
 * Author: James LAWRENCE                                                     *
 * Modified by: Marc GIMPEL                                                   *
 * Based on code by: Jean-Marc VALIN                                          *
 *                                                                            *
 * Date: March 2003                                                           *
 *                                                                            *
 ******************************************************************************/

/* $Id: SbDecoder.java,v 1.3 2005/05/27 13:17:00 mgimpel Exp $ */

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
 * Sideband Speex Decoder
 * 
 * @author Jim Lawrence, helloNetwork.com
 * @author Marc Gimpel, Wimba S.A. (mgimpel@horizonwimba.com)
 * @version $Revision: 1.3 $
 */
public class SbDecoder
  extends SbCodec
  implements Decoder
{
  /** */
  protected Decoder lowdec;
  /** */
  protected Stereo  stereo;
  /** */
  protected boolean enhanced;
  
  private float[] innov2;

  /**
   * Constructor
   */
  public SbDecoder()
  {
    stereo   = new Stereo();
    enhanced = true;
  }

  /**
   * Wideband initialisation
   */
  public void wbinit()
  {
    lowdec = new NbDecoder();
    ((NbDecoder)lowdec).nbinit();
    lowdec.setPerceptualEnhancement(enhanced);
    // Initialize SubModes
    super.wbinit();
    // Initialize variables
    init(160, 40, 8, 640, .7f);
  }

  /**
   * Ultra-wideband initialisation
   */
  public void uwbinit()
  {
    lowdec = new SbDecoder();
    ((SbDecoder)lowdec).wbinit();
    lowdec.setPerceptualEnhancement(enhanced);
    // Initialize SubModes
    super.uwbinit();
    // Initialize variables
    init(320, 80, 8, 1280, .5f);
  }

  /**
   * Initialisation
   * @param frameSize
   * @param subframeSize
   * @param lpcSize
   * @param bufSize
   * @param foldingGain
   */
  public void init(final int frameSize,
                   final int subframeSize,
                   final int lpcSize,
                   final int bufSize,
                   final float foldingGain)
  {
    super.init(frameSize, subframeSize, lpcSize, bufSize, foldingGain);
    excIdx      = 0;
    innov2      = new float[subframeSize];
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
    int i, sub, wideband, ret;
    float[] low_pi_gain, low_exc, low_innov;

    /* Decode the low-band */
    ret = lowdec.decode(bits, x0d);
    if (ret != 0) {
      return ret;
    }
    boolean dtx = lowdec.getDtx();
    if (bits == null) {
      decodeLost(out, dtx);
      return 0;
    }
    /* Check "wideband bit" */
    wideband = bits.peek(); 
    if (wideband!=0) {
      /*Regular wideband frame, read the submode*/
      wideband  = bits.unpack(1);
      submodeID = bits.unpack(3);
    } 
    else {
      /* was a narrowband frame, set "null submode"*/
      submodeID = 0;
    }

    for (i=0;i<frameSize;i++)
      excBuf[i]=0;

    /* If null mode (no transmission), just set a couple things to zero*/
    if (submodes[submodeID] == null) {
      if (dtx) {
        decodeLost(out, true);
        return 0;
      }
      for (i=0;i<frameSize;i++)
        excBuf[i]=VERY_SMALL;

      first=1;
      /* Final signal synthesis from excitation */
      Filters.iir_mem2(excBuf, excIdx, interp_qlpc, high, 0, frameSize,
                       lpcSize, mem_sp);
      filters.fir_mem_up(x0d, h0, y0, fullFrameSize, QMF_ORDER, g0_mem);
      filters.fir_mem_up(high, h1, y1, fullFrameSize, QMF_ORDER, g1_mem);

      for (i=0;i<fullFrameSize;i++)
        out[i]=2*(y0[i]-y1[i]);
      return 0;
    }
    low_pi_gain = lowdec.getPiGain();
    low_exc     = lowdec.getExc();
    low_innov   = lowdec.getInnov();
    submodes[submodeID].lsqQuant.unquant(qlsp, lpcSize, bits);
    
    if (first!=0) {
      for (i=0;i<lpcSize;i++)
        old_qlsp[i] = qlsp[i];
    }

    for (sub=0;sub<nbSubframes;sub++) {
      float tmp, filter_ratio, el=0.0f, rl=0.0f,rh=0.0f;
      int subIdx=subframeSize*sub;
      
      /* LSP interpolation */
      tmp = (1.0f + sub)/nbSubframes;
      for (i=0;i<lpcSize;i++)
        interp_qlsp[i] = (1-tmp)*old_qlsp[i] + tmp*qlsp[i];

      Lsp.enforce_margin(interp_qlsp, lpcSize, .05f);

      /* LSPs to x-domain */
      for (i=0;i<lpcSize;i++)
        interp_qlsp[i] = (float)Math.cos(interp_qlsp[i]);

      /* LSP to LPC */
      m_lsp.lsp2lpc(interp_qlsp, interp_qlpc, lpcSize);
  
      if (enhanced) {
        float k1, k2, k3;
        k1=submodes[submodeID].lpc_enh_k1;
        k2=submodes[submodeID].lpc_enh_k2;
        k3=k1-k2;
        Filters.bw_lpc(k1, interp_qlpc, awk1, lpcSize);
        Filters.bw_lpc(k2, interp_qlpc, awk2, lpcSize);
        Filters.bw_lpc(k3, interp_qlpc, awk3, lpcSize);
      }

      /* Calculate reponse ratio between low & high filter in band middle (4000 Hz) */      
      tmp=1;
      pi_gain[sub]=0;
      for (i=0;i<=lpcSize;i++) {
        rh += tmp*interp_qlpc[i];
        tmp = -tmp;
        pi_gain[sub]+=interp_qlpc[i];
      }
      rl           = low_pi_gain[sub];
      rl           = 1/(Math.abs(rl)+.01f);
      rh           = 1/(Math.abs(rh)+.01f);
      filter_ratio = Math.abs(.01f+rh)/(.01f+Math.abs(rl));
      
      /* reset excitation buffer */
      for (i=subIdx;i<subIdx+subframeSize;i++)
        excBuf[i]=0;

      if (submodes[submodeID].innovation==null) {
        float g;
        int quant;

        quant = bits.unpack(5);
        g     = (float)Math.exp(((double)quant-10)/8.0);       
        g     /= filter_ratio;
        
        /* High-band excitation using the low-band excitation and a gain */
        for (i=subIdx;i<subIdx+subframeSize;i++)
          excBuf[i]=foldingGain*g*low_innov[i];
      } 
      else {
        float gc, scale;
        int qgc = bits.unpack(4);

        for (i=subIdx;i<subIdx+subframeSize;i++)
          el+=low_exc[i]*low_exc[i];

        gc    = (float)Math.exp((1/3.7f)*qgc-2);
        scale = gc*(float)Math.sqrt(1+el)/filter_ratio;
        submodes[submodeID].innovation.unquant(excBuf, subIdx, subframeSize, bits); 

        for (i=subIdx;i<subIdx+subframeSize;i++)
          excBuf[i]*=scale;

        if (submodes[submodeID].double_codebook!=0) {
          for (i=0;i<subframeSize;i++)
            innov2[i]=0;
          submodes[submodeID].innovation.unquant(innov2, 0, subframeSize, bits); 
          for (i=0;i<subframeSize;i++)
            innov2[i]*=scale*(1/2.5f);
          for (i=0;i<subframeSize;i++)
            excBuf[subIdx+i] += innov2[i];
        }
      }

      for (i=subIdx;i<subIdx+subframeSize;i++)
        high[i]=excBuf[i];

      if (enhanced) {
        /* Use enhanced LPC filter */
        Filters.filter_mem2(high, subIdx, awk2, awk1, subframeSize,
                            lpcSize, mem_sp, lpcSize);
        Filters.filter_mem2(high, subIdx, awk3, interp_qlpc, subframeSize,
                            lpcSize, mem_sp, 0);
      }
      else {
         /* Use regular filter */
         for (i=0;i<lpcSize;i++)
            mem_sp[lpcSize+i] = 0;
         Filters.iir_mem2(high, subIdx, interp_qlpc, high, subIdx,
                          subframeSize, lpcSize, mem_sp);
      }
    }

    filters.fir_mem_up(x0d, h0, y0, fullFrameSize, QMF_ORDER, g0_mem);
    filters.fir_mem_up(high, h1, y1, fullFrameSize, QMF_ORDER, g1_mem);

    for (i=0;i<fullFrameSize;i++)
      out[i]=2*(y0[i]-y1[i]);

    for (i=0;i<lpcSize;i++)
      old_qlsp[i] = qlsp[i];

    first = 0;
    return 0;
  }
    
  /**
   * Decode when packets are lost.
   * @param out - the generated mono audio frame.
   * @param dtx
   * @return 0 if successful.
   */
  public int decodeLost(final float[] out, final boolean dtx)
  {
    int i;
    int saved_modeid=0;

    if (dtx) {
      saved_modeid=submodeID;
      submodeID=1;
    }
    else {
      Filters.bw_lpc(0.99f, interp_qlpc, interp_qlpc, lpcSize);
    }

    first=1;
    awk1=new float[lpcSize+1];
    awk2=new float[lpcSize+1];
    awk3=new float[lpcSize+1];
    
    if (enhanced) {
      float k1,k2,k3;
      if (submodes[submodeID] != null) {
        k1=submodes[submodeID].lpc_enh_k1;
        k2=submodes[submodeID].lpc_enh_k2;
      }
      else {
        k1 = k2 = 0.7f;
      }
      k3=k1-k2;
      Filters.bw_lpc(k1, interp_qlpc, awk1, lpcSize);
      Filters.bw_lpc(k2, interp_qlpc, awk2, lpcSize);
      Filters.bw_lpc(k3, interp_qlpc, awk3, lpcSize);
    }

    /* Final signal synthesis from excitation */
    if (!dtx) {
      for (i=0;i<frameSize;i++)
        excBuf[excIdx+i] *= .9;
    }
    for (i=0;i<frameSize;i++)
      high[i]=excBuf[excIdx+i];

    if (enhanced) {
      /* Use enhanced LPC filter */
      Filters.filter_mem2(high, 0, awk2, awk1, high, 0, frameSize,
                          lpcSize, mem_sp, lpcSize);
      Filters.filter_mem2(high, 0, awk3, interp_qlpc, high, 0, frameSize,
                          lpcSize, mem_sp, 0);
    }
    else { /* Use regular filter */
      for (i=0;i<lpcSize;i++)
        mem_sp[lpcSize+i] = 0;
      Filters.iir_mem2(high, 0, interp_qlpc, high, 0, frameSize, lpcSize,
                       mem_sp);
    }
    /*iir_mem2(st->exc, st->interp_qlpc, st->high, st->frame_size, st->lpcSize, st->mem_sp);*/
    
    /* Reconstruct the original */
    filters.fir_mem_up(x0d, h0, y0, fullFrameSize, QMF_ORDER, g0_mem);
    filters.fir_mem_up(high, h1, y1, fullFrameSize, QMF_ORDER, g1_mem);
    for (i=0;i<fullFrameSize;i++)
      out[i]=2*(y0[i]-y1[i]);
    
    if (dtx) {
      submodeID=saved_modeid;
    }
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
  public void setPerceptualEnhancement(boolean enhanced)
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

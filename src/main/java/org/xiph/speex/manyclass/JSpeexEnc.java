package org.xiph.speex.manyclass; /******************************************************************************
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
 * Class: org.xiph.speex.manyclass.JSpeexEnc.java                                                      *
 *                                                                            *
 * Author: Marc GIMPEL                                                        *
 * Based on code by: Jean-Marc VALIN                                          *
 *                                                                            *
 * Date: 9th April 2003                                                       *
 *                                                                            *
 ******************************************************************************/

/* $Id: org.xiph.speex.manyclass.JSpeexEnc.java,v 1.5 2005/05/27 13:14:39 mgimpel Exp $ */

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

import org.xiph.speex.*;

import java.io.File;
import java.io.IOException;
import java.io.EOFException;
import java.io.DataInputStream;
import java.io.FileInputStream;

/**
 * Java Speex Command Line Encoder.
 * 
 * Currently this code has been updated to be compatible with release 1.0.3.
 * 
 * @author Marc Gimpel, Wimba S.A. (mgimpel@horizonwimba.com)
 * @version $Revision: 1.5 $
 */
public class JSpeexEnc
{
  /** Version of the Speex Encoder */
  public static final String VERSION = "Java Speex Command Line Encoder v0.9.7 ($Revision: 1.5 $)";
  /** Copyright display String */
  public static final String COPYRIGHT = "Copyright (C) 2002-2004 Wimba S.A.";
  
  /** Print level for messages : Print debug information */
  public static final int DEBUG = 0;
  /** Print level for messages : Print basic information */
  public static final int INFO  = 1;
  /** Print level for messages : Print only warnings and errors */
  public static final int WARN  = 2;
  /** Print level for messages : Print only errors */
  public static final int ERROR = 3;
  /** Print level for messages */
  protected int printlevel = INFO;

  /** File format for input or output audio file: Raw */
  public static final int FILE_FORMAT_RAW  = 0;
  /** File format for input or output audio file: Ogg */
  public static final int FILE_FORMAT_OGG  = 1;
  /** File format for input or output audio file: Wave */
  public static final int FILE_FORMAT_WAVE = 2;
  /** Defines File format for input audio file (Raw, Ogg or Wave). */
  protected int srcFormat  = FILE_FORMAT_OGG;
  /** Defines File format for output audio file (Raw or Wave). */
  protected int destFormat = FILE_FORMAT_WAVE;

  /** Defines the encoder mode (0=NB, 1=WB and 2=UWB). */
  protected int mode       = -1;
  /** Defines the encoder quality setting (integer from 0 to 10). */
  protected int quality    = 8;
  /** Defines the encoders algorithmic complexity. */
  protected int complexity = 3;
  /** Defines the number of frames per speex packet. */
  protected int nframes    = 1;
  /** Defines the desired bitrate for the encoded audio. */
  protected int bitrate    = -1;
  /** Defines the sampling rate of the audio input. */
  protected int sampleRate = -1;
  /** Defines the number of channels of the audio input (1=mono, 2=stereo). */
  protected int channels   = 1;
  /** Defines the encoder VBR quality setting (float from 0 to 10). */
  protected float vbr_quality = -1;
  /** Defines whether or not to use VBR (Variable Bit Rate). */
  protected boolean vbr    = false;
  /** Defines whether or not to use VAD (Voice Activity Detection). */
  protected boolean vad    = false;
  /** Defines whether or not to use DTX (Discontinuous Transmission). */
  protected boolean dtx    = false;

  /** The audio input file */
  protected String srcFile;
  /** The audio output file */
  protected String destFile;

  /**
   * Builds a plain JSpeex Encoder with default values.
   */
  public JSpeexEnc()
  {
  }

  /**
   * Command line entrance:
   * <pre>
   * Usage: org.xiph.speex.manyclass.JSpeexEnc [options] input_file output_file
   * </pre>
   * @param args Command line parameters.
   * @exception IOException
   */
  public static void main(final String[] args)
    throws IOException
  {
    JSpeexEnc encoder = new JSpeexEnc();
    if (encoder.parseArgs(args)) {
      encoder.encode();
    }
  }

  /**
   * Parse the command line arguments.
   * @param args Command line parameters.
   * @return true if the parsed arguments are sufficient to run the encoder.
   */
  public boolean parseArgs(final String[] args)
  {
    // make sure we have command args
    if (args.length < 2) {
      if (args.length==1 && (args[0].equalsIgnoreCase("-v") || args[0].equalsIgnoreCase("--version"))) {
        version();
        return false;
      }
      usage();
      return false;
    }
    // Determine input, output and file formats
    srcFile = args[args.length-2];
    destFile = args[args.length-1];
    if (srcFile.toLowerCase().endsWith(".wav")) {
      srcFormat = FILE_FORMAT_WAVE;
    }
    else {
      srcFormat = FILE_FORMAT_RAW;
    }
    if (destFile.toLowerCase().endsWith(".spx")) {
      destFormat = FILE_FORMAT_OGG;
    }
    else if (destFile.toLowerCase().endsWith(".wav")) {
      destFormat = FILE_FORMAT_WAVE;
    }
    else {
      destFormat = FILE_FORMAT_RAW;
    }
    // Determine encoder options
    for (int i=0; i<args.length-2; i++) {
      if (args[i].equalsIgnoreCase("-h") || args[i].equalsIgnoreCase("--help")) {
        usage();
        return false;
      }
      else if (args[i].equalsIgnoreCase("-v") || args[i].equalsIgnoreCase("--version")) {
        version();
        return false;
      }
      else if (args[i].equalsIgnoreCase("--verbose")) {
        printlevel = DEBUG;
      }
      else if (args[i].equalsIgnoreCase("--quiet")) {
        printlevel = WARN;
      }
      else if (args[i].equalsIgnoreCase("-n") || 
               args[i].equalsIgnoreCase("-nb") ||
               args[i].equalsIgnoreCase("--narrowband")) {
        mode = 0;
      }
      else if (args[i].equalsIgnoreCase("-w") ||
               args[i].equalsIgnoreCase("-wb") ||
               args[i].equalsIgnoreCase("--wideband")) {
        mode = 1;
      }
      else if (args[i].equalsIgnoreCase("-u") ||
               args[i].equalsIgnoreCase("-uwb") ||
               args[i].equalsIgnoreCase("--ultra-wideband")) {
        mode = 2;
      }
      else if (args[i].equalsIgnoreCase("-q") || args[i].equalsIgnoreCase("--quality")) {
        try {
          vbr_quality = Float.parseFloat(args[++i]);
          quality = (int) vbr_quality;
        }
        catch (NumberFormatException e) {
          usage();
          return false;
        }
      }
      else if (args[i].equalsIgnoreCase("--complexity")) {
        try {
          complexity = Integer.parseInt(args[++i]);
        }
        catch (NumberFormatException e) {
          usage();
          return false;
        }
      }
      else if (args[i].equalsIgnoreCase("--nframes")) {
        try {
          nframes = Integer.parseInt(args[++i]);
        }
        catch (NumberFormatException e) {
          usage();
          return false;
        }
      }
      else if (args[i].equalsIgnoreCase("--vbr")) {
        vbr = true;
      }
      else if (args[i].equalsIgnoreCase("--vad")) {
        vad = true;
      }
      else if (args[i].equalsIgnoreCase("--dtx")) {
        dtx = true;
      }
      else if (args[i].equalsIgnoreCase("--rate")) {
        try {
          sampleRate = Integer.parseInt(args[++i]);
        }
        catch (NumberFormatException e) {
          usage();
          return false;
        }
      }
      else if (args[i].equalsIgnoreCase("--stereo")) {
        channels = 2;
      }
      else {
        usage();
        return false;
      }
    }
    return true;
  }

  /**
   * Prints the usage guidelines.
   */
  public static void usage()
  {
    version();
    System.out.println("");
    System.out.println("Usage: org.xiph.speex.manyclass.JSpeexEnc [options] input_file output_file");
    System.out.println("Where:");
    System.out.println("  input_file can be:");
    System.out.println("    filename.wav  a PCM wav file");
    System.out.println("    filename.*    a raw PCM file (any extension other than .wav)");
    System.out.println("  output_file can be:");
    System.out.println("    filename.spx  an Ogg Speex file");
    System.out.println("    filename.wav  a Wave Speex file (beta!!!)");
    System.out.println("    filename.*    a raw Speex file");
    System.out.println("Options: -h, --help     This help");
    System.out.println("         -v, --version  Version information");
    System.out.println("         --verbose      Print detailed information");
    System.out.println("         --quiet        Print minimal information");
    System.out.println("         -n, -nb        Consider input as Narrowband (8kHz)");
    System.out.println("         -w, -wb        Consider input as Wideband (16kHz)");
    System.out.println("         -u, -uwb       Consider input as Ultra-Wideband (32kHz)");
    System.out.println("         --quality n    Encoding quality (0-10) default 8");
    System.out.println("         --complexity n Encoding complexity (0-10) default 3");
    System.out.println("         --nframes n    Number of frames per Ogg packet, default 1");
    System.out.println("         --vbr          Enable varible bit-rate (VBR)");
    System.out.println("         --vad          Enable voice activity detection (VAD)");
    System.out.println("         --dtx          Enable file based discontinuous transmission (DTX)");
    System.out.println("         if the input file is raw PCM (not a Wave file)");
    System.out.println("         --rate n       Sampling rate for raw input");
    System.out.println("         --stereo       Consider input as stereo");
    System.out.println("More information is available from: http://jspeex.sourceforge.net/");
    System.out.println("This code is a Java port of the Speex codec: http://www.speex.org/");
  }

  /**
   * Prints the version.
   */
  public static void version()
  {
    System.out.println(VERSION);
    System.out.println("using " + SpeexEncoder.VERSION);
    System.out.println(COPYRIGHT);
  }
  
  /**
   * Encodes a PCM file to Speex. 
   * @exception IOException
   */
  public void encode()
    throws IOException
  {
    encode(new File(srcFile), new File(destFile));
  }

  /**
   * Encodes a PCM file to Speex. 
   * @param srcPath
   * @param destPath
   * @exception IOException
   */
  public void encode(final File srcPath, final File destPath)
    throws IOException
  {
    byte[] temp    = new byte[2560]; // stereo UWB requires one to read 2560b
    final int HEADERSIZE = 8;
    final String RIFF      = "RIFF";
    final String WAVE      = "WAVE";
    final String FORMAT    = "fmt ";
    final String DATA      = "data";
    final int WAVE_FORMAT_PCM = 0x0001;
    // Display info
    if (printlevel <= INFO) version();
    if (printlevel <= DEBUG) System.out.println("");
    if (printlevel <= DEBUG) System.out.println("Input File: " + srcPath);
    // Open the input stream
    DataInputStream dis = new DataInputStream(new FileInputStream(srcPath));
    // Prepare input stream
    if (srcFormat == FILE_FORMAT_WAVE) {
      // read the WAVE header
      dis.readFully(temp, 0, HEADERSIZE+4);
      // make sure its a WAVE header
      if (!RIFF.equals(new String(temp, 0, 4)) &&
          !WAVE.equals(new String(temp, 8, 4))) {
        System.err.println("Not a WAVE file");
        return;
      }
      // Read other header chunks
      dis.readFully(temp, 0, HEADERSIZE);
      String chunk = new String(temp, 0, 4);
      int size = readInt(temp, 4);
      while (!chunk.equals(DATA)) {
        dis.readFully(temp, 0, size);
        if (chunk.equals(FORMAT)) {
          /*
          typedef struct waveformat_extended_tag {
          WORD wFormatTag; // format type
          WORD nChannels; // number of channels (i.e. mono, stereo...)
          DWORD nSamplesPerSec; // sample rate
          DWORD nAvgBytesPerSec; // for buffer estimation
          WORD nBlockAlign; // block size of data
          WORD wBitsPerSample; // Number of bits per sample of mono data
          WORD cbSize; // The count in bytes of the extra size 
          } WAVEFORMATEX;
          */
          if (readShort(temp, 0) != WAVE_FORMAT_PCM) {
            System.err.println("Not a PCM file");
            return;
          }
          channels = readShort(temp, 2);
          sampleRate = readInt(temp, 4);
          if (readShort(temp, 14) != 16) {
            System.err.println("Not a 16 bit file " + readShort(temp, 18));
            return;
          }
          // Display audio info
          if (printlevel <= DEBUG) {
            System.out.println("File Format: PCM wave");
            System.out.println("Sample Rate: " + sampleRate);
            System.out.println("Channels: " + channels);
          }
        }
        dis.readFully(temp, 0, HEADERSIZE);
        chunk = new String(temp, 0, 4);
        size = readInt(temp, 4);
      }
      if (printlevel <= DEBUG) System.out.println("Data size: " + size);
    }
    else {
      if (sampleRate < 0) {
        switch (mode) {
        case 0:
          sampleRate = 8000;
          break;
        case 1:
          sampleRate = 16000;
          break;
        case 2:
          sampleRate = 32000;
          break;
        default:
          sampleRate = 8000;
          break;
        }
      }
      // Display audio info
      if (printlevel <= DEBUG) {
        System.out.println("File format: Raw audio");
        System.out.println("Sample rate: " + sampleRate);
        System.out.println("Channels: " + channels);
        System.out.println("Data size: " + srcPath.length());
      }
    }

    // Set the mode if it has not yet been determined
    if (mode < 0) {
      if (sampleRate < 100) // Sample Rate has probably been given in kHz
        sampleRate *= 1000;
      if (sampleRate < 12000)
        mode = 0; // Narrowband
      else if (sampleRate < 24000)
        mode = 1; // Wideband
      else
        mode = 2; // Ultra-wideband
    }
    // Construct a new encoder
    SpeexEncoder speexEncoder = new SpeexEncoder();
    speexEncoder.init(mode, quality, sampleRate, channels);
    if (complexity > 0) {
      speexEncoder.getEncoder().setComplexity(complexity);
    }
    if (bitrate > 0) {
      speexEncoder.getEncoder().setBitRate(bitrate);
    }
    if (vbr) {
      speexEncoder.getEncoder().setVbr(vbr);
      if (vbr_quality > 0) {
        speexEncoder.getEncoder().setVbrQuality(vbr_quality);
      }
    }
    if (vad) {
      speexEncoder.getEncoder().setVad(vad);
    }
    if (dtx) {
      speexEncoder.getEncoder().setDtx(dtx);
    }

    // Display info
    if (printlevel <= DEBUG) {
      System.out.println("");
      System.out.println("Output File: " + destPath);
      System.out.println("File format: Ogg Speex");
      System.out.println("Encoder mode: " + (mode==0 ? "Narrowband" : (mode==1 ? "Wideband" : "UltraWideband")));
      System.out.println("Quality: " + (vbr ? vbr_quality : quality));
      System.out.println("Complexity: " + complexity);
      System.out.println("Frames per packet: " + nframes);
      System.out.println("Varible bitrate: " + vbr);
      System.out.println("Voice activity detection: " + vad);
      System.out.println("Discontinouous Transmission: " + dtx);
    }
    // Open the file writer
    AudioFileWriter writer;
    if (destFormat == FILE_FORMAT_OGG) {
      writer = new OggSpeexWriter(mode, sampleRate, channels, nframes, vbr);
    }
    else if (destFormat == FILE_FORMAT_WAVE) {
      nframes = PcmWaveWriter.WAVE_FRAME_SIZES[mode-1][channels-1][quality];
      writer = new PcmWaveWriter(mode, quality, sampleRate, channels, nframes, vbr);
    }
    else {
      writer = new RawWriter();
    }
    writer.open(destPath);
    writer.writeHeader("Encoded with: " + VERSION);
    int pcmPacketSize = 2 * channels * speexEncoder.getFrameSize();
    try {
      // read until we get to EOF
      while (true) {
        dis.readFully(temp, 0, nframes*pcmPacketSize);
        for (int i=0; i<nframes; i++)
          speexEncoder.processData(temp, i*pcmPacketSize, pcmPacketSize);
        int encsize = speexEncoder.getProcessedData(temp, 0);
        if (encsize > 0) {
          writer.writePacket(temp, 0, encsize);
        }
      }
    }
    catch (EOFException e) {}
    writer.close(); 
    dis.close();
  }
  
  /**
   * Converts Little Endian (Windows) bytes to an int (Java uses Big Endian).
   * @param data the data to read.
   * @param offset the offset from which to start reading.
   * @return the integer value of the reassembled bytes.
   */
  protected static int readInt(final byte[] data, final int offset)
  {
    return (data[offset] & 0xff) |
           ((data[offset+1] & 0xff) <<  8) |
           ((data[offset+2] & 0xff) << 16) |
           (data[offset+3] << 24); // no 0xff on the last one to keep the sign
  }

  /**
   * Converts Little Endian (Windows) bytes to an short (Java uses Big Endian).
   * @param data the data to read.
   * @param offset the offset from which to start reading.
   * @return the integer value of the reassembled bytes.
   */
  protected static int readShort(final byte[] data, final int offset)
  {
    return (data[offset] & 0xff) |
           (data[offset+1] << 8); // no 0xff on the last one to keep the sign
  }
}

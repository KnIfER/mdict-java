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
 * Class: org.xiph.speex.manyclass.JSpeexDec.java                                                      *
 *                                                                            *
 * Author: James LAWRENCE                                                     *
 * Modified by: Marc GIMPEL                                                   *
 * Based on code by: Jean-Marc VALIN                                          *
 *                                                                            *
 * Date: March 2003                                                           *
 *                                                                            *
 ******************************************************************************/

/* $Id: org.xiph.speex.manyclass.JSpeexDec.java,v 1.4 2005/05/27 13:14:38 mgimpel Exp $ */

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

import java.io.*;
import java.util.Random;

/**
 * Java Speex Command Line Decoder.
 * 
 * Decodes SPX files created by Speex's speexenc utility to WAV entirely in pure java.
 * Currently this code has been updated to be compatible with release 1.0.3.
 *
 * NOTE!!! A number of advanced options are NOT supported. 
 * 
 * --  DTX implemented but untested.
 * --  Packet loss support implemented but untested.
 * --  SPX files with more than one comment. 
 * --  Can't force decoder to run at another rate, mode, or channel count. 
 * 
 * @author Jim Lawrence, helloNetwork.com
 * @author Marc Gimpel, Wimba S.A. (mgimpel@horizonwimba.com)
 * @version $Revision: 1.4 $
 */
public class JSpeexDec
{
  /** Version of the Speex Encoder */
  public static final String VERSION = "Java Speex Command Line Decoder v0.9.7 ($Revision: 1.4 $)";
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

  /** Random number generator for packet loss simulation. */
  protected static Random random = new Random();
  /** Speex Decoder */
  protected SpeexDecoder speexDecoder;

  /** Defines whether or not the perceptual enhancement is used. */
  protected boolean enhanced  = true;
  /** If input is raw, defines the decoder mode (0=NB, 1=WB and 2-UWB). */
  private int mode          = 0;
  /** If input is raw, defines the quality setting used by the encoder. */
  private int quality       = 8;
  /** If input is raw, defines the number of frmaes per packet. */
  private int nframes       = 1;
  /** If input is raw, defines the sample rate of the audio. */
  private int sampleRate    = -1;
  /** */
  private float vbr_quality = -1;
  /** */
  private boolean vbr       = false;
  /** If input is raw, defines th number of channels (1=mono, 2=stereo). */
  private int channels      = 1;
  /** The percentage of packets to lose in the packet loss simulation. */
  private int loss          = 0;

  /** The audio input file */
  protected String srcFile;
  /** The audio output file */
  protected String destFile;

  /**
   * Builds a plain JSpeex Decoder with default values.
   */
  public JSpeexDec()
  {
  }
  


  /**
   * Parse the command line arguments.
   * @param args Command line parameters.
   * @return true if the parsed arguments are sufficient to run the decoder.
   */
  public boolean parseArgs(final String[] args)
  {
    // make sure we have command args
    if (args.length < 2) {
      if (args.length==1 && (args[0].equals("-v") || args[0].equals("--version"))) {
        version();
        return false;
      }
      usage();
      return false;
    }
    // Determine input, output and file formats
    srcFile = args[args.length-2];
    destFile = args[args.length-1];
    if (srcFile.toLowerCase().endsWith(".spx")) {
      srcFormat = FILE_FORMAT_OGG;
    }
    else if (srcFile.toLowerCase().endsWith(".wav")) {
      srcFormat = FILE_FORMAT_WAVE;
    }
    else {
      srcFormat = FILE_FORMAT_RAW;
    }
    if (destFile.toLowerCase().endsWith(".wav")) {
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
      else if (args[i].equalsIgnoreCase("--enh")) {
        enhanced = true;
      }
      else if (args[i].equalsIgnoreCase("--no-enh")) {
        enhanced = false;
      }
      else if (args[i].equalsIgnoreCase("--packet-loss")) {
        try {
          loss = Integer.parseInt(args[++i]);
          }
        catch (NumberFormatException e) {
          usage();
          return false;
        }
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
      else if (args[i].equalsIgnoreCase("--stereo")) {
        channels = 2;
      }
      else {
        usage();
        return false;
      }
    }
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
    System.out.println("Usage: org.xiph.speex.manyclass.JSpeexDec [options] input_file output_file");
    System.out.println("Where:");
    System.out.println("  input_file can be:");
    System.out.println("    filename.spx  an Ogg Speex file");
    System.out.println("    filename.wav  a Wave Speex file (beta!!!)");
    System.out.println("    filename.*    a raw Speex file");
    System.out.println("  output_file can be:");
    System.out.println("    filename.wav  a PCM wav file");
    System.out.println("    filename.*    a raw PCM file (any extension other than .wav)");
    System.out.println("Options: -h, --help     This help");
    System.out.println("         -v, --version    Version information");
    System.out.println("         --verbose        Print detailed information");
    System.out.println("         --quiet          Print minimal information");
    System.out.println("         --enh            Enable perceptual enhancement (default)");
    System.out.println("         --no-enh         Disable perceptual enhancement");
    System.out.println("         --packet-loss n  Simulate n % random packet loss");
    System.out.println("         if the input file is raw Speex (not Ogg Speex)");
    System.out.println("         -n, -nb          Narrowband (8kHz)");
    System.out.println("         -w, -wb          Wideband (16kHz)");
    System.out.println("         -u, -uwb         Ultra-Wideband (32kHz)");
    System.out.println("         --quality n      Encoding quality (0-10) default 8");
    System.out.println("         --nframes n      Number of frames per Ogg packet, default 1");
    System.out.println("         --vbr            Enable varible bit-rate (VBR)");
    System.out.println("         --stereo         Consider input as stereo");
    System.out.println("More information is available from: http://jspeex.sourceforge.net/");
    System.out.println("This code is a Java port of the Speex codec: http://www.speex.org/");
  }
  
  /**
   * Prints the version.
   */
  public static void version()
  {
    System.out.println(VERSION);
    System.out.println("using " + SpeexDecoder.VERSION);
    System.out.println(COPYRIGHT);
  }

  /**
   * Decodes a Speex file to PCM.
   * @param dis
   * @param destPath
   * @exception IOException
   */
  public void decode(final DataInputStream dis, final Object destPath, int destFormat)
    throws IOException
  {
    boolean writeToFile=destPath instanceof File;
    byte[] header    = new byte[2048];
    byte[] payload   = new byte[65536];
    byte[] decdat    = new byte[44100*2*2];
    final int    WAV_HEADERSIZE    = 8;
    final short  WAVE_FORMAT_SPEEX = (short) 0xa109;
    final String RIFF           = "RIFF";
    final String WAVE           = "WAVE";
    final String FORMAT         = "fmt ";
    final String DATA           = "data";
    final int    OGG_HEADERSIZE = 27;
    final int    OGG_SEGOFFSET  = 26;
    final String OGGID          = "OggS";
    int segments=0;
    int curseg=0;
    int bodybytes=0;
    int decsize=0; 
    int packetNo=0;
    // construct a new decoder
    speexDecoder = new SpeexDecoder();
    // open the input stream
    AudioFileWriter writer = null;
    int origchksum;
    int chksum;
    try {
      // read until we get to EOF
      while (true) {
        if (srcFormat == FILE_FORMAT_OGG) {
          // read the OGG header
          dis.readFully(header, 0, OGG_HEADERSIZE);
          origchksum = readInt(header, 22);
          header[22] = 0;
          header[23] = 0;
          header[24] = 0;
          header[25] = 0;
          chksum= OggCrc.checksum(0, header, 0, OGG_HEADERSIZE);

          // make sure its a OGG header
          if (!OGGID.equals(new String(header, 0, 4))) {
            System.err.println("missing ogg id!");
            return;
          }

          /* how many segments are there? */
          segments = header[OGG_SEGOFFSET] & 0xFF;
          dis.readFully(header, OGG_HEADERSIZE, segments);
          chksum=OggCrc.checksum(chksum, header, OGG_HEADERSIZE, segments);

          /* decode each segment, writing output to wav */
          for (curseg=0; curseg < segments; curseg++) {
            /* get the number of bytes in the segment */
            bodybytes = header[OGG_HEADERSIZE+curseg] & 0xFF;
            if (bodybytes==255) {
              System.err.println("sorry, don't handle 255 sizes!"); 
              return;
            }
            dis.readFully(payload, 0, bodybytes);
            chksum=OggCrc.checksum(chksum, payload, 0, bodybytes);
            
            /* decode the segment */
            /* if first packet, read the Speex header */
            if (packetNo == 0) {
              if (readSpeexHeader(payload, 0, bodybytes)) {
                if (printlevel <= DEBUG) {
                  System.out.println("File Format: Ogg Speex");
                  System.out.println("Sample Rate: " + sampleRate);
                  System.out.println("Channels: " + channels);
                  System.out.println("Encoder mode: " + (mode==0 ? "Narrowband" : (mode==1 ? "Wideband" : "UltraWideband")));
                  System.out.println("Frames per packet: " + nframes);
                }
                /* once Speex header read, initialize the wave writer with output format */
                if (destFormat == FILE_FORMAT_WAVE) {
                  writer = writeToFile?new PcmWaveWriter(speexDecoder.getSampleRate(), speexDecoder.getChannels())
                    :new PcmRamWaveWriter(speexDecoder.getSampleRate(), speexDecoder.getChannels(), (ByteArrayRandomOutputStream)destPath);
                  if (printlevel <= DEBUG) {
                    System.out.println("");
                    System.out.println("File Format: PCM Wave");
                    System.out.println("Perceptual Enhancement: " + enhanced);
                  }
                }
                else {
                  writer = writeToFile?new RawWriter():new RamWriter((ByteArrayRandomOutputStream)destPath);
                  if (printlevel <= DEBUG) {
                    System.out.println("");
                    System.out.println("File Format: Raw Audio");
                    System.out.println("Perceptual Enhancement: " + enhanced);
                  }
                }
                if(writeToFile)
                  writer.open((File)destPath);
                writer.writeHeader(null);
                packetNo++;
              }
              else {
                packetNo = 0;
              }
            }
            else if (packetNo == 1) { // Ogg Comment packet
                packetNo++;
            }
            else {
              if (loss>0 && random.nextInt(100)<loss) {
                speexDecoder.processData(null, 0, bodybytes);
                for (int i=1; i<nframes; i++) {
                  speexDecoder.processData(true);
                }
              }
              else {
                speexDecoder.processData(payload, 0, bodybytes);
                for (int i=1; i<nframes; i++) {
                  speexDecoder.processData(false);
                }
              }
              /* get the amount of decoded data */
              if ((decsize = speexDecoder.getProcessedData(decdat, 0)) > 0) {
                writer.writePacket(decdat, 0, decsize);
              }
              packetNo++;
            }
          }
          if (chksum != origchksum)
            throw new IOException("Ogg CheckSums do not match");
        }
        else  { // Wave or Raw Speex
          /* if first packet, initialise everything */
          if (packetNo == 0) {
            if (srcFormat == FILE_FORMAT_WAVE) {
              // read the WAVE header
              dis.readFully(header, 0, WAV_HEADERSIZE+4);
              // make sure its a WAVE header
              if (!RIFF.equals(new String(header, 0, 4)) &&
                  !WAVE.equals(new String(header, 8, 4))) {
                System.err.println("Not a WAVE file");
                return;
              }
              // Read other header chunks
              dis.readFully(header, 0, WAV_HEADERSIZE);
              String chunk = new String(header, 0, 4);
              int size = readInt(header, 4);
              while (!chunk.equals(DATA)) {
                dis.readFully(header, 0, size);
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
                  if (readShort(header, 0) != WAVE_FORMAT_SPEEX) {
                    System.err.println("Not a Wave Speex file");
                    return;
                  }
                  channels = readShort(header, 2);
                  sampleRate = readInt(header, 4);
                  bodybytes = readShort(header, 12);
                  /*
                  The extra data in the wave format are
                  18 : ACM major version number
                  19 : ACM minor version number
                  20-100 : Speex header
                  100-... : Comment ?
                  */
                  if (readShort(header, 16) < 82) {
                    System.err.println("Possibly corrupt Speex Wave file.");
                    return;
                  }
                  readSpeexHeader(header, 20, 80);
                  // Display audio info
                  if (printlevel <= DEBUG) {
                    System.out.println("File Format: Wave Speex");
                    System.out.println("Sample Rate: " + sampleRate);
                    System.out.println("Channels: " + channels);
                    System.out.println("Encoder mode: " + (mode==0 ? "Narrowband" : (mode==1 ? "Wideband" : "UltraWideband")));
                    System.out.println("Frames per packet: " + nframes);
                  }
                }
                dis.readFully(header, 0, WAV_HEADERSIZE);
                chunk = new String(header, 0, 4);
                size = readInt(header, 4);
              }
              if (printlevel <= DEBUG) System.out.println("Data size: " + size);
            }
            else {
              if (printlevel <= DEBUG) {
                System.out.println("File Format: Raw Speex");
                System.out.println("Sample Rate: " + sampleRate);
                System.out.println("Channels: " + channels);
                System.out.println("Encoder mode: " + (mode==0 ? "Narrowband" : (mode==1 ? "Wideband" : "UltraWideband")));
                System.out.println("Frames per packet: " + nframes);
              }
              /* initialize the Speex decoder */
              speexDecoder.init(mode, sampleRate, channels, enhanced);
              if (!vbr) {
                switch (mode) {
                  case 0:
                    bodybytes = NbEncoder.NB_FRAME_SIZE[NbEncoder.NB_QUALITY_MAP[quality]];
                    break;
//Wideband
                  case 1:
                    bodybytes = SbEncoder.NB_FRAME_SIZE[SbEncoder.NB_QUALITY_MAP[quality]];
                    bodybytes += SbEncoder.SB_FRAME_SIZE[SbEncoder.WB_QUALITY_MAP[quality]];
                    break;
                  case 2:
                    bodybytes = SbEncoder.NB_FRAME_SIZE[SbEncoder.NB_QUALITY_MAP[quality]];
                    bodybytes += SbEncoder.SB_FRAME_SIZE[SbEncoder.WB_QUALITY_MAP[quality]];
                    bodybytes += SbEncoder.SB_FRAME_SIZE[SbEncoder.UWB_QUALITY_MAP[quality]];
                    break;
//*/
                  default:
                    throw new IOException("Illegal mode encoundered.");
                }
                bodybytes = (bodybytes + 7) >> 3;
              }
              else {
                // We have read the stream to find out more
                bodybytes = 0;
              }
            }
            /* initialize the wave writer with output format */
            if (destFormat == FILE_FORMAT_WAVE) {
              writer = writeToFile?new PcmWaveWriter(sampleRate, channels)
                :new PcmRamWaveWriter(sampleRate, channels, (ByteArrayRandomOutputStream)destPath);
              if (printlevel <= DEBUG) {
                System.out.println("");
                System.out.println("File Format: PCM Wave");
                System.out.println("Perceptual Enhancement: " + enhanced);
              }
            }
            else {
              writer = writeToFile?new RawWriter():new RamWriter((OutputStream)destPath);
              if (printlevel <= DEBUG) {
                System.out.println("");
                System.out.println("File Format: Raw Audio");
                System.out.println("Perceptual Enhancement: " + enhanced);
              }
            }
            if(writeToFile)
              writer.open((File)destPath);
            writer.writeHeader(null);
            packetNo++;
          }
          else {
            dis.readFully(payload, 0, bodybytes);
            if (loss>0 && random.nextInt(100)<loss) {
              speexDecoder.processData(null, 0, bodybytes);
              for (int i=1; i<nframes; i++) {
                speexDecoder.processData(true);
              }
            }
            else {
              speexDecoder.processData(payload, 0, bodybytes);
              for (int i=1; i<nframes; i++) {
                speexDecoder.processData(false);
              }
            }
            /* get the amount of decoded data */
            if ((decsize = speexDecoder.getProcessedData(decdat, 0)) > 0) {
              writer.writePacket(decdat, 0, decsize);
            }
            packetNo++;
          }
        }
      }
    }
    catch (EOFException eof) {}
    /* close the output file */
    writer.close();
  }

  /**
   * Reads the header packet.
   * <pre>
   *  0 -  7: speex_string: "Speex   "
   *  8 - 27: speex_version: "speex-1.0"
   * 28 - 31: speex_version_id: 1
   * 32 - 35: header_size: 80
   * 36 - 39: rate
   * 40 - 43: mode: 0=narrowband, 1=wb, 2=uwb
   * 44 - 47: mode_bitstream_version: 4
   * 48 - 51: nb_channels
   * 52 - 55: bitrate: -1
   * 56 - 59: frame_size: 160
   * 60 - 63: vbr
   * 64 - 67: frames_per_packet
   * 68 - 71: extra_headers: 0
   * 72 - 75: reserved1
   * 76 - 79: reserved2
   * </pre>
   * @param packet
   * @param offset
   * @param bytes
   * @return
   */
  private boolean readSpeexHeader(final byte[] packet,
                                  final int offset,
                                  final int bytes)
  {
    if (bytes!=80) {
      System.out.println("Oooops");
      return false;
    }
    if (!"Speex   ".equals(new String(packet, offset, 8))) {
      return false;
    }
    mode       = packet[40+offset] & 0xFF;
    sampleRate = readInt(packet, offset+36);
    channels   = readInt(packet, offset+48);
    nframes    = readInt(packet, offset+64);
    return speexDecoder.init(mode, sampleRate, channels, enhanced);
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

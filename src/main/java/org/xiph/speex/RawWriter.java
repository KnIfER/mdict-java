/******************************************************************************
 *                                                                            *
 * Copyright (c) 1999-2004 Wimba S.A., All Rights Reserved.                   *
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
 * Class: RawWriter.java                                                      *
 *                                                                            *
 * Author: Marc GIMPEL                                                        *
 *                                                                            *
 * Date: 6th January 2004                                                     *
 *                                                                            *
 ******************************************************************************/

/* $Id: RawWriter.java,v 1.2 2004/10/21 16:21:57 mgimpel Exp $ */

package org.xiph.speex;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.FileOutputStream;

/**
 * Raw Audio File Writer.
 *
 * @author Marc Gimpel, Wimba S.A. (mgimpel@horizonwimba.com)
 * @version $Revision: 1.2 $
 */
public class RawWriter
  extends AudioFileWriter
{
  private OutputStream out;

  /**
   * Closes the output file.
   * @exception IOException if there was an exception closing the Audio Writer.
   */
  public void close()
    throws IOException 
  {
    out.close(); 
  }
  
  /**
   * Open the output file. 
   * @param file - file to open.
   * @exception IOException if there was an exception opening the Audio Writer.
   */
  public void open(final File file)
    throws IOException
  {
    file.delete(); 
    out = new FileOutputStream(file);
  }

  /**
   * Open the output file. 
   * @param filename - file to open.
   * @exception IOException if there was an exception opening the Audio Writer.
   */
  public void open(final String filename)
    throws IOException 
  {
    open(new File(filename)); 
  }

  /**
   * Writes the header pages that start the Ogg Speex file. 
   * Prepares file for data to be written.
   * @param comment description to be included in the header.
   * @exception IOException
   */
  public void writeHeader(final String comment)
    throws IOException
  {
    // a raw audio file has no header
  }

  /**
   * Writes a packet of audio. 
   * @param data audio data
   * @param offset the offset from which to start reading the data.
   * @param len the length of data to read.
   * @exception IOException
   */
  public void writePacket(final byte[] data,
                          final int offset,
                          final int len)
    throws IOException 
  {
    out.write(data, offset, len);
  }
}

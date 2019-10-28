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
 * Class: Bits.java                                                           *
 *                                                                            *
 * Author: James LAWRENCE                                                     *
 * Modified by: Marc GIMPEL                                                   *
 * Based on code by: Jean-Marc VALIN                                          *
 *                                                                            *
 * Date: March 2003                                                           *
 *                                                                            *
 ******************************************************************************/

/* $Id: Bits.java,v 1.2 2004/10/21 16:21:57 mgimpel Exp $ */

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
 * Speex bit packing and unpacking class.
 * 
 * @author Jim Lawrence, helloNetwork.com
 * @author Marc Gimpel, Wimba S.A. (mgimpel@horizonwimba.com)
 * @version $Revision: 1.2 $
 */
public class Bits
{
  /** Default buffer size */
  public static final int DEFAULT_BUFFER_SIZE = 1024;
  
  /** "raw" data */
  private byte[] bytes;
  
  /** Position of the byte "cursor" */
  private int  bytePtr;
  
  /** Position of the bit "cursor" within the current byte */
  private int  bitPtr;  
  
  /**
   * Initialise the bit packing variables.
   */
  public void init()
  {
    bytes = new byte[DEFAULT_BUFFER_SIZE];
    bytePtr=0;
    bitPtr=0;
  }

  /**
   * Advance n bits.
   * @param n - the number of bits to advance.
   */
  public void advance(final int n)
  {
    bytePtr += n >> 3;
    bitPtr += n & 7;
    if (bitPtr>7) {
      bitPtr-=8;
      bytePtr++;
    }
  }

  /**
   * Sets the buffer to the given value.
   * @param newBuffer
   */
  protected void setBuffer(final byte[] newBuffer)
  {
    bytes = newBuffer;
  }  
  
  /**
   * Take a peek at the next bit.
   * @return the next bit.
   */
  public int peek()
  {
    return ((bytes[bytePtr] & 0xFF) >> (7-bitPtr)) & 1;
  }

  /**
   * Read the given array into the buffer.
   * @param newbytes
   * @param offset
   * @param len
   */
  public void read_from(final byte[] newbytes,
                        final int offset,
                        final int len)
  {
    for (int i=0; i<len; i++)
      bytes[i]=newbytes[offset+i];
    bytePtr=0;
    bitPtr=0;
  }

  /**
   * Read the next N bits from the buffer.
   * @param nbBits - the number of bits to read.
   * @return the next N bits from the buffer.
   */
  public int unpack(int nbBits)  
  {
    int d=0;
    while (nbBits!=0) {
      d<<=1;
      d |= ((bytes[bytePtr] & 0xFF)>>(7-bitPtr))&1;
      bitPtr++;
      if (bitPtr==8) {
        bitPtr=0;
        bytePtr++;
      }
      nbBits--;
    }
    return d;
  }
  
  /**
   * Write N bits of the given data to the buffer.
   * @param data - the data to write.
   * @param nbBits - the number of bits of the data to write.
   */
  public void pack(int data, int nbBits)
  {
    int d=data;

    while (bytePtr+((nbBits+bitPtr)>>3) >= bytes.length) {
      // System.err.println("Buffer too small to pack bits");
      /* Expand the buffer as needed. */
      int size = bytes.length*2;
      byte[] tmp = new byte[size];
      System.arraycopy(bytes, 0, tmp, 0, bytes.length);
      bytes = tmp;
    }
    while (nbBits>0) {
      int bit;
      bit = (d>>(nbBits-1))&1;
      bytes[bytePtr] |= bit<<(7-bitPtr);
      bitPtr++;
      if (bitPtr==8)
      {
        bitPtr=0;
        bytePtr++;
      }
      nbBits--;
    }
  }
  
  /**
   * Returns the current buffer array.
   * @return the current buffer array.
   */
  public byte[] getBuffer()
  {
    return bytes;
  }

  /**
   * Returns the number of bytes used in the current buffer.
   * @return the number of bytes used in the current buffer.
   */
  public int getBufferSize()
  {
    return bytePtr + (bitPtr > 0 ? 1 : 0);
  }
}

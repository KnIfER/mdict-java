/** Functions to read/write raw big endian data
 *
 * Copyright (c) 2014 Bartek Fabiszewski
 * http://www.fabiszewski.net
 *
 * This file is converted from libmobi.
 * Licensed under LGPL, either version 3, or any later.
 * See <http://www.gnu.org/licenses/>
 */

package com.knziha.plod.ebook.Utils;

import com.knziha.plod.dictionary.Utils.SU;
import com.knziha.plod.ebook.Utils.CU.huff_rec_pointer;
import com.knziha.plod.ebook.mobi.*;

import java.io.File;
import java.io.FileOutputStream;

public class BU extends com.knziha.plod.dictionary.Utils.BU {

	/**
	 @brief Reads 8-bit value from MOBIBuffer
	 
	 @param[in] buf MOBIBuffer structure containing data
	 @return Read value, 0 if end of buffer is encountered
	 */
	public static short buffer_get8(MOBIBuffer buf) {
	    return (short) (buffer_getByte(buf)&0xff);
	}
	
	public static byte buffer_getByte(MOBIBuffer buf) {
	    if (buf.offset + 1 > buf.maxlen) {
	    	SU.Log("buffer_get8", "End of buffer\n");
	        buf.error = MOBI_RET.MOBI_BUFFER_END;
	        return 0;
	    }
	    return (buf.data[(int) buf.offset++]);
	}
	
	/**
	 @brief Reads 16-bit value from MOBIBuffer
	 
	 @param[in] buf MOBIBuffer structure containing data
	 @return Read value, 0 if end of buffer is encountered
	 */
	public static int buffer_get16(MOBIBuffer buf) {
	    if (buf.offset + 2 > buf.maxlen) {
	        SU.Log("buffer_get16", "End of buffer\n");
	        buf.error = MOBI_RET.MOBI_BUFFER_END;
	        return 0;
	    }
	    int ret = (int)((buf.data[(int) buf.offset]&0xff) << 8 | (buf.data[(int) (buf.offset + 1)]&0xff));//INCONGRUENT byte conversion
	    buf.offset += 2;
	    return ret;
	}
	
	/**
	 @brief Reads 32-bit value from MOBIBuffer
	 
	 @param[in] buf MOBIBuffer structure containing data
	 @return Read value, 0 if end of buffer is encountered
	 */
	public static long buffer_get32(MOBIBuffer buf) {
	    if (buf.offset + 4 > buf.maxlen) {
	        SU.Log("buffer_get32", "End of buffer\n");
	        buf.error = MOBI_RET.MOBI_BUFFER_END;
	        return 0;
	    }
	    long ret = (long) (buf.data[(int) buf.offset]&0xff) << 24 | (long) (buf.data[(int) (buf.offset + 1)]&0xff) << 16 | (long) (buf.data[(int) (buf.offset + 2)]&0xff) << 8 | (long) (buf.data[(int) (buf.offset + 3)]&0xff);
	    buf.offset += 4;
	    return ret;
	}


	/**
	 @brief Move current buffer offset by diff bytes
	 
	 @param[in,out] buf MOBIBuffer buffer containing data
	 @param[in] diff Number of bytes by which the offset is adjusted
	 */
	public static void buffer_seek(MOBIBuffer buf, int diff) {
	    long adiff = Math.abs(diff);
	    if (diff >= 0) {
	        if (buf.offset + adiff <= buf.maxlen) {
	            buf.offset += adiff;
	            return;
	        }
	    } else {
	        if (buf.offset >= adiff) {
	            buf.offset -= adiff;
	            return;
	        }
	    }
	}

	/**
	 @brief Set buffer offset to pos position
	 
	 @param[in,out] buf MOBIBuffer buffer containing data
	 @param[in] pos New position
	 */
	public static void buffer_setpos(MOBIBuffer buf, long pos) {
	    if (pos <= buf.maxlen) {
	        buf.offset = pos;
	        return;
	    }
	}
	

	/**
	 @brief Read 32-bit value from MOBIBuffer into allocated memory
	 
	 Read 32-bit value from buffer into allocated memory.
	 Returns pointer to the value, which must be freed later.
	 If the data is not accessible function will return null pointer.
	 
	 @param[out] val Pointer to value
	 @param[in] buf MOBIBuffer structure containing data
	 */
	public static long buffer_dup32(MOBIBuffer buf) {
	    if (buf.offset + 4 > buf.maxlen) {
	        return -1;
	    }
	    return buffer_get32(buf);
	}
	
	/**
	 @brief Read 16-bit value from MOBIBuffer into allocated memory
	 
	 Read 16-bit value from buffer into allocated memory.
	 Returns pointer to the value, which must be freed later.
	 If the data is not accessible function will return null pointer.
	 
	 @param[out] val Pointer to value or null pointer on failure
	 @param[in] buf MOBIBuffer structure containing data
	 */
	public static int buffer_dup16(MOBIBuffer buf) {
	    if (buf.offset + 2 > buf.maxlen) {
	        return -1;
	    }
	    return buffer_get16(buf);
	}
	
	/**
	 @brief Reads raw data from MOBIBuffer
	 
	 @param[out] data Destination to which data will be appended
	 @param[in] buf MOBIBuffer structure containing data
	 @param[in] len Length of the data to be read from buffer
	 */
	public static void buffer_getraw(byte[] data, MOBIBuffer buf, long len) {
	    if (buf.offset + len > buf.maxlen) {
	        SU.Log("%s", "End of buffer\n");
	        buf.error = MOBI_RET.MOBI_BUFFER_END;
	        return;
	    }
	    System.arraycopy(buf.data, (int) buf.offset, data, 0, (int) len);
	    buf.offset += len;
	}
	

/**
 @brief Adds 8-bit value to MOBIBuffer
 
 @param[in,out] buf MOBIBuffer structure to be filled with data
 @param[in] data Integer to be put into the buffer
 */
public static void buffer_add8(MOBIBuffer buf, byte data) {
    if (buf.offset + 1 > buf.maxlen) {
        SU.Log("%s", "Buffer full\n");
        buf.error = MOBI_RET.MOBI_BUFFER_END;
        return;
    }
    buf.data[(int) (buf.offset++)] = data;
}

/**
 @brief Adds 16-bit value to MOBIBuffer
 
 @param[in,out] buf MOBIBuffer structure to be filled with data
 @param[in] data Integer to be put into the buffer
 */
public static void buffer_add16(MOBIBuffer buf, int data) {
    if (buf.offset + 2 > buf.maxlen) {
        SU.Log("%s", "Buffer full\n");
        buf.error = MOBI_RET.MOBI_BUFFER_END;
        return;
    }
    buf.data[(int) buf.offset]=(byte) ((data&0xff00)&0xff>> 8);
    buf.data[(int) (buf.offset+1)]=(byte) (data&0xff);
    buf.offset += 2;
}

/**
 @brief Adds 32-bit value to MOBIBuffer
 
 @param[in,out] buf MOBIBuffer structure to be filled with data
 @param[in] data Integer to be put into the buffer
 */
public static void buffer_add32(MOBIBuffer buf, long data) {
    if (buf.offset + 4 > buf.maxlen) {
        SU.Log("%s", "Buffer full\n");
        buf.error = MOBI_RET.MOBI_BUFFER_END;
        return;
    }
    buf.data[(int) buf.offset] = (byte) ((data&0xff000000)&0xff>> 24);
    buf.data[(int) buf.offset+1] = (byte) ((data&0xff0000)&0xff>> 16);
    buf.data[(int) buf.offset+2] = (byte) ((data&0xff00)&0xff>> 8);
    buf.data[(int) buf.offset+3] = (byte) (data&0xff);
    buf.offset += 4;
}

/**
 @brief Free pointer to MOBIBuffer structure
 
 Free data initialized with buffer_init_null();
 Unlike buffer_free() it will not free pointer to buf.data
 
 @param[in] buf MOBIBuffer structure
 */
public static void buffer_free_null(MOBIBuffer buf) {
	if (buf == null) { return; }
}


/**
 @brief Copy raw value from one MOBIBuffer into another
 
 @param[out] dest Destination buffer
 @param[in] source Source buffer
 @param[in] len Number of bytes to copy
 */
public static void buffer_copy(MOBIBuffer dest, MOBIBuffer source, long len) {
    if (source.offset + len > source.maxlen) {
        SU.Log("buffer_copy1", "End of buffer\n");
        source.error = MOBI_RET.MOBI_BUFFER_END;
        return;
    }
    if (dest.offset + len > dest.maxlen) {
        SU.Log("buffer_copy2", "End of buffer\n");
        dest.error = MOBI_RET.MOBI_BUFFER_END;
        return;
    }
    System.arraycopy(source.data, (int)source.offset, dest.data, (int)dest.offset, (int)len);
    dest.offset += len;
    source.offset += len;
}

/**
 @brief Copy raw value within one MOBIBuffer
 
 Memmove len bytes from offset (relative to current position)
 to current position in buffer and advance buffer position.
 Data may overlap.
 
 @param[out] buf Buffer
 @param[in] offset Offset to read from
 @param[in] len Number of bytes to copy
 */
public static void buffer_move(MOBIBuffer buf, int offset, long len) {
    long aoffset = Math.abs(offset);
    long source_pointer = buf.offset;
    if (offset >= 0) {
        if (buf.offset + aoffset + len >= buf.maxlen) {
            SU.Log("buffer_move", "End of buffer\n");
            buf.error = MOBI_RET.MOBI_BUFFER_END;
            return;
        }
        source_pointer += aoffset;
    } else {
        if (buf.offset <= aoffset) {
            SU.Log("buffer_move", "End of buffer\n");
            buf.error = MOBI_RET.MOBI_BUFFER_END;
            return;
        }
        source_pointer -= aoffset;
    }
    
    memmove2(buf.data, (int)buf.offset, buf.data, (int)source_pointer, (int)len);
    buf.offset += len;
}

public static void memmove2(byte[] dest, int destPos, byte[] src, int srcPos,  int length) {
	System.arraycopy(src, srcPos, dest, destPos, length);
}

public static void memmove(byte[] dest, int destPos, byte[] src, int srcPos,  int length) {
	if(src!=dest) {
		System.arraycopy(src, srcPos, dest, destPos, length);
	}
	else {
		int count;
		if(srcPos<destPos) {
			count=length;
			while((count--)>0) {
				dest[destPos+count]=src[srcPos+count];
			}
		}
		else if(srcPos>destPos) {
			count=0;
			while(count<length) {
				dest[destPos+count]=src[srcPos+count];
				count++;
			}
		}
		
	}
}

/**
@brief Read at most 8 bytes from buffer, big-endian

If buffer data is shorter returned value is padded with zeroes

@param[in] buf MOBIBuffer structure to read from
@return 64-bit value
*/
//static long buffer_fill64(MOBIBuffer buf) {return buffer_fill64(buf, 0);}

/**
@brief Read at most 8 bytes from buffer, big-endian

If buffer data is shorter returned value is padded with zeroes

@param[in] buf MOBIBuffer structure to read from
@return 64-bit value
*/
public static long buffer_fill64(MOBIBuffer buf,int srcPointer) {
   long val = 0;
   long i = 8;
   long bytesleft = buf.maxlen - buf.offset;
   long pointer = buf.offset;
   //SU.Log("buffer_fill64_____", val, bytesleft, pointer);
   //com.knziha.plod.dictionary.Utils.BU.printBytes(buf.data, 0, 8);
   while ((i--)>0 && (bytesleft--)>0) {
	   //SU.Log(val, (long) (buf.data[(int) (srcPointer+pointer)]&0xff),i * 8,  ((long) (buf.data[(int) (srcPointer+pointer)]&0xff)) << (i * 8));
       val |= ((long) (buf.data[(int) (srcPointer+pointer++)]&0xff)) << (i * 8);
   }
   /* increase counter by 4 bytes only, 4 bytes overlap on each call */
   buf.offset += 4;
   return val;
}


/**
 @brief Reads variable length value from MOBIBuffer
 
 Internal function for wrappers: 
 buffer_get_varlen();
 buffer_get_varlen_dec();
 
 Reads maximum 4 bytes from the buffer. Stops when byte has bit 7 set.
 
 @param[in] buf MOBIBuffer structure containing data
 @param[out] len Value will be increased by number of bytes read
 @param[in] direction 1 - read buffer forward, -1 - read buffer backwards
 @return Read value, 0 if end of buffer is encountered
 */
public static long _buffer_get_varlen(MOBIBuffer buf, len_t len, int direction) {
    long val = 0;
    short byte_count = 0;
    short byte_val;
    short stop_flag = 0x80;
    short mask = 0x7f;
    long shift = 0;
    do {
        if (direction == 1) {
            if (buf.offset + 1 > buf.maxlen) {
                SU.Log("%s", "End of buffer\n");
                buf.error = MOBI_RET.MOBI_BUFFER_END;
                return val;
            }
            byte_val = buf.data[(int) buf.offset++];
            val <<= 7;
            val |= (byte_val & mask);
        } else {
            if (buf.offset < 1) {
                SU.Log("%s", "End of buffer\n");
                buf.error = MOBI_RET.MOBI_BUFFER_END;
                return val;
            }
            byte_val = buf.data[(int) buf.offset--];
            val = val | (long)(byte_val & mask) << shift;
            shift += 7;
        }        
        len.val++;
        byte_count++;
    } while ((byte_val & stop_flag)==0 && (byte_count < 4));
    return val;
}

/**
 @brief Reads variable length value from MOBIBuffer going backwards
 
 Reads maximum 4 bytes from the buffer. Stops when byte has bit 7 set.
 
 @param[in] buf MOBIBuffer structure containing data
 @param[out] len Value will be increased by number of bytes read
 @return Read value, 0 if end of buffer is encountered
 */
public static long buffer_get_varlen_dec(MOBIBuffer buf, len_t len) {
    return _buffer_get_varlen(buf, len, -1);
}


/**
 @brief Reads variable length value from MOBIBuffer
 
 Reads maximum 4 bytes from the buffer. Stops when byte has bit 7 set.
 
 @param[in] buf MOBIBuffer structure containing data
 @param[out] len Value will be increased by number of bytes read
 @return Read value, 0 if end of buffer is encountered
 */
public static long buffer_get_varlen(MOBIBuffer buf, len_t len) {
    return _buffer_get_varlen(buf, len, 1);
}

/**
 @brief Parsed data from HUFF and CDIC records needed to unpack huffman compressed text
 */
public static class MOBIHuffCdic{
    public long index_count; /**< Total number of indices in all CDIC records, stored in each CDIC record header */
    public long index_read; /**< Number of indices parsed, used by parser */
    public long code_length; /**< Code length value stored in CDIC record header */
    public long[] table1 = new long[256]; /**< Table of big-endian indices from HUFF record data1 */
    public long[] mincode_table = new long[33]; /**< Table of big-endian mincodes from HUFF record data2 */
    public long[] maxcode_table = new long[33]; /**< Table of big-endian maxcodes from HUFF record data2 */
    public long[] symbol_offsets; /**< Index of symbol offsets parsed from CDIC records (index_count entries) */
    public huff_rec_pointer[] symbols; /**< Array of pointers to start of symbols data in each CDIC record (index = number of CDIC record) */
} ;


/**
 @brief Adds raw data to MOBIBuffer
 
 @param[in,out] buf MOBIBuffer structure to be filled with data
 @param[in] data Pointer to read data
 @param[in] len Size of the read data
 */
public static void buffer_addraw(MOBIBuffer buf, byte[] data, int dataPos, long len) {
    if (buf.offset + len > buf.maxlen) {
        SU.Log("%s", "Buffer full\n");
        buf.error = MOBI_RET.MOBI_BUFFER_END;
        return;
    }
    System.arraycopy(data, dataPos,buf.data, (int)buf.offset,  (int)len);
    buf.offset += len;
}


/**
 @brief Check if buffer data header contains magic signature
 
 @param[in] buf MOBIBuffer buffer containing data
 @param[in] magic Magic signature
 @return boolean true on match, false otherwise
 */
public static boolean buffer_match_magic(MOBIBuffer buf, byte[] magic) {
  long magic_length = magic.length;
  if (buf.offset + magic_length > buf.maxlen) {
    return false;
  }
  return compareByteArrayIsPara(buf.data, (int) buf.offset, magic);
}
 
final static boolean compareByteArrayIsPara(byte[] A,int offA,byte[] B){
	if(offA+B.length>A.length)
		return false;
	for(int i=0;i<B.length;i++){
		if(A[offA+i]!=B[i])
			return false;
	}
	return true;
}

	public static void recordString(String str, String path){
		try {
			FileOutputStream fo = new FileOutputStream(new File(path), true);
			if(str==null){
				str="\n\n"+"__null__"+"\n\n";
			}
			fo.write(str.getBytes());
			fo.flush();
			fo.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}

/** Functions handling compression
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
import com.knziha.plod.ebook.Utils.BU.MOBIHuffCdic;
import com.knziha.plod.ebook.mobi.*;
import com.knziha.plod.ebook.mobi.MOBIBuffer;

public class CU {
	/** 
	 @brief Decompressor fo PalmDOC version of LZ77 compression
	
	 Decompressor based on this algorithm:
	 http://en.wikibooks.org/wiki/Data_Compression/Dictionary_compression#PalmDoc
	
	 @param[out] out Decompressed destination data
	 @param[in] in Compressed source data
	 @param[in,out] len_out Size of the memory reserved for decompressed data.
	 On return it is set to actual size of decompressed data
	 @param[in] len_in Size of compressed data
	 @return MOBI_RET status code (on success MOBI_SUCCESS)
	 */
	public static MOBI_RET mobi_decompress_lz77(byte[] out, byte[] in, len_t len_out, long len_in) {
	    MOBI_RET ret = MOBI_RET.MOBI_SUCCESS;
	    MOBIBuffer buf_in = new MOBIBuffer(in, (int) len_in);
	    MOBIBuffer buf_out = new MOBIBuffer(out, out.length);
	    while (ret == MOBI_RET.MOBI_SUCCESS && buf_in.offset < buf_in.maxlen) {
	    	byte byte_val = BU.buffer_getByte(buf_in);
	    	int byte_eval = (byte_val&0xff);
	        /* byte pair: space + char */
	        if (byte_eval >= 0xc0) {
	            BU.buffer_add8(buf_out, (byte) 0x20);
	            BU.buffer_add8(buf_out, (byte) (byte_val ^ 0x80));
	        }
	        /* length, distance pair */
	        /* 0x8000 + (distance << 3) + ((length-3) & 0x07) */
	        else if (byte_eval >= 0x80) {
	        	byte next = BU.buffer_getByte(buf_in);
	            int distance = ((((byte_eval<<8) | next&0xff) >> 3) & 0x7ff);
	            int length = (next & 0x7) + 3;
	            //while ((length--)>0) {
	            //    BU.buffer_move(buf_out, -distance, 1);
	            //}
	            BU.buffer_move(buf_out, -distance, length);
	        }
	        /* single char, not modified */
	        else if (byte_eval >= 0x09) {
	            BU.buffer_add8(buf_out, byte_val);
	        }
	        /* val chars not modified */
	        else if (byte_eval >= 0x01) {
	            BU.buffer_copy(buf_out, buf_in, byte_eval);
	        }
	        /* char '\0', not modified */
	        else {
	            BU.buffer_add8(buf_out, byte_val);
	        }
	        if (buf_in.error() || buf_out.error()) {
	            ret = MOBI_RET.MOBI_BUFFER_END;
	        }
	        //if(buf_out.offset>280) break;
	    }
	    len_out.val = buf_out.offset;
	    BU.buffer_free_null(buf_out);
	    BU.buffer_free_null(buf_in);
	    return ret;
	}
	

	
	/* FIXME: what is the reasonable value? */
   final static int MOBI_HUFFMAN_MAXDEPTH = 20; /**< Maximal recursion level for huffman decompression routine */

	
	/**
	 @brief Internal function for huff/cdic decompression
	 
	 Decompressor and HUFF/CDIC records parsing based on:
	 perl EBook::Tools::Mobipocket
	 python mobiunpack.py, calibre
	 
	 @param[out] buf_out MOBIBuffer structure with decompressed data
	 @param[in] buf_in MOBIBuffer structure with compressed data
	 @param[in] huffcdic MOBIHuffCdic structure with parsed data from huff/cdic records
	 @param[in] depth Depth of current recursion level
	 @return MOBI_RET status code (on success MOBI_SUCCESS)
	 */
	public static MOBI_RET mobi_decompress_huffman_internal(MOBIBuffer buf_out, MOBIBuffer buf_in,int srcPointer, MOBIHuffCdic huffcdic, long depth) {
	    if (depth > MOBI_HUFFMAN_MAXDEPTH) {
	        SU.Log("Too many levels of recursion: %zu\n", depth);
	        return MOBI_RET.MOBI_DATA_CORRUPT;
	    }
	    MOBI_RET ret = MOBI_RET.MOBI_SUCCESS;
	    short bitcount = 32;
	    /* this cast should be safe: max record size is 4096 */
	    int bitsleft = (int) (buf_in.maxlen * 8);
	    short code_length = 0;
	    long buffer = BU.buffer_fill64(buf_in, srcPointer);
        //SU.Log("huffman_internal0 --- ", buffer, srcPointer, buf_in.maxlen);
	    while (ret == MOBI_RET.MOBI_SUCCESS) {
	        if (bitcount <= 0) {
	            bitcount += 32;
	            buffer = BU.buffer_fill64(buf_in, srcPointer);
	        }
	        long code = (buffer >> bitcount) & 0xffffffffL;
	        /* lookup code in table1 */
	        long t1 = huffcdic.table1[(int) (code >> 24)];
	        //SU.Log("huffman_internal :: ", buffer, bitcount, (long) (code >> 24), t1);
	        /* get maxcode and codelen from t1 */
	        code_length = (short) (t1 & 0x1f);
	        long maxcode = (((t1 >> 8) + 1) << (32 - code_length)) - 1;
	        /* check termination bit */
	        if ((t1 & 0x80)==0) {
	            /* get offset from mincode, maxcode tables */
	            while (code < huffcdic.mincode_table[code_length]) {
	                code_length++;
	            }
	            maxcode = huffcdic.maxcode_table[code_length];
	        }
	        bitcount -= code_length;
	        bitsleft -= code_length;
	        if (bitsleft < 0) {
	            break;
	        }
	        /* get index for symbol offset */
	        long index = (long) (maxcode - code) >> (32 - code_length);
	        /* check which part of cdic to use */
	        int cdic_index = (int) ((long)index >> huffcdic.code_length);
	        if (index >= huffcdic.index_count) {
	            SU.Log("Wrong symbol offsets index: %u\n", index);
	            return MOBI_RET.MOBI_DATA_CORRUPT;
	        }
	        /* get offset */
	        long offset = huffcdic.symbol_offsets[(int) index];
	        long symbol_length = (((long)huffcdic.symbols[cdic_index].hold((int) offset)&0xff) << 8) |  ((long)huffcdic.symbols[cdic_index].hold((int) (offset + 1))&0xff);
	        /* 1st bit is is_decompressed flag */
	        long is_decompressed = (int) (symbol_length >> 15);
	        /* get rid of flag */
	        symbol_length &= 0x7fff;
	        if (is_decompressed!=0) {
	        	//SU.Log(" no no as",buf_out.error==MOBI_RET.MOBI_SUCCESS);
	            /* symbol is at (offset + 2), 2 bytes used earlier for symbol length */
	            BU.buffer_addraw(buf_out, huffcdic.symbols[cdic_index].holded, (int) (offset + 2+huffcdic.symbols[cdic_index].hoffest), symbol_length);
	            ret = buf_out.error;
	        } else {
	            /* symbol is compressed */
	            /* TODO cache uncompressed symbols? */
	            MOBIBuffer buf_sym = new MOBIBuffer(huffcdic.symbols[cdic_index].holded, (int) symbol_length);
	            buf_sym.offset = 0;
	            buf_sym.error = MOBI_RET.MOBI_SUCCESS;
	            ret = mobi_decompress_huffman_internal(buf_out, buf_sym,(int) (offset + 2)+huffcdic.symbols[cdic_index].hoffest, huffcdic, depth + 1);
	        }
	    }
	    return ret;
	}
	
	/**
	 @brief Decompressor for huff/cdic compressed text records
	 
	 Decompressor and HUFF/CDIC records parsing based on:
	 perl EBook::Tools::Mobipocket
	 python mobiunpack.py, calibre
	 
	 @param[out] out Decompressed destination data
	 @param[in] in Compressed source data
	 @param[in,out] len_out Size of the memory reserved for decompressed data.
	 On return it is set to actual size of decompressed data
	 @param[in] len_in Size of compressed data
	 @param[in] huffcdic MOBIHuffCdic structure with parsed data from huff/cdic records
	 @return MOBI_RET status code (on success MOBI_SUCCESS)
	 */
	public static MOBI_RET mobi_decompress_huffman(byte[] out, byte[] in, len_t len_out, long len_in, MOBIHuffCdic huffcdic) {
	    MOBIBuffer buf_in = new MOBIBuffer(in, (int) len_in);
	    MOBIBuffer buf_out = new MOBIBuffer(out, out.length);
	    buf_out.error = MOBI_RET.MOBI_SUCCESS;
	    MOBI_RET ret = mobi_decompress_huffman_internal(buf_out, buf_in, 0, huffcdic, 0);
	    len_out.val = buf_out.offset;
    	buf_in.data=null;
    	buf_out.data=null;
	    return ret;
	}

	public static class huff_rec_pointer {
		public byte[] holded;
		public int hoffest;
		public huff_rec_pointer(byte[] data, int cdicHeaderLen) {
			holded=data;
			hoffest=cdicHeaderLen;
		}
		public byte hold(int offset) {
			return holded[hoffest+offset];
		}
		
	}
}

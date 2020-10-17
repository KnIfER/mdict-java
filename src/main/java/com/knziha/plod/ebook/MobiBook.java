/** Main MOBI book class.
 * Copyright (c) 2019 KnIfER
 * Copyright (c) 2014 Bartek Fabiszewski
 * http://www.fabiszewski.net
 *
 * This file is converted from libmobi.
 * Licensed under LGPL, either version 3, or any later.
 * See <http://www.gnu.org/licenses/>
 */

package com.knziha.plod.ebook;

import com.knziha.plod.dictionary.Utils.ReusableByteOutputStream;
import com.knziha.plod.dictionary.Utils.SU;
import com.knziha.plod.dictionarymodels.mdict;
import com.knziha.plod.ebook.Utils.BU;
import com.knziha.plod.ebook.Utils.BU.MOBIHuffCdic;
import com.knziha.plod.ebook.Utils.CU;
import com.knziha.plod.ebook.mobi.*;
import org.jcodings.specific.UTF8Encoding;
import org.joni.Matcher;
import org.joni.Option;
import org.joni.Regex;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class MobiBook extends mdict {
	long file_size;
	MOBIData m;
	long compression_type;
	long text_rec_count;
	long extra_flags;
	MOBIHuffCdic huffcdic;
	ArrayList<MOBIPdbRecord> RecordInfos;
	WeakReference<ReusableByteOutputStream> bos_buffer = new WeakReference<>(new ReusableByteOutputStream());

	public MobiBook(File file, com.knziha.plod.PlainDict.PlainDictAppOptions opt) throws IOException{
		super(file, null);
		this.opt = opt;
		m = new MOBIData();
		file_size=f.length();
		FileInputStream data_in = new FileInputStream(file);

//** mobi_load_pdbheader
//**Read palm database header from file into MOBIData structure (MOBIPdbHeader)
		MOBIBuffer buf = new MOBIBuffer(mobi.PALMDB_HEADER_LEN);//BU.buffer_init(PALMDB_HEADER_LEN);
		long len = data_in.read(buf.data, 0, mobi.PALMDB_HEADER_LEN);
		if (len != mobi.PALMDB_HEADER_LEN) {
			buf.data=null;
			throw new IOException("header pares error");
		}
		m.ph = new MOBIPdbHeader();
		/* parse header */
		m.ph.name = _getstring(buf, mobi.PALMDB_NAME_SIZE_MAX);
		m.ph.attributes = BU.buffer_get16(buf);
		m.ph.version = BU.buffer_get16(buf);
		m.ph.ctime = BU.buffer_get32(buf);
		m.ph.mtime = BU.buffer_get32(buf);
		m.ph.btime = BU.buffer_get32(buf);
		m.ph.mod_num = BU.buffer_get32(buf);
		m.ph.appinfo_offset = BU.buffer_get32(buf);
		m.ph.sortinfo_offset = BU.buffer_get32(buf);
		m.ph.type = _getstring(buf, 4);
		m.ph.creator = _getstring(buf, 4);
		m.ph.uid = BU.buffer_get32(buf);
		m.ph.next_rec = BU.buffer_get32(buf);
		m.ph.rec_count = BU.buffer_get16(buf);
		//BU.buffer_free(buf);
//** head end

		if("BOOK".compareToIgnoreCase(m.ph.type)!=0 && "TEXt".compareToIgnoreCase(m.ph.type)!=0) {
			throw new IOException("MOBI_FILE_UNSUPPORTED"+m.ph.type);
		}

		if (m.ph.rec_count == 0) {
			throw new IOException("No records found");
		}

//** mobi_load_reclist
//** Read list of database records from file into MOBIData structure (MOBIPdbRecord)
		RecordInfos = new ArrayList<>(m.ph.rec_count);
		m.rec = new MOBIPdbRecord();
		MOBIPdbRecord curr = m.rec;
		RecordInfos.add(m.rec);
		for (int i = 0; i < m.ph.rec_count; i++) {
			buf = new MOBIBuffer(mobi.PALMDB_RECORD_INFO_SIZE);
			len = data_in.read(buf.data, 0, mobi.PALMDB_RECORD_INFO_SIZE);
			if (len != mobi.PALMDB_RECORD_INFO_SIZE) {
				buf.data=null;
				throw new IOException("record list pares error");
			}
			if (i > 0) {
				curr.next = new MOBIPdbRecord();
				curr = curr.next;
				RecordInfos.add(curr);
			}
			curr.offset = BU.buffer_get32(buf);
			curr.attributes = BU.buffer_get8(buf);
			short h = BU.buffer_get8(buf);
			int l = BU.buffer_get16(buf);
			curr.uid =  (long) h << 16 | l;
			curr.next = null;
			buf.data=null;
		}
//** reclist end

//** mobi_load_rec
//** Read record data and size from file into MOBIData structure (MOBIPdbRecord)
		boolean readAll=true;
		curr = m.rec;
		while (curr != null) {
			MOBIPdbRecord next;
			long size;
			if (curr.next != null) {
				next = curr.next;
				size = next.offset - curr.offset;
			} else {
				long diff = file_size - curr.offset;
				if (diff <= 0) {
					SU.Log("Wrong record size: %li\n", diff);
					throw new IOException("Error reading record data");
				}
				size = (long) diff;
				next = null;
			}
			curr.size = size;
			if(readAll){
				MOBI_RET ret = mobi_load_recdata(curr);
				if (ret != MOBI_RET.MOBI_SUCCESS) {
					mobi_free_rec(m);
					throw new IOException("Error loading record uid %i data\n"+curr.uid);
				}
			}
			//真的要全部读入吗？
			//非也, 弃录而行。
			curr = next;
			readAll=false;
		}
//** Read record data end

		mobi_parse_record0(m, 0);

		if (m.rh!=null && m.rh.encryption_type == mobi.RECORD0_OLD_ENCRYPTION) {
			/* try to set key for encryption type 1 */
			SU.Log("Trying to set key for encryption type 1%s", "\n");
			//mobi_drm_setkey(m, null);
		}

		/* if EXTH is loaded parse KF8 record0 for hybrid KF7/KF8 file */
		if (m.eh!=null) {
			long boundary_rec_number = mobi_get_kf8boundary_seqnumber(m);
			//SU.Log("???", boundary_rec_number==mobi.MOBI_NOTSET);
			if (boundary_rec_number != mobi.MOBI_NOTSET && boundary_rec_number < mobi.UINT32_MAX) {
				/* it is a hybrid KF7/KF8 file */
				m.kf8_boundary_offset = (long) boundary_rec_number;
				m.next = new MOBIData();
				/* link pdb header and records data to KF8data structure */
				m.next.ph = m.ph;
				m.next.rec = m.rec;
				m.next.drm_key = m.drm_key;
				/* close next loop */
				m.next.next = m;
				mobi_parse_record0(m.next, boundary_rec_number + 1);
				/* swap to kf8 part if use_kf8 flag is set */
				if (m.use_kf8) {
					mobi_swap_mobidata(m);
				}
			}
		}
//SU.Log("init done...");
//** post init
		/** Get offset of KF8 Boundary for KF7/KF8 hybrid file cached in MOBIData structure*/
		if (m.use_kf8 && m.kf8_boundary_offset != mobi.MOBI_NOTSET) {
			m.mobi_get_kf8offset = m.kf8_boundary_offset + 1;
		}
		text_rec_count = m.rh.text_record_count;
		compression_type = m.rh.compression_type;
		if(m.mh!=null) extra_flags = m.mh.extra_flags;
		//SU.Log(RecordInfos);
		//SU.Log(text_rec_count, RecordInfos.size());
		parseContent();
		buildContents();
	}


	public static void main(String[] args) throws IOException {
		String file = "D:\\Code\\FigureOut\\libmobi-public\\test\\test.mobi";
		file = "D:\\Downloads\\十四分之一+合集【www.d4j.cn】.azw3";
		file = "D:\\Downloads\\犹太智慧枕边书.mobi";
		file = "D:\\Downloads\\编码_隐匿在计算机软硬件背后的语言.azw_BR7KQB23ROBNK5RQIY6KHTHSP46SFR34.azw";
		com.knziha.plod.PlainDict.PlainDictAppOptions opt = new com.knziha.plod.PlainDict.PlainDictAppOptions();
		MobiBook mbook = new MobiBook(new File(file), opt);

		SU.Log("book instanced", mbook.m.rec.next, mbook.m.rec.next.size);

		//BU.recordString(mbook.getTextRecordAt(3), "D:\\record.txt");

		//mbook.checkHuffman();
		//mbook.check_rec_loaded(1);
		//RecordLogicLayer data = new RecordLogicLayer();
		//String ret = mbook.getTextRecordData(1, data);
		//BU.printFile(data.data, data.val, data.ral, "D:\\record");

		//mbook.checkHuffman();
		for (int i = 1; i < mbook.RecordInfos.size(); i++) {
			BU.recordString("----"+i+"----\n\n"+mbook.getTextRecordAt(i)+"--------\n\n", "D:\\record.txt");
		}
		//mbook.test();

		//SU.Log(mbook.contentList.size());
		//SU.Log(1, mbook.contentList.get(0).content);
		//SU.Log(1, mbook.contentList.get(0).content.length());
		//SU.Log(1, ":", mbook.contentList.get(0));

	}

	/**
	 @brief Swap KF7 and KF8 MOBIData structures in a hybrid file

	 MOBIData structures form a circular linked list in case of hybrid files.
	 By default KF8 structure is first one in the list.
	 This function puts KF7 structure on the first place, so that it starts to be used by default.

	 @param[in,out] m MOBIData structure
	 @return MOBI_RET status code (on success MOBI_SUCCESS)
	 */
	void mobi_swap_mobidata(MOBIData m) {
		MOBIData tmp = new MOBIData();
		tmp.rh = m.rh;
		tmp.mh = m.mh;
		tmp.eh = m.eh;
		m.rh = m.next.rh;
		m.mh = m.next.mh;
		m.eh = m.next.eh;
		m.next.rh = tmp.rh;
		m.next.mh = tmp.mh;
		m.next.eh = tmp.eh;
		tmp = null;
	}

	/**
	 @brief Get sequential number of KF8 Boundary record for KF7/KF8 hybrid file

	 This function gets KF8 boundary offset from EXTH header

	 @param[in] m MOBIData structure
	 @return KF8 Boundary record sequential number or MOBI_NOTSET if not found
	 */
	long mobi_get_kf8boundary_seqnumber(MOBIData m) {
		if (m == null) {
			SU.Log("%s", "Mobi structure not initialized\n");
			return mobi.MOBI_NOTSET;
		}
		MOBIExthHeader exth_tag = mobi_get_exthrecord_by_tag(m, MOBIExthTag.EXTH_KF8BOUNDARY);
		if (exth_tag != null) {
			long rec_number = mobi_decode_exthvalue(exth_tag.data, exth_tag.size);
			rec_number--;
			MOBIPdbRecord record = mobi_get_record_by_seqnumber(m, rec_number);
			check_rec_loaded(record);
			if (record!=null && record.size >= mobi.BOUNDARY_MAGIC.length() - 1) {
				if (compareByteArrayIsPara(record.data, mobi.BOUNDARY_MAGIC.getBytes(StandardCharsets.US_ASCII), mobi.BOUNDARY_MAGIC.length() - 1)) {
					//if (memcmp(record.data, mobi.BOUNDARY_MAGIC, sizeof(mobi.BOUNDARY_MAGIC) - 1) == 0) {
					return rec_number;
				}
			}
		}
		return mobi.MOBI_NOTSET;
	}

	/**
	 @brief Decode big-endian value stored in EXTH record

	 Only for EXTH records storing numeric values

	 @param[in] data Memory area storing EXTH record data
	 @param[in] size Size of EXTH record data
	 @return 32-bit value
	 */
	long mobi_decode_exthvalue(byte[] data, long size) {
		/* FIXME: EXTH numeric data is max 32-bit? */
		long val = 0;
		long i = Math.min(size, 4);
		int pointer=0;
		while ((i--)>0) {
			val |= (long) (data[pointer++]&0xff) << (i * 8);
		}
		return val;
	}


	/**
	 @brief Get EXTH record with given MOBIExthTag tag

	 @param[in] m MOBIData structure with loaded data
	 @param[in] tag MOBIExthTag EXTH record tag
	 @return Pointer to MOBIExthHeader record structure
	 */
	MOBIExthHeader mobi_get_exthrecord_by_tag(MOBIData m, MOBIExthTag tag) {
		if (m.eh == null) {
			return null;
		}
		MOBIExthHeader curr = m.eh;
		while (curr != null) {
			if (curr.tag == tag.value) {
				return curr;
			}
			curr = curr.next;
		}
		return null;
	}


	/**
	 @brief Get palm database record with given sequential number (first record has number 0)

	 @param[in] m MOBIData structure with loaded data
	 @param[in] num Sequential number
	 @return Pointer to MOBIPdbRecord record structure, null on failure
	 */
	static MOBIPdbRecord mobi_get_record_by_seqnumber(MOBIData m, long num) {
		if (m.rec == null) {
			return null;
		}
		MOBIPdbRecord curr = m.rec;
		long i = 0;
		while (curr != null) {
			if (i++ == num) {
				return curr;
			}
			curr = curr.next;
		}
		return null;
	}

	/**
	 @brief Check if loaded MOBI data is KF7/KF8 hybrid file

	 @param[in] m MOBIData structure with loaded Record(s) 0 headers
	 @return true or false
	 */
	boolean mobi_is_hybrid(MOBIData m) {
		if (m.kf8_boundary_offset != mobi.MOBI_NOTSET) {
			return true;
		}
		return false;
	}

	/**
	 @brief Check if loaded document is MOBI/BOOK Mobipocket format

	 @param[in] m MOBIData structure with loaded Record(s) 0 headers
	 @return true or false
	 */
	static boolean mobi_is_mobipocket(MOBIData m) {
		if ("BOOK".compareToIgnoreCase(m.ph.type) == 0 && "MOBI".compareToIgnoreCase(m.ph.creator) == 0) {
			return true;
		}
		return false;
	}

	/**
	 @brief Check if loaded document is dictionary

	 @param[in] m MOBIData structure with loaded mobi header
	 @return true or false
	 */
	boolean mobi_is_dictionary(MOBIData m) {
		/* FIXME: works only for old non-KF8 formats */
		if (mobi_get_fileversion(m) < 8 && mobi_exists_orth(m)) {
			SU.Log("Dictionary detected");
			return true;
		}
		return false;
	}

	/**
	 @brief Check if orth INDX is present in the loaded file

	 @param[in] m MOBIData structure loaded with MOBI data
	 @return true on success, false otherwise
	 */
	boolean mobi_exists_orth(MOBIData m) {
		if (m.mh == null) {
			return false;
		}
		if (m.mh.orth_index == -1 || m.mh.orth_index == mobi.MOBI_NOTSET) {
			return false;
		}
		return true;
	}


	/**
	 @brief Check if loaded document is encrypted

	 @param[in] m MOBIData structure with loaded Record(s) 0 headers
	 @return true or false
	 */
	static boolean mobi_is_encrypted(MOBIData m) {
		if (mobi_is_mobipocket(m) && m.rh!=null &&
				(m.rh.encryption_type == mobi.RECORD0_OLD_ENCRYPTION ||
						m.rh.encryption_type == mobi.RECORD0_MOBI_ENCRYPTION)) {
			return true;
		}
		return false;
	}

	final static boolean compareByteArrayIsPara(byte[] A,byte[] B,int len){
		for(int i=0;i<len;i++){
			if(A[i]!=B[i])
				return false;
		}
		return true;
	}
	public static boolean compareByteArrayIsPara(byte[] A,int offA,byte[] B){
		if(offA+B.length>A.length)
			return false;
		for(int i=0;i<B.length;i++){
			if(A[offA+i]!=B[i])
				return false;
		}
		return true;
	}
	/**
	 @brief Check if loaded document is Print Replica type

	 @param[in] m MOBIData structure with loaded Record(s) 0 headers
	 @return true or false
	 */
	boolean mobi_is_replica(MOBIData m) {
		if (m.rec!=null && m.rh!=null && m.rh.compression_type == mobi.RECORD0_NO_COMPRESSION) {
			MOBIPdbRecord rec = m.rec.next;
			if (rec!=null && rec.size >= mobi.REPLICA_MAGIC.length()) {
				return compareByteArrayIsPara(rec.data, mobi.REPLICA_MAGIC.getBytes(StandardCharsets.US_ASCII), mobi.REPLICA_MAGIC.length() - 1);
			}
		}
		return false;
	}

	/**
	 @brief Get mobi file version

	 @param[in] m MOBIData structure with loaded Record(s) 0 headers
	 @return MOBI document version, 1 if ancient version (no MOBI header) or MOBI_NOTSET if error
	 */
	static long mobi_get_fileversion(MOBIData m) {
		long version = 1;
		if ("BOOK".compareToIgnoreCase(m.ph.type) == 0 && "MOBI".compareToIgnoreCase(m.ph.creator) == 0) {
			if (m.mh!=null && m.mh.header_length>0) {
				long header_length = m.mh.header_length;
				if (header_length < mobi.MOBI_HEADER_V2_SIZE) {
					version = 2;
				} else if (m.mh.version > 1) {
					if ((m.mh.version > 2 && header_length < mobi.MOBI_HEADER_V3_SIZE)
							|| (m.mh.version > 3 && header_length < mobi.MOBI_HEADER_V4_SIZE)
							||(m.mh.version > 5 && header_length < mobi.MOBI_HEADER_V5_SIZE)) {
						return mobi.MOBI_NOTSET;
					}
					version = m.mh.version;
				}
			}
		}
		return version;
	}

	/**
	 @brief Is file version 8 or above

	 @param[in] m MOBIData structure with loaded Record(s) 0 headers
	 @return True if file version is 8 or greater
	 */
	boolean mobi_is_kf8(MOBIData m) {
		long version = mobi_get_fileversion(m);
		if (version != mobi.MOBI_NOTSET && version >= 8) {
			return true;
		}
		return false;
	}


	/**
	 @return
	  * @brief Reads raw data from MOBIBuffer and pads it with zero character

	 @param[out] str Destination for string read from buffer. Length must be (len + 1)
	 @param[in] buf MOBIBuffer structure containing data
	 @param[in] len Length of the data to be read from buffer
	 */
	public static String _getstring(MOBIBuffer buf, long len) {
		if (buf.offset + len > buf.maxlen) {
			SU.Log("buffer_getstring", "End of buffer\n");
			buf.error = MOBI_RET.MOBI_BUFFER_END;
			return "";//INCONGRUENT return + \0
		}
		String ret = new String(buf.data, (int)buf.offset, (int)len);
		buf.offset += len;
		return ret;
	}

	public static String _getstring(MOBIBuffer buf, long len, Charset charset) {
		if (buf.offset + len > buf.maxlen) {
			SU.Log("buffer_getstring", "End of buffer\n");
			buf.error = MOBI_RET.MOBI_BUFFER_END;
			return "";//INCONGRUENT return + \0
		}
		String ret = new String(buf.data, (int)buf.offset, (int)len, charset);
		buf.offset += len;
		return ret;
	}

	/**
	 @brief Parse MOBI header from Record 0 into MOBIData structure (MOBIMobiHeader)

	 @param[in,out] m MOBIData structure to be filled with parsed data
	 @param[in] buf MOBIBuffer buffer to read from
	 @return MOBI_RET status code (on success MOBI_SUCCESS)
	  * @throws IOException
	 */
	MOBI_RET mobi_parse_mobiheader(MOBIData m, MOBIBuffer buf) throws IOException {
		int isKF8 = 0;
		m.mh = new MOBIMobiHeader();
		m.mh.mobi_magic = _getstring(buf, 4);
		m.mh.header_length = BU.buffer_dup32(buf);
		if (!mobi.MOBI_MAGIC.equalsIgnoreCase(m.mh.mobi_magic) || m.mh.header_length == -1) {
			m.mh = null;
			return MOBI_RET.MOBI_DATA_CORRUPT;
		}
		long saved_maxlen = buf.maxlen;
		/* some old files declare zero length mobi header, try to read first 24 bytes anyway */
		long header_length = (m.mh.header_length > 0) ? m.mh.header_length : 24;
		/* read only declared MOBI header length (curr offset minus 8 already read bytes) */
		long left_length = header_length + buf.offset - 8;
		buf.maxlen = saved_maxlen < left_length ? saved_maxlen : left_length;
		m.mh.mobi_type = BU.buffer_dup32(buf);
		long encoding = BU.buffer_get32(buf);
		if (encoding == 1252) {
			//INCONGRUENT
			m.mh.text_encoding = MOBIEncoding.MOBI_CP1252;
			_charset=Charset.forName("windows-1252");
		}
		else if (encoding == 65001) {
			m.mh.text_encoding = MOBIEncoding.MOBI_UTF8;
			_charset=StandardCharsets.UTF_8;
		} else if (encoding == 65002) {
			m.mh.text_encoding = MOBIEncoding.MOBI_UTF16;
			_charset=StandardCharsets.UTF_16;
		} else {
			SU.Log("UNKNOW CHARSET");
			_charset=StandardCharsets.US_ASCII;
			//throw new IOException("Unknown encoding in mobi header: %i\n"+encoding);
		}
		m.mh.uid = BU.buffer_dup32(buf);
		m.mh.version = BU.buffer_dup32(buf);
		if (header_length >= mobi.MOBI_HEADER_V7_SIZE && m.mh.version == 8) {
			isKF8 = 1;
		}

		m.mh.orth_index = BU.buffer_dup32(buf);
		m.mh.infl_index = BU.buffer_dup32(buf);
		m.mh.names_index = BU.buffer_dup32(buf);
		m.mh.keys_index = BU.buffer_dup32(buf);
		m.mh.extra0_index = BU.buffer_dup32(buf);
		m.mh.extra1_index = BU.buffer_dup32(buf);
		m.mh.extra2_index = BU.buffer_dup32(buf);
		m.mh.extra3_index = BU.buffer_dup32(buf);
		m.mh.extra4_index = BU.buffer_dup32(buf);
		m.mh.extra5_index = BU.buffer_dup32(buf);
		m.mh.non_text_index = BU.buffer_dup32(buf);
		m.mh.full_name_offset = BU.buffer_dup32(buf);
		m.mh.full_name_length = BU.buffer_dup32(buf);
		m.mh.locale = BU.buffer_dup32(buf);
		m.mh.dict_input_lang = BU.buffer_dup32(buf);
		m.mh.dict_output_lang = BU.buffer_dup32(buf);
		m.mh.min_version = BU.buffer_dup32(buf);
		m.mh.image_index = BU.buffer_dup32(buf);
		m.mh.huff_rec_index = BU.buffer_dup32(buf);
		m.mh.huff_rec_count = BU.buffer_dup32(buf);
		m.mh.datp_rec_index = BU.buffer_dup32(buf);
		m.mh.datp_rec_count = BU.buffer_dup32(buf);
		m.mh.exth_flags = BU.buffer_dup32(buf);
		BU.buffer_seek(buf, 32); /* 32 unknown bytes */
		m.mh.unknown6 = BU.buffer_dup32(buf);
		m.mh.drm_offset = BU.buffer_dup32(buf);
		m.mh.drm_count = BU.buffer_dup32(buf);
		m.mh.drm_size = BU.buffer_dup32(buf);
		m.mh.drm_flags = BU.buffer_dup32(buf);
		BU.buffer_seek(buf, 8); /* 8 unknown bytes */
		if (isKF8==1) {
			m.mh.fdst_index = BU.buffer_dup32(buf);
		} else {
			m.mh.first_text_index = BU.buffer_dup16(buf);
			m.mh.last_text_index = BU.buffer_dup16(buf);
		}
		m.mh.fdst_section_count = BU.buffer_dup32(buf);
		m.mh.fcis_index = BU.buffer_dup32(buf);
		m.mh.fcis_count = BU.buffer_dup32(buf);
		m.mh.flis_index = BU.buffer_dup32(buf);
		m.mh.flis_count = BU.buffer_dup32(buf);
		m.mh.unknown10 = BU.buffer_dup32(buf);
		m.mh.unknown11 = BU.buffer_dup32(buf);
		m.mh.srcs_index = BU.buffer_dup32(buf);
		m.mh.srcs_count = BU.buffer_dup32(buf);
		m.mh.unknown12 = BU.buffer_dup32(buf);
		m.mh.unknown13 = BU.buffer_dup32(buf);
		BU.buffer_seek(buf, 2); /* 2 byte fill */
		m.mh.extra_flags = BU.buffer_dup16(buf);
		m.mh.ncx_index = BU.buffer_dup32(buf);
		if (isKF8==1) {
			m.mh.fragment_index = BU.buffer_dup32(buf);
			m.mh.skeleton_index = BU.buffer_dup32(buf);
		} else {
			m.mh.unknown14 = BU.buffer_dup32(buf);
			m.mh.unknown15 = BU.buffer_dup32(buf);
		}
		m.mh.datp_index = BU.buffer_dup32(buf);
		if (isKF8==1) {
			m.mh.guide_index = BU.buffer_dup32(buf);
		} else {
			m.mh.unknown16 = BU.buffer_dup32(buf);
		}
		m.mh.unknown17 = BU.buffer_dup32(buf);
		m.mh.unknown18 = BU.buffer_dup32(buf);
		m.mh.unknown19 = BU.buffer_dup32(buf);
		m.mh.unknown20 = BU.buffer_dup32(buf);
		if (buf.maxlen > buf.offset) {
			SU.Log("Skipping %zu unknown bytes in MOBI header\n", (buf.maxlen - buf.offset));
			BU.buffer_setpos(buf, buf.maxlen);
		}
		buf.maxlen = saved_maxlen;
		/* get full name stored at m.mh.full_name_offset */
		if (m.mh.full_name_offset>0 && m.mh.full_name_length>0) {
			long saved_offset = buf.offset;
			long full_name_length = Math.min(m.mh.full_name_length, mobi.MOBI_TITLE_SIZEMAX);
			BU.buffer_setpos(buf, m.mh.full_name_offset);
			m.mh.full_name=full_name_length>0?_getstring(buf, full_name_length):"";
			BU.buffer_setpos(buf, saved_offset);
		}
		return MOBI_RET.MOBI_SUCCESS;
	}


	/**
	 @brief Free all MOBIExthHeader structures and its respective data attached to MOBIData structure

	 Each MOBIExthHeader structure holds metadata and data for each EXTH record

	 @param[in,out] m MOBIData structure
	 */
	void mobi_free_eh(MOBIData m) {
		MOBIExthHeader curr, tmp;
		curr = m.eh;
		while (curr != null) {
			tmp = curr;
			curr = curr.next;
			tmp.data=null;
			tmp = null;
		}
		m.eh = null;
	}

	/**
	 @brief Parse EXTH header from Record 0 into MOBIData structure (MOBIExthHeader)

	 @param[in,out] m MOBIData structure to be filled with parsed data
	 @param[in] buf MOBIBuffer buffer to read from
	 @return MOBI_RET status code (on success MOBI_SUCCESS)
	  * @throws IOException
	 */
	MOBI_RET mobi_parse_extheader(MOBIData m, MOBIBuffer buf) throws IOException {
		long header_length = 12;
		String exth_magic = _getstring(buf, 4);
		long exth_length = BU. buffer_get32(buf) - header_length;
		long rec_count = BU. buffer_get32(buf);
		if (!exth_magic.startsWith(mobi.EXTH_MAGIC) ||
				exth_length + buf.offset > buf.maxlen ||
				rec_count == 0 || rec_count > mobi.MOBI_EXTH_MAXCNT) {
			SU.Log("%s", "Sanity checks for EXTH header failed\n");
			return MOBI_RET.MOBI_DATA_CORRUPT;
		}
		long saved_maxlen = buf.maxlen;
		buf.maxlen = exth_length + buf.offset;
		m.eh = new MOBIExthHeader();
		MOBIExthHeader curr = m.eh;
		for (long i = 0; i < rec_count; i++) {
			if (curr.data!=null) {
				curr.next = new MOBIExthHeader();
				curr = curr.next;
			}
			curr.tag = BU. buffer_get32(buf);
			/* data size = record size minus 8 bytes for uid and size */
			curr.size = BU. buffer_get32(buf) - 8;
			if (curr.size == 0) {
				SU.Log("Skip record %i, data too short\n", curr.tag);
				continue;
			}
			if (buf.offset + curr.size > buf.maxlen) {
				mobi_free_eh(m);
				SU.Log("Record %i too long\n"+curr.tag);
				return MOBI_RET.MOBI_DATA_CORRUPT;
			}
			curr.data = new byte[(int) curr.size];
			BU.buffer_getraw(curr.data, buf, curr.size);
			curr.next = null;
		}
		buf.maxlen = saved_maxlen;
		return MOBI_RET.MOBI_SUCCESS;
	}


	/**
	 @brief Parse Record 0 into MOBIData structure
	 This function will parse MOBIRecord0Header, MOBIMobiHeader and MOBIExthHeader
	 @param[in,out] m MOBIData structure to be filled with parsed data
	 @param[in] seqnumber Sequential number of the palm database record
	 @return MOBI_RET status code (on success MOBI_SUCCESS)
	  * @throws IOException
	 */
	void mobi_parse_record0(MOBIData m, long seqnumber) throws IOException {
		MOBIPdbRecord record0 = mobi_get_record_by_seqnumber(m, seqnumber);
		if (record0 == null) {
			throw new IOException("Record 0 not loaded\n");
		}
		if (record0.size < mobi.RECORD0_HEADER_LEN) {
			throw new IOException("Record 0 too short\n");
		}
		MOBIBuffer buf = new MOBIBuffer(record0);
		m.rh = new MOBIRecord0Header();
		/* parse palmdoc header */
		int compression = BU.buffer_get16(buf);
		BU.buffer_seek(buf, 2); // unused 2 bytes, zeroes
		if ((compression != mobi.RECORD0_NO_COMPRESSION &&
				compression != mobi.RECORD0_PALMDOC_COMPRESSION &&
				compression != mobi.RECORD0_HUFF_COMPRESSION)) {
			buf.data=null;
			m.rh = null;
			throw new IOException("Wrong record0 header: "+record0.data[0]+":"+record0.data[1]+":"+record0.data[2]+":"+record0.data[3]);
		}
		m.rh.compression_type = compression;
		m.rh.text_length = BU.buffer_get32(buf);
		m.rh.text_record_count = BU.buffer_get16(buf);
		m.rh.text_record_size = BU.buffer_get16(buf);
		m.rh.encryption_type = BU.buffer_get16(buf);
		m.rh.unknown1 = BU.buffer_get16(buf);
		if (mobi_is_mobipocket(m)) {
			/* parse mobi header if present  */
			if (mobi_parse_mobiheader(m, buf) == MOBI_RET.MOBI_SUCCESS) {
				/* parse exth header if present */
				mobi_parse_extheader(m, buf);
			}
		}
		buf.data=null;
	}


	/**
	 @brief Free all MOBIPdbRecord structures and its respective data attached to MOBIData structure
	 Each MOBIPdbRecord structure holds metadata and data for each pdb record
	 @param[in,out] m MOBIData structure
	 */
	static void mobi_free_rec(MOBIData m) {
		MOBIPdbRecord curr, tmp;
		curr = m.rec;
		while (curr != null) {
			tmp = curr;
			curr = curr.next;
			tmp.data=null;
			tmp = null;
		}
		m.rec = null;
	}

	/**
	 @brief Read record data from file into MOBIPdbRecord structure

	 @param[in,out] rec MOBIPdbRecord structure to be filled with read data
	 @param[in] file Filedescriptor to read from
	 @return MOBI_RET status code (on success MOBI_SUCCESS)
	  * @throws IOException
	 */
	MOBI_RET mobi_load_recdata(MOBIPdbRecord rec) throws IOException {
		DataInputStream data_in = getStreamAt(rec.offset, true);
		rec.data = new byte[(int) rec.size];
		long len = data_in.read(rec.data, 0, (int) rec.size);
		if (len < rec.size) {
			SU.Log("Truncated data in record %i\n", rec.uid);
			return MOBI_RET.MOBI_DATA_CORRUPT;
		}
		return MOBI_RET.MOBI_SUCCESS;
	}


	@Override
	public DataInputStream getStreamAt(long off, boolean forceReal) throws IOException{
		DataInputStream data_in = new DataInputStream(new FileInputStream(f));
		data_in.skip(off);
		return data_in;
	}

	//!!! dictionary interface
	@Override
	public long getNumberEntries() {
		if(contentList!=null)
			return contentList.size();
		return 1;
	}

	@Override
	public String getEntryAt(int position) {
		if(contentList!=null)
			return "第"+position+"页";
		return "index";
	}

	@Override
	public byte[] getRecordData(int position) throws IOException {
		return null;
	}

	@Override
	public String getRecordsAt(int... positions) throws IOException {
		return getRecordAt(positions[0]);
	}


	/**
	 @brief Parse HUFF record into MOBIHuffCdic structure

	 @param[in,out] huffcdic MOBIHuffCdic structure to be filled with parsed data
	 @param[in] record MOBIPdbRecord structure containing the record
	 @return MOBI_RET status code (on success MOBI_SUCCESS)
	 */
	static MOBI_RET mobi_parse_huff(MOBIHuffCdic huffcdic, MOBIPdbRecord record) {
		MOBIBuffer buf = new MOBIBuffer(record.data, (int) record.size);
		if (buf == null) {
			SU.Log("%s\n", "Memory allocation failed");
			return MOBI_RET.MOBI_MALLOC_FAILED;
		}
		String huff_magic = _getstring(buf, 4);
		long header_length = BU.buffer_get32(buf);
		if (!mobi.HUFF_MAGIC.equalsIgnoreCase(huff_magic) || header_length < mobi.HUFF_HEADER_LEN) {
			SU.Log("HUFF wrong magic: %s\n", huff_magic);
			return MOBI_RET.MOBI_DATA_CORRUPT;
		}
		long data1_offset = BU.buffer_get32(buf);
		long data2_offset = BU.buffer_get32(buf);
		/* skip little-endian table offsets */
		BU.buffer_setpos(buf, data1_offset);
		if (buf.offset + (256 * 4) > buf.maxlen) {
			SU.Log("%s", "HUFF data1 too short\n");
			return MOBI_RET.MOBI_DATA_CORRUPT;
		}
		/* read 256 indices from data1 big-endian */
		for (int i = 0; i < 256; i++) {
			huffcdic.table1[i] = BU.buffer_get32(buf);
		}
		BU.buffer_setpos(buf, data2_offset);
		if (buf.offset + (64 * 4) > buf.maxlen) {
			SU.Log("%s", "HUFF data2 too short\n");
			return MOBI_RET.MOBI_DATA_CORRUPT;
		}
		/* read 32 mincode-maxcode pairs from data2 big-endian */
		huffcdic.mincode_table[0] = 0;
		huffcdic.maxcode_table[0] = 0xFFFFFFFF;
		for (int i = 1; i < 33; i++) {
			long mincode = BU.buffer_get32(buf);
			long maxcode = BU.buffer_get32(buf);
			huffcdic.mincode_table[i] =  mincode << (32 - i);
			huffcdic.maxcode_table[i] =  ((maxcode + 1) << (32 - i)) - 1;
		}
		return MOBI_RET.MOBI_SUCCESS;
	}

	/**
	 @brief Parse CDIC record into MOBIHuffCdic structure

	 @param[in,out] huffcdic MOBIHuffCdic structure to be filled with parsed data
	 @param[in] record MOBIPdbRecord structure containing the record
	 @param[in] num Number of CDIC record in a set, starting from zero
	 @return MOBI_RET status code (on success MOBI_SUCCESS)
	 */
	static MOBI_RET mobi_parse_cdic(MOBIHuffCdic huffcdic, MOBIPdbRecord record, long num) {
		MOBIBuffer buf = new MOBIBuffer(record.data, (int) record.size);
		String cdic_magic = _getstring(buf, 4);
		long header_length = BU.buffer_get32(buf);
		if (!mobi.CDIC_MAGIC.equalsIgnoreCase(cdic_magic) || header_length < mobi.CDIC_HEADER_LEN) {
			SU.Log("CDIC wrong magic: %s or declared header length: %zu\n", cdic_magic, header_length);
			//buffer_free_null(buf);
			return MOBI_RET.MOBI_DATA_CORRUPT;
		}
		/* variables in huffcdic initialized to zero with calloc */
		/* save initial count and length */
		long index_count = BU.buffer_get32(buf);
		long code_length = BU.buffer_get32(buf);
		if (huffcdic.code_length>0 && huffcdic.code_length != code_length) {
			SU.Log("CDIC different code length %zu in record %i, previous was %zu\n", huffcdic.code_length, record.uid, code_length);
			//buffer_free_null(buf);
			return MOBI_RET.MOBI_DATA_CORRUPT;
		}
		if (huffcdic.index_count>0 && huffcdic.index_count != index_count) {
			SU.Log("CDIC different index count %zu in record %i, previous was %zu\n", huffcdic.index_count, record.uid, index_count);
			//buffer_free_null(buf);
			return MOBI_RET.MOBI_DATA_CORRUPT;
		}
		if (code_length == 0 || code_length > mobi.HUFF_CODELEN_MAX) {
			SU.Log("Code length exceeds sanity checks (%zu)\n", code_length);
			//buffer_free_null(buf);
			return MOBI_RET.MOBI_DATA_CORRUPT;
		}
		huffcdic.code_length = code_length;
		huffcdic.index_count = index_count;
		if (index_count == 0) {
			SU.Log("%s", "CDIC index count is null");
			//buffer_free_null(buf);
			return MOBI_RET.MOBI_DATA_CORRUPT;
		}
		/* allocate memory for symbol offsets if not already allocated */
		if (num == 0) {
			if (index_count > (1 << mobi.HUFF_CODELEN_MAX) * mobi.CDIC_RECORD_MAXCNT) {
				SU.Log("CDIC index count too large %zu\n", index_count);
				//buffer_free_null(buf);
				return MOBI_RET.MOBI_DATA_CORRUPT;
			}
			huffcdic.symbol_offsets = new long[(int) index_count ];//* sizeof(*huffcdic.symbol_offsets)
		}
		index_count -= huffcdic.index_read;
		/* limit number of records read to code_length bits */
		if (index_count >> code_length>0) {
			index_count = (1 << code_length);
		}
		if (buf.offset + (index_count * 2) > buf.maxlen) {
			SU.Log("CDIC indices data too short\n");
			//buffer_free_null(buf);
			return MOBI_RET.MOBI_DATA_CORRUPT;
		}
		/* read i * 2 byte big-endian indices */
		//SU.Log("while cdicting...", buf.maxlen, buf.offset);
		while ((index_count--)>0) {
			int offset = BU.buffer_get16(buf);
			long saved_pos = buf.offset;
			BU.buffer_setpos(buf, offset + mobi.CDIC_HEADER_LEN);
			long len = BU.buffer_get16(buf) & 0x7fff;
			if (buf.error !=null && buf.error != MOBI_RET.MOBI_SUCCESS || buf.offset + len > buf.maxlen) {
				//buffer_free_null(buf);
				return MOBI_RET.MOBI_DATA_CORRUPT;
			}
			BU.buffer_setpos(buf, saved_pos);
			huffcdic.symbol_offsets[(int) huffcdic.index_read++] = offset;
		}
		if (buf.offset + code_length > buf.maxlen) {
			SU.Log("%s", "CDIC dictionary data too short\n");
			//BU.//buffer_free_null(buf);
			return MOBI_RET.MOBI_DATA_CORRUPT;
		}
		/* copy pointer to data */
		huffcdic.symbols[(int) num] = new CU.huff_rec_pointer(record.data,mobi.CDIC_HEADER_LEN);
		/* free buffer */
		//buffer_free_null(buf);
		return MOBI_RET.MOBI_SUCCESS;
	}



	/**
	 @brief Calculate the size of extra bytes at the end of text record

	 @param[in] record MOBIPdbRecord structure containing the record
	 @param[in] flags Flags from MOBI header (extra_flags)
	 @return The size of trailing bytes, MOBI_NOTSET on failure
	 */
	static long mobi_get_record_extrasize(MOBIPdbRecord record, int flags) {
		long extra_size = 0;
		MOBIBuffer buf = new MOBIBuffer(record.data, (int) record.size);
		/* set pointer at the end of the record data */
		BU.buffer_setpos(buf, buf.maxlen - 1);
		for (int bit = 15; bit > 0; bit--) {
			if ((flags & (1 << bit))!=0) {
				/* bit is set */
				len_t len = new len_t();
				/* size contains varlen itself and optional data */
				long size = BU.buffer_get_varlen_dec(buf, len);
				/* skip data */
				/* TODO: read and store in record struct */
				BU.buffer_seek(buf, - (int)(size - len.val));
				extra_size += size;
			}
		};
		/* check bit 0 */
		if ((flags & 1) !=0) {
			short b = BU.buffer_get8(buf);
			/* two first bits hold size */
			extra_size += (b & 0x3) + 1;
		}
		//buffer_free_null(buf);
		return extra_size;
	}

	/**
	 @brief Get maximal size of uncompressed text record

	 @param[in] m MOBIData structure with loaded Record(s) 0 headers
	 @return Size of text or MOBI_NOTSET if error
	 */
	static long mobi_get_textrecord_maxsize(MOBIData m) {
		int max_record_size = mobi.RECORD0_TEXT_SIZE_MAX;
		if (m!=null && m.rh!=null) {
			if (m.rh.text_record_size > mobi.RECORD0_TEXT_SIZE_MAX) {
				max_record_size = m.rh.text_record_size;
			}
			if (m.mh!=null && mobi_get_fileversion(m) <= 3) {
				/* workaround for some old files with records larger than declared record size */
				long text_length = max_record_size * m.rh.text_record_count;
				if (text_length <= mobi.RAWTEXT_SIZEMAX && m.rh.text_length > text_length) {
					max_record_size = mobi.RECORD0_TEXT_SIZE_MAX * 2;
				}
			}
		}
		return max_record_size;
	}

	/**
	 @brief Parse a set of HUFF and CDIC records into MOBIHuffCdic structure

	 @param[in] m MOBIData structure with loaded MOBI document
	 @param[in,out] huffcdic MOBIHuffCdic structure to be filled with parsed data
	 @return MOBI_RET status code (on success MOBI_SUCCESS)
	 */
	MOBI_RET mobi_parse_huffdic(MOBIData m, MOBIHuffCdic huffcdic) {
		MOBI_RET ret;
		if (m.mh == null || m.mh.huff_rec_index == -1 || m.mh.huff_rec_count == 0) {
			SU.Log("HUFF/CDIC records metadata not found in MOBI header\n");
			return MOBI_RET.MOBI_DATA_CORRUPT;
		}
		long huff_rec_index = m.mh.huff_rec_index + m.mobi_get_kf8offset;
		long huff_rec_count = m.mh.huff_rec_count;
		if (huff_rec_count > mobi.HUFF_RECORD_MAXCNT) {
			SU.Log("Too many HUFF record (%zu)\n", huff_rec_count);
			return MOBI_RET.MOBI_DATA_CORRUPT;
		}
		MOBIPdbRecord curr = mobi_get_record_by_seqnumber(m, huff_rec_index);
		if (curr == null || !check_rec_loaded(curr) || huff_rec_count < 2) {
			SU.Log("%s", "HUFF/CDIC record not found\n");
			return MOBI_RET.MOBI_DATA_CORRUPT;
		}
		if (curr.size < mobi.HUFF_RECORD_MINSIZE) {
			SU.Log("HUFF record too short : ", curr.size);
			return MOBI_RET.MOBI_DATA_CORRUPT;
		}
		ret = mobi_parse_huff(huffcdic, curr);
		if (ret != MOBI_RET.MOBI_SUCCESS) {
			SU.Log("%s", "HUFF parsing failed\n");
			return ret;
		}
		curr = curr.next;
		/* allocate memory for symbols data in each CDIC record */
		huffcdic.symbols = new CU.huff_rec_pointer[(int) (huff_rec_count - 1)];//malloc((huff_rec_count - 1) * sizeof(*huffcdic.symbols));
		/* get following CDIC records */
		long i = 0;
		SU.Log("huff_rec_count is ", huff_rec_count, m.mobi_get_kf8offset, m.mh.huff_rec_index);
		while (i < huff_rec_count - 1) {
			if (curr == null || !check_rec_loaded(curr)) {
				SU.Log("%s\n", "CDIC record not found");
				return MOBI_RET.MOBI_DATA_CORRUPT;
			}
			ret = mobi_parse_cdic(huffcdic, curr, i++);
			if (ret != MOBI_RET.MOBI_SUCCESS) {
				SU.Log("%s", "CDIC parsing failed\n");
				return ret;
			}
			curr = curr.next;
		}
		return MOBI_RET.MOBI_SUCCESS;
	}

	boolean checkHuffman() {
		//long text_rec_index = 1 + compression_type;
		//MOBIPdbRecord curr = mobi_get_record_by_seqnumber(m, text_rec_index);
		if(compression_type == mobi.RECORD0_HUFF_COMPRESSION && huffcdic == null)
		{
			/* load huff/cdic tables */
			huffcdic = new MOBIHuffCdic();
			MOBI_RET ret = mobi_parse_huffdic(m, huffcdic);
			if (ret != MOBI_RET.MOBI_SUCCESS) {
				//mobi_free_huffcdic(huffcdic);
				return false;
			}
		}
		return true;
	}

	private boolean check_rec_loaded(int i) {
		return check_rec_loaded(RecordInfos.get(i));
	}

	private boolean check_rec_loaded(MOBIPdbRecord curr) {
		if(curr.data==null){
			MOBI_RET ret = null;
			try {
				ret = mobi_load_recdata(curr);
			} catch (IOException e) {
				e.printStackTrace();
			}
			if (ret != MOBI_RET.MOBI_SUCCESS) {
				return false;
			}
		}
		return true;
	}

	private String getTextRecordAt(int i) {
		RecordLogicLayer data = new RecordLogicLayer();
		String ret = getTextRecordData(i, data);
		if(ret!=null){
			SU.Log("getTextRecordData Error : "+ret);
			return "";
		}
		return new String(data.data, data.ral, data.val, _charset);
	}

	protected String getTextRecordData(int i, RecordLogicLayer retriever) {
		MOBIPdbRecord curr = RecordInfos.get(i);
		if(!check_rec_loaded(curr)) return null;
		long extra_size = 0;
		if (extra_flags>0) {
			extra_size = mobi_get_record_extrasize(curr, (int) extra_flags);
			if (extra_size == mobi.MOBI_NOTSET) {
				//mobi_free_huffcdic(huffcdic);
				return null;
			}
		}
		//#ifdef USE_ENCRYPTION
		//#endif
		if (extra_size > curr.size) {
			SU.Log("Wrong record size: -%zu\n", extra_size - curr.size);
			return "Wrong record size";
		} else if (extra_size == curr.size) {
			//curr = curr.next;
			return "Skipping empty record";
		}
		long decompressed_size = mobi_get_textrecord_maxsize(m);
		len_t len = new len_t();
		long record_size = curr.size - extra_size;
		retriever.ral=0;
		switch ((int)compression_type) {
			case mobi.RECORD0_NO_COMPRESSION:
				/* no compression */
				if (record_size > decompressed_size) {
					SU.Log("Record too large: %zu\n", record_size);
					return null;
				}
				decompressed_size = record_size;
				retriever.data=curr.data;
				retriever.val= (int) record_size;
			return null;
			case mobi.RECORD0_PALMDOC_COMPRESSION:
				byte[] decompressed =new byte[(int)decompressed_size];
				/* palmdoc lz77 compression */
				CU.mobi_decompress_lz77(decompressed, curr.data, len, record_size);
				decompressed_size=len.val;
				retriever.data=decompressed;
				retriever.val= (int)decompressed_size;
			return null;
			case mobi.RECORD0_HUFF_COMPRESSION:
				decompressed =new byte[(int)decompressed_size];
				/* mobi huffman compression */
				CU.mobi_decompress_huffman(decompressed, curr.data, len, record_size, huffcdic);
				decompressed_size=len.val;
				retriever.data=decompressed;
				retriever.val= (int)decompressed_size;
			default:
			return "Unknown compression method "+compression_type;
		}
	}

	// reading context
	int readStartBlock=1;
	int readStarOffset=0;
	int readEndBlock=1;
	int readEndOffset=0;

	@Override
	public String getRecordAt(int position) throws IOException {
		//return new String(m.rec.next.data, _charset);
		if (m.rh == null || m.rh.text_record_count == 0) {
			return "404";
		}
		if(!checkHuffman())
			return "404 : huff parse error";

		if(contentList!=null)
			return buildContent(contentList.get(position), true);

		StringBuilder sb = new StringBuilder();

		String text = getTextRecordAt(1);


		return sb.toString();
	}

	static class ContentContext{
		public WeakReference<String> content;
		int startBlock;
		int startOffset;
		int endBlock;
		int endOffset;
		@Override
		public String toString() {
			return String.format("%d#%d to %d#%d ", startBlock, startOffset, endBlock, endOffset);
		}
	}
	ArrayList<ContentContext> contentList;
	public void parseContent(){
		if(encoding==null) bakeJoniEncoding();
		SU.Log(encoding);
		String key = "<!DOCTYPE";
		byte[] pattern = key.getBytes(_charset);
		Regex Joniregex = new Regex(pattern, 0, pattern.length, Option.IGNORECASE, UTF8Encoding.INSTANCE);

		contentList = new ArrayList<>();
		ContentContext lstContext=null;
		RecordLogicLayer data = new RecordLogicLayer();
		checkHuffman();
		for (int i = 1; i < text_rec_count; i++) {
			MOBIPdbRecord curr = RecordInfos.get(i);
			check_rec_loaded(curr);
			String ret = getTextRecordData(i, data);
			if(ret!=null){
				SU.Log("getTextRecordData Error : "+ret);
				continue;
			}
			//return ret!=null?ret:new String(data.data, data.val, data.ral, _charset);
			//String text = getTextRecordAt(i);
			Matcher matcher = Joniregex.matcher(data.data, 0, data.val);
			if(i==1){
				if(matcher_match(matcher, 0, data.val, Option.DEFAULT)<0){
					key = "<mbp:pagebreak/>";
					pattern = key.getBytes(_charset);
					Joniregex = new Regex(pattern, 0, pattern.length, Option.IGNORECASE, UTF8Encoding.INSTANCE);
					matcher = Joniregex.matcher(data.data, 0, data.val);
					lstContext = new ContentContext();
					contentList.add(lstContext);
					lstContext.startBlock=0;
					lstContext.startOffset=0;
				}
			}
			int SearchStart = 0;
			while((SearchStart = matcher_search(matcher, SearchStart, data.val, Option.DEFAULT))>=0){
				ContentContext currContext = new ContentContext();
				currContext.startBlock=i;
				currContext.startOffset=SearchStart;
				contentList.add(currContext);
				if(lstContext!=null){
					lstContext.endBlock=i;
					lstContext.endOffset=SearchStart;
				}
				lstContext=currContext;
				SearchStart+=pattern.length;
				//SU.Log(i, SearchStart, data.val-SearchStart, data.val);
			}
		}
		if(lstContext!=null){
			lstContext.endBlock= (int) text_rec_count;
			lstContext.endOffset=-1;
		}
	}

	private int matcher_search(Matcher matcher, int searchStart, int i, int aDefault) {
		try {
			return matcher.search(searchStart, i, aDefault);
		} catch (Exception e) {
			//e.printStackTrace();
		}
		return -1;
	}

	private int matcher_match(Matcher matcher, int searchStart, int i, int aDefault) {
		try {
			return matcher.match(searchStart, i, aDefault);
		} catch (Exception e) {
			//e.printStackTrace();
		}
		return -1;
	}

	void buildContents(){
		checkHuffman();
		for (ContentContext cI:contentList) {
			cI.content = new WeakReference<>(buildContent(cI, true));
		}
	}

	String buildContent(ContentContext cI, boolean bUseCachedRes){
		if(bUseCachedRes && cI.content!=null && cI.content.get()!=null)
			return cI.content.get();
		checkHuffman();
		RecordLogicLayer data = new RecordLogicLayer();
		ReusableByteOutputStream bos=null;
		if(bUseCachedRes){
			if((bos=bos_buffer.get())==null)
				bos_buffer = new WeakReference<>(bos=new ReusableByteOutputStream());
			else
				bos.reset();
		}else{
			bos = new ReusableByteOutputStream();
		}
		//if(cI==contentList.get(0))SU.Log("buildContents1", cI.startBlock,cI.endBlock);
		for (int i = cI.startBlock; i <= cI.endBlock; i++) {
			check_rec_loaded(RecordInfos.get(i));

			String ret = getTextRecordData(i, data);

			//if(cI==contentList.get(0))SU.Log("buildContents2", i, text);
			int start = i == cI.startBlock ? cI.startOffset : 0;
			//SU.Log("buildContents", start, (i==cI.endBlock?cI.endOffset:data.val)-start, cI.endOffset, data.ral);
			bos.write(data.data, start, ((i==cI.endBlock&&cI.endOffset>0)?cI.endOffset:data.val)-start);
		}
		String ret = new String(bos.getBytes(), 0, bos.getCount(), _charset);
		cI.content = new WeakReference<>(ret);
		return ret;
	}

}

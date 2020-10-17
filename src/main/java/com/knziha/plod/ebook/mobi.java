package com.knziha.plod.ebook;


public class mobi {
	final static int[] INDX_TAG_GUIDE_TITLE_CNCX = new int[]{1, 0}; /**< Guide title CNCX offset */

	final static int[] INDX_TAG_NCX_FILEPOS = new int[]{1, 0}; /**< NCX filepos offset */
	final static int[] INDX_TAG_NCX_TEXT_CNCX = new int[]{3, 0}; /**< NCX text CNCX offset */
	final static int[] INDX_TAG_NCX_LEVEL = new int[]{4, 0}; /**< NCX level */
	final static int[] INDX_TAG_NCX_KIND_CNCX = new int[]{5, 0}; /**< NCX kind CNCX offset */
	final static int[] INDX_TAG_NCX_POSFID = new int[]{6, 0}; /**< NCX pos:fid */
	final static int[] INDX_TAG_NCX_POSOFF = new int[]{6, 1}; /**< NCX pos:off */
	final static int[] INDX_TAG_NCX_PARENT = new int[]{21, 0}; /**< NCX parent */
	final static int[] INDX_TAG_NCX_CHILD_START = new int[]{22, 0}; /**< NCX start child */
	final static int[] INDX_TAG_NCX_CHILD_END = new int[]{23, 0}; /**< NCX last child */

	final static int[] INDX_TAG_SKEL_COUNT = new int[]{1, 0}; /**< Skel fragments count */
	final static int[] INDX_TAG_SKEL_POSITION = new int[]{6, 0}; /**< Skel position */
	final static int[] INDX_TAG_SKEL_LENGTH = new int[]{6, 1}; /**< Skel length */

	final static int[] INDX_TAG_FRAG_AID_CNCX = new int[]{2, 0}; /**< Frag aid CNCX offset */
	final static int[] INDX_TAG_FRAG_FILE_NR = new int[]{3, 0}; /**< Frag file number */
	final static int[] INDX_TAG_FRAG_SEQUENCE_NR = new int[]{4, 0}; /**< Frag sequence number */
	final static int[] INDX_TAG_FRAG_POSITION = new int[]{6, 0}; /**< Frag position */
	final static int[] INDX_TAG_FRAG_LENGTH = new int[]{6, 1}; /**< Frag length */

	final static int[] INDX_TAG_ORTH_STARTPOS = new int[]{1, 0}; /**< Orth entry start position */
	final static int[] INDX_TAG_ORTH_ENDPOS = new int[]{2, 0}; /**< Orth entry end position */

	final static long SIZE_MAX=0xffffffffffffffffL;
	final static int INDX_TAGVALUES_MAX=100;
	final static int MOBI_EXTH_MAXCNT=1024;
	final static int MOBI_TITLE_SIZEMAX=1024;
	/**
	 @brief Usually 32-bit values in mobi records
	 with value 0xffffffff mean "value not set"
	 */
	final static long MOBI_NOTSET = 4294967295L;
	final static long UINT32_MAX = 4294967295L;

	/** @brief Magic numbers of records */
	final static String MOBI_MAGIC = "MOBI";
	final static String EXTH_MAGIC = "EXTH";
	final static String HUFF_MAGIC = "HUFF";
	final static String CDIC_MAGIC = "CDIC";
	final static String FDST_MAGIC = "FDST";
	final static String IDXT_MAGIC = "IDXT";
	final static String INDX_MAGIC = "INDX";
	final static String LIGT_MAGIC = "LIGT";
	final static String ORDT_MAGIC = "ORDT";
	final static String TAGX_MAGIC = "TAGX";
	final static String FONT_MAGIC = "FONT";
	final static String AUDI_MAGIC = "AUDI";
	final static String VIDE_MAGIC = "VIDE";
	final static String SRCS_MAGIC = "SRCS";
	final static String CMET_MAGIC = "CMET";
	final static String BOUNDARY_MAGIC = "BOUNDARY";
	//final static String EOF_MAGIC = "\xe9\x8e\r\n";
	final static String EOF_MAGIC = "\r\n";//INCONGRUENT
	final static String REPLICA_MAGIC = "%MOP";

	/** @brief Difference in seconds between epoch time and mac time */
	final static long EPOCH_MAC_DIFF = 2082844800L;

	final static int MOBI_ATTRNAME_MAXSIZE = 150; /**< Maximum length of tag attribute name, like "href" */
	final static int MOBI_ATTRVALUE_MAXSIZE = 150; /**< Maximum length of tag attribute value */

	/**
	 @defgroup mobi_pdb Params for pdb record header structure
	 @{
	 */
	final static int PALMDB_HEADER_LEN = 78;  /**< Length of header without record info headers */
	final static int PALMDB_NAME_SIZE_MAX = 32;  /**< Max length of db name stored at offset 0 */
	final static int PALMDB_RECORD_INFO_SIZE = 8;  /**< Record info header size of each pdb record */
/** @} */

	/**
	 @defgroup mobi_pdb_defs Default values for pdb record header structure
	 @{
	 */
	final static int PALMDB_ATTRIBUTE_DEFAULT = 0;
	final static int PALMDB_VERSION_DEFAULT = 0;
	final static int PALMDB_MODNUM_DEFAULT = 0;
	final static int PALMDB_APPINFO_DEFAULT = 0;
	final static int PALMDB_SORTINFO_DEFAULT = 0;
	final static String PALMDB_TYPE_DEFAULT = "BOOK";
	final static String PALMDB_CREATOR_DEFAULT = "MOBI";
	final static int PALMDB_NEXTREC_DEFAULT = 0;
/** @} */

	/**
	 @defgroup mobi_rec0 Params for record0 header structure
	 @{
	 */
	final static int RECORD0_HEADER_LEN = 16;  /**< Length of Record 0 header */
	final static int RECORD0_NO_COMPRESSION = 1;  /**< Text record compression type: none */
	final static int RECORD0_PALMDOC_COMPRESSION = 2;  /**< Text record compression type: palmdoc */
	final static int RECORD0_HUFF_COMPRESSION = 17480;  /**< Text record compression type: huff/cdic */
	final static int RECORD0_TEXT_SIZE_MAX = 4096;  /**< Max size of uncompressed text record */
	final static int RECORD0_FULLNAME_SIZE_MAX = 1024;  /**< Max size to full name string */
	final static int RECORD0_NO_ENCRYPTION = 0;  /**< Text record encryption type: none */
	final static int RECORD0_OLD_ENCRYPTION = 1;  /**< Text record encryption type: old mobipocket */
	final static int RECORD0_MOBI_ENCRYPTION = 2;  /**< Text record encryption type: mobipocket */
/** @} */

	/**
	 @defgroup mobi_len Header length / size of records
	 @{   #define (.*?) (\d{1,})(.*)    -->>  int $1 = $2; $3
	 */
	final static int CDIC_HEADER_LEN = 16;
	final static int CDIC_RECORD_MAXCNT = 1024;
	final static int HUFF_CODELEN_MAX = 16;
	final static int HUFF_HEADER_LEN = 24;
	final static int HUFF_RECORD_MAXCNT = 1024;
	final static int HUFF_RECORD_MINSIZE = 2584;
	final static int FONT_HEADER_LEN = 24;
	final static int MEDIA_HEADER_LEN = 12;
	final static int FONT_SIZEMAX = (50 * 1024 * 1024);
	final static int RAWTEXT_SIZEMAX = 0xfffffff;
	final static int MOBI_HEADER_V2_SIZE = 0x18;
	final static int MOBI_HEADER_V3_SIZE = 0x74;
	final static int MOBI_HEADER_V4_SIZE = 0xd0;
	final static int MOBI_HEADER_V5_SIZE = 0xe4;
	final static int MOBI_HEADER_V6_SIZE = 0xe4;
	final static int MOBI_HEADER_V6_EXT_SIZE = 0xe8;
	final static int MOBI_HEADER_V7_SIZE = 0xe4;
	/** @} */

	final static int INDX_LABEL_SIZEMAX = 1000; /**< Max size of index label */
	final static int INDX_INFLTAG_SIZEMAX = 25000; /**< Max size of inflections tags per entry */
	final static int INDX_INFLBUF_SIZEMAX = 500; /**< Max size of index label */
	final static int INDX_INFLSTRINGS_MAX = 500; /**< Max number of inflected strings */
	final static int ORDT_RECORD_MAXCNT = 256; /* max entries count in old ordt */
	final static int CNCX_RECORD_MAXCNT = 0xf;; /* max entries count */
	final static int INDX_RECORD_MAXCNT = 6000; /* max index entries per record */
	final static long INDX_TOTAL_MAXCNT = ((long) INDX_RECORD_MAXCNT * 0xffff); /* max total index entries */
	final static int INDX_NAME_SIZEMAX = 0xff;


	public  static class MOBIData {
		boolean use_kf8 = true; /**< Flag: if set to true (default), KF8 part of hybrid file is parsed, if false - KF7 part will be parsed */
		long kf8_boundary_offset = MOBI_NOTSET; /**< Set to KF8 boundary rec number if present, otherwise: MOBI_NOTSET */
		String drm_key; /**< key for decryption, NULL if not set */
		MOBIPdbHeader ph; /**< Palmdoc database header structure or NULL if not loaded */
		MOBIRecord0Header rh; /**< Record0 header structure or NULL if not loaded */
		MOBIMobiHeader mh; /**< MOBI header structure or NULL if not loaded */
		MOBIExthHeader eh; /**< Linked list of EXTH records or NULL if not loaded */
		MOBIPdbRecord rec; /**< Linked list of palmdoc database records or NULL if not loaded */
		MOBIData next; /**< Pointer to the other part of hybrid file or NULL if not a hybrid file */
		long mobi_get_kf8offset;
		//MOBIData(boolean val){
		//	if(val) {
		//		use_kf8 = true;
		//		kf8_boundary_offset = MOBI_NOTSET;
		//	}
		//}
	};


	/**
	 @brief Header of palmdoc database file
	 */
	public  static class MOBIPdbHeader {
		String name; /**< 0: Database name, zero terminated, trimmed title (+author) */
		int attributes; /**< 32: Attributes bitfield, PALMDB_ATTRIBUTE_DEFAULT */
		int version; /**< 34: File version, PALMDB_VERSION_DEFAULT */
		long ctime; /**< 36: Creation time */
		long mtime; /**< 40: Modification time */
		long btime; /**< 44: Backup time */
		long mod_num; /**< 48: Modification number, PALMDB_MODNUM_DEFAULT */
		long appinfo_offset; /**< 52: Offset to application info (if present) or zero, PALMDB_APPINFO_DEFAULT */
		long sortinfo_offset; /**< 56: Offset to sort info (if present) or zero, PALMDB_SORTINFO_DEFAULT */
		String type; /**< 60: Database type, zero terminated, PALMDB_TYPE_DEFAULT */
		String creator; /**< 64: Creator type, zero terminated, PALMDB_CREATOR_DEFAULT */
		long uid; /**< 68: Used internally to identify record */
		long next_rec; /**< 72: Used only when database is loaded into memory, PALMDB_NEXTREC_DEFAULT */
		int rec_count; /**< 76: Number of records in the file */
	};


	/**
	 @brief Header of the Record 0 meta-record
	 */
	public  static class MOBIRecord0Header {
		/* PalmDOC header (extended), offset 0, length 16 */
		int compression_type; /**< 0; 1 == no compression, 2 = PalmDOC compression, 17480 = HUFF/CDIC compression */
		/* uint16_t unused; // 2; 0 */
		long text_length; /**< 4; uncompressed length of the entire text of the book */
		int text_record_count; /**< 8; number of PDB records used for the text of the book */
		int text_record_size; /**< 10; maximum size of each record containing text, always 4096 */
		int encryption_type; /**< 12; 0 == no encryption, 1 = Old Mobipocket Encryption, 2 = Mobipocket Encryption */
		int unknown1; /**< 14; usually 0 */
	};

	/**
	 @brief Metadata and data of a EXTH record. All records form a linked list.
	 */
	public  static class MOBIExthHeader {
		long tag; /**< Record tag */
		long size; /**< Data size */
		byte[] data; /**< Record data */
		MOBIExthHeader next; /**< Pointer to the next record or NULL */
	};

	/**
	 @brief Metadata and data of a record. All records form a linked list.
	 */
	public  static class MOBIPdbRecord {
		long offset; /**< Offset of the record data from the start of the database */
		long size; /**< Calculated size of the record data */
		short attributes; /**< Record attributes */
		long uid; /**< Record unique id, usually sequential even numbers */
		byte[] data; /**< Record data */
		MOBIPdbRecord next; /**< Pointer to the next record or NULL */
		@Override
		public String toString() {
			return uid+"_"+(data==null);
		}
	};

	/**
	 @brief Error codes returned by functions
	 */
	public enum MOBI_RET {
		MOBI_SUCCESS (0), /**< Generic success return value */
		MOBI_ERROR (1), /**< Generic error return value */
		MOBI_PARAM_ERR (2), /**< Wrong function parameter */
		MOBI_DATA_CORRUPT (3), /**< Corrupted data */
		MOBI_FILE_NOT_FOUND (4), /**< File not found */
		MOBI_FILE_ENCRYPTED (5), /**< Unsupported encrypted data */
		MOBI_FILE_UNSUPPORTED (6), /**< Unsupported document type */
		MOBI_MALLOC_FAILED (7), /**< Memory allocation error */
		MOBI_INIT_FAILED (8), /**< Initialization error */
		MOBI_BUFFER_END (9), /**< Out of buffer error */
		MOBI_XML_ERR (10), /**< XMLwriter error */
		MOBI_DRM_PIDINV (11),  /**< Invalid DRM PID */
		MOBI_DRM_KEYNOTFOUND (12),  /**< Key not found */
		MOBI_DRM_UNSUPPORTED (13), /**< DRM support not included */
		MOBI_WRITE_FAILED (14); /**< Writing to file failed */
		public int value = 0;
		MOBI_RET(int val){
			value=val;
		}
	};


	/**
	 @defgroup mobi_enc Encoding types in MOBI header (offset 28)
	 @{
	 */
	enum MOBIEncoding {
		MOBI_CP1252(1252), /**< cp-1252 encoding */
		MOBI_UTF8(65001), /**< utf-8 encoding */
		MOBI_UTF16(65002); /**< utf-16 encoding */
		int value = 0;
		MOBIEncoding(int val){
			value=val;
		}
	} ;
	/** @} */

	/**
	 @brief EXTH record tags
	 */
	enum MOBIExthTag{
		EXTH_DRMSERVER (1),
		EXTH_DRMCOMMERCE (2),
		EXTH_DRMEBOOKBASE (3),

		EXTH_TITLE (99), /**< <dc:title> */
		EXTH_AUTHOR (100), /**< <dc:creator> */
		EXTH_PUBLISHER (101), /**< <dc:publisher> */
		EXTH_IMPRINT (102), /**< <imprint> */
		EXTH_DESCRIPTION (103), /**< <dc:description> */
		EXTH_ISBN (104), /**< <dc:identifier opf:scheme="ISBN"> */
		EXTH_SUBJECT (105), /**< <dc:subject> */
		EXTH_PUBLISHINGDATE (106), /**< <dc:date> */
		EXTH_REVIEW (107), /**< <review> */
		EXTH_CONTRIBUTOR (108), /**< <dc:contributor> */
		EXTH_RIGHTS (109), /**< <dc:rights> */
		EXTH_SUBJECTCODE (110), /**< <dc:subject BASICCode="subjectcode"> */
		EXTH_TYPE (111), /**< <dc:type> */
		EXTH_SOURCE (112), /**< <dc:source> */
		EXTH_ASIN (113),
		EXTH_VERSION (114),
		EXTH_SAMPLE (115),
		EXTH_STARTREADING (116), /**< Start reading */
		EXTH_ADULT (117), /**< <adult> */
		EXTH_PRICE (118), /**< <srp> */
		EXTH_CURRENCY (119), /**< <srp currency="currency"> */
		EXTH_KF8BOUNDARY (121),
		EXTH_FIXEDLAYOUT (122), /**< <fixed-layout> */
		EXTH_BOOKTYPE (123), /**< <book-type> */
		EXTH_ORIENTATIONLOCK (124), /**< <orientation-lock> */
		EXTH_COUNTRESOURCES (125),
		EXTH_ORIGRESOLUTION (126), /**< <original-resolution> */
		EXTH_ZEROGUTTER (127), /**< <zero-gutter> */
		EXTH_ZEROMARGIN (128), /**< <zero-margin> */
		EXTH_KF8COVERURI (129),
		EXTH_RESCOFFSET (131),
		EXTH_REGIONMAGNI (132), /**< <region-mag> */

		EXTH_DICTNAME (200), /**< <DictionaryVeryShortName> */
		EXTH_COVEROFFSET (201), /**< <EmbeddedCover> */
		EXTH_THUMBOFFSET (202),
		EXTH_HASFAKECOVER (203),
		EXTH_CREATORSOFT (204),
		EXTH_CREATORMAJOR (205),
		EXTH_CREATORMINOR (206),
		EXTH_CREATORBUILD (207),
		EXTH_WATERMARK (208),
		EXTH_TAMPERKEYS (209),

		EXTH_FONTSIGNATURE (300),

		EXTH_CLIPPINGLIMIT (401),
		EXTH_PUBLISHERLIMIT (402),
		EXTH_UNK403 (403),
		EXTH_TTSDISABLE (404),
		EXTH_UNK405 (405),
		EXTH_RENTAL (406),
		EXTH_UNK407 (407),
		EXTH_UNK450 (450),
		EXTH_UNK451 (451),
		EXTH_UNK452 (452),
		EXTH_UNK453 (453),

		EXTH_DOCTYPE (501), /**< PDOC - Personal Doc; EBOK - ebook; EBSP - ebook sample; */
		EXTH_LASTUPDATE (502),
		EXTH_UPDATEDTITLE (503),
		EXTH_ASIN504 (504),
		EXTH_TITLEFILEAS (508),
		EXTH_CREATORFILEAS (517),
		EXTH_PUBLISHERFILEAS (522),
		EXTH_LANGUAGE (524), /**< <dc:language> */
		EXTH_ALIGNMENT (525), /**< <primary-writing-mode> */
		EXTH_CREATORSTRING (526),
		EXTH_PAGEDIR (527),
		EXTH_OVERRIDEFONTS (528), /**< <override-kindle-fonts> */
		EXTH_SORCEDESC (529),
		EXTH_DICTLANGIN (531),
		EXTH_DICTLANGOUT (532),
		EXTH_INPUTSOURCE (534),
		EXTH_CREATORBUILDREV (535);
		int value = 0;
		MOBIExthTag(int val){
			value=val;
		}
	};


	/**
	 @brief Types of files stored in database records
	 */
	enum MOBIFiletype{
		T_UNKNOWN, /**< unknown */
		/* markup */
		T_HTML, /**< html */
		T_CSS, /**< css */
		T_SVG, /**< svg */
		T_OPF, /**< opf */
		T_NCX, /**< ncx */
		/* images */
		T_JPG, /**< jpg */
		T_GIF, /**< gif */
		T_PNG, /**< png */
		T_BMP, /**< bmp */
		/* fonts */
		T_OTF, /**< otf */
		T_TTF, /**< ttf */
		/* media */
		T_MP3, /**< mp3 */
		T_MPG, /**< mp3 */
		T_PDF, /**< pdf */
		/* generic types */
		T_FONT, /**< encoded font */
		T_AUDIO, /**< audio resource */
		T_VIDEO, /**< video resource */
		T_BREAK/**< end of file */
	} ;


	/**
	 @brief Buffer to read to/write from
	 */
	public  static class MOBIBuffer {
		public MOBIBuffer(int palmdbHeaderLen) {
			data = new byte[palmdbHeaderLen];
			maxlen=palmdbHeaderLen;
		}
		public MOBIBuffer(MOBIPdbRecord record0) {
			data = record0.data;
			maxlen=record0.size;
		}
		public MOBIBuffer(byte[] in, int length) {
			data = in;
			maxlen=length;
		}
		public long offset; /**< Current offset in respect to buffer start */
		public long maxlen; /**< Length of the buffer data */
		public byte[] data; /**< Pointer to buffer data */
		public MOBI_RET error; /**< MOBI_SUCCESS = 0 if operation on buffer is successful, non-zero value on failure */
		public boolean error() {
			return error!=null && error.value!=0;
		}
	};



	/**
	 @brief MOBI header which follows Record 0 header

	 All MOBI header fields are pointers. Some fields are not present in the header, then the pointer is NULL.
	 */
	public  static class MOBIMobiHeader {
		/* MOBI header, offset 16 */
		String mobi_magic; /**< 16: M O B I { 77, 79, 66, 73 }, zero terminated */
		long header_length; /**< 20: the length of the MOBI header, including the previous 4 bytes */
		long mobi_type; /**< 24: mobipocket file type */
		MOBIEncoding text_encoding; /**< 28: 1252 = CP1252, 65001 = UTF-8 */
		long uid; /**< 32: unique id */
		long version; /**< 36: mobipocket format */
		long orth_index=-1; /**< 40: section number of orthographic meta index. MOBI_NOTSET if index is not available. */
		long infl_index; /**< 44: section number of inflection meta index. MOBI_NOTSET if index is not available. */
		long names_index; /**< 48: section number of names meta index. MOBI_NOTSET if index is not available. */
		long keys_index; /**< 52: section number of keys meta index. MOBI_NOTSET if index is not available. */
		long extra0_index; /**< 56: section number of extra 0 meta index. MOBI_NOTSET if index is not available. */
		long extra1_index; /**< 60: section number of extra 1 meta index. MOBI_NOTSET if index is not available. */
		long extra2_index; /**< 64: section number of extra 2 meta index. MOBI_NOTSET if index is not available. */
		long extra3_index; /**< 68: section number of extra 3 meta index. MOBI_NOTSET if index is not available. */
		long extra4_index; /**< 72: section number of extra 4 meta index. MOBI_NOTSET if index is not available. */
		long extra5_index; /**< 76: section number of extra 5 meta index. MOBI_NOTSET if index is not available. */
		long non_text_index; /**< 80: first record number (starting with 0) that's not the book's text */
		long full_name_offset; /**< 84: offset in record 0 (not from start of file) of the full name of the book */
		long full_name_length; /**< 88: length of the full name */
		long locale; /**< 92: first byte is main language: 09 = English, next byte is dialect, 08 = British, 04 = US */
		long dict_input_lang; /**< 96: input language for a dictionary */
		long dict_output_lang; /**< 100: output language for a dictionary */
		long min_version; /**< 104: minimum mobipocket version support needed to read this file. */
		long image_index; /**< 108: first record number (starting with 0) that contains an image (sequential) */
		long huff_rec_index=-1; /**< 112: first huffman compression record */
		long huff_rec_count; /**< 116: huffman compression records count */
		long datp_rec_index; /**< 120: section number of DATP record */
		long datp_rec_count; /**< 124: DATP records count */
		long exth_flags; /**< 128: bitfield. if bit 6 (0x40) is set, then there's an EXTH record */
		/* 32 unknown bytes 0? */
		/* unknown2 */
		/* unknown3 */
		/* unknown4 */
		/* unknown5 */
		long unknown6; /**< 164: use MOBI_NOTSET */
		long drm_offset; /**< 168: offset to DRM key info in DRMed files. MOBI_NOTSET if no DRM */
		long drm_count; /**< 172: number of entries in DRM info */
		long drm_size; /**< 176: number of bytes in DRM info */
		long drm_flags; /**< 180: some flags concerning DRM info */
		/* 8 unknown bytes 0? */
		/* unknown7 */
		/* unknown8 */
		int first_text_index; /**< 192: section number of first text record */
		int last_text_index; /**< 194: */
		long fdst_index; /**< 192 (KF8) section number of FDST record */
		//long unknown9; /**< 196: */
		long fdst_section_count; /**< 196 (KF8) */
		long fcis_index; /**< 200: section number of FCIS record */
		long fcis_count; /**< 204: FCIS records count */
		long flis_index; /**< 208: section number of FLIS record */
		long flis_count; /**< 212: FLIS records count */
		long unknown10; /**< 216: */
		long unknown11; /**< 220: */
		long srcs_index; /**< 224: section number of SRCS record */
		long srcs_count; /**< 228: SRCS records count */
		long unknown12; /**< 232: */
		long unknown13; /**< 236: */
		/* uint16_t fill 0 */
		int extra_flags; /**< 242: extra flags */
		long ncx_index; /**< 244: section number of NCX record  */
		long unknown14; /**< 248: */
		long fragment_index; /**< 248 (KF8) section number of fragments record */
		long unknown15; /**< 252: */
		long skeleton_index; /**< 252 (KF8) section number of SKEL record */
		long datp_index; /**< 256: section number of DATP record */
		long unknown16; /**< 260: */
		long guide_index=-1; /**< 260 (KF8) section number of guide record */
		long unknown17; /**< 264: */
		long unknown18; /**< 268: */
		long unknown19; /**< 272: */
		long unknown20; /**< 276: */
		String full_name; /**< variable offset (full_name_offset): full name */
	};


	/**
	 @brief Tag entries in TAGX section (for internal INDX parsing)
	 */
	public  static class TAGXTags {
		short tag; /**< Tag */
		short values_count; /**< Number of values */
		short bitmask; /**< Bitmask */
		short control_byte; /**< EOF control byte */
	} ;


	/**
	 @brief Parsed TAGX section (for internal INDX parsing)

	 TAGX tags hold metadata of index entries.
	 It is present in the first index record.
	 */
	public  static class MOBITagx {
		TAGXTags[] tags; /**< Array of tag entries */
		long tags_count; /**< Number of tag entries */
		long control_byte_count; /**< Number of control bytes */
	};


	/**
	 @brief Parsed ORDT sections (for internal INDX parsing)

	 ORDT sections hold data for decoding index labels.
	 It is mapping of encoded chars to unicode.
	 */
	public  static class MOBIOrdt {
		short[] ordt1; /**< ORDT1 offsets */
		int[] ordt2; /**< ORDT2 offsets */
		long type; /**< Type (0: 16, 1: 8 bit offsets) */
		long ordt1_pos; /**< Offset of ORDT1 data */
		long ordt2_pos; /**< Offset of ORDT2 data */
		long offsets_count; /**< Offsets count */
	} ;


	/**
	 @brief Parsed FDST record

	 FDST record contains offsets of main sections in RAWML - raw text data.
	 The sections are usually html part, css parts, svg part.
	 */
	public  static class MOBIFdst {
		long fdst_section_count; /**< Number of main sections */
		long fdst_section_starts; /**< Array of section start offsets */
		long fdst_section_ends; /**< Array of section end offsets */
	} ;

	/**
	 @brief Parsed tag for an index entry
	 */
	public  static class MOBIIndexTag {
		long tagid; /**< Tag id */
		long tagvalues_count; /**< Number of tag values */
		long[] tagvalues; /**< Array of tag values */
	} ;

	/**
	 @brief Parsed INDX index entry
	 */
	public  static class MOBIIndexEntry {
		String label; /**< Entry string, zero terminated */
		long tags_count; /**< Number of tags */
		MOBIIndexTag[] tags; /**< Array of tags */
		public String toString(){
			return label+"_"+tags_count;
		}
	} ;



	/**
	 @brief Parsed INDX record
	 */
	public  static class MOBIIndx {
		long type; /**< Index type: 0 - normal, 2 - inflection */
		long entries_count; /**< Index entries count */
		int encoding; /**< Index encoding MOBIEncoding */
		long total_entries_count; /**< Total index entries count */
		long ordt_offset; /**< ORDT offset */
		long ligt_offset; /**< LIGT offset */
		long ligt_entries_count; /**< LIGT index entries count */
		long cncx_records_count; /**< Number of compiled NCX records */
		MOBIPdbRecord cncx_record; /**< Link to CNCX record */
		MOBIIndexEntry[] entries; /**< Index entries array */
		String orth_index_name; /**< Orth index name */
	} ;


	/**
	 @brief Parsed IDXT section (for internal INDX parsing)

	 IDXT section holds offsets to index entries
	 */
	static class MOBIIdxt {
		long[] offsets; /**< Offsets to index entries */
		long offsets_count; /**< Offsets count */
	};


	/**
	 @brief Metadata of file types
	 */
	static class MOBIFileMeta {
		MOBIFiletype type; /**< MOBIFiletype type */
		String extension; /**< file extension */
		String mime_type; /**< mime-type */
	};


	/**
	 @brief Reconstructed source file.

	 All file parts are organized in a linked list.
	 */
	static class MOBIPart {
		long uid; /**< Unique id */
		MOBIFiletype type; /**< File type */
		long size; /**< File size */
		byte[] data; /**< File data */
		//int offset; /**< File data */
		MOBIPart next; /**< Pointer to next part or NULL */
		public String toString() {
			return "MOBIPart# "+uid+" "+" ~ "+size;
		}
	} ;


	static class MOBIPtagx {
		short tag;
		short tag_value_count;
		long value_count;
		long value_bytes;
	} ;


	/**
	 @brief HTML attribute type
	 */
	enum MOBIAttrType{
		ATTR_ID(0), /**< Attribute 'id' */
		ATTR_NAME(1); /**< Attribute 'name' */
		int value;
		MOBIAttrType(int val) {
			value=val;
		}
		public void set(MOBIAttrType opt_attr) {
			value=opt_attr.value;
		}
	} ;



	/** @brief NCX index entry structure */
	public static class NCX {
		public NCX(long id2, String text2, String target2, long level2, long parent2, long first_child2, long last_child2) {
			id = id2;
			text = text2;
			target = target2;
			level = level2;
			parent = parent2;
			first_child = first_child2;
			last_child = last_child2;
		}
		long id; /**< Sequential id */
		String text; /**< Entry text content */
		String target; /**< Entry target reference */
		long level; /**< Entry level */
		long parent; /**< Entry parent */
		long first_child; /**< First child id */
		long last_child; /**< Last child id */
	} ;
/** @} */

	public static class len_t{
		public long val;
	}
}

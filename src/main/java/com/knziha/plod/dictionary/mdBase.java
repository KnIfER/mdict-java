/*  Copyright 2018 KnIfER

	Licensed under the Apache License, Version 2.0 (the "License");
	you may not use this file except in compliance with the License.
	You may obtain a copy of the License at

	    http://www.apache.org/licenses/LICENSE-2.0

	Unless required by applicable law or agreed to in writing, software
	distributed under the License is distributed on an "AS IS" BASIS,
	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
	See the License for the specific language governing permissions and
	limitations under the License.

	Mdict-Java Query Library
*/

package com.knziha.plod.dictionary;

import com.knziha.plod.dictionary.Utils.*;
import com.knziha.rbtree.RBTree;
import org.anarres.lzo.*;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;
import java.util.zip.InflaterOutputStream;

import static com.knziha.plod.dictionary.Utils.BU.calcChecksum;

//import org.jvcompress.lzo.MiniLZO;
//import org.jvcompress.util.MInt;
//import test.CMN;


abstract class mdBase {
	protected File f;
	public File f() {return f;}
	final static byte[] _zero4 = new byte[]{0,0,0,0};
	final static byte[] _1zero3 = new byte[]{1,0,0,0};
	final static byte[] _2zero3 = new byte[]{2,0,0,0};
	final static String emptyStr = "";
	public Boolean isCompact = true;
	public Boolean isStripKey = true;
	protected Boolean isKeyCaseSensitive = false;
	/** encryption flag
	   0x00 - no encryption
	   0x01 - encrypt record block
	   0x02 - encrypt key info block*/
	int _encrypt;
	Charset _charset;public Charset getCharset(){return _charset;}
	String _encoding="UTF-16LE";public String getEncoding(){return _encoding;}
	protected int delimiter_width = 1;
	String _passcode = "";

	float _version;
	int _number_width;
	long _num_entries;public long getNumberEntries(){return _num_entries;}
	long _num_key_blocks;public long get_num_key_blocks(){return _num_key_blocks;}
	protected long _num_record_blocks;public long get_num_record_blocks(){return _num_record_blocks;}

	RBTree<myCpr<Integer, Integer>> accumulation_blockId_tree = new RBTree<>();
	long _key_block_size,_key_block_info_size,_key_block_info_decomp_size,_record_block_size;

	int _key_block_offset;
	long _record_block_offset;
	long _record_block_start;

	key_info_struct[] _key_block_info_list;
	record_info_struct[] _record_info_struct_list;

	int rec_decompressed_size;
	long maxComRecSize;
	long maxDecompressedSize;
	//public long maxComKeyBlockSize;
	/** Maximum size of one record block */
	public long maxDecomKeyBlockSize;
	public long maxComKeyBlockSize;
	/** data buffer that holds one record bock of maximum possible size for this dictionary */

	protected DataInputStream getStreamAt(long at) throws IOException {
		DataInputStream data_in1 = new DataInputStream(mOpenInputStream());
		if(at>0) {
			long yue=0;
			while(yue<at) {
				yue+=data_in1.skip(at-yue);
			}
		}
		return data_in1;
	}


	protected InputStream mOpenInputStream() throws IOException {
		//return new FileInputStream(f);
		return new BufferedInputStream(new FileInputStream(f));
	}


	protected HashMap<String,String[]> _stylesheet = new HashMap<>();
	public int lenSty=0;

	//构造
	mdBase(String fn) throws IOException  {
		if(fn==null){
			initLogically();
			return;
		}
		//![0]File in
		f = new File(fn);

		if(StreamAvailable())
			init();
	}

	protected boolean StreamAvailable() {
		return true;
	}

	protected void init()  throws IOException {
		DataInputStream data_in = getStreamAt(0);
		//![1]read_header
		// number of bytes of header text
		byte[] itemBuf = new byte[4];
		data_in.read(itemBuf, 0, 4);
		int header_bytes_size = BU.toInt(itemBuf,0);
		_key_block_offset = 4 + header_bytes_size + 4;
		byte[] header_bytes = new byte[header_bytes_size];
		data_in.read(header_bytes,0, header_bytes_size);

		// 4 bytes: adler32 checksum of header, in little endian
		itemBuf = new byte[4];
		data_in.read(itemBuf, 0, 4);
		int alder32 = getInt(itemBuf[3],itemBuf[2],itemBuf[1],itemBuf[0]);
		//assert alder32 == (BU.calcChecksum(header_bytes)& 0xffffffff);
		if((calcChecksum(header_bytes)& 0xffffffff) != alder32) throw new IOException("unrecognized format");

		//data_in.skipBytes(4);
		//不必关闭文件流 data_in


		Pattern re = Pattern.compile("(\\w+)=[\"](.*?)[\"]",Pattern.DOTALL);
		String headerString = new String(header_bytes, StandardCharsets.UTF_16LE);
		//SU.Log("headerString::", headerString);
		Matcher m = re.matcher(headerString);
		_header_tag = new HashMap<>();
		while(m.find()) {
			_header_tag.put(m.group(1), m.group(2));
		}

		String valueTmp = _header_tag.get("Compact");
		if(valueTmp==null)
			valueTmp = _header_tag.get("Compat");
		if(valueTmp!=null)
			isCompact = !(valueTmp.equals("No"));


		valueTmp = _header_tag.get("StripKey");
		if(valueTmp!=null)
			isStripKey = valueTmp.length()==3;

		valueTmp = _header_tag.get("KeyCaseSensitive");
		if(valueTmp!=null)
			isKeyCaseSensitive = valueTmp.length()==3;

		valueTmp = _header_tag.get("Encoding");
		if(valueTmp!=null && !valueTmp.equals(""))
			_encoding = valueTmp.toUpperCase();

		if(_encoding.equals("GBK")|| _encoding.equals("GB2312")) _encoding = "GB18030";// GB18030 > GBK > GB2312
		if (_encoding.equals("")) _encoding = "UTF-8";
		if(_encoding.equals("UTF-16")) _encoding = "UTF-16LE"; //INCONGRUENT java charset

		_charset = Charset.forName(_encoding);

		if(_encoding.startsWith("UTF-16"))
			delimiter_width = 2;
		else
			delimiter_width = 1;


		String EncryptedFlag = _header_tag.get("Encrypted");
		if(EncryptedFlag!=null){
			_encrypt=IU.parsint(EncryptedFlag, -1);
			if(_encrypt<0){
				_encrypt = EncryptedFlag.equals("Yes")?1:0;
			}
		}

		// stylesheet attribute if present takes form of:
		//   style_number # 1-255
		//   style_begin  # or ''
		//   style_end    # or ''
		// store stylesheet in dict in the form of
		// {'number' : ('style_begin', 'style_end')}

		if(_header_tag.containsKey("StyleSheet")){
			String[] lines = _header_tag.get("StyleSheet").split("\n");
			for(int i=0;i<lines.length;i+=3) {
				_stylesheet.put(lines[i],new String[]{i+1<lines.length?lines[i+1]:"",i+2<lines.length?lines[i+2]:""});
				lenSty++;
			}
		}

		_version = Float.parseFloat(_header_tag.get("GeneratedByEngineVersion"));

		_number_width = _version < 2.0 ?4 : 8;

		//![1]HEADER 分析完毕
		//![2]_read_keys_info START
		//stst = System.currentTimeMillis();
		//size (in bytes) of previous 5 or 4 numbers (can be encrypted)
		int num_bytes = _version >= 2 ?8 * 5 + 4 : 4 * 4;
		itemBuf = new byte[num_bytes];
		data_in.read(itemBuf, 0, num_bytes);
		data_in.close();
		ByteBuffer sf = ByteBuffer.wrap(itemBuf);

		//TODO: pureSalsa20.py decryption
		if(_encrypt==1){if(_passcode.equals(emptyStr)) throw new IllegalArgumentException("_passcode未输入");}
		_num_key_blocks = _read_number(sf);                                           // 1
		_num_entries = _read_number(sf);                                          // 2
		if(_version >= 2.0){_key_block_info_decomp_size = _read_number(sf);}      //[3]
		_key_block_info_size = _read_number(sf);                                  // 4
		_key_block_size = _read_number(sf);                                       // 5

		//前 5 个数据的 adler checksum
		if(_version >= 2.0)
		{
			//int adler32 = BU.calcChecksum(itemBuf,0,num_bytes-4);
			//assert adler32 == (sf.getInt()& 0xffffffff);
		}

		_key_block_offset+=num_bytes+_key_block_info_size;

		//assert(_num_key_blocks == _key_block_info_list.length);

		_record_block_offset = _key_block_offset+_key_block_size;


		read_key_block_info();
	}


	protected void initLogically() {

	}


	void read_key_block_info() {
		// read key block info, which comprises each key_block's:
		//1.(starting && ending words'shrinkedText,in the form of shrinkedTextSize-shrinkedText.Name them as headerText、tailerText)、
		//2.(compressed && decompressed size,which also have version differences, occupying either 4 or 8 bytes)
		try {
			DataInputStream data_in1 = getStreamAt( _key_block_offset-_key_block_info_size);

			byte[] itemBuf = new byte[(int) _key_block_info_size];
			data_in1.read(itemBuf, 0, (int) _key_block_info_size);
			data_in1.close();
			_decode_key_block_info(itemBuf);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


	void _decode_key_block_info(byte[] key_block_info_compressed) {
		key_info_struct[] _key_block_info_list = new key_info_struct[(int) _num_key_blocks];
		//block_blockId_search_list = new String[(int) _num_key_blocks];
		byte[] key_block_info=null;int BlockOff=0;
		int BlockLen=0;//(int) infoI.key_block_decompressed_size;
		if(_version >= 2)
		{
			//处理 Ripe128md 加密的 key_block_info
			if(_encrypt==2){try{
				key_block_info_compressed = BU._mdx_decrypt(key_block_info_compressed);
			} catch (IOException e) {e.printStackTrace();}}
			//!!!getInt CAN BE NEGTIVE ,INCONGRUENT to python CODE
			//!!!MAY HAVE BUG
			//int adler32 = getInt(key_block_info_compressed[4],key_block_info_compressed[5],key_block_info_compressed[6],key_block_info_compressed[7]);

			//解压开始
			switch (key_block_info_compressed[0]|key_block_info_compressed[1]<<8|key_block_info_compressed[2]<<16|key_block_info_compressed[3]<<32){
				case 0://no compression
					key_block_info=key_block_info_compressed;
					BlockOff=8;
					BlockLen=key_block_info_compressed.length-8;
				break;
				case 1:
					key_block_info = lzo_decompress(key_block_info_compressed,8);
				break;
				case 2:
					key_block_info = zlib_decompress(key_block_info_compressed,8);
					BlockLen=key_block_info.length;
				break;
			}
			//assert(adler32 == (BU.calcChecksum(key_block_info) ));
			//ripemd128.printBytes(key_block_info,0, key_block_info.length);
		}
		else
			key_block_info = key_block_info_compressed;
		// decoding……
		long key_block_compressed_size = 0;
		int accumulation_ = 0;//how many entries before one certain block.for construction of a list.
		//遍历blocks
		int bytePointer =0 ;
		for(int i=0;i<_key_block_info_list.length;i++){
			int textbufferST,textbufferLn;
			accumulation_blockId_tree.insert(new myCpr<>(accumulation_, i));
			//read in number of entries in current key block
			if(_version<2) {
				_key_block_info_list[i] = new key_info_struct(BU.toInt(key_block_info,BlockOff+bytePointer),accumulation_);
				bytePointer+=4;
			}
			else {
				//CMN.show(key_block_info_compressed.length+":"+key_block_info.length+":"+bytePointer);
				_key_block_info_list[i] = new key_info_struct(BU.toLong(key_block_info,BlockOff+bytePointer),accumulation_);
				bytePointer+=8;
			}
			key_info_struct infoI = _key_block_info_list[i];
			accumulation_ += infoI.num_entries;
			//CMN.show("infoI.num_entries::"+infoI.num_entries);
			//![0] head word text
			int text_head_size;
			if(_version<2)
				text_head_size = key_block_info[BlockOff+bytePointer++] & 0xFF;
			else {
				text_head_size = BU.toChar(key_block_info,BlockOff+bytePointer);
				bytePointer+=2;
			}
			textbufferST=bytePointer;
			if(!_encoding.startsWith("UTF-16")){
				textbufferLn=text_head_size;
				if(_version>=2)
					bytePointer++;
			}else{
				textbufferLn=text_head_size*2;
				if(_version>=2)
					bytePointer+=2;
			}

			infoI.headerKeyText = new byte[textbufferLn];
			System.arraycopy(key_block_info, BlockOff+textbufferST, infoI.headerKeyText, 0, textbufferLn);


			bytePointer+=textbufferLn;


			//![1]  tail word text
			int text_tail_size;
			if(_version<2)
				text_tail_size = key_block_info[BlockOff+bytePointer++] & 0xFF;
			else {
				text_tail_size = BU.toChar(key_block_info,BlockOff+bytePointer);
				bytePointer+=2;
			}
			textbufferST=bytePointer;

			textbufferLn=text_tail_size*delimiter_width;
			if(_version>=2)
				bytePointer+=delimiter_width;

			infoI.tailerKeyText = new byte[textbufferLn];

			System.arraycopy(key_block_info, BlockOff+textbufferST, infoI.tailerKeyText, 0, textbufferLn);

			bytePointer+=textbufferLn;

			//show(infoI.tailerKeyText+"~tailerKeyText");

			infoI.key_block_compressed_size_accumulator = key_block_compressed_size;
			if(_version<2){//may reduce
				infoI.key_block_compressed_size = BU.toInt(key_block_info,BlockOff+bytePointer);
				key_block_compressed_size += infoI.key_block_compressed_size;
				bytePointer+=4;
				infoI.key_block_decompressed_size = BU.toInt(key_block_info,BlockOff+bytePointer);
				maxDecomKeyBlockSize = Math.max(infoI.key_block_decompressed_size, maxDecomKeyBlockSize);
				maxComKeyBlockSize = Math.max(infoI.key_block_compressed_size, maxComKeyBlockSize);
				bytePointer+=4;
			}else{
				infoI.key_block_compressed_size = BU.toLong(key_block_info,BlockOff+bytePointer);
				key_block_compressed_size += infoI.key_block_compressed_size;
				bytePointer+=8;
				infoI.key_block_decompressed_size = BU.toLong(key_block_info,BlockOff+bytePointer);
				maxDecomKeyBlockSize = Math.max(infoI.key_block_decompressed_size, maxDecomKeyBlockSize);
				maxComKeyBlockSize = Math.max(infoI.key_block_compressed_size, maxComKeyBlockSize);

				bytePointer+=8;
			}
			//CMN.show("maxDecomKeyBlockSize: "+infoI.key_block_decompressed_size);
			//CMN.show("infoI.key_block_decompressed_size::"+infoI.key_block_decompressed_size);
			//CMN.show("infoI.key_block_compressed_size::"+infoI.key_block_compressed_size);

			//block_blockId_search_list[i] = infoI.headerKeyText;
		}
		key_block_info=null;
		//assert(accumulation_ == self._num_entries)
		this._key_block_info_list =  _key_block_info_list;
	}

	long decode_record_block_size(){
		if(_num_record_blocks==0){
			try {
				DataInputStream data_in1 = getStreamAt(_record_block_offset);
				_num_record_blocks = _read_number(data_in1);
				long num_entries = _read_number(data_in1);
				long record_block_info_size = _read_number(data_in1);
				_record_block_size = _read_number(data_in1);
				data_in1.close();
			} catch (IOException ignored) { }
		}
		return _num_record_blocks;
	}

	void decode_record_block_header() throws IOException{
		//![3]Decode_record_block_header
		long start = System.currentTimeMillis();
		DataInputStream data_in1 = getStreamAt(_record_block_offset);

		_num_record_blocks = _read_number(data_in1);
		long num_entries = _read_number(data_in1);
		//assert(num_entries == _num_entries);
		long record_block_info_size = _read_number(data_in1);
		_record_block_size = _read_number(data_in1);

		//record block info section
		record_info_struct[] record_info_struct_list = new record_info_struct[(int) _num_record_blocks];
		//int size_counter = 0;
		long compressed_size_accumulator = 0;
		long decompressed_size_accumulator = 0;
		/* must be faster: batch read-in strategy */
		byte[] numers = new byte[(int) record_block_info_size];
		data_in1.read(numers);
		data_in1.close();
		long compressed_size, decompressed_size;
		for(int i=0;i<_num_record_blocks;i++){
			compressed_size = _version>=2?BU.toLong(numers, i*16):BU.toInt(numers, i*8);
			decompressed_size = _version>=2?BU.toLong(numers, i*16+8):BU.toInt(numers, i*8+4);
			maxComRecSize = Math.max(maxComRecSize, compressed_size);
			maxDecompressedSize=Math.max(maxDecompressedSize, decompressed_size);

			record_info_struct_list[i] = new record_info_struct(compressed_size, compressed_size_accumulator, decompressed_size, decompressed_size_accumulator);
			compressed_size_accumulator+=compressed_size;
			decompressed_size_accumulator+=decompressed_size;
		}
		//assert(size_counter == record_block_info_size);
		_record_info_struct_list=record_info_struct_list;
	}

	public static byte[] zlib_decompress(byte[] encdata,int offset,int size) {
		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			InflaterOutputStream inf = new InflaterOutputStream(out);
			inf.write(encdata,offset, size);
			inf.close();
			return out.toByteArray();
		} catch (Exception ex) {
			ex.printStackTrace();
			//show(emptyStr);
			return "ERR".getBytes();
		}
	}

	long _read_number(ByteBuffer sf) {
		if(_number_width==4)
			return sf.getInt();
		else
			return sf.getLong();
	}
	long _read_number(DataInputStream  sf) {
		if(_number_width==4)
			try {
				return sf.readInt();
			} catch (IOException e) {
				e.printStackTrace();
				return 0;
			}
		else
			try {
				return sf.readLong();
			} catch (IOException e) {
				e.printStackTrace();
				return 0;
			}
	}

	public static byte[] zlib_decompress(byte[] encdata,int offset) {
		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			InflaterOutputStream inf = new InflaterOutputStream(out);
			inf.write(encdata,offset, encdata.length-offset);
			inf.close();
			return out.toByteArray();
		} catch (Exception ex) {
			ex.printStackTrace();
			return "ERR".getBytes();
		}
	}
	public static byte[] lzo_decompress(byte[] compressed,int offset) {
		try {
			ByteArrayInputStream in = new ByteArrayInputStream(compressed,offset,compressed.length-offset);
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			LzoAlgorithm algorithm = LzoAlgorithm.LZO1X;
			LzoDecompressor decompressor = LzoLibrary.getInstance().newDecompressor(algorithm, null);
			LzoInputStream stream = new LzoInputStream(in, decompressor);
			int read;
			byte[] bytes = new byte[1024];
			while ((read = stream.read(bytes)) != -1) {
				out.write(bytes, 0, read);
			}
			out.close();
			stream.close();
			return out.toByteArray();
		} catch (Exception ex) {
			ex.printStackTrace();
			return "ERR".getBytes();
		}
	}


	public int findRecordBlockByKeyOff(long keyOffset, int start, int end) {//return rec blck ID
		int len = end-start;
		if (len > 1) {
			len = len >> 1;
			return keyOffset>=_record_info_struct_list[start + len - 1].decompressed_size_accumulator+_record_info_struct_list[start + len - 1].decompressed_size//注意要抛弃 == 项
					? findRecordBlockByKeyOff(keyOffset,start+len,end)
					: findRecordBlockByKeyOff(keyOffset,start,start+len);
		} else {
			return start;
		}
	}


	static class cached_rec_block{
		byte[] record_block_;
		int blockOff;
		//int blockLen;
		int blockID=-100;
	}
	private volatile cached_rec_block RinfoI_cache_ = new cached_rec_block();

	//存储一组RecordBlock
	cached_rec_block prepareRecordBlock(record_info_struct RinfoI, int Rinfo_id) throws IOException {
		if(RinfoI_cache_.blockID==Rinfo_id)
			return RinfoI_cache_;

		if(RinfoI==null)
			RinfoI = _record_info_struct_list[Rinfo_id];
		DataInputStream data_in = getStreamAt(_record_block_offset+_number_width*4+_num_record_blocks*2*_number_width+
				RinfoI.compressed_size_accumulator);


		int compressed_size = (int) RinfoI.compressed_size;
		int decompressed_size = rec_decompressed_size = (int) RinfoI.decompressed_size;//用于验证

		byte[] record_block_compressed = new byte[compressed_size];
		//System.out.println(compressed_size) ;
		//System.out.println(decompressed_size) ;
		data_in.read(record_block_compressed);
		data_in.close();
		// 4 bytes indicates block compression type
		//BU.printBytes(record_block_compressed,0,4);

		// 4 bytes adler checksum of uncompressed content
		//ByteBuffer sf1 = ByteBuffer.wrap(record_block_compressed);
		//int adler32 = sf1.order(ByteOrder.BIG_ENDIAN).getInt(4);

		cached_rec_block RinfoI_cache = new cached_rec_block();
		//RinfoI_cache.blockLen=decompressed_size;
		RinfoI_cache.blockOff=0;
		//解压开始
		switch (record_block_compressed[0]|record_block_compressed[1]<<8|record_block_compressed[2]<<16|record_block_compressed[3]<<32){
			default:
			case 0://no compression
				RinfoI_cache.record_block_=record_block_compressed;
				RinfoI_cache.blockOff=8;
				//CMN.Log(_key_block_compressed.length,start,8);
				//System.arraycopy(_key_block_compressed, (+8), key_block, 0,key_block.length);
			break;
			case 1:
				RinfoI_cache.record_block_ = new byte[decompressed_size];
				new LzoDecompressor1x().decompress(record_block_compressed, 8, compressed_size-8, RinfoI_cache.record_block_, 0,new lzo_uintp());
			break;
			case 2:
				RinfoI_cache.record_block_ = new byte[decompressed_size];
				//key_block = zlib_decompress(_key_block_compressed,(int) (start+8),(int)(compressedSize-8));
				Inflater inf = new Inflater();
				inf.setInput(record_block_compressed, +8, compressed_size-8);
				try {
					int ret = inf.inflate(RinfoI_cache.record_block_,0,decompressed_size);
				} catch (DataFormatException e) {e.printStackTrace();}
			break;
		}


		//CMN.show(record_block.length+"ss"+decompressed_size);
		// notice not that adler32 return signed value

		//assert(adler32 == (BU.calcChecksum(record_block,0,decompressed_size) ));
		//assert(record_block.length == decompressed_size );
		//当前内容块解压完毕
		RinfoI_cache_=RinfoI_cache;
		return RinfoI_cache;
	}


	//for listview
	public String getEntryAt(int position) {
		if(position==-1) return "about:";
		if(_key_block_info_list==null) read_key_block_info();
		int blockId = accumulation_blockId_tree.xxing(new myCpr<>(position,1)).getKey().value;
		key_info_struct infoI = _key_block_info_list[blockId];
		return new String(prepareItemByKeyInfo(infoI, blockId, null).keys[(int) (position-infoI.num_entries_accumulator)],_charset);
	}


	public static class RecordLogicLayer extends F1ag{
		public int ral;
		public byte[] data;
	}

	protected void getRecordData(int position, RecordLogicLayer retriever) throws IOException{
		if(position<0||position>=_num_entries) return;
		if(_record_info_struct_list==null) decode_record_block_header();
		int blockId = accumulation_blockId_tree.xxing(new myCpr<>(position,1)).getKey().value;
		key_info_struct infoI = _key_block_info_list[blockId];
		cached_key_block infoI_cache = prepareItemByKeyInfo(infoI, blockId, null);

		int i = (int) (position-infoI.num_entries_accumulator);
		Integer Rinfo_id = findRecordBlockByKeyOff(infoI_cache.key_offsets[i],0,_record_info_struct_list.length);//accumulation_RecordB_tree.xxing(new mdictRes.myCpr(,1)).getKey().value;//null 过 key前
		record_info_struct RinfoI = _record_info_struct_list[Rinfo_id];

		cached_rec_block RinfoI_cache = prepareRecordBlock(RinfoI,Rinfo_id);

		// split record block according to the offset info from key block
		long record_start = infoI_cache.key_offsets[i]-RinfoI.decompressed_size_accumulator;
		long record_end;
		if (i < infoI.num_entries-1){
			record_end = infoI_cache.key_offsets[i+1]-RinfoI.decompressed_size_accumulator;
		}
		else {
			if (blockId + 1 < _key_block_info_list.length) {
				//TODO construct a margin checker
				record_end = prepareItemByKeyInfo(null, blockId + 1, null).key_offsets[0] - RinfoI.decompressed_size_accumulator;
			} else
				record_end = rec_decompressed_size;
		}
		retriever.ral=(int)record_start+RinfoI_cache.blockOff;
		retriever.val=(int)record_end+RinfoI_cache.blockOff;
		retriever.data=RinfoI_cache.record_block_;
	}

	public byte[] getRecordData(int position) throws IOException{
		RecordLogicLayer va1=new RecordLogicLayer();
		getRecordData(position, va1);
		byte[] data = va1.data;
		int record_start=va1.ral;
		int record_end=va1.val;

		byte[] record = new byte[(record_end-record_start)];
		int recordLen = record.length;
		if(recordLen+record_start>data.length)
			recordLen = data.length-record_start;

		System.arraycopy(data, record_start, record, 0, recordLen);
		return record;
	}

	public ByteArrayInputStream getResourseAt(int position) throws IOException {
		RecordLogicLayer va1=new RecordLogicLayer();
		getRecordData(position, va1);
		return new BSI(va1.data, va1.ral, va1.val-va1.ral);
	}

	static class cached_key_block{
		byte[][] keys;
		long[] key_offsets;
		byte[] hearderText=null;
		byte[] tailerKeyText=null;
		String hearderTextStr=null;
		String tailerKeyTextStr=null;
		int blockID=-100;
	}
	private cached_key_block infoI_cache_ = new cached_key_block();

	public cached_key_block prepareItemByKeyInfo(key_info_struct infoI,int blockId,cached_key_block infoI_cache){
		cached_key_block infoI_cache_ = this.infoI_cache_;
		if(_key_block_info_list==null) read_key_block_info();
		if(infoI_cache_.blockID==blockId)
			return infoI_cache_;
		if(infoI_cache==null)
			infoI_cache = new cached_key_block();
		try {
			if(infoI==null)
				infoI = _key_block_info_list[blockId];
			infoI_cache.keys = new byte[(int) infoI.num_entries][];
			infoI_cache.key_offsets = new long[(int) infoI.num_entries];
			infoI_cache.hearderText = infoI.headerKeyText;
			infoI_cache.tailerKeyText = infoI.tailerKeyText;
			long start = infoI.key_block_compressed_size_accumulator;
			long compressedSize;

			if(blockId==_key_block_info_list.length-1)
				compressedSize = _key_block_size - _key_block_info_list[_key_block_info_list.length-1].key_block_compressed_size_accumulator;
			else
				compressedSize = _key_block_info_list[blockId+1].key_block_compressed_size_accumulator-infoI.key_block_compressed_size_accumulator;

			DataInputStream data_in = getStreamAt(_key_block_offset+start);

			byte[]  _key_block_compressed = new byte[(int) compressedSize];
			data_in.read(_key_block_compressed, 0, _key_block_compressed.length);
			data_in.close();

			//int adler32 = getInt(_key_block_compressed[+4],_key_block_compressed[+5],_key_block_compressed[+6],_key_block_compressed[+7]);

			byte[] key_block;
			int BlockOff=0;
			int BlockLen=(int) infoI.key_block_decompressed_size;
			//解压开始
			switch (_key_block_compressed[0]|_key_block_compressed[1]<<8|_key_block_compressed[2]<<16|_key_block_compressed[3]<<32){
				default:
				case 0://no compression
					key_block=_key_block_compressed;
					BlockOff=8;
				break;
				case 1:
					key_block = new byte[BlockLen];
					new LzoDecompressor1x().decompress(_key_block_compressed, 8, (int)(compressedSize-8), key_block, 0,new lzo_uintp());
				break;
				case 2:
					key_block = new byte[BlockLen];
					//key_block = zlib_decompress(_key_block_compressed,(int) (start+8),(int)(compressedSize-8));
					Inflater inf = new Inflater();
					inf.setInput(_key_block_compressed, +8,(int)(compressedSize-8));
					try {
						int ret = inf.inflate(key_block,0,(int)(infoI.key_block_decompressed_size));
					} catch (DataFormatException e) {e.printStackTrace();}
				break;
			}
			/*spliting current Key block*/
			int key_start_index=0,
					key_end_index,
					keyCounter = 0;

			while(key_start_index < BlockLen){
				long key_id = _version<2 ?BU.toInt(key_block, BlockOff+key_start_index)
							:BU.toLong(key_block, BlockOff+key_start_index);

				key_end_index = key_start_index + _number_width;
				SK_DELI:
				while(key_end_index+delimiter_width<BlockLen){
					for(int sker=0;sker<delimiter_width;sker++) {
						if(key_block[BlockOff+key_end_index+sker]!=0) {
							key_end_index+=delimiter_width;
							continue SK_DELI;
						}
					}
					break;//all match
				}

				//show("key_start_index"+key_start_index);
				byte[] arraytmp = new byte[key_end_index-(key_start_index+_number_width)];
				System.arraycopy(key_block,BlockOff+key_start_index+_number_width, arraytmp, 0,arraytmp.length);


				//CMN.show(keyCounter+":::"+key_text);
				key_start_index = key_end_index + delimiter_width;
				//SU.Log(infoI_cache.keys.length+"~~~"+keyCounter+"~~~"+infoI.num_entries);
				infoI_cache.keys[keyCounter]=arraytmp;

				infoI_cache.key_offsets[keyCounter]=key_id;
				keyCounter++;
			}
			//long end2=System.currentTimeMillis(); //获取开始时间
			//System.out.println("解压耗时："+(end2-start2));
			//assert(adler32 == (calcChecksum(key_block)));
			infoI_cache.blockID = blockId;
			this.infoI_cache_=infoI_cache;
		} catch (IOException e2) {
			e2.printStackTrace();
		}
		return infoI_cache;

	}

	@Deprecated
	public void findAllKeys(String keyword){
		keyword = mdict.processText(keyword);
		int blockCounter = 0;
		for(key_info_struct infoI:_key_block_info_list){
			prepareItemByKeyInfo(infoI,blockCounter,null);
			for(byte[] entry:infoI_cache_.keys){
				String kk = new String(entry);
				if(kk.contains(keyword))
					SU.Log(kk);
			}
			blockCounter++;
		}
	}



	protected HashMap<String,String> _header_tag;

	public static int getInt(byte buf1, byte buf2, byte buf3, byte buf4)
	{
		int r = 0;
		r |= (buf1 & 0xff);
		r <<= 8;
		r |= (buf2 & 0xff);
		r <<= 8;
		r |= (buf3 & 0xff);
		r <<= 8;
		r |= (buf4 & 0xff);
		return r;
	}
	//per-byte byte array comparing
	final static int compareByteArray(byte[] A,byte[] B){
		int la = A.length,lb = B.length;
		for(int i=0;i<Math.min(la, lb);i++){
			int cpr = (int)(A[i]&0xff)-(int)(B[i]&0xff);
			if(cpr==0)
				continue;
			return cpr;
		}
		if(la==lb)
			return 0;
		else return la>lb?1:-1;
	}
	//per-byte byte array comparing
	final static boolean compareByteArrayIsPara(byte[] A,byte[] B){
		for(int i=0;i<A.length;i++){
			if(A[i]!=B[i])
				return false;
		}
		return true;
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

	public String getPath() {
		return f.getAbsolutePath();
	}
	public boolean moveFileTo(File newF) {
		boolean ret = f.renameTo(newF);
		if(ret)
			f = newF;
		return ret;
	}
	public boolean renameFileTo(File newF) {
		boolean ret = f.renameTo(newF);
		if(ret)
			f = newF;
		return ret;
	}
	public void updateFile(File newF){
		f = newF;
	}




}

/*  Copyright 2018 KnIfER Zenjio-Kang

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

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;
import java.util.zip.InflaterOutputStream;

import org.anarres.lzo.LzoDecompressor1x;
import org.anarres.lzo.lzo_uintp;
//import org.jvcompress.lzo.MiniLZO;
//import org.jvcompress.util.MInt;

import com.knziha.rbtree.RBTree;


class mdBase {
	protected File f;
	mdBase(){};
	
    final static byte[] _zero4 = new byte[]{0,0,0,0};
    final static byte[] _1zero3 = new byte[]{1,0,0,0};
    final static byte[] _2zero3 = new byte[]{2,0,0,0};
	final static String emptyStr = "";
    
	int _encrypt=0;
	Charset _charset;
	protected int delimiter_width = 1;
	String _encoding="UTF-16LE";
	String _passcode = "";

	float _version;
	protected int _number_width;
	long _num_entries;public long getNumberEntries(){return _num_entries;}
    long _num_key_blocks;public long get_num_key_blocks(){return _num_key_blocks;}
    long _num_record_blocks=0;

	RBTree<myCpr<Integer, Integer>> accumulation_blockId_tree = new RBTree<myCpr<Integer, Integer>>();
	long _key_block_size,_key_block_info_size,_key_block_info_decomp_size,_record_block_size;
	
	int _key_block_offset;
	long _record_block_offset;
	
    key_info_struct[] _key_block_info_list;
	record_info_struct[] _record_info_struct_list;
	
    int rec_decompressed_size;
    long maxComRecSize;
    long maxDecompressedSize;
    public long maxComKeyBlockSize;
    public long maxDecomKeyBlockSize;
	byte[] record_block_;
	
    DataInputStream getStreamAt(long at) throws IOException {
    	DataInputStream data_in1 = new DataInputStream(new FileInputStream(f));
    	if(at>0) {
	    	long yue=0;
	    	while(yue<at) {
	    		yue+=data_in1.skip(at-yue);
	    	}
    	}
    	return data_in1;
    }
    
	private HashMap<Integer,String[]> _stylesheet = new HashMap<Integer,String[]>();

    //构造
    mdBase(String fn) throws IOException  {
    //![0]File in
    	f = new File(fn);
    	
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
		//itemBuf = new byte[4];
		//data_in.read(itemBuf, 0, 4);
    	//int alder32 = getInt(itemBuf[3],itemBuf[2],itemBuf[1],itemBuf[0]);
    	//assert alder32 == (BU.calcChecksum(header_bytes)& 0xffffffff);
		data_in.skipBytes(4);
		//不必关闭文件流 data_in

			
		Pattern re = Pattern.compile("(\\w+)=\"(.*?)\"",Pattern.DOTALL);
		String headerString = new String(header_bytes,"UTF-16LE");
		//CMN.show("headerString::"+headerString);
		Matcher m = re.matcher(headerString);
		_header_tag = new HashMap<String,String>();
		while(m.find()) {
			_header_tag.put(m.group(1), m.group(2));
	    }
		
		if(_header_tag.containsKey("Encoding") && !_header_tag.get("Encoding").equals(""))
			_encoding = _header_tag.get("Encoding").toUpperCase();
		
        if(_encoding.equals("GBK")|| _encoding.equals("GB2312")) _encoding = "GB18030";// GB18030 > GBK > GB2312
        if (_encoding.equals("")) _encoding = "UTF-8";
        if(_encoding.equals("UTF-16")) _encoding = "UTF-16LE"; //INCONGRUENT java charset

        _charset = Charset.forName(_encoding);
        
        if(_encoding.startsWith("UTF-16"))
			delimiter_width = 2;
		else
			delimiter_width = 1;
        
        
        /* encryption flag
           0x00 - no encryption
           0x01 - encrypt record block
           0x02 - encrypt key info block*/
        String EncryptedFlag = _header_tag.get("Encrypted");
		if(EncryptedFlag==null || EncryptedFlag.equals("0") || EncryptedFlag.equals("No"))
            _encrypt = 0;
		else if(EncryptedFlag == "1")
            _encrypt = 1;
		else
			try {
				_encrypt = Integer.valueOf(EncryptedFlag);
			} catch (NumberFormatException e) {
				_encrypt=0;
			}

        // stylesheet attribute if present takes form of:
        //   style_number # 1-255
        //   style_begin  # or ''
        //   style_end    # or ''
        // store stylesheet in dict in the form of
        // {'number' : ('style_begin', 'style_end')}
        
        if(_header_tag.containsKey("StyleSheet")){
            String[] lines = _header_tag.get("StyleSheet").split("[\r\n \r \n]");
            for(int i=0;i<=lines.length-3;i+=3)
                _stylesheet.put(i,new String[]{lines[i+1],lines[i+2]});
        }
        
        _version = Float.valueOf(_header_tag.get("GeneratedByEngineVersion"));
        if(_version < 2.0)
            _number_width = 4;
        else
            _number_width = 8;
    //![1]HEADER 分析完毕 
    //![2]_read_keys_info START
		//stst = System.currentTimeMillis();
        //size (in bytes) of previous 5 or 4 numbers (can be encrypted)
        int num_bytes;
        if(_version >= 2)
            num_bytes = 8 * 5 + 4;
        else
            num_bytes = 4 * 4;
		itemBuf = new byte[num_bytes];
		data_in.read(itemBuf, 0, num_bytes);
		data_in.close();
        ByteBuffer sf = ByteBuffer.wrap(itemBuf);
        
        //TODO: pureSalsa20.py decryption
        if(_encrypt==1){if(_passcode==emptyStr) throw new IllegalArgumentException("_passcode未输入");}
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
		byte[] key_block_info;
		if(_version >= 2)
	    {   //zlib压缩
			//CMN.show("yes!");
			//byte[] asd = new byte[]{key_block_info_compressed[0],key_block_info_compressed[1],key_block_info_compressed[2],key_block_info_compressed[3]};
			//assert(new String(asd).equals(new String(new byte[]{2,0,0,0})));
	
			//处理 Ripe128md 加密的 key_block_info
			if(_encrypt==2){try{
	            key_block_info_compressed = BU._mdx_decrypt(key_block_info_compressed);
	            } catch (IOException e) {e.printStackTrace();}}
			//!!!getInt CAN BE NEGTIVE ,INCONGRUENT to python CODE
			//!!!MAY HAVE BUG
	        //int adler32 = getInt(key_block_info_compressed[4],key_block_info_compressed[5],key_block_info_compressed[6],key_block_info_compressed[7]);
	        key_block_info = zlib_decompress(key_block_info_compressed,8);
	        //assert(adler32 == (BU.calcChecksum(key_block_info) ));
	        //ripemd128.printBytes(key_block_info,0, key_block_info.length);
	    }
	    else
	        key_block_info = key_block_info_compressed;
		// decoding……
	    //ByteBuffer sf = ByteBuffer.wrap(key_block_info);
	    long key_block_compressed_size = 0;
	    int accumulation_ = 0;//how many entries before one certain block.for construction of a list.
	    //遍历blocks
	    int bytePointer =0 ;
	    for(int i=0;i<_key_block_info_list.length;i++){
	    	int textbufferST,textbufferLn;
	    	accumulation_blockId_tree.insert(new myCpr<Integer, Integer>(accumulation_,i));
	        //read in number of entries in current key block
	        if(_version<2) {
	            _key_block_info_list[i] = new key_info_struct(BU.toInt(key_block_info,bytePointer),accumulation_);
	            bytePointer+=4;
	        }
	        else {
	        	//CMN.show(key_block_info_compressed.length+":"+key_block_info.length+":"+bytePointer);
	        	_key_block_info_list[i] = new key_info_struct(BU.toLong(key_block_info,bytePointer),accumulation_);
	        	bytePointer+=8;
	        }
	        key_info_struct infoI = _key_block_info_list[i];
	        accumulation_ += infoI.num_entries;
	        //CMN.show("infoI.num_entries::"+infoI.num_entries);
	    //![0] head word text
	        int text_head_size;
	        if(_version<2)
	        	text_head_size = key_block_info[bytePointer++];
	    	else {
	    		text_head_size = BU.toChar(key_block_info,bytePointer);
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
	        System.arraycopy(key_block_info, textbufferST, infoI.headerKeyText, 0, textbufferLn);
	        
	        
	    	bytePointer+=textbufferLn;
	    	
	        
	    //![1]  tail word text
	        int text_tail_size;
	        if(_version<2)
	        	text_tail_size = key_block_info[bytePointer++];
	    	else {
	    		text_tail_size = BU.toChar(key_block_info,bytePointer);
	    		bytePointer+=2;
	    	}
	        textbufferST=bytePointer;
	
	    	textbufferLn=text_tail_size*delimiter_width;
	        if(_version>=2)
	    	bytePointer+=delimiter_width;   
	
	        infoI.tailerKeyText = new byte[textbufferLn];
	        
	        System.arraycopy(key_block_info, textbufferST, infoI.tailerKeyText, 0, textbufferLn);
	        
	    	bytePointer+=textbufferLn;
	    	
	        //show(infoI.tailerKeyText+"~tailerKeyText");
	
	        infoI.key_block_compressed_size_accumulator = key_block_compressed_size;
	        if(_version<2){//may reduce
	        	infoI.key_block_compressed_size = BU.toInt(key_block_info,bytePointer);
	        	key_block_compressed_size += infoI.key_block_compressed_size;
	        	bytePointer+=4;
	        	infoI.key_block_decompressed_size = BU.toInt(key_block_info,bytePointer);
	        	maxDecomKeyBlockSize = Math.max(infoI.key_block_decompressed_size, maxDecomKeyBlockSize);
	        	bytePointer+=4;
	        }else{
	        	infoI.key_block_compressed_size = BU.toLong(key_block_info,bytePointer);
	        	maxComKeyBlockSize = Math.max(infoI.key_block_compressed_size, maxComKeyBlockSize);
	        	key_block_compressed_size += infoI.key_block_compressed_size;
	        	bytePointer+=8;
	        	infoI.key_block_decompressed_size = BU.toLong(key_block_info,bytePointer);
	        	maxDecomKeyBlockSize = Math.max(infoI.key_block_decompressed_size, maxDecomKeyBlockSize);
	
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
        _record_info_struct_list = new record_info_struct[(int) _num_record_blocks];
        //int size_counter = 0;
        long compressed_size_accumulator = 0;
        long decompressed_size_accumulator = 0;
		/*may be faster:batch read-in strategy*/
		byte[] numers = new byte[(int) record_block_info_size];
		data_in1.read(numers);
		data_in1.close();
		for(int i=0;i<_num_record_blocks;i++){
			long compressed_size = _version>=2?BU.toLong(numers,(int) (i*16)):BU.toInt(numers,(int) (i*8));
	        long decompressed_size = _version>=2?BU.toLong(numers,(int) (i*16+8)):BU.toInt(numers,(int) (i*8+4));
            maxComRecSize = Math.max(maxComRecSize, compressed_size);
            
            maxDecompressedSize = Math.max(maxDecompressedSize, decompressed_size);
            _record_info_struct_list[i] = new record_info_struct(compressed_size, compressed_size_accumulator, decompressed_size, decompressed_size_accumulator);
            compressed_size_accumulator+=compressed_size;
            decompressed_size_accumulator+=decompressed_size;
            //size_counter += _number_width * 2;
		}
        //assert(size_counter == record_block_info_size);
        record_block_ = new byte[(int) maxDecompressedSize];	
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
    
    
	public int reduce(long keyOffset, int start, int end) {//return rec blck ID
        int len = end-start;
        if (len > 1) {
          len = len >> 1;
          return keyOffset>=_record_info_struct_list[start + len - 1].decompressed_size_accumulator+_record_info_struct_list[start + len - 1].decompressed_size//注意要抛弃 == 项
                    ? reduce(keyOffset,start+len,end)
                    : reduce(keyOffset,start,start+len);
        } else {
          return start;
        }
    }
    //存储一组RecordBlock
    int prepared_RecordBlock_ID=-100;
    byte[] prepareRecordBlock(record_info_struct RinfoI, int Rinfo_id) throws IOException {//异步
    	if(prepared_RecordBlock_ID==Rinfo_id)
    		return record_block_;
    	
    	if(RinfoI==null)
    		RinfoI = _record_info_struct_list[Rinfo_id];
    	DataInputStream data_in = getStreamAt(_record_block_offset+_number_width*4+_num_record_blocks*2*_number_width+
    			RinfoI.compressed_size_accumulator);
        // record block info section
    	

        //whole section of record_blocks;
       // for(int i123=0; i123<record_block_info_list.size(); i123++){
        	int compressed_size = (int) RinfoI.compressed_size;
        	int decompressed_size = rec_decompressed_size = (int) RinfoI.decompressed_size;//用于验证
        	byte[] record_block = new byte[(int) decompressed_size];
        	byte[] record_block_compressed = new byte[(int) compressed_size];
        	//System.out.println(compressed_size) ;
        	//System.out.println(decompressed_size) ;
        	data_in.read(record_block_compressed);
        	data_in.close();
            // 4 bytes indicates block compression type
            //BU.printBytes(record_block_compressed,0,4);
        	
        	// 4 bytes adler checksum of uncompressed content
        	//ByteBuffer sf1 = ByteBuffer.wrap(record_block_compressed);
            //int adler32 = sf1.order(ByteOrder.BIG_ENDIAN).getInt(4);
            
            if(compareByteArrayIsPara(_zero4, record_block_compressed)){
            	System.arraycopy(record_block_compressed, 8, record_block, 0, decompressed_size-8);
            }
            // lzo compression
            else if(compareByteArrayIsPara(_1zero3, record_block_compressed)){
                //record_block = new byte[ decompressed_size];
                //MInt len = new MInt(decompressed_size);
                //System.arraycopy(record_block_compressed, 8, record_block, 0,(int) (compressed_size-8));
                //MiniLZO.lzo1x_decompress(record_block,compressed_size,record_block,len);
                new LzoDecompressor1x().decompress(record_block_compressed, 8, (compressed_size-8), record_block, 0,new lzo_uintp());

            }
            // zlib compression
            else if(compareByteArrayIsPara(_2zero3, record_block_compressed)){
            	//long stst = System.currentTimeMillis();
                //record_block = zlib_decompress(record_block_compressed,8);
                Inflater inf = new Inflater();
                inf.setInput(record_block_compressed,8,compressed_size-8);
                try {
					int ret = inf.inflate(record_block,0,decompressed_size);
				} catch (DataFormatException e) {
					e.printStackTrace();
				}
                //Log.e("asdsad",(System.currentTimeMillis()-stst)+" "+Rinfo_id);
            }

            //CMN.show(record_block.length+"ss"+decompressed_size);
            // notice not that adler32 return signed value
            
            //assert(adler32 == (BU.calcChecksum(record_block,0,decompressed_size) ));
            //assert(record_block.length == decompressed_size );
        //当前内容块解压完毕		
            record_block_=record_block;
            prepared_RecordBlock_ID=Rinfo_id;
            return record_block;
	}
    
    
    //for lv
	public String getEntryAt(int position) {
		if(position==-1) return "about:";
		if(_key_block_info_list==null) read_key_block_info();
        int blockId = accumulation_blockId_tree.xxing(new myCpr<>(position,1)).getKey().value;
        key_info_struct infoI = _key_block_info_list[blockId];
        prepareItemByKeyInfo(infoI,blockId,null);
        return new String(infoI_cache_.keys[(int) (position-infoI.num_entries_accumulator)],_charset);
	}
	
	
    class cached_key_block{
    	byte[][] keys;
    	long[] key_offsets;
    	byte[] hearderText=null;
    	int blockID=-1;
    }
    protected cached_key_block infoI_cache_ = new cached_key_block();
    
    public cached_key_block prepareItemByKeyInfo(key_info_struct infoI,int blockId,cached_key_block infoI_cache){
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
        	long start = infoI.key_block_compressed_size_accumulator;
            long compressedSize;
            byte[] key_block = new byte[1];
			if(blockId==_key_block_info_list.length-1)
                compressedSize = _key_block_size - _key_block_info_list[_key_block_info_list.length-1].key_block_compressed_size_accumulator;
            else
                compressedSize = _key_block_info_list[blockId+1].key_block_compressed_size_accumulator-infoI.key_block_compressed_size_accumulator;
                
	
			DataInputStream data_in = getStreamAt(_key_block_offset+start);
			
			byte[]  _key_block_compressed = new byte[(int) compressedSize];
			data_in.read(_key_block_compressed, (int)(0), _key_block_compressed.length);
			data_in.close();
			
        
            String key_block_compression_type = new String(new byte[]{_key_block_compressed[(int) 0],_key_block_compressed[(int) (+1)],_key_block_compressed[(int) (+2)],_key_block_compressed[(int) (3)]});
            //int adler32 = getInt(_key_block_compressed[(int) (+4)],_key_block_compressed[(int) (+5)],_key_block_compressed[(int) (+6)],_key_block_compressed[(int) (+7)]);
            if(key_block_compression_type.equals(new String(new byte[]{0,0,0,0}))){
                //无需解压
                key_block = new byte[(int) (_key_block_compressed.length-start-8)];
                System.arraycopy(_key_block_compressed, (int)(+8), key_block, 0,key_block.length);
            }else if(key_block_compression_type.equals(new String(new byte[]{1,0,0,0})))
            {
                //key_block = lzo_decompress(_key_block_compressed,(int) (start+_number_width),(int)(compressedSize-_number_width));
            	/*MInt len = new MInt((int) infoI.key_block_decompressed_size);
            	key_block = new byte[len.v];
                byte[] arraytmp = new byte[(int) compressedSize];
                System.arraycopy(_key_block_compressed, (int)(+8), arraytmp, 0,(int) (compressedSize-8));
                MiniLZO.lzo1x_decompress(arraytmp,arraytmp.length,key_block,len);
                */
            	key_block = new byte[(int) infoI.key_block_decompressed_size];
                new LzoDecompressor1x().decompress(_key_block_compressed, 8, (int)(compressedSize-8), key_block, 0,new lzo_uintp());
            }
            else if(key_block_compression_type.equals(new String(new byte[]{02,00,00,00}))){
                //key_block = zlib_decompress(_key_block_compressed,(int) (start+8),(int)(compressedSize-8));
                Inflater inf = new Inflater();
                inf.setInput(_key_block_compressed,(int) (+8),(int)(compressedSize-8));
                key_block = new byte[(int) infoI.key_block_decompressed_size];
                try {
					int ret = inf.inflate(key_block,0,(int)(infoI.key_block_decompressed_size));
				} catch (DataFormatException e) {e.printStackTrace();}
            }
            /*spliting current Key block*/
            int key_start_index=0,
				key_end_index=0,
				keyCounter = 0;
            
			while(key_start_index < key_block.length){
            	long key_id;
            	if(_version<2)
            		key_id = BU.toInt(key_block, key_start_index);
            	else
            		key_id = BU.toLong(key_block, key_start_index);

            	
                key_end_index = key_start_index + _number_width;  
            	SK_DELI:
  				  while(key_end_index+delimiter_width<key_block.length){
  					for(int sker=0;sker<delimiter_width;sker++) {
  						if(key_block[key_end_index+sker]!=0) {
  							key_end_index+=delimiter_width;
  							continue SK_DELI;
  						}
  					}
  					break;//all match
  				 }
                
                //show("key_start_index"+key_start_index);
                byte[] arraytmp = new byte[key_end_index-(key_start_index+_number_width)];
                System.arraycopy(key_block,key_start_index+_number_width, arraytmp, 0,arraytmp.length);
                
                
				//CMN.show(keyCounter+":::"+key_text);
                key_start_index = key_end_index + delimiter_width;
                //CMN.show(infoI_cache.keys.length+"~~~"+keyCounter+"~~~"+infoI.num_entries);
                infoI_cache.keys[keyCounter]=arraytmp;
                
                infoI_cache.key_offsets[keyCounter]=key_id;
                keyCounter++;
            }
            //long end2=System.currentTimeMillis(); //获取开始时间 
            //System.out.println("解压耗时："+(end2-start2));
            //assert(adler32 == (calcChecksum(key_block)));
	        infoI_cache.blockID = blockId;
	        infoI_cache_=infoI_cache;
            } catch (IOException e2) {
        		e2.printStackTrace();
        	}
        return infoI_cache;
                
    }
    
    
    
    
    
    
    
    

    
    public void printRecordInfo() throws IOException{
        for(int i=0; i<_record_info_struct_list.length; i++){
        	record_info_struct RinfoI = _record_info_struct_list[i];
        	show("RinfoI_compressed_size="+RinfoI.compressed_size);
        	
        }	
    }
     
    
    public void printAllContents() throws IOException{
    	OutputStreamWriter fOut = new OutputStreamWriter(new FileOutputStream(f.getAbsolutePath()+".txt"),_encoding);

        DataInputStream data_in = getStreamAt(_record_block_offset+_number_width*4+_num_record_blocks*2*_number_width);
        // record block info section
        
        // actual record block data
        //int i = (int) (position-infoI.num_entries_accumulator);//处于当前record块的第几个
        //record_info_struct RinfoI = _record_info_struct_list[accumulation_RecordB_tree.xxing(new myCpr(infoI.key_offsets[i],1)).getKey().value];
        
        
        //whole section of record_blocks;
        for(int i=0; i<_record_info_struct_list.length; i++){
        	record_info_struct RinfoI = _record_info_struct_list[i];
        	//data_in.skipBytes((int) RinfoI.compressed_size_accumulator);
        	long compressed_size = RinfoI.compressed_size;
        	long decompressed_size = RinfoI.decompressed_size;//用于验证
        	byte[] record_block_compressed = new byte[(int) compressed_size];
        	data_in.read(record_block_compressed);//+8 TODO optimize
            // 4 bytes indicates block compression type
        	byte[] record_block_type = new byte[4];
        	System.arraycopy(record_block_compressed, 0, record_block_type, 0, 4);
        	String record_block_type_str = new String(record_block_type);
        	// 4 bytes adler checksum of uncompressed content
        	ByteBuffer sf1 = ByteBuffer.wrap(record_block_compressed);
            int adler32 = sf1.order(ByteOrder.BIG_ENDIAN).getInt(4);
            byte[] record_block_ = new byte[1];
            // no compression
            if(record_block_type_str.equals(new String(new byte[]{0,0,0,0}))){
            	record_block_ = new byte[(int) (compressed_size-8)];
            	System.arraycopy(record_block_compressed, 8, record_block_, 0, record_block_.length-8);
            }
            // lzo compression
            else if(record_block_type_str.equals(new String(new byte[]{1,0,0,0}))){
                //stst=System.currentTimeMillis(); //获取开始时间 
                //record_block_ = new byte[(int) decompressed_size];
                //MInt len = new MInt((int) decompressed_size);
                //byte[] arraytmp = new byte[(int) compressed_size];
                //System.arraycopy(record_block_compressed, 8, arraytmp, 0,(int) (compressed_size-8));
                //MiniLZO.lzo1x_decompress(arraytmp,(int) compressed_size,record_block_,len);
            	record_block_ = new byte[(int) (compressed_size-8)];
	            new LzoDecompressor1x().decompress(record_block_compressed, +8, (int)(compressed_size-8), record_block_, 0,new lzo_uintp());

            	//System.out.println("get Record LZO decompressing key blocks done!") ;
                //System.out.println("解压Record耗时："+(System.currentTimeMillis()-st));
            }
            // zlib compression
            else if(record_block_type_str.equals(new String(new byte[]{02,00,00,00}))){
                record_block_ = zlib_decompress(record_block_compressed,8);
            }
            // notice not that adler32 return signed value
            //CMN.show(adler32+"!:"+BU.calcChecksum(record_block_) );
            assert(adler32 == (BU.calcChecksum(record_block_) ));
            assert(record_block_.length == decompressed_size );
 //当前内容块解压完毕
            

            String record_str = new String(record_block_,_charset); 	
            // substitute styles
            //if self._substyle and self._stylesheet:
            //    record = self._substitute_stylesheet(record);
            show(record_str);       
        	
            fOut.append(record_str).append("\n");
            
        }
        data_in.close();
        fOut.close();
    }
    
    public void printAllKeys(){
    	if(_key_block_info_list==null) read_key_block_info();
    	int blockCounter = 0;
    	for(key_info_struct infoI:_key_block_info_list){
    		prepareItemByKeyInfo(infoI,blockCounter,null);
    		for(byte[] entry:infoI_cache_.keys){
    			String kk = new String(entry);
    				show(kk);
    		}
    		show("block no."+(blockCounter++)+"printed");
    	}
    }
    
    public void findAllKeys(String keyword){
        keyword = mdict.processText(keyword);
    	int blockCounter = 0;
    	for(key_info_struct infoI:_key_block_info_list){
    		prepareItemByKeyInfo(infoI,blockCounter,null);
    		for(byte[] entry:infoI_cache_.keys){
    			String kk = new String(entry);
    			if(kk.contains(keyword))
    				show(kk);
    		}
    		blockCounter++;
    	}
    } 
    
    
    
    
    
    
    
    
    
    
    
    
    protected HashMap<String,String> _header_tag;
    public void printDictInfo(){
    	show("\r\n——————————————————————Dict Info——————————————————————");
    	if(_header_tag!=null) {
	        Iterator iter = _header_tag.entrySet().iterator();  
	        while (iter.hasNext()) {  
	            Map.Entry entry = (Map.Entry) iter.next();  
	            Object key = entry.getKey();  
	            Object value = entry.getValue();  
	            System.out.println("|"+key + ":" + value);  
	        }  
    	}
        show("|编码: "+this._encoding);
        show("|_num_entries: "+this._num_entries);
        show("|_num_key_blocks: "+this._num_key_blocks);
        show("|_num_record_blocks: "+this._num_record_blocks);
        show("|maxComRecSize: "+this.maxComRecSize);
        show("|maxDecompressedSize: "+this.maxDecompressedSize);
        if(true) {
	        int counter=0;
	        for(key_info_struct infoI:_key_block_info_list) {
	        	show("|"+infoI.num_entries+"@No."+counter+"||header~"+infoI.headerKeyText+"||tailer~"+infoI.tailerKeyText);
	        	counter++;
	        }
        }
        show("——————————————————————Info of Dict ——————————————————————\r\n");
    }

    //解压等utils
    public static byte[] zlib_decompressRAW_METHON(byte[] encdata,int offset,int len) {
	    try {
			    Inflater inf = new Inflater();
			    inf.setInput(encdata,offset,encdata.length-8);
			    byte[] res = new byte[len];
			    int ret = inf.inflate(res,0,len);
			    //show("zlib_decompressRAW"+ret);
			    return res;
		    } catch (Exception ex) {
		    	ex.printStackTrace(); 
		    	return "ERR".getBytes(); 
		    }
    }
    public static String zlib_decompress_to_str(byte[] encdata,int offset) {
	    try {
			    ByteArrayOutputStream out = new ByteArrayOutputStream(); 
			    InflaterOutputStream inf = new InflaterOutputStream(out); 
			    inf.write(encdata,offset, encdata.length-offset); 
			    inf.close(); 
			    return out.toString("ASCII");
		    } catch (Exception ex) {
		    	ex.printStackTrace(); 
		    	return "ERR"; 
		    }
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
	
	
    void show(String str){
    	System.out.println(str);
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
	
	
	
}

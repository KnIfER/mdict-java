package com.knziha.plod.dictionary;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;
import java.util.zip.InflaterOutputStream;

import org.anarres.lzo.LzoDecompressor1x;
import org.anarres.lzo.lzo_uintp;
import org.jvcompress.lzo.MiniLZO;
import org.jvcompress.util.MInt;

import com.knziha.rbtree.RBTree;
import com.knziha.rbtree.RBTree_additive;
import com.knziha.rbtree.additiveMyCpr1;





/**
 * Mdict Java Library
 * FEATURES:
 * 1. Basic parse and query functions.
 * 2. Mdicts conjunction search.
 * 3. Multi-threaded search in all context text.
 * 4. Multi-threaded fuzzy search in all key entries.
 * @author KnIfER
 * @date 2017/12/30
 */

public class mdict {
	public mdict(){};

    
    public final static String replaceReg = " |:|\\.|,|-|\'|(|)";
    public final static String emptyStr = "";
    private final static String linkRenderStr = "@@@LINK=";
    final static byte[] _zero4 = new byte[]{0,0,0,0};
    final static byte[] _1zero3 = new byte[]{1,0,0,0};
    final static byte[] _2zero3 = new byte[]{2,0,0,0};
    
    protected mdictRes mdd;
    
	private key_info_struct[] _key_block_info_list;
	private record_info_struct[] _record_info_struct_list;
	RBTree<myCpr<Integer, Integer>> accumulation_blockId_tree = new RBTree<myCpr<Integer, Integer>>();
    //private RBTree<myCpr<Long   , Integer>> accumulation_RecordB_tree = new RBTree<myCpr<Long   , Integer>>();
    //RBTree<myCpr<String , Integer>> block_blockId_search_tree = new RBTree<myCpr<String , Integer>>();
    //RBTree<myCprStr<Integer>> block_blockId_search_tree = new RBTree<myCprStr<Integer>>();
    //String[] block_blockId_search_list;
    
    protected File f;
    public String _Dictionary_fName;
    public String _Dictionary_Name;
    public String _Dictionary_fSuffix;
    private int _encrypt=0;
	private int _number_width;
	private String _encoding=emptyStr;
	private Charset _charset;
	private int delimiter_width = 1;
	private String _passcode = emptyStr;
	private HashMap<Integer,String[]> _stylesheet = new HashMap<Integer,String[]>();
	private float _version;
	private long _num_entries;public long getNumberEntries(){return _num_entries;}
	private long _key_block_info_decomp_size;
	private long _num_key_blocks;
    private long _num_record_blocks;
    
    private long _key_block_offset;
    private long _record_block_offset;
    protected String _headerString;
    protected HashMap<String,String> _header_tag;
    

    public class myCprStr<T2> implements Comparable<myCprStr<T2>>{
    	public String key;
    	public T2 value;
    	public myCprStr(String k,T2 v){
    		key=k;value=v;
    	}
    	public int compareTo(myCprStr<T2> other) {
    		if(_encoding.equals("GB18030"))
				try {
					return compareByteArray(this.key.getBytes(_encoding),other.key.getBytes(_encoding));
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
			//else
    			return this.key.compareTo(other.key);

    	}
    	public String toString(){
    		return key+"_"+value;
    	}
    }    
    public static int regTime;
	long _key_block_size,_key_block_info_size,_record_block_size;
	
    //构造
    public mdict(String fn) throws IOException  {
    //![0]File in
    	f = new File(fn);
        _Dictionary_fName = f.getName();
    	int tmpIdx = _Dictionary_fName.lastIndexOf(".");
    	if(tmpIdx!=-1) {
	    	_Dictionary_fSuffix = _Dictionary_fName.substring(tmpIdx+1);
	    	_Dictionary_fName = _Dictionary_fName.substring(0, tmpIdx);
    	}
        String fnTMP = f.getName();
        File f2 = new File(f.getParentFile().getAbsolutePath()+"/"+fnTMP.substring(0,fnTMP.lastIndexOf("."))+".mdd");
    	if(f2.exists()){
    		mdd=new mdictRes(f2.getAbsolutePath());
    	}
    	

    	DataInputStream data_in =new DataInputStream(new FileInputStream(f));	
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
			data_in.skip(4);
		//不必关闭文件流 data_in

			
		Pattern re = Pattern.compile("(\\w+)=\"(.*?)\"",Pattern.DOTALL);
		_headerString = new String(header_bytes,"UTF-16LE");
		//CMN.show("headerString::"+headerString);
		Matcher m = re.matcher(_headerString);
		_header_tag = new HashMap<String,String>();
		while(m.find()) {
			_header_tag.put(m.group(1), m.group(2));
	    }				
		
		
		if(_header_tag.containsKey("Title"))
			_Dictionary_Name=_header_tag.get("Title");
		
		_encoding = _header_tag.get("Encoding").toUpperCase();
				
        if(_encoding.equals("GBK")|| _encoding.equals("GB2312")) _encoding = "GB18030";// GB18030 > GBK > GB2312
        if (_encoding.equals(emptyStr)) _encoding = "UTF-8";
        if(_encoding.equals("UTF-16")) _encoding = "UTF-16LE"; //INCONGRUENT java charset
        
        _charset = Charset.forName(_encoding);
        
        if(_encoding.startsWith("UTF-16"))//TODO optimize
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
		//CMN.show("key_block_info_size="+key_block_info_size);
		//CMN.show("key_block_info_decomp_size="+key_block_info_decomp_size);

        
        //assert(_num_key_blocks == _key_block_info_list.length);

        _record_block_offset = _key_block_offset+_key_block_size;
		//_key_block_compressed = new byte[(int) _key_block_size];
		//data_in.read(_key_block_compressed, 0, (int) _key_block_size);
        //![2]_read_keys_info Not ENDed
        
        
        calcFuzzySpace();
        
        read_key_block_info();
}
    
    void read_key_block_info() {
	    // read key block info, which comprises each key_block's:
	    //1.(starting && ending words'shrinkedText,in the form of shrinkedTextSize-shrinkedText.Name them as headerText、tailerText)、
	    //2.(compressed && decompressed size,which also have version differences, occupying either 4 or 8 bytes)
    	try {
    		DataInputStream data_in1 = new DataInputStream(new FileInputStream(f));
  			data_in1.skipBytes((int)( _key_block_offset-_key_block_info_size));
	    	byte[] itemBuf = new byte[(int) _key_block_info_size];
	    	data_in1.read(itemBuf, 0, (int) _key_block_info_size);
	    	_decode_key_block_info(itemBuf);
		    data_in1.close();
    	} catch (IOException e) {
			e.printStackTrace();
		}
    }

	    private void _decode_key_block_info(byte[] key_block_info_compressed) {
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
        		text_head_size = toChar(key_block_info,bytePointer);
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
        		text_tail_size = toChar(key_block_info,bytePointer);
        		bytePointer+=2;
        	}
            textbufferST=bytePointer;
            if(!_encoding.startsWith("UTF-16")){
            	textbufferLn=text_tail_size;
                if(_version>=2)
            	bytePointer++;         
            }else{
            	textbufferLn=text_tail_size*2;
                if(_version>=2)
            	bytePointer+=2;       
            }

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
        DataInputStream data_in1 = new DataInputStream(new FileInputStream(f));
        data_in1.skipBytes((int) _record_block_offset);
        
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
        record_block = new byte[(int) maxDecompressedSize];	
    }
    
    public String getRecordsAt(int... positions) throws IOException {
    	StringBuilder sb = new StringBuilder();
    	int c=0;
    	for(int i:positions) {
    		String tmp = getRecordAt(i).trim();
    		if(tmp.startsWith(linkRenderStr)) {
    			int idx = lookUp(tmp.substring(linkRenderStr.length()));
    			if(idx!=-1)
    				tmp=getRecordAt(idx);
    		}
    		sb.append(tmp);
    		if(c!=positions.length-1)
        		sb.append("<HR>");
    		c++;
    	}
    	return sb.toString();
    }
    
    int rec_decompressed_size;
    long maxComRecSize;
    public long maxComKeyBlockSize;
    public long maxDecomKeyBlockSize;
    long maxDecompressedSize;
	private byte[] record_block;
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
	
    public String getRecordAt(int position) throws IOException {
    	if(record_block==null)
    		decode_record_block_header();
    	if(position<0||position>=_num_entries) return null;
        int blockId = accumulation_blockId_tree.xxing(new myCpr<Integer,Integer>(position,1)).getKey().value;
        key_info_struct infoI = _key_block_info_list[blockId];
        
        //准备
        prepareItemByKeyInfo(infoI,blockId,null);
        //String[] key_list = infoI_cache_.keys;
        
        //decode record block
        // actual record block data
        int i = (int) (position-infoI.num_entries_accumulator);
        Integer Rinfo_id = reduce(infoI_cache_.key_offsets[i],0,_record_info_struct_list.length);//accumulation_RecordB_tree.xxing(new mdictRes.myCpr(,1)).getKey().value;//null 过 key前
        record_info_struct RinfoI = _record_info_struct_list[Rinfo_id];
        
        prepareRecordBlock(RinfoI,Rinfo_id);
        
            
        // split record block according to the offset info from key block
        //String key_text = key_list[i];
        long record_start = infoI_cache_.key_offsets[i]-RinfoI.decompressed_size_accumulator;
        long record_end;
        if (i < infoI.num_entries-1){
        	record_end = infoI_cache_.key_offsets[i+1]-RinfoI.decompressed_size_accumulator; 	
        }//TODO construct a margin checker
        else{
        	if(blockId+1<_key_block_info_list.length) {
        		prepareItemByKeyInfo(null,blockId+1,null);//没办法只好重新准备一个咯
        		//难道还能根据text末尾的0a 0d 00来分？不大好吧、
            	record_end = infoI_cache_.key_offsets[0]-RinfoI.decompressed_size_accumulator;
        	}else
        		record_end = rec_decompressed_size;
        	//CMN.show(record_block.length+":"+compressed_size+":"+decompressed_size);
        }
        //CMN.show(record_start+"!"+record_end);
        //byte[] record = new byte[(int) (record_end-record_start)]; 
        //CMN.show(record.length+":"+record_block.length+":"+(record_start));
        //System.arraycopy(record_block, (int) (record_start), record, 0, record.length);
        // convert to utf-8
        String record_str = new String(record_block,(int) (record_start),(int) (record_end-record_start),_charset);
        // substitute styles
        //if self._substyle and self._stylesheet:
        //    record = self._substitute_stylesheet(record);
        
        return	record_str;           	
    }
  
    //存储一组RecordBlock
    int prepared_RecordBlock_ID=-1;
    private void prepareRecordBlock(record_info_struct RinfoI, int Rinfo_id) throws IOException {
    	if(prepared_RecordBlock_ID==Rinfo_id)
    		return;
    	if(RinfoI==null)
    		RinfoI = _record_info_struct_list[Rinfo_id];
    	DataInputStream data_in = new DataInputStream(new FileInputStream(f));
        // record block info section
        data_in.skipBytes( (int) (_record_block_offset+_number_width*4+_num_record_blocks*2*_number_width));
        
        
        data_in.skipBytes((int) RinfoI.compressed_size_accumulator);
        //whole section of record_blocks;
       // for(int i123=0; i123<record_block_info_list.size(); i123++){
        	int compressed_size = (int) RinfoI.compressed_size;
        	int decompressed_size = rec_decompressed_size = (int) RinfoI.decompressed_size;//用于验证
        	byte[] record_block_compressed = new byte[(int) compressed_size];
        	//System.out.println(compressed_size) ;
        	//System.out.println(decompressed_size) ;
        	data_in.read(record_block_compressed);
        	data_in.close();
            // 4 bytes indicates block compression type
        	byte[] record_block_type = new byte[4];
        	System.arraycopy(record_block_compressed, 0, record_block_type, 0, 4);
        	String record_block_type_str = new String(record_block_type);
        	//ripemd128.printBytes(record_block_type);
        	// 4 bytes adler checksum of uncompressed content
        	ByteBuffer sf1 = ByteBuffer.wrap(record_block_compressed);
            int adler32 = sf1.order(ByteOrder.BIG_ENDIAN).getInt(4);
            adler32 = sf1.order(ByteOrder.BIG_ENDIAN).getInt(4);
            // no compression
            if(record_block_type_str.equals(new String(new byte[]{0,0,0,0}))){
            	System.arraycopy(record_block_compressed, 8, record_block, 0, compressed_size-8);
            }
            // lzo compression
            else if(record_block_type_str.equals(new String(new byte[]{1,0,0,0}))){
                //record_block = new byte[ decompressed_size];
                MInt len = new MInt(decompressed_size);
                byte[] arraytmp = new byte[ compressed_size];        
                System.arraycopy(record_block_compressed, 8, arraytmp, 0,(int) (compressed_size-8));
                MiniLZO.lzo1x_decompress(arraytmp,compressed_size,record_block,len);
            }
            // zlib compression
            else if(record_block_type_str.equals(new String(new byte[]{02,00,00,00}))){
                // decompress
                // record_block = zlib_decompress(record_block_compressed,8);
                Inflater inf = new Inflater();
                inf.setInput(record_block_compressed,8,compressed_size-8);
                try {
					int ret = inf.inflate(record_block,0,decompressed_size);
				} catch (DataFormatException e) {
					e.printStackTrace();
				}  
            }
            // notice not that adler32 return signed value
            
            //CMN.show(adler32+"'''"+(BU.calcChecksum(record_block,0,decompressed_size)));
            //assert(adler32 == (BU.calcChecksum(record_block,0,decompressed_size) ));
            //assert(record_block.length == decompressed_size );
 //当前内容块解压完毕		
            prepared_RecordBlock_ID=Rinfo_id;
	}

    class cached_key_block{
    	byte[][] keys;
    	long[] key_offsets;
    	byte[] hearderText=null;
    	int blockID=-1;
    }
    final private cached_key_block infoI_cache_ = new cached_key_block();
    
    /*只存储一组 key entrys,减少 IO*/
    public void prepareItemByKeyInfo(key_info_struct infoI,int blockId,cached_key_block infoI_cache){
    	if(_key_block_info_list==null) read_key_block_info();
        if(infoI_cache==null)
        	infoI_cache = infoI_cache_;
    	if(infoI_cache.blockID!=blockId){
    	  try {
	        if(infoI==null)
	        	infoI = _key_block_info_list[blockId];
    		infoI_cache.keys = new byte[(int) infoI.num_entries][];
			infoI_cache.key_offsets = new long[(int) infoI.num_entries];
			infoI_cache.blockID = blockId;
            infoI_cache.hearderText = infoI.headerKeyText;
            long start = infoI.key_block_compressed_size_accumulator;
            long compressedSize;
            byte[] key_block = new byte[1];
            if(blockId==_key_block_info_list.length-1)
                compressedSize = _key_block_size - _key_block_info_list[_key_block_info_list.length-1].key_block_compressed_size_accumulator;
            else
                compressedSize = _key_block_info_list[blockId+1].key_block_compressed_size_accumulator-infoI.key_block_compressed_size_accumulator;
            
			DataInputStream data_in =new DataInputStream(new FileInputStream(f));
			data_in.skip(_key_block_offset+start);
			byte[]  _key_block_compressed = new byte[(int) compressedSize];
			data_in.read(_key_block_compressed, (int)(0), _key_block_compressed.length);
			data_in.close();
            
            
            String key_block_compression_type = new String(new byte[]{_key_block_compressed[0],_key_block_compressed[(int) (+1)],_key_block_compressed[(int) (+2)],_key_block_compressed[(int) (+3)]});
            //int adler32 = getInt(_key_block_compressed[(int) (+4)],_key_block_compressed[(int) (+5)],_key_block_compressed[(int) (+6)],_key_block_compressed[(int) (+7)]);
            if(key_block_compression_type.equals(new String(new byte[]{0,0,0,0}))){
                //无需解压
                System.out.println("no compress!");
                key_block = new byte[(int) (_key_block_compressed.length-start-8)];
                System.arraycopy(_key_block_compressed, (int)(start+8), key_block, 0,key_block.length);
            }else if(key_block_compression_type.equals(new String(new byte[]{1,0,0,0})))
            {
                //key_block = lzo_decompress(_key_block_compressed,(int) (start+_number_width),(int)(compressedSize-_number_width));
            	MInt len = new MInt((int) infoI.key_block_decompressed_size);
            	key_block = new byte[len.v];
                byte[] arraytmp = new byte[(int) compressedSize];
                //show(arraytmp.length+"哈哈哈"+compressedSize);
                System.arraycopy(_key_block_compressed, (int)(+8), arraytmp, 0,(int) (compressedSize-8));
                //CMN.show("_key_block_compressed");
                //ripemd128.printBytes(_key_block_compressed,(int) (start+8),(int)(compressedSize-8));
                //CMN.show(infoI.key_block_decompressed_size+"~"+infoI.key_block_compressed_size);
                //CMN.show(infoI.key_block_decompressed_size+"~"+compressedSize);
                MiniLZO.lzo1x_decompress(arraytmp,arraytmp.length,key_block,len);
                //System.out.println("look up LZO decompressing key blocks done!");
            }
            else if(key_block_compression_type.equals(new String(new byte[]{02,00,00,00}))){
                key_block = zlib_decompress(_key_block_compressed,(int) (+8),(int)(compressedSize-8));

                /*Inflater inf = new Inflater();
                inf.setInput(_key_block_compressed,(int) (+8),(int)(compressedSize-8));
                key_block = new byte[(int) infoI.key_block_decompressed_size];
                try {
					int ret = inf.inflate(key_block,0,(int)(infoI.key_block_decompressed_size));
				} catch (DataFormatException e) {e.printStackTrace();}
                */
            }
            /*!!spliting curr Key block*/
            int key_start_index = 0;
            int key_end_index=0;
            
            int keyCounter = 0;
            
            ByteBuffer sf = ByteBuffer.wrap(key_block);
            
            while(key_start_index < key_block.length){
            	long key_id;
            	if(_version<2)
            		key_id = sf.getInt(key_start_index);//Key_ID
            	else
            		key_id = sf.getLong(key_start_index);//Key_ID
                //show("key_id"+key_id);
                key_end_index = key_start_index + _number_width;  
                SK_DELI:
				  while(key_end_index+delimiter_width<key_block.length){
					for(int sker=0;sker<delimiter_width;sker++) {
						if(key_block[key_end_index+sker]!=0) {
							key_end_index+=delimiter_width;
							continue SK_DELI;
						}
					}
					break;
				 }

                //show("key_start_index"+key_start_index);
                //byte[] arraytmp = new byte[key_end_index-(key_start_index+_number_width)];
                //System.arraycopy(key_block,key_start_index+_number_width, arraytmp, 0,arraytmp.length);


                String key_text = null;

				//key_text = new String(arraytmp,_encoding);
				//key_text = new String(key_block,key_start_index+_number_width,key_end_index-(key_start_index+_number_width));

                infoI_cache.keys[keyCounter] = new byte[key_end_index-(key_start_index+_number_width)];
                System.arraycopy(key_block, key_start_index+_number_width, infoI_cache.keys[keyCounter], 0, key_end_index-(key_start_index+_number_width));
                
                
				//CMN.show(keyCounter+":::"+key_text);
                key_start_index = key_end_index + delimiter_width;
                
                //CMN.show(infoI.keys.length+"~~~"+keyCounter+"~~~"+infoI.num_entries);
                //infoI_cache.keys[keyCounter]=key_text;
                infoI_cache.key_offsets[keyCounter]=key_id;
                keyCounter++;
            }

            //assert(adler32 == (BU.calcChecksum(key_block)));
			} catch (FileNotFoundException e2) {
				e2.printStackTrace();
			} catch (IOException e2) {
				e2.printStackTrace();
			}	
        }
    }
    
    long[] keyBlocksHeaderTextKeyID;
	public static long stst;
    //int counter=0;
    public void fetch_keyBlocksHeaderTextKeyID(){
    	int blockId = 0;
    	keyBlocksHeaderTextKeyID = new long[(int)_num_key_blocks];
    	for(key_info_struct infoI:_key_block_info_list){
    		try {
            long start = infoI.key_block_compressed_size_accumulator;
            long compressedSize;
            byte[] key_block = new byte[1];
            if(blockId==_key_block_info_list.length-1)
                compressedSize = _key_block_size - _key_block_info_list[_key_block_info_list.length-1].key_block_compressed_size_accumulator;
            else
                compressedSize = _key_block_info_list[blockId+1].key_block_compressed_size_accumulator-infoI.key_block_compressed_size_accumulator;
            
			DataInputStream data_in = new DataInputStream(new FileInputStream(f));
			data_in.skip(_key_block_offset+start);
			byte[]  _key_block_compressed = new byte[(int) compressedSize];
			data_in.read(_key_block_compressed, (int)(0), _key_block_compressed.length);
			data_in.close();
			
            byte[] key_block_compression_type = new byte[]{_key_block_compressed[(int) 0],_key_block_compressed[(int) (+1)],_key_block_compressed[(int) (+2)],_key_block_compressed[(int) (+3)]};
            if(compareByteArrayIsPara(key_block_compression_type, _zero4)){
                //无需解压
                System.out.println("no compress!");
                key_block = new byte[(int) (_key_block_size-start-8)];
                System.arraycopy(_key_block_compressed, (int)(+8), key_block, 0,(int) (_key_block_size-start-8));
            }else if(compareByteArrayIsPara(key_block_compression_type, _1zero3))
            {
                //key_block = lzo_decompress(_key_block_compressed,(int) (start+_number_width),(int)(compressedSize-_number_width));
            	MInt len = new MInt((int) infoI.key_block_decompressed_size);
            	key_block = new byte[len.v];
                byte[] arraytmp = new byte[(int) compressedSize];
                //show(arraytmp.length+"哈哈哈"+compressedSize);
                System.arraycopy(_key_block_compressed, (int)(+8), arraytmp, 0,(int) (compressedSize-8));
            	MiniLZO.lzo1x_decompress(arraytmp,arraytmp.length,key_block,len);
                //System.out.println("look up LZO decompressing key blocks done!");
            }
            else if(compareByteArrayIsPara(key_block_compression_type, _2zero3)){
                key_block = zlib_decompress(_key_block_compressed,(int) (+8),(int)(compressedSize-8));
                //System.out.println("zip!");
            }
            //!!spliting curr Key block
            //ByteBuffer sf = ByteBuffer.wrap(key_block);//must outside of while...
            /*主要耗时步骤
		            主要耗时步骤
		            主要耗时步骤*/
            
            	if(_version<2)
            		keyBlocksHeaderTextKeyID[blockId] = getInt(key_block[0],key_block[1],key_block[2],key_block[3]);
            	else
            		//keyBlocksHeaderTextKeyID[blockId] = getLong(key_block[0],key_block[1],key_block[2],key_block[3],key_block[4],key_block[5],key_block[6],key_block[7]);
        		keyBlocksHeaderTextKeyID[blockId] = getLong(key_block);

                blockId++;

			} catch (FileNotFoundException e) {
				
				e.printStackTrace();
			} catch (IOException e) {
				
				e.printStackTrace();
			}
        }
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
        keyword = keyword.toLowerCase().replaceAll(replaceReg,emptyStr);
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
   

   public void printRecordInfo() throws IOException{
        for(int i=0; i<_record_info_struct_list.length; i++){
        	record_info_struct RinfoI = _record_info_struct_list[i];
        	show("RinfoI_compressed_size="+RinfoI.compressed_size);
        	
        }	
    }
     
    
    public void printAllContents() throws IOException{
    	OutputStreamWriter fOut = new OutputStreamWriter(new FileOutputStream(f.getAbsolutePath()+".txt"),_encoding);

        DataInputStream data_in = new DataInputStream(new FileInputStream(f));
        // record block info section
        data_in.skipBytes( (int) (_record_block_offset+_number_width*4+_num_record_blocks*2*_number_width));
        
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
            byte[] record_block = new byte[1];
            // no compression
            if(record_block_type_str.equals(new String(new byte[]{0,0,0,0}))){
            	record_block = new byte[(int) (compressed_size-8)];
            	System.arraycopy(record_block_compressed, 8, record_block, 0, record_block.length-8);
            }
            // lzo compression
            else if(record_block_type_str.equals(new String(new byte[]{1,0,0,0}))){
                //stst=System.currentTimeMillis(); //获取开始时间 
                record_block = new byte[(int) decompressed_size];
                MInt len = new MInt((int) decompressed_size);
                byte[] arraytmp = new byte[(int) compressed_size];
                System.arraycopy(record_block_compressed, 8, arraytmp, 0,(int) (compressed_size-8));
                MiniLZO.lzo1x_decompress(arraytmp,(int) compressed_size,record_block,len);
            	//System.out.println("get Record LZO decompressing key blocks done!") ;
                //System.out.println("解压Record耗时："+(System.currentTimeMillis()-st));
            }
            // zlib compression
            else if(record_block_type_str.equals(new String(new byte[]{02,00,00,00}))){
                record_block = zlib_decompress(record_block_compressed,8);
            }
            // notice not that adler32 return signed value
            //CMN.show(adler32+"!:"+BU.calcChecksum(record_block) );
            assert(adler32 == (BU.calcChecksum(record_block) ));
            assert(record_block.length == decompressed_size );
 //当前内容块解压完毕
            

            String record_str = new String(record_block,_charset); 	
            // substitute styles
            //if self._substyle and self._stylesheet:
            //    record = self._substitute_stylesheet(record);
            show(record_str);       
        	
            fOut.append(record_str).append("\n");
            
        }
        data_in.close();
        fOut.close();
    }
    
    volatile int thread_number_count = 1;
    int split_recs_thread_number;
    public void flowerFindAllContents(String key,int selfAtIdx,int theta) throws IOException, DataFormatException{
    	final byte[][] keys = new byte[][] {key.getBytes(_charset),key.toUpperCase().getBytes(_charset),(key.substring(0,1).toUpperCase()+key.substring(1)).getBytes(_charset)};

    	key = key.toLowerCase();
		String upperKey = key.toUpperCase();
    	final byte[][][] matcher = new byte[upperKey.equals(key)?1:2][][];
		matcher[0] = flowerSanLieZhi(key);
		if(matcher.length==2)
		matcher[1] = flowerSanLieZhi(upperKey);
		
 	    if(_key_block_info_list==null) read_key_block_info();

    	if(record_block==null)
    		decode_record_block_header();
    	
        fetch_keyBlocksHeaderTextKeyID();
        
        
        split_recs_thread_number = _num_record_blocks<6?1:(int) (_num_record_blocks/6);//Runtime.getRuntime().availableProcessors()/2*2+10;
        split_recs_thread_number = split_keys_thread_number>16?6:split_keys_thread_number;
        final int thread_number = Math.min(Runtime.getRuntime().availableProcessors()/2*2+2, split_keys_thread_number);
	     
	     
        final int step = (int) (_num_record_blocks/split_recs_thread_number);
    	final int yuShu=(int) (_num_record_blocks%split_recs_thread_number);
    	
		if(combining_search_tree_4==null)
			combining_search_tree_4 = new ArrayList[split_recs_thread_number];
    	
		poolEUSize = dirtykeyCounter =0;
		
        ExecutorService fixedThreadPool = Executors.newFixedThreadPool(thread_number);
        for(int ti=0; ti<split_recs_thread_number; ti++){//分  thread_number 股线程运行
        	if(searchCancled) break;
	    	final int it = ti;
	    	if(split_recs_thread_number>thread_number) while (poolEUSize>=thread_number) {
				  try {
					Thread.sleep(1);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}  
				} 

            if(combining_search_tree_4[it]==null)
            	combining_search_tree_4[it] = new ArrayList<Integer>();
            
        	if(split_recs_thread_number>thread_number) countDelta(1);
        	
	        fixedThreadPool.execute(
	        new Runnable(){@Override public void run() 
	        {
	        	if(searchCancled) { poolEUSize=0; return; }
	            final byte[] record_block_compressed = new byte[(int) maxComRecSize];//!!!避免反复申请内存
	            final byte[] record_block = new byte[(int) maxDecompressedSize];//!!!避免反复申请内存
	            final cached_key_block infoI_cacheI = new cached_key_block();
	            try 
	            {
		            FileInputStream data_in = new FileInputStream(f);
		            data_in.skip(_record_info_struct_list[it*step].compressed_size_accumulator+_record_block_offset+_number_width*4+_num_record_blocks*2*_number_width);
		            int jiaX=0;
		            if(it==split_recs_thread_number-1) jiaX=yuShu;
	            	for(int i=it*step; i<it*step+step+jiaX; i++)//_num_record_blocks 
	            	{
	            		if(searchCancled) { poolEUSize=0; return; }
	                    record_info_struct RinfoI = _record_info_struct_list[i];
	                    
	                    int compressed_size = (int) RinfoI.compressed_size;
	                    int decompressed_size = (int) RinfoI.decompressed_size;
	                    data_in.read(record_block_compressed,0, compressed_size);//,0, compressed_size
	                    
	                    //解压开始
	                    if(compareByteArrayIsPara(record_block_compressed,0,_zero4)){
	                        System.arraycopy(record_block_compressed, 8, record_block, 0, compressed_size-8);
	                    }
	                    else if(compareByteArrayIsPara(record_block_compressed,0,_1zero3)){
	                        //MInt len = new MInt((int) decompressed_size);
	                        //byte[] arraytmp = new byte[ compressed_size];
	                        //System.arraycopy(record_block_compressed, 8, arraytmp, 0, (compressed_size-8));
	                        //MiniLZO.lzo1x_decompress(arraytmp,(int) compressed_size,record_block,len);
	                        new LzoDecompressor1x().decompress(record_block_compressed, 8, (compressed_size-8), record_block, 0, new lzo_uintp());
	                    }
	                    else if(compareByteArrayIsPara(record_block_compressed,0,_2zero3)){    
	                        Inflater inf = new Inflater();
	                        inf.setInput(record_block_compressed,8,compressed_size-8);
	                        int ret = inf.inflate(record_block,0,decompressed_size);  		
	                    	//CMN.show("asdasd"+ret);		
	                    }
	                    //内容块解压完毕
                    	long off = RinfoI.decompressed_size_accumulator;
                    	int key_block_id = binary_find_closest(keyBlocksHeaderTextKeyID,off);
                    	OUT:
                    	while(true) {
                    		if(key_block_id>=_key_block_info_list.length) break;
                    		prepareItemByKeyInfo(null,key_block_id,infoI_cacheI);
                    		long[] ko = infoI_cacheI.key_offsets;
                    		//show("binary_find_closest "+binary_find_closest(ko,off)+"  :  "+off);
	                    	for(int relative_pos=binary_find_closest(ko,off);relative_pos<ko.length;relative_pos++) {
	                    		
	                    		
	                    		int recordodKeyLen = 0;
		                    	if(relative_pos<ko.length-1){//不是最后一个entry
		                    		recordodKeyLen=(int) (ko[relative_pos+1]-ko[relative_pos]);
		                    	}
		                    	else if(key_block_id<keyBlocksHeaderTextKeyID.length-1){//不是最后一块key block
		                    		recordodKeyLen=(int) (keyBlocksHeaderTextKeyID[key_block_id+1]-ko[relative_pos]);
		                    	}else {
		                    		recordodKeyLen = (int) (decompressed_size-(ko[ko.length-1]-RinfoI.decompressed_size_accumulator));
		                    	}
		                    	
		                    	//show(getEntryAt((int) (relative_pos+_key_block_info_list[key_block_id].num_entries_accumulator),infoI_cacheI));
		                    	//CMN.show(record_block.length-1+" ko[relative_pos]: "+ko[relative_pos]+" recordodKeyLen: "+recordodKeyLen+" end: "+(ko[relative_pos]+recordodKeyLen-1));
		                    	
		                    	/*
		                    	if(getEntryAt((int) (relative_pos+_key_block_info_list[key_block_id].num_entries_accumulator),infoI_cacheI).equals("鼓钟"))
		                    	{		                    	
		                    		CMN.show("decompressed_size: "+decompressed_size+" record_block: "+(record_block.length-1)+" ko[relative_pos]: "+ko[relative_pos]+" recordodKeyLen: "+recordodKeyLen+" end: "+(ko[relative_pos]+recordodKeyLen-1));

			                    	CMN.show(flowerIndexOf(record_block,(int) (ko[relative_pos]-RinfoI.decompressed_size_accumulator),recordodKeyLen,matcher,0,0)+"");
			                    	

			                    	CMN.show(new String(record_block,(int) (ko[relative_pos]-RinfoI.decompressed_size_accumulator)+248,10,_charset));
			                    	CMN.show(recordodKeyLen+" =recordodKeyLen");
			                    	CMN.show((ko[relative_pos]-RinfoI.decompressed_size_accumulator+recordodKeyLen)+" sdf "+RinfoI.decompressed_size+" sdf "+RinfoI.compressed_size);
			                    	
		                    		CMN.show("\r\n"+new String(record_block,(int) (ko[relative_pos]-RinfoI.decompressed_size_accumulator),recordodKeyLen,_charset));

		                    	}*/
		                    	
		                    	if(ko[relative_pos]-RinfoI.decompressed_size_accumulator+recordodKeyLen>RinfoI.decompressed_size) {
		                    		//show("break OUT");
		                    		break OUT;
		                    	}
		                    	
		                    	
		                    	//if(indexOf(record_block,(int) (ko[relative_pos]-RinfoI.decompressed_size_accumulator),recordodKeyLen,keys[0],0,keys[0].length,0)!=-1) {
		                    	if(flowerIndexOf(record_block,(int) (ko[relative_pos]-RinfoI.decompressed_size_accumulator),recordodKeyLen,matcher,0,0)!=-1) {
		                    		int pos = (int) (relative_pos+_key_block_info_list[key_block_id].num_entries_accumulator);
		                    		fuzzyKeyCounter++;
		                    		
		                    		//String LexicalEntry = getEntryAt(pos,infoI_cacheI);
		                    		//show(getEntryAt(pos,infoI_cacheI));
		                    		//ripemd128.printBytes(record_block,offIdx,recordodKeyLen);
		                    		
		                    		combining_search_tree_4[it].add(pos);
		                    	}
		                    	dirtykeyCounter++;
	                    	}
	                    	key_block_id++;
                    	}
	                }
	            	data_in.close();
	                
	            } catch (Exception e) {e.printStackTrace();}
            	thread_number_count--;
	            if(split_recs_thread_number>thread_number) countDelta(-1);
	        }});
        }
        fixedThreadPool.shutdown();
		try {
			fixedThreadPool.awaitTermination(1, TimeUnit.MINUTES);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
    }
    
    
	
    /*
     * https://stackoverflow.com/questions/21341027/find-indexof-a-byte-array-within-another-byte-array
     * Gustavo Mendoza's Answer*/
    static int indexOf(byte[] source, int sourceOffset, int sourceCount, byte[] target, int targetOffset, int targetCount, int fromIndex) {
        if (fromIndex >= sourceCount) {
            return (targetCount == 0 ? sourceCount : -1);
        }
        if (fromIndex < 0) {
            fromIndex = 0;
        }
        if (targetCount == 0) {
            return fromIndex;
        }

        byte first = target[targetOffset];
        int max = sourceOffset + (sourceCount - targetCount);

        for (int i = sourceOffset + fromIndex; i <= max; i++) {
            /* Look for first character. */
            if (source[i] != first) {
                while (++i <= max && source[i] != first)
                    ;
            }

            /* Found first character, now look at the rest of v2 */
            if (i <= max) {
                int j = i + 1;
                int end = j + targetCount - 1;
                for (int k = targetOffset + 1; j < end && source[j] == target[k]; j++, k++)
                    ;

                if (j == end) {
                    /* Found whole string. */
                    return i - sourceOffset;
                }
            }
        }
        return -1;
    }
   
    //for list view
	public String getEntryAt(int position) {
		if(position==-1)
			return "about:";
		if(_key_block_info_list==null) read_key_block_info();
        int blockId = accumulation_blockId_tree.xxing(new myCpr(position,1)).getKey().value;
        //CMN.show(blockId+"");
        key_info_struct infoI = _key_block_info_list[blockId];
        prepareItemByKeyInfo(infoI,blockId,null);
        //CMN.show(infoI.keys.length+":"+(position-infoI.num_entries_accumulator));
        return new String(infoI_cache_.keys[(int) (position-infoI.num_entries_accumulator)],_charset);
	}
	
	public String getEntryAt(int position,cached_key_block infoI_cacheI) {
		if(_key_block_info_list==null) read_key_block_info();
        int blockId = accumulation_blockId_tree.xxing(new myCpr(position,1)).getKey().value;
        key_info_struct infoI = _key_block_info_list[blockId];
        String pre = new String(infoI_cacheI.keys[(int) (position-infoI.num_entries_accumulator)],_charset);
        return pre;
	}
	
	public int split_keys_thread_number;
	//public ArrayList<myCpr<String,Integer>>[] combining_search_tree;
	public ArrayList<additiveMyCpr1>[] combining_search_tree2;
	public ArrayList<additiveMyCpr1>[] combining_search_tree233;
	public ArrayList<Integer>[] combining_search_tree_4;



   public void countDelta(int delta) {
       Lock lock = new ReentrantLock();
       lock.lock();
       try {
           poolEUSize+=delta;
       } catch (Exception e) {
       }finally {
           lock.unlock();
       }
   }
   
   public volatile boolean searchCancled=false;
   public volatile int dirtykeyCounter;
   public volatile static int fuzzyKeyCounter ;
   volatile int poolEUSize;
   
	byte[] keywordArray;
	byte[] keywordArrayC1;
	byte[] keywordArrayCA;
   

  public int thread_number,step,yuShu;
  public void calcFuzzySpace(){
	     //final String fkeyword = keyword.toLowerCase().replaceAll(replaceReg,emptyStr);
	     //int entryIdx = 0;
	     //show("availableProcessors: "+Runtime.getRuntime().availableProcessors());
	     //show("keyBLockN: "+_key_block_info_list.length);
	     split_keys_thread_number = _num_key_blocks<6?1:(int) (_num_key_blocks/6);//Runtime.getRuntime().availableProcessors()/2*2+10;
	     split_keys_thread_number = split_keys_thread_number>16?6:split_keys_thread_number;
	     thread_number = Math.min(Runtime.getRuntime().availableProcessors()/2*2+2, split_keys_thread_number);
	     

	     thread_number_count = split_keys_thread_number;
	     step = (int) (_num_key_blocks/split_keys_thread_number);
	 	 yuShu=(int) (_num_key_blocks%split_keys_thread_number);
	 	 
  }
  
//XXX2
public void flowerFindAllKeys(String key,
        final int SelfAtIdx,int theta) throws UnsupportedEncodingException
	{		
	  if(_key_block_info_list==null) read_key_block_info();


  	key = key.toLowerCase();
	String upperKey = key.toUpperCase();
  	final byte[][][] matcher = new byte[upperKey.equals(key)?1:2][][];
	matcher[0] = flowerSanLieZhi(key);
	if(matcher.length==2)
	matcher[1] = flowerSanLieZhi(upperKey);
  	
   //final String fkeyword = keyword.toLowerCase().replaceAll(replaceReg,emptyStr);
   //int entryIdx = 0;
   show("availableProcessors: "+Runtime.getRuntime().availableProcessors());
   show("keyBLockN: "+_key_block_info_list.length);
   split_keys_thread_number = _num_key_blocks<6?1:(int) (_num_key_blocks/6);//Runtime.getRuntime().availableProcessors()/2*2+10;
   final int thread_number = Math.min(Runtime.getRuntime().availableProcessors()/2*2+5, split_keys_thread_number);
   
   poolEUSize = dirtykeyCounter =0;
   
   thread_number_count = split_keys_thread_number;
   final int step = (int) (_num_key_blocks/split_keys_thread_number);
	  final int yuShu=(int) (_num_key_blocks%split_keys_thread_number);

	  ExecutorService fixedThreadPoolmy = Executors.newFixedThreadPool(thread_number);
	   
	  show("~"+step+"~"+split_keys_thread_number+"~"+_num_key_blocks);
	  if(combining_search_tree2==null)
		  combining_search_tree2 = new ArrayList[split_keys_thread_number];
	  
   for(int ti=0; ti<split_keys_thread_number; ti++){//分  thread_number 股线程运行
	   		if(searchCancled) break;
	        if(split_keys_thread_number>thread_number) while (poolEUSize>=thread_number) {  
	              try {
	    			Thread.sleep(1);
		    		} catch (InterruptedException e) {
		    			e.printStackTrace();
		    		}  
	        } 
	        if(split_keys_thread_number>thread_number) countDelta(1);
	    	final int it = ti;
	        fixedThreadPoolmy.execute(
	        new Runnable(){@Override public void run() 
	        {
	        	if(searchCancled) { poolEUSize=0; return; }
	            int jiaX=0;
	            if(it==split_keys_thread_number-1) jiaX=yuShu;
	            final byte[] key_block = new byte[65536];/*分配资源 32770   65536*/
	            if(combining_search_tree2[it]==null)
	            	combining_search_tree2[it] = new ArrayList<additiveMyCpr1>();
           	
	            
	            int compressedSize_many = 0;
	           //小循环	
	            for(int blockId=it*step; blockId<it*step+step+jiaX; blockId++){
	                   //prepareItemByKeyInfo(_key_block_info_list[blockCounter],blockCounter);
	                   key_info_struct infoI = _key_block_info_list[blockId];
	                   if(blockId==_key_block_info_list.length-1)
	                	   compressedSize_many += _key_block_size - _key_block_info_list[_key_block_info_list.length-1].key_block_compressed_size_accumulator;
	                   else
	                	   compressedSize_many += _key_block_info_list[blockId+1].key_block_compressed_size_accumulator-infoI.key_block_compressed_size_accumulator;
	            }
	            
            long start = _key_block_info_list[it*step].key_block_compressed_size_accumulator;

            try {
					DataInputStream data_in = new DataInputStream(new FileInputStream(f));
					data_in.skip(_key_block_offset+start);
					byte[]  _key_block_compressed_many = new byte[ compressedSize_many];
					data_in.read(_key_block_compressed_many, 0, _key_block_compressed_many.length);
					data_in.close();
					
					//大循环	
					for(int blockId=it*step; blockId<it*step+step+jiaX; blockId++){
						if(searchCancled) { poolEUSize=0; return; }
						
						int compressedSize;
						key_info_struct infoI = _key_block_info_list[blockId];
						if(blockId==_key_block_info_list.length-1)
							compressedSize = (int) (_key_block_size - _key_block_info_list[_key_block_info_list.length-1].key_block_compressed_size_accumulator);
						else
							compressedSize = (int) (_key_block_info_list[blockId+1].key_block_compressed_size_accumulator-infoI.key_block_compressed_size_accumulator);
						
						int startI = (int) (infoI.key_block_compressed_size_accumulator-start);
						   
						
						//byte[] record_block_type = new byte[]{_key_block_compressed_many[(int) startI],_key_block_compressed_many[(int) (startI+1)],_key_block_compressed_many[(int) (startI+2)],_key_block_compressed_many[(int) (startI+3)]};
						//int adler32 = getInt(_key_block_compressed_many[(int) (startI+4)],_key_block_compressed_many[(int) (startI+5)],_key_block_compressed_many[(int)(startI+6)],_key_block_compressed_many[(int) (startI+7)]);
	
						if(compareByteArrayIsPara(_key_block_compressed_many,startI,_zero4)){
							  System.arraycopy(_key_block_compressed_many, (startI+8), key_block, 0, (int)(_key_block_size-8));
						}else if(compareByteArrayIsPara(_key_block_compressed_many,startI,_1zero3))
						{
							  MInt len = new MInt();//(int) infoI.key_block_decompressed_size
							  byte[] arraytmp = new byte[(int) compressedSize];
							  System.arraycopy(_key_block_compressed_many, (startI+8), arraytmp, 0, (compressedSize-8));
							  MiniLZO.lzo1x_decompress(arraytmp,arraytmp.length,key_block,len);
						}
						else if(compareByteArrayIsPara(_key_block_compressed_many,startI,_2zero3))
						{
								//byte[] key_block2 = zlib_decompress(_key_block_compressed_many,(int) (startI+8),(int)(compressedSize-8));
								//System.arraycopy(key_block2, 0, key_block, 0, key_block2.length);
								//find_in_keyBlock(key_block2,infoI,keyword,SelfAtIdx,it);
								
								Inflater inf = new Inflater();
								//CMN.show(_key_block_compressed_many.length+";;"+(startI+8)+";;"+(compressedSize-8));
								inf.setInput(_key_block_compressed_many,(startI+8),(compressedSize-8));
								//key_block = new byte[(int) infoI.key_block_decompressed_size];
								try {
								  //CMN.show(""+infoI.key_block_decompressed_size);
									int ret = inf.inflate(key_block,0,(int)(infoI.key_block_decompressed_size));
								} catch (DataFormatException e) {e.printStackTrace();}
						}
						
						find_in_keyBlock(key_block,infoI,matcher,SelfAtIdx,it);
						
						
						
					} 
	            }
            catch (Exception e1) {e1.printStackTrace();}
           thread_number_count--;
           if(split_keys_thread_number>thread_number) countDelta(-1);
	        }});
   }//任务全部分发完毕
	fixedThreadPoolmy.shutdown();
	try {
		fixedThreadPoolmy.awaitTermination(1, TimeUnit.MINUTES);
	} catch (InterruptedException e1) {
		e1.printStackTrace();
	}
}  


HashSet<Integer> miansi = new HashSet<>();//. is 免死金牌  that exempt you from death for just one time
HashSet<Integer> yueji = new HashSet<>();//* is 越级天才, i.e., super super genius leap

int flowerIndexOf(byte[] source, int sourceOffset, int sourceCount, byte[][][] matchers,int marcherOffest, int fromIndex) throws UnsupportedEncodingException 
{
	int lastSeekLetSize=0;
	while(fromIndex<sourceCount) {
		//CMN.show("==");
		//int idx = -1;
		int fromIndex_=fromIndex;
		boolean isSeeking=true;
		boolean Matched = false;
		for(int lexiPartIdx=marcherOffest;lexiPartIdx<matchers[0].length;lexiPartIdx++) {
			//if(fromIndex_>sourceCount-1) return -1;
			//CMN.show("stst: "+sourceCount+"::"+(fromIndex_+seekPos)+" fromIndex_: "+fromIndex_+" seekPos: "+seekPos+" lexiPartIdx: "+lexiPartIdx);
	
			//CMN.show("seekPos: "+seekPos+" lexiPartIdx: "+lexiPartIdx+" fromIndex_: "+fromIndex_);
			if(miansi.contains(lexiPartIdx)) {
				if(lexiPartIdx==matchers[0].length-1) {
					if(fromIndex_>=sourceCount)
						return -1;
					return fromIndex-lastSeekLetSize;//HERE
				}//Matched=true
				//CMN.show("miansi: "+lexiPartIdx);
				//CMN.show("miansi: "+sourceCount+"::"+(fromIndex_+seekPos)+"sourceL: "+source.length);
				//CMN.show("jumpped c is: "+new String(source,fromIndex_+seekPos,Math.min(4, sourceCount-(fromIndex_+seekPos-sourceOffset)),_encoding).substring(0, 1));
				int newSrcCount = Math.min(4, sourceCount-(fromIndex_));
				if(newSrcCount<=0)
					return -1;
				String c = new String(source,sourceOffset+fromIndex_,newSrcCount,_charset);
				int jumpShort = c.substring(0, 1).getBytes(_charset).length;
				fromIndex_+=jumpShort;
				continue;
			}else if(yueji.contains(lexiPartIdx)) {
				if(lexiPartIdx==matchers[0].length-1) 
					return fromIndex-lastSeekLetSize;//HERE
				if(flowerIndexOf(source, sourceOffset+fromIndex_,sourceCount-(fromIndex_), matchers,lexiPartIdx+1, 0)!=-1){
					return fromIndex-lastSeekLetSize;
				}
				return -1;
			}
			Matched = false;
			if(isSeeking) {		
				int seekPos=-1;
				int newSeekPos=-1;
				for(byte[][] marchLet:matchers) {
	    			//if(marchLet==null) break;
	    			if(newSeekPos==-1)
	    				newSeekPos = indexOf(source, sourceOffset, sourceCount, marchLet[lexiPartIdx],0,marchLet[lexiPartIdx].length, fromIndex_) ;
	    			else        				
	    				newSeekPos = indexOf(source, sourceOffset, newSeekPos, marchLet[lexiPartIdx],0,marchLet[lexiPartIdx].length, fromIndex_) ;
	    			//Lala=MinimalIndexOf(source, sourceOffset, sourceCount, new byte[][] {matchers[0][lexiPartIdx],matchers[1][lexiPartIdx]},0,-1,fromIndex_+seekPos);
	    			if(newSeekPos!=-1) {
	    				seekPos=newSeekPos;
						lastSeekLetSize=matchers[0][lexiPartIdx].length;
						Matched=true;
					}
				}
				//CMN.show("seekPos:"+seekPos+" fromIndex_: "+fromIndex_);
				if(!Matched)
					return -1;
				seekPos+=lastSeekLetSize;
				fromIndex=fromIndex_=seekPos;
				isSeeking=false;
				continue;
				}
			else {
				//CMN.show("deadline"+fromIndex_+" "+sourceCount);
				if(fromIndex_>sourceCount-1) {
					//CMN.show("deadline reached"+fromIndex_+" "+sourceCount);
					return -1;
				}
				for(byte[][] marchLet:matchers) {
					if(marchLet==null) break;
					if(bingStartWith(source,sourceOffset,marchLet[lexiPartIdx],0,-1,fromIndex_)) {
						Matched=true;
		    			//CMN.show("matchedHonestily: "+sourceCount+"::"+(fromIndex_+seekPos)+" fromIndex_: "+fromIndex_+" seekPos: "+seekPos);
						//CMN.show("matchedHonestily: "+lexiPartIdx);
					}
				}
			}
			if(!Matched) {
				//CMN.show("Matched failed this round: "+lexiPartIdx);
				break;
			}
			fromIndex_+=matchers[0][lexiPartIdx].length;
		}
		if(Matched)
			return fromIndex-lastSeekLetSize;
	}
	return -1;
}


private byte[][] flowerSanLieZhi(String str) throws UnsupportedEncodingException {
	miansi.clear();
	yueji.clear();
	byte[][] res = new byte[str.length()][];
	for(int i=0;i<str.length();i++){
		String c = str.substring(i, i+1);
		if(c.equals("."))
			miansi.add(i);
		else if(c.equals("*"))
			yueji.add(i);
		else
			res[i] = c.getBytes(_charset);
	}
	return res;
}
 
 
    protected void find_in_keyBlock(byte[] key_block,key_info_struct infoI,byte[][][] matcher,int SelfAtIdx,int it) {
	 //!!spliting curr Key block
       int key_start_index = 0;
       //String delimiter;
       int key_end_index=0;
       //int keyCounter = 0;
       
       ByteBuffer sf = ByteBuffer.wrap(key_block);//must outside of while...
       int keyCounter = 0;
       while(key_start_index < infoI.key_block_decompressed_size){
     	  long key_id;
           if(_version<2)
        	      sf.position(4);
               //key_id = sf.getInt(key_start_index);//Key_ID
           else
        	   	  sf.position(8);
               //key_id = sf.getLong(key_start_index);//Key_ID
           //show("key_id"+key_id);
           key_end_index = key_start_index + _number_width;  

           SK_DELI:
		  while(true){
			for(int sker=0;sker<delimiter_width;sker++) {
				if(key_block[key_end_index+sker]!=0) {
					key_end_index+=delimiter_width;
					continue SK_DELI;
				}
			}
			break;
		  }
           
           if(true)
		try {
			//TODO: alter
			//xxxx
			int try_idx = flowerIndexOf(key_block,key_start_index+_number_width, key_end_index-(key_start_index+_number_width), matcher,0,0);

			
			if(try_idx!=-1){
				//复核 re-collate
				String LexicalEntry = new String(key_block,key_start_index+_number_width,key_end_index-(key_start_index+_number_width),_charset);
				//int LexicalEntryIdx = LexicalEntry.toLowerCase().indexOf(keyword.toLowerCase());
	         	//if(LexicalEntryIdx==-1) {
	         	//	key_start_index = key_end_index + width;
	         	//	dirtykeyCounter++;continue;
	         	//}
				//StringBuilder sb = new StringBuilder(LexicalEntry);
	         	//byte[] arraytmp = new byte[key_end_index-(key_start_index+_number_width)];
	         	//System.arraycopy(key_block,key_start_index+_number_width, arraytmp, 0,arraytmp.length);
				//additiveMyCpr1 tmpnode = new additiveMyCpr1(LexicalEntry,""+SelfAtIdx+""+((int) (infoI.num_entries_accumulator+keyCounter)));//new ArrayList<Integer>() new int[] {SelfAtIdx,(int) (infoI.num_entries_accumulator+keyCounter)}
				//tmpnode.value.add(SelfAtIdx);
				//tmpnode.value.add((int) (infoI.num_entries_accumulator+keyCounter));
				combining_search_tree2[it].add(new additiveMyCpr1(LexicalEntry,infoI.num_entries_accumulator+keyCounter));

	         	fuzzyKeyCounter++;
         }
		} catch (Exception e) {
			e.printStackTrace();
		}


           key_start_index = key_end_index + delimiter_width;
           keyCounter++;dirtykeyCounter++;
       }
       //assert(adler32 == (calcChecksum(key_block)));	
}

   protected void find_in_keyBlock(byte[] key_block,key_info_struct infoI,String keyword,int SelfAtIdx,int it) {
	 //!!spliting curr Key block
       int key_start_index = 0;
       int key_end_index=0;
       //int keyCounter = 0;
       
       ByteBuffer sf = ByteBuffer.wrap(key_block);//must outside of while...
       int keyCounter = 0;
       while(key_start_index < infoI.key_block_decompressed_size){
     	   //long key_id;
           if(_version<2)
        	      sf.position(4);
               //key_id = sf.getInt(key_start_index);//Key_ID
           else
        	   	  sf.position(8);
               //key_id = sf.getLong(key_start_index);//Key_ID
           //show("key_id"+key_id);
           
               key_end_index = key_start_index + _number_width;  
               SK_DELI:
     		  while(true){
     			for(int sker=0;sker<delimiter_width;sker++) {
     				if(key_block[key_end_index+sker]!=0) {
     					key_end_index+=delimiter_width;
     					continue SK_DELI;
     				}
     			}
     			break;
     		  }
               
     if(true)
		try {
			//TODO: alter
			//xxxx
			//if(new String(key_block,key_start_index+_number_width,key_end_index-(key_start_index+_number_width),_encoding).toLowerCase().indexOf(keyword.toLowerCase())!=-1) {
			int try_idx = indexOf(key_block,key_start_index+_number_width,key_end_index-(key_start_index+_number_width),keywordArray,0,keywordArray.length,0);
			if(try_idx==-1)
				try_idx = indexOf(key_block,key_start_index+_number_width,key_end_index-(key_start_index+_number_width),keywordArrayC1,0,keywordArray.length,0);
			if(try_idx==-1 && keyword.length()>0)
				try_idx = indexOf(key_block,key_start_index+_number_width,key_end_index-(key_start_index+_number_width),keywordArrayCA,0,keywordArray.length,0);
         
			if(try_idx!=-1){
				//复核 re-collate
				String LexicalEntry = new String(key_block,key_start_index+_number_width,key_end_index-(key_start_index+_number_width),_charset);
				int LexicalEntryIdx = LexicalEntry.toLowerCase().indexOf(keyword.toLowerCase());
	         	if(LexicalEntryIdx==-1) {
	         		key_start_index = key_end_index + delimiter_width;
	         		dirtykeyCounter++;continue;
	         	}
	         	
				//StringBuilder sb = new StringBuilder(LexicalEntry);
	         	//byte[] arraytmp = new byte[key_end_index-(key_start_index+_number_width)];
	         	//System.arraycopy(key_block,key_start_index+_number_width, arraytmp, 0,arraytmp.length);
				//additiveMyCpr1 tmpnode = new additiveMyCpr1(LexicalEntry,""+SelfAtIdx+""+((int) (infoI.num_entries_accumulator+keyCounter)));//new ArrayList<Integer>() new int[] {SelfAtIdx,(int) (infoI.num_entries_accumulator+keyCounter)}
				//tmpnode.value.add(SelfAtIdx);
				//tmpnode.value.add((int) (infoI.num_entries_accumulator+keyCounter));
				combining_search_tree2[it].add(new additiveMyCpr1(LexicalEntry,infoI.num_entries_accumulator+keyCounter));

	         	fuzzyKeyCounter++;
         }
		} catch (Exception e) {
			e.printStackTrace();
		}


           key_start_index = key_end_index + delimiter_width;
           keyCounter++;dirtykeyCounter++;
       }
       //assert(adler32 == (calcChecksum(key_block)));	
}

    
  
  byte[] key_block_cache = null;
  int key_block_cacheId=-1;
  int key_block_Splitted_flag=-1;
  //Advanced mdict conjunction search. 33nd
  public void size_confined_lookUp(String keyword,
          RBTree_additive combining_search_tree, int SelfAtIdx, int theta) 
		{
		if(_key_block_info_list==null) read_key_block_info();
		//int blockId = 0;
		//blockId = binary_find_closest_loose(block_blockId_search_list,keyword);
	    int blockId = _encoding.startsWith("GB")?reduce2(keyword.getBytes(_charset),0,_key_block_info_list.length):reduce(keyword,0,_key_block_info_list.length);
		if(blockId==-1) return;
		//show(_Dictionary_fName+_key_block_info_list[blockId].tailerKeyText+"1~"+_key_block_info_list[blockId].headerKeyText);
		while(blockId!=0 &&  compareByteArray(_key_block_info_list[blockId-1].tailerKeyText,keyword.getBytes())>=0)
			blockId--;

      
		byte[][][] matcher = new byte[2][][];
		matcher[0] = SanLieZhi(keyword);
		String upperKey = keyword.toUpperCase();
		if(!upperKey.equals(keyword))
			matcher[1] = SanLieZhi(upperKey);
		
		OUT:
		while(theta>0) {
			key_info_struct infoI = _key_block_info_list[blockId];
			boolean doHarvest=false;
			try {
				long start = infoI.key_block_compressed_size_accumulator;
				long compressedSize;
				if(!(key_block_cacheId==blockId && key_block_cache!=null)) {
					if(blockId==_key_block_info_list.length-1)
						compressedSize = _key_block_size - _key_block_info_list[_key_block_info_list.length-1].key_block_compressed_size_accumulator;
					else
						compressedSize = _key_block_info_list[blockId+1].key_block_compressed_size_accumulator-infoI.key_block_compressed_size_accumulator;
		             
		  			DataInputStream data_in =new DataInputStream(new FileInputStream(f));
		  			data_in.skip(_key_block_offset+start);
		  			byte[]  _key_block_compressed = new byte[(int) compressedSize];
		  			data_in.read(_key_block_compressed, (int)(0), _key_block_compressed.length);
		  			data_in.close();
		              
					String key_block_compression_type = new String(new byte[]{_key_block_compressed[0],_key_block_compressed[(int) (+1)],_key_block_compressed[(int) (+2)],_key_block_compressed[(int) (+3)]});
					//int adler32 = getInt(_key_block_compressed[(int) (+4)],_key_block_compressed[(int) (+5)],_key_block_compressed[(int) (+6)],_key_block_compressed[(int) (+7)]);
					if(key_block_compression_type.equals(new String(new byte[]{0,0,0,0}))){
					    //无需解压
						System.out.println("no compress!");
						key_block_cache = new byte[(int) (_key_block_compressed.length-start-8)];
						System.arraycopy(_key_block_compressed, (int)(start+8), key_block_cache, 0,key_block_cache.length);
					}else if(key_block_compression_type.equals(new String(new byte[]{1,0,0,0})))
					{
						MInt len = new MInt((int) infoI.key_block_decompressed_size);
						key_block_cache = new byte[len.v];
						byte[] arraytmp = new byte[(int) compressedSize];
						System.arraycopy(_key_block_compressed, (int)(+8), arraytmp, 0,(int) (compressedSize-8));
						MiniLZO.lzo1x_decompress(arraytmp,arraytmp.length,key_block_cache,len);
					}
					else if(key_block_compression_type.equals(new String(new byte[]{02,00,00,00}))){
						key_block_cache = zlib_decompress(_key_block_compressed,(int) (+8),(int)(compressedSize-8));
					}
					key_block_cacheId = blockId;
				}
				/*!!spliting curr Key block*/
				int key_start_index = 0;
				int key_end_index=0;
				int keyCounter = 0;
		
				while(key_start_index < key_block_cache.length){
				    key_end_index = key_start_index + _number_width;  
					
				    SK_DELI:
		     		  while(true){
		     			for(int sker=0;sker<delimiter_width;sker++) {
		     				if(key_block_cache[key_end_index+sker]!=0) {
		     					key_end_index+=delimiter_width;
		     					continue SK_DELI;
		     				}
		     			}
		     			break;
		     		  }
					
					if(!doHarvest)
					if(EntryStartWith(key_block_cache,key_start_index+_number_width,key_end_index-(key_start_index+_number_width),matcher)) {
						doHarvest=true;
					}
					if(doHarvest) {
						String key_text = null;
						try{
							key_text = new String(key_block_cache,key_start_index+_number_width,key_end_index-(key_start_index+_number_width),_charset);				
						}	
						catch (Exception e1) {
							e1.printStackTrace();
						}
						if(key_text.toLowerCase().startsWith(keyword)) {
							combining_search_tree.insert(key_text,SelfAtIdx,(int)(infoI.num_entries_accumulator+keyCounter));
						}else {//失匹配
							theta=0;
							break OUT;
						}
						if(--theta<0)
							break OUT;
					}
					
					key_start_index = key_end_index + delimiter_width;
					keyCounter++;
				}
	  			} catch (Exception e2) {
	  				e2.printStackTrace();
	  			}	
			if(!doHarvest)
				break OUT;
			++blockId;
		}
	}

  //联合搜索  4th gen
  public void size_confined_lookUp_neo2(String keyword,
          RBTree_additive combining_search_tree, int SelfAtIdx, int theta) 
		{
		if(_key_block_info_list==null) read_key_block_info();
		//int blockId = 0;
		//blockId = binary_find_closest_loose(block_blockId_search_list,keyword);
	    int blockId = _encoding.startsWith("GB")?reduce2(keyword.getBytes(_charset),0,_key_block_info_list.length):reduce(keyword,0,_key_block_info_list.length);
		if(blockId==-1) return;
		//show(_Dictionary_fName+_key_block_info_list[blockId].tailerKeyText+"1~"+_key_block_info_list[blockId].headerKeyText);
		while(blockId!=0 &&  compareByteArray(_key_block_info_list[blockId-1].tailerKeyText,keyword.getBytes())>=0)
			blockId--;

    
		byte[][][] matcher = new byte[2][][];
		matcher[0] = SanLieZhi(keyword);
		String upperKey = keyword.toUpperCase();
		if(!upperKey.equals(keyword))
			matcher[1] = SanLieZhi(upperKey);
		
		OUT:
		while(theta>0) {
			key_info_struct infoI = _key_block_info_list[blockId];
			boolean doHarvest=false;
			try {
				long start = infoI.key_block_compressed_size_accumulator;
				long compressedSize;
				if(!(key_block_cacheId==blockId && key_block_cache!=null)) {
					if(blockId==_key_block_info_list.length-1)
						compressedSize = _key_block_size - _key_block_info_list[_key_block_info_list.length-1].key_block_compressed_size_accumulator;
					else
						compressedSize = _key_block_info_list[blockId+1].key_block_compressed_size_accumulator-infoI.key_block_compressed_size_accumulator;
		             
		  			DataInputStream data_in =new DataInputStream(new FileInputStream(f));
		  			data_in.skip(_key_block_offset+start);
		  			byte[]  _key_block_compressed = new byte[(int) compressedSize];
		  			data_in.read(_key_block_compressed, (int)(0), _key_block_compressed.length);
		  			data_in.close();
		              
					String key_block_compression_type = new String(new byte[]{_key_block_compressed[0],_key_block_compressed[(int) (+1)],_key_block_compressed[(int) (+2)],_key_block_compressed[(int) (+3)]});
					//int adler32 = getInt(_key_block_compressed[(int) (+4)],_key_block_compressed[(int) (+5)],_key_block_compressed[(int) (+6)],_key_block_compressed[(int) (+7)]);
					if(key_block_compression_type.equals(new String(new byte[]{0,0,0,0}))){
					    //无需解压
						System.out.println("no compress!");
						key_block_cache = new byte[(int) (_key_block_compressed.length-start-8)];
						System.arraycopy(_key_block_compressed, (int)(start+8), key_block_cache, 0,key_block_cache.length);
					}else if(key_block_compression_type.equals(new String(new byte[]{1,0,0,0})))
					{
						MInt len = new MInt((int) infoI.key_block_decompressed_size);
						key_block_cache = new byte[len.v];
						byte[] arraytmp = new byte[(int) compressedSize];
						System.arraycopy(_key_block_compressed, (int)(+8), arraytmp, 0,(int) (compressedSize-8));
						MiniLZO.lzo1x_decompress(arraytmp,arraytmp.length,key_block_cache,len);
					}
					else if(key_block_compression_type.equals(new String(new byte[]{02,00,00,00}))){
						key_block_cache = zlib_decompress(_key_block_compressed,(int) (+8),(int)(compressedSize-8));
					}
					key_block_cacheId = blockId;
				}
				/*!!spliting curr Key block*/
				int key_start_index = 0;
				int key_end_index=0;
				int keyCounter = 0;
	
	
				while(key_start_index < key_block_cache.length){
					key_end_index = key_start_index + _number_width;  
				    SK_DELI:
		     		  while(true){
		     			for(int sker=0;sker<delimiter_width;sker++) {
		     				if(key_block_cache[key_end_index+sker]!=0) {
		     					key_end_index+=delimiter_width;
		     					continue SK_DELI;
		     				}
		     			}
		     			break;
		     		  }
					
					if(!doHarvest)
					if(EntryStartWith(key_block_cache,key_start_index+_number_width,key_end_index-(key_start_index+_number_width),matcher)) {
						doHarvest=true;
					}
					if(doHarvest) {
						String key_text = new String(key_block_cache,key_start_index+_number_width,key_end_index-(key_start_index+_number_width),_charset);				
						
						if(key_text.toLowerCase().startsWith(keyword)) {
							combining_search_tree.insert(key_text,SelfAtIdx,(int)(infoI.num_entries_accumulator+keyCounter));
						}else {//失匹配
							theta=0;
							break OUT;
						}
						if(--theta<0)
							break OUT;
					}
					
					key_start_index = key_end_index + delimiter_width;
					keyCounter++;
				}
	  			} catch (Exception e2) {
	  				e2.printStackTrace();
	  			}	
			if(!doHarvest)
				break OUT;
			++blockId;
		}
	}   
    //联合搜索  4th gen
  public void size_confined_lookUp_neo(String keyword,
          RBTree_additive combining_search_tree, int SelfAtIdx, int theta) 
		{
  			keyword = keyword.toLowerCase().replaceAll(replaceReg,emptyStr);
  			
			int idx = lookUp(keyword);
			if(idx!=-1) {
				for(int i=0;i<theta;i++) {
					String shouxiang = getEntryAt(idx+i);
					if(shouxiang.toLowerCase().replaceAll(replaceReg,emptyStr).startsWith(keyword)) {
						if(combining_search_tree!=null)
							combining_search_tree.insert(shouxiang,SelfAtIdx,idx+i);
						else
							combining_search_list.add(new myCpr(shouxiang,idx+i));
					}else
						break;
				}
			}	
			//combining_search_tree.insert(key_text,SelfAtIdx,(int)(infoI.num_entries_accumulator+keyCounter));
	}  
  
  

  int[][] scaler;
  public ArrayList<myCpr<String, Integer>> combining_search_list;
  //联合搜索  555
public void size_confined_lookUp5(String keyword,
        RBTree_additive combining_search_tree, int SelfAtIdx, int theta) 
		{
	keyword = keyword.toLowerCase().replaceAll(replaceReg,emptyStr);
	byte[] kAB = keyword.getBytes(_charset);
	if(_encoding.startsWith("GB")) {
		if(compareByteArray(_key_block_info_list[(int)_num_key_blocks-1].tailerKeyText,kAB)<0)
			return;
		if(compareByteArray(_key_block_info_list[0].headerKeyText,kAB)>0)
			return;
	}else {
		if(new String(_key_block_info_list[(int)_num_key_blocks-1].tailerKeyText,_charset).compareTo(keyword)<0)
			return;
		if(new String(_key_block_info_list[0].headerKeyText,_charset).compareTo(keyword)>0)
			return;
	}
	if(_key_block_info_list==null) read_key_block_info();
    int blockId = _encoding.startsWith("GB")?reduce2(kAB,0,_key_block_info_list.length):reduce(keyword,0,_key_block_info_list.length);
	if(blockId==-1) return;
	//show(_Dictionary_fName+_key_block_info_list[blockId].tailerKeyText+"1~"+_key_block_info_list[blockId].headerKeyText);
	//while(blockId!=0 &&  compareByteArray(_key_block_info_list[blockId-1].tailerKeyText,kAB)>=0)
	//	blockId--;

	boolean doHarvest=false;
	
	//OUT:
	while(theta>0) {
		key_info_struct infoI = _key_block_info_list[blockId];
		try {
			long start = infoI.key_block_compressed_size_accumulator;
			long compressedSize;
			if(!(key_block_cacheId==blockId && key_block_cache!=null)) {
				if(blockId==_key_block_info_list.length-1)
					compressedSize = _key_block_size - _key_block_info_list[_key_block_info_list.length-1].key_block_compressed_size_accumulator;
				else
					compressedSize = _key_block_info_list[blockId+1].key_block_compressed_size_accumulator-infoI.key_block_compressed_size_accumulator;
	             
	  			DataInputStream data_in =new DataInputStream(new FileInputStream(f));
	  			data_in.skip(_key_block_offset+start);
	  			byte[]  _key_block_compressed = new byte[(int) compressedSize];
	  			data_in.read(_key_block_compressed, (int)(0), _key_block_compressed.length);
	  			data_in.close();
	  			
	            //int adler32 = getInt(_key_block_compressed[(int) (+4)],_key_block_compressed[(int) (+5)],_key_block_compressed[(int) (+6)],_key_block_compressed[(int) (+7)]);
				if(compareByteArrayIsPara(_zero4, _key_block_compressed)){
					//System.out.println("no compress!");
					key_block_cache = new byte[(int) (_key_block_compressed.length-start-8)];
					System.arraycopy(_key_block_compressed, (int)(start+8), key_block_cache, 0,key_block_cache.length);
				}else if(compareByteArrayIsPara(_1zero3, _key_block_compressed))
				{
					MInt len = new MInt((int) infoI.key_block_decompressed_size);
					key_block_cache = new byte[len.v];
					byte[] arraytmp = new byte[(int) compressedSize];
					System.arraycopy(_key_block_compressed, (int)(+8), arraytmp, 0,(int) (compressedSize-8));
					MiniLZO.lzo1x_decompress(arraytmp,arraytmp.length,key_block_cache,len);
					//key_block_cache =  new byte[(int) infoI.key_block_decompressed_size];
                    //new LzoDecompressor1x().decompress(_key_block_compressed, 8, (int)(compressedSize-8), key_block_cache, 0, new lzo_uintp());
				}
				else if(compareByteArrayIsPara(_2zero3, _key_block_compressed)){
					//key_block_cache = zlib_decompress(_key_block_compressed,(int) (+8),(int)(compressedSize-8));
					key_block_cache =  new byte[(int) infoI.key_block_decompressed_size];
					Inflater inf = new Inflater();
                    inf.setInput(_key_block_compressed,8,(int)compressedSize-8);
                    int ret = inf.inflate(key_block_cache,0,key_block_cache.length);  
				}
				key_block_cacheId = blockId;
			}
			/*!!spliting curr Key block*/
			if(key_block_Splitted_flag!=blockId) {
				if(!doHarvest)
					scaler = new int[(int) infoI.num_entries][2];
				int key_start_index = 0;
				int key_end_index=0;
				int keyCounter = 0;
				
				while(key_start_index < key_block_cache.length){
					  key_end_index = key_start_index + _number_width;
					  SK_DELI:
					  while(true){
						for(int sker=0;sker<delimiter_width;sker++) {
							if(key_block_cache[key_end_index+sker]!=0) {
								key_end_index+=delimiter_width;
								continue SK_DELI;
							}
						}
						break;
					  }
					//CMN.show(new String(key_block_cache,key_start_index+_number_width,key_end_index-(key_start_index+_number_width),_charset));
					//if(EntryStartWith(key_block_cache,key_start_index+_number_width,key_end_index-(key_start_index+_number_width),matcher)) {
					if(doHarvest) {
						String kI = new String(key_block_cache, key_start_index+_number_width,key_end_index-(key_start_index+_number_width), _charset);
						if(kI.toLowerCase().replaceAll(replaceReg,emptyStr).startsWith(keyword)) {
							if(combining_search_tree!=null)
								combining_search_tree.insert(kI,SelfAtIdx,(int)(keyCounter+infoI.num_entries_accumulator));
							else
								combining_search_list.add(new myCpr(kI,(int)(keyCounter+infoI.num_entries_accumulator)));
							theta--;
						}else return;
						if(theta<=0) return;
					}else {
						scaler[keyCounter][0] = key_start_index+_number_width;
						scaler[keyCounter][1] = key_end_index-(key_start_index+_number_width);
					}
	
					key_start_index = key_end_index + delimiter_width;
					keyCounter++;
				}
				if(!doHarvest) key_block_Splitted_flag=blockId;
			}
  			} catch (Exception e2) {
  				e2.printStackTrace();
  			}
		
		
		if(!doHarvest) {
			int idx;
			if(_encoding.startsWith("GB"))
				idx = reduce2(kAB, key_block_cache, scaler, 0, (int) infoI.num_entries);
			else
				idx = reduce(keyword, key_block_cache, scaler, 0, (int) infoI.num_entries);
			//CMN.show(new String(key_block_cache, scaler[idx][0],scaler[idx][1], _charset));
			//CMN.show(new String(key_block_cache, scaler[idx+1][0],scaler[idx+1][1], _charset));
			
			String kI = new String(key_block_cache, scaler[idx][0],scaler[idx][1], _charset);
			while(true) {
				if(kI.toLowerCase().replaceAll(replaceReg,emptyStr).startsWith(keyword)) {
					if(combining_search_tree!=null)
						combining_search_tree.insert(kI,SelfAtIdx,(int)(idx+infoI.num_entries_accumulator));
					else
						combining_search_list.add(new myCpr<String,Integer>(kI,(int)(idx+infoI.num_entries_accumulator)));
					theta--;
				}else
					return;
				idx++;
				//if(idx>=infoI.num_entries) CMN.show("nono!");
				if(theta<=0)
					return;
				if(idx>=infoI.num_entries) {
					break;
				}
				kI = new String(key_block_cache, scaler[idx][0],scaler[idx][1], _charset);
			}
			doHarvest=true;
		}
		++blockId;
		if(_key_block_info_list.length<=blockId) return;
	}
}
public int reduce(String phrase,byte[] data,int[][] scaler,int start,int end) {//via mdict-js
    int len = end-start;
    if (len > 1) {
      len = len >> 1;
	  //int iI = start + len - 1;
      return phrase.compareTo(new String(data, scaler[start + len - 1][0], scaler[start + len - 1][1], _charset).toLowerCase().replaceAll(replaceReg,emptyStr))>0
                ? reduce(phrase,data,scaler,start+len,end)
                : reduce(phrase,data,scaler,start,start+len);
    } else {
      return start;
    }
}

public int reduce2(byte[] phrase,byte[] data,int[][] scaler,int start,int end) {//via mdict-js
    int len = end-start;
    if (len > 1) {
      len = len >> 1;
	  //int iI = start + len - 1;
	  byte[] sub_data = new String(data, scaler[start + len - 1][0], scaler[start + len - 1][1], _charset).toLowerCase().replaceAll(replaceReg,emptyStr).getBytes(_charset);
      return compareByteArray(phrase, sub_data)>0
                ? reduce2(phrase,data,scaler,start+len,end)
                : reduce2(phrase,data,scaler,start,start+len);
    } else {
      return start;
    }
}

	public int reduce2(byte[] phrase,int start,int end) {//via mdict-js
        int len = end-start;
        if (len > 1) {
          len = len >> 1;
          return compareByteArray(phrase, _key_block_info_list[start + len - 1].tailerKeyText)>0
                    ? reduce2(phrase,start+len,end)
                    : reduce2(phrase,start,start+len);
        } else {
          return start;
        }
    }
	public int reduce(String phrase,int start,int end) {//via mdict-js
        int len = end-start;
        if (len > 1) {
          len = len >> 1;
          return phrase.compareTo(new String(_key_block_info_list[start + len - 1].tailerKeyText,_charset))>0
                    ? reduce(phrase,start+len,end)
                    : reduce(phrase,start,start+len);
        } else {
          return start;
        }
    }
	
    public int lookUp(String keyword)
    {
    	if(_key_block_info_list==null) read_key_block_info();
    	keyword = keyword.toLowerCase().replaceAll(replaceReg,emptyStr);
    	byte[] kAB = keyword.getBytes(_charset);
    	
    	if(_encoding.startsWith("GB")) {
    		if(compareByteArray(_key_block_info_list[(int)_num_key_blocks-1].tailerKeyText,kAB)<0)
    			return -1;
    		if(compareByteArray(_key_block_info_list[0].headerKeyText,kAB)>0)
    			return -1;
    	}else {
    		if(new String(_key_block_info_list[(int)_num_key_blocks-1].tailerKeyText,_charset).compareTo(keyword)<0)
    			return -1;
    		if(new String(_key_block_info_list[0].headerKeyText,_charset).compareTo(keyword)>0)
    			return -1;
    	}
    	
        int blockId = _encoding.startsWith("GB")?reduce2(keyword.getBytes(_charset),0,_key_block_info_list.length):reduce(keyword,0,_key_block_info_list.length);
        if(blockId==-1) return blockId;

        //show("blockId:"+blockId);
        int res;
        while(blockId!=0 &&  compareByteArray(_key_block_info_list[blockId-1].tailerKeyText,kAB)>=0)
        	blockId--;
        //CMN.show("finally blockId is:"+blockId+":"+_key_block_info_list.length);

        key_info_struct infoI = _key_block_info_list[blockId];

        prepareItemByKeyInfo(infoI,blockId,null);

        if(_encoding.startsWith("GB"))
        	res = binary_find_closest2(infoI_cache_.keys,keyword);//keyword
        else
        	res = binary_find_closest(infoI_cache_.keys,keyword);//keyword
        	
        if (res==-1){
        	System.out.println("search failed!"+keyword);
        	return -1;
        }
        else{
        	//String KeyText= infoI_cache_.keys[res];
        	//for(String ki:infoI.keys) CMN.show(ki);
        	//show("match key "+KeyText+" at "+res);
        	long lvOffset = infoI.num_entries_accumulator+res;
        	//long wjOffset = infoI.key_block_compressed_size_accumulator+infoI_cache_.key_offsets[res];
        	return (int) lvOffset;
        }   
    }

    
    public static int  binary_find_closest(String[] array,String val){
    	int middle = 0;
    	int iLen = array.length;
    	int low=0,high=iLen-1;
    	if(array==null || iLen<1){
    		return -1;
    	}
    	//if(iLen==1)
    	//	return 0;
    	
    	if(val.compareTo(array[0].toLowerCase().replaceAll(replaceReg,emptyStr))<=0){
    		if(array[0].toLowerCase().replaceAll(replaceReg,emptyStr).startsWith(val))
    			return 0;
    		else
    			return -1;
    	}else if(val.compareTo(array[iLen-1].toLowerCase().replace(" ",emptyStr).replace("-",emptyStr))>=0){
    		return iLen-1;
    	}
		//System.out.println(array[0]+":"+array[array.length-1]);
		//System.out.println(array[0]+":"+val.compareTo(array[0].toLowerCase().replaceAll(replaceReg,emptyStr)));
		//System.out.println(array[0]+":"+val);
		//System.out.println(array[0]+":"+array[0].toLowerCase().replaceAll("[: . , - ]",emptyStr));


    	int counter=0;
    	int subStrLen1,subStrLen0,cprRes1,cprRes0,cprRes;String houXuan1,houXuan0;
    	while(low<high){
    		counter+=1;
    		//System.out.println("bfc_1_debug  "+low+":"+high+"   执行第"+counter+" 次");
    		//System.out.println("bfc_1_debug  "+array[low]+":"+array[high]);
    		middle = (low+high)/2;
    		houXuan1 = array[middle+1].toLowerCase().replaceAll(replaceReg,emptyStr);
    		houXuan0 = array[middle  ].toLowerCase().replaceAll(replaceReg,emptyStr);//.replace(" ",emptyStr).replace("-",emptyStr).replace("'",emptyStr);
    		cprRes1=houXuan1.compareTo(val);
        	cprRes0=houXuan0.compareTo(val);
        	/*if(cprRes0>=0){
        		System.out.println("what");
        		high=middle;
        	}else if(cprRes1<=0){// ||(cprRes0<=0 && high==middle+1)
        		System.out.println("cprRes1<=0 && cprRes0<0");
        		//System.out.println(houXuan1);
        		//System.out.println(houXuan0);
        		low=middle+1;
        	}else{
        		//System.out.println("asd");
        		high=middle;
        	}*/
        	if(cprRes0<0){
        		//System.out.println("rRes1<0&&cpr");
        		if(cprRes1>0) {
        			low=middle;
        			high=middle+1;
        			break;
        		}
        		low=middle+1;
        	}else{
        		high=middle;
        	}
    	}
    	
    	int resPreFinal;
		//System.out.println(resPreFinal);
		//System.out.println("执行了几次："+counter);

		if(array[high].toLowerCase().replaceAll(replaceReg,emptyStr).startsWith(val)) return high;
		if(array[low].toLowerCase().replaceAll(replaceReg,emptyStr).startsWith(val)) return low;
		return -1;
    	
    }
    
    
    public int  binary_find_closest(byte[][] array,String val){
    	int middle = 0;
    	int iLen = array.length;
    	int low=0,high=iLen-1;
    	if(array==null || iLen<1){
    		return -1;
    	}
    	//if(iLen==1)
    	//	return 0;
		//System.out.println(new String(array[0])+":"+new String(array[array.length-1]));
    	if(val.compareTo(new String(array[0],_charset).toLowerCase().replaceAll(replaceReg,emptyStr))<=0){
    		if(new String(array[0],_charset).toLowerCase().replaceAll(replaceReg,emptyStr).startsWith(val))
    			return 0;
    		else
    			return -1;
    	}else if(val.compareTo(new String(array[iLen-1]).toLowerCase().replace(" ",emptyStr).replace("-",emptyStr))>=0){
    		return iLen-1;
    	}
		//System.out.println(array[0]+":"+val.compareTo(array[0].toLowerCase().replaceAll(replaceReg,emptyStr)));
		//System.out.println(array[0]+":"+val);
		//System.out.println(array[0]+":"+array[0].toLowerCase().replaceAll("[: . , - ]",emptyStr));


    	int counter=0;
    	int subStrLen1,subStrLen0,cprRes1,cprRes0,cprRes;String houXuan1,houXuan0;
    	while(low<high){
    		counter+=1;
    		//System.out.println("bfc_1_debug  "+low+":"+high+"   执行第"+counter+" 次");
    		//System.out.println("bfc_1_debug  "+array[low]+":"+array[high]);
    		middle = (low+high)/2;
    		houXuan1 = processKey(array[middle+1]);
    		houXuan0 = processKey(array[middle  ]);//.replace(" ",emptyStr).replace("-",emptyStr).replace("'",emptyStr);
    		cprRes1=houXuan1.compareTo(val);
        	cprRes0=houXuan0.compareTo(val);
        	/*if(cprRes0>=0){
        		System.out.println("what");
        		high=middle;
        	}else if(cprRes1<=0){// ||(cprRes0<=0 && high==middle+1)
        		System.out.println("cprRes1<=0 && cprRes0<0");
        		//System.out.println(houXuan1);
        		//System.out.println(houXuan0);
        		low=middle+1;
        	}else{
        		//System.out.println("asd");
        		high=middle;
        	}*/
        	if(cprRes0<0){
        		//System.out.println("rRes1<0&&cpr");
        		if(cprRes1>0) {
        			low=middle;
        			high=middle+1;
        			break;
        		}
        		low=middle+1;
        	}else{
        		high=middle;
        	}
    	}
    	
    	int resPreFinal;
		//System.out.println(resPreFinal);
		//System.out.println("执行了几次："+counter);

		if(processKey(array[low]).startsWith(val)) return low;
		if(processKey(array[high]).startsWith(val)) return high;
		return -1;
    	
    }
    
    
    //binary_find_closest: with_charset! with_charset! with_charset!
    public int  binary_find_closest2(byte[][] array,String val) {
    	int middle = 0;
    	int iLen = array.length;
    	int low=0,high=iLen-1;
    	if(array==null || iLen<1){
    		return -1;
    	}
    	//if(iLen==1)
    	//	return 0;
    	
    	byte[] valBA = val.getBytes(_charset);
    	
    	int boundaryCheck = compareByteArray(valBA,processKey(array[0]).getBytes(_charset));
    	if(boundaryCheck<0){
			return -1;
    	}else  if(boundaryCheck==0) {return 0;}
    	
    	boundaryCheck = compareByteArray(valBA,processKey(array[iLen-1]).getBytes(_charset));
    	if(boundaryCheck>0){
    			return -1;
    	}else if(boundaryCheck==0) {return iLen-1;}
    	
    	//int counter=0;
    	//int subStrLen1,subStrLen0,cprRes;
    	int cprRes1,cprRes0;
    	byte[] houXuan1BA,houXuan0BA;
    	while(low<high){
    		//counter+=1;
    		System.out.println(low+":"+high);
    		middle = (low+high)/2;
    		houXuan1BA = processKey(array[middle+1]).getBytes(_charset);
    		houXuan0BA = processKey(array[middle  ]).getBytes(_charset);
    		cprRes1=compareByteArray(houXuan1BA,valBA);
        	cprRes0=compareByteArray(houXuan0BA,valBA);
        	if(cprRes0<0){
        		//System.out.println("rRes1<0&&cpr");
        		if(cprRes1>0) {
        			low=middle;
        			high=middle+1;
        			break;
        		}
        		low=middle+1;
        	}else{
        		high=middle;
        	}
    	}
    	

		if(processKey(array[low]).startsWith(val)) return low;
		if(processKey(array[high]).startsWith(val)) return high;
    	//System.out.println(array[low]+array[high]);
		//System.out.println(resPreFinal);
		//System.out.println("执行了几次："+counter);
		return -1;//判为矢匹配.
    }
    
    public String processKey(byte[] in){
    	return new String(in,_charset).toLowerCase().replaceAll(replaceReg,emptyStr);
    }
    
    
   
    public static int  binary_find_closest_loose(String[] array,String val){
    	int middle = 0;
    	int iLen = array.length;
    	int low=0,high=iLen-1;
    	if(array==null || iLen<1){
    		return -1;
    	}
    	//if(iLen==1)
    	//	return 0;
    	
    	if(val.compareTo(array[0])<=0){
    		if(array[0].startsWith(val))
    			return 0;
    		else
    			return -1;
    	}else if(val.compareTo(array[iLen-1])>=0){
    		return iLen-1;
    	}
		//System.out.println(array[0]+":"+array[array.length-1]);
		//System.out.println(array[0]+":"+val.compareTo(array[0].toLowerCase().replaceAll(replaceReg,emptyStr)));
		//System.out.println(array[0]+":"+val);
		//System.out.println(array[0]+":"+array[0].toLowerCase().replaceAll("[: . , - ]",emptyStr));


    	int counter=0;
    	int subStrLen1,subStrLen0,cprRes1,cprRes0,cprRes;String houXuan1,houXuan0;
    	while(low<high){
    		counter+=1;
    		System.out.println("bsl_debug"+low+":"+high);
    		middle = (low+high)/2;
    		houXuan1 = array[middle+1];
    		houXuan0 = array[middle  ];
    		cprRes1=houXuan1.compareTo(val);
        	cprRes0=houXuan0.compareTo(val);
        	if(cprRes0>=0){
        		high=middle;
        	}else if(cprRes1<=0){
        		//System.out.println("cprRes1<=0 && cprRes0<0");
        		//System.out.println(houXuan1);
        		//System.out.println(houXuan0);
        		low=middle+1;
        	}else{
        		//System.out.println("asd");
        		high=middle;
        	}
    	}
    	
    	int resPreFinal;
    	if(low==high) resPreFinal = high;
    	else{
    		resPreFinal = Math.abs(array[low].compareTo(val))>Math.abs(array[high].compareTo(val))?high:low;
    	}
		//System.out.println(resPreFinal);
		//System.out.println("执行了几次："+counter);
    	houXuan1 = array[resPreFinal];
    	//show("houXuan1"+houXuan1);
    	return resPreFinal;//
    }
    
    //per-byte byte array comparing
    private final static int compareByteArray(byte[] A,byte[] B){
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
    private final static boolean compareByteArrayIsPara(byte[] A,byte[] B){
    	for(int i=0;i<A.length;i++){
    		if(A[i]!=B[i])
    			return false;
    	}
    	return true;
    }
    private final static boolean compareByteArrayIsPara(byte[] A,int offA,byte[] B){
    	if(offA+B.length>A.length)
    		return false;
    	for(int i=0;i<B.length;i++){
    		if(A[offA+i]!=B[i])
    			return false;
    	}
    	return true;
    }
    
    private final static int indexOf(byte[] outerArray, byte[] smallerArray,int offset) {
        for(int i = offset; i < outerArray.length - smallerArray.length+1; i++) {
            boolean found = true;
            for(int j = 0; j < smallerArray.length; j++) {
               if (outerArray[i+j] != smallerArray[j]) {
                   found = false;
                   break;
               }
            }
            if (found) return i;
         }
       return -1;  
    }
    public static int  binary_find_closest(long[] array,long val){
    	int middle = 0;
    	int iLen = array.length;
    	int low=0,high=iLen-1;
    	if(array==null || iLen<1){
    		return -1;
    	}
    	if(iLen==1){
    		return 0;
    	}
    	if(val-array[0]<=0){
			return 0;
    	}else if(val-array[iLen-1]>=0){
    		return iLen-1;
    	}
    	int counter=0;
    	long cprRes1,cprRes0;
    	while(low<high){
    		counter+=1;
    		//System.out.println(low+":"+high);
    		middle = (low+high)/2;
    		cprRes1=array[middle+1]-val;
        	cprRes0=array[middle  ]-val;
        	if(cprRes0>=0){
        		high=middle;
        	}else if(cprRes1<=0){
        		//System.out.println("cprRes1<=0 && cprRes0<0");
        		//System.out.println(houXuan1);
        		//System.out.println(houXuan0);
        		low=middle+1;
        	}else{
        		//System.out.println("asd");
        		high=middle;
        	}
    	}
		return low;
    }
    
    

    //解压等utils
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
    
    public static byte[] zlib_decompress(byte[] encdata,int offset,int size) {
	    try {
			    ByteArrayOutputStream out = new ByteArrayOutputStream(); 
			    InflaterOutputStream inf = new InflaterOutputStream(out); 
			    inf.write(encdata,offset, size); 
			    inf.close(); 
			    return out.toByteArray(); 
		    } catch (Exception ex) {
		    	ex.printStackTrace(); 
		    	show(emptyStr);
		    	return "ERR".getBytes(); 
		    }
    }    
 
  
    static void show(String val){System.out.println(val);}

	private long _read_number(ByteBuffer sf) {
    	if(_number_width==4)
    		return sf.getInt();
    	else
    		return sf.getLong();
	}
	private long _read_number(DataInputStream  sf) {
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

    
	public static short getShort(byte buf1, byte buf2) 
    {
        short r = 0;
        r |= (buf1 & 0x00ff);
        r <<= 8;
        r |= (buf2 & 0x00ff);
        return r;
    }
    
    public static int getInt(byte buf1, byte buf2, byte buf3, byte buf4) 
    {
        int r = 0;
        r |= (buf1 & 0x000000ff);
        r <<= 8;
        r |= (buf2 & 0x000000ff);
        r <<= 8;
        r |= (buf3 & 0x000000ff);
        r <<= 8;
        r |= (buf4 & 0x000000ff);
        return r;
    }
    public static long getLong(byte[] buf) 
    {
        long r = 0;
        r |= (buf[0] & 0xff);
        r <<= 8;
        r |= (buf[1] & 0xff);
        r <<= 8;
        r |= (buf[2] & 0xff);
        r <<= 8;
        r |= (buf[3] & 0xff);
        r <<= 8;
        r |= (buf[4] & 0xff);
        r <<= 8;
        r |= (buf[5] & 0xff);
        r <<= 8;
        r |= (buf[6] & 0xff);
        r <<= 8;
        r |= (buf[7] & 0xff);
        return r;
    }
    public static long getLong(byte buf1, byte buf2, byte buf3, byte buf4,byte buf11, byte buf21, byte buf31, byte buf41) 
    {
        long r = 0;
        r |= (buf1 & 0x000000ff);
        r <<= 8;
        r |= (buf2 & 0x000000ff);
        r <<= 8;
        r |= (buf3 & 0x000000ff);
        r <<= 8;
        r |= (buf4 & 0x000000ff);
        r <<= 8;
        r |= (buf11 & 0x000000ff);
        r <<= 8;
        r |= (buf21 & 0x000000ff);
        r <<= 8;
        r |= (buf31 & 0x000000ff);
        r <<= 8;
        r |= (buf41 & 0x000000ff);
        return r;
    }
    public static String byteTo16(byte bt){
        String[] strHex={"0","1","2","3","4","5","6","7","8","9","a","b","c","d","e","f"};
        String resStr=emptyStr;
        int low =(bt & 15);
        int high = bt>>4 & 15;
        resStr = strHex[high]+strHex[low];
        return resStr;
    }
    
    
    public String getDictInfo(){
    	return new StringBuilder()
    			.append("Engine Version: ").append(_version).append("<BR>")
    			.append("CreationDate: ").append((_header_tag.containsKey("CreationDate")?_header_tag.get("CreationDate"):"UNKNOWN")).append("<BR>")
    			.append("Charset &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; : ").append(this._encoding).append("<BR>")
    			.append("_num_entries: ").append(this._num_entries).append("<BR>")
    			.append("_num_key_blocks: ").append(this._num_key_blocks).append("<BR>")
    			.append("_num_rec_blocks: ").append(this._num_record_blocks).append("<BR>")
    			.append(mdd==null?"no assiciated mdRes":(mdd._encoding+","+mdd.getNumberEntrys()+","+mdd._num_key_blocks+","+mdd._num_record_blocks)).toString();
    }
    
    public void printDictInfo(){
    	show("\r\n——————————————————————Dict Info——————————————————————");
        Iterator iter = _header_tag.entrySet().iterator();  
        while (iter.hasNext()) {  
            Map.Entry entry = (Map.Entry) iter.next();  
            Object key = entry.getKey();  
            Object value = entry.getValue();  
            System.out.println("|"+key + ":" + value);  
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

 
    public long toLong1(byte[] b,int offset)

	{
		long l = 0;
		l = b[offset+0];
		l |= ((long) b[offset+1] << 8);
		l |= ((long) b[offset+2] << 16);
		l |= ((long) b[offset+3] << 24);
		l |= ((long) b[offset+4] << 32);
		l |= ((long) b[offset+5] << 40);
		l |= ((long) b[offset+6] << 48);
		l |= ((long) b[offset+7] << 56);
		return l;
	}
    public static char toChar(byte[] buffer,int offset) {   
        char  values = 0;   
        for (int i = 0; i < 2; i++) {    
            values <<= 8; values|= (buffer[offset+i] & 0xff);   
        }   
        return values;  
     }

	public String getCodec() {
		return _encoding;
	}     
	  
    static boolean EntryStartWith(byte[] source, int sourceOffset, int sourceCount, byte[][][] matchers) {
		boolean Matched = false;
		int fromIndex=0;
    	for(int lexiPartIdx=0;lexiPartIdx<matchers[0].length;lexiPartIdx++) {
    		Matched = false;
    		for(byte[][] marchLet:matchers) {
    			if(marchLet==null) break;
    			if(bingStartWith(source,sourceOffset,marchLet[lexiPartIdx],0,-1,fromIndex)) {
    				Matched=true;
    			}
    		}
    		if(!Matched)
    			return false;
    		fromIndex+=matchers[0][lexiPartIdx].length;
    	}
    	return true;
    }

    private byte[][] SanLieZhi(String str) {
    	byte[][] res = new byte[str.length()][];
    	for(int i=0;i<str.length();i++){
    		String c = str.substring(i, i+1);
    		res[i] = c.getBytes(_charset);
		}
		return res;
	}

    static boolean bingStartWith(byte[] source, int sourceOffset,byte[] target, int targetOffset, int targetCount, int fromIndex) {
    	if (fromIndex >= source.length) {
    		return false;
        }
    	if(targetCount<=-1)
    		targetCount=target.length;
    	if(sourceOffset+targetCount>=source.length)
        	return false;
    	for (int i = sourceOffset + fromIndex; i <= sourceOffset+fromIndex+targetCount-1; i++) {
    		if (source[i] != target[targetOffset+i-sourceOffset-fromIndex]) 
    			return false;
    	}
    	return true;
    }


}


 
package com.knziha.plod.dictionary;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.Adler32;
import java.util.zip.DataFormatException;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterOutputStream;

import org.jvcompress.lzo.MiniLZO;
import org.jvcompress.util.MInt;

import com.knziha.plod.dictionary.mdict.cached_key_block;
import com.knziha.plod.dictionary.mdict.myCpr;
import com.knziha.rbtree.RBTree;



/**
 * Mdict java : resource file (.mdd) class
 * @author KnIfER
 * @date 2017/12/8
 */

public class mdictRes {
    File f;
	
	int _encrypt=0;
	String _encoding="UTF-16LE";
	String _passcode = "";
	HashMap<Integer,String[]> _stylesheet = new HashMap<Integer,String[]>();
	float _version;
	int _number_width;
	int _key_block_offset;
	long _record_block_offset;
	long _num_entries;public long getNumberEntrys(){return _num_entries;}
    long _num_key_blocks;
    long _num_record_blocks=0;
    
	key_info_struct[] _key_block_info_list;
	record_info_struct[] _record_info_struct_list;

    RBTree<myCpr<Integer, Integer>> accumulation_blockId_tree = new RBTree<myCpr<Integer, Integer>>();
    long accumulation_blockId_tree_TIME = 0;
    long block_blockId_search_tree_TIME = 0;
    
    public static class myCpr<T1 extends Comparable<T1>,T2> implements Comparable<myCpr<T1,T2>>{
    	public T1 key;
    	public T2 value;
    	public myCpr(T1 k,T2 v){
    		key=k;value=v;
    	}
    	public int compareTo(myCpr<T1,T2> other) {
    		return this.key.compareTo(other.key);
    	}
    	public String toString(){
    		return key+"_"+value;
    	}
    }
	

	long _key_block_size,_key_block_info_size;
	
    //构造
	public mdictRes(String fn) throws IOException{
        //String url = "file:///android_asset/index.html";
        //if (!TextUtils.isEmpty(url))
        //    mWebView.loadUrl(url);
        
		//![]File in
    	//byte[] asd = new byte[]{'s',2,3,4,1,2,3,4,1,2,3,4};NameOfPlants.mdx 简明英汉汉英词典.mdx
    	//File f = new File("/sdcard/BlueDict/Dicts/简明英汉汉英词典.mdx");
    	f = new File(fn);
    	//FileInputStream data_in =new FileInputStream(f);	
    	DataInputStream data_in =new DataInputStream(new FileInputStream(f));	
//![0]read_header 
    	// number of bytes of header text
    	byte[] itemBuf = new byte[4];
		data_in.read(itemBuf, 0, 4);
    	int header_bytes_size =getInt(itemBuf[0],itemBuf[1],itemBuf[2],itemBuf[3]);
    	byte[] header_bytes = new byte[header_bytes_size];
    	data_in.read(header_bytes,0, header_bytes_size); 
		// 4 bytes: adler32 checksum of header, in little endian
		itemBuf = new byte[4];
		data_in.read(itemBuf, 0, 4);
    	int alder32 = getInt(itemBuf[3],itemBuf[2],itemBuf[1],itemBuf[0]);
		//assert alder32 == (calcChecksum(header_bytes)& 0xffffffff);
		_key_block_offset = 4 + header_bytes_size + 4;
		//data_in.close();
		
		Pattern re = Pattern.compile("(\\w+)=\"(.*?)\"",Pattern.DOTALL);
		Matcher m = re.matcher(new String(header_bytes,"UTF-16LE"));
		HashMap<String,String> header_tag = new HashMap<String,String>();
		while(m.find()) {
			//for(int i=0;i<=m.groupCount();i++)
	        // System.out.println("Found value: " + m.group(i)); 
			header_tag.put(m.group(1), m.group(2));
	      }
		if (_encoding==""){
			_encoding = header_tag.get("Encoding");
            // GB18030 > GBK > GB2312
            if(_encoding =="GBK"|| _encoding =="GB2312")
            	_encoding = "GB18030";
		}
		if(!header_tag.containsKey("Encrypted") || header_tag.get("Encrypted") == "0")
            _encrypt = 0;
		else if(header_tag.get("Encrypted") == "1")
            _encrypt = 1;
        else
            _encrypt = Integer.valueOf(header_tag.get("Encrypted"));
        
        if(header_tag.containsKey("StyleSheet")){
            String[] lines = header_tag.get("StyleSheet").split("[\r\n \r \n]");
            for(int i=0;i<=lines.length-3;i+=3)
                _stylesheet.put(i,new String[]{lines[i+1],lines[i+2]});
        }
        _version = Float.valueOf(header_tag.get("GeneratedByEngineVersion"));
        if(_version < 2.0)
            _number_width = 4;
        else
            _number_width = 8;
        
        //TODO: pureSalsa20.py decryption
        if(_encrypt==1){if(_passcode=="") throw new IllegalArgumentException("_passcode未输入");}
	//![0]HEADER 分析完毕 
	//_read_keys START
        //size (in bytes) of previous 5 numbers (can be encrypted)
        int num_bytes;
        if(_version >= 2)
            num_bytes = 8 * 5+4;
        else
            num_bytes = 4 * 4;
		itemBuf = new byte[num_bytes];

		data_in.read(itemBuf, 0, num_bytes);
		data_in.close();
		
        ByteBuffer sf = ByteBuffer.wrap(itemBuf);

        _num_key_blocks = _read_number(sf);       // 1
        _num_entries = _read_number(sf);
        if(_version >= 2.0){long key_block_info_decomp_size = _read_number(sf);}
        
        _key_block_info_size = _read_number(sf);
        _key_block_size = _read_number(sf);
        
        // adler checksum of previous 5 numbers
        if(_version >= 2.0){
            int adler32 = BU.calcChecksum(itemBuf,0,num_bytes-4);
            assert adler32 == (sf.getInt()& 0xffffffff);
        }
        
        _key_block_offset+=num_bytes+_key_block_info_size;
        

        

        _record_block_offset = _key_block_offset+_key_block_size;

		//System.out.println(block_blockId_search_tree.sxing(new myCpr("rmt",1)) );
		//System.out.println(block_blockId_search_tree.sxing(new myCpr("rmt",1)).getKey().value );
		//System.out.println("accumulation_blockId_tree_TIME 建树时间="+accumulation_blockId_tree_TIME);
		//System.out.println("block_blockId_search_tree_TIME 建树时间="+block_blockId_search_tree_TIME); 
        
        read_key_block_info();
        decode_record_block_header();


	}
//构造结束
	
    void read_key_block_info() {
	    // read key block info, which comprises each key_block's:
	    //1.(starting && ending words'shrinkedText,in the form of shrinkedTextSize-shrinkedText.Name them as headerText、tailerText)、
	    //2.(compressed && decompressed size,which also have version differences, occupying either 4 or 8 bytes)
    	try {
    		DataInputStream data_in1 = new DataInputStream(new FileInputStream(f));
  			data_in1.skipBytes((int)( _key_block_offset-_key_block_info_size));
	    	byte[] itemBuf = new byte[(int) _key_block_info_size];
	    	data_in1.read(itemBuf, 0, (int) _key_block_info_size);
	    	data_in1.close();
		    _key_block_info_list = _decode_key_block_info(itemBuf);
    	} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    
    void decode_record_block_header() throws IOException{
        //![3]Decode_record_block_header
        long start = System.currentTimeMillis();
        DataInputStream data_in1 = new DataInputStream(new FileInputStream(f));
        data_in1.skipBytes((int) _record_block_offset);
        
        long num_record_blocks = _read_number(data_in1);
        long num_entries = _read_number(data_in1);
        //assert(num_entries == _num_entries);
        long record_block_info_size = _read_number(data_in1);
        long record_block_size = _read_number(data_in1);
        
        //record block info section
        _record_info_struct_list = new record_info_struct[(int) num_record_blocks];
        //int size_counter = 0;
        long compressed_size_accumulator = 0;
        long decompressed_size_accumulator = 0;
		/*may be faster:batch read-in strategy*/
		byte[] numers = new byte[(int) record_block_info_size];
		data_in1.read(numers);
		data_in1.close();
		for(int i=0;i<num_record_blocks;i++){
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
		_num_record_blocks = num_record_blocks;
    }
	
    int rec_decompressed_size;
    long maxComRecSize;
    long maxDecompressedSize;
	private byte[] record_block_;
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
	
    public byte[] getRecordAt(int position) throws IOException {//异步
    	if(position==-1) return null;
    	if(_num_record_blocks<=0) decode_record_block_header();
    	
        int blockId = accumulation_blockId_tree.xxing(new mdictRes.myCpr(position,1)).getKey().value;
        
        key_info_struct infoI = _key_block_info_list[blockId];
        //准备
        cached_key_block infoI_cache = prepareItemByKeyInfo(infoI,blockId);
        
        //String[] key_list = infoI_cache_.keys;
        
        String[] key_list = infoI_cache.keys;
//decode record block
        DataInputStream data_in = new DataInputStream(new FileInputStream(f));
        // record block info section
        data_in.skipBytes( (int) (_record_block_offset+_number_width*4+_num_record_blocks*2*_number_width));
        
        // actual record block data
        int i = (int) (position-infoI.num_entries_accumulator);
        Integer Rinfo_id = reduce(infoI_cache.key_offsets[i],0,_record_info_struct_list.length);//accumulation_RecordB_tree.xxing(new mdictRes.myCpr(,1)).getKey().value;//null 过 key前
        record_info_struct RinfoI = _record_info_struct_list[Rinfo_id];
        
        byte[] record_block = prepareRecordBlock(RinfoI,Rinfo_id);
        
      
        // split record block according to the offset info from key block
        //String key_text = key_list[i];
        long record_start = Long.valueOf(infoI_cache.key_offsets[i])-RinfoI.decompressed_size_accumulator;
        long record_end;
        if (i < key_list.length-1){
        	record_end = Long.valueOf(infoI_cache.key_offsets[i+1])-RinfoI.decompressed_size_accumulator; 	
        }
        else{
        	record_end = record_block.length;
        }
        
        byte[] record = new byte[(int) (record_end-record_start)];         
        System.arraycopy(record_block, (int) (record_start), record, 0, record.length);



        return	record;           	

      
    }
    
    //存储一组RecordBlock
    int prepared_RecordBlock_ID=-100;
    private byte[] prepareRecordBlock(record_info_struct RinfoI, int Rinfo_id) throws IOException {//异步
    	if(prepared_RecordBlock_ID==Rinfo_id)
    		return record_block_;

    	if(RinfoI==null)
    		RinfoI = _record_info_struct_list[Rinfo_id];
    	DataInputStream data_in = new DataInputStream(new FileInputStream(f));
        // record block info section
        data_in.skipBytes( (int) (_record_block_offset+_number_width*4+_num_record_blocks*2*_number_width));
        
        
        data_in.skipBytes((int) RinfoI.compressed_size_accumulator);//crash
        //whole section of record_blocks;
       // for(int i123=0; i123<record_block_info_list.size(); i123++){
        	int compressed_size = (int) RinfoI.compressed_size;
        	int decompressed_size = rec_decompressed_size = (int) RinfoI.decompressed_size;//用于验证
        	byte[] record_block = new byte[(int) decompressed_size];
        	byte[] record_block_compressed = new byte[(int) compressed_size];
        	//System.out.println(compressed_size) ;
        	//System.out.println(decompressed_size) ;
        	data_in.read(record_block_compressed);
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
            	System.arraycopy(record_block_compressed, 8, record_block, 0, decompressed_size-8);
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
                 record_block = zlib_decompress(record_block_compressed,8);
            	/*
                Inflater inf = new Inflater();
                inf.setInput(record_block_compressed,8,compressed_size-8);
                try {
					int ret = inf.inflate(record_block,0,decompressed_size);
				} catch (DataFormatException e) {
					e.printStackTrace();
				}  */
            }
            // notice not that adler32 return signed value
            
            //assert(adler32 == (BU.calcChecksum(record_block,0,decompressed_size) ));
            //assert(record_block.length == decompressed_size );
        //当前内容块解压完毕		
            record_block_=record_block;
            prepared_RecordBlock_ID=Rinfo_id;
            return record_block;
	}
    
    
    class cached_key_block{
    	String[] keys;
    	long[] key_offsets;
    	String hearderText="";
    	int blockID=-1;
    }
    cached_key_block infoI_cache_ = new cached_key_block();
    
    cached_key_block prepareItemByKeyInfo(key_info_struct infoI,int blockId){
    	if(_key_block_info_list==null) read_key_block_info();
        if(infoI_cache_.blockID==blockId) 
        	return infoI_cache_;
    	cached_key_block infoI_cache = new cached_key_block();
        if(infoI_cache.blockID!=blockId){
    	  try {
	        if(infoI==null)
	        	infoI = _key_block_info_list[blockId];
	        infoI_cache.keys = new String[(int) infoI.num_entries];
	        infoI_cache.key_offsets = new long[(int) infoI.num_entries];
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

			
        
            String key_block_compression_type = new String(new byte[]{_key_block_compressed[(int) 0],_key_block_compressed[(int) (+1)],_key_block_compressed[(int) (+2)],_key_block_compressed[(int) (3)]});
            int adler32 = getInt(_key_block_compressed[(int) (+4)],_key_block_compressed[(int) (+5)],_key_block_compressed[(int) (+6)],_key_block_compressed[(int) (+7)]);
            if(key_block_compression_type.equals(new String(new byte[]{0,0,0,0}))){
                //无需解压
                System.out.println("no compress!");
                key_block = new byte[(int) (_key_block_compressed.length-start-8)];
                System.arraycopy(_key_block_compressed, (int)(+8), key_block, 0,key_block.length);
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
                //key_block = zlib_decompress(_key_block_compressed,(int) (start+8),(int)(compressedSize-8));
                //zip
                Inflater inf = new Inflater();
                inf.setInput(_key_block_compressed,(int) (+8),(int)(compressedSize-8));
                key_block = new byte[(int) infoI.key_block_decompressed_size];
                try {
					int ret = inf.inflate(key_block,0,(int)(infoI.key_block_decompressed_size));
				} catch (DataFormatException e) {e.printStackTrace();}
            }
            //!!spliting curr Key block
            int key_start_index = 0;
            String delimiter;
            int width = 0,i1=0,key_end_index=0;
            int keyCounter = 0;
            ByteBuffer sf = ByteBuffer.wrap(key_block);
            String key_block_str = "";
			try {
				key_block_str = new String(key_block,"ASCII");
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
			while(key_start_index < key_block.length){
            	long key_id;
            	if(_version<2)
            		key_id = sf.getInt(key_start_index);//Key_ID
            	else
            		key_id = sf.getLong(key_start_index);//Key_ID
                //show("key_id"+key_id);
                if(_encoding.startsWith("UTF-16")){//TODO optimize
                    width = 2;
                    key_end_index = key_start_index + _number_width;  
                    while(i1<key_block.length){
                    	if(key_block[key_end_index]==0 && key_block[key_end_index+1]==0)
                    		break;
                    	key_end_index+=width;
                    }
                }else{
                    width = 1;
                    key_end_index = key_start_index + _number_width;  
                    while(i1<key_block.length){
                    	//CMN.show(key_block.length+"_"+key_end_index);
                    	if(key_block[key_end_index]==0)
                    		break;
                    	key_end_index+=width;
                    }
                }

                //show("key_start_index"+key_start_index);
                byte[] arraytmp = new byte[key_end_index-(key_start_index+_number_width)];
                System.arraycopy(key_block,key_start_index+_number_width, arraytmp, 0,arraytmp.length);
                
                String key_text = null;
				try {
					key_text = new String(arraytmp,_encoding);
				} catch (UnsupportedEncodingException e1) {
					e1.printStackTrace();
				}
				//CMN.show(keyCounter+":::"+key_text);
                key_start_index = key_end_index + width;
                //CMN.show(infoI_cache.keys.length+"~~~"+keyCounter+"~~~"+infoI.num_entries);
                infoI_cache.keys[keyCounter]=key_text;
                
                infoI_cache.key_offsets[keyCounter]=key_id;
                keyCounter++;
            }
            //long end2=System.currentTimeMillis(); //获取开始时间 
            //System.out.println("解压耗时："+(end2-start2));
            //assert(adler32 == (calcChecksum(key_block)));
	        infoI_cache.blockID = blockId;
	        infoI_cache_=infoI_cache;
            } catch (IOException e2) {
        		// TODO Auto-generated catch block
        		e2.printStackTrace();
        	}
        }
        return infoI_cache;
                
    }

    
    //for lv
	public String getEntryAt(int position) {
		if(_key_block_info_list==null) read_key_block_info();
		//if(_key_block_info_list==null) read_key_block_info();
        int blockId = accumulation_blockId_tree.xxing(new mdictRes.myCpr(position,1)).getKey().value;
        key_info_struct infoI = _key_block_info_list[blockId];
        prepareItemByKeyInfo(infoI,blockId);
        return infoI_cache_.keys[(int) (position-infoI.num_entries_accumulator)];
	}

   
	
	public int reduce(String phrase,int start,int end) {//via mdict-js
        int len = end-start;
        if (len > 1) {
          len = len >> 1;
          return phrase.compareTo(_key_block_info_list[start + len - 1].tailerKeyText)>0
                    ? reduce(phrase,start+len,end)
                    : reduce(phrase,start,len);
        } else {
          return start;
        }
    }
    
    public int lookUp(String keyword)
                            throws UnsupportedEncodingException
    {

		if(_key_block_info_list==null) read_key_block_info();
		
    	int blockId = reduce(keyword,0,_key_block_info_list.length);
        
        while(blockId>0 && _key_block_info_list[blockId].headerKeyText.compareTo(keyword)>0)
        	blockId--;
        key_info_struct infoI = _key_block_info_list[blockId];
        
        cached_key_block infoI_cache = prepareItemByKeyInfo(infoI,blockId);
        
        int res = binary_find_closest(infoI_cache.keys,keyword);//keyword
        if (res==-1){
        	System.out.println("search failed!");
        	return -1;
        }
        else{
        	String KeyText= infoI_cache.keys[res];
        	long lvOffset = infoI.num_entries_accumulator+res;
        	long wjOffset = infoI.key_block_compressed_size_accumulator+infoI_cache.key_offsets[res];
        	return (int) lvOffset;
        }
         
        
        
    }
    
    public static int  binary_find_closest(String[] array,String val){
    	val = val.toLowerCase();
    	//TODO 2018.5.12 从P.L.O.D中粘贴代码过来，尚未修改?
    	int middle = 0;
    	int iLen = array.length;
    	int low=0,high=iLen-1;
    	if(array==null || iLen<1){
    		return -1;
    	}
    	//if(iLen==1)
    	//	return 0;
    	
    	if(val.compareTo(array[0].toLowerCase())<=0){
    		if(array[0].toLowerCase().startsWith(val))
    			return 0;
    		else
    			return -1;
    	}else if(val.compareTo(array[iLen-1].toLowerCase())>=0){
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
    		System.out.println("bfc_1_debug  "+low+":"+high+"   执行第"+counter+" 次");
    		//System.out.println("bfc_1_debug  "+array[low]+":"+array[high]);
    		middle = (low+high)/2;
    		houXuan1 = array[middle+1].toLowerCase();
    		houXuan0 = array[middle  ].toLowerCase();//.replace(" ",emptyStr).replace("-",emptyStr).replace("'",emptyStr);
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
        			if(houXuan1.startsWith(val))
        				low=middle+1;
        			else
        				high=middle;
        			continue;
        		}
        		low=middle+1;
        	}else{
        		high=middle;
        	}
    	}
    	
    	int resPreFinal;
		//System.out.println(""+resPreFinal);
		System.out.println("执行了几次："+counter+";;"+low+";;"+high);

		if(array[high].toLowerCase().startsWith(val)) return high;
		if(array[low].toLowerCase().startsWith(val)) return low;
		return -1;
    	
    }
    
    
    
	private key_info_struct[] _decode_key_block_info(byte[] key_block_info_compressed) throws UnsupportedEncodingException {
        key_info_struct[] _key_block_info_list = new key_info_struct[(int) _num_key_blocks];
    	byte[] key_block_info;
    	if(_version >= 2)
        {   //zlib压缩
    		byte[] asd = new byte[]{key_block_info_compressed[0],key_block_info_compressed[1],key_block_info_compressed[2],key_block_info_compressed[3]};
    		//ripemd128.printBytes((new String(asd)).getBytes());
    		assert(new String(asd).equals(new String(new byte[]{2,0,0,0})));
            //处理 Ripe128md 加密的 key_block_info
    		if(_encrypt==2){try{
                key_block_info_compressed = BU._mdx_decrypt(key_block_info_compressed);
                } catch (IOException e) {e.printStackTrace();}}
			//!!!getInt CAN BE NEGTIVE ,INCONGRUENT to python CODE
    		//!!!MAY HAVE BUG
            int adler32 = getInt(key_block_info_compressed[4],key_block_info_compressed[5],key_block_info_compressed[6],key_block_info_compressed[7]);
            key_block_info = zlib_decompress(key_block_info_compressed,8);
            //assert(adler32 == (calcChecksum(key_block_info) ));
        }
        else
            key_block_info = key_block_info_compressed;
    	// decoding……
        ByteBuffer sf = ByteBuffer.wrap(key_block_info);
        byte[] textbuffer = new byte[1];
        String headerKeyText,tailerKeyText;
        long key_block_compressed_size = 0,key_block_decompressed_size = 0;
        long start1,end1,start2,end2;
        int accumulation_ = 0,num_entries=0;//how many entries before one certain block.for construction of a list.
        int byte_width = 2,text_term = 1;//DECREPTING version1...bcz I am lazy
        //遍历blocks
        for(int i=0;i<_key_block_info_list.length;i++){
            // number of entries in current key block

            start1=System.currentTimeMillis(); //获取开始时间  
        	accumulation_blockId_tree.insert(new myCpr<Integer, Integer>(accumulation_,i));
            end1=System.currentTimeMillis(); //获取结束时间
            accumulation_blockId_tree_TIME+=end1-start1;
            _key_block_info_list[i] = new key_info_struct(sf.getLong(),accumulation_);
            key_info_struct infoI = _key_block_info_list[i];
            accumulation_ += infoI.num_entries;

            //![0] head word text
            int text_head_size = sf.getChar();
            if(!_encoding.equals("UTF-16LE")){
                textbuffer = new byte[text_head_size];
                sf.get(textbuffer, 0,text_head_size);
                sf.get(); 
            }else{
                textbuffer = new byte[text_head_size*2];
                sf.get(textbuffer, 0, text_head_size*2);
                sf.get();sf.get();                
            }
            infoI.headerKeyText = new String(textbuffer,_encoding);
            //System.out.println("headerKeyText is:"+infoI.headerKeyText);
            
            //![1]  tail word text
            int text_tail_size = sf.getChar();
            if(!_encoding.equals("UTF-16LE")){
                textbuffer = new byte[text_tail_size];
                sf.get(textbuffer, 0, text_tail_size);
                sf.get();         
            }else{
                textbuffer = new byte[text_tail_size*2];
                sf.get(textbuffer, 0, text_tail_size*2);
                sf.get();sf.get();             
            }
          //TODO:兼容性
            infoI.tailerKeyText = new String(textbuffer,_encoding);
            //System.out.println("tailerKeyText is:"+infoI.tailerKeyText);
            
            //infoI = new key_info_struct(headerKeyText,
            //		tailerKeyText,
            //		key_block_compressed_size,
            //		key_block_decompressed_size);
            infoI.key_block_compressed_size_accumulator = key_block_compressed_size;
            key_block_compressed_size += sf.getLong();
            infoI.key_block_decompressed_size = sf.getLong();
            
            //block_blockId_search_tree.insert(new myCpr<String, Integer>(infoI.headerKeyText,i));
            //block_blockId_search_tree.insert(new myCpr<String, Integer>(tailerKeyText,i));

        }
        //assert(accumulation_ == self._num_entries)
        return _key_block_info_list;
	}
	
    public void printAllKeys(){
		if(_key_block_info_list==null) read_key_block_info();
    	int blockCounter = 0;
    	for(key_info_struct infoI:_key_block_info_list){
    		prepareItemByKeyInfo(infoI,blockCounter);
    		for(String entry:infoI_cache_.keys){
    			CMN.show(entry);
    		}
    		//CMN.show("block no."+(blockCounter++)+"printed");
    	}
    }
    
    public void printAllContents() throws IOException{
    	FileOutputStream fOut = new FileOutputStream(f.getAbsolutePath()+".txt");

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
                long st=System.currentTimeMillis(); //获取开始时间 
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
                 
            fOut.write(record_block);
            
            
        }
        fOut.close();
    }

    //解压
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
    public static byte[] zlib_decompress(byte[] encdata,int offset,int size) {
	    try {
			    ByteArrayOutputStream out = new ByteArrayOutputStream(); 
			    InflaterOutputStream inf = new InflaterOutputStream(out); 
			    inf.write(encdata,offset, size); 
			    inf.close(); 
			    return out.toByteArray(); 
		    } catch (Exception ex) {
		    	ex.printStackTrace(); 
		    	return "ERR".getBytes(); 
		    }
    }    
    
    public static byte[] zlib_decompress2(byte[] data,int offset) {  
        byte[] output = new byte[0];  
  
        Inflater decompresser = new Inflater();  
        decompresser.reset();  
        decompresser.setInput(data,offset,data.length-offset);  

        ByteArrayOutputStream o = new ByteArrayOutputStream(data.length);  
        try {  
            byte[] buf = new byte[1024];  
            while (!decompresser.finished()) {  
                int i = decompresser.inflate(buf);  
                o.write(buf, 0, i);  
            }  
            output = o.toByteArray();  
        } catch (Exception e) {  
            output = data;  
            e.printStackTrace();  
        } finally {  
            try {  
                o.close();  
            } catch (IOException e) {  
                e.printStackTrace();  
            }  
        }  
  
        decompresser.end();  
        return output;  
    }  
    public static byte[] gzip_decompress(byte[] bytes,int offset) {  
        if (bytes == null || bytes.length == 0) {  
            return null;  
        }  
        ByteArrayOutputStream out = new ByteArrayOutputStream();  
        ByteArrayInputStream in = new ByteArrayInputStream(bytes,offset, bytes.length-offset);  
        try {  
            GZIPInputStream ungzip = new GZIPInputStream(in);  
            byte[] buffer = new byte[256];  
            int n;  
            while ((n = ungzip.read(buffer)) >= 0) {  
                out.write(buffer, 0, n);  
            }  
        } catch (IOException e) {  
            e.printStackTrace();
        }  
  
        return out.toByteArray();  
    }  
    

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
				// TODO Auto-generated catch block
				e.printStackTrace();
				return 0;
			}
		else
			try {
				return sf.readLong();
			} catch (IOException e) {
				// TODO Auto-generated catch block
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
   
    public static String byteTo16(byte bt){
        String[] strHex={"0","1","2","3","4","5","6","7","8","9","a","b","c","d","e","f"};
        String resStr="";
        int low =(bt & 15);
        int high = bt>>4 & 15;
        resStr = strHex[high]+strHex[low];
        return resStr;
    }
}



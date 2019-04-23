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

import com.knziha.plod.dictionary.Utils.BU;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterOutputStream;

import org.anarres.lzo.LzoDecompressor1x;
import org.anarres.lzo.lzo_uintp;

//import org.jvcompress.lzo.MiniLZO;
//import org.jvcompress.util.MInt;





/**
 * Mdict java : resource file (.mdd) class
 * @author KnIfER
 * @date 2017/12/8
 */

public class mdictRes extends mdBase{
    
	HashMap<Integer,String[]> _stylesheet = new HashMap<Integer,String[]>();

	protected mdictRes() {};
    //构造
	public mdictRes(String fn) throws IOException{
		super(fn);
        decode_record_block_header();
	}
	

   
    public byte[] getRecordAt(int position) throws IOException {//异步
    	if(position<0||position>=_num_entries) return null;
    	if(record_block_==null)
    		decode_record_block_header();
    	
        int blockId = accumulation_blockId_tree.xxing(new myCpr<>(position,1)).getKey().value;
        //blockId-=1;
        key_info_struct infoI = _key_block_info_list[blockId];
        //准备
        cached_key_block infoI_cache = prepareItemByKeyInfo(infoI,blockId,null);

        int i = (int) (position-infoI.num_entries_accumulator);
        Integer Rinfo_id = reduce(infoI_cache.key_offsets[i],0,_record_info_struct_list.length);//accumulation_RecordB_tree.xxing(new myCpr(,1)).getKey().value;//null 过 key前
        record_info_struct RinfoI = _record_info_struct_list[Rinfo_id];
        
        byte[] record_block = prepareRecordBlock(RinfoI,Rinfo_id);
        
      
        // split record block according to the offset info from key block
        long record_start = infoI_cache.key_offsets[i]-RinfoI.decompressed_size_accumulator;
        long record_end;
        if (i < infoI.num_entries-1){
        	record_end = infoI_cache.key_offsets[i+1]-RinfoI.decompressed_size_accumulator; 	
        }
        else{
        	if(blockId+1<_key_block_info_list.length) {
        		//没办法只好重新准备一个咯
            	record_end = prepareItemByKeyInfo(null,blockId+1,null).key_offsets[0]-RinfoI.decompressed_size_accumulator;
            	//record_end=0;
        	}else {
        		record_end = RinfoI.decompressed_size;
        		//record_end=0;
        	}
        }
        
        byte[] record = new byte[(int) (record_end-record_start)];
        int recordLen = record.length;
        if(recordLen+record_start>record_block.length)
        	recordLen = (int) (record_block.length-record_start);
    	
        System.arraycopy(record_block, (int) (record_start), record, 0, recordLen);

        return	record;
    }
    
   
	

	public int reduce(String phrase,int start,int end) {//via mdict-js
        int len = end-start;
        if (len > 1) {
          len = len >> 1;
  		  //show(new String(_key_block_info_list[start].tailerKeyText,_charset));
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
		//keyword = mdict.processText(keyword);
		keyword = keyword.toLowerCase();
		
    	int blockId = reduce(keyword,0,_key_block_info_list.length);
    	//show("blockId:"+blockId);
        //while(blockId!=0 &&  compareByteArray(_key_block_info_list[blockId-1].tailerKeyText,keyword.getBytes(_charset))>=0)
        //	blockId--;

    	
        key_info_struct infoI = _key_block_info_list[blockId];
        
        cached_key_block infoI_cache = prepareItemByKeyInfo(infoI,blockId,null);
        
        int res = reduce_keys(infoI_cache.keys,keyword,0,infoI_cache.keys.length);//keyword
        
        if (res==-1){
        	System.out.println("search failed!");
        	return -1;
        }
        else{
    		if(!new String(infoI_cache.keys[res],_charset).toLowerCase().equals(keyword))
        		return -1;
        	//String KeyText= infoI_cache.keys[res];
        	//long wjOffset = infoI.key_block_compressed_size_accumulator+infoI_cache.key_offsets[res];
        	return (int) (infoI.num_entries_accumulator+res);
        }
    }
    
    
	public int reduce_keys(byte[][] keys,String val,int start,int end) {//via mdict-js
        int len = end-start;
		//show(new String(keys[start],_charset)+"  "+new String(keys[Math.min(end, keys.length-1)],_charset));
        if (len > 1) {
          len = len >> 1;
          return val.compareTo(new String(keys[start + len - 1],_charset).toLowerCase())>0
                    ? reduce_keys(keys,val,start+len,end)
                    : reduce_keys(keys,val,start,start+len);
        } else {
          return start;
        }
    }
	
	
    public int  binary_find_closest(byte[][] array,String val){
    	if(array==null)
    		return -1;
    	int iLen = array.length;
    	if(iLen<1)
    		return -1;
    	int boundaryCheck = val.compareTo(mdict.processText(new String(array[0],_charset)));
    	if(boundaryCheck<0){
    		return -1;
    	}else if(boundaryCheck==0)
			return 0;
    	boundaryCheck = val.compareTo(mdict.processText(new String(array[iLen-1],_charset)));
    	if(boundaryCheck>0){
    		return -1;
    	}else if(boundaryCheck==0)
			return iLen-1;
    	
    	int resPreFinal = reduce_keys(array,val,0,array.length);
    	return resPreFinal;
    }

	
    public void printAllKeys(){
		if(_key_block_info_list==null) read_key_block_info();
    	int blockCounter = 0;
    	for(key_info_struct infoI:_key_block_info_list){
    		prepareItemByKeyInfo(infoI,blockCounter,null);
    		for(byte[] entry:infoI_cache_.keys){
    			//CMN.show(entry);
    			System.out.println(new String(entry,_charset));
    		}
    		//CMN.show("block no."+(blockCounter++)+"printed");
    	}
    }
    
    public void printAllContents() throws IOException{
    	FileOutputStream fOut = new FileOutputStream(f.getAbsolutePath()+".txt");

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
            byte[] record_block = new byte[1];
            // no compression
            if(record_block_type_str.equals(new String(new byte[]{0,0,0,0}))){
            	record_block = new byte[(int) (compressed_size-8)];
            	System.arraycopy(record_block_compressed, 8, record_block, 0, record_block.length-8);
            }
            // lzo compression
            else if(record_block_type_str.equals(new String(new byte[]{1,0,0,0}))){
            	record_block = new byte[(int) (compressed_size-8)];
                //long st=System.currentTimeMillis(); //获取开始时间 
                //record_block = new byte[(int) decompressed_size];
                //MInt len = new MInt((int) decompressed_size);
                //byte[] arraytmp = new byte[(int) compressed_size];
                //System.arraycopy(record_block_compressed, 8, arraytmp, 0,(int) (compressed_size-8));
                //MiniLZO.lzo1x_decompress(arraytmp,(int) compressed_size,record_block,len);
	            new LzoDecompressor1x().decompress(record_block_compressed, +8, (int)(compressed_size-8), record_block, 0,new lzo_uintp());

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



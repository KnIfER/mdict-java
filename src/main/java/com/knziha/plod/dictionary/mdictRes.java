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

import com.knziha.plod.dictionary.Utils.BSI;
import com.knziha.plod.dictionary.Utils.BU;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.zip.InflaterOutputStream;

import com.knziha.plod.dictionary.Utils.key_info_struct;
import com.knziha.plod.dictionary.Utils.record_info_struct;
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

	HashMap<Integer,String[]> _stylesheet = new HashMap<>();


    //构造
	public mdictRes(String fn) throws IOException{
		super(fn);
        //decode_record_block_header();
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


//	public ByteArrayInputStream getResourseByKey(String key) throws IOException {
//		int idx = lookUp(key);
//		if(idx>=0){
//			RecordLogicLayer va1=new RecordLogicLayer();
//			getRecordData(idx, va1);
//			return new BSI(va1.data, va1.ral, va1.val-va1.ral);
//		}
//		return null;
//	}

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
}



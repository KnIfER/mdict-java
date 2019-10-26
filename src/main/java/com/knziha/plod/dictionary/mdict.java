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

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

import com.knziha.plod.dictionary.Utils.Flag;
import com.knziha.plod.dictionary.Utils.myCpr;
import org.anarres.lzo.LzoDecompressor1x;
import org.anarres.lzo.lzo_uintp;

import com.knziha.plod.dictionary.Utils.BU;
import com.knziha.plod.dictionary.Utils.IU;
import com.knziha.rbtree.RBTree_additive;
import org.apache.commons.text.StringEscapeUtils;


/**
 * Mdict Java Library
 * FEATURES:
 * 1. Basic parse and query functions.
 * 2. Mdicts conjunction search.
 * 3. Multi-threaded search in all contexts.
 * 4. Multi-threaded wildcard match in all key entries.
 * @author KnIfER
 */

public class mdict extends mdBase{
	@Deprecated//dummy, don't call this.
	public mdict(){};

	public final static Pattern replaceReg = Pattern.compile("[ &:$/.,\\-'()\\[\\]#<>!\\n]");
	public final static Pattern replaceReg2 = Pattern.compile("[ \\-]");
	public final static Pattern numSuffixedReg = Pattern.compile(".+?([0-9]{1,})");
	public final static Pattern markerReg = Pattern.compile("`([\\w\\W]{1,3}?)`");// for `1` `2`...
	private final static String linkRenderStr = "@@@LINK=";

	public mdictRes mdd;

	public String _Dictionary_fName;
	public String _Dictionary_Name;
	public String _Dictionary_fSuffix;

	public boolean getIsDedicatedFilter(){
		return false;
	}
	public boolean getIsDedicatedFilter(byte firstFlag){
		return false;
	}
	public void setIsDedicatedFilter(boolean val){
	}
	//public int KeycaseStrategy=0;//0:global 1:Java API 2:classical
	public int getCaseStrategy(){
		return 0;
	}
	public void setCaseStrategy(int val){
	}
	public static boolean bGlobalUseClassicalKeycase=false;

	public String currentDisplaying;

	//构造
	public mdict(String fn) throws IOException {
		super(fn);
	}

	protected void init(String fn) throws IOException{
		super.init(fn);
		_Dictionary_fName = f.getName();
		int tmpIdx = _Dictionary_fName.lastIndexOf(".");
		if(tmpIdx!=-1) {
			_Dictionary_fSuffix = _Dictionary_fName.substring(tmpIdx+1);
			_Dictionary_fName = _Dictionary_fName.substring(0, tmpIdx);
		}
		String fnTMP = f.getName();
		File p =f.getParentFile();
		if(p!=null) {
			File f2 = new File(p.getAbsolutePath()+"/"+fnTMP.substring(0,fnTMP.lastIndexOf("."))+".mdd");
			if(f2.exists()){
				mdd=new mdictRes(f2.getAbsolutePath());
			}
			if(mdd==null) {
				if(_header_tag.containsKey("SharedMdd")) {
					File SharedMddF=new File(f.getParentFile(),_header_tag.get("SharedMdd")+".mdd");
					if(SharedMddF.exists())
						mdd=new mdictRes(SharedMddF.getAbsolutePath());
				}
			}
		}
		if(_header_tag.containsKey("Title"))
			_Dictionary_Name=_header_tag.get("Title");
		calcFuzzySpace();

	}

	public String getCachedEntryAt(int pos) {
		return currentDisplaying;
	}

	//for lv
	public String getEntryAt(int position, Flag mflag) {
		if(position==-1) return "about:";
		if(_key_block_info_list==null) read_key_block_info();
		int blockId = accumulation_blockId_tree.xxing(new myCpr<>(position,1)).getKey().value;
		key_info_struct infoI = _key_block_info_list[blockId];
		if(compareByteArray(infoI.headerKeyText, infoI.tailerKeyText)==0)
			mflag.data = new String(infoI.headerKeyText,_charset);
		else
			mflag.data = null;
		prepareItemByKeyInfo(infoI,blockId,null);
		//TODO null pointer error
		return new String(infoI_cache_.keys[(int) (position-infoI.num_entries_accumulator)],_charset);
	}


	public int reduce_index2(byte[] phrase,int start,int end) {//via mdict-js
		int len = end-start;
		if (len > 1) {
			len = len >> 1;
			//show("reducearound:"+(start + len - 1)+"@"+len+": "+new String(_key_block_info_list[start + len - 1].tailerKeyText));
			//show(start+"::"+end+"   "+new String(_key_block_info_list[start].tailerKeyText,_charset)+"::"+new String(_key_block_info_list[end==_key_block_info_list.length?end-1:end].tailerKeyText,_charset));
			byte[] zhujio = _key_block_info_list[start + len - 1].tailerKeyText;
			return compareByteArray(phrase, isCompact?zhujio:processMyText(new String(zhujio,_charset)).getBytes(_charset))>0
					? reduce_index2(phrase,start+len,end)
					: reduce_index2(phrase,start,start+len);
		} else {
			return start;
		}
	}
	public int reduce_index(String phrase,int start,int end) {//via mdict-js
		int len = end-start;
		if (len > 1) {
			len = len >> 1;
			//show("reducearound:"+(start + len - 1)+"@"+len+": "+new String(_key_block_info_list[start + len - 1].tailerKeyText));
			//show(start+"::"+end+"   "+new String(_key_block_info_list[start].tailerKeyText,_charset)+"::"+new String(_key_block_info_list[end==_key_block_info_list.length?end-1:end].tailerKeyText,_charset));
			String zhujio = new String(_key_block_info_list[start + len - 1].tailerKeyText,_charset);
			return phrase.compareToIgnoreCase(isCompact?zhujio:processMyText(zhujio))>0
					? reduce_index(phrase,start+len,end)
					: reduce_index(phrase,start,start+len);
		} else {
			return start;
		}
	}
	public int lookUp(String keyword) {
		return lookUp(keyword,false);
	}
	String HeaderTextStr,TailterTextStr;

	public int lookUp(String keyword,boolean isSrict)
	{
		if(_key_block_info_list==null) read_key_block_info();
		String keyOrg=keyword;
		keyword = processMyText(keyword);
		byte[] kAB = keyword.getBytes(_charset);

		int blockId = -1;

		if(_encoding.startsWith("GB")) {
			int boudaryCheck = compareByteArray(_key_block_info_list[(int)_num_key_blocks-1].tailerKeyText,kAB);
			if(boudaryCheck<0)
				return -1;
			if(boudaryCheck==0) blockId = (int)_num_key_blocks-1;
			boudaryCheck = compareByteArray(_key_block_info_list[0].headerKeyText,kAB);
			if(boudaryCheck>0)
				return -1;
			if(boudaryCheck==0) return 0;
		}else {
			int boudaryCheck = new String(_key_block_info_list[(int)_num_key_blocks-1].tailerKeyText,_charset).compareTo(keyword);
			if(boudaryCheck<0)
				return -1;
			if(boudaryCheck==0) blockId = (int)_num_key_blocks-1;
			if(HeaderTextStr==null)
				HeaderTextStr=processMyText(new String(_key_block_info_list[0].headerKeyText,_charset));
			boudaryCheck = HeaderTextStr.compareTo(keyword);
			if(boudaryCheck>0) {
				if(HeaderTextStr.startsWith(keyword)) {
					return isSrict?-(0+2):0;
				}else
					return -1;
			}
			if(boudaryCheck==0) return 0;
		}
		if(blockId==-1)
			blockId = _encoding.startsWith("GB")?reduce_index2(keyword.getBytes(_charset),0,_key_block_info_list.length):reduce_index(keyword,0,_key_block_info_list.length);
		if(blockId==-1) return blockId;
		//show("blockId:"+blockId);
		//while(blockId!=0 &&  compareByteArray(_key_block_info_list[blockId-1].tailerKeyText,kAB)>=0) blockId--;
		//SU.Log("finally blockId is:"+blockId+":"+_key_block_info_list.length);


		key_info_struct infoI = _key_block_info_list[blockId];

		//smart shunt
		if(compareByteArray(infoI.headerKeyText, infoI.tailerKeyText)==0) {
			if(isSrict)
				return -1*(int) ((infoI.num_entries_accumulator+2));
			else
				return (int) infoI.num_entries_accumulator;
		}

		cached_key_block infoI_cache = prepareItemByKeyInfo(infoI,blockId,null);

		int res;
		if(_encoding.startsWith("GB"))
			//res = binary_find_closest2(infoI_cache.keys,keyword);//keyword
			res = reduce_keys2(infoI_cache.keys,kAB,0,infoI_cache.keys.length);
		else
			//res = binary_find_closest(infoI_cache.keys,keyword);//keyword
			res = reduce_keys(infoI_cache.keys,keyword,0,infoI_cache.keys.length);

		if (res==-1){
			System.out.println("search failed!"+keyword);
			return -1;
		}
		if(isCompact) {//compatibility fix.




		}
		String other_key = new String(infoI_cache.keys[res],_charset);
		String looseMatch = processMyText(other_key);
		boolean bIsEqual = looseMatch.equals(keyword);

		if(!bIsEqual){
			boolean b1 = keyOrg.endsWith(">"),b2=false;
			Matcher m=null;
			if(!b1) {
				m = numSuffixedReg.matcher(keyOrg);
				b2=m.find();
			}
			if((b1||b2) && other_key.endsWith(">")){
				int idx3 = other_key.lastIndexOf("<", other_key.length() - 2);
				int idx2 = b2?m.start(1):keyOrg.lastIndexOf("<",keyOrg.length()-2);
				//CMN.Log(idx2,idx3,idx2,idx3);
				if(idx2!=-1 && idx2==idx3) {
					int start = parseint(other_key.substring(idx2+1,other_key.length()-1));
					int target;
					if(b2){
						target = IU.parsint(m.group(1));
					}else{
						String itemA=keyOrg.substring(idx2+1,keyOrg.length()-1);
						target = parseint(itemA);
					}
					//CMN.Log(keyOrg,other_key,start,target);
					int PstPosition = (int) (infoI.num_entries_accumulator + res + (target-start));
					String other_other_key = getEntryAt(PstPosition);
					if(other_other_key.length()>idx2 && other_other_key.endsWith(">") && other_other_key.charAt(idx2)=='<')
						if(keyOrg.startsWith(other_other_key.substring(0, idx2))){
							String itemB=other_other_key.substring(idx2+1,other_other_key.length()-1);
							int end = parseint(itemB);
							if(target==end)
								return PstPosition;
						}
				}
			}
		}

		if(isSrict)
			if(!bIsEqual) {
				//SU.Log(res+"::"+Integer.toString(-1*(res+2)));
				return -1*(int) ((infoI.num_entries_accumulator+res+2));
			}
		//String KeyText= infoI_cache.keys[res];
		//for(String ki:infoI.keys) SU.Log(ki);
		//show("match key "+KeyText+" at "+res);
		return (int) (infoI.num_entries_accumulator+res);
	}

	private int parseint(String item) {
		if(IU.shuzi.matcher(item).find())
			return IU.parsint(item);
		else if(IU.hanshuzi.matcher(item).find())
			return IU.recurse1wCalc(item,0,item.length()-1,1);
		return -1;
	}

	private int try_get_tailing_number(String keyOrg) {
		if(keyOrg.endsWith(">")){
			int idx2 = keyOrg.lastIndexOf("<",keyOrg.length()-2);
			if(idx2!=-1){
				String item = keyOrg.substring(idx2+1,keyOrg.length()-1);
				if(IU.hanshuzi.matcher(item).find())
					return IU.recurse1wCalc(item,0,item.length()-1,1);
				else if(IU.shuzi.matcher(item).find())
					return IU.parsint(item);
			}
		}else{
			Matcher m = numSuffixedReg.matcher(keyOrg);
			if(m.find()){
				return IU.parsint(m.group());
			}
		}
		return -1;
	}

	public int reduce_keys(byte[][] keys,String val,int start,int end) {//via mdict-js
		int len = end-start;
		if (len > 1) {
			len = len >> 1;
			//String zhujue = processMyText(new String(keys[start + len - 1],_charset));
          /*if(!isCompact) {//fixing python writemdict compatibility
			  if(infoI_cache.hearderTextStr==null) {
				  infoI_cache.hearderTextStr=new String(infoI_cache.hearderText,_charset);
				  infoI_cache.tailerKeyTextStr=new String(infoI_cache.tailerKeyText,_charset);
			  }
			  if(infoI_cache.tailerKeyTextStr.compareTo(zhujue)>0 || infoI_cache.hearderTextStr.compareTo(zhujue)<0) {
				  zhujue = replaceReg2.matcher(zhujue).replaceAll(emptyStr);
			  }
		  }*/

			//show("->"+new String(keys[start + len - 1],_charset)+" ="+val.compareTo(processMyText(new String(keys[start + len - 1],_charset))));
			//show(start+"::"+end+"   "+new String(keys[start],_charset)+"::"+new String(keys[end==keys.length?end-1:end],_charset));


			return val.compareTo(processMyText(new String(keys[start + len - 1],_charset)))>0
					? reduce_keys(keys,val,start+len,end)
					: reduce_keys(keys,val,start,start+len);
		} else {
			return start;
		}
	}
	public int reduce_keys2(byte[][] keys,byte[] val,int start,int end) {//via mdict-js
		int len = end-start;
		if (len > 1) {
			len = len >> 1;
			//String zhujue = processMyText(new String(keys[start + len - 1],_charset));
          /*if(!isCompact) {//fixing python writemdict compatibility
			  if(infoI_cache.hearderTextStr==null) {
				  infoI_cache.hearderTextStr=new String(infoI_cache.hearderText,_charset);
				  infoI_cache.tailerKeyTextStr=new String(infoI_cache.tailerKeyText,_charset);
			  }
			  if(infoI_cache.tailerKeyTextStr.compareTo(zhujue)>0 || infoI_cache.hearderTextStr.compareTo(zhujue)<0) {
				  zhujue = replaceReg2.matcher(zhujue).replaceAll(emptyStr);
			  }
		  }*/
			//SU.Log(start+"::"+end+"   "+new String(keys[start],_charset)+"::"+new String(keys[end],_charset));
			return compareByteArray(val, processMyText(new String(keys[start + len - 1],_charset)).getBytes(_charset))>0
					? reduce_keys2(keys,val,start+len,end)
					: reduce_keys2(keys,val,start,start+len);
		} else {
			return start;
		}
	}

	public Object ReRoute(String keyraw) throws IOException {
		int c=0;
		int i = lookUp(keyraw, true);
		if(i<0){
			return -1;
		}
		String tmp = getRecordAt(i);
		if(getIsDedicatedFilter())
			return tmp;
		if(tmp.startsWith(linkRenderStr)) {
			//SU.Log("rerouting",tmp);
			String key = tmp.substring(linkRenderStr.length());
			return key.trim();
		}
		return null;
	}

	public String getRecordsAt(int... positions) throws IOException {
		StringBuilder sb = new StringBuilder();
		int c=0;
		for(int i:positions) {
			String tmp = getRecordAt(i);
			if(tmp.startsWith(linkRenderStr)) {
				//Log.e("rerouting",tmp);
				//SU.Log("rerouting",tmp);
				//SU.Log(tmp.replace("\n", "1"));
				String key = tmp.substring(linkRenderStr.length());
				int offset = offsetByTailing(key);
				key = key.trim();
				//Log.e("rerouting offset",""+offset);
				int idx = lookUp(key);
				if(idx!=-1) {
					String looseKey = processMyText(key);
					int tmpIdx = lookUp(key,false);
					if(tmpIdx!=-1) {
						String looseMatch = getEntryAt(tmpIdx);
						while(processMyText(looseMatch).equals(looseKey)) {
							if(looseMatch.equals(key)) {
								idx=tmpIdx;
								break;
							}
							if(tmpIdx>=getNumberEntries()-1)
								break;
							looseMatch = getEntryAt(++tmpIdx);
						}
					}


					if(offset>0) {
						if(key.equals(getEntryAt(idx+offset)))
							idx+=offset;
					}
					tmp=getRecordAt(idx);
				}
			}
			sb.append(tmp);//.trim()
			if(c!=positions.length-1)
				sb.append("<HR>");
			c++;
		}
		return processStyleSheet(sb.toString(), positions[0]);
	}

	public static int offsetByTailing(String token) {
		//calculating relative offset represented by number of tailing '\n'.
		//entrys: abc abc acc TO: abc abc\n acc
		if(token.endsWith("\n")) {
			int first=token.length()-1;
			while(first-1>0 && token.charAt(first-1)=='\n') {
				first--;
			}
			return token.length()-first;
		}
		return 0;
	}

	public String getRecordAt(int position) throws IOException {
		if(record_block_==null)
			decode_record_block_header();
		if(position<0||position>=_num_entries) return null;
		int blockId = accumulation_blockId_tree.xxing(new myCpr<>(position,1)).getKey().value;
		key_info_struct infoI = _key_block_info_list[blockId];

		//准备
		prepareItemByKeyInfo(infoI,blockId,null);
		//String[] key_list = infoI_cache_.keys;

		//decode record block
		// actual record block data
		int i = (int) (position-infoI.num_entries_accumulator);
		Integer Rinfo_id = reduce(infoI_cache_.key_offsets[i],0,_record_info_struct_list.length);//accumulation_RecordB_tree.xxing(new mdictRes.myCpr(,1)).getKey().value;//null 过 key前
		record_info_struct RinfoI = _record_info_struct_list[Rinfo_id];

		byte[] record_block = prepareRecordBlock(RinfoI,Rinfo_id);


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
			//SU.Log(record_block.length+":"+compressed_size+":"+decompressed_size);
		}
		//SU.Log(record_start+"!"+record_end);
		//byte[] record = new byte[(int) (record_end-record_start)];
		//SU.Log(record.length+":"+record_block.length+":"+(record_start));
		//System.arraycopy(record_block, (int) (record_start), record, 0, record.length);
		// convert to utf-8
		if(compareByteArrayIsPara(record_block, (int) (record_end-3), new byte[] {0x0d,0x0a,0}))
			record_end-=3;
		String record_str = new String(record_block,(int) (record_start),(int) (record_end-record_start),_charset);
		// substitute styles
		//if self._substyle and self._stylesheet:
		//    record = self._substitute_stylesheet(record);

		return	record_str;
	}





	long[] keyBlocksHeaderTextKeyID;
	public static long stst;
	//int counter=0;
	public void fetch_keyBlocksHeaderTextKeyID(){
		int blockId = 0;
		keyBlocksHeaderTextKeyID = new long[(int)_num_key_blocks];
		byte[] key_block = new byte[(int) maxDecomKeyBlockSize];
		for(key_info_struct infoI:_key_block_info_list){
			try {
				long start = infoI.key_block_compressed_size_accumulator;
				long compressedSize;
				if(blockId==_key_block_info_list.length-1)
					compressedSize = _key_block_size - _key_block_info_list[_key_block_info_list.length-1].key_block_compressed_size_accumulator;
				else
					compressedSize = _key_block_info_list[blockId+1].key_block_compressed_size_accumulator-infoI.key_block_compressed_size_accumulator;

				DataInputStream data_in = getStreamAt(_key_block_offset+start);

				byte[]  _key_block_compressed = new byte[(int) compressedSize];
				data_in.read(_key_block_compressed, 0,(int) compressedSize);
				data_in.close();

				switch (_key_block_compressed[0]|_key_block_compressed[1]<<8|_key_block_compressed[2]<<16|_key_block_compressed[2]<<32){
					case 0://no compression
						System.arraycopy(_key_block_compressed, 8, key_block, 0,(int) (compressedSize-8));
					case 1:
						new LzoDecompressor1x().decompress(_key_block_compressed, 8, (int)(compressedSize-8), key_block, 0,new lzo_uintp());
						break;
					case 2:
						//key_block = zlib_decompress(_key_block_compressed,(int) (+8),(int)(compressedSize-8));
						Inflater inf = new Inflater();
						inf.setInput(_key_block_compressed, 8 ,(int)(compressedSize-8));
						try {
							int ret = inf.inflate(key_block,0,(int)(infoI.key_block_decompressed_size));
						} catch (DataFormatException e) {e.printStackTrace();}
					break;
				}
				//!!spliting curr Key block

				if(_version<2)
					keyBlocksHeaderTextKeyID[blockId] = BU.toInt(key_block, 0);
				else
					keyBlocksHeaderTextKeyID[blockId] = BU.toLong(key_block, 0);

				blockId++;

			} catch (IOException e) {

				e.printStackTrace();
			}
		}
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

		if(record_block_==null)
			decode_record_block_header();

		fetch_keyBlocksHeaderTextKeyID();


		split_recs_thread_number = _num_record_blocks<6?1:(int) (_num_record_blocks/6);//Runtime.getRuntime().availableProcessors()/2*2+10;
		split_recs_thread_number = split_keys_thread_number>16?6:split_keys_thread_number;
		final int thread_number = Math.min(Runtime.getRuntime().availableProcessors()/2*2+2, split_keys_thread_number);


		//Log.e("fatal_","split_recs_thread_number"+split_recs_thread_number);
		//Log.e("fatal_","thread_number"+thread_number);

		final int step = (int) (_num_record_blocks/split_recs_thread_number);
		final int yuShu=(int) (_num_record_blocks%split_recs_thread_number);

		if(combining_search_tree_4==null)
			combining_search_tree_4 = new ArrayList[split_recs_thread_number];

		poolEUSize = dirtykeyCounter =0;

		ExecutorService fixedThreadPool = Executors.newFixedThreadPool(thread_number);
		for(int ti=0; ti<split_recs_thread_number; ti++){//分  thread_number 股线程运行
			if(searchCancled) break;
			final int it = ti;
			//if(false)
			if(split_recs_thread_number>thread_number) while (poolEUSize>=thread_number) {
				try {
					Thread.sleep(1);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

			if(combining_search_tree_4[it]==null)
				combining_search_tree_4[it] = new ArrayList<>();

			if(split_recs_thread_number>thread_number) countDelta(1);

			fixedThreadPool.execute(
					new Runnable(){@Override public void run()
					{
						if(searchCancled) { poolEUSize=0; return; }
						final byte[] record_block_compressed = new byte[(int) maxComRecSize];//!!!避免反复申请内存
						final byte[] record_block_ = new byte[(int) maxDecompressedSize];//!!!避免反复申请内存
						try
						{
							InputStream data_in = mOpenInputStream();
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
									System.arraycopy(record_block_compressed, 8, record_block_, 0, compressed_size-8);
								}
								else if(compareByteArrayIsPara(record_block_compressed,0,_1zero3)){
									//MInt len = new MInt((int) decompressed_size);
									//byte[] arraytmp = new byte[ compressed_size];
									//System.arraycopy(record_block_compressed, 8, arraytmp, 0, (compressed_size-8));
									//MiniLZO.lzo1x_decompress(arraytmp,(int) compressed_size,record_block_,len);
									new LzoDecompressor1x().decompress(record_block_compressed, 8, (compressed_size-8), record_block_, 0, new lzo_uintp());
								}
								else if(compareByteArrayIsPara(record_block_compressed,0,_2zero3)){
									Inflater inf = new Inflater();
									inf.setInput(record_block_compressed,8,compressed_size-8);
									int ret = inf.inflate(record_block_,0,decompressed_size);
									//SU.Log("asdasd"+ret);
								}
								//内容块解压完毕
								long off = RinfoI.decompressed_size_accumulator;
								int key_block_id = binary_find_closest(keyBlocksHeaderTextKeyID,off);
								OUT:
								while(true) {
									if(key_block_id>=_key_block_info_list.length) break;
									cached_key_block infoI_cacheI = prepareItemByKeyInfo(null,key_block_id,null);
									long[] ko = infoI_cacheI.key_offsets;
									//show("binary_find_closest "+binary_find_closest(ko,off)+"  :  "+off);
									for(int relative_pos=binary_find_closest(ko,off);relative_pos<ko.length;relative_pos++) {


										int recordodKeyLen = 0;
										if(relative_pos<ko.length-1){//不是最后一个entry
											recordodKeyLen=(int) (ko[relative_pos+1]-ko[relative_pos]);
										}//else {
										//	recordodKeyLen = (int) (prepareItemByKeyInfo(null,key_block_id+1,null).key_offsets[0]-ko[ko.length-1]);
										//}

										else if(key_block_id<keyBlocksHeaderTextKeyID.length-1){//不是最后一块key block
											recordodKeyLen=(int) (keyBlocksHeaderTextKeyID[key_block_id+1]-ko[relative_pos]);
										}else {
											recordodKeyLen = (int) (decompressed_size-(ko[ko.length-1]-RinfoI.decompressed_size_accumulator));
										}


										//show(getEntryAt((int) (relative_pos+_key_block_info_list[key_block_id].num_entries_accumulator),infoI_cacheI));
										//SU.Log(record_block_.length-1+" ko[relative_pos]: "+ko[relative_pos]+" recordodKeyLen: "+recordodKeyLen+" end: "+(ko[relative_pos]+recordodKeyLen-1));

		                    	/*
		                    	if(getEntryAt((int) (relative_pos+_key_block_info_list[key_block_id].num_entries_accumulator),infoI_cacheI).equals("鼓钟"))
		                    	{
		                    		SU.Log("decompressed_size: "+decompressed_size+" record_block_: "+(record_block_.length-1)+" ko[relative_pos]: "+ko[relative_pos]+" recordodKeyLen: "+recordodKeyLen+" end: "+(ko[relative_pos]+recordodKeyLen-1));

			                    	SU.Log(flowerIndexOf(record_block_,(int) (ko[relative_pos]-RinfoI.decompressed_size_accumulator),recordodKeyLen,matcher,0,0)+"");


			                    	SU.Log(new String(record_block_,(int) (ko[relative_pos]-RinfoI.decompressed_size_accumulator)+248,10,_charset));
			                    	SU.Log(recordodKeyLen+" =recordodKeyLen");
			                    	SU.Log((ko[relative_pos]-RinfoI.decompressed_size_accumulator+recordodKeyLen)+" sdf "+RinfoI.decompressed_size+" sdf "+RinfoI.compressed_size);

		                    		SU.Log("\r\n"+new String(record_block_,(int) (ko[relative_pos]-RinfoI.decompressed_size_accumulator),recordodKeyLen,_charset));

		                    	}*/

										if(ko[relative_pos]-RinfoI.decompressed_size_accumulator+recordodKeyLen>RinfoI.decompressed_size) {
											//show("break OUT");
											break OUT;
										}


										//if(indexOf(record_block_,(int) (ko[relative_pos]-RinfoI.decompressed_size_accumulator),recordodKeyLen,keys[0],0,keys[0].length,0)!=-1) {
										if(flowerIndexOf(record_block_,(int) (ko[relative_pos]-RinfoI.decompressed_size_accumulator),recordodKeyLen,matcher,0,0)!=-1) {
											int pos = (int) (relative_pos+_key_block_info_list[key_block_id].num_entries_accumulator);
											fuzzyKeyCounter++;

											//String LexicalEntry = getEntryAt(pos,infoI_cacheI);
											//show(getEntryAt(pos,infoI_cacheI));
											//ripemd128.printBytes(record_block_,offIdx,recordodKeyLen);

											combining_search_tree_4[it].add(pos);
										}
										dirtyfzPrgCounter++;
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
			fixedThreadPool.awaitTermination(5, TimeUnit.MINUTES);
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

	public int split_keys_thread_number;
	//public ArrayList<myCpr<String,Integer>>[] combining_search_tree;
	public ArrayList<Integer>[] combining_search_tree2;
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
	public volatile static int fullTextKeyCounter;

	public volatile boolean fuzzyCancled=false;
	public volatile int dirtyfzPrgCounter;
	public volatile static int fuzzyKeyCounter;

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
								  final int SelfAtIdx,int theta)
	{
		if(_key_block_info_list==null) read_key_block_info();


		String keyword = key.toLowerCase();
		Pattern keyPattern=null;//用于 复核 ，并不直接参与搜索
		try {
			Pattern.compile(keyword.replace("*", ".+?"),Pattern.CASE_INSENSITIVE);
		}catch(Exception e) {}

		String upperKey = keyword.toUpperCase();
		final byte[][][] matcher = new byte[upperKey.equals(keyword)?1:2][][];
		matcher[0] = flowerSanLieZhi(keyword);
		if(matcher.length==2)
			matcher[1] = flowerSanLieZhi(upperKey);

		//final String fkeyword = keyword.toLowerCase().replaceAll(replaceReg,emptyStr);
		//int entryIdx = 0;
		//show("availableProcessors: "+Runtime.getRuntime().availableProcessors());
		//show("keyBLockN: "+_key_block_info_list.length);
		split_keys_thread_number = _num_key_blocks<6?1:(int) (_num_key_blocks/6);//Runtime.getRuntime().availableProcessors()/2*2+10;
		final int thread_number = Math.min(Runtime.getRuntime().availableProcessors()/2*2+5, split_keys_thread_number);

		poolEUSize = dirtyfzPrgCounter =0;

		thread_number_count = split_keys_thread_number;
		final int step = (int) (_num_key_blocks/split_keys_thread_number);
		final int yuShu=(int) (_num_key_blocks%split_keys_thread_number);

		//ExecutorService fixedThreadPoolmy = Executors.newFixedThreadPool(thread_number);
		ExecutorService fixedThreadPoolmy = Executors.newWorkStealingPool();
		//Executors.defaultThreadFactory()
		//show("~"+step+"~"+split_keys_thread_number+"~"+_num_key_blocks);
		if(combining_search_tree2==null)
			combining_search_tree2 = new ArrayList[split_keys_thread_number];

		for(int ti=0; ti<split_keys_thread_number; ti++){//分  thread_number 股线程运行
			if(fuzzyCancled) break;
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
						if(fuzzyCancled) {poolEUSize=0; return; }
						int jiaX=0;
						if(it==split_keys_thread_number-1) jiaX=yuShu;
						if(combining_search_tree2[it]==null)
							combining_search_tree2[it] = new ArrayList<>();


						int compressedSize_many = 0, _maxDecomKeyBlockSize = 0;
						//小循环
						for(int blockId=it*step; blockId<it*step+step+jiaX; blockId++){
							//prepareItemByKeyInfo(_key_block_info_list[blockCounter],blockCounter);
							key_info_struct infoI = _key_block_info_list[blockId];
							_maxDecomKeyBlockSize = Math.max(_maxDecomKeyBlockSize, (int) infoI.key_block_decompressed_size);
							if(blockId==_key_block_info_list.length-1)
								compressedSize_many += _key_block_size - _key_block_info_list[_key_block_info_list.length-1].key_block_compressed_size_accumulator;
							else
								compressedSize_many += _key_block_info_list[blockId+1].key_block_compressed_size_accumulator-infoI.key_block_compressed_size_accumulator;
						}
						//TODO optimise compressedSize_many
						//SU.Log("compressedSize_many;;"+compressedSize_many);
						byte[] key_block = new byte[_maxDecomKeyBlockSize];/*分配资源 maxDecomKeyBlockSize 32770   65536 (common cache for index blocks)*/
						long start = _key_block_info_list[it*step].key_block_compressed_size_accumulator;

						try {
							DataInputStream data_in = getStreamAt(_key_block_offset+start);

							byte[]  _key_block_compressed_many = new byte[ compressedSize_many];
							data_in.read(_key_block_compressed_many, 0, _key_block_compressed_many.length);
							data_in.close();
							data_in=null;
							//大循环
							for(int blockId=it*step; blockId<it*step+step+jiaX; blockId++){
								if(fuzzyCancled) { poolEUSize=0; return; }

								int compressedSize;
								key_info_struct infoI = _key_block_info_list[blockId];

								//redundant check growing cache size
								if(infoI.key_block_decompressed_size>key_block.length) {
									key_block=null;
									key_block = new byte[(int) maxDecomKeyBlockSize];
								}

								if(blockId==_key_block_info_list.length-1)
									compressedSize = (int) (_key_block_size - _key_block_info_list[_key_block_info_list.length-1].key_block_compressed_size_accumulator);
								else
									compressedSize = (int) (_key_block_info_list[blockId+1].key_block_compressed_size_accumulator-infoI.key_block_compressed_size_accumulator);

								int startI = (int) (infoI.key_block_compressed_size_accumulator-start);


								//byte[] record_block_type = new byte[]{_key_block_compressed_many[(int) startI],_key_block_compressed_many[(int) (startI+1)],_key_block_compressed_many[(int) (startI+2)],_key_block_compressed_many[(int) (startI+3)]};
								//int adler32 = getInt(_key_block_compressed_many[(int) (startI+4)],_key_block_compressed_many[(int) (startI+5)],_key_block_compressed_many[(int)(startI+6)],_key_block_compressed_many[(int) (startI+7)]);

								//SU.Log(key_block.length+";;"+infoI.key_block_decompressed_size+";;"+maxDecomKeyBlockSize);
								if(compareByteArrayIsPara(_key_block_compressed_many,startI,_zero4)){
									System.arraycopy(_key_block_compressed_many, (startI+8), key_block, 0, (int)(_key_block_size-8));
								}else if(compareByteArrayIsPara(_key_block_compressed_many,startI,_1zero3))
								{
									new LzoDecompressor1x().decompress(_key_block_compressed_many, startI+8, compressedSize-8, key_block, 0,new lzo_uintp());
								}
								else if(compareByteArrayIsPara(_key_block_compressed_many,startI,_2zero3))
								{
									Inflater inf = new Inflater();
									inf.setInput(_key_block_compressed_many,(startI+8),(compressedSize-8));
									try {
										int ret = inf.inflate(key_block,0,(int)(infoI.key_block_decompressed_size));
									} catch (DataFormatException e) {e.printStackTrace();}
								}
								find_in_keyBlock(keyPattern, key_block,infoI,matcher,SelfAtIdx,it);
							}
							_key_block_compressed_many=null;
						}
						catch (Exception e1) {e1.printStackTrace();}
						thread_number_count--;
						if(split_keys_thread_number>thread_number) countDelta(-1);
					}});
		}//任务全部分发完毕
		fixedThreadPoolmy.shutdown();
		try {
			fixedThreadPoolmy.awaitTermination(1, TimeUnit.MINUTES);
			fixedThreadPoolmy.shutdownNow();
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		fixedThreadPoolmy=null;
		//System.gc();
	}


	HashSet<Integer> miansi = new HashSet<>();//. is 免死金牌  that exempt you from death for just one time
	HashSet<Integer> yueji = new HashSet<>();//* is 越级天才, i.e., super super genius leap

	int flowerIndexOf(byte[] source, int sourceOffset, int sourceCount, byte[][][] matchers,int marcherOffest, int fromIndex)
	{
		int lastSeekLetSize=0;
		while(fromIndex<sourceCount) {
			//SU.Log("==");
			//int idx = -1;
			int fromIndex_=fromIndex;
			boolean isSeeking=true;
			boolean Matched = false;
			for(int lexiPartIdx=marcherOffest;lexiPartIdx<matchers[0].length;lexiPartIdx++) {
				//if(fromIndex_>sourceCount-1) return -1;
				//SU.Log("stst: "+sourceCount+"::"+(fromIndex_+seekPos)+" fromIndex_: "+fromIndex_+" seekPos: "+seekPos+" lexiPartIdx: "+lexiPartIdx);

				//SU.Log("seekPos: "+seekPos+" lexiPartIdx: "+lexiPartIdx+" fromIndex_: "+fromIndex_);
				if(miansi.contains(lexiPartIdx)) {
					if(lexiPartIdx==matchers[0].length-1) {
						if(fromIndex_>=sourceCount)
							return -1;
						return fromIndex-lastSeekLetSize;//HERE
					}//Matched=true
					//SU.Log("miansi: "+lexiPartIdx);
					//SU.Log("miansi: "+sourceCount+"::"+(fromIndex_+seekPos)+"sourceL: "+source.length);
					//SU.Log("jumpped c is: "+new String(source,fromIndex_+seekPos,Math.min(4, sourceCount-(fromIndex_+seekPos-sourceOffset)),_encoding).substring(0, 1));
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
					//SU.Log("seekPos:"+seekPos+" fromIndex_: "+fromIndex_);
					if(!Matched)
						return -1;
					seekPos+=lastSeekLetSize;
					fromIndex=fromIndex_=seekPos;
					isSeeking=false;
					continue;
				}
				else {
					//SU.Log("deadline"+fromIndex_+" "+sourceCount);
					if(fromIndex_>sourceCount-1) {
						//SU.Log("deadline reached"+fromIndex_+" "+sourceCount);
						return -1;
					}
					for(byte[][] marchLet:matchers) {
						if(marchLet==null) break;
						if(bingStartWith(source,sourceOffset,marchLet[lexiPartIdx],0,-1,fromIndex_)) {
							Matched=true;
							//SU.Log("matchedHonestily: "+sourceCount+"::"+(fromIndex_+seekPos)+" fromIndex_: "+fromIndex_+" seekPos: "+seekPos);
							//SU.Log("matchedHonestily: "+lexiPartIdx);
						}
					}
				}
				if(!Matched) {
					//SU.Log("Matched failed this round: "+lexiPartIdx);
					break;
				}
				fromIndex_+=matchers[0][lexiPartIdx].length;
			}
			if(Matched)
				return fromIndex-lastSeekLetSize;
		}
		return -1;
	}


	private byte[][] flowerSanLieZhi(String str) {
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


	protected void find_in_keyBlock(Pattern keyPattern,byte[] key_block,key_info_struct infoI,byte[][][] matcher,int SelfAtIdx,int it) {
		//!!spliting curr Key block
		int key_start_index = 0;
		//String delimiter;
		int key_end_index=0;
		//int keyCounter = 0;

		//ByteBuffer sf = ByteBuffer.wrap(key_block);//must outside of while...
		int keyCounter = 0;
		while(key_start_index < infoI.key_block_decompressed_size){
			//long key_id;
			//if(_version<2)
			// sf.position(4);
			//key_id = sf.getInt(key_start_index);//Key_ID
			//else
			// sf.position(8);
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
						//int LexicalEntryIdx = LexicalEntry.toLowerCase().indexOf(keyword);
						if(keyPattern!=null)
							if(!keyPattern.matcher(LexicalEntry).find()) {key_start_index = key_end_index + delimiter_width;dirtyfzPrgCounter++;keyCounter++;continue;}
						//StringBuilder sb = new StringBuilder(LexicalEntry);
						//byte[] arraytmp = new byte[key_end_index-(key_start_index+_number_width)];
						//System.arraycopy(key_block,key_start_index+_number_width, arraytmp, 0,arraytmp.length);
						//additiveMyCpr1 tmpnode = new additiveMyCpr1(LexicalEntry,""+SelfAtIdx+""+((int) (infoI.num_entries_accumulator+keyCounter)));//new ArrayList<Integer>() new int[] {SelfAtIdx,(int) (infoI.num_entries_accumulator+keyCounter)}
						//tmpnode.value.add(SelfAtIdx);
						//tmpnode.value.add((int) (infoI.num_entries_accumulator+keyCounter));
						combining_search_tree2[it].add((int) (infoI.num_entries_accumulator+keyCounter));//new additiveMyCpr1(LexicalEntry,infoI.num_entries_accumulator+keyCounter));
						//SU.Log("fuzzyKeyCounter"+fuzzyKeyCounter);
						fuzzyKeyCounter++;
					}
				} catch (Exception e) {
					e.printStackTrace();
				}


			key_start_index = key_end_index + delimiter_width;
			keyCounter++;dirtyfzPrgCounter++;
		}
		//assert(adler32 == (calcChecksum(key_block)));
	}

	protected void find_in_keyBlock(byte[] key_block,key_info_struct infoI,String keyword,int SelfAtIdx,int it) {
		//!!spliting curr Key block
		int key_start_index = 0;
		int key_end_index=0;
		//int keyCounter = 0;

		//ByteBuffer sf = ByteBuffer.wrap(key_block);//must outside of while...
		int keyCounter = 0;
		while(key_start_index < infoI.key_block_decompressed_size){
			//long key_id;
			//if(_version<2)
			//sf.position(4);
			//key_id = sf.getInt(key_start_index);//Key_ID
			// else
			//sf.position(8);
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
							dirtyfzPrgCounter++;continue;
						}

						//StringBuilder sb = new StringBuilder(LexicalEntry);
						//byte[] arraytmp = new byte[key_end_index-(key_start_index+_number_width)];
						//System.arraycopy(key_block,key_start_index+_number_width, arraytmp, 0,arraytmp.length);
						//additiveMyCpr1 tmpnode = new additiveMyCpr1(LexicalEntry,""+SelfAtIdx+""+((int) (infoI.num_entries_accumulator+keyCounter)));//new ArrayList<Integer>() new int[] {SelfAtIdx,(int) (infoI.num_entries_accumulator+keyCounter)}
						//tmpnode.value.add(SelfAtIdx);
						//tmpnode.value.add((int) (infoI.num_entries_accumulator+keyCounter));
						combining_search_tree2[it].add((int) (infoI.num_entries_accumulator+keyCounter));//new additiveMyCpr1(LexicalEntry,));

						fuzzyKeyCounter++;
					}
				} catch (Exception e) {
					e.printStackTrace();
				}


			key_start_index = key_end_index + delimiter_width;
			keyCounter++;dirtyfzPrgCounter++;
		}
		//assert(adler32 == (calcChecksum(key_block)));
	}



	byte[] key_block_cache = null;
	int key_block_cacheId=-1;
	int key_block_Splitted_flag=-1;
	int[][] scaler = null;

	public int reduce(String phrase,byte[] data,int[][] scaler,int start,int end) {//via mdict-js
		int len = end-start;
		if (len > 1) {
			len = len >> 1;
			//int iI = start + len - 1;
			String zhujio= new String(data, scaler[start + len - 1][0], scaler[start + len - 1][1], _charset);
			/*if(!isCompact) {//fixing python writemdict compatibility
				  if(wml.hearderTextStr==null) {
					  wml.hearderTextStr=new String(wml.infoI.headerKeyText,_charset);
					  wml.tailerKeyTextStr=new String(wml.infoI.tailerKeyText,_charset);
				  }
				  if(wml.tailerKeyTextStr.compareTo(zhujio)>0 || wml.hearderTextStr.compareTo(zhujio)<0) {
					  zhujio = replaceReg2.matcher(zhujio).replaceAll(emptyStr);
				  }
			  }*/
			return phrase.compareTo(processMyText(zhujio))>0
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
			String zhujio= new String(data, scaler[start + len - 1][0], scaler[start + len - 1][1], _charset);
			/*if(!isCompact) {//fixing python writemdict compatibility
				  if(wml.hearderTextStr==null) {
					  wml.hearderTextStr=new String(wml.infoI.headerKeyText,_charset);
					  wml.tailerKeyTextStr=new String(wml.infoI.tailerKeyText,_charset);
				  }
				  if(wml.tailerKeyTextStr.compareTo(zhujio)>0 || wml.hearderTextStr.compareTo(zhujio)<0) {
					  zhujio = replaceReg2.matcher(zhujio).replaceAll(emptyStr);
				  }
			  }*/
			byte[] sub_data = processMyText(zhujio).getBytes(_charset);

			return compareByteArray(phrase, sub_data)>0
					? reduce2(phrase,data,scaler,start+len,end)
					: reduce2(phrase,data,scaler,start,start+len);
		} else {
			return start;
		}
	}

	class WMCompatLet{
		key_info_struct infoI;
		String hearderTextStr;
		String tailerKeyTextStr;
		WMCompatLet(key_info_struct _infoI){
			infoI=_infoI;
		}
	}

	public ArrayList<myCpr<String, Integer>> combining_search_list;
	//联合搜索  555
	public void size_confined_lookUp5(String keyword,
									  RBTree_additive combining_search_tree, int SelfAtIdx, int theta) //多线程
	{
		int[][] scaler_ = null;
		byte[] key_block_cache_ = null;
		ArrayList<myCpr<String, Integer>> _combining_search_list = combining_search_list;
		keyword = processMyText(keyword);
		byte[] kAB = keyword.getBytes(_charset);
		if(_encoding.startsWith("GB")) {
			if(compareByteArray(_key_block_info_list[(int)_num_key_blocks-1].tailerKeyText,kAB)<0)
				return;
			if(compareByteArray(_key_block_info_list[0].headerKeyText,kAB)>0)
				return;
		}else {
			if(new String(_key_block_info_list[(int)_num_key_blocks-1].tailerKeyText,_charset).compareTo(keyword)<0)
				return;
			if(HeaderTextStr==null)
				HeaderTextStr=processMyText(new String(_key_block_info_list[0].headerKeyText,_charset));
			if(HeaderTextStr.compareTo(keyword)>0) {
				if(!HeaderTextStr.startsWith(keyword))
					return;
				else {

				}
			}
		}
		if(_key_block_info_list==null) read_key_block_info();
		int blockId = _encoding.startsWith("GB")?reduce_index2(kAB,0,_key_block_info_list.length):reduce_index(keyword,0,_key_block_info_list.length);
		if(blockId==-1) return;
		//show(_Dictionary_fName+_key_block_info_list[blockId].tailerKeyText+"1~"+_key_block_info_list[blockId].headerKeyText);
		//while(blockId!=0 &&  compareByteArray(_key_block_info_list[blockId-1].tailerKeyText,kAB)>=0)
		//	blockId--;

		boolean doHarvest=false;

		//OUT:
		while(theta>0) {//complexity explanation: the aim is to harvest at most number theta matching results. but they might be crossing-blocks.
			key_info_struct infoI = _key_block_info_list[blockId];
			try {
				long start = infoI.key_block_compressed_size_accumulator;
				long compressedSize;
				if(key_block_cacheId!=blockId || key_block_cache==null) {
					if(blockId==_key_block_info_list.length-1)
						compressedSize = _key_block_size - _key_block_info_list[_key_block_info_list.length-1].key_block_compressed_size_accumulator;
					else
						compressedSize = _key_block_info_list[blockId+1].key_block_compressed_size_accumulator-infoI.key_block_compressed_size_accumulator;

					DataInputStream data_in = getStreamAt(_key_block_offset+start);

					byte[]  _key_block_compressed = new byte[(int) compressedSize];
					data_in.read(_key_block_compressed, 0, _key_block_compressed.length);
					data_in.close();

					//int adler32 = getInt(_key_block_compressed[(int) (+4)],_key_block_compressed[(int) (+5)],_key_block_compressed[(int) (+6)],_key_block_compressed[(int) (+7)]);
					if(compareByteArrayIsPara(_zero4, _key_block_compressed)){
						//System.out.println("no compress!");
						key_block_cache_ = new byte[(int) (_key_block_compressed.length-start-8)];
						System.arraycopy(_key_block_compressed, (int)(start+8), key_block_cache_, 0,key_block_cache_.length);
					}else if(compareByteArrayIsPara(_1zero3, _key_block_compressed))
					{
						//MInt len = new MInt((int) infoI.key_block_decompressed_size);
						//key_block_cache_ = new byte[len.v];
						//byte[] arraytmp = new byte[(int) compressedSize];
						//System.arraycopy(_key_block_compressed, (int)(+8), arraytmp, 0,(int) (compressedSize-8));
						//MiniLZO.lzo1x_decompress(arraytmp,arraytmp.length,key_block_cache_,len);
						key_block_cache_ =  new byte[(int) infoI.key_block_decompressed_size];
						new LzoDecompressor1x().decompress(_key_block_compressed, 8, (int)(compressedSize-8), key_block_cache_, 0, new lzo_uintp());
					}
					else if(compareByteArrayIsPara(_2zero3, _key_block_compressed)){
						//key_block_cache_ = zlib_decompress(_key_block_compressed,(int) (+8),(int)(compressedSize-8));
						key_block_cache_ =  new byte[(int) infoI.key_block_decompressed_size];
						Inflater inf = new Inflater();
						inf.setInput(_key_block_compressed,8,(int)compressedSize-8);
						int ret = inf.inflate(key_block_cache_,0,key_block_cache_.length);
					}
					key_block_cache=key_block_cache_;
					key_block_cacheId = blockId;
				}else {
					key_block_cache_=key_block_cache;
				}
				/*!!spliting curr Key block*/
				if(key_block_Splitted_flag!=blockId || scaler==null) {
					if(!doHarvest)
						scaler_ = new int[(int) infoI.num_entries][2];
					int key_start_index = 0;
					int key_end_index=0;
					int keyCounter = 0;

					while(key_start_index < key_block_cache_.length){
						key_end_index = key_start_index + _number_width;
						SK_DELI:
						while(true){
							for(int sker=0;sker<delimiter_width;sker++) {
								if(key_block_cache_[key_end_index+sker]!=0) {
									key_end_index+=delimiter_width;
									continue SK_DELI;
								}
							}
							break;
						}
						//SU.Log(new String(key_block_cache_,key_start_index+_number_width,key_end_index-(key_start_index+_number_width),_charset));
						//if(EntryStartWith(key_block_cache_,key_start_index+_number_width,key_end_index-(key_start_index+_number_width),matcher)) {
						if(doHarvest) {
							String kI = new String(key_block_cache_, key_start_index+_number_width,key_end_index-(key_start_index+_number_width), _charset);
							if(processMyText(kI).startsWith(keyword)) {
								if(combining_search_tree!=null)
									combining_search_tree.insert(kI,SelfAtIdx,(int)(keyCounter+infoI.num_entries_accumulator));
								else
									_combining_search_list.add(new myCpr(kI,(int)(keyCounter+infoI.num_entries_accumulator)));
								theta--;
							}else return;
							if(theta<=0) return;
						}else {
							scaler_[keyCounter][0] = key_start_index+_number_width;
							scaler_[keyCounter][1] = key_end_index-(key_start_index+_number_width);
						}

						key_start_index = key_end_index + delimiter_width;
						keyCounter++;
					}
					if(!doHarvest) {
						scaler=scaler_;
						key_block_Splitted_flag=blockId;
					}
				}else {
					scaler_=scaler;
				}
			} catch (Exception e2) {
				e2.printStackTrace();
			}


			if(!doHarvest) {
				int idx;
				if(_encoding.startsWith("GB"))
					idx = reduce2(kAB, key_block_cache_, scaler_, 0, (int) infoI.num_entries);
				else
					idx = reduce(keyword, key_block_cache_, scaler_, 0, (int) infoI.num_entries);
				//SU.Log(new String(key_block_cache_, scaler_[idx][0],scaler_[idx][1], _charset));
				//SU.Log(new String(key_block_cache_, scaler_[idx+1][0],scaler_[idx+1][1], _charset));

				String kI = new String(key_block_cache_, scaler_[idx][0],scaler_[idx][1], _charset);
				while(true) {
					if(processMyText(kI).startsWith(keyword)) {
						if(combining_search_tree!=null)
							combining_search_tree.insert(kI,SelfAtIdx,(int)(idx+infoI.num_entries_accumulator));
						else
							_combining_search_list.add(new myCpr<>(kI,(int)(idx+infoI.num_entries_accumulator)));
						theta--;
					}else
						return;
					idx++;
					//if(idx>=infoI.num_entries) SU.Log("nono!");
					if(theta<=0)//no need to proceed. Max results fetched.
						return;
					if(idx>=infoI.num_entries) {
						break;
					}
					kI = new String(key_block_cache_, scaler_[idx][0],scaler_[idx][1], _charset);
				}
				doHarvest=true;
			}
			++blockId;
			if(_key_block_info_list.length<=blockId) return;
		}
	}







	public String processKey(byte[] in){ return processMyText(new String(in,_charset));  }




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









	public static short getShort(byte buf1, byte buf2)
	{
		short r = 0;
		r |= (buf1 & 0x00ff);
		r <<= 8;
		r |= (buf2 & 0x00ff);
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

	public static String byteTo16(byte bt){
		String[] strHex={"0","1","2","3","4","5","6","7","8","9","a","b","c","d","e","f"};
		String resStr;
		int low =(bt & 15);
		int high = bt>>4 & 15;
		resStr = strHex[high]+strHex[low];
		return resStr;
	}


	public String getDictInfo(){
		DecimalFormat numbermachine = new DecimalFormat("#.00");
		return new StringBuilder()
				.append("Engine Version: ").append(_version).append("<BR>")
				.append("CreationDate: ").append((_header_tag.containsKey("CreationDate")?_header_tag.get("CreationDate"):"UNKNOWN")).append("<BR>")
				.append("Charset &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; : ").append(this._encoding).append("<BR>")
				.append("Num Entries: ").append(_num_entries).append("<BR>")
				.append("Num Key Blocks: ").append(_num_key_blocks).append("<BR>")
				.append("Num Rec Blocks: ").append(decode_record_block_size()).append("<BR>")
				.append("Avg. Key Block Size: ").append(numbermachine.format(1.0*_key_block_size/_num_key_blocks/1024)).append(" kb, ").append(numbermachine.format(1.0*_num_entries/_num_key_blocks)).append(" items / block").append("<BR>")
				.append("Avg. Rec Block Size: ").append(numbermachine.format(1.0*_record_block_size/_num_record_blocks/1024)).append(" kb, ").append(numbermachine.format(1.0*_num_entries/_num_record_blocks)).append(" items / block").append("<BR>")
				.append("Compact  排序: ").append(isCompact).append("<BR>")
				.append("StripKey 排序: ").append(isStripKey).append("<BR>")
				.append("Case Sensitive: ").append(isKeyCaseSensitive).append("<BR>")
				.append(mdd==null?"&lt;no assiciated mdRes&gt;":("MdResource count "+mdd.getNumberEntries()+","+mdd._encoding+","+mdd._num_key_blocks+","+mdd._num_record_blocks)).append("<BR>")
				.append("Internal Name: ").append(_Dictionary_Name).append("<BR>")
				.append("Path: ").append(getPath()).toString();
	}

	static boolean EntryStartWith(byte[] source, int sourceOffset, int sourceCount, byte[][][] matchers) {
		boolean Matched;
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


	public static String processText(CharSequence input) {
		return replaceReg.matcher(input).replaceAll(emptyStr).toLowerCase();
	}

	public String processMyText(String input) {
		String ret = isStripKey?replaceReg.matcher(input).replaceAll(emptyStr):input;
		int KeycaseStrategy=getCaseStrategy();
		return isKeyCaseSensitive?ret:(((KeycaseStrategy>0)?(KeycaseStrategy==2):bGlobalUseClassicalKeycase)?mOldSchoolToLowerCase(ret):ret.toLowerCase());
	}

	public String mOldSchoolToLowerCase(String input) {
		StringBuilder sb = new StringBuilder(input);
		for(int i=0;i<sb.length();i++) {
			if(sb.charAt(i)>='A' && sb.charAt(i)<='Z')
				sb.setCharAt(i, (char) (sb.charAt(i)+32));
		}
		return sb.toString();
	}

	public String processStyleSheet(String input, int pos) {
		if(_stylesheet.size()==0)
			return input;
		Matcher m = markerReg.matcher(input);
		//HashSet<String> Already = new HashSet<>();
		StringBuilder transcriptor = new StringBuilder();
		String last=null;
		int lastEnd=0;
		boolean returnRaw=true;
		while(m.find()) {
			String now = m.group(1);
			String[] nowArr = _stylesheet.get(now);
			if(nowArr==null)
				if(now.equals("0")) {
					nowArr=new String[] {getCachedEntryAt(pos),""};
				}
			if(nowArr==null) {
				if(last!=null) {
					transcriptor.append(last);
					last=null;
				}
				continue;
			}
			transcriptor.append(input, lastEnd, m.start());
			if(last!=null) transcriptor.append(last);
			transcriptor.append(StringEscapeUtils.unescapeHtml3(nowArr[0]));
			lastEnd = m.end();
			last = StringEscapeUtils.unescapeHtml3(nowArr[1]);
			returnRaw=false;
		}
		if(returnRaw)
			return input;
		else
			return transcriptor.append(last==null?"":last).append(input.substring(lastEnd)).toString();
	}

	public File f() {
		return f;
	}
}



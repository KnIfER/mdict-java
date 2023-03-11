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

import com.alibaba.fastjson.JSONObject;
import com.knziha.plod.dictionary.Utils.BU;
import com.knziha.plod.dictionary.Utils.F1ag;
import com.knziha.plod.dictionary.Utils.Flag;
import com.knziha.plod.dictionary.Utils.GetIndexedString;
import com.knziha.plod.dictionary.Utils.IU;
import com.knziha.plod.dictionary.Utils.SU;
import com.knziha.plod.dictionary.Utils.key_info_struct;
import com.knziha.plod.dictionary.Utils.myCpr;
import com.knziha.plod.dictionary.Utils.record_info_struct;
import com.knziha.plod.dictionarymodels.BookPresenter;
import com.knziha.plod.plaindict.CMN;
import com.knziha.rbtree.RBTree_additive;

import org.apache.commons.lang3.StringUtils;
import org.knziha.metaline.Metaline;
import org.anarres.lzo.LzoDecompressor1x;
import org.anarres.lzo.lzo_uintp;
import org.apache.commons.text.StringEscapeUtils;
import org.jcodings.Encoding;
import org.jcodings.specific.*;
import org.joni.Option;
import org.joni.Regex;
import org.joni.exception.SyntaxException;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

import static com.knziha.plod.dictionary.SearchResultBean.SEARCHENGINETYPE_REGEX;
import static com.knziha.plod.dictionary.SearchResultBean.SEARCHTYPE_SEARCHINNAMES;
import static com.knziha.plod.dictionary.SearchResultBean.SEARCHTYPE_SEARCHINTEXTS;


/**
 * <h2>Mdict Java Library</h2>
 * <b>FEATURES</b>:<br/>
 * 1. getEntryAt(long) & lookUp(String) : List entries and perform fast binary search for mdict file format.<br/>
 * 2. lookUpRange(String,...) : Search for multiple entries. <br/>
 * 3. flowerFindAllContents(String,...) : Fast Multi-threaded search in all contents.<br/>
 * 4. flowerFindAllKeys(String,...) : Fast Multi-threaded search in all entries.<br/>
 * 5. Optional regex expression engine( Joni ) or wildcards( .* ) for above two searching methods.<br/><br/>
 * Author : KnIfER<br/>
 * <b>Licence</b> : Apache2.0 under this package (com.knziha.plod.dictionary.*); GPL3.0 for anything else including the mdictBuilder. <br/>
 */
@SuppressWarnings("SpellCheckingInspection")
public class mdict extends mdBase implements UniversalDictionaryInterface{
	private mdict parent;
	protected Encoding encoding;

	/** Packed mdd files. */
	protected List<mdictRes> mdd;
	/** Unpacked file tree. */
	protected List<File> ftd;

	protected mdict virtualIndex;

	public String _Dictionary_fName;
	public /*final*/ boolean isResourceFile;
	private EncodeChecker encodeChecker;
	private HashMap<Long, String> PageNumberMap;
	protected File fZero;
	private long fZero_LPT;
	
	byte[] options;
	
	public static String error_input;

	public boolean getIsDedicatedFilter(byte firstFlag){
		return false;
	}
	
	
	int mCaseStrategy;
	
	
	//public int KeycaseStrategy=0;//0:global 1:Java API 2:classical
	public int getCaseStrategy(){
		return mCaseStrategy;
	}
	public void setCaseStrategy(int val){
		mCaseStrategy = val;
	}
	
	@Override
	public File getFile() {
		return f;
	}
	
	public boolean getOnlyContainsImg(){
		return false;
	}
	
	public static boolean bGlobalUseClassicalKeycase=false;
	
	public boolean isGBoldCodec;

	public String currentDisplaying;

	public volatile boolean searchCancled;

	/** validation schema<br/>
	 * 0=none; 1=check even; 2=check four; 3=check direct; 4=check direct for all; 5=1/3; */
	protected int checkEven;
	protected int maxEB;

	public byte[] htmlOpenTag;
	public byte[] htmlCloseTag;
	public byte[][] htmlTags;
	public byte[][] htmlTagsA;
	public byte[][] htmlTagsB;
	public byte[] spaceBytes;
	//构造
	public mdict(String fn) throws IOException{
		this(new File(fn), 0, null, null);
	}
	
	//构造
	public mdict(File fn, int pseudoInit, StringBuilder buffer, Object tag) throws IOException {
		super(fn, pseudoInit, buffer, tag);
		if(pseudoInit==1) {
			_Dictionary_fName = f.getName();
		}
	}

	protected mdict(mdict master, DataInputStream data_in, long _ReadOffset) throws IOException {
		super(master, data_in);
		parent=master;
		ReadOffset=_ReadOffset;
		isResourceFile=false;
	}

	@Override
	protected void init(DataInputStream data_in) throws IOException {
		super.init(data_in);
		_Dictionary_fName = f.getName();
		textLineBreak=lineBreakText.getBytes(_charset);
		// ![0] load options
		ScanSettings();
		// ![1] load mdds
		loadInResourcesFiles(null);
		calcFuzzySpace();
		if(_header_tag.containsKey("hasSlavery")){
			try {
				long skip = data_in.skipBytes((int) _key_block_size);
				decode_record_block_size(data_in);
				int toTail=(int) (_record_block_size+_record_block_info_size);
				//SU.Log("Slavery.Init ...", skip, _key_block_size, ReadOffset, _record_block_offset, _version, toTail);
				skip+=data_in.skipBytes(toTail);
				if(skip==_key_block_size + toTail && data_in.available()>0){
					virtualIndex = new mdict(this, data_in, ReadOffset+_record_block_offset+(_version>=2?32:16)+toTail);
					SU.Log("Slavery.Init OK");
				}
			} catch (IOException e) {
				SU.Log("Slavery.Init Error");
				SU.Log(e);
			}
		}
		data_in.close();
		isGBoldCodec = _encoding.startsWith("GB") && !_header_tag.containsKey("PLOD");
	}

	protected boolean handleDebugLines(String line) {
		//SU.Log("handleDebugLines", line);
		if(line.length()>0){
			line = line.replace("\\", File.separator);
			if(line.startsWith(":")){
				String[] arr = line.substring(1).split(":"); //竟然长度为3
				long id;
				if(arr.length==2&&(id=IU.parseLong(arr[0]))>=0) {
					if(PageNumberMap==null) {
						PageNumberMap=new HashMap<>();
					}
					PageNumberMap.put(id, arr[1]);
				}
			} else if(line.startsWith(File.separator)){
				File p = f.getParentFile();
				File f = new File(p, line.substring(1));
				if(f.exists()) {
					if(f.isDirectory()) {
						ftd.add(f);
					}
				}
				return true;
			} else if(line.contains(File.separator)){
				File f = new File(line);
				if(f.isDirectory()) {
					ftd.add(f);
				}
			}
			
			if(line.startsWith("`")&&line.length()>1){
				int nxt=line.indexOf("`", 1);
				_stylesheet.put(line.substring(1,nxt), line.substring(nxt+1).trim().split("`",2));
				return true;
			}
		}
		//SU.Log(".0.txt 存在", _Dictionary_fName, line);
		return false;
	}

	public boolean hasVirtualIndex() {
		return virtualIndex!=null;
	}
	
	public InputStream getResourceByKey(String key) {
		//SU.Log("getResourceByKey", _Dictionary_fName, ftd);
		try {
			if(ftd !=null && ftd.size()>0){
				String keykey = key.replace("\\",File.separator);
				for(File froot: ftd){
					File ft= new File(froot, keykey);
					//SU.Log("getResourceByKey", _Dictionary_fName, ft.getAbsolutePath(), ft.exists());
					if(ft.exists()) {
						try {
							return new FileInputStream(ft);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
			}
			if(isResourceFile){
				int idx = lookUp(key);
				if(idx>=0) {
					try {
						return getResourseAt(idx);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
			else if(mdd!=null && mdd.size()>0){
				for(mdictRes mddTmp:mdd){
					int idx = mddTmp.lookUp(key);
					if(idx>=0) {
						//CMN.debug("got resource @", idx, key+"=?"+mddTmp.getEntryAt(idx));
						try {
							return mddTmp.getResourseAt(idx);
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
					//else SU.Log("chrochro inter_ key is not find:",_Dictionary_fName,key, idx);
				}
			}
		} catch (Exception e) {
			CMN.hotTracingObject = getPath();
			throw e;
		}
		return null;
	}

	protected void ScanSettings() {

	}

	public String getCachedEntryAt(long pos) {
		return currentDisplaying;
	}

	@Override
	public long getNumberEntries() {
		if(virtualIndex!=null)
			return virtualIndex._num_entries;
		return _num_entries;
	}

	//for lv
	public String getEntryAt(long position, Flag mflag) {
		if(virtualIndex!=null)
			return virtualIndex.getEntryAt(position, mflag);
		if(position==-1) return "about:";
		if(position>=_num_entries) return position+">!!!"+_num_entries;
		if(_key_block_info_list==null) read_key_block_info(null);
		int blockId = accumulation_blockId_tree.xxing(new myCpr<>(position,1)).getKey().value;
		key_info_struct infoI = _key_block_info_list[blockId];
		if(compareByteArray(infoI.headerKeyText, infoI.tailerKeyText)==0)
			mflag.data = new String(infoI.headerKeyText,_charset);
		else
			mflag.data = null;
		//TODO null pointer error
		CMN.hotTracingObject = getPath();
		try {
			cached_key_block keyInfo = prepareItemByKeyInfo(infoI, blockId, null);
			String ret = keyInfo.getString((int) (position - infoI.num_entries_accumulator));
			CMN.hotTracingObject = null;
			return ret;
		} catch (Exception e) {
			CMN.hotTracingObject = getPath() + e;
			CMN.debug(e);
			return "!!!";
		}
	}

	@Override
	public String getEntryAt(long position) {
		if(virtualIndex!=null)
			return virtualIndex.getEntryAt(position);
		return super.getEntryAt(position);
	}

	public int reduce_index2(byte[] phrase, int start, int end) {//via mdict-js
		int len = end-start;
		if (len > 1) {
			len = len >> 1;
			//show("reducearound:"+(start + len - 1)+"@"+len+": "+new String(_key_block_info_list[start + len - 1].tailerKeyText));
			//show(start+"::"+end+"   "+new String(_key_block_info_list[start].tailerKeyText,_charset)+"::"+new String(_key_block_info_list[end==_key_block_info_list.length?end-1:end].tailerKeyText,_charset));
			byte[] zhujio = _key_block_info_list[start + len - 1].tailerKeyText;
			return compareByteArray(phrase, /*isCompact*/false?zhujio:processMyText(new String(zhujio,_charset)).getBytes(_charset))>0
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
			//SU.Log("reducearound:"+(start + len - 1)+"@"+len+": "+new String(_key_block_info_list[start + len - 1].tailerKeyText));
			//SU.Log(start+"::"+end+"   "+new String(_key_block_info_list[start].tailerKeyText,_charset)+"::"+new String(_key_block_info_list[end==_key_block_info_list.length?end-1:end].tailerKeyText,_charset));
			String zhujio = new String(_key_block_info_list[start + len - 1].tailerKeyText,_charset);
			return phrase.compareToIgnoreCase(/*isCompact*/false?zhujio:processMyText(zhujio))>0
					? reduce_index(phrase,start+len,end)
					: reduce_index(phrase,start,start+len);
		} else {
			return start;
		}
	}
	public int reduce_index_with_keycase(String phrase,int start,int end) {//via mdict-js
		int len = end-start;
		if (len > 1) {
			len = len >> 1;
			//SU.Log("reducearound:"+(start + len - 1)+"@"+len+": "+new String(_key_block_info_list[start + len - 1].tailerKeyText));
			//SU.Log(start+"::"+end+"   "+new String(_key_block_info_list[start].tailerKeyText,_charset)+"::"+new String(_key_block_info_list[end==_key_block_info_list.length?end-1:end].tailerKeyText,_charset));
			String zhujio = new String(_key_block_info_list[start + len - 1].tailerKeyText,_charset);
			return phrase.compareTo(/*isCompact*/false?zhujio:processMyText(zhujio))>0
					? reduce_index_with_keycase(phrase,start+len,end)
					: reduce_index_with_keycase(phrase,start,start+len);
		} else {
			return start;
		}
	}
	public int lookUp(String keyword) {
		return lookUp(keyword,false);
	}
	String HeaderTextStr, TailerTextStr;
	
	public int lookUp(String keyword,boolean isSrict) {
		return lookUp(keyword,isSrict,null);
	}
	
	public int lookUp(String keyword, boolean isSrict, List<UniversalDictionaryInterface> morphogen)
	{
		if(isResourceFile) {
			if(!keyword.startsWith("\\"))
				keyword="\\"+keyword;
			return super.lookUp(keyword, isSrict);
		}
		if(virtualIndex!=null){
			return virtualIndex.lookUp(keyword, isSrict);
		}
		if(_key_block_info_list==null) read_key_block_info(null);
		String keyOrg=keyword;
		keyword = processMyText(keyword);
		byte[] kAB = keyword.getBytes(_charset);

		int blockId = -1;
		
		//isGBoldCodec = true;
		//SU.Log("isKeyCaseSensitive="+isKeyCaseSensitive, "isGBoldCodec="+isGBoldCodec, "isStripKey="+isStripKey);
		
		if(isGBoldCodec) {
			int boudaryCheck = compareByteArray(_key_block_info_list[(int)_num_key_blocks-1].tailerKeyText,kAB);
			if(boudaryCheck<0)
				return -1;
			if(boudaryCheck==0) blockId = (int)_num_key_blocks-1;
			boudaryCheck = compareByteArray(_key_block_info_list[0].headerKeyText,kAB);
			if(boudaryCheck>0)
				return -1;
			if(boudaryCheck==0) return 0;
		}
		else {
			int boudaryCheck = processMyText(new String(_key_block_info_list[(int)_num_key_blocks-1].tailerKeyText,_charset)).compareTo(keyword);
			//SU.Log("TailTextStr::", processMyText(new String(_key_block_info_list[(int)_num_key_blocks-1].tailerKeyText,_charset)), boudaryCheck, keyword);
			if(boudaryCheck<0)
				return -1;
			if(boudaryCheck==0) blockId = (int)_num_key_blocks-1;
			if(HeaderTextStr==null)
				HeaderTextStr=processMyText(new String(_key_block_info_list[0].headerKeyText,_charset));
			boudaryCheck = HeaderTextStr.compareTo(keyword);
			//SU.Log("HeaderTextStr::", HeaderTextStr, boudaryCheck, keyword);
			if(boudaryCheck>0) {
				if(HeaderTextStr.startsWith(keyword)) {
					return isSrict?-(0+2):0;
				}else
					return -1;
			}
			if(boudaryCheck==0) return 0;
		}
		if(blockId==-1) {
			if(isGBoldCodec) blockId = reduce_index2(keyword.getBytes(_charset),0,_key_block_info_list.length);
			else if(isKeyCaseSensitive) blockId = reduce_index_with_keycase(keyword,0,_key_block_info_list.length);
			else blockId = reduce_index(keyword,0,_key_block_info_list.length);
		}
		if(blockId==-1) return blockId;

		//SU.Log("blockId:",blockId, new String(_key_block_info_list[blockId].headerKeyText,_charset), new String(_key_block_info_list[blockId].tailerKeyText,_charset));
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
		if(isGBoldCodec)
			//res = binary_find_closest2(infoI_cache.keys,keyword);//keyword
			res = reduce_keys2(infoI_cache,kAB,0,infoI_cache.key_offsets.length);
		else
			//res = binary_find_closest(infoI_cache.keys,keyword);//keyword
			res = reduce_keys(infoI_cache,keyword,0,infoI_cache.key_offsets.length);

		if (res==-1){
			SU.Log("search failed!"+keyword);
			return -1;
		}
		//SU.Log(keyword, res, getEntryAt((int) (res+infoI.num_entries_accumulator)));
		////if(isCompact) //compatibility fix
		String other_key = infoI_cache.getString(res);
		String looseMatch = processMyText(other_key);
		boolean bIsEqual = looseMatch.equals(keyword);

		if(!bIsEqual){
			boolean b2=false;
			Matcher m=null;
			if(other_key.endsWith(">") && (keyOrg.endsWith(">")||(b2 = (m = numSuffixedReg.matcher(keyOrg)).find()))){
				/* possible to be number-suffixed */
				int idx2 = b2?m.start(1):keyOrg.lastIndexOf("<");
				if(idx2>0 && idx2==other_key.lastIndexOf("<")) {
					int start = parseint(other_key.substring(idx2+1,other_key.length()-1));
					int target = b2? IU.parsint(m.group(1))
						:parseint(keyOrg.substring(idx2+1,keyOrg.length()-1));
					//CMN.Log(keyOrg,other_key,start,target);
					int PstPosition = (int) (infoI.num_entries_accumulator + res + (target-start));
					String other_other_key = getEntryAt(PstPosition);
					if(other_other_key.length()>idx2 && other_other_key.endsWith(">") && other_other_key.charAt(idx2)=='<') {
						/* match end key's number */
						//if(keyOrg.startsWith(other_other_key.substring(0, idx2))){
						if(keyOrg.regionMatches(true, 0, other_other_key, 0, idx2)) {
							int end = parseint(other_other_key.substring(idx2+1,other_other_key.length()-1));
							if(target==end) {
								SU.Log("target==end", getEntryAt(PstPosition));
								return PstPosition;
							}
						}
					}
				}
			}
			
			if (morphogen != null) {
				char ch = keyword.charAt(0);
				boolean same1stChar = looseMatch.charAt(0)==ch;
				if (!same1stChar) {
					try {
						looseMatch = getEntryAt(infoI.num_entries_accumulator+res-1);
						same1stChar = looseMatch.charAt(0)==ch;
					} catch (Exception e) {
						SU.Log(e);
					}
				}
				if (same1stChar) {
					for(UniversalDictionaryInterface d:morphogen) {
						int idx = d.guessRootWord(this, keyword);
						if (idx>=0) {
							return idx;
						}
					}
				}
			}
			
			if(isSrict) {
				//SU.Log("isSrict:", morphogen, looseMatch.charAt(0),other_key.charAt(0));
				//SU.Log("isSrict:", keyword, other_key, getEntryAt((int) (infoI.num_entries_accumulator+res)), res, "::",  -1 * (res + 2));
				return -1*(int) ((infoI.num_entries_accumulator+res+2));
			}
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

	public int reduce_keys(cached_key_block keys,String val,int start,int end) {//via mdict-js
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

			//SU.Log(val+"->"+keys.getString(start + len - 1)+" ="+val.compareTo(processMyText(keys.getString(start + len - 1))));
			//SU.Log(start+"::"+end+"   "+keys.getString(start)+"::"+keys.getString(end==keys.key_offsets.length?end-1:end));


			return val.compareTo(processMyText(keys.getString(start + len - 1)))>0
					? reduce_keys(keys,val,start+len,end)
					: reduce_keys(keys,val,start,start+len);
		} else {
			return start;
		}
	}
	public int reduce_keys2(cached_key_block keys,byte[] val,int start,int end) {//via mdict-js
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
			return compareByteArray(val, processMyText(keys.getString(start + len - 1)).getBytes(_charset))>0
					? reduce_keys2(keys,val,start+len,end)
					: reduce_keys2(keys,val,start,start+len);
		} else {
			return start;
		}
	}

	public Object ReRoute(String keyraw) throws IOException {
		if(virtualIndex!=null)
			return virtualIndex.ReRoute(keyraw);
		if(isResourceFile)
			return -1;
		int c=0;
		int i = lookUp(keyraw, true);
		if(i<0){
			return -1;
		}
		return getRecordAt(i, null, true);
	}
	
	
	/**debug('fatal debug annot::restoring::multiple!!! 多视图列表');
	 window.remarkPage = function(t, fun) {
		var frames = document.getElementsByClassName('_PDict_body'), map=[];
		for(var i=0,f;f=frames[i++];) {
			map[f.getAttribute('pd_pos')] = f;
		}
		t = t.split('\t\n\0'); // \n\n\n
	 	for(var i=0,f,n;n=t[i];i+=2) {
			f = map[t[i+1]];
			if(f) {
	 			RestoreMarks(n, f, document)
			}
		}
	 }
	 window.markPage = function(tcn) {
	 	var f = getSelection().getRangeAt(0).startContainer;
	 	while(f=f.parentNode) {
		 	if(f.classList.contains('_PDict_body')) {
	 			MakeMark(tcn, f, 0, parseInt(f.getAttribute('pd_pos')))
			 	break;
			 }
		 }
	 }
	 */
	@Metaline(trim = false)
	public final static String RESTORE_MARKS = ""; // 仅为旧版多页面，不以url形式加载的

	public String getRecordsAt(GetRecordAtInterceptor getRecordAtInterceptor, long... positions) throws IOException {
		if(isResourceFile)
			return constructLogicalPage(positions);
		String ret;
		long p0 = positions[0];
		if(positions.length==1) {
			ret = getRecordAt(p0, getRecordAtInterceptor, true);
		} else {
			StringBuilder sb = new StringBuilder();
			sb.append("<script>");
			sb.append(RESTORE_MARKS);
			sb.append("</script>");
			int c=0;
			for(long i:positions) {
				sb.append("<div class='_PDict_body' pd_pos=").append(i).append(">");
				sb.append(getRecordAt(i, getRecordAtInterceptor, true));//.trim()
				sb.append("</div>");
				if(c!=positions.length-1)
					sb.append("<HR>");
				c++;
			}
			ret = sb.toString();
		}
		return processStyleSheet(ret, p0);
	}

	// 初始化
	/** @param positions virutal indexes*/
	public String getVirtualRecordsAt(Object presenter, long... positions) throws IOException {
		if(virtualIndex==null)
			return getRecordsAt(null, positions);
		StringBuilder sb = new StringBuilder();
		int c=0, lastAI=-1;
		for(long i:positions) {
			String vi = virtualIndex.getRecordAt(i, null, true);
			JSONObject vc = JSONObject.parseObject(vi);
			int AI=vc.getInteger("I");
			if(lastAI==AI){
				//TODO overlaping case
			}
			else{
				String JS = vc.getString("JS");
				String record = getRecordAt(AI, null, true);
				int headId= record.indexOf("<head>");
				if(headId<0) {
					headId=-6;
					sb.append("<head>");
				}
				sb.append(record, 0, headId+6);
				sb.append("<script>");
				sb.append(JS==null?"":JS);
				sb.append("</script>");
				if(headId<0) sb.append("<head>");
				sb.append(record, headId+6, record.length());
			}
			lastAI=AI;

			if(c!=positions.length-1)
				sb.append("<HR>");
			c++;
		}
		sb.append("<div class=\"_PDict\" style='display:none;'><p class='bd_body'/>");
		if(mdd!=null && mdd.size()>0) sb.append("<p class='MddExist' id='MddExist'/>");
		sb.append("</div>");
		return processStyleSheet(sb.toString(), positions[0]);
	}
	
//	@Override
//	public String getVirtualTextValidateJs(Object presenter, WebViewmy mWebView, long position) {
//		return "";
//	}
	
	@Override
	public String getVirtualTextEffectJs(Object presenter, long[] positions) {
		return "";
	}
	
	@Override
	public long getBooKID() {
		return _bid;
	}
	
	@Override
	public void setBooKID(long id) {
		_bid = id;
	}
	
	/**
	<style>
	audio {
		position:absolute;
		top:32%;
		width:100%;
	}
	h2 {
	 	position:absolute;
	 	top:1%;
	 	width:100%;
	 	text-align: center;
	}
	</style>
	 */
	@Metaline
	String logicalPageHeader="SUBPAGE";

	/** Construct Logical Page For mdd resource file. */
	private String constructLogicalPage(long...positions) {
		CMN.Log("constructLogicalPage!!!");
		StringBuilder LoPageBuilder = new StringBuilder();
		LoPageBuilder.append(logicalPageHeader);
		for(long i:positions) {
			String key = getEntryAt(i);
			if(key.startsWith("/")||key.startsWith("\\"))
				key=key.substring(1);
			key=StringEscapeUtils.escapeHtml3(key);
			if(htmlReg.matcher(key).find()){
				LoPageBuilder.append(decodeRecordData(positions[0], StandardCharsets.UTF_8));
			}else{
				if(imageReg.matcher(key).find()){
					LoPageBuilder.append("<img style='width:100%; height:auto;' src=\"").append(key).append("\"></img>");
				}
				else if(soundReg.matcher(key).find()){
					LoPageBuilder.append("<h2>").append(key).append("</h2>");
					LoPageBuilder.append("<audio controls='controls' autoplay='autoplay' src=\"").append(key).append("\"></audio>");
					LoPageBuilder.append("<h2 style='top:56%'>").append(key).append("</h2>");
				}
				else if(videoReg.matcher(key).find()){
					LoPageBuilder.append("<video width='320' height='240' controls=\"controls\" src=\"").append(key).append("\"></video>");
				}
			}
		}
		LoPageBuilder.append("<div class='bd_body'/>");
		return LoPageBuilder.toString();
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

	public String getRecordAt(long position) throws IOException {
		return getRecordAt(position, null, true);
	}
	
	public String getRecordAt(long position, GetRecordAtInterceptor getRecordAtInterceptor, boolean allowJump) throws IOException {
		if(getRecordAtInterceptor!=null)
		{
			String ret = getRecordAtInterceptor.getRecordAt(this, position);
			if (ret!=null) {
				return ret;
			}
		}
		if(ftd!=null && ftd.size()>0 && ReadOffset==0){
			File ft;
			for(File f:ftd){
				String key=getDebugPageResourceKey(position);
				ft=new File(f, key);
				//SU.Log(ft.getAbsolutePath(), ft.exists());
				if(ft.exists())
					return BU.fileToString(ft);
			}
		}
		if(position<0||position>=_num_entries)
			return "404 index out of bound";
		RecordLogicLayer va1=new RecordLogicLayer();
		super.getRecordData(position, va1);
		byte[] data = va1.data;
		int record_start=va1.ral;
		int record_end=va1.val;
		
		//SU.Log("record_start", record_start, record_end);
		
		if(record_start==record_end) {
			return StringUtils.EMPTY;
		}
		
		if(record_end>=record_start+textLineBreak.length
				&& textTailed(data, record_end-textLineBreak.length, textLineBreak)) record_end-=textLineBreak.length;

		String tmp = new String(data, record_start, record_end-record_start,_charset);

		if(allowJump && tmp.startsWith(linkRenderStr)) {
			//SU.Log("rerouting",tmp);
			//SU.Log(tmp.replace("\n", "1"));
			String key = tmp.substring(linkRenderStr.length());
			//todo clean up
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
				if (idx!=position)
					tmp=getRecordAt(idx, null, false);
			}
		}
		return tmp;
	}

	private String getDebugPageResourceKey(long position) {
		String key=null;
		if(PageNumberMap!=null) {
			key=PageNumberMap.get(position);
		}
		if(key==null) {
			key=Long.toString(position);
		}
		return key;
	}

	@Override
	public String decodeRecordData(long position, Charset charset) {
		if(ftd !=null && ftd.size()>0){
			for(File froot: ftd){
				File ft= new File(froot, ""+position);
				if(ft.exists()) {
					try {
						return new String(BU.fileToByteArr(ft), charset);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
		return super.decodeRecordData(position, charset);
	}

	// todo
	public static boolean textTailed(byte[] data, int off, byte[] textLineBreak) {
		if(off+2<data.length){
			return data[off]==textLineBreak[0]&&data[off+1]==textLineBreak[1]&&data[off+2]==textLineBreak[2];
		}
		return false;
	}

	long[] keyBlocksHeaderTextKeyID;
	public void fetch_keyBlocksHeaderTextKeyID(){
		int blockId = 0;
		long[] _keyBlocksHeaderTextKeyID = new long[(int)_num_key_blocks];
		byte[] key_block = new byte[(int) maxDecomKeyBlockSize];
		byte[]  _key_block_compressed = new byte[(int) maxComKeyBlockSize];
		byte[] currentKeyBlock;
		for(key_info_struct infoI:_key_block_info_list){
			currentKeyBlock=key_block;
			try {
				long start = infoI.key_block_compressed_size_accumulator;
				long compressedSize;
				if(blockId==_key_block_info_list.length-1)
					compressedSize = _key_block_size - _key_block_info_list[_key_block_info_list.length-1].key_block_compressed_size_accumulator;
				else
					compressedSize = _key_block_info_list[blockId+1].key_block_compressed_size_accumulator-infoI.key_block_compressed_size_accumulator;

				DataInputStream data_in = getStreamAt(_key_block_offset+start, false);

				data_in.read(_key_block_compressed, 0,(int) compressedSize);
				data_in.close();

				int BlockOff=0;
				int BlockLen=(int) infoI.key_block_decompressed_size;
				//解压开始
				switch (_key_block_compressed[0]|_key_block_compressed[1]<<8|_key_block_compressed[2]<<16|_key_block_compressed[3]<<32){
					case 0://no compression
						BlockOff=8;
						currentKeyBlock=_key_block_compressed;
						//System.arraycopy(_key_block_compressed, 8, key_block, 0,(int) (compressedSize-8));
					break;
					case 1:
						new LzoDecompressor1x().decompress(_key_block_compressed, 8, (int)(compressedSize-8), currentKeyBlock, 0,new lzo_uintp());
					break;
					case 2:
						//key_block = zlib_decompress(_key_block_compressed,(int) (+8),(int)(compressedSize-8));
						Inflater inf = new Inflater();
						inf.setInput(_key_block_compressed, 8 ,(int)(compressedSize-8));
						try {
							int ret = inf.inflate(currentKeyBlock,0,(int)(infoI.key_block_decompressed_size));
						} catch (DataFormatException e) {e.printStackTrace();}
						inf.end();
					break;
					case 3:
						SU.Zstd_decompress(_key_block_compressed, 8, (int)(compressedSize-8), currentKeyBlock, 0, (int)(infoI.key_block_decompressed_size));
					break;
					case 4:
						SU.Lz4_decompress(_key_block_compressed, 8, currentKeyBlock,0,(int)(infoI.key_block_decompressed_size));
					break;
				}
				//!!spliting curr Key block

				_keyBlocksHeaderTextKeyID[blockId] = _version<2 ?BU.toInt(currentKeyBlock, BlockOff)
						:BU.toLong(currentKeyBlock, BlockOff);

				blockId++;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		keyBlocksHeaderTextKeyID=_keyBlocksHeaderTextKeyID;
	}


	protected int split_recs_thread_number;
	public void flowerFindAllContents(String key, Object book, AbsAdvancedSearchLogicLayer SearchLauncher) throws IOException{
		SU.Log("Find In All Contents Started");
		if(isResourceFile||getOnlyContainsImg()) return;
		byte[][][][][] matcher=null;
		Regex Joniregex = null;
		if(SearchLauncher.getSearchEngineType()==SEARCHENGINETYPE_REGEX){
			if(encoding==null) encoding = bakeJoniEncoding(_charset);
			if(encoding!=null) {
				//if (getRegexAutoAddHead() && !key.startsWith(".*"))
				//	key = ".*" + key;
				byte[] pattern = key.getBytes(_charset);
				Joniregex = new Regex(pattern, 0, pattern.length, getRegexOption(), encoding);
			}
		}
		if(Joniregex==null) matcher =  leafSanLieZhi(SearchLauncher, _charset);

		if(_key_block_info_list==null) read_key_block_info(null);

		if(_record_info_struct_list==null) decode_record_block_header();

		if(keyBlocksHeaderTextKeyID==null) fetch_keyBlocksHeaderTextKeyID();

		split_recs_thread_number = _num_record_blocks<6?1:(int) (_num_record_blocks/6);//Runtime.getRuntime().availableProcessors()/2*2+10;
		split_recs_thread_number = split_keys_thread_number>16?6:split_keys_thread_number;
		//split_recs_thread_number = Runtime.getRuntime().availableProcessors();
		final int thread_number = Math.min(Runtime.getRuntime().availableProcessors()/2*2+2, split_keys_thread_number);
		//split_recs_thread_number = (int) (_num_record_blocks/16);

		final int step = (int) (_num_record_blocks/split_recs_thread_number);
		final int yuShu=(int) (_num_record_blocks%split_recs_thread_number);
		
		SU.Log("fatal_","split_recs_thread_number"+split_recs_thread_number);
		SU.Log("fatal_","thread_number"+thread_number);
		SU.Log("fatal_","step/yuShu", step, yuShu);


		ArrayList<SearchResultBean>[] _combining_search_tree=SearchLauncher.getTreeBuilding(book, split_recs_thread_number);

		SearchLauncher.poolEUSize.set(SearchLauncher.dirtyProgressCounter=0);
		
		//if (preparedStream==null) prepareFileStream();

		//ArrayList<Thread> fixedThreadPool = new ArrayList<>(thread_number);
		ExecutorService fixedThreadPool = OpenThreadPool(thread_number);
		for(int ti=0; ti<split_recs_thread_number; ti++){//分  thread_number 股线程运行
			if(SearchLauncher.IsInterrupted || searchCancled) break;
			final int it = ti;
			//if(false)
			if(split_recs_thread_number>thread_number) while (SearchLauncher.poolEUSize.get()>=thread_number) {
				try {
					Thread.sleep(2);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

			if(_combining_search_tree[it]==null)
				_combining_search_tree[it] = new ArrayList<>();

			if(split_recs_thread_number>thread_number) SearchLauncher.poolEUSize.addAndGet(1);
			
			Regex finalJoniregex = Joniregex;
			byte[][][][][] finalMatcher = matcher;
			fixedThreadPool.execute(
				new Runnable(){@Override public void run() {
					if(SearchLauncher.IsInterrupted || searchCancled) { SearchLauncher.poolEUSize.set(0); return; }
					final byte[] record_block_compressed = new byte[(int) maxComRecSize];//!!!避免反复申请内存
					final byte[] record_block_ = new byte[(int) maxDecompressedSize];//!!!避免反复申请内存
					F1ag flag = new F1ag();
					InputStream data_in = null;
					try
					{
						long seekTarget=_record_info_struct_list[it*step].compressed_size_accumulator+_record_block_offset+_number_width*4+_num_record_blocks*2*_number_width;
						//data_in = mOpenInputStream();
						//long seek = data_in.skip(seekTarget);
						data_in = getStreamAt(seekTarget, false);
						//if(seek!=seekTarget)
						//	throw new RuntimeException("seek!=seekTarget !!!");
						int jiaX=0;
						if(it==split_recs_thread_number-1) jiaX=yuShu;
						THREAD:
						for(int i=it*step; i<it*step+step+jiaX; i++)//_num_record_blocks
						{
							if(SearchLauncher.IsInterrupted || searchCancled) { SearchLauncher.poolEUSize.set(0); break; }
							record_info_struct RinfoI = _record_info_struct_list[i];

							int compressed_size = (int) RinfoI.compressed_size;
							int decompressed_size = (int) RinfoI.decompressed_size;
							data_in.read(record_block_compressed, 0, compressed_size);

							//解压开始
							switch (record_block_compressed[0]|record_block_compressed[1]<<8|record_block_compressed[2]<<16|record_block_compressed[3]<<32){
								case 0:
									System.arraycopy(record_block_compressed, 8, record_block_, 0, compressed_size-8);
								break;
								case 1:
									new LzoDecompressor1x().decompress(record_block_compressed, 8, (compressed_size-8), record_block_, 0, new lzo_uintp());
								break;
								case 2:
									Inflater inf = new Inflater();
									inf.setInput(record_block_compressed,8,compressed_size-8);
									int ret = inf.inflate(record_block_,0,decompressed_size);
									inf.end();
								break;
								case 3:
									SU.Zstd_decompress(record_block_compressed, 8, compressed_size-8, record_block_, 0, decompressed_size);
								break;
								case 4:
									SU.Lz4_decompress(record_block_compressed, 8, record_block_, 0, decompressed_size);
								break;
							}

							//内容块解压完毕
							long off = RinfoI.decompressed_size_accumulator;
							int key_block_id = binary_find_closest(keyBlocksHeaderTextKeyID,off);

							org.joni.Matcher Jonimatcher = null;
							if(finalJoniregex !=null)
								Jonimatcher = finalJoniregex.matcher(record_block_);
							long[] ko; int recordodKeyLen, try_idx;
							OUT:
							while(true) {
								if(SearchLauncher.IsInterrupted  || searchCancled || key_block_id>=_key_block_info_list.length) break THREAD;
								ko = prepareItemByKeyInfo(null,key_block_id,null).key_offsets;
								//if(infoI_cacheI.blockID!=key_block_id)
								//	throw new RuntimeException("bad !!!"+infoI_cacheI.blockID+" != "+key_block_id);
								for(int relative_pos=binary_find_closest(ko,off);relative_pos<ko.length;relative_pos++) {
									if(relative_pos<ko.length-1){//不是最后一个entry
										recordodKeyLen=(int) (ko[relative_pos+1]-ko[relative_pos]);
									}
									else if(key_block_id<keyBlocksHeaderTextKeyID.length-1){//不是最后一块key block
										recordodKeyLen=(int) (keyBlocksHeaderTextKeyID[key_block_id+1]-ko[relative_pos]);
									}else {
										recordodKeyLen = (int) (decompressed_size-(ko[ko.length-1]-RinfoI.decompressed_size_accumulator));
									}

									if(ko[relative_pos]-RinfoI.decompressed_size_accumulator+recordodKeyLen>RinfoI.decompressed_size) {
										//show("break OUT");
										break OUT;
									}

									/*
									File dump = new File("D:\\record_dump."+i+".bin");
									if(!dump.exists()) //块调试器
										BU.printFile(record_block_, 0, decompressed_size, dump);*/

									int start=(int) (ko[relative_pos]-RinfoI.decompressed_size_accumulator);

//									GlobalOptions.debug=relative_pos+_key_block_info_list[key_block_id].num_entries_accumulator==1844;
//									if(GlobalOptions.debug){
//										SU.Log("full res ::str", new String(record_block_, start, recordodKeyLen, _charset));
//										SU.Log("full res ::relative_pos", relative_pos, "kos_len", ko.length-1, "key_block_id", key_block_id, "keyBlocksHeaderTextKeyID_len", keyBlocksHeaderTextKeyID.length-1);
//										SU.Log("full res ::start-len", start, recordodKeyLen, record_block_.length);
//									} else continue;

									if(Jonimatcher==null){
										try_idx=-1;
										ArrayList<ArrayList<Object>> mpk;
										ArrayList<Object> mParallelKeys;
										for (int j = 0; j < finalMatcher.length; j++) { // and group
											if(SearchLauncher.IsInterrupted || searchCancled) break THREAD;
											mpk = SearchLauncher.mParallelKeys.get(j);
											for (int k = 0; k < finalMatcher[j].length; k++) { // or group
												mParallelKeys = mpk.get(k);
												int len = finalMatcher[j][k].length;
												int[] jumpMap = (int[]) mParallelKeys.get(len);
												try_idx=flowerIndexOf(record_block_,start,recordodKeyLen, finalMatcher[j][k],0,0, SearchLauncher, flag, mParallelKeys, jumpMap);
												//SU.Log("and_group>>"+j, "or_group#"+k, try_idx, nna);
												if(try_idx<0 ^ (jumpMap[len]&4)==0) break;
											}
											if(try_idx<0){
												break;
											}
										}
									} else {
										try_idx=Jonimatcher.searchInterruptible(start, start+recordodKeyLen, Option.DEFAULT);
									}

//									if(GlobalOptions.debug) SU.Log("full res ::", try_idx, key, (int) (ko[relative_pos]-RinfoI.decompressed_size_accumulator), recordodKeyLen, record_block_.length);
									
									if(SearchLauncher.IsInterrupted || searchCancled) break THREAD;

									if(try_idx!=-1) {
										//SU.Log("full res ::", try_idx, finalKey, new String(record_block_, (int) (ko[relative_pos]-RinfoI.decompressed_size_accumulator)+try_idx-100, 200, _charset));
										long pos = relative_pos+_key_block_info_list[key_block_id].num_entries_accumulator;
										SearchLauncher.dirtyResultCounter++;
										_combining_search_tree[it].add(new SearchResultBean(pos));
									}
									SearchLauncher.dirtyProgressCounter++;
								}
								key_block_id++;
							}
						}
						data_in.close();

					} catch (Exception e) {
						try {
							if(data_in!=null) data_in.close();
						} catch (IOException ignored) {  }
						//BU.printBytes(record_block_compressed,0,4);
						//CMN.Log(record_block_compressed[0]|record_block_compressed[1]<<8|record_block_compressed[2]<<16|record_block_compressed[3]<<32);
						SU.Log(Thread.currentThread().getId(), e);
					}
					SearchLauncher.thread_number_count--;
					if(split_recs_thread_number>thread_number) SearchLauncher.poolEUSize.addAndGet(-1);
				}}
			);
		}
		SearchLauncher.currentThreads=fixedThreadPool;
		fixedThreadPool.shutdown();
		try {
			fixedThreadPool.awaitTermination(45, TimeUnit.MINUTES);
		} catch (Exception e1) {
			SU.Log("Find In Full Text Interrupted!!!");
			//e1.printStackTrace();
		}
//		for(Thread t:fixedThreadPool){
//			try {
//				t.join();
//			} catch (InterruptedException e) {
//				SU.Log("Find In Full Text Interrupted!!!");
//				e.printStackTrace();
//			}
//		}
	}
	
	public void doForAllRecords(Object book, AbsAdvancedSearchLogicLayer SearchLauncher, DoForAllRecords dor, Object parm) throws IOException {
		//SU.Log("Find In All Contents Started");
		if(isResourceFile||getOnlyContainsImg()||dor==null) return;
		byte[][][][][] matcher=null;
		
		if(_key_block_info_list==null) read_key_block_info(null);
		
		if(_record_info_struct_list==null) decode_record_block_header();
		
		if(keyBlocksHeaderTextKeyID==null) fetch_keyBlocksHeaderTextKeyID();
		
		split_recs_thread_number = _num_record_blocks<6?1:(int) (_num_record_blocks/6);//Runtime.getRuntime().availableProcessors()/2*2+10;
		split_recs_thread_number = split_keys_thread_number>16?6:split_keys_thread_number;
		//split_recs_thread_number = Runtime.getRuntime().availableProcessors();
		final int thread_number = Math.min(Runtime.getRuntime().availableProcessors()/2*2+2, split_keys_thread_number);
		// split_recs_thread_number = 1;
		
		final int step = (int) (_num_record_blocks/split_recs_thread_number);
		final int yuShu=(int) (_num_record_blocks%split_recs_thread_number);
		
		SU.Log("fatal_","split_recs_thread_number"+split_recs_thread_number);
		SU.Log("fatal_","thread_number"+thread_number);
		SU.Log("fatal_","step/yuShu", step, yuShu);
		
		SearchLauncher.unitAborted = false;
		
		SearchLauncher.poolEUSize.set(SearchLauncher.dirtyProgressCounter=0);
		
		//ArrayList<Thread> fixedThreadPool = new ArrayList<>(thread_number);
		ExecutorService fixedThreadPool = OpenThreadPool(thread_number);
		for(int ti=0; ti<split_recs_thread_number; ti++){//分  thread_number 股线程运行
			if(SearchLauncher.IsInterrupted || searchCancled) break;
			final int it = ti;
			//if(false)
			if(split_recs_thread_number>thread_number) while (SearchLauncher.poolEUSize.get()>=thread_number) {
				try {
					Thread.sleep(2);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			
			if(split_recs_thread_number>thread_number) SearchLauncher.poolEUSize.addAndGet(1);
			
			fixedThreadPool.execute(
					//(
					new Runnable(){@Override public void run()
					{
						if(SearchLauncher.IsInterrupted || SearchLauncher.unitAborted) { SearchLauncher.poolEUSize.set(0); return; }
						final byte[] record_block_compressed = new byte[(int) maxComRecSize];//!!!避免反复申请内存
						final byte[] record_block_ = new byte[(int) maxDecompressedSize];//!!!避免反复申请内存
						F1ag flag = new F1ag();
						InputStream data_in = null;
						Object tParm = dor.onThreadSt(parm);
						try
						{
							long seekTarget=_record_info_struct_list[it*step].compressed_size_accumulator+_record_block_offset+_number_width*4+_num_record_blocks*2*_number_width;
							//data_in = mOpenInputStream();
							//long seek = data_in.skip(seekTarget);
							data_in = getStreamAt(seekTarget, false);
							//if(seek!=seekTarget)
							//	throw new RuntimeException("seek!=seekTarget !!!");
							int jiaX=0;
							if(it==split_recs_thread_number-1) jiaX=yuShu;
							for(int i=it*step; i<it*step+step+jiaX; i++)//_num_record_blocks
							{
								//if(SearchLauncher.IsInterrupted || SearchLauncher.unitAborted) { SearchLauncher.poolEUSize.set(0); return; }
								record_info_struct RinfoI = _record_info_struct_list[i];
								
								int compressed_size = (int) RinfoI.compressed_size;
								int decompressed_size = (int) RinfoI.decompressed_size;
								data_in.read(record_block_compressed, 0, compressed_size);
								
								//解压开始
								switch (record_block_compressed[0]|record_block_compressed[1]<<8|record_block_compressed[2]<<16|record_block_compressed[3]<<32){
									case 0:
										System.arraycopy(record_block_compressed, 8, record_block_, 0, compressed_size-8);
										break;
									case 1:
										new LzoDecompressor1x().decompress(record_block_compressed, 8, (compressed_size-8), record_block_, 0, new lzo_uintp());
										break;
									case 2:
										Inflater inf = new Inflater();
										inf.setInput(record_block_compressed,8,compressed_size-8);
										int ret = inf.inflate(record_block_,0,decompressed_size);
										inf.end();
										break;
									case 3:
										SU.Zstd_decompress(record_block_compressed, 8, compressed_size-8, record_block_, 0, decompressed_size);
										break;
									case 4:
										SU.Lz4_decompress(record_block_compressed, 8, record_block_, 0, decompressed_size);
										break;
								}
								
								//内容块解压完毕
								long off = RinfoI.decompressed_size_accumulator;
								int key_block_id = binary_find_closest(keyBlocksHeaderTextKeyID,off);
								
								long[] ko; int recordodKeyLen, try_idx;
								OUT:
								while(true) {
									if(SearchLauncher.IsInterrupted  || SearchLauncher.unitAborted || key_block_id>=_key_block_info_list.length) break;
									ko = prepareItemByKeyInfo(null,key_block_id,null).key_offsets;
									//if(infoI_cacheI.blockID!=key_block_id)
									//	throw new RuntimeException("bad !!!"+infoI_cacheI.blockID+" != "+key_block_id);
									for(int relative_pos=binary_find_closest(ko,off);relative_pos<ko.length;relative_pos++) {
										if(relative_pos<ko.length-1){//不是最后一个entry
											recordodKeyLen=(int) (ko[relative_pos+1]-ko[relative_pos]);
										}
										else if(key_block_id<keyBlocksHeaderTextKeyID.length-1){//不是最后一块key block
											recordodKeyLen=(int) (keyBlocksHeaderTextKeyID[key_block_id+1]-ko[relative_pos]);
										}else {
											recordodKeyLen = (int) (decompressed_size-(ko[ko.length-1]-RinfoI.decompressed_size_accumulator));
										}
										
										if(ko[relative_pos]-RinfoI.decompressed_size_accumulator+recordodKeyLen>RinfoI.decompressed_size) {
											//show("break OUT");
											break OUT;
										}

									/*
									File dump = new File("D:\\record_dump."+i+".bin");
									if(!dump.exists()) //块调试器
										BU.printFile(record_block_, 0, decompressed_size, dump);*/
										
										int start=(int) (ko[relative_pos]-RinfoI.decompressed_size_accumulator);

//									GlobalOptions.debug=relative_pos+_key_block_info_list[key_block_id].num_entries_accumulator==1844;
//									if(GlobalOptions.debug){
//										SU.Log("full res ::str", new String(record_block_, start, recordodKeyLen, _charset));
//										SU.Log("full res ::relative_pos", relative_pos, "kos_len", ko.length-1, "key_block_id", key_block_id, "keyBlocksHeaderTextKeyID_len", keyBlocksHeaderTextKeyID.length-1);
//										SU.Log("full res ::start-len", start, recordodKeyLen, record_block_.length);
//									} else continue;
										
										dor.doit(parm, tParm, null, relative_pos+_key_block_info_list[key_block_id].num_entries_accumulator
										, null, record_block_, start, recordodKeyLen, _charset);
										
										if(SearchLauncher.IsInterrupted || SearchLauncher.unitAborted) break;
										
										//if(GlobalOptions.debug) SU.Log("full res ::", try_idx, key, (int) (ko[relative_pos]-RinfoI.decompressed_size_accumulator), recordodKeyLen, record_block_.length);
										SearchLauncher.dirtyProgressCounter++;
									}
									key_block_id++;
								}
							}
							data_in.close();
							
						} catch (Exception e) {
							try {
								if(data_in!=null) data_in.close();
							} catch (IOException ignored) {  }
							//BU.printBytes(record_block_compressed,0,4);
							//CMN.Log(record_block_compressed[0]|record_block_compressed[1]<<8|record_block_compressed[2]<<16|record_block_compressed[3]<<32);
							SU.Log(e);
						}
						dor.onThreadEd(parm);
						SearchLauncher.thread_number_count--;
						if(split_recs_thread_number>thread_number) SearchLauncher.poolEUSize.addAndGet(-1);
					}}
					//)
					//)
			);
			//t.start();
		}
		SearchLauncher.currentThreads=fixedThreadPool;
		fixedThreadPool.shutdown();
		try {
			fixedThreadPool.awaitTermination(5, TimeUnit.MINUTES);
		} catch (Exception e1) {
			SU.Log("Find In Full Text Interrupted!!!");
			//e1.printStackTrace();
		}
//		for(Thread t:fixedThreadPool){
//			try {
//				t.join();
//			} catch (InterruptedException e) {
//				SU.Log("Find In Full Text Interrupted!!!");
//				e.printStackTrace();
//			}
//		}
	}
	
	public static Encoding bakeJoniEncoding(Charset charset) {
		switch (charset.name()){
			case "US-ASCII":
				return ASCIIEncoding.INSTANCE;
			case "Big5":
				return BIG5Encoding.INSTANCE;
			case "Big5-HKSCS":
				return Big5HKSCSEncoding.INSTANCE;
			case "x-IBM949":
				return CP949Encoding.INSTANCE;
			case "EUC-JP":
				return EUCJPEncoding.INSTANCE;
			case "EUC-KR":
				return EUCKREncoding.INSTANCE;
			case "x-EUC-TW":
				return EUCTWEncoding.INSTANCE;
			case "GB2312":
				return GB2312Encoding.INSTANCE;
			case "GB18030":
				return GB18030Encoding.INSTANCE;
			case "GBK":
				return GBKEncoding.INSTANCE;
			case "ISO-8859-1":
				return ISO8859_1Encoding.INSTANCE;
			case "ISO-8859-2":
				return ISO8859_2Encoding.INSTANCE;
			case "ISO-8859-3":
				return ISO8859_3Encoding.INSTANCE;
			case "ISO-8859-4":
				return ISO8859_4Encoding.INSTANCE;
			case "ISO-8859-5":
				return ISO8859_5Encoding.INSTANCE;
			case "ISO-8859-6":
				return ISO8859_6Encoding.INSTANCE;
			case "ISO-8859-7":
				return ISO8859_7Encoding.INSTANCE;
			case "ISO-8859-8":
				return ISO8859_8Encoding.INSTANCE;
			case "ISO-8859-9":
				return ISO8859_9Encoding.INSTANCE;
			case "ISO-8859-10":
				return ISO8859_10Encoding.INSTANCE;
			case "ISO-8859-11":
				return ISO8859_11Encoding.INSTANCE;
			case "ISO-8859-13":
				return ISO8859_13Encoding.INSTANCE;
			case "ISO-8859-14":
				return ISO8859_14Encoding.INSTANCE;
			case "ISO-8859-15":
				return ISO8859_15Encoding.INSTANCE;
			case "ISO-8859-16":
				return ISO8859_16Encoding.INSTANCE;
			case "KOI8-R":
				return KOI8REncoding.INSTANCE;
			case "KOI8-U":
				return KOI8UEncoding.INSTANCE;
			case "Shift_JIS":
				return SJISEncoding.INSTANCE;
			default:
			case "UTF-8":
				return UTF8Encoding.INSTANCE;
			case "UTF-16BE":
				return UTF16BEEncoding.INSTANCE;
			case "UTF-16LE":
				return UTF16LEEncoding.INSTANCE;
			case "UTF-32BE":
				return UTF32BEEncoding.INSTANCE;
			case "UTF-32LE":
				return UTF32LEEncoding.INSTANCE;
			case "Windows-31j":
				return Windows_31JEncoding.INSTANCE;
			case "Windows-1250":
				return Windows_1250Encoding.INSTANCE;
			case "Windows-1251":
				return Windows_1251Encoding.INSTANCE;
			case "Windows-1252":
				return Windows_1252Encoding.INSTANCE;
			case "Windows-1253":
				return Windows_1253Encoding.INSTANCE;
			case "Windows-1254":
				return Windows_1254Encoding.INSTANCE;
			case "Windows-1257":
				return Windows_1257Encoding.INSTANCE;
		}
	}

	public static int kalyxIndexOf2(byte[] source, int sourceOffset, int sourceCount, byte[][] targets, int fromIndex, F1ag seelHolder) {
		int ldx=-1;
		for (int i = 0; i < targets.length; i++) {
			int idx=indexOf(source, sourceOffset, sourceCount, targets[i], 0, targets[i].length, fromIndex);
			if(idx>=fromIndex&&(ldx==-1||idx<ldx)) {
				seelHolder.val=i;
				ldx=idx;
				if(idx==fromIndex) break;
				//break;
			}
		}
		return ldx;
	}

	/** derived */
	public static int kalyxIndexOf(byte[] source, int sourceOffset, int sourceCount, byte[][] targets, int fromIndex, F1ag seelHolder) {
		int targetCounts = targets.length;
		int lookat=targetCounts;
		int max = sourceOffset + sourceCount;
		int i = sourceOffset + fromIndex;
		byte sI=0;
		while(i < max) {
			for (; i < max; i++) {//亦步亦趋求首项
				/* Look for first character. */
				sI=source[i];
				for (lookat = 0; lookat < targetCounts && sI!=targets[lookat][0]; lookat++)
					;
				if(lookat < targetCounts) break;
			}
			if(lookat>=targetCounts)/* 开始即结束 */ return -1;
			for (; lookat < targetCounts; lookat++){
				byte[] target = targets[lookat];
				if (sI == target[0]) {
					/* Found first character, now look at the rest of v2 */
					int targetCount = target.length;
					if(i<=max-targetCount) {
						int j = i + 1;
						int end = j + targetCount - 1;
						for (int k = 1; j < end && source[j] == target[k]; j++, k++)
							;
						if (j == end) {
							/* Found whole string. */
							seelHolder.val = lookat;
							return i - sourceOffset;//试之得
						}
					}
					//试之不得
				}
			}
			i++;
		}
		return -1;
	}

	public int safeKalyxIndexOf(byte[] source, int sourceOffset, int sourceCount, byte[][] targets, int fromIndex, F1ag seelHolder) {
		int fromIndex_ = fromIndex;
		int ret, len;
		boolean pass=true;
		while((ret = kalyxIndexOf(source, sourceOffset, sourceCount, targets, fromIndex, seelHolder))>=fromIndex){
			len = ret-fromIndex_;
			//if(false)
			if (checkEven != 0 && ret>fromIndex_) {
				if (checkEven == 2) {
					pass = len % 4 == 0;
				} else if (checkEven == 1 && len % 2 != 0) {
					pass = false;
				} else {
					if(encodeChecker !=null)
						pass = encodeChecker.checkBefore(source, sourceOffset, fromIndex_, ret);
					else{
						int start;
						for (int i = 1; i < maxEB; i++) {
							if(checkEven==1&&i==1/*二四变长编码*/
									|| checkEven!=5&&i==3/*一二四变长编码*/) i++;
							if (ret - i < 0) break;
							start = sourceOffset + ret - i;
							len = sourceOffset + ret - start;
							String validfyCode = new String(source, start, len, _charset);
							len = validfyCode.length();
							pass = len > 0 && (validfyCode.charAt(len - 1) != 65533);
							//SU.Log("validfyCode", validfyCode, validfyCode.charAt(len - 1) == 65533, pass);
							if(pass) break;
						}
					}
				}
			}
			if(pass) return ret;
			fromIndex = ret + targets[seelHolder.val].length;
		}
		return -1;
	}

	public int safeKalyxLastIndexOf(byte[] source, int sourceOffset, int sourceStart, byte[][] targets, int fromIndex, F1ag seelHolder) {
		//if(true) return -1;
		int fromIndex_ = fromIndex;
		int ret, len;
		boolean pass=true;
		while((ret = kalyxLastIndexOf(source, sourceOffset, sourceStart, targets, fromIndex, seelHolder))>=0){
			len = fromIndex_-ret;
			if(ret<fromIndex_){
				if (checkEven == 2) {
					pass = len % 4 == 0;
				} else if (checkEven == 1 && len % 2 != 0) {
					pass = false;
				} else {
					if(encodeChecker !=null)
						pass = encodeChecker.checkAfter(source, sourceOffset, fromIndex, ret);
					else{
						int start =  ret + targets[seelHolder.val].length;
						for (int i = 1; i < maxEB; i++) {
							if(checkEven==1&&i==1/*二四变长编码*/
									|| checkEven!=5&&i==3/*一二四变长编码*/) i++;
							if(start+i>fromIndex+1) break;
							String validfyCode = new String(source, sourceOffset+start, i, _charset);
							len = validfyCode.length();
							pass = len > 0 && (validfyCode.charAt(len - 1) != 65533);
							//SU.Log("validfyCode", validfyCode, validfyCode.charAt(len - 1) == 65533, pass);
							if(pass) break;
						}
					}
//					int start = sourceOffset + ret + targets[seelHolder.val].length;
//					len = maxEB;
//					String validfyCode = new String(source, start, len, _charset);
//					//SU.Log("validfyCode", validfyCode);
//					pass = validfyCode.length() > 0 && validfyCode.charAt(0) != 65533;
				}
			}

			if(pass) return ret;
			fromIndex = ret - targets[seelHolder.val].length;
		}
		return -1;
	}

	public static int kalyxLastIndexOf(byte[] source, int sourceOffset, int sourceStart, byte[][] targets, int fromIndex, F1ag seelHolder) {
		int targetCounts = targets.length;
		byte[] target;
		int lookat=0;
		byte sI=0;
		int i = sourceOffset + fromIndex;
		while(i >= sourceStart) {
			for (; i >= sourceStart; i--) {//亦步亦趋求首项
				/* Look for first character. */
				sI = source[i];
				for (lookat = 0; lookat < targetCounts && sI != (target=targets[lookat])[target.length-1]; lookat++)
					;
				if(lookat < targetCounts) break;
			}
			if(lookat>=targetCounts)/* 开始即结束 */ return -1;
			for (; lookat < targetCounts; lookat++){
				target = targets[lookat];
				if (sI == target[target.length-1]) {
					/* Found first character, now look at the rest of v2 */
					int targetCount = target.length;
					if(i+1>=sourceStart+targetCount) {
						int j = i - 1;
						int end = j - targetCount + 1;
						for (int k = target.length-2; j > end && source[j] == target[k]; j--, k--)
							;
						if (j == end) {
							/* Found whole string. */
							seelHolder.val = lookat;
							return i - sourceOffset;//试之得
						}
					}
					//试之不得
				}
			}
			i--;
		}
		return -1;
	}

	/*
	 * https://stackoverflow.com/questions/21341027/find-indexof-a-byte-array-within-another-byte-array
	 * Gustavo Mendoza's Answer*/
	public static int indexOf(byte[] source, int sourceOffset, int sourceCount, byte[] target, int targetOffset, int targetCount, int fromIndex) {
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
	//public ArrayList<Integer>[] combining_search_tree_4;

	public void executeAdvancedSearch(String key, int i, AbsAdvancedSearchLogicLayer layer) throws IOException {
		if(layer.type==SEARCHTYPE_SEARCHINNAMES){
			flowerFindAllKeys(key, i, layer);
		}
		else{
			flowerFindAllContents(key, i, layer);
		}
	}

	public String getVirtualRecordAt(Object presenter, long vi) throws IOException {
		return virtualIndex.getRecordAt(vi, null, true);
	}

	public String getDictionaryName() {
		return _Dictionary_fName;
	}
	
	public boolean hasMdd() {
		return mdd!=null && mdd.size()>0 || ftd!=null && ftd.size()>0 || isResourceFile;
	}
	
	@Override
	public String getRichDescription() {
		return _header_tag==null?"":_header_tag.get("Description");
	}
	
	@Override
	public boolean getIsResourceFile() {
		return isResourceFile;
	}
	
	@Override
	public Object[] getSoundResourceByName(String canonicalName) throws IOException {
		CMN.debug("mdict::getSoundResourceByName", canonicalName, mdd);
		if(getIsResourceFile()){
			int idx = lookUp(canonicalName, false);
			CMN.debug("mdict::getSoundResourceByName res lookup::", canonicalName, getIsResourceFile(), idx, canonicalName);
			if(idx>=0){
				String matched=getEntryAt(idx);
				if(matched.regionMatches(true,0, canonicalName, 0, canonicalName.length())){
					String spx = "spx";
					return new Object[]{matched.regionMatches(true,canonicalName.length(), spx, 0, spx.length()), getResourseAt(idx)};
				}
			}
		} else {
			if(mdd!=null && mdd.size()>0){
				for(mdictRes mddTmp:mdd){
					int idx = mddTmp.lookUp(canonicalName, false);
					CMN.debug("mdict::getSoundResourceByName lookup::", idx, canonicalName);
					if(idx>=0) {
						String matched=mddTmp.getEntryAt(idx);
						CMN.debug("mdict::getSoundResourceByName res matched=?", matched);
						if(matched.regionMatches(true,0, canonicalName, 0, canonicalName.length())){
							String spx = "spx";
							return new Object[]{matched.regionMatches(true,canonicalName.length(), spx, 0, spx.length()), mddTmp.getResourseAt(idx)};
						}
					}
					//else SU.Log("chrochro inter_ key is not find:",bookImpl.getFileName(),canonicalName, idx);
				}
			}
		}
		return null;
	}
	
	@Override
	public String getCharsetName() {
		return _charset.name();
	}
	
	public List<mdictRes> getMdd() {
		return mdd;
	}
	
	public String getAboutHtml() {
		return getAboutString();
	}

	public boolean hasStyleSheets() {
		//SU.Log("_stylesheet", _stylesheet.size(), _stylesheet.keySet().size(), _stylesheet.values().size());
		return _stylesheet.size()>0;
	}
	
	public static abstract class AbsAdvancedSearchLogicLayer{
		public int type;
		public int Idx;
		public volatile boolean IsInterrupted;
		public volatile String ErrorMessage=null;
		public volatile int thread_number_count = 1;
		public GetIndexedString jnFanMap;
		public GetIndexedString fanJnMap;
		public volatile int dirtyResultCounter;
		public volatile int dirtyProgressCounter;
		public volatile int dirtyTotalProgress;
		public long st;
		public String key;
		public volatile boolean unitAborted;

		public ArrayList<ArrayList<ArrayList<Object>>> mParallelKeys = new ArrayList<>();

		private boolean trimStart;
		private int trimEnd;

		/** .is 免死金牌  that exempt you from death for just one time */
		private HashSet<Integer> miansi = new HashSet<>();
		/** *is 越级天才, i.e., super super genius leap */
		private HashSet<Integer> yueji = new HashSet<>();

		/** Spray a search term into a 2D byte array. */
		public void flowerSanLieZhi(String str) {
			trimStart=false;
			trimEnd=-1;
			mParallelKeys.clear();
			int len=str.length();
			String character;
			ArrayList<ArrayList<Object>> currAndGroup = new ArrayList<>(2);
			ArrayList<Object> currOrGroup = new ArrayList<>(2);
			for(int i=0;i<len;i++){
				char ch = str.charAt(i);
				if(i<len-1 && ch>>10==0b110110 && str.charAt(i+1)>>10==0b110111){//surrogate pair
					character = str.substring(i, i+2);
					i++;
				} else{
					character = str.substring(i, i+1);
				}
				boolean process=false;
				switch (character){
					case "|"://新开或组
						MiansiYueji(currOrGroup, false);
						currAndGroup.add(currOrGroup);
						currOrGroup = new ArrayList<>(1);
					break;
					case "&"://新开与组
						if(i<len-1 && str.charAt(i+1)=='&'){//新开与与组
							MiansiYueji(currOrGroup, true);
							currAndGroup.add(currOrGroup);
							currOrGroup = new ArrayList<>(1);
							i+=1;
						} else {
							MiansiYueji(currOrGroup, false);
							currAndGroup.add(currOrGroup);
							mParallelKeys.add(currAndGroup);
							currOrGroup = new ArrayList<>(1);
							currAndGroup = new ArrayList<>(1);
						}
					break;
					case ".":{
						miansi.add(currOrGroup.size());
						int size=1;
						if(i<len-1 && str.charAt(i+1)=='('){ // quantifier
							int nextParenthese = str.indexOf(')', i+2);
							if(nextParenthese>0){
								int sz = IU.parsint(str.substring(i+2, nextParenthese), -1);
								if(sz>0){
									size = sz;
								}
								i=nextParenthese;
							}
						}
						currOrGroup.add(size);
					} break;
					case "*":{
						yueji.add(currOrGroup.size());
						int size=1;
						if(i<len-1 && str.charAt(i+1)=='('){ // quantifier
							int nextParenthese = str.indexOf(')', i+2);
							if(nextParenthese>0){
								int sz = IU.parsint(str.substring(i+2, nextParenthese), -1);
								if(sz>0){
									size = sz+1;
								}
								i=nextParenthese;
							}
						}
						currOrGroup.add(size);
					} break;
					case "^"://起始
						if(currOrGroup.size()==0){
							trimStart=true;
						}
					break;
					case "$"://终止
						trimEnd=i;
					break;
					case "\\"://转义
					break;
					default:
						process=true;
					break;
				}
				if(process){
					ArrayList<String> lexipart = new ArrayList<>(2);
					lexipart.add(character);
					String val=null;
					if(getEnableFanjnConversion()){ //繁简一
						if((val=jnFanMap.get(ch))!=null){
							for (int j = 1; j < val.length(); j++) {
								lexipart.add(val.substring(j, j+1));
							}
						} else if((val=fanJnMap.get(ch))!=null){
							lexipart.add(val.substring(0, 1));
						}
					}
					if(val==null){ //忽略大小写
						String UpperKey = character.toUpperCase();
						if(!UpperKey.equals(character)){
							lexipart.add(UpperKey);
						}
					}
					currOrGroup.add(lexipart);
				}
			}
			if(currOrGroup.size()>0){
				MiansiYueji(currOrGroup, false);
				currAndGroup.add(currOrGroup);
			}
			mParallelKeys.add(currAndGroup);
//			CMN.Log("与组共：", mParallelKeys.size());
//			for (int i = 0; i < mParallelKeys.size(); i++) {
//				CMN.Log("-->与组第：", i); CMN.Log("---->或组共：", mParallelKeys.get(i).size());
//				for (int j = 0; j < mParallelKeys.get(i).size(); j++) {
//					ArrayList<Object> mParallelKeys_ = mParallelKeys.get(i).get(j);
//					CMN.Log("---->或组第：", j, "长", mParallelKeys_.size());
//					for (int k = 0; k < mParallelKeys_.size(); k++) {
//						if(mParallelKeys_.get(k) instanceof ArrayList){
//							ArrayList<String> item = (ArrayList<String>) mParallelKeys_.get(k);
//							CMN.Log("-------->", item.toString());
//						}
//					}
//				}
//
//			}
		}

		private void MiansiYueji(ArrayList<Object> currOrGroup, boolean Andand) {
			int[] jumpMap = new int[currOrGroup.size()+1];
			for (int i = 0; i < jumpMap.length-1; i++) {
				if(miansi.contains(i)){
					jumpMap[i]=(int)currOrGroup.get(i);
				} else if(yueji.contains(i)){
					jumpMap[i]=-(int)currOrGroup.get(i);
				}
			}
			int val=0;
			if(trimStart)
				val=1;
			if(trimEnd==jumpMap.length-1)
				val|=2;
			if(Andand)
				val|=4;
			jumpMap[jumpMap.length-1]=val;
			currOrGroup.add(jumpMap);
			miansi.clear();
			yueji.clear();
			trimStart=false;
			trimEnd=-1;
		}

		public AtomicInteger poolEUSize = new AtomicInteger(0);

		public Object currentThreads;

		public ArrayList<ArrayList<Integer>[]> combining_search_tree;

		public abstract ArrayList<SearchResultBean>[] getTreeBuilding(Object book, int splitNumber);

		//public abstract void setCombinedTree(Object book, ArrayList<Integer>[] val, int searchType);

		public abstract ArrayList<SearchResultBean>[] getTreeBuilt(Object book);

		public abstract boolean getEnableFanjnConversion();

		public abstract Pattern getBakedPattern();
		
		public abstract String getPagePattern();

		public abstract void setCurrentPhrase(String currentPhrase);
		
		/** 0=wild card match; 1=regular expression search; 2=plain search. */
		public abstract int getSearchEngineType();
	}

	public int thread_number,step,yuShu;
	public void calcFuzzySpace(){
		//final String fkeyword = keyword.toLowerCase().replaceAll(replaceReg,emptyStr);
		//int entryIdx = 0;
		//show("availableProcessors: "+Runtime.getRuntime().availableProcessors());
		//show("keyBLockN: "+_key_block_info_list.length);
		split_keys_thread_number = _num_key_blocks<6?1:(int) (_num_key_blocks/6);//Runtime.getRuntime().availableProcessors()/2*2+10;
		split_keys_thread_number = split_keys_thread_number>16?6:split_keys_thread_number;
		thread_number = Math.min(Runtime.getRuntime().availableProcessors()/2*2+2, split_keys_thread_number);


		step = (int) (_num_key_blocks/split_keys_thread_number);
		yuShu=(int) (_num_key_blocks%split_keys_thread_number);

	}

	protected boolean getRegexAutoAddHead(){
		//if(parent!=null)
		//	return parent.getRegexAutoAddHead();
		return false;
	}

	protected int getRegexOption(){
		if(parent!=null)
			return parent.getRegexOption();
		return Option.IGNORECASE;
	}

	//XXX2
	public void flowerFindAllKeys(String key, Object book, AbsAdvancedSearchLogicLayer SearchLauncher)
	{
		if(virtualIndex!=null){
			virtualIndex.flowerFindAllKeys(key, book, SearchLauncher);
			return;
		}
		Pattern keyPattern=null;//用于 复核 ，并不直接参与搜索
		byte[][][][][] matcher=null;
		Regex Joniregex = null;
		boolean regexIntent=SearchLauncher.getSearchEngineType()==SEARCHENGINETYPE_REGEX;
		if(regexIntent){
			if(encoding==null) encoding = bakeJoniEncoding(_charset);
			if(encoding!=null) {
				//.if (getRegexAutoAddHead() && !key.startsWith(".*"))
				//.	key = ".*" + key;
				byte[] pattern = key.getBytes(_charset);
				Joniregex = new Regex(pattern, 0, pattern.length, getRegexOption()|Option.SINGLELINE, encoding);
			}
		}

		if(Joniregex==null){
			String keyword = key.toLowerCase();
			try {
				keyPattern=Pattern.compile(regexIntent?keyword:keyword.replace("*", ".+?"),Pattern.CASE_INSENSITIVE);
			}catch(Exception ignored) {}

			matcher =  leafSanLieZhi(SearchLauncher, _charset);
		}

		if(_key_block_info_list==null) read_key_block_info(null);

		//final String fkeyword = keyword.toLowerCase().replaceAll(replaceReg,emptyStr);
		//int entryIdx = 0;
		//SU.Log("availableProcessors: ", Runtime.getRuntime().availableProcessors());
		//show("keyBLockN: "+_key_block_info_list.length);
		split_keys_thread_number = _num_key_blocks<6?1:(int) (_num_key_blocks/6);//Runtime.getRuntime().availableProcessors()/2*2+10;
		final int thread_number = Math.min(Runtime.getRuntime().availableProcessors()/2*2+5, split_keys_thread_number);

		SearchLauncher.poolEUSize.set(SearchLauncher.dirtyProgressCounter=0);

		SearchLauncher.thread_number_count = split_keys_thread_number;
		final int step = (int) (_num_key_blocks/split_keys_thread_number);
		final int yuShu=(int) (_num_key_blocks%split_keys_thread_number);

		ExecutorService fixedThreadPoolmy = OpenThreadPool(thread_number);

		//show("~"+step+"~"+split_keys_thread_number+"~"+_num_key_blocks);
		
		ArrayList<SearchResultBean>[] final_combining_search_tree=SearchLauncher.getTreeBuilding(book, split_keys_thread_number);


		for(int ti=0; ti<split_keys_thread_number; ti++){//分  thread_number 股线程运行
			if(SearchLauncher.IsInterrupted || searchCancled ) break;
			//if(false)
			if(split_keys_thread_number>thread_number) while (SearchLauncher.poolEUSize.get()>=thread_number) {
				try {
					Thread.sleep(2);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			if(split_keys_thread_number>thread_number) SearchLauncher.poolEUSize.addAndGet(1);
			final int it = ti;
			Regex finalJoniregex = Joniregex;
			Pattern finalKeyPattern = keyPattern;
			byte[][][][][] finalMatcher = matcher;
			fixedThreadPoolmy.execute(
					new Runnable(){@Override public void run()
					{
						if(SearchLauncher.IsInterrupted || searchCancled ) {SearchLauncher.poolEUSize.set(0); return; }
						int jiaX=0;
						if(it==split_keys_thread_number-1) jiaX=yuShu;
						if(final_combining_search_tree[it]==null)
							final_combining_search_tree[it] = new ArrayList<>();
						ArrayList<SearchResultBean> item = final_combining_search_tree[it];

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
						//SU.Log("compressedSize_many;"+compressedSize_many);
						byte[] key_block = new byte[_maxDecomKeyBlockSize];/*分配资源 maxDecomKeyBlockSize 32770   65536 (common cache for index blocks)*/
						long start = _key_block_info_list[it*step].key_block_compressed_size_accumulator;

						try {
							DataInputStream data_in = getStreamAt(_key_block_offset+start, false);

							byte[]  _key_block_compressed_many = new byte[ compressedSize_many];
							data_in.read(_key_block_compressed_many, 0, _key_block_compressed_many.length);
							data_in.close();
							data_in=null;
							//大循环
							for(int blockId=it*step; blockId<it*step+step+jiaX; blockId++) {
								if(SearchLauncher.IsInterrupted || searchCancled ) { SearchLauncher.poolEUSize.set(0); return; }

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

								//SU.Log(key_block.length+";"+infoI.key_block_decompressed_size+";"+maxDecomKeyBlockSize);
								//SU.Log(_key_block_compressed_many.length, startI, key_block.length,_key_block_size-8);
								//解压开始
								switch (_key_block_compressed_many[startI]|_key_block_compressed_many[startI+1]<<8|_key_block_compressed_many[startI+2]<<16|_key_block_compressed_many[startI+3]<<32){
									case 0:
										System.arraycopy(_key_block_compressed_many, (startI+8), key_block, 0, (int) infoI.key_block_decompressed_size);
									break;
									case 1:
										new LzoDecompressor1x().decompress(_key_block_compressed_many, startI+8, compressedSize-8, key_block, 0,new lzo_uintp());
									break;
									case 2:
										Inflater inf = new Inflater();
										inf.setInput(_key_block_compressed_many, startI+8, compressedSize-8);
										try {
											int ret = inf.inflate(key_block,0,(int)(infoI.key_block_decompressed_size));
										} catch (Exception e) {SU.Log(e);}
										inf.end();
									break;
									case 3:
										SU.Zstd_decompress(_key_block_compressed_many, startI+8, compressedSize-8, key_block, 0, (int)(infoI.key_block_decompressed_size));
									break;
									case 4:
										SU.Lz4_decompress(_key_block_compressed_many, startI+8, key_block, 0, (int)(infoI.key_block_decompressed_size));
									break;
								}
								find_in_keyBlock(finalJoniregex, finalKeyPattern, key_block,infoI, finalMatcher,book,item, SearchLauncher);
							}
							_key_block_compressed_many=null;
						}
						catch (Exception e1) {
							SU.Log(e1);
							if(e1 instanceof SyntaxException){
								SearchLauncher.IsInterrupted =true;
								SearchLauncher.ErrorMessage=e1.getMessage();
							}
						}
						SearchLauncher.thread_number_count--;
						if(split_keys_thread_number>thread_number) SearchLauncher.poolEUSize.addAndGet(-1);
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
	
	@Override
	public String getResourcePaths() {
		StringBuilder ret = new StringBuilder();
		if (hasMdd()) {
			if(mdd!=null)
			for(mdictRes md:mdd) {
				ret.append(md.getPath()).append("\n");
			}
			if(ftd!=null)
			for(File fd:ftd) {
				ret.append(fd.getPath()).append("\n");
			}
		}
		return ret.toString();
	}
	
	@Override
	public byte[] getOptions() {
		return options;
	}
	
	@Override
	public void setOptions(byte[] options) {
		this.options = options;
	}
	
	@Override
	public int getType() {
		return 0;
	}
	
	protected ExecutorService OpenThreadPool(int thread_number) {
		if(parent!=null)
			return parent.OpenThreadPool(thread_number);
		return Executors.newFixedThreadPool(thread_number);
	}
	
	protected int flowerIndexOf(byte[] source, int sourceOffset, int sourceCount, byte[][][] matchers, int marcherOffest, int fromIndex, AbsAdvancedSearchLogicLayer launcher, F1ag flag, ArrayList<Object> mParallelKeys, int[] jumpMap)
	{
		int tagCheckFrame = 32;
		int tagSkipFrame = 32;
		int lastSeekLetSize=0;
		int totalLen = matchers.length;
		boolean bSearchInContents = launcher.type==SEARCHTYPE_SEARCHINTEXTS;
		boolean bSkipTags = bSearchInContents;
		boolean bCheckInTag = bSearchInContents;
		boolean trimStart = fromIndex==0 && (jumpMap[matchers.length]&1)!=0;
		int hudieling;
		if(true) {
			//checkEven=0;
			//bSkipTags=false;
			//bCheckInTag=false;
		}
//		boolean debug=false;
//		if(new String(source, sourceOffset, sourceCount, _charset).equals("democracy")) {
//			SU.Log("正在搜索"); debug=true;
//		}
		if (bSkipTags && false) {
			//todo skip html tags 三步，检查紧邻< ，搜索>，而后推进fromIndex_
			int fromIndex_=fromIndex;
			while(bingStartWith(source,sourceOffset,sourceCount,htmlOpenTag, 0,htmlOpenTag.length,fromIndex_)){
				int htmlForward = safeKalyxIndexOf(source, sourceOffset, Math.min(sourceCount, fromIndex_+htmlOpenTag.length+tagSkipFrame), htmlTagsB, fromIndex_+htmlOpenTag.length, flag);
				if(htmlForward>=fromIndex_+htmlOpenTag.length){
					fromIndex_=htmlForward+htmlCloseTag.length;
				} else break;
				while(
						bingStartWith(source, sourceOffset,sourceCount,spaceBytes, 0,spaceBytes.length,fromIndex_)
						||bingStartWith(source, sourceOffset,sourceCount,spaceBytes, 0,spaceBytes.length,fromIndex_)
				)
					fromIndex_+=spaceBytes.length;
			}
			//SU.Log("跳过了:", fromIndex, fromIndex_);
			fromIndex=fromIndex_;
		}
		int bInTagCheckNeeded=-1;
//		RBTree<MyIntPair> tagsMap = new RBTree<>();
//		for (int i = 0; i < sourceCount; i++) {
//			if (source[i]==htmlOpenTag[0]) {
//				//tagsMap.insert(new MyIntPair(i,0));
//			}
//			else if(source[i]==htmlCloseTag[0]) {
//				//tagsMap.insert(new MyIntPair(i,0));
//			}
//		}
		while(fromIndex<sourceCount) {
			//SU.Log("==");
			//int idx = -1;
			int fromIndex_=fromIndex;
			boolean isSeeking=!trimStart;
			boolean Matched = false;
			boolean pass; int len;
			int lexiPartIdx;
			for(lexiPartIdx=marcherOffest;lexiPartIdx<totalLen;lexiPartIdx++) {
				//if(fromIndex_>sourceCount-1) return -1;
				hudieling = jumpMap[lexiPartIdx];
//				if(debug) {
//					SU.Log("stst: " + sourceCount + "::" + (fromIndex_ /*+ seekPos*/) + " fromIndex_: " + fromIndex_/* + " seekPos: " + seekPos*/ + " lexiPartIdx: " + lexiPartIdx);
//					SU.Log("hudieling::"+hudieling, "isSeeking::"+isSeeking, /*"seekPos: " + seekPos + */" lexiPartIdx: " + lexiPartIdx + " fromIndex_: " + fromIndex_);
//				}
				if(hudieling>0) {
					if(lexiPartIdx==totalLen-1) {
						if(fromIndex_>=sourceCount)
							return -1;
						return fromIndex-lastSeekLetSize;//HERE
					}//Matched=true
					//SU.Log("miansi: "+lexiPartIdx);
					//SU.Log("miansi: "+sourceCount+"::"+(fromIndex_+seekPos)+"sourceL: "+source.length);
					//SU.Log("jumpped c is: "+new String(source,fromIndex_+seekPos,Math.min(4, sourceCount-(fromIndex_+seekPos-sourceOffset)),_encoding).substring(0, 1));
					int newSrcCount = Math.min(maxEB*hudieling, sourceCount-(fromIndex_));
					if(newSrcCount<=0) return -1;
					//TODO calc char length for different charsets.
					String c = new String(source,sourceOffset+fromIndex_,newSrcCount,_charset);
					if(hudieling>c.length()) return -1;
					int jumpShort = c.substring(0, hudieling).getBytes(_charset).length;
					fromIndex_+=jumpShort;
					continue;
				}
				else if(hudieling<0) {
					if(lexiPartIdx==totalLen-1)
						return fromIndex-lastSeekLetSize;//HERE
					int newSrcCount=sourceCount;
					hudieling*=-1;//todo 边界处理
					if(hudieling>1) newSrcCount=fromIndex_+hudieling-1;
					if(flowerIndexOf(source, sourceOffset,newSrcCount, matchers,lexiPartIdx+1, fromIndex_, launcher, flag, mParallelKeys, jumpMap)!=-1){
						return fromIndex-lastSeekLetSize;
					}
					return -1;
				}
				Matched = false;
				if(isSeeking) {
					int seekPos=-1;
					int	newSeekPos = kalyxIndexOf(source, sourceOffset, sourceCount, matchers[lexiPartIdx], fromIndex_, flag);
//					if(debug) SU.Log("newSeekPos", newSeekPos, checkEven);
					if(newSeekPos>=fromIndex_){
						//todo verify first match
						pass = true;
						lastSeekLetSize = matchers[lexiPartIdx][flag.val].length;
						if (checkEven != 0 ) {//zzz
							if((len = newSeekPos - fromIndex_) != 0) {
								if (checkEven == 2) {
									pass = len % 4 == 0;
								} else if (checkEven == 1 && len % 2 != 0) {
									pass = false;
								} else {
									if(encodeChecker !=null)
										pass = encodeChecker.checkBefore(source, sourceOffset, fromIndex_, newSeekPos);
									else {
										//len = sourceOffset + newSeekPos;
										int start;// = (checkEven == 3 || bSearchInContents) ? Math.max(sourceOffset + fromIndex_, sourceOffset + newSeekPos - maxEB) : (sourceOffset + fromIndex_);
										//start = (sourceOffset + fromIndex_) ;
										//int start = Math.max(sourceOffset+fromIndex_, sourceOffset+newSeekPos-4);
										for (int i = 1; i < maxEB; i++) {
											if (checkEven == 1 && i == 1/*二四变长编码*/
													|| checkEven != 5 && i == 3/*一二四变长编码*/) i++;
											if (newSeekPos - i < 0) break;
											start = sourceOffset + newSeekPos - i;
											//if (start > sourceOffset + fromIndex_) break;
											len = sourceOffset + newSeekPos - start;
											String validfyCode = new String(source, start, len, _charset);
											len = validfyCode.length();
											pass = len > 0 && (validfyCode.charAt(len - 1) != 65533);
											//SU.Log("validfyCode", validfyCode, validfyCode.charAt(len - 1) == 65533, pass);
											if (pass) break;
										}
										//}
										//								if(pass){
										//									validfyCode = new String(source, sourceOffset+newSeekPos+matchers[lexiPartIdx][flag.val].length, maxEB, _charset);
										//									//SU.Log("validfyCode", validfyCode);
										//									len = validfyCode.length();
										//									pass = len > 0 && validfyCode.charAt(0) != 65533;
										//								}
									}
								}
							}
//							else if((len = sourceCount - newSeekPos - matchers[lexiPartIdx][flag.val].length) != 0){
//								int start = newSeekPos+matchers[lexiPartIdx][flag.val].length;
//								len = Math.min(start+maxEB, len);
//								String validfyCode = new String(source, start, len, _charset);
//								SU.Log("validfyCode2", validfyCode, validfyCode.charAt(len - 1) == 65533);
//								pass = validfyCode.length() > 0 && validfyCode.charAt(0) != 65533;
//							}
						}
						if (pass) {
							if(bCheckInTag)
							if(true) // delay the check
								bInTagCheckNeeded=newSeekPos+lastSeekLetSize;
							else {
								// 标签内检查
								//todo skip html tags 检查不在<>之中。 前进找>，截止于<>两者。若找先到<则放行。
								//									若找先到>则需要进一步检查。 [36]
								//									后退找<，截止于<>两者。若找先到>则放行。[36]
								//									若找先到<则简单认为需要跳过。
								int from = newSeekPos+lastSeekLetSize;
								int htmlForward = safeKalyxIndexOf(source, sourceOffset, Math.min(sourceCount, from+tagCheckFrame), htmlTags, from, flag);
								if (htmlForward>=from) {
									if(flag.val==1){ // x >
										from = newSeekPos-1;
										int htmlBackward = safeKalyxLastIndexOf(source, sourceOffset, sourceOffset+Math.max(0, from-tagCheckFrame), htmlTags, from, flag);
										if(htmlBackward>=0){
											if(flag.val==0){ // < x
												fromIndex=fromIndex_=htmlForward+htmlTags[flag.val].length;
												lexiPartIdx--;
												continue;
											}
										}
									} else if(htmlForward==from){// 紧邻<
										while(bingStartWith(source,sourceOffset,sourceCount,htmlOpenTag, 0,htmlOpenTag.length,from)){
											from+=htmlOpenTag.length;
											htmlForward = safeKalyxIndexOf(source, sourceOffset, Math.min(sourceCount, from+tagCheckFrame), htmlTagsB, from, flag);
											if(htmlForward>=from+htmlCloseTag.length){
												from=htmlForward+htmlCloseTag.length;
												newSeekPos=from-lastSeekLetSize;
												//CMN.Log("跳过！");
											} else break;
										}
//										from = htmlForward+htmlTags[flag.val].length;
//										htmlForward = safeKalyxIndexOf(source, sourceOffset, from+36, htmlTagsB, from, flag);
//										if(htmlForward>=from){
//											newSeekPos = htmlForward+htmlTagsB[0].length-lastSeekLetSize;
//										}
									}
								}
							}
							seekPos = newSeekPos;
							Matched = true;
						}
						else {
							fromIndex=fromIndex_=newSeekPos + lastSeekLetSize;
							lexiPartIdx--;
							continue;
						}
					}
//					if(SU.debug) SU.Log("seekPos:"+seekPos+" fromIndex_: "+fromIndex_, Matched);
					if(!Matched)
						return -1;
					seekPos+=lastSeekLetSize;
					fromIndex=fromIndex_=seekPos;
					isSeeking=false;
					continue;
				}/* End seek */
				else {
//					if(debug)SU.Log("deadline", fromIndex_+" "+sourceCount);
					if(fromIndex_>sourceCount-1) {
//						if(debug)SU.Log("deadline reached"+fromIndex_+" "+sourceCount, new String(source, sourceOffset, sourceCount, _charset));
						return -1;
					}
//					if(debug) {
//						SU.Log("matchedHonestily? ", lexiPartIdx, mParallelKeys.get(lexiPartIdx));
//						SU.Log("matchedHonestily? str", new String(source, sourceOffset+fromIndex_, 100, _charset), bingStartWith(source,sourceOffset, sourceCount, matchers[lexiPartIdx][0],0,matchers[lexiPartIdx][0].length,fromIndex_));
//					}
					for(byte[] marchLet:matchers[lexiPartIdx]) {
						if(marchLet==null) break;
						if(bingStartWith(source,sourceOffset, sourceCount, marchLet,0,marchLet.length,fromIndex_)) {
							Matched=true;
							if(bInTagCheckNeeded>=0){
								//延后的标签内检查 delay the check
								int from = bInTagCheckNeeded;
								//todo skip html tags 检查不在<>之中。 前进找>，截止于<>两者。若找先到<则放行。
								//									若找先到>则需要进一步检查。 [36]
								//									后退找<，截止于<>两者。若找先到>则放行。[36]
								//									若找先到<则简单认为需要跳过。
								//int htmlForward = indexOf(source, sourceOffset, Math.min(sourceCount, from+tagCheckFrame), htmlCloseTag, 0, htmlCloseTag.length, from);
								int htmlForward = safeKalyxIndexOf(source, sourceOffset, Math.min(sourceCount, from+tagCheckFrame), htmlTags, from, flag);
								if (htmlForward>=from) {
									if(flag.val==1){ // x >
										from = bInTagCheckNeeded-1;
										//int htmlBackward = safeKalyxLastIndexOf(source, sourceOffset, sourceOffset+Math.max(0, from-tagCheckFrame), htmlTags, from, flag);
										int htmlBackward = safeKalyxLastIndexOf(source, sourceOffset, sourceOffset+Math.max(0, from-tagCheckFrame), htmlTags, from, flag);
										if(htmlBackward>=0){
											if(flag.val==0){ // < x
												fromIndex=fromIndex_=htmlForward+htmlTags[flag.val].length;
												Matched=false;
												bInTagCheckNeeded=0;
												break; // re-seek
											}
										}
									}
								}
								bInTagCheckNeeded=0;
							}
//							if(debug) {
//								SU.Log("matchedHonestily: ", sourceCount, "::", " fromIndex_: ", fromIndex_ + " seekPos: ");
//								SU.Log("matchedHonestily: ", lexiPartIdx, mParallelKeys.get(lexiPartIdx));
//							}
							fromIndex_+=marchLet.length;
							break;
						}
					}
					if(Matched && bSkipTags) {
						//todo skip html tags 三步，检查紧邻< ，搜索>，而后推进fromIndex_
						while(bingStartWith(source,sourceOffset,sourceCount,htmlOpenTag, 0,htmlOpenTag.length,fromIndex_)){
							int htmlForward = safeKalyxIndexOf(source, sourceOffset, Math.min(sourceCount, fromIndex_+htmlOpenTag.length+tagSkipFrame), htmlTagsB, fromIndex_+htmlOpenTag.length, flag);
							if(htmlForward>=fromIndex_+htmlOpenTag.length){
								fromIndex_=htmlForward+htmlCloseTag.length;
								//CMN.Log("跳过！");
							} else break;
						}
					}
				}/* End honest match */
				if(!Matched) {
					//SU.Log("Matched failed this round: "+lexiPartIdx);
					break;
				}
			}/* End lexical parts loop */
			if(Matched){
				if((jumpMap[matchers.length]&2)!=0&&fromIndex_<sourceCount)
					return -1;
				return fromIndex-lastSeekLetSize;
			}
			else if(trimStart)
				return -1;
		}
		return -1;
	}

	public static byte[][][][][] leafSanLieZhi(AbsAdvancedSearchLogicLayer searchLauncher, Charset _charset) {
		ArrayList<ArrayList<ArrayList<Object>>> pm = searchLauncher.mParallelKeys;
		byte[][][][][] res = new byte[pm.size()][][][][];
		ArrayList<String> item;
		for (int i = 0; i < pm.size(); i++) {
			ArrayList<ArrayList<Object>> andGroup = pm.get(i);
			res[i] = new byte[andGroup.size()][][][];
			for (int j = 0; j < andGroup.size(); j++) {
				ArrayList<Object> orGroup = andGroup.get(j);
				res[i][j] = new byte[orGroup.size()-1][][];
				for (int k = 0; k < orGroup.size()-1; k++) {
					if(orGroup.get(k) instanceof ArrayList){
						item = (ArrayList<String>)orGroup.get(k);
						int size = item.size();
						byte[][] lexipart = new byte[size][];
						for (int l = 0; l < size; l++) {
							lexipart[l] = item.get(l).getBytes(_charset);
						}
						res[i][j][k] = lexipart;
					}
				}
			}
		}
		return res;
	}

	protected void find_in_keyBlock(Regex JoniRegx, Pattern keyPattern, byte[] key_block, key_info_struct infoI, byte[][][][][] matcher, Object book, ArrayList<SearchResultBean> it, AbsAdvancedSearchLogicLayer SearchLauncher) {
		//org.joni.Matcher Jonimatcher = null;
		//if(JoniRegx!=null)
		//	Jonimatcher = JoniRegx.matcher(key_block);
		//!!spliting curr Key block
		int key_start_index = 0;
		//String delimiter;
		int key_end_index;
		//int keyCounter = 0;
		Flag flag = new Flag();
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
			key_end_index = key_start_index + _number_width + entryNumExt;

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

			//if(true)
			try {
				//TODO: alter
				//xxxx
//				int try_idx = -1;
//						JoniRegx==null?
//						flowerIndexOf(key_block,key_start_index+_number_width, key_end_index-(key_start_index+_number_width), matcher,0,0, SearchLauncher, flag, mParallelKeys)
//						:JoniRegx.matcher(key_block, key_start_index+_number_width, key_end_index).search(key_start_index+_number_width, key_end_index, Option.SINGLELINE)
//						;
//				try_idx=-1;

//				GlobalOptions.debug=(infoI.num_entries_accumulator+keyCounter)==34;
//				if(GlobalOptions.debug){
//				} else {key_start_index = key_end_index + delimiter_width; SearchLauncher.dirtyProgressCounter++; keyCounter++; continue;}

				ArrayList<ArrayList<Object>> mpk;
				ArrayList<Object> mParallelKeys;
				int try_idx = -1;
				if(JoniRegx==null) {
					for (int j = 0; j < matcher.length; j++) { // and group
						mpk = SearchLauncher.mParallelKeys.get(j);
						for (int k = 0; k < matcher[j].length; k++) { // or group
							mParallelKeys = mpk.get(k);
							int len = matcher[j][k].length;
							int[] jumpMap = (int[]) mParallelKeys.get(len);
							try_idx = flowerIndexOf(key_block, key_start_index+_number_width + entryNumExt, key_end_index-(key_start_index+_number_width + entryNumExt), matcher[j][k], 0, 0, SearchLauncher, flag, mParallelKeys, jumpMap);
							//SU.Log("and_group>>"+j, "or_group#"+k, try_idx);
							//BU.printBytes3(matcher[j][k]);
							if (try_idx < 0 ^ (jumpMap[len] & 4) == 0) break;
						}
						if (try_idx < 0) {
							break;
						}
					}
				} else {
					try_idx = JoniRegx.matcher(key_block, key_start_index+_number_width + entryNumExt, key_end_index).search(key_start_index+_number_width + entryNumExt, key_end_index, Option.SINGLELINE);
				}

				if(try_idx!=-1){
					//SU.Log("复核 re-collate");
					if(false)
					if (keyPattern != null){
						String LexicalEntry = new String(key_block, key_start_index + _number_width + entryNumExt, key_end_index - (key_start_index + _number_width + entryNumExt), _charset);
						//SU.Log("checking ", LexicalEntry);
						if (!keyPattern.matcher(LexicalEntry).find()) {
							SU.Log("发现错匹！！！", LexicalEntry, _Dictionary_fName);
//							key_start_index = key_end_index + delimiter_width;
//							SearchLauncher.dirtyProgressCounter++;
//							keyCounter++;
//							continue;
						}
					}
					//StringBuilder sb = new StringBuilder(LexicalEntry);
					//byte[] arraytmp = new byte[key_end_index-(key_start_index+_number_width)];
					//System.arraycopy(key_block,key_start_index+_number_width, arraytmp, 0,arraytmp.length);
					//additiveMyCpr1 tmpnode = new additiveMyCpr1(LexicalEntry,""+SelfAtIdx+""+((int) (infoI.num_entries_accumulator+keyCounter)));//new ArrayList<Integer>() new int[] {SelfAtIdx,(int) (infoI.num_entries_accumulator+keyCounter)}
					//tmpnode.value.add(SelfAtIdx);
					//tmpnode.value.add((int) (infoI.num_entries_accumulator+keyCounter));
					it.add(new SearchResultBean(infoI.num_entries_accumulator+keyCounter));//new additiveMyCpr1(LexicalEntry,infoI.num_entries_accumulator+keyCounter));
					//SU.Log("fuzzyKeyCounter"+fuzzyKeyCounter);
					SearchLauncher.dirtyResultCounter++;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}


			key_start_index = key_end_index + delimiter_width;
			keyCounter++;SearchLauncher.dirtyProgressCounter++;
		}
		//assert(adler32 == (calcChecksum(key_block)));
	}

	byte[] key_block_cache = null;
	int key_block_cacheId=-1;
	int key_block_Splitted_flag=-1;
	int[][] scaler = null;

	public int reduceInBlock(String phrase,byte[] data,int[][] scaler,int start,int end) {//via mdict-js
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
					? reduceInBlock(phrase,data,scaler,start+len,end)
					: reduceInBlock(phrase,data,scaler,start,start+len);
		} else {
			return start;
		}
	}

	public int reduceInBlock(byte[] phrase,byte[] data,int[][] scaler,int start,int end) {//via mdict-js
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
					? reduceInBlock(phrase,data,scaler,start+len,end)
					: reduceInBlock(phrase,data,scaler,start,start+len);
		} else {
			return start;
		}
	}

	//快速联合搜索
	public int lookUpRangeQuick(int startIndex, String keyword, ArrayList<myCpr<String, Long>> rangReceiver, RBTree_additive treeBuilder, long SelfAtIdx, int theta, AtomicBoolean task, boolean strict) //多线程
	{
		long position = startIndex;
		if(position==-1 || position>=_num_entries) return 0;
		if(_key_block_info_list==null) read_key_block_info(null);
		int blockId = accumulation_blockId_tree.xxing(new myCpr<>(position,1)).getKey().value;
		key_info_struct infoI;
		
		int[][] scaler_ = null;
		byte[] key_block_cache_ = null;
		if(keyword==null) keyword = getEntryAt(position);
		keyword = processMyText(keyword);
		byte[] kAB = keyword.getBytes(_charset);
		
		boolean doHarvest=false;
		int results=0;
		//OUT:
		while(theta>0) {
			infoI = _key_block_info_list[blockId];
			try {
				long start = infoI.key_block_compressed_size_accumulator;
				long compressedSize;
				if(key_block_cacheId!=blockId || key_block_cache==null) {
					if(blockId==_key_block_info_list.length-1)
						compressedSize = _key_block_size - _key_block_info_list[_key_block_info_list.length-1].key_block_compressed_size_accumulator;
					else
						compressedSize = _key_block_info_list[blockId+1].key_block_compressed_size_accumulator-infoI.key_block_compressed_size_accumulator;
					
					DataInputStream data_in = getStreamAt(_key_block_offset+start, false);
					
					byte[]  _key_block_compressed = new byte[(int) compressedSize];
					data_in.read(_key_block_compressed, 0, _key_block_compressed.length);
					data_in.close();
					
					//int adler32 = getInt(_key_block_compressed[(int) (+4)],_key_block_compressed[(int) (+5)],_key_block_compressed[(int) (+6)],_key_block_compressed[(int) (+7)]);
					if(checkByteArray(_zero4, _key_block_compressed)){
						//System.out.println("no compress!");
						key_block_cache_ = new byte[(int) (_key_block_compressed.length-start-8)];
						System.arraycopy(_key_block_compressed, (int)(start+8), key_block_cache_, 0,key_block_cache_.length);
					}
					else if(checkByteArray(_1zero3, _key_block_compressed)) {
						//MInt len = new MInt((int) infoI.key_block_decompressed_size);
						//key_block_cache_ = new byte[len.v];
						//byte[] arraytmp = new byte[(int) compressedSize];
						//System.arraycopy(_key_block_compressed, (int)(+8), arraytmp, 0,(int) (compressedSize-8));
						//MiniLZO.lzo1x_decompress(arraytmp,arraytmp.length,key_block_cache_,len);
						key_block_cache_ =  new byte[(int) infoI.key_block_decompressed_size];
						new LzoDecompressor1x().decompress(_key_block_compressed, 8, (int)(compressedSize-8), key_block_cache_, 0, new lzo_uintp());
					}
					else if(checkByteArray(_2zero3, _key_block_compressed)) {
						//key_block_cache_ = zlib_decompress(_key_block_compressed,(int) (+8),(int)(compressedSize-8));
						key_block_cache_ =  new byte[(int) infoI.key_block_decompressed_size];
						Inflater inf = new Inflater();
						inf.setInput(_key_block_compressed,8,(int)compressedSize-8);
						int ret = inf.inflate(key_block_cache_,0,key_block_cache_.length);
						inf.end();
					}
					key_block_cache=key_block_cache_;
					key_block_cacheId = blockId;
				} else {
					key_block_cache_=key_block_cache;
				}
				/*!!spliting curr Key block*/
				if(key_block_Splitted_flag!=blockId || scaler==null) {
					if(!doHarvest)
						scaler_ = new int[(int) infoI.num_entries][2];
					int key_start_index = 0;
					int key_end_index;
					int keyCounter = 0;
					
					while(key_start_index < key_block_cache_.length){
						key_end_index = key_start_index + _number_width + entryNumExt;
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
							String kI = new String(key_block_cache_, key_start_index+_number_width + entryNumExt,key_end_index-(key_start_index+_number_width + entryNumExt), _charset);
							String proKey = processMyText(kI);
							if(proKey.startsWith(keyword) && (!strict || proKey.equals(keyword))) {
								long toAdd = keyCounter+infoI.num_entries_accumulator;
								for (int i = 0; i < rangReceiver.size(); i++) if(rangReceiver.get(i).value==toAdd) return results;
								if(treeBuilder !=null) treeBuilder.insert(kI, SelfAtIdx, toAdd);
								else rangReceiver.add(new myCpr<String, Long>(kI, toAdd));
								theta--;
								results++;
							} else return results;
							if(theta<=0) {
								if (results<1000 && proKey.equals(keyword)) {
									theta += 30;
								} else {
									return results;
								}
							}
						} else {
							scaler_[keyCounter][0] = key_start_index+_number_width + entryNumExt;
							scaler_[keyCounter][1] = key_end_index-(key_start_index+_number_width + entryNumExt);
						}
						
						key_start_index = key_end_index + delimiter_width;
						keyCounter++;
					}
					if(!doHarvest) {
						scaler=scaler_;
						key_block_Splitted_flag=blockId;
					}
				} else {
					scaler_=scaler;
				}
			} catch (Exception e2) {
				e2.printStackTrace();
			}
			
			if(!doHarvest) {
				int idx;
				
				if(_encoding.startsWith("GB")) idx = reduceInBlock(kAB, key_block_cache_, scaler_, 0, (int) infoI.num_entries);
				else idx = reduceInBlock(keyword, key_block_cache_, scaler_, 0, (int) infoI.num_entries);
				
				//SU.Log(new String(key_block_cache_, scaler_[idx][0],scaler_[idx][1], _charset));
				//SU.Log(new String(key_block_cache_, scaler_[idx+1][0],scaler_[idx+1][1], _charset));
				String kI = new String(key_block_cache_, scaler_[idx][0],scaler_[idx][1], _charset);
				while(true) {
					String proKey = processMyText(kI);
					if(proKey.startsWith(keyword) && (!strict || proKey.equals(keyword))) {
						long toAdd = idx+infoI.num_entries_accumulator;
						for (int i = 0; i < rangReceiver.size(); i++) if(rangReceiver.get(i).value==toAdd) return results;
						if(treeBuilder !=null) treeBuilder.insert(kI, SelfAtIdx, toAdd);
						else rangReceiver.add(new myCpr<>(kI, toAdd));
						theta--;
						results++;
					} else {
						if (results==0) {
							return -1*(int) ((infoI.num_entries_accumulator+idx+2));
						}
						return results;
					}
					idx++;
					//if(idx>=infoI.num_entries) SU.Log("nono!");
					if(theta<=0) { // Max limit reached.
						if (results<1000 && proKey.equals(keyword)) {
							theta += 30;
						} else {
							return results;
						}
					}
					if(idx>=infoI.num_entries) {
						break;
					}
					kI = new String(key_block_cache_, scaler_[idx][0],scaler_[idx][1], _charset);
				}
				doHarvest=true;
				// todo optimize the loop
			}
			++blockId;
			if(_key_block_info_list.length<=blockId) return results;
		}
		return results;
	}
	
	//联合搜索  555
	public int lookUpRange(String keyword, ArrayList<myCpr<String, Long>> rangReceiver, RBTree_additive treeBuilder, long SelfAtIdx, int theta, AtomicBoolean task, boolean strict) //多线程
	{
		if(virtualIndex!=null) {
			return virtualIndex.lookUpRange(keyword, rangReceiver, treeBuilder, SelfAtIdx, theta, task, false);
		}
		int[][] scaler_ = null;
		byte[] key_block_cache_ = null;
		keyword = processMyText(keyword);
		byte[] kAB = keyword.getBytes(_charset);
		if(_encoding.startsWith("GB")) {
			if(compareByteArray(_key_block_info_list[(int)_num_key_blocks-1].tailerKeyText,kAB)<0)
				return 0;
			if(compareByteArray(_key_block_info_list[0].headerKeyText,kAB)>0)
				return 0;
		} else {
			if((TailerTextStr==null? TailerTextStr =processMyText(new String(_key_block_info_list[(int)_num_key_blocks-1].tailerKeyText,_charset).toLowerCase()):TailerTextStr).compareTo(keyword)<0) {
				return 0;
			}
			if((HeaderTextStr==null? HeaderTextStr=processMyText(new String(_key_block_info_list[0].headerKeyText,_charset).toLowerCase()):HeaderTextStr).compareTo(keyword)>0) {
				if(!HeaderTextStr.startsWith(keyword))
					return 0;
			}
		}
		if(_key_block_info_list==null) read_key_block_info(null);
		int blockId;
		if(_encoding.startsWith("GB")) blockId = reduce_index2(kAB,0,_key_block_info_list.length);
		else if(isKeyCaseSensitive) blockId = reduce_index_with_keycase(keyword,0,_key_block_info_list.length);
		else blockId = reduce_index(keyword,0,_key_block_info_list.length);
		
		if(blockId==-1) return 0;
		//show(_Dictionary_fName+_key_block_info_list[blockId].tailerKeyText+"1~"+_key_block_info_list[blockId].headerKeyText);
		//while(blockId!=0 &&  compareByteArray(_key_block_info_list[blockId-1].tailerKeyText,kAB)>=0)
		//	blockId--;
		
		boolean doHarvest=false;
		int results=0;
		//OUT:
		while(theta>0) {
			key_info_struct infoI = _key_block_info_list[blockId];
			try {
				long start = infoI.key_block_compressed_size_accumulator;
				long compressedSize;
				if(key_block_cacheId!=blockId || key_block_cache==null) {
					if(blockId==_key_block_info_list.length-1)
						compressedSize = _key_block_size - _key_block_info_list[_key_block_info_list.length-1].key_block_compressed_size_accumulator;
					else
						compressedSize = _key_block_info_list[blockId+1].key_block_compressed_size_accumulator-infoI.key_block_compressed_size_accumulator;

					DataInputStream data_in = getStreamAt(_key_block_offset+start, false);

					byte[]  _key_block_compressed = new byte[(int) compressedSize];
					data_in.read(_key_block_compressed, 0, _key_block_compressed.length);
					data_in.close();

					//int adler32 = getInt(_key_block_compressed[(int) (+4)],_key_block_compressed[(int) (+5)],_key_block_compressed[(int) (+6)],_key_block_compressed[(int) (+7)]);
					if(checkByteArray(_zero4, _key_block_compressed)){
						//System.out.println("no compress!");
						key_block_cache_ = new byte[(int) (_key_block_compressed.length-start-8)];
						System.arraycopy(_key_block_compressed, (int)(start+8), key_block_cache_, 0,key_block_cache_.length);
					}
					else if(checkByteArray(_1zero3, _key_block_compressed)) {
						//MInt len = new MInt((int) infoI.key_block_decompressed_size);
						//key_block_cache_ = new byte[len.v];
						//byte[] arraytmp = new byte[(int) compressedSize];
						//System.arraycopy(_key_block_compressed, (int)(+8), arraytmp, 0,(int) (compressedSize-8));
						//MiniLZO.lzo1x_decompress(arraytmp,arraytmp.length,key_block_cache_,len);
						key_block_cache_ =  new byte[(int) infoI.key_block_decompressed_size];
						new LzoDecompressor1x().decompress(_key_block_compressed, 8, (int)(compressedSize-8), key_block_cache_, 0, new lzo_uintp());
					}
					else if(checkByteArray(_2zero3, _key_block_compressed)) {
						//key_block_cache_ = zlib_decompress(_key_block_compressed,(int) (+8),(int)(compressedSize-8));
						key_block_cache_ =  new byte[(int) infoI.key_block_decompressed_size];
						Inflater inf = new Inflater();
						inf.setInput(_key_block_compressed,8,(int)compressedSize-8);
						int ret = inf.inflate(key_block_cache_,0,key_block_cache_.length);
						inf.end();
					}
					key_block_cache=key_block_cache_;
					key_block_cacheId = blockId;
				} else {
					key_block_cache_=key_block_cache;
				}
				/*!!spliting curr Key block*/
				if(key_block_Splitted_flag!=blockId || scaler==null) {
					if(!doHarvest)
						scaler_ = new int[(int) infoI.num_entries][2];
					int key_start_index = 0;
					int key_end_index;
					int keyCounter = 0;

					while(key_start_index < key_block_cache_.length){
						key_end_index = key_start_index + _number_width + entryNumExt;
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
							String kI = new String(key_block_cache_, key_start_index+_number_width + entryNumExt,key_end_index-(key_start_index+_number_width + entryNumExt), _charset);
							String proKey = processMyText(kI);
							if(proKey.startsWith(keyword) && (!strict || proKey.equals(keyword))) {
								if(treeBuilder !=null)
									treeBuilder.insert(kI, SelfAtIdx, keyCounter+infoI.num_entries_accumulator);
								else
									rangReceiver.add(new myCpr<String, Long>(kI, keyCounter+infoI.num_entries_accumulator));
								theta--;
								results++;
							} else return results;
							if(theta<=0) {
								if (results<1000 && proKey.equals(keyword)) {
									theta += 30;
								} else {
									return results;
								}
							}
						} else {
							scaler_[keyCounter][0] = key_start_index+_number_width + entryNumExt;
							scaler_[keyCounter][1] = key_end_index-(key_start_index+_number_width + entryNumExt);
						}

						key_start_index = key_end_index + delimiter_width;
						keyCounter++;
					}
					if(!doHarvest) {
						scaler=scaler_;
						key_block_Splitted_flag=blockId;
					}
				} else {
					scaler_=scaler;
				}
			} catch (Exception e2) {
				e2.printStackTrace();
			}

			if(!doHarvest) {
				int idx;
				
				if(_encoding.startsWith("GB")) idx = reduceInBlock(kAB, key_block_cache_, scaler_, 0, (int) infoI.num_entries);
				else idx = reduceInBlock(keyword, key_block_cache_, scaler_, 0, (int) infoI.num_entries);
				
				//SU.Log(new String(key_block_cache_, scaler_[idx][0],scaler_[idx][1], _charset));
				//SU.Log(new String(key_block_cache_, scaler_[idx+1][0],scaler_[idx+1][1], _charset));
				String kI = new String(key_block_cache_, scaler_[idx][0],scaler_[idx][1], _charset);
				while(true) {
					String proKey = processMyText(kI);
					if(proKey.startsWith(keyword) && (!strict || proKey.equals(keyword))) {
						if(treeBuilder !=null)
							treeBuilder.insert(kI, SelfAtIdx, idx+infoI.num_entries_accumulator);
						else
							rangReceiver.add(new myCpr<>(kI, idx+infoI.num_entries_accumulator));
						theta--;
						results++;
					} else {
						if (results==0) {
							return -1*(int) ((infoI.num_entries_accumulator+idx+2));
						}
						return results;
					}
					idx++;
					//if(idx>=infoI.num_entries) SU.Log("nono!");
					if(theta<=0) { // Max limit reached.
						if (results<1000 && proKey.equals(keyword)) {
							theta += 30;
						} else {
							return results;
						}
					}
					if(idx>=infoI.num_entries) {
						break;
					}
					kI = new String(key_block_cache_, scaler_[idx][0],scaler_[idx][1], _charset);
				}
				doHarvest=true;
				// todo optimize the loop
			}
			++blockId;
			if(_key_block_info_list.length<=blockId) return results;
		}
		return results;
	}

	public String processKey(byte[] in){
		return processMyText(new String(in,_charset));
	}

	public static int binary_find_closest(long[] array,long val){
		int middle;
		int iLen ;
		if(array==null || (iLen=array.length)<1){
			return -1;
		}
		int low=0,high=iLen-1;
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


	public String getAboutString() {
		return _header_tag.get("Description");
	}
	
	public String getField(String fieldName) {
		return _header_tag.get(fieldName);
	}
	
	public String getDictInfo(){
		DecimalFormat numbermachine = new DecimalFormat("#.00");
		return new StringBuilder()
				.append("Engine Version: ").append(_version).append("<BR>")
				.append("CreationDate: ").append(_header_tag.get("CreationDate")).append("<BR>")
				.append("Charset &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; : ").append(this._encoding).append("<BR>")
				.append("Num Entries: ").append(_num_entries).append("<BR>")
				.append("Num Key Blocks: ").append(_num_key_blocks).append("<BR>")
				.append("Num Rec Blocks: ").append(decode_record_block_size(null)).append("<BR>")
				.append("Avg. Key Block: ").append(numbermachine.format(1.0*_key_block_size/_num_key_blocks/1024)).append(" kb, ").append(numbermachine.format(1.0*_num_entries/_num_key_blocks)).append(" items / block").append("<BR>")
				.append("Avg. Rec Block: ").append(numbermachine.format(1.0*_record_block_size/_num_record_blocks/1024)).append(" kb, ").append(numbermachine.format(1.0*_num_entries/_num_record_blocks)).append(" items / block").append("<BR>")
				.append("Compact  排序: ").append(isCompact).append("<BR>")
				.append("StripKey 排序: ").append(isStripKey).append("<BR>")
				.append("Ignore KeyCase v1: ").append(!isKeyCaseSensitive).append("<BR>")
				.append("Ignore KeyCase v2: ").append(isKeyCaseInsensitive).append("<BR>")
				.append("Ignore 变音符号: ").append(isDiacriticsInsensitive).append("<BR>")
				//.append(mdd==null?"&lt;no assiciated mdRes&gt;":("MdResource count "+mdd.getNumberEntries()+","+mdd._encoding+","+mdd._num_key_blocks+","+mdd._num_record_blocks)).append("<BR>")
				.append("书籍名称: ").append(_header_tag.get("Title")).append("<BR>")
				.append("Path: ").append(getPath()).toString();
	}

	public static boolean bingStartWith(byte[] source, int sourceOffset, int sourceCount,byte[] target, int targetOffset, int targetCount, int fromIndex) {
		if (fromIndex >= sourceCount || targetCount+fromIndex > sourceCount) { // || targetCount+fromIndex>=sourceCount || fromIndex>=sourceCount
			return false;
		}
		//if(targetCount<=-1)
		//	targetCount=target.length;
		//if(sourceOffset+targetCount>=source.length)
		//	return false;
		int i = sourceOffset+fromIndex;
		int v1 = i+targetCount-1, v2=targetOffset-i;
		for (; i <= v1 && source[i] == target[i+v2]; i++);
		return i==v1+1;
	}

	public String processMyText(String input) {
		String ret;
		try {
			ret = isStripKey?replaceReg.matcher(input).replaceAll(emptyStr):input;
		} catch (StackOverflowError e) {
			error_input = input;
			ret = input;
		}
		if(isDiacriticsInsensitive) {
			ret = SU.removeDiacritics(ret);
		}
		//return ret.toLowerCase();
		return isKeyCaseSensitive?ret:isKeyCaseInsensitive?ret.toLowerCase():AngloToLowerCase(ret);
	}

	/** This is the old way. */
	public static String AngloToLowerCase(String input) {
		StringBuilder sb = new StringBuilder(input);
		for(int i=0;i<sb.length();i++) {
			if(sb.charAt(i)>='A' && sb.charAt(i)<='Z')
				sb.setCharAt(i, (char) (sb.charAt(i)+32));
		}
		return sb.toString();
	}

	public String processStyleSheet(String input, long pos) {
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
				if("0".equals(now)) {
					nowArr=new String[] {getCachedEntryAt(pos),""}; // todo check mt
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
			last = nowArr.length==2?StringEscapeUtils.unescapeHtml3(nowArr[1]):"";
			returnRaw=false;
		}
		if(returnRaw)
			return input;
		else
			return transcriptor.append(last==null?"":last).append(input.substring(lastEnd)).toString();
	}

	@Override
	public String toString() {
		return _Dictionary_fName+"("+hashCode()+")";
	}

	public void Rebase(File newPath) {
		if(!f.equals(newPath)) {
			String OldFName = _Dictionary_fName;
			f = newPath;
			_Dictionary_fName = f.getName();
			HashSet<String> mddCon = new HashSet<>();
			if(mdd!=null) {
				for (mdictRes md : mdd) {
					//todo file access
					MoveOrRenameResourceLet(md, OldFName,_Dictionary_fName, newPath);
					mddCon.add(md.getPath());
				}
			}
			try {
				loadInResourcesFiles(mddCon);
			} catch (IOException ignored) {  }
		}
	}

	protected void MoveOrRenameResourceLet(mdictRes md, String token, String pattern, File newPath) {
		File f = md.f();
		String tokee = f().getName();
		if(tokee.startsWith(token) && tokee.charAt(Math.min(token.length(), tokee.length()))=='.'){
			String suffix = tokee.substring(token.length());
			String np = f.getParent();
			File mnp;
			if(np!=null && np.equals(np=newPath.getParent())){ //重命名
				mnp=new File(np, pattern+suffix);
			} else {
				mnp=new File(np, f.getName());
			}
			if(mnp!=null && f.renameTo(mnp)){
				md.Rebase(mnp);
			}
		}
	}
	
	public StringBuilder AcquireStringBuffer(int capacity) {
		StringBuilder sb;
		if(univeral_buffer != null && isMainThread()){
			sb = univeral_buffer;
			sb.ensureCapacity(capacity);
			sb.setLength(0);
			//SU.Log("复用字符串构建器……");
		} else {
			sb = new StringBuilder(capacity);
		}
		return sb;
	}
	
	protected boolean isMainThread() {
		return false;
	}
	
	private void loadInResourcesFiles(HashSet<String> mddCon) throws IOException {
		if(!isResourceFile){
			File p=f.getParentFile();
			if(p!=null && _num_record_blocks>=0) {
				String full_Dictionary_fName = _Dictionary_fName;
				StringBuilder sb = AcquireStringBuffer(full_Dictionary_fName.length()+15);
				int idx = full_Dictionary_fName.lastIndexOf(".");
				if(idx!=-1) {
					sb.append(full_Dictionary_fName, 0, idx);
				} else {
					sb.append(full_Dictionary_fName);
				}
				int base_full_name_L = sb.length();
				File f2 = new File(p, sb.append(".0.txt").toString());
				if(f2.exists()){
					fZero_LPT = 0;
					fZero=f2;
					ftd = new ArrayList<>();
					handleDebugLines();
				}
				sb.setLength(base_full_name_L);
				f2 = new File(p, sb.append(".mdd").toString());
				if (f2.exists() && (mddCon==null||!mddCon.contains(f2.getPath()))) {
					mdd = new ArrayList<>();
					mdd.add(new mdictRes(f2));
					int cc = 1;
					sb.setLength(base_full_name_L);
					while ((f2 = new File(p, sb.append(".").append(cc++).append(".mdd").toString())).exists()) {
						sb.setLength(base_full_name_L);
						if(mddCon==null||!mddCon.contains(f2.getPath()))
							mdd.add(new mdictRes(f2));
					}
				}
				//if(_header_tag.containsKey("SharedMdd")) {
				//}
			}
		}
	}

	/* watch debug definition file (dictname.0.txt) */
	public void handleDebugLines() {
		if(ftd!=null) {
			long _fZero_LPT = fZero.lastModified();
			if(_fZero_LPT!=fZero_LPT) {
				ftd.clear();
				try {
					BufferedReader br = new BufferedReader(new FileReader(fZero));
					String line;
					while((line=br.readLine())!=null){
						handleDebugLines(line.trim());
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				fZero_LPT = _fZero_LPT;
			}
		}
	}

	@Override
	protected void postGetCharset() {
		htmlOpenTag = "<".getBytes(_charset);
		htmlCloseTag = ">".getBytes(_charset);
		htmlTags = new byte[][]{htmlOpenTag, htmlCloseTag};
		htmlTagsA = new byte[][]{htmlOpenTag};
		htmlTagsB = new byte[][]{htmlCloseTag};
		spaceBytes = " ".getBytes(_charset);
		switch (_charset.name()){
			case "EUC-JP":
				checkEven=5;
				maxEB = 3;
			break;
			case "EUC-KR":
			case "x-EUC-TW":
			case "Shift_JIS":
			case "Windows-31j":
				checkEven=3;
				maxEB = 2;
			break;
			case "GB2312"://1981 unsafe double bytes
			case "GBK"://1995 unsafe double bytes
				checkEven=4;
				maxEB = 2;
			break;
			case "GB18030"://2000 unsafe double bytes
				checkEven=4;
				maxEB = 4;
				encodeChecker = new EncodeChecker();
			break;
			case "UTF-16BE":
			case "UTF-16LE":
				checkEven=1;
				maxEB = 4;
			break;
			case "UTF-32BE":
			case "UTF-32LE":
				checkEven=2;
				maxEB = 4;
			break;
			case "Big5":// safe double bytes?
			case "Big5-HKSCS":// safe double bytes?
				checkEven=3;
			break;
			case "UTF-8":// safe tripple bytes?
				//checkEven=3;
				maxEB = 4;
			break;
			default:
				maxEB = 1;
			break;
		}
	}

	public static class EncodeChecker{
		public boolean checkBefore(byte[] source, int sourceOffset, int fromIndex_, int ret) {
			try {
				int code = source[sourceOffset + ret - 1]&0xff;
				if(code<=0x7F){//1
					return true;
				}

				if(ret-2>=0)
				if((code>=0x40&&code<=0xFE&&code!=0x7F)){//2
					code = source[sourceOffset + ret - 2]&0xff;//1
					if((code>=0x81&&code<=0xFE)){
						return true;
					}
				}

				if(ret-4>=0)
				if((code>=0x30&&code<=0x39)) {//4
					code = source[sourceOffset + ret - 2]&0xff;
					if ((code >= 0x81 && code <= 0xFE)) {//3
						code = source[sourceOffset + ret - 3]&0xff;
						if ((code >= 0x30 && code <= 0x39)) {//2
							code = source[sourceOffset + ret - 4]&0xff;
							if ((code >= 0x81 && code <= 0xFE)) {//1
								return true;
							}
						}
					}
				}
			} catch (Exception e) {
				SU.Log(e);
			}
			return false;
		}

		public boolean checkAfter(byte[] source, int sourceOffset, int toIndex, int ret) {
			try {
				int code = source[sourceOffset + ret]&0xff;
				if(code<=0x7F){//1
					return true;
				}

				if(ret + 1<=toIndex)
				if((code>=0x40&&code<=0xFE&&code!=0x7F)){//2
					code = source[sourceOffset + ret + 1]&0xff;//1
					if((code>=0x81&&code<=0xFE)){
						return true;
					}
				}

				if(ret + 3<=toIndex)
				if ((code >= 0x81 && code <= 0xFE)) {//1
					code = source[sourceOffset + ret + 1]&0xff;
					if ((code >= 0x30 && code <= 0x39)) {//2
						code = source[sourceOffset + ret + 2]&0xff;
						if ((code >= 0x81 && code <= 0xFE)) {//3
							code = source[sourceOffset + ret + 3]&0xff;
							if((code>=0x30&&code<=0x39)) {//4
								return true;
							}
						}
					}
				}
			} catch (Exception e) {
				SU.Log(e);
			}
			return false;
		}
	}
	
	@Override
	public void saveConfigs(Object book) {
	
	}
	
	@Override
	public int guessRootWord(UniversalDictionaryInterface d, String keyword){
		return -1;
	}
	
//	@Override
//	public void onPageFinished(BookPresenter invoker, WebViewmy mWebView, String url, boolean b) {
//	
//	}
}



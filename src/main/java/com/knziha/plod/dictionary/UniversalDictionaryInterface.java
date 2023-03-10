package com.knziha.plod.dictionary;

import com.knziha.plod.dictionary.Utils.Flag;
import com.knziha.plod.dictionary.Utils.myCpr;
import com.knziha.plod.dictionarymodels.BookPresenter;
import com.knziha.rbtree.RBTree_additive;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public interface UniversalDictionaryInterface {
	String getEntryAt(long position, Flag mflag);
	String getEntryAt(long position);
	long getNumberEntries();
	
	String getRecordAt(long position, GetRecordAtInterceptor getRecordAtInterceptor, boolean allowJump) throws IOException;
	String getRecordsAt(GetRecordAtInterceptor getRecordAtInterceptor, long... positions) throws IOException;
	byte[] getRecordData(int position) throws IOException;
	void setCaseStrategy(int val);
	
	File getFile();
	String getDictionaryName();
	boolean hasVirtualIndex();
	//void onPageFinished(BookPresenter invoker, WebViewmy mWebView, String url, boolean b);
	
	
	StringBuilder AcquireStringBuffer(int capacity);
	
	boolean hasMdd();
	
	String getRichDescription();
	String getDictInfo();
	
	boolean getIsResourceFile();
	
	Object[] getSoundResourceByName(String canonicalName) throws IOException;
	
	String getCharsetName();
	
	void Reload(Object context);
	
	int lookUp(String keyword, boolean isSrict, List<UniversalDictionaryInterface> morphogen);
	
	int lookUp(String keyword,boolean isSrict);
	
	int lookUp(String keyword);
	
	int guessRootWord(UniversalDictionaryInterface d, String keyword);
	
	/** @return num matched. 0=noting. >0 is count. <0 is closest */
	int lookUpRange(String keyword, ArrayList<myCpr<String, Long>> rangReceiver, RBTree_additive treeBuilder, long SelfAtIdx, int theta, AtomicBoolean task, boolean strict);
	
	int lookUpRangeQuick(int startIndex, String keyword, ArrayList<myCpr<String, Long>> rangReceiver, RBTree_additive treeBuilder, long SelfAtIdx, int theta, AtomicBoolean task, boolean strict);
	
	InputStream getResourceByKey(String key);
	
	Object ReRoute(String key) throws IOException;
	
	String getVirtualRecordAt(Object presenter, long vi) throws IOException;
	
	String getVirtualRecordsAt(Object presenter, long[] positions) throws IOException;
	
//	String getVirtualTextValidateJs(Object presenter, WebViewmy mWebView, long position);
	
	String getVirtualTextEffectJs(Object presenter, long[] positions);
	
	long getBooKID();
	
	void setBooKID(long id);
	
	void flowerFindAllContents(String key, Object book, mdict.AbsAdvancedSearchLogicLayer SearchLauncher) throws IOException;
	void flowerFindAllKeys(String key, Object book, mdict.AbsAdvancedSearchLogicLayer SearchLauncher) throws IOException;
	
	String getResourcePaths();
	
	byte[] getOptions();
	void setOptions(byte[] options);
	int getType();
	
	long getEntryExtNumber(long position, int index);
	
	String getField(String fieldName);
	
	void setPerThreadKeysCaching(ConcurrentHashMap<Long, Object> keyBlockOnThreads);
	
	interface DoForAllRecords{
		void doit(Object parm, Object tParm, String entry, long position, String text, byte[] data, int from, int len, Charset _charset);
		Object onThreadSt(Object parm);
		void onThreadEd(Object parm);
	}
	
	void doForAllRecords(Object book, mdict.AbsAdvancedSearchLogicLayer SearchLauncher, DoForAllRecords dor, Object parm) throws IOException;
	
	InputStream getRecordStream(int idx) throws IOException;
	
	void saveConfigs(Object book);
}

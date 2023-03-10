package com.knziha.plod.dictionarymodels;

import com.knziha.plod.dictionary.GetRecordAtInterceptor;
import com.knziha.plod.dictionary.UniversalDictionaryInterface;
import com.knziha.plod.dictionary.Utils.F1ag;
import com.knziha.plod.dictionary.Utils.Flag;
import com.knziha.plod.dictionary.Utils.myCpr;
import com.knziha.plod.dictionary.mdict;
import com.knziha.plod.plaindict.MainActivityUIBase;
import com.knziha.plod.plaindict.PDICMainAppOptions;
import com.knziha.rbtree.RBTree_additive;

import org.jcodings.Encoding;
import org.joni.Option;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.knziha.plod.dictionary.mdict.kalyxIndexOf;
import static com.knziha.plod.dictionary.mdict.kalyxLastIndexOf;

public class DictionaryAdapter implements UniversalDictionaryInterface {
	public enum PLAIN_BOOK_TYPE
	{
		PLAIN_TYPE_TEXT
		,PLAIN_TYPE_WEB
		,PLAIN_TYPE_MDICT
		,PLAIN_TYPE_PDF
		,PLAIN_TYPE_DSL
		,PLAIN_TYPE_EMPTY
	}
	File f;
	boolean fExist;
	long _bid;
	long _num_entries;
	long _num_record_blocks;
	String _Dictionary_fName;
	PDICMainAppOptions opt;
	Charset _charset;
	Encoding encoding;
	public volatile boolean searchCancled;
	
	PLAIN_BOOK_TYPE mType;
	byte[] options;
	
	/** validation schema<br/>
	 * 0=none; 1=check even; 2=check four; 3=check direct; 4=check direct for all; 5=1/3; */
	protected int checkEven;
	protected int maxEB;
	
	public String htmlOpenTagStr="<";
	public String htmlCloseTagStr=">";
	public byte[] htmlOpenTag;
	public byte[] htmlCloseTag;
	public byte[][] htmlTags;
	public byte[][] htmlTagsA;
	public byte[][] htmlTagsB;
//	private mdict.EncodeChecker encodeChecker;
	
	protected int split_recs_thread_number;
	
	protected boolean hasResources;
	
	
	public DictionaryAdapter(File fn, MainActivityUIBase _a) {
		f = fn;
		_Dictionary_fName=fn.getName();
		if (_a!=null) {
			opt=_a.opt;
		}
		mType = PLAIN_BOOK_TYPE.PLAIN_TYPE_EMPTY;
	}
	
	protected DictionaryAdapter() {
	}
	
	@Override
	public String getEntryAt(long position, Flag mflag) {
		return "";
	}
	
	@Override
	public String getEntryAt(long position) {
		return "";
	}
	
	@Override
	public long getNumberEntries() {
		return _num_entries;
	}
	
	@Override
	public String getRecordAt(long position, GetRecordAtInterceptor getRecordAtInterceptor, boolean allowJump) throws IOException {
		return null;
	}
	
	@Override
	public String getRecordsAt(GetRecordAtInterceptor getRecordAtInterceptor, long... positions) throws IOException {
		if(getRecordAtInterceptor!=null)
		{
			String ret = getRecordAtInterceptor.getRecordAt(this, positions[0]);
			if (ret!=null) {
				return ret;
			}
		}
		return getRecordAt(positions[0], null, true);
	}
	
	@Override
	public byte[] getRecordData(int position) throws IOException {
		return null;
	}
	
	@Override
	public void setCaseStrategy(int val) {
	
	}
	
	@Override
	public File getFile() {
		return f;
	}
	
	@Override
	public String getDictionaryName() {
		return _Dictionary_fName;
	}
	
	@Override
	public boolean hasVirtualIndex() {
		return false;
	}
	
	@Override
	public StringBuilder AcquireStringBuffer(int capacity) {
		return new StringBuilder(capacity);
	}
	
	@Override
	public boolean hasMdd() {
		return hasResources;
	}
	
	@Override
	public String getRichDescription() {
		return "";
	}
	
	@Override
	public String getDictInfo() {
		return "";
	}
	
	@Override
	public boolean getIsResourceFile() {
		return false;
	}
	
	@Override
	public Object[] getSoundResourceByName(String canonicalName) throws IOException {
		return null;
	}
	
	@Override
	public String getCharsetName() {
		return _charset==null?null:_charset.name();
	}
	
	@Override
	public void Reload(Object context) {
	
	}
	
	@Override
	public int lookUp(String keyword, boolean isSrict, List<UniversalDictionaryInterface> morphogen) {
		return lookUp(keyword, isSrict);
	}
	
	@Override
	public int lookUp(String keyword, boolean isSrict) {
		return -1;
	}
	
	@Override
	public int lookUp(String keyword) {
		return lookUp(keyword, false);
	}
	
	@Override
	public int guessRootWord(UniversalDictionaryInterface d, String keyword){
		return -1;
	}
	
	@Override
	public int lookUpRange(String keyword, ArrayList<myCpr<String, Long>> rangReceiver, RBTree_additive treeBuilder, long SelfAtIdx, int theta, AtomicBoolean task, boolean strict) {
		return 0;
	}
	
	@Override
	public int lookUpRangeQuick(int startIndex, String keyword, ArrayList<myCpr<String, Long>> rangReceiver, RBTree_additive treeBuilder, long SelfAtIdx, int theta, AtomicBoolean task, boolean strict) {
		return 0;
	}
	
	@Override
	public InputStream getResourceByKey(String key) {
		return null;
	}
	
	@Override
	public Object ReRoute(String key) throws IOException {
		return null;
	}
	
	@Override
	public String getVirtualRecordAt(Object presenter, long vi) throws IOException {
		return null;
	}
	
	@Override
	public String getVirtualRecordsAt(Object presenter, long[] args) throws IOException {
		return getVirtualRecordAt(presenter, args[0]);
	}
	
//	@Override
//	public String getVirtualTextValidateJs(Object presenter, WebViewmy mWebView, long position) {
//		return "";
//	}
	
	@Override
	public String getVirtualTextEffectJs(Object presenter, long[] positions) {
		return null;
	}
	
	@Override
	public long getBooKID() {
		return _bid;
	}
	
	@Override
	public void setBooKID(long id) {
		_bid = id;
	}
	
	@Override
	public void flowerFindAllContents(String key, Object book, mdict.AbsAdvancedSearchLogicLayer SearchLauncher) throws IOException{
	
	}
	
	@Override
	public void flowerFindAllKeys(String key, Object book, mdict.AbsAdvancedSearchLogicLayer SearchLauncher) throws IOException{
	
	}
	
	@Override
	public String getResourcePaths() {
		return "";
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
		return mType.ordinal();
	}
	
	@Override
	public long getEntryExtNumber(long position, int index) {
		return 0;
	}
	
	@Override
	public String getField(String fieldName) {
		return null;
	}
	
	@Override
	public void setPerThreadKeysCaching(ConcurrentHashMap<Long, Object> keyBlockOnThreads) {
	
	}
	
	@Override
	public void doForAllRecords(Object book, mdict.AbsAdvancedSearchLogicLayer SearchLauncher, DoForAllRecords dor, Object parm) throws IOException {
		Object tParm = dor.onThreadSt(parm);
		searchCancled = false;
		for (int i = 0; i < _num_entries; i++) {
			if(SearchLauncher.IsInterrupted || searchCancled) break;
			String text = getRecordAt(i, null, false);
			dor.doit(parm, tParm, null, i, text, null, 0, 0, _charset);
			SearchLauncher.dirtyProgressCounter++;
		}
		dor.onThreadEd(parm);
	}
	
//	public boolean handlePageUtils(BookPresenter presenter, WebViewmy mWebView, int pos) {
//		return false;
//	}
	
	public String getLexicalEntryAt(int position) {
		return null;
	}
	
	protected void postGetCharset() {
		// stub
	}
	
	protected ExecutorService OpenThreadPool(int thread_number) {
		return Executors.newFixedThreadPool(thread_number);
		//return Executors.newCachedThreadPool();
		//return Executors.newScheduledThreadPool(thread_number);
		//return Executors.newWorkStealingPool();
	}
	
	protected int flowerIndexOf(byte[] source, int sourceOffset, int sourceCount, byte[][][] matchers, int marcherOffest, int fromIndex, mdict.AbsAdvancedSearchLogicLayer launcher, F1ag flag, ArrayList<Object> mParallelKeys, int[] jumpMap)
	{
		// stub
		return -1;
	}

	protected int getRegexOption(){
		return Option.IGNORECASE;
	}
	
	@Override
	public InputStream getRecordStream(int idx) throws IOException {
		return new ByteArrayInputStream(getRecordData(idx));
	}
	
	@Override
	public void saveConfigs(Object book) {
	
	}
	
//	@Override
//	public void onPageFinished(BookPresenter invoker, WebViewmy mWebView, String url, boolean b) {
//	
//	}
}

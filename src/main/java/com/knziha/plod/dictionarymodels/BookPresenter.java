package com.knziha.plod.dictionarymodels;

import com.alibaba.fastjson.JSONObject;
import com.knziha.plod.dictionary.UniversalDictionaryInterface;
import com.knziha.plod.dictionary.Utils.IU;
import com.knziha.plod.dictionary.Utils.SU;
import com.knziha.plod.plaindict.PDICMainAppOptions;
import com.knziha.plod.plaindict.PlainDictionary;
import edu.umd.cs.findbugs.annotations.NonNull;
import org.knziha.metaline.Metaline;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ConcurrentHashMap;

public class BookPresenter {
	public final static String baseUrl = "http://mdbr.com/base.html";
	public final String mBaseUrl;
	public final String idStr;
	public final String idStr10;
	public File placeHolder;

	long FFStamp;
	long firstFlag;
	byte firstVersionFlag;

	protected String searchKey;
	protected String lastSch;
	
	private final DictionaryAdapter.PLAIN_BOOK_TYPE mType;
	public /*final*/ UniversalDictionaryInterface bookImpl;

	public BookPresenter(PlainMdict mdtmp) {
		bookImpl = mdtmp;
		mType = DictionaryAdapter.PLAIN_BOOK_TYPE.PLAIN_TYPE_MDICT;
	}

	/** set by {@link PDICMainAppOptions#getAllowPlugRes} */
	public boolean isHasExtStyle() {
		return hasExtStyle;
	}

	private boolean hasExtStyle;


	/**几乎肯定是段落，不是单词或成语。**/
	public static boolean testIsParagraph(String searchText, int paragraphWords) {
		if (searchText.length()>15) {
			int words=0;
			int ppos=-1;
			char c;
			boolean white=false;
			for (int v = 0; v < searchText.length(); v++) {
				c=searchText.charAt(v);
				if(c<=' ') {
					if (v>ppos+1) words++;
					ppos=v;
					if(!white&&words>0) white=true;
				}
				if(c>=0x4e00&&c<=0x9fbb) {
					words++;
					if(!white) white=true;
				}
				if (words>=paragraphWords && white) {
					return true;
				}
			}
		}
		return false;
	}

	public int getCaseStrategy() {
		return (int) (firstFlag&3);
	}

	public void setCaseStrategy(int val) {
		firstFlag&=~3;
		firstFlag|=val;
		bookImpl.setCaseStrategy(val);
	}

	public boolean getIsolateImages(){
		//return (firstFlag & 0x2) != 0;
		return false;
	}

	public void setIsolateImages(boolean val){
		firstFlag&=~0x2;
		if(val) firstFlag|=0x2;
	}

	@Metaline(flagPos=6) public boolean getIsDedicatedFilter(){ firstFlag=firstFlag; throw new RuntimeException(); }
	@Metaline(flagPos=6) public void setIsDedicatedFilter(boolean value){ firstFlag=firstFlag; throw new RuntimeException(); }

	/** 内容可重载（检查数据库或文件系统中的重载内容） */
	@Metaline(flagPos=7) public boolean getContentEditable(){ firstFlag=firstFlag; throw new RuntimeException(); }
	@Metaline(flagPos=7) public void setContentEditable(boolean value){ firstFlag=firstFlag; throw new RuntimeException(); }
	/** 内容可编辑（处于编辑状态） */
	@Metaline(flagPos=8) public boolean getEditingContents(){ firstFlag=firstFlag; throw new RuntimeException(); }
	@Metaline(flagPos=8) public void setEditingContents(boolean value){ firstFlag=firstFlag; throw new RuntimeException(); }



	@Metaline(flagPos=5) public boolean getUseInternalBG(){ firstFlag=firstFlag; throw new RuntimeException(); }
	@Metaline(flagPos=5) public void setUseInternalBG(boolean value){ firstFlag=firstFlag; throw new RuntimeException(); }

	@Metaline(flagPos=4) public boolean getUseInternalFS(){ firstFlag=firstFlag; throw new RuntimeException(); }
	@Metaline(flagPos=4) public void setUseInternalFS(boolean val){ firstFlag=firstFlag; throw new RuntimeException(); }
	@Metaline(flagPos=9) public boolean getUseTitleBackground(){ firstFlag=firstFlag; throw new RuntimeException(); }
	@Metaline(flagPos=9) public void setUseTitleBackground(boolean value){ firstFlag=firstFlag; throw new RuntimeException(); }
	@Metaline(flagPos=10) public boolean getUseTitleForeground(){ firstFlag=firstFlag; throw new RuntimeException(); }
	@Metaline(flagPos=10) public void setUseTitleForeground(boolean value){ firstFlag=firstFlag; throw new RuntimeException(); }
	@Metaline(flagPos=11) public boolean getImageOnly(){ firstFlag=firstFlag; throw new RuntimeException(); }

	// 12~20

	//	public boolean getStarLevel(){
//		0x100000~0x400000  20~22
//	}
	@Metaline(flagPos=23) public boolean getContentFixedHeightWhenCombined(){ firstFlag=firstFlag; throw new RuntimeException(); }
	@Metaline(flagPos=24) public boolean getNoScrollRect(){ firstFlag=firstFlag; throw new RuntimeException(); }

	@Metaline(flagPos=25) public boolean getShowToolsBtn(){ firstFlag=firstFlag; throw new RuntimeException(); }
	@Metaline(flagPos=25) public void setShowToolsBtn(boolean value){ firstFlag=firstFlag; throw new RuntimeException(); }
	@Deprecated @Metaline(flagPos=26, shift=1) public boolean getRecordHiKeys(){ firstFlag=firstFlag; throw new RuntimeException(); }

	@Metaline(flagPos=27) public boolean getOfflineMode(){ firstFlag=firstFlag; throw new RuntimeException(); }

	@Metaline(flagPos=28) public boolean getLimitMaxMinChars(){ firstFlag=firstFlag; throw new RuntimeException(); }

	@Metaline(flagPos=29) public boolean getAcceptParagraph(){ firstFlag=firstFlag; throw new RuntimeException(); }
	@Metaline(flagPos=29) public void setAcceptParagraph(boolean value){ firstFlag=firstFlag; throw new RuntimeException(); }

	@Metaline(flagPos=30) public boolean getUseInternalParagraphWords(){ firstFlag=firstFlag; throw new RuntimeException(); }
	@Metaline(flagPos=30) public void setUseInternalParagraphWords(boolean value){ firstFlag=firstFlag; throw new RuntimeException(); }

	@Metaline(flagPos=31, shift=1) public boolean getImageBrowsable(){ firstFlag=firstFlag; throw new RuntimeException(); }
	@Metaline(flagPos=31, shift=1) public void setImageBrowsable(boolean value){ firstFlag=firstFlag; throw new RuntimeException(); }
	@Metaline(flagPos=32) public boolean getAutoFold(){ firstFlag=firstFlag; throw new RuntimeException(); }
	@Metaline(flagPos=32) public void setAutoFold(boolean val){ firstFlag=firstFlag; throw new RuntimeException(); }

	@Metaline(flagPos=33) public boolean getDrawHighlightOnTop(){ firstFlag=firstFlag; throw new RuntimeException(); }
	@Metaline(flagPos=33) public void setDrawHighlightOnTop(boolean value){ firstFlag=firstFlag; throw new RuntimeException(); }


	@Metaline(flagPos=34, shift=1) public boolean checkVersionBefore_5_4() { firstFlag=firstFlag; throw new RuntimeException();}
	@Metaline(flagPos=34, shift=1) public void uncheckVersionBefore_5_4(boolean val) { firstFlag=firstFlag; throw new RuntimeException();}


	@Metaline(flagPos=35) public boolean isMergedBook() { firstFlag=firstFlag; throw new RuntimeException();}
	@Metaline(flagPos=35) public void isMergedBook(boolean val) { firstFlag=firstFlag; throw new RuntimeException();}

	@Metaline(flagPos=36) public boolean getEntryJumpList(){ firstFlag=firstFlag; throw new RuntimeException(); }
	/** 词条跳转到点译弹窗 (entry://) */
	@Metaline(flagPos=37) public boolean getPopEntry(){ firstFlag=firstFlag; throw new RuntimeException(); }

	@Metaline(flagPos=38) public boolean hasFilesTag() { firstFlag=firstFlag; throw new RuntimeException();}
	@Metaline(flagPos=38) public void hasFilesTag(boolean val) { firstFlag=firstFlag; throw new RuntimeException();}

	@Metaline(flagPos=39, shift=1) public boolean getUseHosts() { firstFlag=firstFlag; throw new RuntimeException();}
	@Metaline(flagPos=39, shift=1) public void setUseHosts(boolean val) { firstFlag=firstFlag; throw new RuntimeException();}

	@Metaline(flagPos=40) public boolean getUseMirrors() { firstFlag=firstFlag; throw new RuntimeException();}
	@Metaline(flagPos=40) private void setUseMirrorsInternal(boolean val) { firstFlag=firstFlag; throw new RuntimeException();}

	@Metaline(flagPos=41, flagSize=5) public int getMirrorIdx() { firstFlag=firstFlag; throw new RuntimeException();}

	@Metaline(flagPos=46) public boolean padSet() { firstFlag=firstFlag; throw new RuntimeException();}
	@Metaline(flagPos=46) public void padSet(boolean val) { firstFlag=firstFlag; throw new RuntimeException();}
	@Metaline(flagPos=47, shift=1) public boolean padLeft() { firstFlag=firstFlag; throw new RuntimeException();}
	@Metaline(flagPos=47, shift=1) public void padLeft(boolean val) { firstFlag=firstFlag; throw new RuntimeException();}
	@Metaline(flagPos=48, shift=1) public boolean padRight() { firstFlag=firstFlag; throw new RuntimeException();}
	@Metaline(flagPos=48, shift=1) public void padRight(boolean val) { firstFlag=firstFlag; throw new RuntimeException();}

	@Metaline(flagPos=49) public boolean hasWebEntrances() { firstFlag=firstFlag; throw new RuntimeException();}
	@Metaline(flagPos=49) public void hasWebEntrances(boolean val) { firstFlag=firstFlag; throw new RuntimeException();}

	@Metaline(flagPos=50) public boolean padBottom() { firstFlag=firstFlag; throw new RuntimeException();}
	@Metaline(flagPos=50) public void padBottom(boolean val) { firstFlag=firstFlag; throw new RuntimeException();}

	@Metaline(flagPos=51, shift=1) public boolean tapschWebStandalone() { firstFlag=firstFlag; throw new RuntimeException();}
	@Metaline(flagPos=51, shift=1) public void tapschWebStandalone(boolean v) { firstFlag=firstFlag; throw new RuntimeException();}

	@Metaline(flagPos=52) public boolean tapschWebStandaloneSet() { firstFlag=firstFlag; throw new RuntimeException();}
	@Metaline(flagPos=52) public void tapschWebStandaloneSet(boolean v) { firstFlag=firstFlag; throw new RuntimeException();}

	@Metaline(flagPos=53, flagSize=2) public int zhoAny() { firstFlag=firstFlag; throw new RuntimeException();}
	@Metaline(flagPos=53) public boolean zhoVer() { firstFlag=firstFlag; throw new RuntimeException();}
	@Metaline(flagPos=54) public boolean zhoHor() { firstFlag=firstFlag; throw new RuntimeException();}
	@Metaline(flagPos=55) public boolean zhoHigh() { firstFlag=firstFlag; throw new RuntimeException();}


	public static ConcurrentHashMap<Long, UniversalDictionaryInterface> bookImplsMap = new ConcurrentHashMap<>();

	public static ConcurrentHashMap<String, Long> bookImplsNameMap = new ConcurrentHashMap<>();

	public BookPresenter(@NonNull File fullPath, PlainDictionary THIS, int pseudoInit) throws IOException {
		if (pseudoInit<0) {
			placeHolder = fullPath;
			mBaseUrl = idStr = idStr10 = null;
			mType = DictionaryAdapter.PLAIN_BOOK_TYPE.PLAIN_TYPE_EMPTY;
			return;
		}
		bookImpl = getBookImpl(THIS, fullPath, pseudoInit);

		int type = 0;
		if (bookImpl!=null)
		{
			type = bookImpl.getType();
			if (type<0 || type>5)
			{
				type = 0;
			}
		} else if(pseudoInit==0) {
			throw new RuntimeException("Failed To Create Book! "+fullPath);
		}
		mType = DictionaryAdapter.PLAIN_BOOK_TYPE.values()[type];

		StringBuilder sb = new StringBuilder(32);
		sb.append("d");
		long id = bookImpl==null?0:getId();
		idStr = IU.NumberToText_SIXTWO_LE(id, sb).toString();
		sb.setLength(0);
		idStr10 = sb.append(id).append(".com").toString();
		sb.setLength(0);
		mBaseUrl = sb.append("http://mdbr.").append("d").append(idStr10).append("/base.html").toString();
	}

	public final long getId() {
		return bookImpl.getBooKID();
	}

	public int lookUp(String keyword) {
		return bookImpl.lookUp(keyword);
	}

	public static int hashCode(String toHash, int start) {
		int h=0;
		int len = toHash.length();
		for (int i = start; i < len; i++) {
			h = 31 * h + Character.toLowerCase(toHash.charAt(i));
		}
		return h;
	}

	public static UniversalDictionaryInterface getBookImpl(PlainDictionary THIS, File fullPath, int pseudoInit) throws IOException {
		UniversalDictionaryInterface bookImpl = null;
		String pathFull = fullPath.getPath();
		long bid = -1;
		String name = fullPath.getName();
		//if (THIS!=null && THIS.systemIntialized) // 会变空白…
		try {
			Long bid_ = bookImplsNameMap.get(name);
			if (bid_ == null) {
				bid = THIS.getBookID(fullPath.getPath(), name);
				//CMN.Log("新标识::", bid, name);
				if (bid != -1) bookImplsNameMap.put(name, bid);
			} else {
				bid = bid_;
			}
			bookImpl = bookImplsMap.get(bid);
		} catch (Exception e) { } // 读取quanxian时可能未获取权限，无法打开数据库。
		//CMN.Log("getBookImpl", fullPath, bookImpl);
		if ((pseudoInit&2)==0 && bookImpl==null) {
			int sufixp = pathFull.lastIndexOf(".");
			if (sufixp<pathFull.length()-name.length()) sufixp=-1;
			int hash = hashCode(sufixp<0?name:pathFull, sufixp+1);
//			if(sufixp>=0) name = pathFull;
//			int hash=0,i = sufixp+1,len = name.length();
//			for (; i < len; i++) hash = (pseudoInit>>2) * hash + Character.toLowerCase(name.charAt(i));
			switch(hash){
				case 107969:
				case 107949:
				case 3645348:
//					if (pathFull.startsWith("/ASSET"))
//						bookImpl = new PlainMdictAsset(fullPath, pseudoInit&3, THIS==null?null:THIS.MainStringBuilder, THIS);
//					else
					bookImpl = new PlainMdict(fullPath, pseudoInit&3, null, null);
					break;
//				case 117588:
//					bookImpl = new PlainWeb(fullPath, THIS);
//					break;
//				case 110834:
//					bookImpl = new PlainPDF(fullPath, THIS);
//					break;
//				case 115312:
//					bookImpl = new PlainText(fullPath, THIS);
//					break;
//				case 3222:
//					bookImpl = new PlainDSL(fullPath, THIS, THIS.taskRecv);
//					break;
//				case 99773:
//					bookImpl = new PlainDSL(fullPath, THIS, THIS.taskRecv);
//					break;
//				//case 96634189:
//				default:
//					bookImpl = new DictionaryAdapter(fullPath, THIS);
//					break;
//				//case 120609:
//				//return new mdict_zip(fullPath, THIS);
//				//case 3088960:
//				//return new mdict_docx(fullPath, THIS);
			}
			if (bookImpl!=null) {
				bookImpl.setBooKID(bid);
				//if ((pseudoInit&3)==0 && THIS.getUsingDataV2())
				{
					if(bid!=-1) bookImplsMap.put(bid, bookImpl);
				}
			}
		}
		return bookImpl;
	}

	int getCount() {
		return (int) bookImpl.getNumberEntries();
	}

	public JSONObject getDictInfo(JSONObject json) {
		if(json==null)json = new JSONObject();
		else json.clear();
		json.put("name", getDictionaryName());
		json.put("tag", true);
		json.put("id", idStr);
		json.put("tbg", SU.toHexRGB(getTitleBackground()));
		json.put("tfg", SU.toHexRGB(getTitleForeground()));
		json.put("bg", getUseInternalBG()?SU.toHexRGB(getContentBackground()):null);
		json.put("img", getImageBrowsable() && bookImpl.hasMdd());
		PlainWeb webx = getWebx();
		if(webx!=null) {
			json.put("isWeb", 1);
			if(webx.hasField("synthesis") && PDICMainAppOptions.allowMergeSytheticalPage())
				json.put("synth", 1);
			String sch = webx.getSearchUrl();
			json.put("sch", sch);
		}
		return json;
	}

	private int getContentBackground() {
		return 0xffffffff;
	}

	private int getTitleForeground() {
		return 0xffffffff;
	}

	private int getTitleBackground() {
		return 0xff0000ff;
	}

	public String getDictionaryName() {
		String ret=bookImpl.getDictionaryName();
		return ret==null?"":ret;
	}

	public boolean getIsWebx() {
		return mType==DictionaryAdapter.PLAIN_BOOK_TYPE.PLAIN_TYPE_WEB;
	}

	public PlainWeb getWebx() {
		return getIsWebx()?((PlainWeb)bookImpl):null;
	}
	
	public InputStream getDebuggingResource(String decoded) {
		return null;
	}

	public String getPath() {
		return bookImpl.getFile().getPath();
	}

	public void plugCssWithSameFileName(StringBuilder mdPageBuilder) {
	}

	public void ApplyPadding(StringBuilder sb) {
//		if (PDICMainAppOptions.padLeft() && padLeft()) {
//			if (CMN.GlobalPagePaddingLeft==null)
//				CMN.GlobalPagePaddingLeft = opt.getString("GPL", "3%");
//			sb.append("padding-left:").append(CMN.GlobalPagePaddingLeft).append(";");
//			//else mWebView.evaluateJavascript("document.body.style.paddingLeft='"+CMN.GlobalPagePaddingLeft+"'", null);
//		}
//		if (PDICMainAppOptions.padRight() && padRight()) {
//			if (CMN.GlobalPagePaddingRight==null)
//				CMN.GlobalPagePaddingRight = opt.getString("GPR", "3%");
//			sb.append("padding-right:").append(CMN.GlobalPagePaddingRight).append(";");
//			//else mWebView.evaluateJavascript("document.body.style.paddingRight='"+CMN.GlobalPagePaddingRight+"'", null);
//		}
	}
	public final String GetSearchKey() {
		return searchKey;
	}

	public final void SetSearchKey(String key) {
		searchKey = key;
	}

	public CharSequence GetAppSearchKey() {
		return null;
	}

	public PlainMdict getMdict() {
		return (PlainMdict) bookImpl;
	}
}

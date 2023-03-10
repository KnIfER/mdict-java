package com.knziha.plod.plaindict;

import com.knziha.plod.dictionary.UniversalDictionaryInterface;
import com.knziha.plod.dictionary.Utils.IU;
import com.knziha.plod.dictionarymodels.DictionaryAdapter;
import com.knziha.plod.dictionarymodels.PlainMdict;
import edu.umd.cs.findbugs.annotations.NonNull;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

public class BookPresenter {
	public final static String baseUrl = "http://mdbr.com/base.html";
	public final String mBaseUrl;
	public final String idStr;
	public final String idStr10;
	public File placeHolder;
	
	private final DictionaryAdapter.PLAIN_BOOK_TYPE mType;
	public /*final*/ UniversalDictionaryInterface bookImpl;

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
	
}

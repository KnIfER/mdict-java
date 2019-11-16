package com.knziha.plod.dictionaryBuilder;

import com.knziha.plod.dictionary.Utils.IU;
import com.knziha.plod.dictionary.mdict;
import com.knziha.rbtree.myAbsCprKey;
import org.apache.commons.text.StringEscapeUtils;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import static test.CMN.emptyStr;

/**
 * Author KnIfER
 * Date 2018/05/31
 */

public class mdictBuilder extends mdictBuilderBase {
	public String _stylesheet = "";
	private boolean isKeyCaseSensitive=false;
	private boolean isStripKey=true;
	String sharedMdd;
	private final String nullStr=null;

	public mdictBuilder(String Dictionary_Name, String about,String charset) {
		data_tree= new ArrayListTree<>();
		privateZone = new IntervalTree();
		_Dictionary_Name=Dictionary_Name;
		_about=StringEscapeUtils.escapeHtml4(about);
		if(charset.toUpperCase().startsWith("UTF-16")){
			charset="UTF-16LE";
		}
		_encoding=charset;
		_charset=Charset.forName(_encoding);
		mContentDelimiter="\r\n\0".getBytes(_charset);
	}

	public int insert(String key,String data) {
		data_tree.insertNode(new myCprKey(key,data));
		return 0;
	}


	public void insert(String key, File file) {
		data_tree.insertNode(new myCprKey(key,nullStr));
		fileTree.put(key, file);
	}
	public void recordFile(String key,File file) {
		fileTree.put(key, file);
	}

	public void insert(String key, ArrayList<myCprKey> bioc) {
		data_tree.insertNode(new myCprKey(key+"[<>]",nullStr));
		bookTree.put(key, bioc);
	}
	public void append(String key, File inhtml) {
		((ArrayListTree<myAbsCprKey>)data_tree).add(new myCprKey(key,nullStr));
		fileTree.put(key, inhtml);
	}

	@Override
	String constructHeader() {
		String encoding = _encoding;
		if(encoding.equals("UTF-16LE"))
			encoding = "UTF-16"; //INCONGRUENT java charset
		if (encoding.equals(""))
			encoding = "UTF-8";
		final float _version = 2.0f;
		SimpleDateFormat timemachine = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
		StringBuilder sb = new StringBuilder().append(new String(new byte[] {(byte) 0xff,(byte) 0xfe}, StandardCharsets.UTF_16LE))
				.append("<Dictionary GeneratedByEngineVersion=")//v
				.append("\"").append(_version).append("\"")
				.append(" RequiredEngineVersion=")//v
				.append("\"").append(_version).append("\"")
				.append(" CreationDate=")
				.append("\"").append(timemachine.format(new Date(System.currentTimeMillis()))).append("\"")
				.append(" Encrypted=")
				.append("\"").append("0").append("\"")//is NO valid?
				.append(" Encoding=")
				.append("\"").append(encoding).append("\"")
				.append(" Format=")//Format
				.append("\"").append("Html").append("\"")
				.append(" Compact=")//c
				.append("\"").append("Yes").append("\"")
				.append(" KeyCaseSensitive=")//k
				.append("\"").append(isKeyCaseSensitive?"Yes":"No").append("\"")
				.append(" StripKey=")//k
				.append("\"").append(isStripKey?"Yes":"No").append("\"")
				.append(" Description=")
				.append("\"").append(_about).append("\"")
				.append(sharedMdd!=null?" SharedMdd='"+sharedMdd+"\"":"")
				.append(" Title=")
				.append("\"").append(_Dictionary_Name).append("\"")
				.append(" StyleSheet=")
				.append("\"").append(_stylesheet).append("\"")
				.append("/>");
		return sb.toString();
	}

	@Override
	byte[] bakeMarginKey(String key){
		return key.toLowerCase().replace(" ",emptyStr).replace("-",emptyStr).getBytes(_charset);
	}

	private int bookGetRecordLen(String key) {
		int len =0;
		ArrayList<myCprKey> bookc = bookTree.get(key.substring(0, key.length()-4));
		for(myCprKey xx:bookc) {
			if(xx.value!=null) {
				try {
					len+=xx.value.getBytes(_encoding).length;
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
			}
			else
				len+=fileTree.get(xx.key).length();
		}
		return len;
	}

	private int bookGetNumKeys(String key) {
		ArrayList<myCprKey> bookc = bookTree.get(key.substring(0, key.length()-4));
		return bookc.size();
	}

	public int getCountOf(String key) {
		return ((ArrayListTree<myAbsCprKey>)data_tree).getCountOf(new myCprKey(key,""));
	}

	public void setSharedMdd(String name) {
		sharedMdd=name;
	}

	public void setKeycaseSensitive(boolean b) {
		isKeyCaseSensitive=b;
	}

	protected String mOldSchoolToLowerCase(String input) {
		StringBuilder sb = new StringBuilder(input);
		for(int i=0;i<sb.length();i++) {
			if(sb.charAt(i)>='A' && sb.charAt(i)<='Z')
				sb.setCharAt(i, (char) (sb.charAt(i)+32));
		}
		return sb.toString();
	}

	protected String processMyText(String input) {
		String ret = isStripKey?mdict.replaceReg.matcher(input).replaceAll(emptyStr):input;
		return isKeyCaseSensitive?ret:ret.toLowerCase();
	}

	public class myCprKey extends myAbsCprKey {
		public String value;
		public myCprKey(String vk, String v) {
			super(vk);
			value=v;
		}
		@Override
		public int compareTo(myAbsCprKey other) {
			if(other instanceof myCprKey){
				if(key.endsWith(">") && other.key.endsWith(">")) {
					int idx2 = key.lastIndexOf("<",key.length()-2);
					if(idx2!=-1) {
						int idx3 = other.key.lastIndexOf("<",key.length()-2);
						if(idx3!=-1) {
							if(key.startsWith(other.key.substring(0,idx3))) {
								String itemA=key.substring(idx2+1,key.length()-1);
								String itemB=other.key.substring(idx2+1,other.key.length()-1);
								idx2=-1;idx3=-1;
								if(IU.shuzi.matcher(itemA).find()) {
									idx2=IU.parsint(itemA);
								}else if(IU.hanshuzi.matcher(itemA).find()) {
									idx2=IU.recurse1wCalc(itemA, 0, itemA.length()-1, 1);
								}
								if(idx2!=-1) {
									if(IU.shuzi.matcher(itemB).find()) {
										idx3=IU.parsint(itemB);
									}else if(IU.hanshuzi.matcher(itemB).find()) {
										idx3=IU.recurse1wCalc(itemB, 0, itemB.length()-1, 1);
									}
									if(idx3!=-1)
										return idx2-idx3;
								}

							}
						}
					}
				}
				return (processMyText(key).compareTo(processMyText(other.key)));
			}
			return 111;
		}

		@Override
		public Object value() {
			return value;
		}

		@Override
		public byte[] getBinVals() {
			return value==null?null:value.getBytes(_charset);
		}

	}
}
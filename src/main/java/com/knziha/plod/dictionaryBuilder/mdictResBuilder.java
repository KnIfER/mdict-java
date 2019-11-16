package com.knziha.plod.dictionaryBuilder;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import com.knziha.rbtree.myAbsCprKey;

import com.knziha.plod.dictionary.mdictRes;
import com.knziha.rbtree.RBTree;


/**
 * Author KnIfER
 * Date 2018/08/17
 */

public class mdictResBuilder extends mdictBuilderBase {
	public void insert(String key, File inhtml) {
		data_tree.insertNode(new myCpr_Strict(key,nullStr));
		fileTree.put(key, inhtml);
	}
	private final byte[] nullStr=null;

	public mdictResBuilder(String Dictionary_Name, String about) {
		perKeyBlockSize_IE_IndexBlockSize=16;
		bAbortOldRecordBlockOnOverFlow=false;
		globalCompressionType=2;
		_encoding="UTF-16LE";
		_charset=StandardCharsets.UTF_16LE;
		data_tree= new RBTree<>();
		_Dictionary_Name=Dictionary_Name;
		_about=about;
	}

	public int insert(String key,byte[] data) {
		data_tree.insertNode(new myCpr_Strict(key,data));
		return 0;
	}

	@Override
	String constructHeader() {
		final float _version = 2.0f;
		SimpleDateFormat timemachine = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
		StringBuilder sb = new StringBuilder()
				.append("<Library_Data GeneratedByEngineVersion=")//v
				.append("\"").append(_version).append("\"")
				.append(" RequiredEngineVersion=")//v
				.append("\"").append(_version).append("\"")
				.append(" CreationDate=")//v
				.append("\"").append(timemachine.format(new Date(System.currentTimeMillis()))).append("\"")
				.append(" Encrypted=")
				.append("\"").append("0").append("\"")//is NO valid?
				.append(" Encoding=")
				.append("\"").append("").append("\"")
				.append(" Format=")//Format
				.append("\"").append("").append("\"")
				.append(" Compact=")//c
				.append("\"").append("Yes").append("\"")
				.append(" KeyCaseSensitive=")//k
				.append("\"").append("No").append("\"")
				.append(" StripKey=")//k
				.append("\"").append("No").append("\"")
				.append(" Description=")
				.append("\"").append(_about).append("\"")
				.append(" Title=")
				.append("\"").append(_Dictionary_Name).append("\"")
				.append("/>");
		return sb.toString();
	}

	@Override
	byte[] bakeMarginKey(String key){
		return key.toLowerCase().getBytes(_charset);
	}

	public mdictRes getMdd() {
		try {
			return new mdictRes(f.getAbsolutePath());
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	class myCpr_Strict extends myAbsCprKey {
		public byte[] value;

		public myCpr_Strict(String k,byte[] v){
			super(k);
			value=v;
		}

		@Override
		public int compareTo(myAbsCprKey other) {
			if(other instanceof myCpr_Strict){
				return this.key.toLowerCase().compareTo(other.key.toLowerCase());
			}
			return 111;
		}

		@Override
		public Object value() {
			return value;
		}

		@Override
		public byte[] getBinVals() {
			return value;
		}
	}
}
package com.knziha.plod.dictionarymodels;

import javafx.beans.property.*;
import org.apache.commons.text.StringEscapeUtils;

import java.io.File;
import java.io.IOException;


/*
 ui side of mdict
 data:2019.10.22
 author:KnIfER
*/
public class mdict extends com.knziha.plod.dictionary.mdict {
	static{
		System.setProperty("file.encoding", "UTF-8");
	}
	public boolean tmpIsFilter;
	@Deprecated
	public mdict(){
	}
	//构造
	public mdict(String fn) throws IOException {
		super(fn);
	}

	public String getAboutString() {
		String pure=StringEscapeUtils.unescapeHtml3(_header_tag.get("Description"));
		StringBuilder sb= new StringBuilder();
		sb.append("<style>html,body{padding:25px;color:#fff;}body::-webkit-scrollbar{display:none;}</style><script>window.onclick=function(e){parent.window.dismiss_menu();}</script>");
		sb.append(_Dictionary_fName).append(" - about:").append("<br/>").append("<br/>");
		sb.append(pure)
			.append("<BR>").append("<HR>").append(getDictInfo());
		return sb.toString();
	}

	@Override
	public boolean renameFileTo(File newF) {
		boolean ret=super.renameFileTo(newF);
		if(ret){
			_Dictionary_fName = f.getName();
			int tmpIdx = _Dictionary_fName.lastIndexOf(".");
			if(tmpIdx!=-1) {
				_Dictionary_fSuffix = _Dictionary_fName.substring(tmpIdx+1);
				_Dictionary_fName = _Dictionary_fName.substring(0, tmpIdx);
			}
		}
		return ret;
	}

	@Override
	public boolean getIsDedicatedFilter() {
		return tmpIsFilter;
	}

	@Override
	public String getCachedEntryAt(int pos) {
		return getEntryAt(pos);
	}

	public final StringProperty getFileNameProperty() {
		return new SimpleStringProperty(_Dictionary_fName);
	}
	public final DoubleProperty getFileSizeProperty() {
		return new SimpleDoubleProperty((f.length()/1024.0/1024));
	}
	public final BooleanProperty getFormationProperty() {
		return new SimpleBooleanProperty(tmpIsFilter);
	}
}

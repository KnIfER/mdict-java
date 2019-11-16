package com.knziha.plod.dictionarymodels;

import com.knziha.plod.PlainDict.PlainDictAppOptions;
import javafx.beans.property.*;
import org.apache.commons.text.StringEscapeUtils;
import org.joni.Option;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


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
	PlainDictAppOptions opt;
	//构造
	public mdict(String fn, PlainDictAppOptions _opt) throws IOException {
		super(fn);
		opt=_opt;
	}

	public String getAboutHtml() {
		String pure=StringEscapeUtils.unescapeHtml3(getAboutString());
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
	protected boolean getUseJoniRegex(int mode){
		return opt.GetRegexSearchEngineEnabled();
	}

	@Override
	protected boolean getRegexAutoAddHead(){
		return opt.GetRegexSearchEngineAutoAddHead();
	}

	@Override
	protected int getRegexOption(){
		int ret=Option.NONE;
		if(!opt.GetRegexSearchEngineCaseSensitive())
			ret|=Option.IGNORECASE;
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

	public void savePagesTo(File file, int...position) throws IOException {
		if(isResourceFile){
			RecordLogicLayer hey = new RecordLogicLayer();
			for (int i = 0; i < position.length; i++) {
				FileOutputStream out = new FileOutputStream(new File(file.getParentFile(), "TODO.aaa"));
				getRecordData(position[i], hey);
				out.write(hey.data, hey.ral, hey.val-hey.ral);
				out.close();
			}
		}else {
			FileOutputStream out = new FileOutputStream(file);
			out.write(getRecordsAt(position).getBytes(StandardCharsets.UTF_8));
			out.close();
		}
	}

	@Override
	protected ExecutorService OpenThreadPool(int thread_number) {
		return Executors.newWorkStealingPool();
	}
}

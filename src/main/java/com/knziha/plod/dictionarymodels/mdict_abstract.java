package com.knziha.plod.dictionarymodels;

import test.CMN;

import java.io.File;
import java.io.IOException;

/*
 ui side of mdict
 data:2018.07.30
 author:KnIfER
*/


public abstract class mdict_abstract extends mdict {
	//构造
	public mdict_abstract(String fn) throws IOException {
		super(goodNull(fn), null);
	}

	@Override
	protected void initLogically() {
		_num_record_blocks=-1;
		String fn = CMN.UniversalObject;
		fn = new File(fn).getAbsolutePath();
		f = new File(fn);
		_Dictionary_fName = f.getName();
		int tmpIdx = _Dictionary_fName.lastIndexOf(".");
		if(tmpIdx!=-1) {
			_Dictionary_fSuffix = _Dictionary_fName.substring(tmpIdx+1);
			_Dictionary_fName = _Dictionary_fName.substring(0, tmpIdx);
		}
	}

	static String goodNull(String fn) {
		CMN.UniversalObject=fn;
		return null;
	}

	@Override
	public boolean renameFileTo(File newF) {
		f = newF;
		_Dictionary_fName = f.getName();
		int tmpIdx = _Dictionary_fName.lastIndexOf(".");
		if(tmpIdx!=-1) {
			_Dictionary_fSuffix = _Dictionary_fName.substring(tmpIdx+1);
			_Dictionary_fName = _Dictionary_fName.substring(0, tmpIdx);
		}
		return true;
	}
}

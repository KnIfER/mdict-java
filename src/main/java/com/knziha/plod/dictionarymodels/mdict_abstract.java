package com.knziha.plod.dictionarymodels;

import com.knziha.plod.dictionary.Utils.SU;
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
	public mdict_abstract(File fn) throws IOException {
		super(fn, null);
	}

	@Override
	public boolean renameFileTo(File newF) {
		f = newF;
		_Dictionary_fName = f.getName();
		int tmpIdx = _Dictionary_fName.lastIndexOf(".");
		if(tmpIdx!=-1) {
			_Dictionary_fName = _Dictionary_fName.substring(0, tmpIdx);
		}
		return true;
	}

	@Override
	protected boolean StreamAvailable() {
		return false;
	}
}

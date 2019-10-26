package com.knziha.plod.dictionarymodels;

import java.io.File;


/*
 ui side of mdict
 data:2018.07.30
 author:KnIfER
*/


public class mdict_preempter extends mdict {
	//构造
	public mdict_preempter(String fn) {
		fn = new File(fn).getAbsolutePath();
		f = new File(fn);
        _Dictionary_fName = f.getName();
    	int tmpIdx = _Dictionary_fName.lastIndexOf(".");
    	if(tmpIdx!=-1) {
	    	_Dictionary_fSuffix = _Dictionary_fName.substring(tmpIdx+1);
	    	_Dictionary_fName = _Dictionary_fName.substring(0, tmpIdx);
    	}
	}
}

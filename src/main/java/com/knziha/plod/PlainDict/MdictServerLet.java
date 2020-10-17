package com.knziha.plod.PlainDict;

import com.knziha.plod.dictionarymodels.mdict;

public interface MdictServerLet {
	String md_getName(int i);
	mdict md_get(int i);
	int md_get(String i);
	int md_getSize();
}

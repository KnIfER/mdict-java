package com.knziha.plod.PlainDict;

import com.knziha.plod.dictionarymodels.PlainMdict;

public interface MdictServerLet {
	String md_getName(int i);
	PlainMdict md_get(int i);
	int md_get(String i);
	int md_getSize();
}

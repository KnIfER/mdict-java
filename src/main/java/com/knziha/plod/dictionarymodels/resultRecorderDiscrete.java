package com.knziha.plod.dictionarymodels;

import com.knziha.plod.dictionary.Utils.Flag;

public abstract class resultRecorderDiscrete {

	public String currentSearchTerm="";
	public com.knziha.plod.dictionary.mdict.AbsAdvancedSearchLogicLayer SearchLauncher;

	Flag mflag = new Flag();
	
	public String getResAt(int pos) {return "";}

	public abstract PlainMdict getMdAt(int pos);

	public abstract int getIndexAt(int pos);

	public abstract void renderContentAt(int pos, int selfAtIdx, boolean post);

	public abstract int size();
	
	public abstract void invalidate();
	
	public abstract void invalidate(int adapter_idx);

	public abstract void shutUp();
	
	public int dictIdx=0;

}

package com.knziha.plod.dictionarymodels;

import com.knziha.plod.dictionary.Utils.Flag;

public class resultRecorderDiscrete {

	public String currentSearchTerm="";
	
	public resultRecorderDiscrete(){};
	
	Flag mflag = new Flag();
	
	public String getResAt(int pos) {return "";}
	
	public void renderContentAt(int pos) {}
	
	public int size() {return 0;}
	
	public void invalidate() {}
	
	public void invalidate(int adapter_idx) {}

	public void shutUp() {}
	
	public int dictIdx=0;

}

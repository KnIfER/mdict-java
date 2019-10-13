package com.knziha.plod.PlainDict;


import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "opt")
public class PlainDictAppOptions
{
	public static String locale;

	public PlainDictAppOptions(){

	}

	private long FirstFlag=0;

	@XmlElement(name = "MFF")
	public long getFirstFlag() {
		return FirstFlag;
	}

	private void setFirstFlag(long val) {
		FirstFlag=val;
	}



	private void updateFFAt(int o, boolean val) {
		FirstFlag &= (~o);
		if(val) FirstFlag |= o;
		//defaultReader.edit().putInt("MFF",FirstFlag).commit();
	}
	private void updateFFAt(long o, boolean val) {
		FirstFlag &= (~o);
		if(val) FirstFlag |= o;
		//defaultReader.edit().putInt("MFF",FirstFlag).commit();
	}


	public boolean GetCombinedSearching() {
		return (FirstFlag & 1) == 1;
	}
	public boolean SetCombinedSearching(boolean val) {
		updateFFAt(1,val);
		return val;
	}

	public boolean GetBottombarOnBottom() {
		return (FirstFlag & 2) == 2;
	}
	public boolean SetBottombarOnBottom(boolean val) {
		updateFFAt(2,val);
		return val;
	}



}
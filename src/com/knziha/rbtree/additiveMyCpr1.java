package com.knziha.rbtree;
import java.util.ArrayList;
import java.util.List;

import com.knziha.plod.dictionary.mdict;






public class additiveMyCpr1 implements Comparable<additiveMyCpr1>{
	public String key;
	public Object value;
	public additiveMyCpr1(String k,Object v){
		key=k;value=v;
	}
	
	public int compareTo(additiveMyCpr1 other) {
		//CMN.show(this.key.replaceAll(CMN.replaceReg,CMN.emptyStr));
		return mdict.replaceReg.matcher(key).replaceAll("").toLowerCase().compareTo(mdict.replaceReg.matcher(other.key).replaceAll("").toLowerCase());
	}
	public String toString(){
		String str = ""; //for(Integer i:value) str+="@"+i;
		return key+"____"+str;
	}
}
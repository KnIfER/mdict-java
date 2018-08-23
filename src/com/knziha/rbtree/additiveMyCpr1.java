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
		return this.key.toLowerCase().replaceAll(mdict.replaceReg,mdict.emptyStr).compareTo(other.key.toLowerCase().replaceAll(mdict.replaceReg,mdict.emptyStr));
	}
	public String toString(){
		String str = ""; //for(Integer i:value) str+="@"+i;
		return key+"____"+str;
	}
}
package rbtree;
import java.util.ArrayList;

import plod.CMN;


public class additiveMyCpr1 implements Comparable<additiveMyCpr1>{
	public String key;
	public ArrayList<Integer> value;
	public additiveMyCpr1(String k,ArrayList<Integer> v){
		key=k;value=v;
	}
	public int compareTo(additiveMyCpr1 other) {
		//CMN.show(this.key.replaceAll(CMN.replaceReg,CMN.emptyStr));
		return this.key.replaceAll(CMN.replaceReg,CMN.emptyStr).compareTo(other.key.replaceAll(CMN.replaceReg,CMN.emptyStr));
	}
	public String toString(){
		String str = ""; for(Integer i:value) str+="@"+i;
		return key+"____"+str;
	}
}
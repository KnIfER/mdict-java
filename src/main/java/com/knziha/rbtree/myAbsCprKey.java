package com.knziha.rbtree;

public abstract class myAbsCprKey implements Comparable<myAbsCprKey> {
	public String key;
	public myAbsCprKey(String vk){
		key=vk;
	}
	public abstract int compareTo(myAbsCprKey other);
	public String toString(){
		return key+"_"+value();
	}
	public abstract Object value();
	public abstract byte[] getBinVals();
}

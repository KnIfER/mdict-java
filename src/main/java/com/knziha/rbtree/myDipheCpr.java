package com.knziha.rbtree;

public class myDipheCpr<T1 extends Comparable<T1>,T2 extends Comparable<T2>> implements Comparable<myDipheCpr<T1,T2>>{
    	public T1 key;
    	public T2 value;
    	//private final String emptyStr = "";
    	public myDipheCpr(T1 k,T2 v){
    		key=k;value=v;
    	}
    	public int compareTo(myDipheCpr<T1,T2> other) {
			return this.key.compareTo(other.key);
    	}
    	public String toString(){
    		return key+"_"+value;
    	}
}
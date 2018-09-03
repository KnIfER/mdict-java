package com.knziha.plod.dictionary;

public class myCpr<T1 extends Comparable<T1>,T2> implements Comparable<myCpr<T1,T2>>{
    	public T1 key;
    	public T2 value;
    	//private final String emptyStr = "";
    	public myCpr(T1 k,T2 v){
    		key=k;value=v;
    	}
    	public int compareTo(myCpr<T1,T2> other) {
 
    		if(key.getClass()==String.class) {

    			return (mdict.processText((String)key)
    					.compareTo(mdict.processText((String)other.key)));
    		}
    		else
    			return this.key.compareTo(other.key);
    	}
    	public String toString(){
    		return key+"_"+value;
    	}
    }
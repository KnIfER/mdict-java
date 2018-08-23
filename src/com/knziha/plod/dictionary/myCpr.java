package com.knziha.plod.dictionary;

public class myCpr<T1 extends Comparable<T1>,T2> implements Comparable<myCpr<T1,T2>>{
    	public T1 key;
    	public T2 value;
    	public myCpr(T1 k,T2 v){
    		key=k;value=v;
    	}
    	public int compareTo(myCpr<T1,T2> other) {
 
    		if(key.getClass()==String.class) {

    			return ((String)key)
    					.toLowerCase().replace(" ",mdict.emptyStr).replace("-",mdict.emptyStr)
    					.compareTo(((String)other.key)    					
						.toLowerCase().replace(" ",mdict.emptyStr).replace("-",mdict.emptyStr)
    							);
    		}
    		else
    			return this.key.compareTo(other.key);
    	}
    	public String toString(){
    		return key+"_"+value;
    	}
    }
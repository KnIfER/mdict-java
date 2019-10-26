package com.knziha.rbtree;
import java.util.ArrayList;


public class additiveMyCpr<T1 extends Comparable<T1>,T2> implements Comparable<additiveMyCpr<T1,T2>>{
	public T1 key;
	public ArrayList<T2> value;
	public additiveMyCpr(T1 k,ArrayList<T2> v){
		key=k;value=v;
	}
	public int compareTo(additiveMyCpr<T1,T2> other) {
		return this.key.compareTo(other.key);
	}
	public String toString(){
		String str = ""; for(T2 i:value) str+="@"+i;
		return key+"____"+str;
	}
}
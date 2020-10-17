package com.knziha.plod.dictionary.Utils;

public class MyIntPair<T1,T2>{
	public int key;
	public int value;
	public MyIntPair(int k, int v){
		key=k;value=v;
	}

	public void set(int k, int v){
		key=k;value=v;
	}
}
package com.knziha.plod.dictionaryBuilder;


import java.util.HashMap;

import com.knziha.plod.dictionary.Utils.myCpr;

public class IntervalTree extends ArrayListTree<myCpr<Integer,Integer>>{
	
	
	IntervalTree(){
		super();
	}
	
	myCpr<Integer,Integer> container(int key){
		if(data.size()<=0)
			return null;
		if(key<data.get(0).key || key>data.get(data.size()-1).value)
			return null;
		int idx = reduce(new myCpr<>(key,0),0,data.size());
		//CMN.show(idx+" key"+key+"  ll"+data.get(idx).key);
		if(data.get(idx).key==key)
			return data.get(idx);
		//CMN.show("??");
		if(data.get(idx-1).key<=key && data.get(idx-1).value>=key)
			return data.get(idx-1);
				
		return null;
	}
	
	public void addInterval(int start,int end){
		insert(new myCpr<>(start,end));
	}
	public void addInterval(int start,int end,String name){
		insert(new myCpr<>(start,end));
		names.put(start, name);
	}
	public HashMap<Integer, String> names = new HashMap<>();
}

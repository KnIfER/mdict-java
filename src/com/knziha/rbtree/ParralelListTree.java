package com.knziha.rbtree;

import java.util.ArrayList;

import com.knziha.plod.dictionary.myCpr;
import com.knziha.rbtree.RBTNode;

import test.CMN;

public class ParralelListTree<T1 extends Comparable<T1>,T2 extends Comparable<T2>> {
	//wonderful!
	boolean bIsDuplicative=false;
	public final ArrayList<T1> data;
	public final ArrayList<T2> value;
	
	
	public ParralelListTree(){
		data = new  ArrayList<>();
		value = new  ArrayList<>();
	}
	public ParralelListTree(int initialCap){
		data = new  ArrayList<>(initialCap);
		value = new  ArrayList<>(initialCap);
	}
	
	public int insert(T1 val, T2 cal){
		if(data.size()==0 || data.get(data.size()-1).compareTo(val)<(bIsDuplicative?1:0)) {//递增 bonus
			data.add(val);
			value.add(cal);
			return data.size();
		}
		int idx=-1;
		//if(data.get(0).compareTo(val)>=0)
		//	idx=0;
		//else
			idx = binarySearch(data,val);

		CMN.Log(val+ " idx, "+idx);
		
		if(idx==-1)
			return idx;
		if(idx<0) {
			idx = -2-idx;
			if(idx>=0) {
				
			}else
				return -1;
		}
		
		if(!bIsDuplicative && val.compareTo(data.get(idx))==0) {
			return -1;
		}
		data.add(idx,val);
		value.add(idx,cal);
		return idx;
	}
	
	@SuppressWarnings("rawtypes")
    private static int binarySearch(ArrayList a,Object key) {
			int low = 0;
			int high = a.size() - 1;
			
			while (low <= high) {
				int mid = (low + high) >>> 1;
				Comparable midVal = (Comparable)a.get(mid);
				@SuppressWarnings("unchecked")
				int cmp = midVal.compareTo(key);
				
				if (cmp < 0)
					low = mid + 1;
				else if (cmp > 0)
					high = mid - 1;
				else
					return mid; // key found
			}
			return -(low+2);  // key not found.
	}
	
	public int lookUpKey(T1 key, boolean isStrict) {
		int ret = binarySearch(data,key);
		if(isStrict)
			return ret;
		else {
			if(ret<-1)
				return -2-ret;
			return ret;
		}
	}
	public int lookUpVal(T2 val, boolean isStrict) {
		int ret = binarySearch(data,val);
		if(isStrict)
			return ret;
		else {
			if(ret<-1)
				return -2-ret;
			return ret;
		}
	}

	public Integer size() {
		return data.size();
	}

	public void add(T1 val) {
		data.add(val);
	}
	
	
	
}

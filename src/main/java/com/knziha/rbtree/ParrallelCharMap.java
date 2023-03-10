package com.knziha.rbtree;

import com.knziha.plod.plaindict.CMN;

public class ParrallelCharMap {
	//wonderful!
	public final char[] data;
	public final String[] value;
	int offset=0;

	public ParrallelCharMap(int size){
		data = new  char[size];
		value = new String[size];
	}
	
	public int insert(char val, String cal){
		if(offset ==0 || data[offset -1]<val) {//递增 bonus
			data[offset]=val;
			value[offset]=cal;
			return ++offset;
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
		
		if(val-data[idx]==0) {
			return -1;
		}
		data[offset]=val;
		value[offset]=cal;
		return ++offset;
	}
	
	@SuppressWarnings("rawtypes")
    private static int binarySearch(char[] a,char key) {
			int low = 0;
			int high = a.length - 1;
			
			while (low <= high) {
				int mid = (low + high) >>> 1;
				char midVal = a[mid];
				@SuppressWarnings("unchecked")
				int cmp = midVal-key;
				
				if (cmp < 0)
					low = mid + 1;
				else if (cmp > 0)
					high = mid - 1;
				else
					return mid; // key found
			}
			return -(low+2);  // key not found.
	}
	
	public int lookUpKey(char key, boolean isStrict) {
		int ret = binarySearch(data,key);
		if(isStrict)
			return ret;
		else {
			if(ret<-1)
				return -2-ret;
			return ret;
		}
	}
	public int lookUpVal(char val, boolean isStrict) {
		int ret = binarySearch(data,val);
		if(isStrict)
			return ret;
		else {
			if(ret<-1)
				return -2-ret;
			return ret;
		}
	}

	public int size() {
		return offset;
	}
}

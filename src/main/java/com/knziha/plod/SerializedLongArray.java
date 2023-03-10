package com.knziha.plod;

import com.knziha.plod.ArrayList.SerializedArrayList;
import com.knziha.plod.dictionary.Utils.IU;

public class SerializedLongArray extends SerializedArrayList {
	public SerializedLongArray(int initialCapacity) {
		super(8, initialCapacity);
		DUMMY_DATA();
	}
	
	public boolean remove(long value){
		IU.writeLong(dummyData, 0, value);
		int index = super.indexOf(dummyData);
		if (index>=0) {
			super.remove(index, null);
			return true;
		}
		return false;
	}
	
	
	public void add(long value) {
		IU.writeLong(dummyData, 0, value);
		super.add(dummyData);
	}
	
	public void add(int index, long value){
		IU.writeLong(dummyData, 0, value);
		super.add(index, dummyData);
	}
	
	public long get(int position) {
		return IU.readLong(elementData, position*elementStep);
	}
}

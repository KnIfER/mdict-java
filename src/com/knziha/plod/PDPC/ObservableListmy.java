package com.knziha.plod.PDPC;

import javafx.collections.ObservableListBase;

public class ObservableListmy extends ObservableListBase<Integer> {
	dict_Activity_resultRecoder rec = new dict_Activity_resultRecoder();
	
	@Override
	public Integer get(int index) {
		return new Integer(index);
		//return null;
	}

	@Override
	public int size() {
		return rec.size();
	}

}

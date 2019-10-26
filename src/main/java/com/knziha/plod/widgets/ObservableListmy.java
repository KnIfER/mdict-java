package com.knziha.plod.widgets;

import com.knziha.plod.dictionarymodels.resultRecorderDiscrete;
import javafx.collections.ObservableListBase;

public class ObservableListmy extends ObservableListBase<Integer> {
	public resultRecorderDiscrete rec = new resultRecorderDiscrete();
	
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

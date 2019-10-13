package com.knziha.plod.widgets;

import com.knziha.plod.dictionarymodels.dict_Activity_resultRecoder;
import javafx.collections.ObservableListBase;

public class ObservableListmy extends ObservableListBase<Integer> {
	public dict_Activity_resultRecoder rec = new dict_Activity_resultRecoder();
	
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

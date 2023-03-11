package com.knziha.plod.plaindict.javafx.widgets;

import com.knziha.plod.dictionarymodels.resultRecorderDiscrete;
import javafx.collections.ObservableListBase;

public class ObservableListmy extends ObservableListBase<Integer> {
	public final resultRecorderDiscrete rec;

	public ObservableListmy(resultRecorderDiscrete rec) {
		this.rec = rec;
	}

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

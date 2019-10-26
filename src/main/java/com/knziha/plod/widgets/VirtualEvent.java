package com.knziha.plod.widgets;

import javafx.event.Event;

public class VirtualEvent extends Event{
	String _id;
	public VirtualEvent(Object source) {
		super(source,null,null);
	}
}

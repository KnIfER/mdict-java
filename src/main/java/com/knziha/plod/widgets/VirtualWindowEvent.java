package com.knziha.plod.widgets;

import javafx.stage.Window;
import javafx.stage.WindowEvent;

public class VirtualWindowEvent extends WindowEvent {
	String _id;
	public VirtualWindowEvent(Window source) {
		super(source,null);
	}
}

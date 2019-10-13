package com.knziha.plod.widgets;

import com.sun.org.apache.regexp.internal.RE;
import javafx.scene.Cursor;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import test.CMN;

import java.awt.*;

public class DragResizeView extends Rectangle {
	public ColumnConstraints col1;
	public GridPane gl_to_guard;
	private double dragStartX;
	private double newSize;
	private double percent=75;
	private double dragStartPercent=75;
	private boolean dragDetected;
	private boolean mouseClicked;
	private boolean mouseEntered;

	public DragResizeView(double _percent) {
    	setId("DragResizeView");
		int width=10;
		int height=40;
		setWidth(width);
		setHeight(height);
		percent=_percent;
		addEventHandler(MouseEvent.MOUSE_ENTERED, event ->{
			mouseEntered=true;
			invalid_cursor();
		});

		addEventHandler(MouseEvent.MOUSE_EXITED, event -> {
			mouseEntered=false;
			invalid_cursor();
		});

		addEventHandler(MouseEvent.MOUSE_RELEASED, event ->{
			mouseClicked=dragDetected=false;
			invalid_cursor();
		});

		addEventHandler(MouseEvent.MOUSE_CLICKED, event ->{
			mouseClicked=true;
			invalid_cursor();
		});

		addEventHandler(MouseEvent.MOUSE_DRAGGED, event ->{
			if(!dragDetected){
				dragDetected=true;
				dragStartX = event.getSceneX();
				dragStartPercent = percent;
			}else{
				double delta = dragStartX - event.getSceneX();
				percent = dragStartPercent-delta*100/gl_to_guard.getWidth();
				//CMN.Log(percent);
				col1.setPercentWidth(percent);
			}
		});

		setFill(Color.TRANSPARENT);
	}

	private void invalid_cursor() {
		getParent().setCursor(!mouseEntered && !mouseEntered?Cursor.DEFAULT:Cursor.H_RESIZE);
	}


}
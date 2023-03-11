package com.knziha.plod.plaindict.javafx;


import com.knziha.plod.plaindict.javafx.widgets.ObservableListmy;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.ListCell;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.util.regex.Matcher;


public class ColoredEntryCell extends ListCell<Integer> {
		public ObservableListmy adapter;
		public ColoredEntryCell(ObservableListmy adapter_){
			adapter=adapter_;
		}
		public EventHandler<MouseEvent> clicker = new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent event) {
				adapter.rec.renderContentAt(Integer.parseInt(((Node)event.getSource()).idProperty().getValue()), -1, false);
			}
		};
		@Override
		protected void updateItem(Integer pos, boolean empty) {
			super.updateItem(pos, empty);
			if(empty || pos==null)
				return;
			BorderPane cell = new BorderPane();

			String text = adapter.rec.getResAt(pos);
			Node textTitleView;
			AdvancedSearchLogicLayer layer = (AdvancedSearchLogicLayer) adapter.rec.SearchLauncher;
			if(layer.currentPattern!=null && layer.getTint()){
				TextFlow titleFlow = new TextFlow();
				textTitleView=titleFlow;
				ObservableList<Node> textGroup = titleFlow.getChildren();
				Matcher m = layer.currentPattern.matcher(text);
				Text title; int idx=0;
				while(m.find()){
					int start = m.start(0);
					int end = m.end(0);
					title = new Text(text.substring(idx, start));
					title.setFont(Font.font("宋体",18));
					title.setFill(Color.BLACK);
					textGroup.add(title);
					title = new Text(text.substring(start, end));
					title.setFont(Font.font("宋体",18));
					title.setFill(Color.RED);
					textGroup.add(title);
					idx=end;
				}
				if(idx<text.length()){
					title = new Text(text.substring(idx));
					title.setFont(Font.font("宋体",18));
					title.setFill(Color.BLACK);
					textGroup.add(title);
				}
			}
			else{
				textTitleView=new Text(text);
			}
			//title.setStyle("-fx-font-style:bold;");

			Text dictName = new Text(layer.md.get(adapter.rec.dictIdx).getDictionaryName());
			dictName.setFont(Font.font("宋体",12));
			dictName.setStyle("-fx-fill: #666666;-fx-opacity: 0.66;");
			//Text source = new Text("dd");
			//source.setFont(Font.font(10));

			cell.setTop(textTitleView);
			cell.setLeft(dictName);
			cell.setId(pos.toString());
			//cell.setRight(source);
			//cell.setOnMouseClicked(clicker);
			cell.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
				getListView().requestFocus();
				getListView().getSelectionModel().clearSelection();
				getListView().getSelectionModel().select(pos);
			});
			setGraphic(cell);
		}
	}
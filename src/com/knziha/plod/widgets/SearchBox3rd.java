package com.knziha.plod.widgets;
 
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;

public class SearchBox3rd extends SearchBox {

    public TextField textBox;
    Button upButton;
    Button downButton;
    Button clearButton;

    public SearchBox3rd() {
        setId("SearchBox");
    	getStylesheets().add(searchBoxCss);
        getStyleClass().add("search-box");
        upButton = new Button(); upButton.getStyleClass().add("up-button");
        downButton = new Button(); downButton.getStyleClass().add("down-button");
        clearButton = new Button(); clearButton.getStyleClass().add("clear-button");
        clearButton.setOnAction((ActionEvent actionEvent) -> {
			textBox.setText(null);
        });
        textBox = new TextField();
        textBox.setPromptText("Page");
        textBox.setOnDragDetected(this::startDrag);
        textBox.setOnDragOver(this::enterDrag);
        textBox.setOnDragDropped(this::handleDrop);
        textBox.setStyle("-fx-font-size: 12.8pt;");
        
        final ChangeListener<String> textListener =
            (ObservableValue<? extends String> observable,
             String oldValue, String newValue) -> {
            };
        textBox.textProperty().addListener(textListener);
        getChildren().addAll(textBox, upButton , downButton, clearButton);
    }


	@Override
    protected void layoutChildren() {
        int width=38;
        int height=38;
        double delta = 0;//getPadding().getTop()-getPadding().getBottom();
		double MarginTop = 0;
        int counter=0;
        int counter2=1;
    	clearButton.resize(width, height);
    	clearButton.relocate(getWidth() - width*1 - 3, MarginTop);
        
    	double marginRight = 5.5 * counter2;
        counter+=counter2;
        if(downButton.isVisible()) {
        	counter++;
        	downButton.resize(width, height);
        	downButton.relocate(getWidth() - width*counter - 6 -marginRight, MarginTop);
        }
        if(upButton.isVisible()) {
        	counter++;
	        upButton.resize(width, height);
	        upButton.relocate(getWidth() - width*counter - 6 -marginRight, MarginTop);
        }
        
        
        textBox.resize(getWidth(), 40);
        textBox.relocate(0, MarginTop);
    }
}
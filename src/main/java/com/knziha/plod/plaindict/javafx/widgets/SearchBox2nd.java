package com.knziha.plod.plaindict.javafx.widgets;
 
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;

public class SearchBox2nd extends SearchBox {

    TextField textBox;
    Button upButton;
    Button downButton;
    Button sideButton1;
    Button sideButton2;
    
    public SearchBox2nd() {
        setId("SearchBox");
    	getStylesheets().add(searchBoxCss);
        getStyleClass().add("search-box");
        upButton = new Button(); upButton.getStyleClass().add("up-button");
        downButton = new Button(); downButton.getStyleClass().add("down-button");
        sideButton1 = new Button(); sideButton1.getStyleClass().add("side-button1");
        sideButton2 = new Button(); sideButton2.getStyleClass().add("side-button2");
        setCombinedSearch(false);
        sideButton1.setOnAction((ActionEvent actionEvent) -> {
        	setCombinedSearch(false);
        });
        sideButton2.setOnAction((ActionEvent actionEvent) -> {
        	setCombinedSearch(true);
        });
        textBox = new TextField();
        textBox.setPromptText("Search");
        textBox.setOnDragDetected(this::startDrag);
        textBox.setOnDragOver(this::enterDrag);
        textBox.setOnDragDropped(this::handleDrop);
        textBox.setStyle("-fx-font-size: 12.8pt;");
        
        final ChangeListener<String> textListener =
            (ObservableValue<? extends String> observable,
             String oldValue, String newValue) -> {
            };
        textBox.textProperty().addListener(textListener);
        getChildren().addAll(textBox, upButton , downButton, sideButton1, sideButton2);
    }

	public final BooleanProperty isCombinedSearching = new SimpleBooleanProperty(false);
    public void setCombinedSearch(boolean val) {
		isCombinedSearching.set(val);
    	sideButton1.setVisible(val);
    	sideButton2.setVisible(!val);
	}

	@Override
    protected void layoutChildren() {
        int width=40;
        int height=40;
        double delta = 0;//getPadding().getTop()-getPadding().getBottom();
        double MarginTop = (getHeight()-height)/2+5-delta/2 + paddingTop;
        int counter=0;
        int counter2=1;
    	sideButton1.resize(width, height);
    	sideButton1.relocate(getWidth() - width*1 - 3, MarginTop);

    	sideButton2.resize(width, height);
    	sideButton2.relocate(getWidth() - width*1 - 3, MarginTop);
        
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
        
        
        textBox.resize(getWidth()-width * counter2 -marginRight, 40);
        textBox.relocate(0, MarginTop);
    }
}
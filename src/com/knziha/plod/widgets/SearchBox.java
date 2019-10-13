package com.knziha.plod.widgets;
 
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.Region;
import javafx.scene.text.Text;
import com.knziha.plod.widgets.splitpane.HiddenSplitPaneApp;

public class SearchBox extends Region {
	final static Text snapshotter = new Text();
    final static String searchBoxCss = HiddenSplitPaneApp.class.getResource("SearchBox.css").toExternalForm();
    public TextField textBox;
    public Button clearButton;
    public Button searchButton;
    public int paddingTop;
    public SearchBox() {
        setId("SearchBox");
    	getStylesheets().add(searchBoxCss);
        getStyleClass().add("search-box");
        clearButton = new Button(); clearButton.getStyleClass().add("clear-button");
        searchButton = new Button(); searchButton.getStyleClass().add("search-button");
        clearButton.setVisible(false);
        clearButton.setOnAction((ActionEvent actionEvent) -> {
            textBox.setText("");
            textBox.requestFocus();
        });
        textBox = new TextField();
        textBox.setPromptText("Search");
        textBox.setOnDragDetected(this::startDrag);
        textBox.setOnDragOver(this::enterDrag);
        textBox.setOnDragDropped(this::handleDrop);
        //textBox.setStyle("-fx-font-size: 16pt;");
        //textBox.setFont(Font.font(null, FontWeight.BOLD, 14));
        textBox.setStyle("-fx-font-size: 14pt;");

        final ChangeListener<String> textListener =
            (ObservableValue<? extends String> observable,
             String oldValue, String newValue) -> {
                clearButton.setVisible(textBox.getText().length() != 0);
            };
        textBox.textProperty().addListener(textListener);
        getChildren().addAll(textBox, clearButton , searchButton);
    }
 
    @Override
    protected void layoutChildren() {
        int width=40;
        int height=40;
        double delta = getPadding().getBottom();
        //CMN.show(delta+"");
        double MarginTop = (getHeight()-height)/2 + paddingTop;
        int counter=0;
        if(searchButton.isVisible()) {
        	counter++;
        	searchButton.resize(width, height);
        	searchButton.relocate(getWidth() - width*counter - 6, MarginTop);
        }
        if(clearButton.isVisible()) {
        	counter++;
	        clearButton.resize(width, height);
	        clearButton.relocate(getWidth() - width*counter - 6, MarginTop);
        }
        textBox.resize(getWidth(), height);
    }
    
    protected void startDrag(MouseEvent event) {
    	TextField textBox = ((TextField)event.getSource());
    	if(!"".equals(textBox.getSelectedText()) && textBox.getText().equals(textBox.getSelectedText())) {
            Dragboard db = textBox.startDragAndDrop(TransferMode.MOVE);
            snapshotter.setText(textBox.getText());
            db.setDragView(snapshotter.snapshot(null, null));
            ClipboardContent cc = new ClipboardContent();
            cc.put(DataFormat.PLAIN_TEXT, textBox.getSelectedText());
            db.setContent(cc);
            event.consume();
    	}
    }    
    protected void enterDrag(DragEvent event) {
		event.acceptTransferModes(TransferMode.MOVE);
	    event.consume();
    }

    protected void handleDrop(DragEvent event) {
        Dragboard db = event.getDragboard();
        if(db.hasContent(DataFormat.PLAIN_TEXT)) {
        	String val = (String) db.getContent(DataFormat.PLAIN_TEXT);
        	((TextField)event.getSource()).setText(val);
        	event.consume();
        }
    }
    
    
    
}
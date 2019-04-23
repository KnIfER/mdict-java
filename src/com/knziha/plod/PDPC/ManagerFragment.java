package com.knziha.plod.PDPC;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.Function;

import com.knziha.plod.dictionary.mdict;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.util.Callback;

public class ManagerFragment extends Region{
	Region MainView;
	String lastMdlibPath;
	DragSortListView<mdict> tableView;
	
	ManagerFragment(String lastMdlibPath_,ArrayList<mdict> md){
		super();
		lastMdlibPath=lastMdlibPath_;
		tableView = new DragSortListView<>();
		MainView=tableView;
        tableView.getColumns().add(createCol("名称", propertyMapper_Name, 150));
        tableView.getColumns().add(createCol("相对路径", propertyMapper_rName, 150));

        HashMap<String,mdict> mdict_cache = new HashMap<>();
        
        for(mdict mdTmp:md) {
			mdict_cache.put(mdTmp.getPath(),mdTmp);
			tableView.getItems().add(mdTmp);
		}
        
        
		File def = new File(PU.getProjectPath(),"default.txt");
        try {
			BufferedReader in = new BufferedReader(new FileReader(def));
	        String line = in.readLine();
	        int idx=0;
	        while(line!=null){
        		if(!line.startsWith("\\"))
        			line=lastMdlibPath+line;
        		if(!mdict_cache.containsKey(line)) {
        			if(idx<=md.size())
        				tableView.getItems().add(idx,new mdict_nonexist(line));//若是md加会memory报错。
        		}
        		idx++;
	        	line = in.readLine();
	        }
	        in.close();
	        
		} catch (IOException e2) {
			e2.printStackTrace();
		}
        
        
        
        
        
        
        
        tableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        
        
        getSions().add(tableView);
        
        
        
	}
	
	
	
	Function<mdict, ObservableValue<String>> propertyMapper_Name=new Function<mdict, ObservableValue<String>>(){
		@Override
		public ObservableValue<String> apply(mdict f) {
			return new SimpleStringProperty(f._Dictionary_fName);
		}};
		
	Function<mdict, ObservableValue<String>> propertyMapper_rName=new Function<mdict, ObservableValue<String>>(){
		@Override
		public ObservableValue<String> apply(mdict f) {
			String ret = f.getPath();
			if(ret.startsWith(lastMdlibPath))
				ret=ret.substring(lastMdlibPath.length());
			return new SimpleStringProperty(ret);
		}};
		
    private TableColumn<mdict, String> createCol(String title,  Function<mdict, ObservableValue<String>> mapper, double size) {
        
    	TableColumn<mdict, String> col = new TableColumn<>(title);
        
        col.setCellValueFactory(cellData -> mapper.apply(cellData.getValue()));
        
        col.setPrefWidth(size);

        Callback<TableColumn<mdict,String>, TableCell<mdict,String>> value = new Callback<TableColumn<mdict,String>, TableCell<mdict,String>>(){
			@Override
			public TableCell<mdict, String> call(TableColumn<mdict, String> param) {
				 return new TableCell<mdict, String>() {
	                    @Override
	                    protected void updateItem(String item, boolean empty) {
	                        if (!empty) {
	                            int currentIndex = indexProperty()
	                                    .getValue() < 0 ? 0
	                                    : indexProperty().getValue();
	                            String clmStatus = param
	                                    .getTableView().getItems()
	                                    .get(currentIndex).getPath();
	                            if(new File(clmStatus).exists()) {
	                                setTextFill(Color.BLACK);
	                                setStyle("-fx-font-weight: bold");
	                                setStyle("-fx-background-color: transparent");
	                                setText(item);
	                            }else {
	                                setTextFill(Color.WHITE);
	                                setStyle("-fx-font-weight: bold");
	                                setStyle("-fx-background-color: red");
	                                setText(item);
	                            }
	                        }
	                    }
	                };
			}};
        col.setCellFactory(value);
        
        return col ;
    }
    
    @Override
    protected void layoutChildren() {
    	MainView.resize(getWidth(), getHeight());
        //clearButton.resizeRelocate(getWidth() - 18, 6, 12, 13);
    }
	
	public ObservableList<Node> getSions(){
		return getChildren();
	}
	
}

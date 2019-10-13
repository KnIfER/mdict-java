package com.knziha.plod.widgets;


import java.util.ArrayList;

import javafx.collections.ObservableList;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;

public class DragSortListView<S> extends TableView<S>{
    private static final DataFormat SERIALIZED_MIME_TYPE = new DataFormat("application/x-java-serialized-object");

	private ArrayList<S> selections = new ArrayList<>();

    public boolean isDirty=false;
	
    public DragSortListView() {
        super();
        init();
    }
    
    public DragSortListView(ObservableList<S> items) {
    	super(items);
    	init();
    }
    
    private void init(){        
    	setRowFactory(tv -> {
        TableRow<S> row = new TableRow<>();

        row.setOnDragDetected(event -> {
            if (! row.isEmpty()) {
                Integer index = row.getIndex();
                //CMN.show("OnDragDetected "+index);
                selections.clear();
                ObservableList<S> items = getSelectionModel().getSelectedItems();
                //CMN.show(items.size()+"");
                //ObservableList<Integer> idxes = getSelectionModel().getSelectedIndices();
                //for(Integer iI:idxes)
                for(S iI:items) {
                	//CMN.show(iI.getFirstName());
                	selections.add(iI);
                }
                
                
                Dragboard db = row.startDragAndDrop(TransferMode.MOVE);
                db.setDragView(row.snapshot(null, null));
                ClipboardContent cc = new ClipboardContent();
                cc.put(SERIALIZED_MIME_TYPE, index);
                db.setContent(cc);
                event.consume();
            }
        });

        row.setOnDragOver(event -> {
            //CMN.show("OnDragOver ");
            Dragboard db = event.getDragboard();
            if (db.hasContent(SERIALIZED_MIME_TYPE)) {
                if (row.getIndex() != ((Integer)db.getContent(SERIALIZED_MIME_TYPE)).intValue()) {
                    event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
                    event.consume();
                }
            }
        });

        row.setOnDragDropped(event -> {
            //CMN.show("OnDragDropped ");
            isDirty=true;
            Dragboard db = event.getDragboard();
            if (db.hasContent(SERIALIZED_MIME_TYPE)) {
            	
                int dropIndex;S dI=null; 
            	
                if (row.isEmpty()) {
                    dropIndex = getItems().size() ;
                } else {
                    dropIndex = row.getIndex();
                    dI = getItems().get(dropIndex);
                }
                int delta=0;
                if(dI!=null)
                while(selections.contains(dI)) {
                	delta=1;
                	--dropIndex;
                	if(dropIndex<0) {
                		dI=null;dropIndex=0;
                		break;
                	}
                	dI = getItems().get(dropIndex);
                }
                
                for(S sI:selections) {
                    getItems().remove(sI);
                }
                
                if(dI!=null)
                	dropIndex=getItems().indexOf(dI)+delta;
                else if(dropIndex!=0)
                	dropIndex=getItems().size();
                
                
                //int draggedIndex = (Integer) db.getContent(SERIALIZED_MIME_TYPE);
                getSelectionModel().clearSelection();
                
                for(S sI:selections) {
                	//draggedIndex = selections.get(i);
                    getItems().add(dropIndex, sI);
                    getSelectionModel().select(dropIndex);
                    dropIndex++;
                
                }
                
                event.setDropCompleted(true);
                selections.clear();
                event.consume();
            }
        });

        return row ;
    });
    	
    }
    
    
    
}

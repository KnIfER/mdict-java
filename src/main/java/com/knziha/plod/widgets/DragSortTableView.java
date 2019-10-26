package com.knziha.plod.widgets;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.input.*;

import java.util.ArrayList;
import java.util.HashSet;

/** A drag-sort table view that supports multi-selection.
 */
public class DragSortTableView<S> extends TableView<S>{
    private static final DataFormat SERIALIZED_MIME_TYPE = new DataFormat("application/x-java-serialized-object");
    public TableRow<S> contextMenuLauncher;

	private HashSet<S> selections = new HashSet<>();

    public boolean isDirty=false;

    private int dragFrom;

    ContextMenu mContextMenu;

    public DragSortTableView() {
        this(FXCollections.<S>observableArrayList());
    }

    public DragSortTableView(ObservableList<S> items) {
        super(items);
        init();
    }

    /** See {@link MouseEvent#DRAG_DETECTED}<br/>{@link DragEvent#DRAG_OVER}<br/>{@link DragEvent#DRAG_DROPPED}
     */
    private void init(){
        EventHandler<InputEvent> dragHandler = event -> {
            TableRow<S> row_ = (TableRow<S>) event.getSource();
            switch (event.getEventType().getName()) {
                case "DRAG_DETECTED":
                    if(((MouseEvent)event).getButton()==MouseButton.PRIMARY) {
                        dragFrom = row_.getIndex();
                        if (!row_.isEmpty()) {
                            selections.clear();
                            selections.addAll(getSelectionModel().getSelectedItems());
                            Dragboard db = row_.startDragAndDrop(TransferMode.MOVE);
                            db.setDragView(row_.snapshot(null, null));
                            ClipboardContent cc = new ClipboardContent();
                            cc.put(SERIALIZED_MIME_TYPE, dragFrom);
                            db.setContent(cc);
                            event.consume();
                        }
                    }
                    break;
                case "DRAG_OVER":
                    DragEvent ev = (DragEvent) event;
                    Dragboard db = ev.getDragboard();
                    if (db.hasContent(SERIALIZED_MIME_TYPE)) {
                        if (row_.getIndex() != ((Integer) db.getContent(SERIALIZED_MIME_TYPE))) {
                            ev.acceptTransferModes(TransferMode.COPY_OR_MOVE);
                            ev.consume();
                        }
                    }
                    break;
                case "DRAG_DROPPED":
                    ev = (DragEvent) event;
                    db = ev.getDragboard();
                    isDirty = true;
                    if (db.hasContent(SERIALIZED_MIME_TYPE)) {
                        ObservableList<S> mItems = getItems();
                        int dragTo=row_.isEmpty()?mItems.size()-1:row_.getIndex();
                        //CMN.Log(dragFrom, dragTo);

                        ArrayList<S> mSelected = new ArrayList<>(selections.size());
                        if(dragTo> dragFrom) dragTo++;
                        for (int i = mItems.size()-1; i >= 0; i--) {
                            S S = mItems.get(i);
                            if(selections.contains(S)){
                                mSelected.add(0,mItems.remove(i));//keep in order
                                if(i<dragTo) dragTo--;
                            }
                        }

                        //order of a hashset is unknown
                        mItems.addAll(dragTo, mSelected);
                        getSelectionModel().clearSelection();
                        getSelectionModel().selectRange(dragTo, dragTo+selections.size());

                        selections.clear();
                        ev.setDropCompleted(true);
                        ev.consume();
                    }
                break;
            }
        };

        addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            if(mContextMenu!=null)
                mContextMenu.hide();
        });

        EventHandler<? super ContextMenuEvent> contextMenuHandler=event->{
            if(mContextMenu!=null){
                TableRow<S> row_ = (TableRow<S>) event.getSource();
                contextMenuLauncher=row_;
                if(!row_.isEmpty()){
                    mContextMenu.show(this, event.getScreenX(), event.getScreenY());
                }
            }
        };

        setRowFactory(tv -> {
            TableRow<S> row = new TableRow<>();
            row.setOnDragDetected(dragHandler);
            row.setOnDragOver(dragHandler);
            row.setOnDragDropped(dragHandler);
            row.setOnContextMenuRequested(contextMenuHandler);
            if(mRowAdapter!=null)
                mRowAdapter.onBindView(row);
            return row;
        });
    }

    public void setItemContextMenu(ContextMenu _ContextMenu){
        mContextMenu=_ContextMenu;
    }
    public void setRowAdapter(RowAdapter _RowAdapter){
        mRowAdapter=_RowAdapter;
    }
    public interface RowAdapter{
        void onBindView(TableRow row);
    }
    RowAdapter mRowAdapter;
}

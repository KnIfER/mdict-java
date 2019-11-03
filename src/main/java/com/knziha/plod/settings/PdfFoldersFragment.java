package com.knziha.plod.settings;

import com.knziha.plod.PlainDict.PU;
import com.knziha.plod.PlainDict.PlainDictAppOptions;
import com.knziha.plod.dictionarymodels.mdict;
import com.knziha.plod.dictionarymodels.mdict_nonexist;
import com.knziha.plod.dictionarymodels.mdict_preempter;
import com.knziha.plod.widgets.DragSortTableView;
import com.knziha.plod.widgets.splitpane.HiddenSplitPaneApp;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.MenuItem;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;
import javafx.util.Callback;

import java.awt.*;
import java.io.*;
import java.text.DecimalFormat;
import java.util.HashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.Function;

/** dictionary manager ui*/
public class PdfFoldersFragment extends VBox {
	private File lastOpenDir;
	public HBox mainRegion;
	public Region tableRegion;
	public VBox toolsRegion;
	public DragSortTableView<File> tableView;
	public HashSet<File> rejector = new HashSet<>();
	public HashSet<File> tree = new HashSet<>();
	PlainDictAppOptions opt;

	static class tableParent extends Region{
		tableParent(DragSortTableView tv){
			super();
			if(tv!=null)
				getChildren().add(tv);
		}
		@Override
		protected void layoutChildren() {
			if(getChildren().size()>0)
				getChildren().get(0).resize(getWidth(), getHeight());
		}
	}

	@SuppressWarnings("unchecked")
	public PdfFoldersFragment(PlainDictAppOptions _opt){
		super();
		mainRegion=new HBox();
		ResourceBundle bundle = ResourceBundle.getBundle("UIText", Locale.getDefault());
		opt=_opt;
		// + status bar
		Text statusBar = new Text();
		statusBar.setText("Ready.");
		statusBar.setUnderline(true);
		// + table
		tableView = new DragSortTableView<>();
		ContextMenu contextMenu = new ContextMenu();
		String[] allItems = new String[]{
				disable,
				enable,
				remove,
				setastree,
				openlocation
		};
		ObservableList<MenuItem> items = contextMenu.getItems();
		for(String mI:allItems){
			MenuItem item = new MenuItem(bundle.getString(mI));
			item.setId(mI);
			items.add(item);
		}
		contextMenu.setOnAction(event -> {
			String id=event.getTarget().toString();
			int idx=id.indexOf("id=");
			id=id.substring(idx+3, id.indexOf(",",idx));
			switch(id){
				case disable:
					ObservableList<File> selItems = tableView.getSelectionModel().getSelectedItems();
					for(File mdTmp:selItems)
						rejector.add(mdTmp);
					tableView.isDirty=true;
					tableView.refresh();
				break;
				case enable:{
					selItems = tableView.getSelectionModel().getSelectedItems();
					for(File mdTmp:selItems)
						rejector.remove(mdTmp);
					tableView.isDirty=true;
					tableView.refresh();
				} break;
				case remove:{
					Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
					alert.setHeaderText("Sure to remove selected items from current set?");
					Optional<ButtonType> result = alert.showAndWait();
					if (result.isPresent() && result.get() == ButtonType.OK){
						selItems = tableView.getItems();
						selItems.removeAll(tableView.getSelectionModel().getSelectedItems());
						tableView.getSelectionModel().clearSelection();
						tableView.isDirty=true;
						tableView.refresh();
					}
				} break;
				case setastree:{
					if(!tableView.contextMenuLauncher.isEmpty()){
						selItems = tableView.getItems();
						int index=tableView.contextMenuLauncher.indexProperty().get();
						File item=selItems.get(index);
						if(!tree.contains(item)) tree.add(item);
						else tree.remove(item);
						tableView.isDirty=true;
						tableView.refresh();
					}
				} break;
				case openlocation:{
					if(!tableView.contextMenuLauncher.isEmpty()){
						selItems = tableView.getItems();
						int index=tableView.contextMenuLauncher.indexProperty().get();
						if(index>selItems.size()) index=0;
						try {
							Desktop.getDesktop().open(selItems.get(index));// what a shame
						} catch (Exception e) { e.printStackTrace(); }
						break;
					}
				} break;
			}
		});
		tableView.setItemContextMenu(contextMenu);
        tableView.getColumns().add(createCol(bundle.getString("name"),propertyMapper_FilePath, -1, null));
        tableView.getColumns().add(createCol(bundle.getString(setastree), propertyMapper_isTree, -1, null));


		File def = new File(PU.getProjectPath(),"CONFIG/PDFolders.lst");
		if(def.exists())
        try {
			BufferedReader in = new BufferedReader(new FileReader(def));
	        String line;
	        int idx=0;
			ObservableList<File> mdModifying = tableView.getItems();
			while((line = in.readLine())!=null){
        		if(line.length()>0){
					boolean isTree = false, disabled=false;
					if (line.startsWith("[:")) {
						int nextbrace=line.indexOf("]",2);
						if(nextbrace>=3){
							String[] args = line.substring(2, nextbrace).split(":");
							for (int i = 0; i < args.length; i++) {
								switch (args[i]){
									case "T":
										isTree = true;
									break;
									case "D":
										disabled = true;
									break;
								}
							}
						}
						if(nextbrace!=-1)
							line = line.substring(nextbrace+1);
					}
					File f=new File(line);
					if(disabled){
						rejector.add(f);
					}
					if(isTree){
						tree.add(f);
					}
					mdModifying.add(f);
					idx++;
				}
	        }
	        in.close();
		} catch (IOException e2) {
			e2.printStackTrace();
		}

        tableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

		// + tools
		toolsRegion = new VBox();
		toolsRegion.setPadding(new Insets(0,0,0,15));
		Insets padding = new Insets(10, 0, 5, 0);
		Button btnTmp;

		allItems=new String[]{
				add,
				cancelmod
		};
		EventHandler<ActionEvent> clicker=event1 -> {
			switch(((Node) event1.getSource()).getId()){
				case add:{
					DirectoryChooser fileChooser = new DirectoryChooser();
					File startPath=lastOpenDir!=null?lastOpenDir:new File(opt.GetLastMdlibPath());
					if(!startPath.exists())
						startPath=new File(opt.projectPath);
					fileChooser.setInitialDirectory(startPath);
					File files = fileChooser.showDialog(getScene().getWindow());
					if(files!=null)
						if(!tableView.getItems().contains(files)) {
							tableView.getItems().add(files);
							tableView.isDirty=true;
						}
				} break;
				case cancelmod:
				break;
			}
		};
		for(String bI:allItems){
			btnTmp = new Button(bundle.getString(bI));
			btnTmp.setId(bI);
			btnTmp.setOnAction(clicker);
			VBox.setMargin(btnTmp, padding);
			toolsRegion.getChildren().add(btnTmp);
		}

		// + children and stylizing
		final String hidingSplitPaneCss = HiddenSplitPaneApp.class.getResource("HiddenSplitPane.css").toExternalForm();
		tableRegion=new tableParent(tableView);
		tableView.setId("hiddenSplitter");
		tableRegion.getStyleClass().add("rounded");
		setId("hiddenSplitter");
		getStylesheets().add(hidingSplitPaneCss);
		mainRegion.getChildren().addAll(tableRegion, toolsRegion);
		mainRegion.setHgrow(tableRegion, Priority.ALWAYS);
		setVgrow(mainRegion, Priority.ALWAYS);
		getChildren().addAll(mainRegion, statusBar);
	}

	Function<File, ObservableValue<String>> propertyMapper_FilePath=m -> new SimpleStringProperty(m.getAbsolutePath());
	Function<File, ObservableValue<Boolean>> propertyMapper_isTree=m -> new SimpleBooleanProperty(tree.contains(m));

	DecimalFormat time_machine = new DecimalFormat("#.00");

	<T> TableColumn<File, T> createCol(String title,
						   Function<File, ObservableValue<T>> mapper, double prefSize,
	Callback<TableColumn<File,T>, TableCell<File,T>> bindviewCallback) {
    	TableColumn<File, T> col = new TableColumn<>(title);
        col.setCellValueFactory(cellData -> mapper.apply(cellData.getValue()));
        if(prefSize>0) col.setPrefWidth(prefSize);
        if(bindviewCallback==null)
		bindviewCallback =
			param -> new TableCell<File, T>() {
			@Override
			protected void updateItem(T item, boolean empty) {
				if (!empty) {
					int currentIndex=indexProperty().getValue();
					if(currentIndex<0) currentIndex=0;
					File mdTmp = param.getTableView().getItems().get(currentIndex);
					if(!mdTmp.isDirectory()){
						setStyle("-fx-background-color: #ffb6c1ad");
					} else{
						setStyle("-fx-background-color: transparent");
					}
					setOpacity(rejector.contains(mdTmp) ? 0.5 : 1);
					String value1 = null;
					if(item instanceof Boolean)
						value1 = ((Boolean)item)?"√":"";
					if(value1 ==null)
						value1 = String.valueOf(item);
					setText(value1);
				}else
					setText(null);
			}
		};
        col.setCellFactory(bindviewCallback);
        return col ;
    }

	public boolean try_write_configureLet(File newf) {
		try {
			ObservableList<File> mdModified = tableView.getItems();
			BufferedWriter out = new BufferedWriter(new FileWriter(newf,false));
			for(File mdTmp:mdModified) {
				String name = mdTmp.getPath();
				boolean isTree=tree.contains(mdTmp), disabled=rejector.contains(name);
				if(isTree||disabled){
					out.write("[");
					if(isTree) out.write(":T");
					if(disabled) out.write(":D");
					out.write("]");
				}
				out.write(name);
				out.write("\n");
			}
			out.flush();
			out.close();
			return true;
		} catch (IOException e2) {
			e2.printStackTrace();
		}
		return false;
	}


	final static String disable="disable";
	final static String enable="enable";
	final static String remove="remove";
	final static String setastree="setastree";
	final static String openlocation="openlocation";


	public final static String add="add";
	public final static String commit ="commit";
	public final static String reset ="reset";
	public final static String refresh ="refresh";
	public final static String saveas="saveas";
	public final static String switchset="switchset";
	public final static String cancelmod="cancelmod";
}

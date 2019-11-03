package com.knziha.plod.dictionarymanager;

import com.knziha.plod.PlainDict.MdictServer;
import com.knziha.plod.PlainDict.PU;
import com.knziha.plod.PlainDict.PlainDictAppOptions;
import com.knziha.plod.PlainDict.PlainDictionaryPcJFX;
import com.knziha.plod.dictionarymodels.mdict;
import com.knziha.plod.dictionarymodels.mdict_nonexist;
import com.knziha.plod.dictionarymodels.mdict_preempter;
import com.knziha.plod.widgets.DragSortTableView;
import com.knziha.plod.widgets.splitpane.HiddenSplitPaneApp;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Callback;
import test.CMN;

import java.awt.*;
import java.io.*;
import java.text.DecimalFormat;
import java.util.*;
import java.util.List;
import java.util.function.Function;

/** dictionary manager ui*/
public class ManagerFragment extends VBox {
	private final HashMap<String, mdict> mdict_cache;
	private File lastOpenDir;
	public HBox mainRegion;
	public Region tableRegion;
	public VBox toolsRegion;
	public DragSortTableView<mdict> tableView;
	public HashSet<String> rejector = new HashSet<>();
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
	public ManagerFragment(PlainDictionaryPcJFX app, MdictServer server, PlainDictAppOptions _opt){
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
				rename,
				remove,
				setasformation,
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
					ObservableList<mdict> selItems = tableView.getSelectionModel().getSelectedItems();
					for(mdict mdTmp:selItems)
						rejector.add(mdTmp.getPath());
					tableView.isDirty=true;
					tableView.refresh();
				break;
				case enable:{
					selItems = tableView.getSelectionModel().getSelectedItems();
					for(mdict mdTmp:selItems)
						rejector.remove(mdTmp.getPath());
					tableView.isDirty=true;
					tableView.refresh();
				} break;
				case rename:{
					selItems = tableView.getItems();
					if(!tableView.contextMenuLauncher.isEmpty()){
						int index=tableView.contextMenuLauncher.indexProperty().get();
						mdict mdTmp = tableView.getItems().get(index);
						if(index<selItems.size()){
							FileChooser fileChooser = new FileChooser();
							fileChooser.getExtensionFilters().addAll(
									new FileChooser.ExtensionFilter("Mdict file", "*.mdx")
							);
							fileChooser.setInitialDirectory(mdTmp.f().getParentFile());
							fileChooser.setInitialFileName(mdTmp.f().getName());
							File new_file = fileChooser.showSaveDialog(getScene().getWindow());
							if(new_file!=null){
								CMN.Log(new_file);
								String fn = new_file.getName(),suffix="";
								int suffix_index=fn.indexOf(".");
								if(suffix_index!=-1) {
									fn=fn.substring(0, suffix_index);
									suffix=fn.substring(suffix_index);
								}
								boolean renamed=false;
								if(mdTmp instanceof mdict_nonexist || !mdTmp.f().exists()){
									if(new_file.exists()) {
										try {
											tableView.getItems().set(index, new mdict_preempter(new_file.getAbsolutePath()));
											renamed=true;
										} catch (IOException e) {
											e.printStackTrace();
										}
									}
								}
								if(!renamed)
									mdTmp.renameFileTo(new_file);
								tableView.refresh();
								tableView.isDirty=true;
							}
						}
					}
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
				case setasformation:{
					selItems = tableView.getItems();
					if(!tableView.contextMenuLauncher.isEmpty()){
						int index=tableView.contextMenuLauncher.indexProperty().get();
						if(index>selItems.size()) index=0;
						boolean val=!selItems.get(index).tmpIsFilter;
						selItems = tableView.getSelectionModel().getSelectedItems();
						for(mdict mdTmp:selItems)
							mdTmp.tmpIsFilter=val;
						tableView.isDirty=true;
						tableView.refresh();
					}
				} break;
				case openlocation:{
					selItems = tableView.getItems();
					if(!tableView.contextMenuLauncher.isEmpty()){
						int index=tableView.contextMenuLauncher.indexProperty().get();
						if(index>selItems.size()) index=0;
						try {
							Desktop.getDesktop().open(selItems.get(index).f().getParentFile());// what a shame
						} catch (Exception e) { e.printStackTrace(); }
						break;
					}
				} break;
			}
		});
		tableView.setItemContextMenu(contextMenu);
		TableColumn col;
        tableView.getColumns().add(col=createCol(bundle.getString("name"), mdict::getFileNameProperty, -1, null));
        tableView.getColumns().add(createCol(bundle.getString("relpath"), propertyMapper_FilePath, -1, col.getCellFactory()));
        tableView.getColumns().add(createCol(bundle.getString("filesize"), mdict::getFileSizeProperty, -1, null));
        tableView.getColumns().add(createCol(bundle.getString("setasformation"), mdict::getFormationProperty, -1, null));
        mdict_cache = new HashMap<>(server.md.size());
        HashMap<String,mdict> filter_cache = new HashMap<>(server.currentFilter.size());

        for(mdict mdTmp:server.md) {
			mdict_cache.put(mdTmp.getPath(),mdTmp);
		}
        for(mdict mdTmp:server.currentFilter) {
			filter_cache.put(mdTmp.getPath(),mdTmp);
		}
		tableView.getItems().addAll(server.md);

		File def = app.getCurrentSetFile();
        try {
			BufferedReader in = new BufferedReader(new FileReader(def));
	        String line;
	        int idx=0;
			ObservableList<mdict> mdModifying = tableView.getItems();
			while((line = in.readLine())!=null){
        		if(line.length()>0){
					boolean isFilter = false, disabled=false;
					if (line.startsWith("[:")) {
						int nextbrace=line.indexOf("]",2);
						if(nextbrace>=3){
							String[] args = line.substring(2, nextbrace).split(":");
							for (int i = 0; i < args.length; i++) {
								switch (args[i]){
									case "F":
										isFilter = true;
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
					if(!PlainDictionaryPcJFX.windowPath.matcher(line).matches() && !line.startsWith("/"))
						line=opt.GetLastMdlibPath()+File.separator+line;
					if(disabled){
						rejector.add(line);
					}
					if(!mdict_cache.containsKey(line)) {
						mdict mdTmp=filter_cache.get(line);
						if(mdTmp==null)
							mdTmp=new_mdict_prempter(line, isFilter);
						mdModifying.add(Math.min(mdModifying.size(), idx), mdTmp);
					}
					idx++;
				}
	        }
	        in.close();
		} catch (IOException e2) {
			e2.printStackTrace();
		}

		mdict_cache.putAll(filter_cache);
        tableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

		// + tools
		toolsRegion = new VBox();
		toolsRegion.setPadding(new javafx.geometry.Insets(0,0,0,15));
		javafx.geometry.Insets padding = new Insets(10, 0, 5, 0);
		Button btnTmp;

		allItems=new String[]{
			add,
			commit,
			reset,
			saveas,
			switchset,
			cancelmod
		};
		EventHandler<ActionEvent> clicker=event1 -> {
			switch(((Node) event1.getSource()).getId()){
				case add:{
					FileChooser fileChooser = new FileChooser();
					fileChooser.getExtensionFilters().addAll(
							new FileChooser.ExtensionFilter("mdict file", "*.mdx"),
							new FileChooser.ExtensionFilter("mdict resource file", "*.mdd")
					);
					File startPath=null;
					mdict mdTmp = tableView.getSelectionModel().getSelectedItem();
					if(mdTmp!=null)
						startPath=mdTmp.f().getParentFile();
					if(startPath==null || !startPath.exists())
						startPath=lastOpenDir!=null?lastOpenDir:new File(opt.GetLastMdlibPath());
					if(!startPath.exists())
						startPath=new File(opt.projectPath);
					fileChooser.setInitialDirectory(startPath);
					List<File> files = fileChooser.showOpenMultipleDialog(getScene().getWindow());
					if(files!=null){
						ObservableList<mdict> mdModifying = tableView.getItems();
						lastOpenDir = files.get(0).getParentFile();
						for(File fI:files){
							tableView.isDirty=true;
							if(!mdict_cache.containsKey(fI.getAbsolutePath())){
								try {
									mdModifying.add(new mdict_preempter(fI.getAbsolutePath()));
								} catch (IOException e) {
									e.printStackTrace();
								}
							}
						}
					}
				} break;
				case commit:{
					File target = new File(PU.getProjectPath(),"CONFIG/"+opt.getCurrentPlanName()+".set");
					if(try_write_configureLet(target))
						statusBar.setText(opt.getCurrentPlanName()+" >> saved");
					else
						statusBar.setText("Some error occurred!");
				} break;
				case reset:{
					File target = new File(PU.getProjectPath(),"CONFIG/"+opt.getCurrentPlanName()+".set");
					try_read_configureLet(target);
					tableView.isDirty=true;
					tableView.refresh();
					statusBar.setText("reset << "+opt.getCurrentPlanName());
				} break;
				case saveas:{
					FileChooser fileChooser = new FileChooser();
					fileChooser.getExtensionFilters().addAll(
							new FileChooser.ExtensionFilter("Dictionary Set", "*.set")
					);
					fileChooser.setInitialDirectory(new File(PlainDictAppOptions.projectPath, "CONFIG"));
					fileChooser.setInitialFileName(opt.getCurrentPlanName());
					File file = fileChooser.showSaveDialog(getScene().getWindow());
					if(file!=null){
						if(try_write_configureLet(file)){
							String fn = file.getName();
							opt.setCurrentPlanName(fn.substring(0,fn.length()-4));
							statusBar.setText(opt.getCurrentPlanName()+" >> saved");
							((Stage)getScene().getWindow()).setTitle(bundle.getString(PlainDictionaryPcJFX.UI.manager)+" - "+opt.getCurrentPlanName());
						}
					}
				} break;
				case switchset:{
					FileChooser fileChooser = new FileChooser();
					fileChooser.getExtensionFilters().addAll(
							new FileChooser.ExtensionFilter("Dictionary Set", "*.set")
					);
					fileChooser.setInitialDirectory(new File(PlainDictAppOptions.projectPath, "CONFIG"));
					fileChooser.setInitialFileName(opt.getCurrentPlanName());
					File file = fileChooser.showOpenDialog(getScene().getWindow());
					if(file!=null){
						if(try_read_configureLet(file)){
							String fn = file.getName();
							opt.setCurrentPlanName(fn.substring(0,fn.length()-4));
							statusBar.setText("loaded << " + opt.getCurrentPlanName());
							((Stage)getScene().getWindow()).setTitle(bundle.getString(PlainDictionaryPcJFX.UI.manager)+" - "+opt.getCurrentPlanName());
						}
					}
				} break;
				case cancelmod:{
					tableView.isDirty=false;
					((Stage)getScene().getWindow()).close();
				} break;
				case s_all:{
					tableView.getSelectionModel().selectRange(0, tableView.getItems().size());
				} break;
				case s_none:{
					tableView.getSelectionModel().clearSelection();
				} break;
				case s_disabled:{
					//cannot detect ctrl-click
					ObservableList<mdict> mdModifying = tableView.getItems();
					tableView.getSelectionModel().clearSelection();
					for(int i=0;i<mdModifying.size();i++){
						mdict mdTmp = mdModifying.get(i);
						if(rejector.contains(mdTmp.getPath())){
							tableView.getSelectionModel().select(i);
						}
					}
				} break;
				case s_invalid:{
					ObservableList<mdict> mdModifying = tableView.getItems();
					tableView.getSelectionModel().clearSelection();
					for(int i=0;i<mdModifying.size();i++){
						mdict mdTmp = mdModifying.get(i);
						if(mdTmp instanceof mdict_nonexist || !mdTmp.f().exists()){
							tableView.getSelectionModel().select(i);
						}
					}
				} break;
			}
		};
		ObservableList<Node> childrenNodes = toolsRegion.getChildren();
		for(String bI:allItems){
			childrenNodes.add(new_button(bI, bundle, clicker, padding));
		}

		Separator separator = new Separator();
		separator.setPadding(padding);
		separator.setOrientation(Orientation.HORIZONTAL);
		childrenNodes.add(separator);

		allItems=new String[]{
			s_invalid,
			s_disabled,
			s_all,
			s_none
		};
		for(String bI:allItems){
			childrenNodes.add(new_button(bI, bundle, clicker, padding));
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

	private Node new_button(String bI, ResourceBundle bundle, EventHandler<ActionEvent> clicker, Insets padding) {
		Button btnTmp = new Button(bundle.getString(bI));
		btnTmp.setId(bI);
		btnTmp.setOnAction(clicker);
		VBox.setMargin(btnTmp, padding);
		return btnTmp;
	}

	public static mdict new_mdict_prempter(String line, boolean isFilter) throws IOException {
		mdict mdTmp;
		if(!new File(line).exists())
			mdTmp=new mdict_nonexist(line);
		else
			mdTmp=new mdict_preempter(line);
		mdTmp.tmpIsFilter=isFilter;
		return mdTmp;
	}

	Function<mdict, ObservableValue<String>> propertyMapper_FilePath=new Function<mdict, ObservableValue<String>>(){
		@Override
		public ObservableValue<String> apply(mdict m) {
			String ret = m.getPath();
			if(ret.startsWith(opt.GetLastMdlibPath()+File.separator))
				ret=ret.substring(opt.GetLastMdlibPath().length()+1);
			return new SimpleStringProperty(ret);
	}};

	DecimalFormat time_machine = new DecimalFormat("#.00");

	<T> TableColumn<mdict, T> createCol(String title,
						   Function<mdict, ObservableValue<T>> mapper, double prefSize,
	Callback<TableColumn<mdict,T>, TableCell<mdict,T>> bindviewCallback) {
    	TableColumn<mdict, T> col = new TableColumn<>(title);
        col.setCellValueFactory(cellData -> mapper.apply(cellData.getValue()));
        if(prefSize>0) col.setPrefWidth(prefSize);
        if(bindviewCallback==null)
		bindviewCallback =
			param -> new TableCell<mdict, T>() {
			@Override
			protected void updateItem(T item, boolean empty) {
				if (!empty) {
					int currentIndex=indexProperty().getValue();
					if(currentIndex<0) currentIndex=0;
					mdict mdTmp = param.getTableView().getItems().get(currentIndex);
					String clmPath = mdTmp.getPath();
					if(mdTmp instanceof mdict_nonexist || !mdTmp.f().exists()){
						setStyle("-fx-background-color: #ffb6c1ad");
					} else{
						setStyle("-fx-background-color: transparent");
					}
					//CMN.Log(clmPath, rejector.contains(clmPath), getTextFill());
					setOpacity(rejector.contains(clmPath) ? 0.5 : 1);
					String value1 = null;
					if(item instanceof Double)
						value1 = time_machine.format(item);
					else if(item instanceof Boolean)
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

	public boolean try_read_configureLet(File newf) {
		try {
			BufferedReader in = new BufferedReader(new FileReader(newf));
			String line;
			int idx=0;
			ObservableList<mdict> mdModifying = tableView.getItems();
			mdModifying.clear();
			while((line = in.readLine())!=null){
				if(line.length()>0){
					boolean isFilter = false, disabled=false;
					if (line.startsWith("[:")) {
						int nextbrace=line.indexOf("]",2);
						if(nextbrace>=3){
							String[] args = line.substring(2, nextbrace).split(":");
							for (int i = 0; i < args.length; i++) {
								switch (args[i]){
									case "F":
										isFilter = true;
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
					if(!PlainDictionaryPcJFX.windowPath.matcher(line).matches() && !line.startsWith("/"))
						line=opt.GetLastMdlibPath()+File.separator+line;
					if(disabled){
						rejector.add(line);
					}
					mdict mdTmp = mdict_cache.get(line);
					if(mdTmp==null)
						mdTmp=new_mdict_prempter(line, isFilter);
					mdModifying.add(mdTmp);
					idx++;
				}
			}
			in.close();
			return true;
		} catch (IOException e2) {
			e2.printStackTrace();
		}
		return false;
	}

	public boolean try_write_configureLet(File newf) {
		try {
			ObservableList<mdict> mdModified = tableView.getItems();
			BufferedWriter out = new BufferedWriter(new FileWriter(newf,false));
			String parent = new File(opt.GetLastMdlibPath()).getAbsolutePath()+File.separatorChar;
			for(mdict mdTmp:mdModified) {
				String name = mdTmp.getPath();
				boolean isFiler=mdTmp.tmpIsFilter, disabled=rejector.contains(name);
				if(name.startsWith(parent))
					name = name.substring(parent.length());
				if(isFiler||disabled){
					out.write("[");
					if(isFiler) out.write(":F");
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
	final static String rename="rename";
	final static String remove="remove";
	final static String relocate="relocate";
	final static String setasformation="setasformation";
	final static String openlocation="openlocation";


	public final static String add="add";
	public final static String commit ="commit";
	public final static String reset ="reset";
	public final static String refresh ="refresh";
	public final static String saveas="saveas";
	public final static String switchset="switchset";
	public final static String cancelmod="cancelmod";
	public final static String s_invalid="s_invalid";
	public final static String s_disabled="s_disabled";
	public final static String s_all="s_all";
	public final static String s_none="s_none";
}

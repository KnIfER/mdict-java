package com.knziha.plod.dictionarymanager;

import com.knziha.plod.PlainDict.PlainDictAppOptions;
import com.knziha.plod.PlainDict.PlainDictionaryPcJFX;
import com.knziha.plod.dictionarymodels.mdict;
import com.knziha.plod.widgets.DragResizeView;
import com.knziha.plod.widgets.VirtualWindowEvent;
import com.knziha.plod.widgets.splitpane.HiddenSplitPaneApp;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.image.Image;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.ResourceBundle;
import java.util.function.Function;

import static com.knziha.plod.PlainDict.PlainDictionaryPcJFX.EscComb;
import static com.knziha.plod.PlainDict.PlainDictionaryPcJFX.AltDComb;

/** dictionary & dictionary set picker ui*/
public class DictPickerDialog extends Stage {
	private final DragResizeView dv;
	Text statusBar;
	TableView<File> tv1;
	TableView<mdict> tv2;
	PlainDictAppOptions opt;
	public int adapter_idx;
	public int dirtyFlag;
	HashMap<String, mdict> mdict_cache = new HashMap<>();
	ArrayList<mdict> md;
	ArrayList<mdict> currentFilter;
	private String lastName;

	public DictPickerDialog(PlainDictionaryPcJFX app,ArrayList<File> _sets, PlainDictAppOptions _opt, ResourceBundle bundle) {
		super();
		opt=_opt;
		currentFilter=app.server.currentFilter;
		setTitle(bundle.getString(PlainDictionaryPcJFX.UI.switchdict));
		getIcons().add(new Image(HiddenSplitPaneApp.class.getResourceAsStream("shared-resources/bundle.png")));
		int defaultPercent=25;
		dv = new DragResizeView(defaultPercent);
		GridPane mainGrid=dv.gl_to_guard=new GridPane();
		mainGrid.setHgap(0);
		addEventFilter(KeyEvent.KEY_PRESSED, e -> {
			if (EscComb.match(e)|| AltDComb.match(e)) {
				hide();
				e.consume();
			}
		});
		tv1 = new TableView<>();
		tv2 = new TableView<>();
		tv1.getColumns().add(createCol(tv1, "配置", propertyMapper_FilePath, -1, null));
		tv2.getColumns().add(createCol(tv2, "词典", mdict::getFileNameProperty, -1, null));
		tv1.getItems().addAll(_sets);
		tv2.getItems().addAll(md=app.server.md);
		for(mdict mdTmp:md){
			mdict_cache.put(mdTmp.getPath(), mdTmp);
		}
		for(mdict mdTmp:currentFilter){
			mdict_cache.put(mdTmp.getPath(), mdTmp);
		}
		dv.heightProperty().bind(tv1.heightProperty());
		GridPane.setVgrow(tv1, Priority.ALWAYS);
		GridPane.setVgrow(dv, Priority.ALWAYS);
		GridPane.setHgrow(tv2, Priority.ALWAYS);
		GridPane.setVgrow(tv2, Priority.ALWAYS);
		mainGrid.add(tv1,0,0);
		mainGrid.add(dv,1,0);
		mainGrid.add(tv2,2,0);
		//不能获得窗口标题栏颜色。
		dv.setFill(Color.valueOf("#f4f4f4"));
		final String hidingSplitPaneCss = HiddenSplitPaneApp.class.getResource("HiddenSplitPane.css").toExternalForm();
		tv1.setId("hiddenFrame");
		tv2.setId("hiddenFrame");
		mainGrid.getStylesheets().add(hidingSplitPaneCss);
		mainGrid.getColumnConstraints().add(dv.col1 = new ColumnConstraints());
		dv.col1.setPercentWidth(defaultPercent);

		VBox vb = new VBox(mainGrid, statusBar = new Text());
		VBox.setVgrow(mainGrid, Priority.ALWAYS);
		Scene scene = new Scene(vb, 800, 600);
		statusBar.setText("ready.");
		statusBar.setUnderline(true);
		vb.setPadding(new Insets(10,10,10,10));
		setScene(scene);
		update(app, _sets, opt, false);
	}

	public void update(PlainDictionaryPcJFX app,ArrayList<File> _sets, PlainDictAppOptions opt, boolean refreshItems) {
		if(refreshItems){
			tv1.getItems().clear();
			tv1.getItems().addAll(_sets);
			tv2.getItems().clear();
			tv2.getItems().addAll(md=app.server.md);
			for(mdict mdTmp:md){
				mdict_cache.put(mdTmp.getPath(), mdTmp);
			}
		}
		int cc=0;
		for(File fI:_sets){
			if(fI.getName().equalsIgnoreCase(opt.getCurrentPlanName()+".set")){
				tv1.getSelectionModel().select(cc);
				break;
			}
			cc++;
		}
		tv2.getSelectionModel().select(adapter_idx=app.adapter_idx);
		if(adapter_idx<md.size())lastName=md.get(adapter_idx)._Dictionary_fName;
	}

	<R, T> TableColumn<R, T> createCol(TableView<R> tv, String title,
										Function<R, ObservableValue<T>> mapper, double prefSize,
										Callback<TableColumn<R,T>, TableCell<R,T>> bindviewCallback) {
		TableColumn<R, T> col = new TableColumn<>(title);
		col.setCellValueFactory(cellData -> mapper.apply(cellData.getValue()));
		col.prefWidthProperty().bind(tv.widthProperty());
		if(bindviewCallback==null)
			bindviewCallback =
					param -> new TableCell<R, T>() {
						@Override
						protected void updateItem(T item, boolean empty) {
							if (!empty) {
								int position=indexProperty().get();
								TableView tv = getTableView();
								boolean isSelected;
								if(tv==tv1)
									isSelected=tv1.getItems().get(position).getName().equalsIgnoreCase(opt.getCurrentPlanName()+".set");
								else
									isSelected=position==adapter_idx;
								setText(String.valueOf(item));
								setUnderline(isSelected);
								setOnMouseClicked(event -> {
									if(event.getClickCount()==2){//双击
										onItemClicked(tv, position, getText());
										if(tv==tv1&&opt.GetDoubleClickCloseSet() || tv!=tv1&&opt.GetDoubleClickCloseDict()){
											close();
											getOnCloseRequest().handle(null);
											return;
										}
										if(tv==tv2){//&&opt.GetUpdateWebDictsDirect()
											getOnCloseRequest().handle(new VirtualWindowEvent(null));
										}
										event.consume();
									}
								});
							}else
								setText(null);
						}
					};
		col.setCellFactory(bindviewCallback);
		return col ;
	}

	private <R> void onItemClicked(TableView<R> tableView, int position, String text) {
		if(tableView==tv1){
			statusBar.setText("配置 "+text+" 加载成功");
			opt.setCurrentPlanName(text);
			try_read_configureLet(tv1.getItems().get(position));
			tv1.refresh();
			dirtyFlag|=0x1;
		}else{
			statusBar.setText("当前词典 ： "+text);
			adapter_idx=position;
			tv2.refresh();
			lastName=md.get(position)._Dictionary_fName;
			dirtyFlag|=0x2;
		}
	}

	public boolean try_read_configureLet(File newf) {
		try {
			BufferedReader in = new BufferedReader(new FileReader(newf));
			String line;
			int idx=0;
			tv2.getItems().clear();
			md.clear();
			currentFilter.clear();
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
					if(!disabled){
						mdict mdTmp = mdict_cache.get(line);
						if(mdTmp==null)
							mdTmp=new mdict(line, opt);
						((mdTmp.tmpIsFilter=isFilter)?currentFilter:md).add(mdTmp);
					}
					idx++;
				}
			}
			adapter_idx=0;
			for (int i = 0; i < md.size(); i++) {
				if(md.get(i)._Dictionary_fName.equals(lastName)){
					adapter_idx=i;
					break;
				}
			}
			tv2.getItems().addAll(md);
			tv2.getSelectionModel().select(adapter_idx);
			in.close();
			return true;
		} catch (IOException e2) {
			e2.printStackTrace();
		}
		return false;
	}

	Function<File, ObservableValue<String>> propertyMapper_FilePath=new Function<File, ObservableValue<String>>(){
		@Override
		public ObservableValue<String> apply(File m) {
			return new SimpleStringProperty(m.getName().substring(0, m.getName().length()-4));
		}};
}

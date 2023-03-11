package com.knziha.plod.plaindict.javafx;

import com.knziha.plod.dictionarymodels.BookPresenter;
import com.knziha.plod.dictionarymodels.PlainMdict;
import com.knziha.plod.dictionarymodels.resultRecorderScattered;
import com.knziha.plod.plaindict.CMN;
import com.knziha.plod.plaindict.javafx.PlainDictionaryPcJFX.AdvancedScopedSearchLayer;
import com.knziha.plod.plaindict.javafx.widgets.ObservableListmy;
import com.knziha.plod.plaindict.javafx.widgets.SearchBox;
import com.knziha.plod.plaindict.javafx.widgets.SearchBox2nd;
import com.knziha.plod.plaindict.javafx.widgets.splitpane.HiddenSplitPaneApp;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Side;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

import java.util.ArrayList;
import java.util.ResourceBundle;
import java.util.Timer;
import java.util.TimerTask;

//高级搜索
public class AdvancedSearchDialog extends javafx.stage.Stage {
	TextField etSearch;
	Button btnSearch2;
	Text statusBar;
	TabPane tabPane;
	SearchBox2nd box2;
	final PlainDictionaryPcJFX app;
	final ResourceBundle bundle;

	ArrayList<AdvancedSearchLogicLayer> AdvancedSearchLogicalSet = new ArrayList<>(2);

	class SearchRunnable implements Runnable{
		final AdvancedSearchLogicLayer layer;
		final boolean isCombinedSearch;
		SearchRunnable(AdvancedSearchLogicLayer _SearchLauncher, boolean _isCombinedSearch){
			layer=_SearchLauncher;
			isCombinedSearch=_isCombinedSearch;
		}
		@Override
		public void run() {
			String key = layer.key = etSearch.getText();
			statusBar.setText(" 🔍 "+key+" ...");
			layer.st = System.currentTimeMillis();
			ArrayList<BookPresenter> _md = layer.md;
			if(_md==null) _md=app.md;
			if(isCombinedSearch){
				for(int i=0;i<_md.size();i++){
					try {
						if(layer.IsInterrupted) return;
						_md.get(layer.Idx=i).getMdict().executeAdvancedSearch(key,i,layer);//do actual search
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}else {
				try {
					if(layer.IsInterrupted) return;
					_md.get(layer.Idx).getMdict().executeAdvancedSearch(key,layer.Idx,layer);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			ObservableListmy adapter = layer.adapter;
			adapter.rec.invalidate();
			layer.Ticker.cancel();
			layer.bakeMessage();
			System.gc();
			Platform.runLater(() -> {
				if(layer.IsInterrupted) return;
				layer.refreshList(true);
			});
		}
	}

	AdvancedSearchDialog(PlainDictionaryPcJFX app)
	{
		super();
		this.app = app;
		this.bundle = app.bundle;
		setTitle(bundle.getString(UI.advsearch));
		getIcons().add(new Image(HiddenSplitPaneApp.class.getResourceAsStream("shared-resources/galaxy.png")));

		initOwner(app.stage);
		app.stage.setAlwaysOnTop(false);

		SearchBox box = new SearchBox(); //box.setPadding(new Insets(0,0,10,0));
		box2 = new SearchBox2nd(); //if(isNeoJRE) box2.setPadding(new Insets(10,0,0,0));
		box.textBox.setStyle("-fx-font-size: 12.8pt;");
		box2.setCombinedSearch(app.opt.GetAdvCombinedSearching());
		box2.isCombinedSearching.addListener((observable, oldValue, newValue)
				-> app.opt.SetAdvCombinedSearching(newValue));
		GridPane topGrid=new GridPane();
		topGrid.setPadding(new Insets(0,0,0,0));
		topGrid.add(box, 0, 1);
		topGrid.add(box2, 0, 2);
		GridPane.setMargin(box, new Insets(box.paddingTop=5,5,0,5));
		box.paddingTop=5/2;
		GridPane.setMargin(box2, new Insets(0,0,0,5));
		ColumnConstraints columnConstraints = new ColumnConstraints();
		columnConstraints.setFillWidth(true);
		columnConstraints.setHgrow(Priority.ALWAYS);
		topGrid.getColumnConstraints().add(columnConstraints);
		topGrid.setHgap(0);
		topGrid.setVgap(0);
		int height=40 * 2 + 8;
		topGrid.setPrefHeight(height);
		topGrid.setMaxHeight(height);
		topGrid.setMinHeight(height);

		tabPane = new TabPane();
		statusBar = new Text();
		tabPane.setPadding(new Insets(4,0,0,0));

		etSearch = box.textBox;
		btnSearch2 = box.searchButton;

		VBox content = new VBox();
		content.getChildren().addAll(topGrid,tabPane,statusBar);


		final String tabCss = HiddenSplitPaneApp.class.getResource("tabPane.css").toExternalForm();
		tabPane.getStylesheets().add(tabCss);
		tabPane.styleProperty().set("-fx-content-display:right;");

		Tab tab1 = new Tab();
		tab1.setText(UI.wildmatch = bundle.getString("wildmatch"));
		tab1.setTooltip(new Tooltip(bundle.getString("hintwm")));
		tab1.setClosable(false);
		Text lable = new Text("");
		lable.setStyle("-fx-fill: #ff0000;");
		tab1.setGraphic(lable);

		Tab tab2 = new Tab();
		tab2.setText(UI.fulltext = bundle.getString("fulltext"));
		tab2.setTooltip(new Tooltip(bundle.getString("hintwm")));
		tab2.setClosable(false);
		Text lable1 = new Text("");
		lable1.setStyle("-fx-fill: #ff0000;");
		tab2.setGraphic(lable1);


		tabPane.setRotateGraphic(false);
		tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.SELECTED_TAB);
		tabPane.setSide(Side.TOP);
		tabPane.getTabs().addAll(tab1,tab2);
		tabPane.getStyleClass().add(TabPane.STYLE_CLASS_FLOATING);
		final String lvCss = HiddenSplitPaneApp.class.getResource("lvCss.css").toExternalForm();
		tabPane.getStylesheets().add(lvCss);
		ContextMenu contextMenu = new ContextMenu();
		String[] allItems = new String[]{
				UI.sr_inter,
				UI.sr_save,
				UI.sr_new
		};
		ObservableList<MenuItem> items = contextMenu.getItems();
		for(String mI:allItems){
			MenuItem item = new MenuItem(bundle.getString(mI));
			item.setId(mI);
			items.add(item);
		}

		contextMenu.setOnAction(event -> {
			String id = event.getTarget().toString();
			int idx = id.indexOf("id=");
			id = id.substring(idx + 3, id.indexOf(",", idx));
			switch (id) {
				case UI.sr_inter:{
					AdvancedSearchLogicLayer layer = AdvancedSearchLogicalSet.get(tabPane.getSelectionModel().getSelectedIndex());
					if(layer.workerThread!=null) {
						layer.Terminate(false);
					}
				} break;
				case UI.sr_new:{
					Tab tab = new Tab();
					tab.setText(UI.wildmatch = bundle.getString("wildmatch"));
					tab.setTooltip(new Tooltip(bundle.getString("hintwm")));
					Text text = new Text("");
					text.setStyle("-fx-fill: #ff0000;");
					text.setText("");
					tab.setGraphic(text);
					AdvancedSearchLogicLayer layer = new AdvancedScopedSearchLayer(app.opt, app.md, tab, statusBar, 1);

					ObservableListmy adapter = layer.adapter = new ObservableListmy(new resultRecorderScattered(layer, app.engine));
					ListView<Integer> listView = layer.listView = new ListView<>(adapter);
					listView.getSelectionModel().selectedIndexProperty().addListener((ov, oldV, newV) -> {//this is Number ChangeListener
						if(newV != null){
							int index=newV.intValue();
							if(listView.getSelectionModel().getSelectedItem()!=null) {
								PlainMdict mdTmp = layer.adapter.rec.getMdAt(index);
								CMN.Log("onitemclicked!!!", index, layer.adapter.rec.getIndexAt(index), layer.md.size());
								boolean post=false;
								if (!app.md.contains(mdTmp)) {
									app.md.add(new BookPresenter(mdTmp));
									post=true;
								}
								CMN.Log("virtual position : ", app.md.indexOf(mdTmp));
								layer.adapter.rec.renderContentAt(index, app.md.indexOf(mdTmp), post);
							}
						}
					});
					listView.setCellFactory((ListView<Integer> l) -> new ColoredEntryCell(layer.adapter));//setCellFactory((ListView<String> l) -> new ColorCell());
					tab.setContent(listView);
					tab.setOnCloseRequest(event1 -> {
						AdvancedSearchLogicalSet.remove(layer);
						layer.md.clear();
					});

					AdvancedSearchLogicalSet.add(layer);
					tabPane.getTabs().add(tab);
				} break;
			}
		});

		tabPane.setContextMenu(contextMenu);

		VBox.setVgrow(tabPane, Priority.ALWAYS);

		AdvancedSearchLogicalSet.add(app.fuzzySearchLayer=new AdvancedSearchLogicLayer(app.opt, app.md, tab1, statusBar, -1));
		AdvancedSearchLogicalSet.add(app.fullSearchLayer=new AdvancedSearchLogicLayer(app.opt, app.md, tab2, statusBar, -2));

		ObservableListmy adapter = app.fuzzySearchLayer.adapter = new ObservableListmy(new resultRecorderScattered(app.fuzzySearchLayer, app.engine));
		ListView<Integer> listView = app.fuzzySearchLayer.listView = new ListView<>(adapter);
		listView.getSelectionModel().selectedIndexProperty().addListener((ov, oldV, newV) -> {//this is Number ChangeListener
			if(newV != null){
				app.fuzzySearchLayer.adapter.rec.renderContentAt(newV.intValue(), -1, false);
			}
		});
		listView.setCellFactory((ListView<Integer> l) -> new ColoredEntryCell(app.fuzzySearchLayer.adapter));//setCellFactory((ListView<String> l) -> new ColorCell());
		tab1.setContent(listView);

		adapter = app.fullSearchLayer.adapter = new ObservableListmy(new resultRecorderScattered(app.fullSearchLayer, app.engine));
		listView = app.fullSearchLayer.listView = new ListView<>(adapter);
		listView.getSelectionModel().selectedIndexProperty().addListener((ov, oldV, newV) -> {
			if(newV != null){
				app.fullSearchLayer.adapter.rec.renderContentAt(newV.intValue(), -1, false);
			}
		});
		listView.setCellFactory((ListView<Integer> l) -> new ColoredEntryCell(app.fullSearchLayer.adapter));//setCellFactory((ListView<String> l) -> new ColorCell());
		tab2.setContent(listView);


		btnSearch2.setOnMouseClicked(e -> {etSearch.getOnKeyPressed().handle(new KeyEvent(KeyEvent.KEY_PRESSED, null, null, KeyCode.ENTER, false, false, false, false));});
		etSearch.setOnKeyPressed(event -> {
			if(event.getCode()==KeyCode.ENTER) {
				boolean isCombinedSearch=box2.isCombinedSearching.get();
				if(etSearch.getText().equals(""))
					return;
				AdvancedSearchLogicLayer layer = AdvancedSearchLogicalSet.get(tabPane.getSelectionModel().getSelectedIndex());
				if(layer.workerThread!=null) {
					layer.Terminate(true);
				}

				layer.IsInterrupted=false;
				if(!isCombinedSearch)
					layer.Idx= this.app.app.adapter_idx;

				ArrayList<BookPresenter> _md = layer.md;
				int GETNUMBERENTRIES=0;
				/* important to be here. clear and fetch total entry count.*/
				for(int i=0, end=_md.size();i<end;i++){//遍历所有词典
					PlainMdict mdtmp = _md.get(i).getMdict();
					if(isCombinedSearch||i==layer.Idx)
						GETNUMBERENTRIES+=mdtmp.getNumberEntries();
					ArrayList<Integer>[] _combining_search_tree_ = null;
//								layer.type<0?layer.getInternalTree(mdtmp):layer.getCombinedTree(i);
					if(_combining_search_tree_!=null)
						for(int ti=0;ti<_combining_search_tree_.length;ti++){//遍历搜索结果
							if(_combining_search_tree_[ti]!=null) {
								_combining_search_tree_[ti].clear();
							}
						}
				}
				System.gc();
				if(layer.Ticker!=null) {
					layer.Ticker.cancel();
					((Text)layer.chiefAmbassador.getGraphic()).setText("");
				}
				layer.Ticker=new Timer();
				final Timer mTicker=layer.Ticker;

				layer.workerThread = new Thread(new SearchRunnable(layer, isCombinedSearch));
				layer.workerThread.start();

				int finalGETNUMBERENTRIES = GETNUMBERENTRIES;
				mTicker.schedule(new TimerTask() {
					@Override
					public void run() {
						int GETDIRTYKEYCOUNT=0;
						if(isCombinedSearch){
							for(int i=0;i<layer.Idx;i++)
								GETDIRTYKEYCOUNT+=layer.md.get(i).bookImpl.getNumberEntries();
						}

						GETDIRTYKEYCOUNT+=layer.dirtyProgressCounter;
						final int progress = (int) Math.ceil(100.f*GETDIRTYKEYCOUNT/ finalGETNUMBERENTRIES);
						Platform.runLater(() -> ((Text)layer.chiefAmbassador.getGraphic()).setText(progress+"%"));
					}
				},0,200);
			}
		});

		Scene Scene = new Scene(content, 350, 810);
		setScene(Scene);
		onCloseRequestProperty().set(event -> {
			hide();
			event.consume();
		});
	}


}

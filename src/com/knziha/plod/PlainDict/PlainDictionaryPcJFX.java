package com.knziha.plod.PlainDict;

import java.awt.*;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.List;

import static javafx.concurrent.Worker.State.FAILED;
import static org.nanohttpd.protocols.http.response.Response.newFixedLengthResponse;

import javax.swing.JProgressBar;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import com.knziha.plod.dictionary.Utils.IU;
import com.knziha.plod.dictionary.Utils.SU;
import com.knziha.plod.dictionarymanager.ManagerFragment;
import com.knziha.plod.dictionarymodels.mdict_nonexist;
import com.knziha.plod.dictionarymodels.resultRecorderScattered;
import com.knziha.plod.dictionarymodels.resultRecorderScattered2;
import com.knziha.plod.widgets.*;
import javafx.event.ActionEvent;
import org.w3c.dom.NodeList;

import com.knziha.plod.dictionary.mdict;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.concurrent.Worker.State;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Modality;
import javafx.stage.Stage;
import netscape.javascript.JSObject;
import com.knziha.plod.widgets.splitpane.HiddenSplitPaneApp;
import test.CMN;

public class PlainDictionaryPcJFX extends Application {
	GridPane topGrid;
	SearchBox searchBox;
	TextField etSearch;
	private WebEngine engine;
	Stage stage;
	Label advancedSearch;
	AdvancedSearchDialog advancedSearchDialog;
	String lastMdlibPath = "E:\\assets\\mdicts\\";
	int currentDisplaying=0;

	public volatile static boolean fuzzyIsInterrupted;
	public volatile int fuzzyIdx;
	Thread fuzzyThread;
	Timer fuzzyTicker;
	public volatile static boolean fullIsInterrupted;
	public volatile int fullIdx;
	Thread fullThread;
	Timer fullTicker;

	private final JTextField txtURL = new JTextField();
	private final JProgressBar progressBar = new JProgressBar();
	private ColumnConstraints col1;
	private DragResizeView dv;
	private SearchBox3rd searchInPageBox;
	private double searchBoxPercent;
	private boolean bSearchInPage=true;
	private PlainDictAppOptions opt;
	private long FFStamp;

	public class AppHandle {
		public void setPos(String pos) {
			currentDisplaying=Integer.parseInt(pos);
			CMN.Log("currentDisplaying" +pos);
		}

		public void log(String val) {
			CMN.Log("web_log: "+val);
		}

		public void setLastMd(String val) {
			server.adapter_idx=Integer.parseInt(val);
			server.currentDictionary= md.get(server.adapter_idx);
		}

		public String getCurrentPageKey() {
			if(searchInPageBox!=null && searchInPageBox.getParent()!=null)
				return searchInPageBox.textBox.getText();
			return null;
		}

		public void setCombinedSeaching(boolean val){
			opt.SetCombinedSearching(val);
		}
	}

	AppHandle AppMessenger = new AppHandle();

	public void loadURL(final String url) {
		Platform.runLater(() -> {
			String tmp = toURL(url);
			if (tmp == null) {
				tmp = toURL("http://" + url);
			}
			engine.load(tmp);
		});
	}
	protected void loadContent(final String content) {
		Platform.runLater(() -> engine.loadContent(content));

	}

	protected void executeJavaScript(final String script) {
		Platform.runLater(() -> engine.executeScript(script));
	}

	protected void executeJavaScriptSilent(final String script) {
		Platform.runLater(() -> {try{engine.executeScript(script);} catch (Exception ignored){}});
	}

	private static String toURL(String str) {
		try {
			return new URL(str).toExternalForm();
		} catch (MalformedURLException exception) {
			return null;
		}
	}


	ResourceBundle bundle;

	//构造
	public PlainDictionaryPcJFX() {
		super();
		bundle = ResourceBundle.getBundle("UIText" , Locale.CHINA);
		if(!new File(projectPath).exists()) {

		}
		if(userPath==null || !new File(userPath).exists()) {
			userPath=projectPath;
		}
		usrHome = new File(userPath,".PLOD.plaindictionary");
		usrHome.mkdir();
		userPath=null;
		SU.debug=true;
	}


	static class UI{
		final static String open="open";
		final static String browser ="browser";
		final static String searchpage ="searchpage";
		final static String manager="manager";
	}

	Scene scene;
	WebView view;
	@Override
	public void start(Stage stage_) throws Exception {
		ScanSettings(new File(projectPath,"settings.xml"));
		server = new MdictServer(8080, opt);
		md = server.md;
		scanInFiles();
		server.setOnMirrorRequestListener(uri -> {
			if(uri==null)uri="";
			String[] args = uri.split("&");
			int pos=currentDisplaying; pos=IU.parsint(args[1].split("=")[1]);
			int dx=server.adapter_idx; dx=IU.parsint(args[0].split("=")[1]);
			String key=etSearch.getText();try {key=URLDecoder.decode(args[2].split("=")[1],"UTF-8");}catch(Exception e) {}
			//CMN.show("currentDisplaying"+currentDisplaying);
			NodeList xx = engine.getDocument().getElementsByTagName("iframe");
			StringBuilder sb=new StringBuilder();
			for(int i=0;i<xx.getLength();i++) {
				String content = xx.item(i).getTextContent();
				CMN.show("content?"+content);
				int split=content.indexOf("@");
				if(split==-1) continue; //TODO
				sb.append("<p style=\"background: rgb(43, 67, 129); margin-left: 0px; color: rgb(255, 255, 255);\">"+md.get(Integer.parseInt(content.substring(0,split)))._Dictionary_fName+"</p>")
						.append("<iframe id='").append(content, 0, split)
						.append("' src='").append(content.substring(split+1))
						.append("' width=\"100%\" frameborder=\"0\" height=\"171\"></iframe>");
			}
			return newFixedLengthResponse(server.constructDerivedHtml(key, pos, dx,sb.toString()));
		});
		stage = stage_;
		stage.setTitle("平典");
		stage.setOnHidden(e -> {
			if(!stage.isMaximized() && !stage.isIconified()) {
				DumpSettings(new File(projectPath,"settings.xml"));
				dumpLonelyInteger(new File(usrHome,"width"),(int)scene.getWidth());
				dumpLonelyInteger(new File(usrHome,"height"),(int)scene.getHeight());
				dumpLonelyInteger(new File(usrHome,"posX"),(int)stage.getX());
				dumpLonelyInteger(new File(usrHome,"posY"),(int)stage.getY());
			}
			if(advancedSearchDialog!=null)
				dumpLonelyInteger(new File(usrHome,"combined-search"),advancedSearchDialog.box2.isCombinedSearching?1:0);
			Platform.exit();
		});
		stage.resizableProperty().addListener((arg0, arg1, arg2) -> {
			//throw new UnsupportedOperationException("Not supported yet.");
		});
		stage.heightProperty().addListener((arg0, v1, v2) -> {
			if(advancedSearchDialog!=null) {
				advancedSearchDialog.setX(advancedSearchDialog.xProperty().doubleValue()+v2.doubleValue()-v1.doubleValue());
			}
		});
		stage.xProperty().addListener((arg0, v1, v2) -> {
			if(advancedSearchDialog!=null) {
				advancedSearchDialog.setX(advancedSearchDialog.xProperty().doubleValue()+v2.doubleValue()-v1.doubleValue());
			}
		});
		stage.yProperty().addListener((arg0, v1, v2) -> {
			if(advancedSearchDialog!=null) {
				advancedSearchDialog.setY(advancedSearchDialog.yProperty().doubleValue()+v2.doubleValue()-v1.doubleValue());
			}
		});
		//        setIconImage(Toolkit.getDefaultToolkit().createImage("G:\\.0PtClm\\Muse\\_All_the_spirites\\app图标\\PLOD\\launcherMax_white.png"));

		int width=1250;
		int height=810;
		int tmp;
		tmp = getLonelyInteger(new File(usrHome,"width"));
		if(tmp!=-1) width=tmp;
		tmp = getLonelyInteger(new File(usrHome,"height"));
		if(tmp!=-1) height=tmp;
		tmp = getLonelyInteger(new File(usrHome,"posX"));
		if(tmp!=-1) stage.setX(tmp);
		tmp = getLonelyInteger(new File(usrHome,"posY"));
		if(tmp!=-1) stage.setY(tmp);

		scene = new Scene(new VBox(), width, height);

		//开始画菜单
		// + File
		Menu menuFile = new Menu(bundle.getString("file"));

		// + View
		Menu menuView = new Menu(bundle.getString("view"));

		MenuItem add = new MenuItem(bundle.getString(UI.open));
		add.setId(UI.open);
		MenuItem manage = new MenuItem(bundle.getString(UI.manager));
		manage.setId(UI.manager);

		menuFile.getItems().addAll(add,manage);

		MenuItem browser = new MenuItem(bundle.getString(UI.browser));
		browser.setId(UI.browser);

		MenuItem searchpage = new MenuItem(bundle.getString(UI.searchpage));
		searchpage.setId(UI.searchpage);
		menuView.getItems().addAll(browser, searchpage);

		add.setAccelerator(KeyCombination.valueOf("CTRL+O"));
		manage.setAccelerator(KeyCombination.valueOf("CTRL+O"));
		searchpage.setAccelerator(KeyCombination.valueOf("CTRL+F"));

		EventHandler clicker1 = (EventHandler<ActionEvent>) event -> {
			switch(((MenuItem)event.getSource()).getId()){
				case UI.open:{
					FileChooser fileChooser = new FileChooser();
					fileChooser.getExtensionFilters().addAll(
							new ExtensionFilter("mdict file", "*.mdx")
					);
					fileChooser.setInitialDirectory(new File("D:\\assets"));//lastMdlibPath
					List<File> files = fileChooser.showOpenMultipleDialog(stage);
					int sizebrefore=mdlibsCon.size();

					if(files!=null) {
						HashSet<String> mdict_cache = new HashSet<>(md.size());
						for(mdict mdTmp:md)
							mdict_cache.add(mdTmp.getPath());
						ArrayList<String> toAdd = new ArrayList<>(md.size());
						for(File fI:files) {
							String fileNameKey=fI.getAbsolutePath();
							if(!mdict_cache.contains(fileNameKey))
								try {
									server.md.add(new mdict(fileNameKey));
									mdict_cache.add(fileNameKey);
									if(!mdlibsCon.contains(fileNameKey)){
										toAdd.add(fileNameKey);
									}
								} catch (Exception e) { e.printStackTrace(); }
						}
						if(toAdd.size()>0){
							File rec = new File(projectPath,"CONFIG/mdlibs.txt");
							try {
								BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(rec, true));
								for(String sI:toAdd){
									out.write(sI.getBytes(StandardCharsets.UTF_8));
									out.write("\r\n".getBytes(StandardCharsets.UTF_8));
								}
								out.flush();
								out.close();
								mdlibsCon.addAll(toAdd);
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
						engine.executeScript("ScanInDicts();");
					}
					if(sizebrefore!=mdlibsCon.size()){
						SaveMdicts();
					}
				} break;
				case UI.manager:{
					CMN.Log("action_handler",event.getSource(), event.getTarget());
					final Stage dialog = new Stage();
					dialog.setTitle(bundle.getString("manager"));

					Button yes = new Button("Yes");

					//label displayLabel = new Label("What do you want to do ?");
					//displayLabel.setFont(Font.font(null, FontWeight.BOLD, 14));

					dialog.initModality(Modality.WINDOW_MODAL);
					dialog.initOwner(stage);

					//SplitPane dialogHbox = new SplitPane();
					HBox dialogHbox = new HBox();

					ManagerFragment region1 = new ManagerFragment(lastMdlibPath,server.md);


					region1.getStyleClass().add("rounded");
					//region2.getStyleClass().add("rounded");

					HBox.setHgrow(region1, Priority.ALWAYS);
					//SplitPane.setResizableWithParent(region1, true);
					//dialogHbox.setDividerPositions(1);

					VBox region2 = new VBox();


					VBox.setMargin(yes, new Insets(5,0,5,0));
					region2.getChildren().add(yes);
					region2.getChildren().add(new Button("Yes"));




					final String hidingSplitPaneCss = HiddenSplitPaneApp.class.getResource("HiddenSplitPane.css").toExternalForm();
					dialogHbox.setId("hiddenSplitter");
					dialogHbox.getChildren().addAll(region1, region2);
					dialogHbox.getStylesheets().add(hidingSplitPaneCss);


					//dialogHbox.get(new Label("What do you want to do ?"));
					//dialogHbox.getChildren().add(new Label("What do you want to do ?"));

					yes.addEventHandler(MouseEvent.MOUSE_CLICKED,
							e -> {
								// inside here you can use the minimize or close the previous stage//
								dialog.close();
							});


					Scene dialogScene = new Scene(dialogHbox, 800, 600);
					dialog.onCloseRequestProperty().set((e -> {
						//CMN.show("close");
						if(region1.tableView.isDirty) {
							ObservableList<mdict> xx = region1.tableView.getItems();
							File def = new File(PU.getProjectPath(),"default.txt");
							try {
								BufferedWriter out = new BufferedWriter(new FileWriter(def,false));
								String parent = new File(lastMdlibPath).getAbsolutePath()+File.separatorChar;
								for(mdict mdTmp:xx) {
									String name = mdTmp.getPath();
									if(name.startsWith(parent))
										name = name.substring(parent.length());
									out.write(name);
									out.write("\n");
								}
								out.flush();
								out.close();
							} catch (IOException e2) {
								e2.printStackTrace();
							}

							ArrayList<mdict> mdNew = new ArrayList<>();
							for(mdict mdTmp:xx) {
								if(!mdict_nonexist.class.isInstance(mdTmp)) {
									mdNew.add(mdTmp);
								}
							}
							md=server.md=mdNew;
							engine.executeScript("ScanInDicts();");
						}
						//event.consume();
					}));
					dialog.setScene(dialogScene);
					dialog.show();
				} break;
				case UI.browser:{
					//创建一个URI实例
					URI uri = null;
					try {
						uri = URI.create("http://127.0.0.1:8080/MIRROR.jsp?DX="+server.adapter_idx+"&POS="+currentDisplaying+"&KEY="+URLEncoder.encode(etSearch.getText(),"UTF-8"));
					} catch (UnsupportedEncodingException e1) {
						e1.printStackTrace();
					}
					// 获取当前系统桌面扩展
					Desktop dp = Desktop.getDesktop();
					// 判断系统桌面是否支持要执行的功能
					if (dp.isSupported(Desktop.Action.BROWSE)) {
						// 获取系统默认浏览器打开链接
						try {
							NodeList list = engine.getDocument().getElementsByTagName("iframe");
							dp.browse(uri);
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				} break;
				case UI.searchpage:{
					if(bSearchInPage=!bSearchInPage)
						AttachSearchInPage(true);
					else
						DettachSearchInPage();
				} break;
			}
		};

		add.setOnAction(clicker1);
		manage.setOnAction(clicker1);
		browser.setOnAction(clicker1);
		searchpage.setOnAction(clicker1);



		Label menuLabel1 = new Label(bundle.getString("set"));
		menuLabel1.setOnMouseClicked(event -> {
		});
		Menu fileMenuButton = new Menu();
		fileMenuButton.setGraphic(menuLabel1);

		Label menuLabel = new Label(bundle.getString("dict"));
		menuLabel.setOnMouseClicked(event -> {
			//engine.executeScript("highlight('h');");
		});
		Menu fileMenuButton1 = new Menu();
		fileMenuButton1.setGraphic(menuLabel);

		// + advcanced search
		advancedSearch = new Label(bundle.getString("advsearch"));
		advancedSearch.setOnMouseClicked(event -> {
			if(advancedSearchDialog==null) {
				advancedSearchDialog = new AdvancedSearchDialog();
				advancedSearchDialog.setOnCloseRequest(e -> {advancedSearch.getOnMouseClicked().handle(new MouseEvent(MouseEvent.MOUSE_PRESSED, 0, 0, 0, 0, MouseButton.PRIMARY, 1, false, false, false, false, false, false, false, false, false, false, null));e.consume();});
			}
			advancedSearchDialog.setWidth(350);
			advancedSearchDialog.setHeight(stage.getHeight());
			if(advancedSearchDialog.isShowing())
				advancedSearchDialog.hide();
			else {
				advancedSearchDialog.show();
				if(stage.isMaximized()) {
					advancedSearchDialog.setMaximized(true);
				}else {
					advancedSearchDialog.setX(stage.xProperty().doubleValue()-335);
					advancedSearchDialog.setY(stage.yProperty().doubleValue());
				}
			}
			File flg = new File(usrHome, "show-advanced");
			File flg2 = new File(usrHome, "hide-advanced");
			if(advancedSearchDialog.isShowing()) {
				if(!flg.exists()) {
					if(flg2.exists())
						flg2.renameTo(flg);
					else
						try {
							flg.createNewFile();
						} catch (IOException e) {
							e.printStackTrace();
						}
				}else
					flg2.delete();
			}else {
				if(flg.exists()) {
					flg.renameTo(flg2);
				} else
				if(flg.exists()) {
					flg.delete();
				}
			}
		});

		//advancedSearchBtn.setGraphic(advancedSearch);
		Menu advancedSearchBtn = new Menu("", advancedSearch);
		advancedSearchBtn.setStyle("-fx-padding:0 0 0 0;");
		advancedSearch.setPadding(new Insets(5,10,5,10));
		MenuBar menuBar = new MenuBar();

		// +++
		menuBar.getMenus().addAll(menuFile, fileMenuButton,fileMenuButton1, menuView, advancedSearchBtn);


		view = new WebView();
		view.setFontScale(1.25);
		view.setZoom(1.25f);
		engine = view.getEngine();
		engine.setJavaScriptEnabled(true);
		loadURL("http://127.0.0.1:8080");
		//engine.setUserAgent("");
		engine.titleProperty().addListener((observable, oldValue, newValue) -> SwingUtilities.invokeLater(() -> {
			//PlaneDictionaryPc.this.setTitle(newValue);
		}));
		engine.setOnStatusChanged(event -> SwingUtilities.invokeLater(() -> {
			//lblStatus.setText(event.getData());
		}));
		engine.locationProperty().addListener((ov, oldValue, newValue) -> SwingUtilities.invokeLater(() -> txtURL.setText(newValue)));


		String StartIncannation=(opt.GetCombinedSearching()?"document.getElementById('fileBtn').onclick();":"")+"document.getElementById('wordP').style.display='none';document.getElementById('lv').style.paddingTop='0';document.getElementById('lv').style.marginTop='5px';document.getElementById('seekbar_container').style.marginTop='-5px';";
		engine.getLoadWorker().workDoneProperty().addListener((observable, oldValue, newValue) -> {
			SwingUtilities.invokeLater(() -> {
				//CMN.Log(oldValue,">>",newValue);
				if(newValue.doubleValue()>=99)
					executeJavaScript(StartIncannation);
			});
		});
		engine.getLoadWorker()
				.exceptionProperty()
				.addListener((o, old, value) -> {
					if (engine.getLoadWorker().getState() == FAILED) {
						SwingUtilities.invokeLater(() -> {});
					}
				});
		com.sun.javafx.webkit.WebConsoleListener.setDefaultListener((webView, message, lineNumber, sourceId) -> {
			CMN.Log("Console: ",message," [" + sourceId,":",lineNumber, "] ");
		});
		engine.getLoadWorker().stateProperty()
				.addListener(
						(ov, oldState, newState) -> {
							if (newState == State.SUCCEEDED) {
								JSObject win = (JSObject) engine.executeScript("window");
								win.setMember("app", AppMessenger);
							}
						}
				);

		view.getStyleClass().add("browser");
		((VBox) scene.getRoot()).getChildren().addAll(menuBar);
		searchBox = new SearchBox();
		//box.searchButton.setVisible(false);
		etSearch = searchBox.textBox;//new TextField ();
		searchBox.searchButton.setOnMouseClicked(e -> {etSearch.getOnKeyPressed().handle(new KeyEvent(KeyEvent.KEY_PRESSED, null, null, KeyCode.ENTER, false, false, false, false));});
		etSearch.clear();
		etSearch.setOnKeyPressed(event -> {
			//executeJavaScript("lookup('"+etSearch.getText()+"')");
			if(event.getCode()==KeyCode.ENTER)
				executeJavaScript("lookup('"+etSearch.getText()+"')");
		});
		//etSearch.setOnKeyReleased(new EventHandler<KeyEvent>() {
		//	@Override
		//	public void handle(KeyEvent event) {
		//		executeJavaScript("lookup('"+etSearch.getText()+"')");
		//	}});
		final ChangeListener<String> textListener =
				(ObservableValue<? extends String> observable,
				 String oldValue, String newValue) -> {
					executeJavaScript("lookup('"+newValue+"')");
					searchBox.clearButton.setVisible(newValue.length() != 0);
				};
		etSearch.textProperty().addListener(textListener);

		etSearch.addEventHandler(
				DragEvent.DRAG_OVER,
				event -> {
					if (event.getDragboard().hasString()) {
						event.acceptTransferModes(TransferMode.COPY);
					}
					event.consume();
				});
		etSearch.addEventHandler(
				DragEvent.DRAG_DROPPED,
				event -> {
					Dragboard dragboard = event.getDragboard();
					if (event.getTransferMode() == TransferMode.COPY &&
							dragboard.hasString()) {
						etSearch.setText(dragboard.getString());
						event.setDropCompleted(true);
					}
					event.consume();
				});

		topGrid = new GridPane();

		topGrid.setHgap(0);
		topGrid.setPadding(new Insets(5, 12, 0, 2));
		topGrid.add(searchBox, 0, 0);
		GridPane.setHgrow(searchBox, Priority.ALWAYS);
		if(bSearchInPage)
			AttachSearchInPage(false);
		int gridHeight=50;
		topGrid.setPrefHeight(gridHeight);
		topGrid.setMaxHeight(gridHeight);
		topGrid.setMinHeight(gridHeight);
		//height? 40.0?
		((VBox) scene.getRoot()).getChildren().add(topGrid);



		((VBox) scene.getRoot()).getChildren().add(view);
		VBox.setVgrow(view, Priority.ALWAYS);
		stage.setScene(scene);
		stage.getIcons().add(new Image(PlainDictionaryPcJFX.class.getResourceAsStream("Mdict-browser/MdbR/MdbR.png")));

		stage.show();
		try {
			server.start();
		} catch (IOException e) {
			e.printStackTrace();
		}
		//setBackground(Color.black);


		//browser.loadContent("<a onclick=\"new Audio('https://www.collinsdictionary.com/sounds/6/669/66956/66956.mp3').play();\">AUDIO TEST</a>");

		//server.currentPage = server.md.get(server.adapter_idx).lookUp("wolf");

		//try {
		//	Thread.sleep(1000);
		//} catch (InterruptedException e) {
		//	e.printStackTrace();
		//}

		//CMN.show(grid.getHeight()+"");
		//executeJavaScript("document.getElementById('lv').style.visibility='hidden';");
		//browser.executeJavaScript("new Audio('\\sound\\asd.mp3').play();");
		//browser.loadContent(md.getRecordAt(md.lookUp("wolf")));
		//browser.loadContent("asdasd<img src='E:\\assets\\mdicts\\wordsmyth2018.png'></img>");

		if(new File(usrHome, "show-advanced").exists())
			Platform.runLater(() -> advancedSearch.getOnMouseClicked().handle(new MouseEvent(MouseEvent.MOUSE_PRESSED, 0, 0, 0, 0, MouseButton.PRIMARY, 1, false, false, false, false, false, false, false, false, false, false, null)));


		//tg
		new Timer().schedule(new TimerTask() {
			@Override
			public void run() {
				etSearch.setText("happy");
			}
		},1000);


	}

	private void AttachSearchInPage(boolean focus) {
		if(col1==null){
			searchBoxPercent=61;
			dv = new DragResizeView(searchBoxPercent);
			dv.gl_to_guard=topGrid;
			topGrid.getColumnConstraints().add(dv.col1=col1=new ColumnConstraints());
			GridPane.setHgrow(searchInPageBox=new SearchBox3rd(), Priority.ALWAYS);
			searchInPageBox.textBox.textProperty().addListener((observable, oldValue, newValue) -> {
				executeJavaScript("highlight('"+newValue+"');");
			});
		}

		col1.setPercentWidth(searchBoxPercent);

		if(!topGrid.getChildren().contains(topGrid)){
			topGrid.add(dv, 1, 0);
			topGrid.add(searchInPageBox, 2, 0);
		}
		if(focus) searchInPageBox.textBox.requestFocus();
		String text=searchInPageBox.textBox.getText().trim();
		if(text.length()>0)
			engine.executeScript("highlight('"+text+"');");
	}

	private void DettachSearchInPage() {
		topGrid.getChildren().remove(dv);
		topGrid.getChildren().remove(searchInPageBox);
		col1.setPercentWidth(100);
		if(searchInPageBox.textBox.getText().trim().length()>0)
			engine.executeScript("clearHighlights();");
	}

	private void SaveMdicts() {
		File def = new File(PU.getProjectPath(),"default.txt");
		try {
			BufferedWriter out = new BufferedWriter(new FileWriter(def,false));
			String parent = new File(lastMdlibPath).getAbsolutePath()+File.separatorChar;
			for(mdict mdTmp:server.md) {
				String name = mdTmp.getPath();
				if(name.startsWith(parent))
					name = name.substring(parent.length());
				out.write(name);
				out.write("\r\n");
			}
			out.flush();
			out.close();
		} catch (IOException e2) {
			e2.printStackTrace();
		}
	}

	private void dumpLonelyInteger(File tmp, int val) {
		if(!tmp.exists()) tmp.mkdirs();
		File tmp0 = new File(tmp, String.valueOf(val));
		if(tmp.isDirectory()) {
			File[] lst = tmp.listFiles();
			if(lst!=null && lst.length>0) {
				if(lst.length>1) {
					for(int i=1;i<lst.length;i++) {
						lst[i].delete();
					}
				}
				lst[0].renameTo(tmp0);
			} else try {
				tmp0.createNewFile();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	}
	private int getLonelyInteger(File tmp) {
		if(tmp.exists()) {
			if(tmp.isDirectory()) {
				try {
					File[] lst = tmp.listFiles();
					if(lst!=null && lst.length>0)
						return Integer.valueOf(lst[0].getName());
				}catch(Exception e) {}
			}
		}
		return -1;
	}




	File usrHome;
	public static String projectPath;
	public static String userPath;
	public static void main(String[] args) {
		//CMN.show(System.getProperty("java.version"));
		//1.8.0_171
		//10.0.2
		projectPath = PU.getProjectPath();
		userPath = System.getProperty("user.home");
		String VersionCode = System.getProperty("java.version");
		CMN.Log("projectPath", projectPath);
		CMN.Log("usrHome", System.getProperty("user.home"));
		CMN.Log("VersionCode", VersionCode);
		if(VersionCode.startsWith("9") || VersionCode.startsWith("10")) isNeoJRE=true;
		launch(args);
	}

	public static boolean isNeoJRE=false;

	HashSet<String> mdlibsCon;

	BufferedWriter output;
	BufferedWriter output2;

	ArrayList<mdict> md;
	private void scanInFiles() {
		//![] start loading dictionaries
		File def = new File(projectPath,"default.txt");      //!!!原配置
		File rec = new File(projectPath,"CONFIG/mdlibs.txt");
		final boolean retrieve_all=!def.exists() && rec.exists();

		if(retrieve_all) {
		}else if(def.exists()){
			try {
				BufferedReader in = new BufferedReader(new FileReader(def));
				String line = in.readLine();
				while(line!=null){
					try {
						if(!line.contains(":"))
							line=lastMdlibPath+"/"+line;
						mdict mdtmp = new mdict(line);
						//if(mdtmp._Dictionary_fName.equals(opt.getLastMdFn()))
						//	server.adapter_idx = md.size();
						server.md.add(mdtmp);
					} catch (Exception e) {
						e.printStackTrace();
						//show(R.string.err, new File(line).getName(),line,e.getLocalizedMessage());
					}
					line = in.readLine();
				}
				in.close();
			} catch (IOException e2) {
				e2.printStackTrace();
			}
		}else{
			def.getParentFile().mkdirs();
			try {
				def.createNewFile();
			} catch (IOException ignored) {}
		}

		//dbCon = new DBWangYiLPController(this,true);   getExternalFilesDir(null).getAbsolutePath()
		mdlibsCon = new HashSet<>();
		if(rec.exists())
			try {
				BufferedReader in = new BufferedReader(new FileReader(rec));

				String line = in.readLine();
				while(line!=null){
					mdlibsCon.add(line);															   //!!!旧爱
					line = in.readLine();
				}
				in.close();
			} catch (Exception e2) {
				e2.printStackTrace();
			}
		else {
			rec.getParentFile().mkdirs();
			try {
				rec.createNewFile();
			} catch (IOException ignored) {}
		}



		try {
			output = new BufferedWriter(new FileWriter(rec,true));
			output2 = new BufferedWriter(new FileWriter(def,true));
		} catch (IOException e1) {
			e1.printStackTrace();
		}


		//	mdlibsCon.clear();
		//dbCon.prepareContain();
		File mdlib = new File(lastMdlibPath);
		if(mdlib.exists() && mdlib.isDirectory()) {
			File[] arr = mdlib.listFiles(pathname -> {
				if(pathname.isFile()) {
					String fn = pathname.getName();
					if(fn.toLowerCase().endsWith(".mdx")) {
						//if(!dbCon.contains(fn)) {
						//	dbCon.insert(fn,pathname.getAbsolutePath());
						//	return true;
						//}
						if(retrieve_all || !mdlibsCon.contains(fn)) {                                             //!!!新欢
							try {
								mdlibsCon.add(fn);
								output.write(fn);
								output.write("\n");
								output2.write(fn);
								output2.write("\n");
							} catch (IOException e) {
								e.printStackTrace();
							}
							return true;
						}

					}
				}
				return false;
			});
			if(arr!=null)
				for (final File i :arr) {
					try {
						mdict mdtmp = new mdict(i.getAbsolutePath());
						md.add(mdtmp);
					} catch (Exception e) {
						e.printStackTrace();
						//show(R.string.err,i.getName(),i.getAbsolutePath(),e.getLocalizedMessage());
					}
				}
		}
		try {
			output.flush();
			output.close();
			output2.flush();
			output2.close();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		if(md.size()>0)
			server.currentDictionary = md.get(0);
	}



	MdictServer server;


	class ColorCell extends ListCell<Integer> {
		ObservableListmy adapter;
		ColorCell(ObservableListmy adapter_){
			adapter=adapter_;
		}
		EventHandler<MouseEvent> clicker = new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent event) {
				adapter.rec.renderContentAt(Integer.valueOf(((Node)event.getSource()).idProperty().getValue()));
			}
		};
		@Override
		protected void updateItem(Integer pos, boolean empty) {
			super.updateItem(pos, empty);
			if(empty || pos==null)
				return;
			BorderPane cell = new BorderPane();

			Text title = new Text(adapter.rec.getResAt(pos));
			title.setFont(Font.font("宋体",18));
			//title.setStyle("-fx-font-style:bold;");

			Text date = new Text(md.get(adapter.rec.dictIdx)._Dictionary_fName);
			date.setFont(Font.font("宋体",12));
			date.setStyle("-fx-fill: #666666;-fx-opacity: 0.66;");
			//Text source = new Text("dd");
			//source.setFont(Font.font(10));

			cell.setTop(title);
			cell.setLeft(date);
			cell.setId(pos.toString());
			//cell.setRight(source);
			//cell.setOnMouseClicked(clicker);
			cell.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
				getListView().requestFocus();
				getListView().getSelectionModel().clearSelection();
				getListView().getSelectionModel().select(pos);
			});
			setGraphic(cell);
		}
	}

	class AdvancedSearchDialog extends Stage{
		TextField etSearch2;
		Button btnSearch2;
		Text statusBar;
		TabPane tabPane;
		SearchBox2nd box2;
		ObservableListmy adapter2;
		ObservableListmy adapter3;

		AdvancedSearchDialog()
		{
			super();
			//高级搜索
			setTitle(bundle.getString("advsearch"));
			getIcons().add(new Image(HiddenSplitPaneApp.class.getResourceAsStream("shared-resources/galaxy.png")));

			//label displayLabel = new Label("What do you want to do ?");
			//displayLabel.setFont(Font.font(null, FontWeight.BOLD, 14));
			//dialog.initModality(Modality.WINDOW_MODAL);
			initOwner(stage);
			stage.setAlwaysOnTop(false);

			SearchBox box = new SearchBox(); //box.setPadding(new Insets(0,0,10,0));
			box2 = new SearchBox2nd(); //if(isNeoJRE) box2.setPadding(new Insets(10,0,0,0));
			box.textBox.setStyle("-fx-font-size: 12.8pt;");
			box2.setCombinedSearch(opt.GetCombinedSearching());
			GridPane topGrid=new GridPane();
			topGrid.setPadding(new Insets(0,0,0,0));
			//topGrid.getChildren().addAll(box, box2);
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
			etSearch2 = box.textBox;
			btnSearch2 = box.searchButton;

			VBox content = new VBox();
			content.getChildren().addAll(topGrid,tabPane,statusBar);


			final String tabCss = HiddenSplitPaneApp.class.getResource("tabPane.css").toExternalForm();
			tabPane.getStylesheets().add(tabCss);
			tabPane.styleProperty().set("-fx-content-display:right;");
			Tab tab1 = new Tab();
			tab1.setText(bundle.getString("wildmatch"));
			tab1.setTooltip(new Tooltip(bundle.getString("hintwm")));
			tab1.setClosable(false);
			Text lable = new Text("");
			lable.setStyle("-fx-fill: #ff0000;");
			tab1.setGraphic(lable);

			Tab tab2 = new Tab();
			tab2.setText(bundle.getString("fulltext"));
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

			VBox.setVgrow(tabPane, Priority.ALWAYS);


			adapter2 = new ObservableListmy();
			adapter2.rec = new resultRecorderScattered(md, engine);
			final ListView<Integer> listView = new ListView<Integer>(adapter2);
			listView.getSelectionModel().selectedIndexProperty().addListener((ov, oldV, newV) -> {//this is Number ChangeListener
				if(newV != null){
					int selectedIdx = newV.intValue();//用以更新列表
					//CMN.show("liste_clicker"+selectedIdx);
					int Dingx=adapter2.rec.dictIdx;
					engine.executeScript("currentDicts["+Dingx+"].OldSel="+selectedIdx+";if(lastDingX!="+Dingx+"){var e = [];e.name="+Dingx+";p2(e);}");
					adapter2.rec.renderContentAt(selectedIdx);
				}
			});
			listView.setCellFactory((ListView<Integer> l) -> new ColorCell(adapter2));//setCellFactory((ListView<String> l) -> new ColorCell());
			tab1.setContent(listView);

			adapter3 = new ObservableListmy();
			adapter3.rec = new resultRecorderScattered2(md, engine);
			final ListView<Integer> listView2 = new ListView<Integer>(adapter3);
			listView2.getSelectionModel().selectedIndexProperty().addListener(new ChangeListener<Number>(){
				@Override
				public void changed(ObservableValue<? extends Number> ov, Number oldV, Number newV){
					if(newV != null){
						int selectedIdx = newV.intValue();
						//CMN.show("liste_clicker"+selectedIdx);
						int Dingx=adapter3.rec.dictIdx;
						engine.executeScript("currentDicts["+Dingx+"].OldSel="+selectedIdx+";if(lastDingX!="+Dingx+"){var e = [];e.name="+Dingx+";p2(e);}");
						adapter3.rec.renderContentAt(selectedIdx);
					}
				}
			});
			listView2.setCellFactory((ListView<Integer> l) -> new ColorCell(adapter3));//setCellFactory((ListView<String> l) -> new ColorCell());
			tab2.setContent(listView2);





			btnSearch2.setOnMouseClicked(e -> {etSearch2.getOnKeyPressed().handle(new KeyEvent(KeyEvent.KEY_PRESSED, null, null, KeyCode.ENTER, false, false, false, false));});
			etSearch2.setOnKeyPressed(new EventHandler<KeyEvent>() {
				@Override
				public void handle(KeyEvent event) {
					if(event.getCode()==KeyCode.ENTER) {
						switch(tabPane.getSelectionModel().getSelectedIndex()) {
							case 0://模糊搜索
								if(etSearch2.getText().equals("")) break;
								if(fuzzyThread!=null) {
									fuzzyIsInterrupted=true;
									if(fuzzyTicker!=null) {
										fuzzyTicker.cancel();
										((Text)tab1.getGraphic()).setText("");
									}
									for(int i=0;i<md.size();i++){//遍历所有词典
										md.get(i).fuzzyCancled=true;
									}
									//fuzzyThread.interrupt();
									try {
										fuzzyThread.join();
									} catch (InterruptedException e) {
										e.printStackTrace();
									}
								}
								//!!this's being here is very important, put it in the worker-thread will cause lag
								int GETNUMBERENTRIES=0;
								fuzzyIsInterrupted=false;
								for(int i=0;i<md.size();i++){//遍历所有词典
									mdict mdtmp = md.get(i);
									GETNUMBERENTRIES+=mdtmp.getNumberEntries();
									mdtmp.fuzzyCancled=false;
									if(mdtmp.combining_search_tree2==null) {
									}
									else
										for(int ti=0;ti<mdtmp.combining_search_tree2.length;ti++){//遍历搜索结果
											if(mdtmp.combining_search_tree2[ti]==null) {
												continue;
											}
											mdtmp.combining_search_tree2[ti].clear();
										}
								}
								System.gc();
								final int GETNUMBERENTRIES_=GETNUMBERENTRIES;
								CMN.show(GETNUMBERENTRIES_+":");
								if(fuzzyTicker!=null) {
									fuzzyTicker.cancel();
									((Text)tab1.getGraphic()).setText("");
								}
								fuzzyTicker=new Timer();
								final Timer mTicker=fuzzyTicker;
								fuzzyThread = new Thread(new Runnable() {
									@Override
									public void run() {
										String key = etSearch2.getText().toString();
										CMN.show("Searching "+key+" ...");
										long st = System.currentTimeMillis();
										if(box2.isCombinedSearching){
											for(int i=0;i<md.size();i++){
												try {
													if(fuzzyIsInterrupted) return;
													fuzzyIdx = i;
													md.get(i).flowerFindAllKeys(key,i,30);//do actual search
													//System.gc();
												} catch (Exception e) {
													e.printStackTrace();
												}
											}
										}else {
											try {
												if(fuzzyIsInterrupted) return;
												server.currentDictionary.flowerFindAllKeys(key,server.adapter_idx,30);
											} catch (Exception e) {
												e.printStackTrace();
											}
										}
										//for(int i=0;i<md.size();i++){
										//	int tmp=md.get(i).dirtyfzPrgCounter;
										//	if(md.get(i).dirtyfzPrgCounter!=md.get(i).getNumberEntries())
										//	CMN.show(md.get(i)._Dictionary_fName+": "+md.get(i).dirtyfzPrgCounter+"!="+md.get(i).getNumberEntries());
										//}
										adapter2.rec.invalidate();
										statusBar.setText("模糊搜索 \""+key+"\" "+(System.currentTimeMillis()-st)*1.f/1000+"s -> "+adapter2.rec.size()+"项");
										mTicker.cancel();
										System.gc();
										Platform.runLater(new Runnable() {
											@Override
											public void run() {
												if(fuzzyIsInterrupted) return;
												listView.setItems(null);
												listView.setItems(adapter2);
												listView.scrollTo(0);
												((Text)tab1.getGraphic()).setText("");
											}
										});
									}});
								fuzzyThread.start();
								//fuzzyThread.run();
								//if(false)
								mTicker.schedule(new TimerTask() {
									@Override
									public void run() {
										int GETDIRTYKEYCOUNTER=0;
										for(int i=0;i<fuzzyIdx;i++){
											GETDIRTYKEYCOUNTER+=md.get(i).getNumberEntries();
										}
										GETDIRTYKEYCOUNTER+=md.get(fuzzyIdx).dirtyfzPrgCounter;
										final int progress = (int) Math.ceil(100.f*GETDIRTYKEYCOUNTER/GETNUMBERENTRIES_);
										Platform.runLater(new Runnable() {
											@Override
											public void run() {
												//CMN.show(""+progress);
												((Text)tab1.getGraphic()).setText(progress+"%");
											}
										});
									}
								},0,100);
								break;
							case 1://全文搜索
								if(etSearch2.getText().equals("")) break;
								if(fullThread!=null) {
									fullIsInterrupted=true;
									if(fullTicker!=null) {
										fullTicker.cancel();
										((Text)tab1.getGraphic()).setText("");
									}
									for(int i=0;i<md.size();i++){//遍历所有词典
										md.get(i).fuzzyCancled=true;
									}
									//fuzzyThread.interrupt();
									try {
										fullThread.join();
									} catch (InterruptedException e) {
										e.printStackTrace();
									}
								}
								//!!this's being here is very important, put it in the worker-thread will cause lag
								int GETNUMBERENTRIES1=0;
								fullIsInterrupted=false;
								for(int i=0;i<md.size();i++){//遍历所有词典
									mdict mdtmp = md.get(i);
									GETNUMBERENTRIES1+=mdtmp.getNumberEntries();
									mdtmp.searchCancled=false;
									if(mdtmp.combining_search_tree_4==null) {
									}
									else
										for(int ti=0;ti<mdtmp.combining_search_tree_4.length;ti++){//遍历搜索结果
											if(mdtmp.combining_search_tree_4[ti]==null) {
												continue;
											}
											mdtmp.combining_search_tree_4[ti].clear();
										}
								}
								System.gc();
								final int GETNUMBERENTRIES_1=GETNUMBERENTRIES1;
								CMN.show(GETNUMBERENTRIES_1+":");
								if(fullTicker!=null) {
									fullTicker.cancel();
									((Text)tab1.getGraphic()).setText("");
								}
								fullTicker=new Timer();
								final Timer mTicker1=fullTicker;
								fullThread = new Thread(new Runnable() {
									@Override
									public void run() {
										String key = etSearch2.getText().toString();
										CMN.show("Searching "+key+" ...");
										long st = System.currentTimeMillis();
										if(box2.isCombinedSearching){
											for(int i=0;i<md.size();i++){
												try {
													if(fullIsInterrupted) return;
													fullIdx = i;
													md.get(i).flowerFindAllContents(key,i,30);//do actual search
													//System.gc();
												} catch (Exception e) {
													e.printStackTrace();
												}
											}
										}else {
											try {
												if(fullIsInterrupted) return;
												server.currentDictionary.flowerFindAllContents(key,server.adapter_idx,30);
											} catch (Exception e) {
												e.printStackTrace();
											}
										}
										//for(int i=0;i<md.size();i++){
										//	int tmp=md.get(i).dirtyfzPrgCounter;
										//	if(md.get(i).dirtyfzPrgCounter!=md.get(i).getNumberEntries())
										//	CMN.show(md.get(i)._Dictionary_fName+": "+md.get(i).dirtyfzPrgCounter+"!="+md.get(i).getNumberEntries());
										//}
										adapter3.rec.invalidate();
										adapter3.rec.currentSearchTerm=key.replace("*", ".*");
										statusBar.setText("全文搜索 \""+key+"\" "+(System.currentTimeMillis()-st)*1.f/1000+"s -> "+adapter3.rec.size()+"项");
										mTicker1.cancel();
										System.gc();
										Platform.runLater(new Runnable() {
											@Override
											public void run() {
												if(fullIsInterrupted) return;
												listView2.setItems(null);
												listView2.setItems(adapter3);
												listView2.scrollTo(0);
												((Text)tab2.getGraphic()).setText("");
											}
										});
									}});
								fullThread.start();
								//fuzzyThread.run();
								//if(false)
								mTicker1.schedule(new TimerTask() {
									@Override
									public void run() {
										int GETDIRTYKEYCOUNTER=0;
										for(int i=0;i<fullIdx;i++){
											GETDIRTYKEYCOUNTER+=md.get(i).getNumberEntries();
										}
										GETDIRTYKEYCOUNTER+=md.get(fullIdx).dirtykeyCounter;
										final int progress = (int) Math.ceil(100.f*GETDIRTYKEYCOUNTER/GETNUMBERENTRIES_1);
										Platform.runLater(new Runnable() {
											@Override
											public void run() {
												//CMN.show(""+progress);
												((Text)tab2.getGraphic()).setText(progress+"%");
											}
										});
									}
								},0,100);
								break;
							//listView.refresh();
							//CMN.show(adapter2.size()+""+adapter2.rec.size());
						}
					}
				}});



			Scene Scene = new Scene(content, 350, 810);
			setScene(Scene);
			onCloseRequestProperty().set(event -> {
				CMN.show("EventHandler???"+scene.getHeight());
				hide();
				event.consume();
			});

		}

	}


	public void ScanSettings(File file) {
		if(file.exists())
		try {
			JAXBContext context = JAXBContext
					.newInstance(PlainDictAppOptions.class);
			Unmarshaller um = context.createUnmarshaller();
			opt = (PlainDictAppOptions) um.unmarshal(file);
			CMN.Log("loaded", Long.toHexString(opt.getFirstFlag()));
		} catch (Exception e) {
			CMN.Log(e);
		}
		if(opt==null)
			opt=new PlainDictAppOptions();
		FFStamp=opt.getFirstFlag();
	}

	public void DumpSettings(File file) {
		try {
			JAXBContext context = JAXBContext
					.newInstance(PlainDictAppOptions.class);
			Marshaller m = context.createMarshaller();
			m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			m.marshal(opt, file);
			CMN.Log("saving", Long.toHexString(opt.getFirstFlag()));
		} catch (Exception e) {
			CMN.Log(e);
		}
	}
}
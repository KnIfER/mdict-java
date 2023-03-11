package com.knziha.plod.plaindict.javafx;

import com.knziha.plod.dictionary.Utils.IU;
import com.knziha.plod.dictionary.Utils.SU;
import com.knziha.plod.dictionarymodels.BookPresenter;
import com.knziha.plod.dictionarymodels.MagentTransient;
import com.knziha.plod.dictionarymodels.PlainMdict;
import com.knziha.plod.ebook.MobiBook;
import com.knziha.plod.plaindict.*;
import com.knziha.plod.plaindict.javafx.widgets.DragResizeView;
import com.knziha.plod.plaindict.javafx.widgets.SearchBox;
import com.knziha.plod.plaindict.javafx.widgets.SearchBox3rd;
import com.knziha.plod.plaindict.javafx.widgets.VirtualEvent;
import com.knziha.plod.settings.SettingsDialog;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.concurrent.Worker.State;
import javafx.css.Styleable;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.*;
import javafx.stage.FileChooser.ExtensionFilter;
import netscape.javascript.JSObject;

import javax.swing.*;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.io.*;
import java.lang.ref.WeakReference;
import java.net.*;
import java.util.List;
import java.util.Timer;
import java.util.*;
import java.util.regex.Pattern;

import static javafx.concurrent.Worker.State.FAILED;
		 
public class PlainDictionaryPcJFX extends Application implements EventHandler {
	ResourceBundle bundle;
	GridPane topGrid;
	SearchBox searchBox;
	public TextField etSearch;
	public WebEngine engine;
	javafx.stage.Stage stage;
	Label advancedSearchLabel;
	String lastPasteItem;
	AdvancedSearchDialog advancedSearchDialog;
	
	public static Pattern windowPath=Pattern.compile("^[a-zA-Z]:\\\\.*");
	public final static KeyCombination EscComb = KeyCombination.valueOf("ESC");
	public final static KeyCombination AltDComb = KeyCombination.valueOf("ALT+D");

	AdvancedSearchLogicLayer fuzzySearchLayer;
	AdvancedSearchLogicLayer fullSearchLayer;
	public static int ThirdFlag;

	private final JTextField txtURL = new JTextField();
	private final JProgressBar progressBar = new JProgressBar();
	private ColumnConstraints col1;
	private DragResizeView dv;
	private SearchBox3rd searchInPageBox;
	public PlainDictAppOptions opt;
	private long FFStamp;
	Stage contextDialog;
	private DictPickerDialog pickDictDialog;
	private MenuBar toolBar;
	private ArrayList<mFile> DocumentIncludePaths;
	private ArrayList<File> fileSets;
	private int WebLvSize=195;
	WeakReference<SettingsDialog> settingDialog;
	WeakReference<Stage> managerDialog;
	private Clipboard clipboard;
	
	Map<String, PlainMdict> md_table = Collections.synchronizedMap(new HashMap<>());
	
	public final ArrayList<BookPresenter> md;
	public final MainActivityUIBase app;
	
	public MdictServerLaptop server;

	public static boolean isNeoJRE=false;
	
	//构造
	public PlainDictionaryPcJFX() {
		super();
		bundle = ResourceBundle.getBundle("UIText" , Locale.getDefault());
		SU.debug=true;
		app = new MainActivityUIBase() {
			public String etSearch_getText() {
				return etSearch.getText();
			}
		};
		md = app.loadManager.md;
	}

	public class AppHandle {
		int flag;
		public void setFlag(int val) {
			flag=val;
		}
		public int getFlag() {
			return flag;
		}
		public void setPos(String pos) {
			app.currentDisplaying=Integer.parseInt(pos);
			CMN.Log("currentDisplaying" +pos);
		}

		public void log(String val) {
			CMN.Log("web_log: "+val, val.getClass());
		}

		public void setLastMd(String val) {
			int idx = IU.parsint(val, 0);
			app.currentDictionary = app.loadManager.md_get(idx);
			app.adapter_idx=idx;
		}

		public String getCurrentPageKey(boolean bAppendFlag){
			try {
				if (searchInPageBox != null && searchInPageBox.getParent() != null) {
					String ret = searchInPageBox.textBox.getText();
					ret = ret.replace("\\", "\\\\");
					if (bAppendFlag) {
						ret = MakeRCSP() + "x" + ret;
					}
					//ret = URLEncoder.encode(ret, "UTF-8");
					CMN.Log("sending...", ret);
					return ret;
				}
			} catch (Exception e) {
				CMN.debug(e);
			}
			return null;
		}

		public String getKeyWord() {
			return etSearch.getText();
		}

		public void recordRecords(String record){
			app.record_for_mirror=record;
		}

		public void setCombinedSeaching(boolean val){
			opt.SetCombinedSearching(val);
		}

		public void saveCurrent(String val){
			//CMN.Log("saveCurrent", val);
			FileChooser fileChooser = new FileChooser();
			fileChooser.getExtensionFilters().addAll(
				new ExtensionFilter("Html file", "*.html")
			);
			fileChooser.setInitialDirectory(opt.projectPath);
			String[] arr = val.split("@");
			int len=arr.length;
			if(len>1){
				try {
					int md_i=Integer.parseInt(arr[0]);
					BookPresenter book = md.get(md_i);
					int[] arr2 = new int[len-1];
					for (int i = 1; i < len; i++)
						arr2[i-1]=Integer.parseInt(arr[i]);
					PlainMdict mdTmp = book.getMdict();
					fileChooser.setInitialFileName(mdTmp._Dictionary_fName+" - "+mdTmp.getEntryAt(arr2[0])+"."+arr2[0]);
					File file = fileChooser.showSaveDialog(stage);
					if(file!=null){
						mdTmp.savePagesTo(file, arr2);
					}
				} catch (IOException e) { CMN.Log(""+e); }
			}
		}

		public void InPageSearch(String text){
			AttachSearchInPage(true, text);
		}

		public boolean getInPageSearch(String text){
			return opt.GetSearchInPage() && searchInPageBox.textBox.getText().trim().length()>0;
		}

		public void handleWebLink(String url){
			if(opt.GetBrowserPathOverwriteEnabled() &&opt.getBrowserPathOverwrite()!=null && new File(opt.getBrowserPathOverwrite()).exists()){
				List<String> cmd = new ArrayList<>();
				cmd.add(opt.getBrowserPathOverwrite());
				cmd.add(url);
				if(opt.getBrowserArgs()!=null)
					addArgs(cmd, opt.getBrowserArgs());
				ProcessBuilder process = new ProcessBuilder(cmd);
				try {
					process.start();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}else {
				Desktop dp = Desktop.getDesktop();
				if (dp.isSupported(Desktop.Action.BROWSE)) {
					try {
						dp.browse(new URI(url));
					} catch (Exception e) { CMN.Log(""+e); }
				}
			}
		}

		private void addArgs(List<String> cmd, String args) {
			String[] arr = args.split("&");
			for(String aI:arr){
				if(aI.length()>0)
					cmd.add(aI);
			}
		}

		public void handleWebSearch(String url, int slot){
			String urlbase = (opt.GetSearchUrlOverwriteEnabled()?opt.GetSearchUrlOverwrite():opt.SearchUrlDefault);
			if(slot==1)
				urlbase=opt.GetSearchUrlMiddle(urlbase);
			else if(slot==2)
				urlbase=opt.GetSearchUrlRight(urlbase);
			handleWebLink(urlbase.replace("%s",url));
		}

		public void handlePdfLink(String url){
			try {
				url=URLDecoder.decode(url,"utf8").substring(6);
				int page=-1;
				try {
					String[] arr = url.split("#");
					page=Integer.parseInt(arr[1]);
					url=arr[0];
				} catch (Exception ignored) { }
				getPdfFolders();
				File path=new File(url);
				if(!path.exists()){
					url=path.getName();
					for(mFile f:DocumentIncludePaths){
						File scanned = ScanPdfForName(url, f);
						if(scanned!=null) {
							path=scanned;
							break;
						}
					}
				}
				if(path.exists()){
					List<String> cmd = new ArrayList<>();
					cmd.add(opt.GetPdfOverwriteEnabled()?opt.GetPdfOverwrite():"acrobat");
					if(page>=0) {
						addArgs(cmd, opt.GetPdfArgsOverwrite().replace("$P", ""+page));
					}
					cmd.add(path.getAbsolutePath());
					ProcessBuilder process = new ProcessBuilder(cmd);
					process.start();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		public void handleLvResize(int size){
			WebLvSize=size;
		}

		public void reloadDict(int idx){ // nimpl
//			PlainMdict mdTmp = md.get(idx).getMdict();
//			try {
//				PlainMdict mdNew = new PlainMdict(mdTmp.f(), opt);
//				md.set(idx, mdNew);
//				if(currentDictionary==mdTmp)
//					currentDictionary=mdNew;
//				engine.executeScript("ScanInDicts();"+"reloaded("+idx+");");
//			} catch (Exception e) {
//				e.printStackTrace();
//			}
		}

		public void openFolder(int idx){
			try {
				Desktop.getDesktop().open(md.get(idx).bookImpl.getFile().getParentFile());
			} catch (Exception e) { e.printStackTrace(); }
		}

		Object Runtime = new Object(){
			int id;
		};
		
		public String getUILanguage() {
			return "en";
		}
	}

	private File ScanPdfForName(String name, mFile f) {
		if(f.isDirectory()){
			if(new File(f, name).exists()){
				return new File(f, name);
			}
			if(f.isTree){
				String[] arr = f.list();
				if(arr!=null){
					StringBuilder sb=new StringBuilder(f.getAbsolutePath()).append(File.separator);
					int baseLen=sb.length();
					for(String sI:arr){
						sb.setLength(baseLen);
						mFile fI=new mFile(sb.append(sI).toString(), true);
						if(fI.isDirectory())
							return ScanPdfForName(name, fI);
					}
				}
			}
		}
		return null;
	}

	private void getPdfFolders() {
		if(DocumentIncludePaths==null){
			DocumentIncludePaths=new ArrayList<>(12);
			File def = new File(PlainDictAppOptions.projectPath, "CONFIG/PDFolders.lst");
			if(def.exists())
				try {
					BufferedReader in = new BufferedReader(new FileReader(def));
					String line;
					int idx=0;
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
							mFile f=new mFile(line, isTree);
							if(!disabled){
								DocumentIncludePaths.add(f);
							}
							idx++;
						}
					}
					in.close();
				} catch (IOException e2) {
					e2.printStackTrace();
				}
		}
	}
	public class SimpleIdHandle{
		int get(){ return 0; }
	}

	AppHandle AppMessenger = new AppHandle();
	SimpleIdHandle simpleId = new SimpleIdHandle();

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
		} catch (Exception e) {
			CMN.debug(e);
			return null;
		}
	}

	final Runnable maximizeRunner = () -> SyncPaneToMain(contextDialog);

	Scene scene;
	WebView view;
	static int port=8080;
	@Override
	public void start(javafx.stage.Stage stage_) throws Exception {
		//sun.net.http.allowRestrictedHeaders=true;
		ScanSettings(new File(PlainDictAppOptions.projectPath,"settings.xml"));
		server = new MdictServerLaptop(port, app);
		//scanInFiles();
		toolBar = new MenuBar();
		stage = stage_;
		stage.setTitle("平典");
		stage.setOnHidden(e -> {
			if(!stage.isMaximized() && !stage.isIconified()) {
				opt.setScreenW((int)scene.getWidth());
				opt.setScreenH((int)scene.getHeight());
				opt.setScreenX((int)stage.getX());
				opt.setScreenY((int)stage.getY());
			}
			DumpSettings(new File(PlainDictAppOptions.projectPath,"settings.xml"));
			server.stop();
			Platform.exit();
		});
		stage.focusedProperty().addListener((observable, oldValue, newValue) -> {
			if(newValue){
				if((ThirdFlag&1)!=0){
					DocumentIncludePaths=null;
					ThirdFlag&=~1;
				}
				if(opt.GetAutoPaste()){
					checkClipBoard();
				}
				boolean refreshAllDicts=true;
				if(refreshAllDicts) {
					for(BookPresenter mdTmp:md) {
//						mdTmp.handleDebugLines();
					}
				} else {
//					currentDictionary.handleDebugLines();
				}
			}
		});
		stage.maximizedProperty().addListener((observable, oldValue, newValue) -> {
			if(newValue){
				if(advancedSearchDialog!=null && !opt.GetDetachAdvSearch()) {
					double sh = Screen.getPrimary().getVisualBounds().getHeight();
					double h = sh * 0.75;
					advancedSearchDialog.setX(WebLvSize);
					advancedSearchDialog.setY(sh - h);
					advancedSearchDialog.setHeight(h);
				}
			}
			if(contextDialog!=null){
				Platform.runLater(maximizeRunner);
			}
		});
		stage.heightProperty().addListener((arg0, v1, v2) -> {
			if(!stage.isMaximized() && advancedSearchDialog!=null && !opt.GetDetachAdvSearch()) {
				//advancedSearchDialog.setX(advancedSearchDialog.xProperty().doubleValue()+v2.doubleValue()-v1.doubleValue());
				advancedSearchDialog.setHeight(v2.doubleValue());
			}
			if(contextDialog!=null) {
				contextDialog.setHeight(stage.getHeight());
			}
		});
		stage.widthProperty().addListener((arg0, v1, v2) -> {
			if(!stage.isMaximized() && contextDialog!=null) {
				contextDialog.setWidth(stage.getWidth());
			}
		});
		stage.xProperty().addListener((arg0, v1, v2) -> {
			if(!stage.isMaximized() && advancedSearchDialog!=null && !opt.GetDetachAdvSearch()) {
				//advancedSearchDialog.setX(advancedSearchDialog.xProperty().doubleValue()+v2.doubleValue()-v1.doubleValue());
				advancedSearchDialog.setX(v2.doubleValue()-advancedSearchDialog.getScene().getWidth()-3);
			}
			if(contextDialog!=null) {
				contextDialog.setX(v2.doubleValue());
			}
		});
		stage.yProperty().addListener((arg0, v1, v2) -> {
			if(!stage.isMaximized() && advancedSearchDialog!=null && !opt.GetDetachAdvSearch()) {
				//advancedSearchDialog.setY(advancedSearchDialog.yProperty().doubleValue()+v2.doubleValue()-v1.doubleValue());
				advancedSearchDialog.setY(v2.doubleValue());
			}
			if(contextDialog!=null) {
				contextDialog.setY(v2.doubleValue()+(stage.getHeight()+toolBar.getHeight()-10-stage.getScene().getHeight()));
			}
		});
		//        setIconImage(Toolkit.getDefaultToolkit().createImage("G:\\.0PtClm\\Muse\\_All_the_spirites\\app图标\\PLOD\\launcherMax_white.png"));

		if(opt.getScreenX()!=-1) stage.setX(opt.getScreenX());
		if(opt.getScreenY()!=-1) stage.setY(opt.getScreenY());
		scene = new Scene(new VBox(), opt.getScreenW(), opt.getScreenH());

		//开始画菜单
		// + File
		Menu menuFile = new Menu(bundle.getString("file"));
		MenuItem add = new MenuItem(bundle.getString(UI.open));
		add.setId(UI.open);
		MenuItem manager = new MenuItem(bundle.getString(UI.manager));
		manager.setId(UI.manager);
		MenuItem mainfolder = new MenuItem(bundle.getString(UI.mainfolder));
		mainfolder.setId(UI.mainfolder);
		menuFile.getItems().addAll(add,manager,mainfolder);

		// + View
		Menu menuView = new Menu(bundle.getString("view"));
		MenuItem browser = new MenuItem(bundle.getString(UI.browser));
		browser.setId(UI.browser);
		MenuItem searchpage = new MenuItem(bundle.getString(UI.searchpage));
		searchpage.setId(UI.searchpage);
		menuView.getItems().addAll(browser, searchpage);

		add.setAccelerator(KeyCombination.valueOf("CTRL+O"));
		manager.setAccelerator(KeyCombination.valueOf("CTRL+M"));
		browser.setAccelerator(KeyCombination.valueOf("CTRL+B"));
		searchpage.setAccelerator(KeyCombination.valueOf("CTRL+F"));

		add.setOnAction(this);
		manager.setOnAction(this);
		mainfolder.setOnAction(this);
		browser.setOnAction(this);
		searchpage.setOnAction(this);

		// + qiehuan
		Label qiehuanLabel = new Label(bundle.getString(UI.switchdict));
		qiehuanLabel.setId(UI.switchdict);
		qiehuanLabel.setOnMouseClicked(this);
		Menu qiehuanMenu = new Menu("", qiehuanLabel);
		qiehuanLabel.setPadding(new Insets(5,10,5,10));
		qiehuanMenu.setStyle("-fx-padding:0 0 0 0;");

		// + advcanced search
		advancedSearchLabel = new Label(bundle.getString(UI.advsearch));
		advancedSearchLabel.setId(UI.advsearch);
		advancedSearchLabel.setOnMouseClicked(this);
		Menu advancedSearchMenu = new Menu("", advancedSearchLabel);
		advancedSearchLabel.setPadding(new Insets(5,10,5,10));
		advancedSearchMenu.setStyle("-fx-padding:0 0 0 0;");

		// + settings
		Label settings = new Label(bundle.getString(UI.settings));
		settings.setId(UI.settings);
		settings.setOnMouseClicked(this);
		Menu menuSettings = new Menu("", settings);
		settings.setPadding(new Insets(5,10,5,10));
		menuSettings.setStyle("-fx-padding:0 0 0 0;");

		// + degbug button
		Label gcLabel = new Label("GC");
		gcLabel.setId("gc");
		gcLabel.setOnMouseClicked(this);
		Menu gc = new Menu("", gcLabel);
		gcLabel.setPadding(new Insets(5,10,5,10));
		gc.setStyle("-fx-padding:0 0 0 0;");

		// +++
		toolBar.getMenus().addAll(menuFile,advancedSearchMenu, menuSettings, menuView, qiehuanMenu, gc);

		view = new WebView();
		view.setFontScale(1.25);
		view.setZoom(1.25f);
		
		engine = view.getEngine();
		engine.setJavaScriptEnabled(true);
		loadURL("http://127.0.0.1:"+port);
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
			//LogCorsPolicy();
			if(!message.startsWith("http://127.0.0.1:"+port+"/"))
				CMN.Log("Console: ",message," [" + (sourceId==null?sourceId:sourceId.replace("http://127.0.0.1:"+port+"/", "host")),":",lineNumber, "] ");
		});
		
		engine.getLoadWorker().stateProperty().addListener((ov, oldState, newState) -> {
				if (newState == State.SUCCEEDED) {
					JSObject win = (JSObject) engine.executeScript("window");
					win.setMember("app", AppMessenger);
					win.setMember("sid", simpleId);
					CMN.Log("添加了！！！");
					//win.setMember("chrome", AppMessenger);
					//win = (JSObject)engine.executeScript("window.chrome");
					//win.setMember("Runtime", AppMessenger);
				}
			}
		);

		view.getStyleClass().add("browser");
		((VBox) scene.getRoot()).getChildren().addAll(toolBar);
		searchBox = new SearchBox();
		//box.searchButton.setVisible(false);
		etSearch = searchBox.textBox;//new TextField ();
		searchBox.searchButton.setOnMouseClicked(e -> {etSearch.getOnKeyPressed().handle(new KeyEvent(KeyEvent.KEY_PRESSED, null, null, KeyCode.ENTER, false, false, false, false));});
		etSearch.clear();
		etSearch.setOnKeyPressed(event -> {
			//executeJavaScript("lookup('"+etSearch.getText()+"')");
			if(event.getCode()==KeyCode.ENTER) {
				executeJavaScript("enter=1;lookup(\""+etSearch.getText().replace("\"", "\\\"")+"\")");
			}
		});
		//etSearch.setOnKeyReleased(new EventHandler<KeyEvent>() {
		//	@Override
		//	public void handle(KeyEvent event) {
		//		executeJavaScript("lookup('"+etSearch.getText()+"')");
		//	}});
		final ChangeListener<String> textListener =
				(ObservableValue<? extends String> observable,
				 String oldValue, String newValue) -> {
					executeJavaScript("lookup(\""+newValue.replace("\"", "\\\"")+"\")");
					searchBox.clearButton.setVisible(newValue.length() != 0);
				};
		etSearch.textProperty().addListener(textListener);

		topGrid = new GridPane();

		topGrid.setHgap(0);
		topGrid.setPadding(new Insets(5, 12, 0, 2));
		topGrid.add(searchBox, 0, 0);
		GridPane.setHgrow(searchBox, Priority.ALWAYS);
		if(opt.GetSearchInPage())
			AttachSearchInPage(false, null);
		int gridHeight=50;
		topGrid.setPrefHeight(gridHeight);
		topGrid.setMaxHeight(gridHeight);
		topGrid.setMinHeight(gridHeight);
		//height? 40.0?
		((VBox) scene.getRoot()).getChildren().add(topGrid);
		((VBox) scene.getRoot()).getChildren().add(view);
		VBox.setVgrow(view, Priority.ALWAYS);
		stage.setScene(scene);
		stage.getIcons().add(new Image(MdictServer.class.getResourceAsStream("Mdict-browser/MdbR/MdbR.png")));

		stage.show();

		try {
			server.start();
		} catch (Exception e) {
			CMN.debug(e);
		}
		//setBackground(Color.black);
		//browser.loadContent("<a onclick=\"new Audio('https://www.collinsdictionary.com/sounds/6/669/66956/66956.mp3').play();\">AUDIO TEST</a>");
		stage.addEventFilter(KeyEvent.KEY_PRESSED, new EventHandler<KeyEvent>() {
			final KeyCombination keyComb = KeyCombination.valueOf("CTRL+ALT+S");
			final KeyCombination keyComb2 = KeyCombination.valueOf("CTRL+SHIFT+V");
			public void handle(KeyEvent e) {
				if (keyComb.match(e)) {
					PlainDictionaryPcJFX.this.handle(new VirtualEvent(settings));
					e.consume();
				} else if (keyComb2.match(e)) {
					checkClipBoard();
					etSearch.requestFocus();
					e.consume();
				} else if (EscComb.match(e)) {

				} else if (AltDComb.match(e)) {
					PlainDictionaryPcJFX.this.handle(new ActionEvent(qiehuanLabel,null));
				}
			}
		});

		if(opt.GetShowAdvanced())
			Platform.runLater(() -> this.handle(new ActionEvent(advancedSearchLabel,null)));

		// tg
		//server.md.add(new MobiBook("D:\\Downloads\\编码_隐匿在计算机软硬件背后的语言.azw_BR7KQB23ROBNK5RQIY6KHTHSP46SFR34.azw", opt));
		if(SU.debug) {
			//tg
			//Platform.runLater(() -> this.handle(new ActionEvent(settings,null)));
			new Timer().schedule(new TimerTask() {
				@Override
				public void run() {
					Platform.runLater(() -> {
						//etSearch.setText("we're");
						if(searchInPageBox!=null)searchInPageBox.textBox.setText("happy");
						if(advancedSearchDialog!=null) advancedSearchDialog.etSearch.setText("人");
						//etSearch.setText("happiness");
						try {
							final Socket socket = new Socket("localhost", port);
							socket.getOutputStream();
							socket.getOutputStream().write("GET / HTTP/1.1".getBytes());
							socket.close();
						} catch (Exception e) {
							CMN.debug(e);
						}
						//this.handle(new ActionEvent(manager,null));
						//this.handle(new ActionEvent(qiehuanLabel,null));
					});
				}
			},800);
		}
		
	}

	// click
	@Override
	public void handle(Event event) {
		switch(((Styleable)event.getSource()).getId()){
			case UI.open: {
				FileChooser fileChooser = new FileChooser();
				fileChooser.getExtensionFilters().addAll(
					new ExtensionFilter("mdict file", "*.mdx")
				);
				fileChooser.setInitialDirectory(new File(opt.GetLastMdlibPath()));
				List<File> files = fileChooser.showOpenMultipleDialog(stage);
				if(files!=null) {
					int sizebefore=md.size();
					HashSet<String> mdict_cache = new HashSet<>(md.size());
					for(BookPresenter mdTmp:md) mdict_cache.add(mdTmp.getPath());
					for(File fI:files) {
						String fileNameKey=fI.getPath();
						if(!mdict_cache.contains(fileNameKey))
							try {
								md.add(app.new_book(fileNameKey));
								app.loadManager.md_size = md.size();
								mdict_cache.add(fileNameKey);
							} catch (Exception e) { e.printStackTrace(); }
					}
					engine.executeScript("ScanInDicts();");
					if(sizebefore!=md.size()){
						AppendMdicts(sizebefore);
					}
				}
			} break;
			case UI.switchdict: {//切换词典
				if(!DismissSyncedPane(DictPickerDialog.class)){
					if(pickDictDialog==null)
						pickDictDialog = new DictPickerDialog(this, readFileSets(), opt, bundle);
					SyncPaneToMain(pickDictDialog);
					contextDialog=pickDictDialog;
					pickDictDialog.show();
				}
			} break;
			case UI.manager:{
				Stage mDialog;
				if(managerDialog==null || managerDialog.get()==null || managerDialog.get().getScene()==null){
					managerDialog = new WeakReference<>(mDialog=new Stage());
					mDialog.setTitle(bundle.getString("manager")+" - "+opt.getCurrentPlanName());
					mDialog.initModality(Modality.WINDOW_MODAL);
					mDialog.initOwner(stage);
					ManagerFragment managerFragment = new ManagerFragment(this, server, opt);
					Scene dialogScene = new Scene(managerFragment, 800, 600);
					mDialog.addEventFilter(KeyEvent.KEY_PRESSED, new EventHandler<KeyEvent>() {
						public void handle(KeyEvent e) {
							if (EscComb.match(e)) {
								mDialog.hide();
								e.consume();
							}
						}
					});
					mDialog.onCloseRequestProperty().set((e -> {
						//CMN.show("close");
						if(managerFragment.tableView.isDirty) {
							ObservableList<BookPresenter> mdModified = managerFragment.tableView.getItems();
							managerFragment.try_write_configureLet(getCurrentSetFile());
							md.ensureCapacity(mdModified.size());
							md.clear();
							server.currentFilter.clear();
							for(BookPresenter mdTmp:mdModified) {
								boolean disabled=managerFragment.rejector.contains(mdTmp.getPath());
								//CMN.Log(mdTmp.getClass().getName(), mdTmp._Dictionary_fName, isFiler, disabled);
								if(disabled) continue;
								if(mdTmp instanceof MagentTransient) {
									MagentTransient magent = (MagentTransient) mdTmp;
									if (mdTmp.f().exists()) {
										try {
											mdTmp=app.new_book(mdTmp.f().getPath());
											md.add(mdTmp);
										} catch (Exception ignored) {
											CMN.Log(e);
										}
									}
								}
							}
							engine.executeScript("ScanInDicts();");
						}
						//event.consume();
					}));
					mDialog.setScene(dialogScene);
				}else{
					mDialog = managerDialog.get();
					((ManagerFragment)mDialog.getScene().getRoot()).tableView.refresh();
				}
				mDialog.show();
			} break;
			case UI.mainfolder:{
				DirectoryChooser fileChooser = new DirectoryChooser();
				fileChooser.setInitialDirectory(new File(opt.GetLastMdlibPath()));
				File path = fileChooser.showDialog(stage);
				if(path!=null){
					opt.setLastMdlibPath(path.getAbsolutePath());
				}
			} break;
			case UI.browser:{
				try {
					AppMessenger.handleWebLink("http://127.0.0.1:"+port+"/MIRROR.jsp?DX=" + app.adapter_idx + "&POS=" + app.currentDisplaying + "&KEY=" + URLEncoder.encode(etSearch.getText(), "UTF-8"));
				} catch (UnsupportedEncodingException e1) {
					e1.printStackTrace();
				}
				//cmd.add("--start-maximized");
				//cmd.add("--incognito");
				//cmd.add("--user-data-dir=D:/test");
			} break;
			case UI.searchpage:{
				if(opt.SetSearchInPage(!opt.GetSearchInPage()))
					AttachSearchInPage(true, null);
				else
					DettachSearchInPage();
			} break;
			case UI.advsearch:{
				if(advancedSearchDialog==null) {
					advancedSearchDialog = new AdvancedSearchDialog(this);
					advancedSearchDialog.setWidth(350);
				}
				if(opt.SetShowAdvanced(!advancedSearchDialog.isShowing())){
					advancedSearchDialog.show();
					if(!stage.isMaximized()) {
						//advancedSearchDialog.setX(stage.xProperty().doubleValue()-335);
						advancedSearchDialog.setX(stage.getX()-advancedSearchDialog.getScene().getWidth()-3);
						advancedSearchDialog.setY(stage.getY());
						advancedSearchDialog.setHeight(stage.getHeight());
					}
				} else {
					advancedSearchDialog.hide();
				}
			} break;
			case UI.settings:{
				if(!DismissSyncedPane(SettingsDialog.class)){
					SettingsDialog mSettingsDialog;
					if(settingDialog==null || settingDialog.get()==null){
						settingDialog = new WeakReference<>(mSettingsDialog=new SettingsDialog(stage, opt, bundle));
					}else{
						mSettingsDialog = settingDialog.get();
					}
					SyncPaneToMain(mSettingsDialog);
					contextDialog=mSettingsDialog;
					mSettingsDialog.show();
				}
			} break;
			case "gc":
				System.gc();
				System.gc();
			break;
		}
	}
	
	private void checkClipBoard() {
		if(clipboard==null) clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		Transferable trans = clipboard.getContents(null);
		if (trans != null) // && trans.isDataFlavorSupported(DataFlavor.stringFlavor)
		try {
			String text = (String) trans.getTransferData(DataFlavor.stringFlavor);
			if (!text.equals(lastPasteItem)) {
				CMN.Log("剪贴板", text);
				etSearch.setText(text);
				lastPasteItem = text;
			}
		} catch (Exception e) {
			CMN.debug(e);
		}
	}

	public File getCurrentSetFile() {
		return new File(opt.projectPath,opt.GetDirectSetLoad()?"CONFIG/"+opt.getCurrentPlanName()+".set":"default.txt");
	}

	private ArrayList<File> readFileSets() {
		if(fileSets==null){
			File CONFIG = new File(PlainDictAppOptions.projectPath,"CONFIG");
			String[] arr = CONFIG.list();
			if (arr==null) arr = new String[0];
			fileSets = new ArrayList<>(arr.length);
			for(String sI:arr){
				if(sI.endsWith(".set"))
					fileSets.add(new File(CONFIG, sI));
			}
		}
		return fileSets;
	}


	private void SyncPaneToMain(Stage pane) {
		pane.setHeight(stage.getScene().getHeight());
		pane.setWidth(stage.getWidth());
		pane.setX(stage.getX());
		pane.setY(stage.getY()+(stage.getHeight()+toolBar.getHeight()-10-stage.getScene().getHeight()));
		if(pane.getOwner()==null){
			pane.initModality(Modality.NONE);
			pane.initOwner(stage);
			if(pane == pickDictDialog) {
				pickDictDialog.SyncPaneToMain(this);
			} else {
				pane.setOnCloseRequest(event -> {
					((SettingsDialog)pane).destroyView();
					//pane.getScene().getWindow().hide();
					contextDialog=null;
				});
			}
		}
	}

	private boolean DismissSyncedPane(Class tagetClass) {
		if(contextDialog!=null)
		if(contextDialog.isShowing()){
			boolean ret=tagetClass.isInstance(contextDialog);
			contextDialog.close();
			contextDialog.getOnCloseRequest().handle(null);
			contextDialog=null;
			return ret;
		}
		else contextDialog=null;
		return false;
	}

	private int MakeRCSP() {
		return opt.FetPageSearchUseRegex()|
				opt.FetPageSearchCaseSensitive()<<1|
				opt.FetPageSearchSeparateWord()<<2|
				opt.FetPageWithoutSpace()<<3
				;
	}

	private void AttachSearchInPage(boolean focus, String text) {
		if(col1==null){
			dv = new DragResizeView(opt.getSearchBoxPercent());
			dv.gl_to_guard=topGrid;
			topGrid.getColumnConstraints().add(dv.col1=col1=new ColumnConstraints());
			GridPane.setHgrow(searchInPageBox=new SearchBox3rd(), Priority.ALWAYS);
			searchInPageBox.textBox.textProperty().addListener((observable, oldValue, newValue) -> {
				executeJavaScript(new StringBuilder()
						.append("highlight('").append(MakeRCSP()).append("x")
						.append(AppMessenger.getCurrentPageKey(false))
						.append("',").append(");").toString());
			});
			searchInPageBox.downButton.setOnAction(event -> executeJavaScript("jumpHighlight(1)"));
			searchInPageBox.upButton.setOnAction(event -> executeJavaScript("jumpHighlight(-1)"));
			col1.percentWidthProperty().addListener((observable, oldValue, newValue) -> {
				if(opt.GetSearchInPage()) opt.setSearchBoxPercent(newValue.doubleValue());
			});
		}

		col1.setPercentWidth(opt.getSearchBoxPercent());

		if(dv.getParent()==null){
			topGrid.add(dv, 1, 0);
			topGrid.add(searchInPageBox, 2, 0);
		}
		if(focus) searchInPageBox.textBox.requestFocus();
		if(text==null)
			text=searchInPageBox.textBox.getText().trim();
		else{//todo remove listener
			searchInPageBox.textBox.setText(text);
		}
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

	private void AppendMdicts(int startIndex) {
		File def = getCurrentSetFile();
		try {
			BufferedWriter out = new BufferedWriter(new FileWriter(def,true));
			String parent = new File(opt.GetLastMdlibPath()).getAbsolutePath()+File.separatorChar;
			for (int i = startIndex; i < md.size(); i++) {
				PlainMdict mdTmp=md.get(i).getMdict();
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
	
	static{
		System.setProperty("sun.net.http.allowRestrictedHeaders", "true");
	}

	private static void LogCorsPolicy() {
		String message = System.getProperty("sun.net.http.allowRestrictedHeaders");
		CMN.Log("message", message);
	}
	
	public static void main(String[] args) {
		//CMN.show(System.getProperty("java.version"));
		//1.8.0_171
		//10.0.2
		LogCorsPolicy();
		
		Locale.setDefault(Locale.CHINA);
		File projectPath = PU.getProjectPath();
		if(projectPath==null) {
			projectPath = new File(System.getProperty("user.home"));
		}
		if(!projectPath.isDirectory()) {
			try {
				projectPath = new File("").getCanonicalFile();
			} catch (IOException ignored) { }
		}
		File installPath = new File(projectPath, "PDInstall");
		if(installPath.isFile()) {
			try {
				BufferedReader fin = new BufferedReader(new FileReader(installPath));
				installPath = new File(fin.readLine());
				if(installPath.isDirectory()) {
					projectPath = installPath;
				}
				fin.close();
			} catch (IOException ignored) { }
		}
		
		PlainDictAppOptions.projectPath = projectPath;
		String VersionCode = System.getProperty("java.version");
		CMN.Log("projectPath", PlainDictAppOptions.projectPath);
		CMN.Log("usrHome", System.getProperty("user.home"));
		CMN.Log("VersionCode", VersionCode);
		if(VersionCode.startsWith("9") || VersionCode.startsWith("10")) isNeoJRE=true;
		launch(args);
	}

	private PlainMdict new_mdict(String line, PlainDictAppOptions opt) throws IOException {
		if(com.knziha.plod.dictionary.mdict.mobiReg.matcher(line).find())
			return new MobiBook(new File(line), opt);
		return new PlainMdict(new File(line), opt);
	}
	static class AdvancedScopedSearchLayer extends AdvancedSearchLogicLayer{
		AdvancedScopedSearchLayer(PlainDictAppOptions opt, ArrayList<BookPresenter> md, Tab chiefAmbassador, Text statusBar, int type) {
			super(opt, new ArrayList<>(md), chiefAmbassador, statusBar, type);
			combining_search_tree = new ArrayList<>();
			for (int i = 0; i < md.size(); i++) {
				combining_search_tree.add(new ArrayList[]{});
			}
		}
	}

	public void ScanSettings(File file) {
		try {
			JAXBContext context = JAXBContext.newInstance(PlainDictAppOptions.class);
			Unmarshaller um = context.createUnmarshaller();
			opt = (PlainDictAppOptions) um.unmarshal(file);
		} catch (Exception e) { CMN.debug(e); }
		if(opt==null)
			opt=new PlainDictAppOptions();
		FFStamp=opt.getFirstFlag();
	}

	public void DumpSettings(File file) {
		try {
			JAXBContext context = JAXBContext.newInstance(PlainDictAppOptions.class);
			Marshaller m = context.createMarshaller();
			m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			m.marshal(opt, file);
		} catch (Exception e) {
			CMN.Log(e);
		}
	}

	static class mFile extends File{
		public mFile(String pathname, boolean _isTree) {
			super(pathname);
			isTree=_isTree;
		}
		boolean isTree;
	}
}
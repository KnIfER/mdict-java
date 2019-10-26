package com.knziha.plod.PlainDict;

import com.knziha.plod.dictionary.Utils.IU;
import com.knziha.plod.dictionary.Utils.SU;
import com.knziha.plod.dictionarymanager.DictPickerDialog;
import com.knziha.plod.dictionarymanager.ManagerFragment;
import com.knziha.plod.dictionarymodels.*;
import com.knziha.plod.settings.SettingsDialog;
import com.knziha.plod.widgets.*;
import com.knziha.plod.widgets.splitpane.HiddenSplitPaneApp;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.concurrent.Worker.State;
import javafx.css.Styleable;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.*;
import javafx.stage.FileChooser.ExtensionFilter;
import netscape.javascript.JSObject;
import org.w3c.dom.NodeList;
import test.CMN;

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
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Timer;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static javafx.concurrent.Worker.State.FAILED;
import static org.nanohttpd.protocols.http.response.Response.newFixedLengthResponse;

public class PlainDictionaryPcJFX extends Application {
	GridPane topGrid;
	SearchBox searchBox;
	TextField etSearch;
	private WebEngine engine;
	javafx.stage.Stage stage;
	Label advancedSearchLabel;
	AdvancedSearchDialog advancedSearchDialog;
	int currentDisplaying=0;
	public static Pattern windowPath=Pattern.compile("^[a-zA-Z]:\\\\.*");
	public final static KeyCombination EscComb = KeyCombination.valueOf("ESC");

	public volatile static boolean fuzzyIsInterrupted;
	public volatile int fuzzyIdx;
	Thread fuzzyThread;
	Timer fuzzyTicker;
	public volatile static boolean fullIsInterrupted;
	public volatile int fullIdx;
	Thread fullThread;
	Timer fullTicker;
	public static int ThirdFlag;

	private final JTextField txtURL = new JTextField();
	private final JProgressBar progressBar = new JProgressBar();
	private ColumnConstraints col1;
	private DragResizeView dv;
	private SearchBox3rd searchInPageBox;
	private PlainDictAppOptions opt;
	private long FFStamp;
	private Stage contextDialog;
	private DictPickerDialog pickDictDialog;
	private MenuBar toolBar;
	private ArrayList<mFile> DocumentIncludePaths;
	private ArrayList<File> DictionarySets;
	private int WebLvSize=195;
	WeakReference<SettingsDialog> settingDialog;
	WeakReference<Stage> managerDialog;
	private Clipboard clipboard;
	private String record_for_mirror;

	public class AppHandle {

		public void setPos(String pos) {
			currentDisplaying=Integer.parseInt(pos);
			CMN.Log("currentDisplaying" +pos);
		}

		public void log(String val) {
			CMN.Log("web_log: "+val, val.getClass());
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

		public String getKeyWord() {
			return etSearch.getText();
		}

		public void recordRecords(String record){
			record_for_mirror=record;
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
			fileChooser.setInitialDirectory(new File(opt.projectPath));
			String[] arr = val.split("@");
			int len=arr.length;
			if(len>1){
				try {
					int md_i=Integer.parseInt(arr[0]);
					mdict mdTmp = md.get(md_i);
					int[] arr2 = new int[len-1];
					for (int i = 1; i < len; i++)
						arr2[i-1]=Integer.parseInt(arr[i]);
					fileChooser.setInitialFileName(mdTmp._Dictionary_fName+" - "+mdTmp.getEntryAt(arr2[0])+"."+arr2[0]);
					File file = fileChooser.showSaveDialog(stage);
					if(file!=null){
						FileOutputStream out = new FileOutputStream(file);
						out.write(mdTmp.getRecordsAt(arr2).getBytes(StandardCharsets.UTF_8));
						out.close();
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

		public void handleWebSearch(String url){
			handleWebLink((opt.GetSearchUrlOverwriteEnabled()?opt.GetSearchUrlOverwrite():opt.SearchUrlDefault).replace("%s",url));
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

		public void reloadDict(int idx){
			mdict mdTmp = md.get(idx);
			try {
				mdict mdNew = new mdict(mdTmp.f().getAbsolutePath());
				md.set(idx, mdNew);
				if(server.currentDictionary==mdTmp)
					server.currentDictionary=mdNew;
				engine.executeScript("ScanInDicts();"+"reloaded("+idx+");");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		public void openFolder(int idx){
			try {
				Desktop.getDesktop().open(md.get(idx).f().getParentFile());// what a shame
			} catch (Exception e) { e.printStackTrace(); }
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
			File def = new File(PU.getProjectPath(),"CONFIG/PDFolders.lst");
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
		bundle = ResourceBundle.getBundle("UIText" , Locale.getDefault());
		if(!new File(PlainDictAppOptions.projectPath).exists()) {

		}
		if(PlainDictAppOptions.userPath==null || !new File(PlainDictAppOptions.userPath).exists()) {
			PlainDictAppOptions.userPath=PlainDictAppOptions.projectPath;
		}
		usrHome = new File(PlainDictAppOptions.userPath,".PLOD.plaindictionary");
		//usrHome.mkdir();
		PlainDictAppOptions.userPath=null;
		SU.debug=true;
	}


	public static class UI{
		public final static String open="open";
		public final static String browser ="browser";
		public final static String searchpage ="searchpage";
		public final static String manager="manager";
		public final static String mainfolder="mainfolder";
		public final static String overwrite_browser="ow_browser";
		public final static String overwrite_browser_search ="ow_search";
		public final static String ow_bsrarg="ow_bsrarg";
		public final static String overwrite_pdf_reader="ow_pdf";
		public final static String overwrite_pdf_reader_args="ow_pdfarg";
		public final static String pdffolders="pdffolders";
		public final static String advsearch="advsearch";
		public final static String switchdict="switchdict";
		public final static String settings="settings";
		public final static String pastebin_fw="pastebin_fw";
	}

	final Runnable maximizeRunner = () -> SyncPaneToMain(contextDialog);

	Scene scene;
	WebView view;
	@Override
	public void start(javafx.stage.Stage stage_) throws Exception {
		ScanSettings(new File(PlainDictAppOptions.projectPath,"settings.xml"));
		server = new MdictServer(8080, opt);
		md = server.md;
		scanInFiles();
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


		EventHandler clicker1 =  event -> {
			switch(((Styleable)event.getSource()).getId()){
				case UI.open:{
					FileChooser fileChooser = new FileChooser();
					fileChooser.getExtensionFilters().addAll(
							new ExtensionFilter("mdict file", "*.mdx")
					);
					fileChooser.setInitialDirectory(new File(opt.GetLastMdlibPath()));
					List<File> files = fileChooser.showOpenMultipleDialog(stage);
					if(files!=null) {
						int sizebefore=md.size();
						HashSet<String> mdict_cache = new HashSet<>(md.size());
						for(mdict mdTmp:md) mdict_cache.add(mdTmp.getPath());
						for(File fI:files) {
							String fileNameKey=fI.getAbsolutePath();
							if(!mdict_cache.contains(fileNameKey))
								try {
									server.md.add(new mdict(fileNameKey));
									mdict_cache.add(fileNameKey);
								} catch (Exception e) { e.printStackTrace(); }
						}
						engine.executeScript("ScanInDicts();");
						if(sizebefore!=md.size()){
							AppendMdicts(sizebefore);
						}
					}
				} break;
				case UI.manager:{
					Stage mDialog;
					if(managerDialog==null || managerDialog.get()==null){
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
								ObservableList<mdict> mdModified = managerFragment.tableView.getItems();
								managerFragment.try_write_configureLet(getCurrentSetFile());
								md.ensureCapacity(mdModified.size());
								md.clear();
								server.currentFilter.clear();
								for(mdict mdTmp:mdModified) {
									boolean isFiler=mdTmp.tmpIsFilter, disabled=managerFragment.rejector.contains(mdTmp.getPath());
									CMN.Log(mdTmp.getClass().getName(), mdTmp._Dictionary_fName, isFiler, disabled);
									if(disabled) continue;
									if(mdTmp instanceof mdict_nonexist)
										continue;
									if(mdTmp instanceof mdict_preempter){
										try {
											mdTmp=new mdict(mdTmp.getPath());
											mdTmp.tmpIsFilter=isFiler;
										} catch (IOException ignored) { CMN.Log(e); continue; }
									}
									if(isFiler)
										server.currentFilter.add(mdTmp);
									else
										md.add(mdTmp);
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
						AppMessenger.handleWebLink("http://127.0.0.1:8080/MIRROR.jsp?DX=" + server.adapter_idx + "&POS=" + currentDisplaying + "&KEY=" + URLEncoder.encode(etSearch.getText(), "UTF-8"));
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
						advancedSearchDialog = new AdvancedSearchDialog();
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
				case UI.switchdict:{
					if(!DismissSyncedPane(DictPickerDialog.class)){
						if(pickDictDialog==null)
							pickDictDialog = new DictPickerDialog(this, ScanSets(), opt, bundle);
						SyncPaneToMain(pickDictDialog);
						contextDialog=pickDictDialog;
						pickDictDialog.show();
					}
				}break;
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
		};

		add.setOnAction(clicker1);
		manager.setOnAction(clicker1);
		mainfolder.setOnAction(clicker1);
		browser.setOnAction(clicker1);
		searchpage.setOnAction(clicker1);

		// + qiehuan
		Label qiehuanLabel = new Label(bundle.getString(UI.switchdict));
		qiehuanLabel.setId(UI.switchdict);
		qiehuanLabel.setOnMouseClicked(clicker1);
		Menu qiehuanMenu = new Menu("", qiehuanLabel);
		qiehuanLabel.setPadding(new Insets(5,10,5,10));
		qiehuanMenu.setStyle("-fx-padding:0 0 0 0;");

		// + advcanced search
		advancedSearchLabel = new Label(bundle.getString(UI.advsearch));
		advancedSearchLabel.setId(UI.advsearch);
		advancedSearchLabel.setOnMouseClicked(clicker1);
		Menu advancedSearchMenu = new Menu("", advancedSearchLabel);
		advancedSearchLabel.setPadding(new Insets(5,10,5,10));
		advancedSearchMenu.setStyle("-fx-padding:0 0 0 0;");

		// + settings
		Label settings = new Label(bundle.getString(UI.settings));
		settings.setId(UI.settings);
		settings.setOnMouseClicked(clicker1);
		Menu menuSettings = new Menu("", settings);
		settings.setPadding(new Insets(5,10,5,10));
		menuSettings.setStyle("-fx-padding:0 0 0 0;");

		// + degbug button
		Label gcLabel = new Label("GC");
		gcLabel.setId("gc");
		gcLabel.setOnMouseClicked(clicker1);
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
			if(!message.startsWith("http://127.0.0.1:8080/"))
				CMN.Log("Console: ",message," [" + sourceId.replace("http://127.0.0.1:8080/", "host"),":",lineNumber, "] ");
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
		((VBox) scene.getRoot()).getChildren().addAll(toolBar);
		searchBox = new SearchBox();
		//box.searchButton.setVisible(false);
		etSearch = searchBox.textBox;//new TextField ();
		searchBox.searchButton.setOnMouseClicked(e -> {etSearch.getOnKeyPressed().handle(new KeyEvent(KeyEvent.KEY_PRESSED, null, null, KeyCode.ENTER, false, false, false, false));});
		etSearch.clear();
		etSearch.setOnKeyPressed(event -> {
			//executeJavaScript("lookup('"+etSearch.getText()+"')");
			if(event.getCode()==KeyCode.ENTER)
				executeJavaScript("enter=1;lookup('"+etSearch.getText()+"')");
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
		stage.getIcons().add(new Image(PlainDictionaryPcJFX.class.getResourceAsStream("Mdict-browser/MdbR/MdbR.png")));

		stage.show();
		try {
			server.start();
		} catch (IOException e) {
			e.printStackTrace();
		}
		//setBackground(Color.black);
		//browser.loadContent("<a onclick=\"new Audio('https://www.collinsdictionary.com/sounds/6/669/66956/66956.mp3').play();\">AUDIO TEST</a>");
		stage.addEventFilter(KeyEvent.KEY_PRESSED, new EventHandler<KeyEvent>() {
			final KeyCombination keyComb = KeyCombination.valueOf("CTRL+ALT+S");
			final KeyCombination keyComb2 = KeyCombination.valueOf("CTRL+SHIFT+V");
			public void handle(KeyEvent e) {
				if (keyComb.match(e)) {
					clicker1.handle(new VirtualEvent(settings));
					e.consume();
				} else if (keyComb2.match(e)) {
					checkClipBoard();
					etSearch.requestFocus();
					e.consume();
				} else if (EscComb.match(e)) {

				}
			}
		});


		if(opt.GetShowAdvanced())
			Platform.runLater(() -> clicker1.handle(new VirtualEvent(advancedSearchLabel)));

		//tg
		//Platform.runLater(() -> clicker1.handle(new VirtualEvent(settings)));
		new Timer().schedule(new TimerTask() {
			@Override
			public void run() {
				Platform.runLater(() -> {
					if(searchInPageBox!=null)searchInPageBox.textBox.setText("happy");
					etSearch.setText("happiness");
					//clicker1.handle(new ActionEvent(manager,null));
					//clicker1.handle(new ActionEvent(qiehuanLabel,null));
				});
			}
		},800);


		server.setOnMirrorRequestListener(uri -> {
			if(uri==null)uri="";
			String[] args = uri.split("&");
			int pos=currentDisplaying; pos=IU.parsint(args[1].split("=")[1]);
			int dx=server.adapter_idx; dx=IU.parsint(args[0].split("=")[1]);
			String key=etSearch.getText();try {key=URLDecoder.decode(args[2].split("=")[1],"UTF-8");}catch(Exception e) {}
			//CMN.show("currentDisplaying"+currentDisplaying);
			//StringBuilder sb=new StringBuilder();
			//NodeList clientIframes = engine.getDocument().getElementsByTagName("iframe");// 会 crash
			////CMN.Log("clientIframes", clientIframes.getLength(), clientIframes);
			//for(int i=0;i<clientIframes.getLength();i++) {
			//	String content = clientIframes.item(i).getTextContent();
			//	CMN.Log("[content mirror : ] ", content);
			//	int split=content.indexOf("@");
			//	if(split==-1) continue; //TODO
			//	int DX=Integer.parseInt(content.substring(0, split));
			//	sb.append("<div class='cp3' onclick='p3(this.nextSibling)'>")
			//			.append(md.get(DX)._Dictionary_fName).append("</div>")
			//			.append("<iframe id='md_").append(DX)
			//			.append("' src='").append("\\content\\").append(content)
			//			.append("' width=\"100%\" frameborder=\"0\" height=\"171\">").append(content).append("</iframe>");
			//}

			return newFixedLengthResponse(server.constructDerivedHtml(key, pos, dx,record_for_mirror));
		});
	}

	String lastPasteItem;
	private void checkClipBoard() {
		if(clipboard==null)
			clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		Transferable trans = clipboard.getContents(null);
		if (trans != null) {
			if (trans.isDataFlavorSupported(DataFlavor.stringFlavor)) {
				try {
					String text = (String) trans.getTransferData(DataFlavor.stringFlavor);
					if(!text.equals(lastPasteItem)){
						CMN.Log("剪贴板", text);
						etSearch.setText(text);
						lastPasteItem=text;
					}
				} catch (Exception e) {

				}
			}
		}
	}

	public File getCurrentSetFile() {
		return new File(opt.projectPath,opt.GetDirectSetLoad()?"CONFIG/"+opt.getCurrentPlanName()+".set":"default.txt");
	}

	private ArrayList<File> ScanSets() {
		if(DictionarySets==null){
			File CONFIG = new File(PlainDictAppOptions.projectPath,"CONFIG");
			String[] arr = CONFIG.list();
			DictionarySets=new ArrayList<>(arr.length);
			for(String sI:arr){
				if(sI.endsWith(".set"))
					DictionarySets.add(new File(CONFIG, sI));
			}
		}
		return DictionarySets;
	}


	private void SyncPaneToMain(Stage pane) {
		pane.setHeight(stage.getScene().getHeight());
		pane.setWidth(stage.getWidth());
		pane.setX(stage.getX());
		pane.setY(stage.getY()+(stage.getHeight()+toolBar.getHeight()-10-stage.getScene().getHeight()));
		if(pane.getOwner()==null){
			pane.initModality(Modality.NONE);
			pane.initOwner(stage);
			if(pane == pickDictDialog){
				pane.setOnCloseRequest(new EventHandler<WindowEvent>() {
					@Override
					public void handle(WindowEvent event) {
						//CMN.Log("???setOnCloseRequest");
						if(pickDictDialog.dirtyFlag!=0){
							server.adapter_idx=pickDictDialog.adapter_idx;
							server.currentDictionary=server.md.get(server.adapter_idx);
							if(!opt.GetDirectSetLoad() && (pickDictDialog.dirtyFlag&0x1)!=0){
								File from;
								if((from=new File(opt.projectPath,"CONFIG/"+opt.getCurrentPlanName()+".set")).exists()){
									try {
										FileChannel inChannel =new FileInputStream(from).getChannel();
										FileChannel outChannel=new FileOutputStream(new File(PU.getProjectPath(),"default.txt")).getChannel();
										inChannel.transferTo(0, inChannel.size(), outChannel);
										inChannel.close();
										outChannel.close();
									} catch (Exception ignored) { }
								}
							}
							engine.executeScript("lastDingX="+server.adapter_idx+"; ScanInDicts();");
							pickDictDialog.dirtyFlag=0;
						}
						if(!(event instanceof VirtualWindowEvent))
							contextDialog=null;
					}
				});
			}else{
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

	private void AttachSearchInPage(boolean focus, String text) {
		if(col1==null){
			dv = new DragResizeView(opt.getSearchBoxPercent());
			dv.gl_to_guard=topGrid;
			topGrid.getColumnConstraints().add(dv.col1=col1=new ColumnConstraints());
			GridPane.setHgrow(searchInPageBox=new SearchBox3rd(), Priority.ALWAYS);
			searchInPageBox.textBox.textProperty().addListener((observable, oldValue, newValue) -> {
				executeJavaScript("highlight('"+newValue+"');");
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
				mdict mdTmp=md.get(i);
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


	File usrHome;
	public static void main(String[] args) {
		//CMN.show(System.getProperty("java.version"));
		//1.8.0_171
		//10.0.2
		Locale.setDefault(Locale.CHINA);
		PlainDictAppOptions.projectPath = PU.getProjectPath();
		PlainDictAppOptions.userPath = System.getProperty("user.home");
		String VersionCode = System.getProperty("java.version");
		CMN.Log("projectPath", PlainDictAppOptions.projectPath);
		CMN.Log("usrHome", System.getProperty("user.home"));
		CMN.Log("VersionCode", VersionCode);
		if(VersionCode.startsWith("9") || VersionCode.startsWith("10")) isNeoJRE=true;
		launch(args);
	}

	public static boolean isNeoJRE=false;

	//HashSet<String> mdlibsCon;



	ArrayList<mdict> md;
	private void scanInFiles() {
		//![] start loading dictionaries
		File def = getCurrentSetFile();      //!!!原配置
		File CONFIG = new File(PlainDictAppOptions.projectPath,"CONFIG");
		if(!CONFIG.isDirectory()) CONFIG.mkdirs();
		int cc=0;
		if(def.exists()){
			try {
				BufferedReader in = new BufferedReader(new FileReader(def));
				String line;
				while((line = in.readLine())!=null){
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
						if(disabled) continue;
						if(nextbrace!=-1)
							line = line.substring(nextbrace+1);
					}
					CMN.Log("?",opt.GetLastMdlibPath(), line, !windowPath.matcher(line).matches() , !line.startsWith("/"));
					if(!windowPath.matcher(line).matches() && !line.startsWith("/"))
					line=opt.GetLastMdlibPath()+File.separator+line;
					try {
						mdict mdtmp = new mdict(line);
						if(isFilter){
							mdtmp.tmpIsFilter=true;
							server.currentFilter.add(mdtmp);
						}else{
							server.md.add(mdtmp);
						}
						//if(mdtmp._Dictionary_fName.equals(opt.getLastMdFn()))
						//	server.adapter_idx = md.size();
					} catch (Exception e) {
						cc++;
						CMN.Log(e);
					}
				}
				in.close();
			} catch (IOException e2) {
				e2.printStackTrace();
			}
		}
		else{
			def.getParentFile().mkdirs();
			try {
				def.createNewFile();
			} catch (IOException ignored) {}
		}

		if(md.size()>0)
			server.currentDictionary = md.get(0);
	}



	public MdictServer server;


	//高级搜索
	class AdvancedSearchDialog extends javafx.stage.Stage {
		TextField etSearch2;
		Button btnSearch2;
		Text statusBar;
		TabPane tabPane;
		SearchBox2nd box2;
		ObservableListmy adapter2;
		ObservableListmy adapter3;
		ListView<Integer> listView;
		ListView<Integer> listView2;
		Pattern currentPattern;
		AdvancedSearchDialog()
		{
			super();
			setTitle(bundle.getString(UI.advsearch));
			getIcons().add(new Image(HiddenSplitPaneApp.class.getResourceAsStream("shared-resources/galaxy.png")));

			initOwner(stage);
			stage.setAlwaysOnTop(false);

			SearchBox box = new SearchBox(); //box.setPadding(new Insets(0,0,10,0));
			box2 = new SearchBox2nd(); //if(isNeoJRE) box2.setPadding(new Insets(10,0,0,0));
			box.textBox.setStyle("-fx-font-size: 12.8pt;");
			box2.setCombinedSearch(opt.GetAdvCombinedSearching());
			box2.isCombinedSearching.addListener((observable, oldValue, newValue)
					-> opt.SetAdvCombinedSearching(newValue));
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
			listView = new ListView<>(adapter2);
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
			listView2 = new ListView<>(adapter3);
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
			etSearch2.setOnKeyPressed(event -> {
				if(event.getCode()==KeyCode.ENTER) {
					switch(tabPane.getSelectionModel().getSelectedIndex()) {
						case 0:{//模糊搜索
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
									String key = etSearch2.getText();
									CMN.show("Searching "+key+" ...");
									long st = System.currentTimeMillis();
									if(box2.isCombinedSearching.get()){
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
											currentPattern=null;
											try { currentPattern=Pattern.compile(key.replace("*", ".*"));
											} catch (Exception ignored) { }
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
						} break;
						case 1:{//全文搜索
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
									if(box2.isCombinedSearching.get()){
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

									adapter3.rec.invalidate();
									adapter3.rec.currentSearchTerm=key.replace("*", ".*");
									statusBar.setText("全文搜索 \""+key+"\" "+(System.currentTimeMillis()-st)*1.f/1000+"s -> "+adapter3.rec.size()+"项");
									mTicker1.cancel();
									System.gc();
									Platform.runLater(() -> {
										if(fullIsInterrupted) return;
										listView2.setItems(null);
										listView2.setItems(adapter3);
										listView2.scrollTo(0);
										((Text)tab2.getGraphic()).setText("");
									});
								}});
							fullThread.start();

							mTicker1.schedule(new TimerTask() {
								@Override
								public void run() {
									int GETDIRTYKEYCOUNTER=0;
									for(int i=0;i<fullIdx;i++){
										GETDIRTYKEYCOUNTER+=md.get(i).getNumberEntries();
									}
									GETDIRTYKEYCOUNTER+=md.get(fullIdx).dirtykeyCounter;
									final int progress = (int) Math.ceil(100.f*GETDIRTYKEYCOUNTER/GETNUMBERENTRIES_1);
									Platform.runLater(() -> {
										((Text)tab2.getGraphic()).setText(progress+"%");
									});
								}
							},0,100);
						} break;
					}
				}
			});

			Scene Scene = new Scene(content, 350, 810);
			setScene(Scene);
			onCloseRequestProperty().set(event -> {
				hide();
				event.consume();
			});
		}


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

				String text = adapter.rec.getResAt(pos);
				Node textTitleView;

				if(getListView()==listView && opt.GetTintWildResult() && currentPattern!=null){
					TextFlow titleFlow = new TextFlow();
					textTitleView=titleFlow;
					ObservableList<Node> textGroup = titleFlow.getChildren();
					Matcher m = currentPattern.matcher(text);
					Text title; int idx=0;
					while(m.find()){
						int start = m.start(0);
						int end = m.end(0);
						title = new Text(text.substring(idx, start));
						title.setFont(Font.font("宋体",18));
						title.setFill(Color.BLACK);
						textGroup.add(title);
						title = new Text(text.substring(start, end));
						title.setFont(Font.font("宋体",18));
						title.setFill(Color.RED);
						textGroup.add(title);
						idx=end;
					}
					if(idx<text.length()-1){
						title = new Text(text.substring(idx));
						title.setFont(Font.font("宋体",18));
						title.setFill(Color.BLACK);
						textGroup.add(title);
					}
				}
				else{
					textTitleView=new Text(text);
				}
				//title.setStyle("-fx-font-style:bold;");

				Text dictName = new Text(md.get(adapter.rec.dictIdx)._Dictionary_fName);
				dictName.setFont(Font.font("宋体",12));
				dictName.setStyle("-fx-fill: #666666;-fx-opacity: 0.66;");
				//Text source = new Text("dd");
				//source.setFont(Font.font(10));

				cell.setTop(textTitleView);
				cell.setLeft(dictName);
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

	}


	public void ScanSettings(File file) {
		if(file.exists())
		try {
			JAXBContext context = JAXBContext
					.newInstance(PlainDictAppOptions.class);
			Unmarshaller um = context.createUnmarshaller();
			opt = (PlainDictAppOptions) um.unmarshal(file);
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
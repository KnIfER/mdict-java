package com.knziha.plod.settings;

import com.knziha.plod.PlainDict.PU;
import com.knziha.plod.PlainDict.PlainDictAppOptions;
import com.knziha.plod.PlainDict.PlainDictionaryPcJFX;
import com.knziha.plod.widgets.splitpane.HiddenSplitPaneApp;
import com.knziha.plod.widgets.toggle.ToggleSwitch;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.Property;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import com.knziha.plod.PlainDict.PlainDictionaryPcJFX.UI;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ResourceBundle;

import static com.knziha.plod.PlainDict.PlainDictionaryPcJFX.EscComb;

//设置界面
public class SettingsDialog extends javafx.stage.Stage implements ChangeListener, EventHandler {
	private final  VBox content;
	PlainDictAppOptions opt;
	ResourceBundle bundle;
	static Insets padding = new Insets(10,10,0,0);
	static Insets padding2 = new Insets(10,10,0,10);
	TextField PathInput1;
	TextField PathInput2;
	private WeakReference<Stage> pdffolderDialog;
	private WeakReference<Stage> regConfDialog;

	public SettingsDialog(javafx.stage.Stage owner, PlainDictAppOptions _opt, ResourceBundle _bundle)
	{
		super();
		opt=_opt;
		bundle=_bundle;
		setTitle(bundle.getString(UI.settings));
		getIcons().add(new Image(HiddenSplitPaneApp.class.getResourceAsStream("shared-resources/settings.png")));
		addEventFilter(KeyEvent.KEY_PRESSED, new EventHandler<KeyEvent>() {
			public void handle(KeyEvent e) {
				if (EscComb.match(e)) {
					hide();
					e.consume();
				}
			}
		});
		content = new VBox(make_switchable_path_picker(true, UI.overwrite_browser,opt.getBrowserPathOverwrite(), opt.GetBrowserPathOverwriteEnabled())
			, make_switchable_path_picker(true, UI.overwrite_browser_search,opt.GetSearchUrlOverwrite(), opt.GetSearchUrlOverwriteEnabled())
			, make_switchable_path_picker(false, UI.overwrite_browser_search1,opt.getSearchUrlMiddle(), false)
			, make_switchable_path_picker(false, UI.overwrite_browser_search2,opt.getSearchUrlRight(), false)
			, make_switchable_path_picker(false, UI.ow_bsrarg,opt.getBrowserArgs(), false)
			, make_switchable_path_picker(true, UI.overwrite_pdf_reader,opt.GetPdfOverwrite(), opt.GetPdfOverwriteEnabled())
			, make_switchable_path_picker(true, UI.overwrite_pdf_reader_args,opt.GetPdfArgsOverwrite(), opt.GetPdfArgsOverwriteEnabled())
			, make_simple_buttons(UI.pdffolders, regex_config)
			, make_simple_seperator()
			, make_3_simple_switcher(bundle,this, regex_enable, opt.GetRegexSearchEngineEnabled(), ps_regex, opt.GetPageSearchUseRegex(), class_case, opt.GetClassicalKeyCaseStrategy())
			, make_3_simple_switcher(bundle,this, tintwild, opt.GetTintWildResult(), remwsize, opt.GetRemWindowSize(), remwpos, opt.GetRemWindowPos())
			, make_3_simple_switcher(bundle,this, tintfull, opt.GetTintFullResult(), doclsset, opt.GetDoubleClickCloseSet(), doclsdict, opt.GetDoubleClickCloseDict())
			, make_simple_seperator()
			, make_3_simple_switcher(bundle,this, dt_dictpic, opt.GetDetachDictPicker(), dt_advsrch, opt.GetDetachAdvSearch(), dt_setting, opt.GetDetachSettings())
			, make_simple_seperator()
			, make_3_simple_switcher(bundle,this, autopaste, opt.GetAutoPaste(), filterpaste, opt.GetFilterPaste(), Toodoo, false)
			, make_simple_seperator()

		);
		ScrollPane sv = new ScrollPane(content);
		sv.setFitToWidth(true);
		Scene Scene = new Scene(sv, 800, 600);
		setScene(Scene);
	}

	static Node make_simple_seperator() {
		Separator sep = new Separator();
		sep.setPadding(padding2);
		return sep;
	}

	static HBox make_simple_switcher(ResourceBundle bundle, ChangeListener<? super Boolean> listener, String message, boolean enabled) {
		ToggleSwitch switcher = new ToggleSwitch();
		switcher.setId(message);
		switcher.selectedProperty().addListener(listener);
		switcher.setSelected(enabled);
		HBox vb1 = new HBox(switcher, new Text(bundle.getString(message)));
		vb1.setPadding(padding);
		return vb1;
	}

	static HBox make_2_simple_switcher(ResourceBundle bundle, ChangeListener<? super Boolean> listener, String message1, boolean enabled1, String message2, boolean enabled2) {
		HBox vb1 = new HBox(
			make_simple_switcher(bundle, listener, message1, enabled1),
			make_simple_switcher(bundle, listener, message2, enabled2)
		);
		DoubleBinding bindTarget = vb1.widthProperty().divide(2);
		for(Node nI:vb1.getChildren()){
			((Region)nI).prefWidthProperty().bind(bindTarget);
		}
		return vb1;
	}

	static HBox make_3_simple_switcher(ResourceBundle bundle, ChangeListener<? super Boolean> listener, String message1, boolean enabled1, String message2, boolean enabled2, String message3, boolean enabled3) {
		HBox vb1 = new HBox(
			make_simple_switcher(bundle, listener, message1, enabled1),
			make_simple_switcher(bundle, listener, message2, enabled2),
			make_simple_switcher(bundle, listener, message3, enabled3)
		);
		DoubleBinding bindTarget = vb1.widthProperty().divide(3);
		for(Node nI:vb1.getChildren()){
			((Region)nI).prefWidthProperty().bind(bindTarget);
		}
		return vb1;
	}

	private HBox make_simple_button(String val) {
		Button btnTmp = new Button();
		btnTmp.setId(val);
		btnTmp.setText(bundle.getString(val));
		btnTmp.setOnAction(this);
		HBox vb1 = new HBox(btnTmp);
		vb1.setPadding(padding2);
		return vb1;
	}
	private HBox make_simple_buttons(String...val) {
		HBox vb1 = new HBox();
		vb1.setPadding(padding2);
		vb1.setSpacing(10);
		for (String vI:val) {
			Button btnTmp = new Button();
			btnTmp.setId(vI);
			btnTmp.setText(bundle.getString(vI));
			vb1.getChildren().add(btnTmp);
			btnTmp.setOnAction(this);
		}
		return vb1;
	}

	//using message as id.
	private HBox make_switchable_path_picker(boolean hasChecker, String message, String val, boolean enabled) {
		ToggleSwitch switcher = null;
		if(hasChecker){
			switcher = new ToggleSwitch();
			switcher.setId(message);
			switcher.selectedProperty().addListener(this);
			switcher.setSelected(enabled);
		}
		TextField pathInput = new TextField(val);
		HBox.setHgrow(pathInput, Priority.ALWAYS);
		pathInput.setId(message);
		pathInput.textProperty().addListener(this);
		HBox vb1 = new HBox(new Text(bundle.getString(message)), pathInput);
		if(switcher!=null){
			vb1.getChildren().add(0, switcher);
			vb1.setPadding(padding);
		}else{
			vb1.setPadding(padding2);
		}
		vb1.setAlignment(Pos.CENTER_LEFT);

		boolean b1,b2;
		if((b1=message.equals(UI.overwrite_browser)) || message.equals(UI.overwrite_pdf_reader)){//add file-picker
			if(b1) PathInput1=pathInput;
			else PathInput2=pathInput;
			Button btnTmp = new Button(bundle.getString(UI.open));
			btnTmp.setId(message);
			btnTmp.setOnAction(this);
			vb1.getChildren().add(btnTmp);
		}
		return vb1;
	}

	@Override
	public void changed(ObservableValue observable, Object oldValue, Object newValue) {
		if(observable instanceof Property){
			Property prop = (Property) observable;
			if(prop.getBean() instanceof Node){
				Node node = (Node) prop.getBean();
				switch (node.getId()){
					case UI.overwrite_browser:
						if(prop instanceof StringProperty)
							opt.setBrowserPathOverwrite((String)newValue);
						else
							opt.SetBrowserPathOverwriteEnabled((boolean)newValue);
					break;
					case UI.ow_bsrarg:
						opt.setBrowserArgs((String)newValue);
					break;
					case UI.overwrite_browser_search:
						if(prop instanceof StringProperty)
							opt.setSearchUrlOverwrite((String)newValue);
						else
							opt.SetSearchUrlOverwritEnabled((boolean)newValue);
					break;
					case UI.overwrite_browser_search1:
						opt.setSearchUrlMiddle((String)newValue);
					break;
					case UI.overwrite_browser_search2:
						opt.setSearchUrlRight((String)newValue);
					break;
					case UI.overwrite_pdf_reader:
						if(prop instanceof StringProperty)
							opt.setPdfOverwrite((String)newValue);
						else
							opt.SetPdfOverwriteEnabled((boolean)newValue);
					break;
					case UI.overwrite_pdf_reader_args:
						if(prop instanceof StringProperty)
							opt.setPdfArgsOverwrite((String)newValue);
						else
							opt.SetPdfArgsOverwriteEnabled((boolean)newValue);
					break;
					case tintwild:
						opt.SetTintWildResult((boolean)newValue);
					break;
					case remwsize:
						opt.SetRemWindowSize((boolean)newValue);
					break;
					case remwpos:
						opt.SetRemWindowPos((boolean)newValue);
					break;
					case tintfull:
						opt.SetTintFullResult((boolean)newValue);
					break;
					case doclsset:
						opt.SetDoubleClickCloseSet((boolean)newValue);
					break;
					case doclsdict:
						opt.SetDoubleClickCloseDict((boolean)newValue);
					break;
					case dt_setting:
						opt.SetDetachSettings((boolean)newValue);
					break;
					case dt_advsrch:
						opt.SetDetachAdvSearch((boolean)newValue);
					break;
					case dt_dictpic:
						opt.SetDetachDictPicker((boolean)newValue);
					break;
					case autopaste:
						opt.SetAutoPaste((boolean)newValue);
					break;
					case filterpaste:
						opt.SetFilterPaste((boolean)newValue);
					break;
					case regex_enable:
						opt.SetRegexSearchEngineEnabled((boolean)newValue);
					break;
					case ps_regex:
						opt.SetPageSearchUseRegex((boolean)newValue);
					break;
					case class_case:
						opt.SetClassicalKeyCaseStrategy((boolean)newValue);
					break;
				}
			}
		}
	}

	@Override
	public void handle(Event event) {
		if(event.getSource() instanceof Node){
			Node start = (Node) event.getSource();
			String val=null;
			TextField pathInput=null;
			switch(start.getId()){
				default:
				return;
				case UI.overwrite_browser:{
					val = opt.getBrowserPathOverwrite();
					pathInput=PathInput1;
				} break;
				case UI.overwrite_pdf_reader:{
					val = opt.GetPdfOverwrite();
					pathInput=PathInput2;
				} break;
				case UI.pdffolders:{
					Stage mDialog;
					if(pdffolderDialog==null || pdffolderDialog.get()==null){
						pdffolderDialog = new WeakReference<>(mDialog=new Stage());
						mDialog.setTitle(bundle.getString(UI.pdffolders));
						mDialog.getIcons().add(new Image(HiddenSplitPaneApp.class.getResourceAsStream("shared-resources/settings.png")));
						mDialog.initModality(Modality.WINDOW_MODAL);
						mDialog.initOwner(start.getScene().getWindow());
						PdfFoldersFragment pdfFragment = new PdfFoldersFragment(opt);
						Scene dialogScene = new Scene(pdfFragment, 800, 600);
						mDialog.setScene(dialogScene);
						mDialog.addEventFilter(KeyEvent.KEY_PRESSED, new EventHandler<KeyEvent>() {
							public void handle(KeyEvent e) {
								if (EscComb.match(e)) {
									mDialog.hide();
									e.consume();
								}
							}
						});
						mDialog.onCloseRequestProperty().set(new EventHandler<WindowEvent>() {
							@Override
							public void handle(WindowEvent event) {
								if(pdfFragment.tableView.isDirty) {
									ObservableList<File> mdModified = pdfFragment.tableView.getItems();
									pdfFragment.try_write_configureLet(new File(PU.getProjectPath(),"CONFIG/PDFolders.lst"));
									PlainDictionaryPcJFX.ThirdFlag|=0x1;
								}
							}
						});
					}else{
						mDialog = pdffolderDialog.get();
					}
					mDialog.show();
				} return;
				case regex_config:
					Stage mDialog;
					if(regConfDialog==null || regConfDialog.get()==null){
						regConfDialog = new WeakReference<>(mDialog=new Stage());
						mDialog.setTitle("正则表达式");
						mDialog.getIcons().add(new Image(HiddenSplitPaneApp.class.getResourceAsStream("shared-resources/settings.png")));
						//mDialog.initModality(Modality.NONE);
						mDialog.initOwner(start.getScene().getWindow());
						RegexConfigFragment mFragment = new RegexConfigFragment(opt);
						Scene dialogScene = new Scene(mFragment, 400, 300);
						mDialog.setScene(dialogScene);
						mDialog.addEventFilter(KeyEvent.KEY_PRESSED, new EventHandler<KeyEvent>() {
							public void handle(KeyEvent e) {
								if (EscComb.match(e)) {
									mDialog.hide();
									e.consume();
								}
							}
						});
						mDialog.onCloseRequestProperty().set(new EventHandler<WindowEvent>() {
							@Override
							public void handle(WindowEvent event) {

							}
						});
					}else{
						mDialog = regConfDialog.get();
					}
					mDialog.show();
				return;
			}
			FileChooser fileChooser = new FileChooser();
			fileChooser.getExtensionFilters().addAll(
				new FileChooser.ExtensionFilter("Windows Excecutable", "*.exe"),
				new FileChooser.ExtensionFilter("Excecutable file", "*")
			);
			File f=null;
			f=val==null?null:(f=new File(val)).isDirectory()?f:f.getParentFile();
			if(f==null || !f.exists()) f=opt.projectPath;
			fileChooser.setInitialDirectory(f);
			File file = fileChooser.showOpenDialog(getScene().getWindow());
			if(file!=null)
				pathInput.setText(file.getAbsolutePath());
		}
	}


	//public static final String wildwutsp = "wildwutsp";
	//public static final String fullwutsp = "fullwutsp";
	public static final String tintwild = "tintwild";
	public static final String remwsize = "remwsize";
	public static final String remwpos = "remwpos";
	public static final String dirload = "dirload";
	public static final String tintfull = "tintfull";
	public static final String doclsset = "doclsset";
	public static final String doclsdict = "doclsdict";
	public static final String dt_setting = "dt_setting";
	public static final String dt_advsrch = "dt_advsrch";
	public static final String dt_dictpic = "dt_dictpic";
	public static final String autopaste = "autopaste";
	public static final String filterpaste = "filterpaste";

	public static final String regex_enable = "regex_enable";
	public static final String ps_regex = "ps_regex";
	public static final String Toodoo = "todo";
	public static final String regex_config = "regex_config";
	public static final String class_case = "class_case";

	public void destroyView(){
		//for(Node nI:content.getChildren()){
		//	if(nI instanceof HBox){
		//		for(Node nII:((HBox)nI).getChildren()){
		//			if(nII instanceof ToggleSwitch){
		//				nII.remove
		//			}
		//		}
		//	}
		//}
	}
}

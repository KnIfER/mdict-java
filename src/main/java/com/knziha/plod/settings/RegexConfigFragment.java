package com.knziha.plod.settings;

import com.knziha.plod.plaindict.PlainDictAppOptions;
import com.knziha.plod.plaindict.javafx.widgets.splitpane.HiddenSplitPaneApp;
import javafx.beans.property.Property;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

import java.util.Locale;
import java.util.ResourceBundle;

import static com.knziha.plod.settings.SettingsDialog.make_simple_seperator;
import static com.knziha.plod.settings.SettingsDialog.make_simple_switcher;

/** some ui*/
public class RegexConfigFragment extends VBox implements ChangeListener {
	PlainDictAppOptions opt;
	ResourceBundle bundle;
	Text statusBar;
	TabPane tabPane;

	@SuppressWarnings("unchecked")
	public RegexConfigFragment(PlainDictAppOptions _opt){
		super();
		tabPane = new TabPane();
		statusBar = new Text();
		tabPane.setPadding(new Insets(4,0,0,0));
		opt=_opt;

		bundle = ResourceBundle.getBundle("UIText", Locale.getDefault());

		Tab tab1 = new Tab();
		tab1.setText(bundle.getString(onegine));
		tab1.setTooltip(new Tooltip("词条、全文检索时，可选用正则引擎，或快速 .* 通配"));
		tab1.setClosable(false);
		Text lable = new Text("");
		lable.setStyle("-fx-fill: #ff0000;");
		tab1.setGraphic(lable);

		Tab tab2 = new Tab();
		tab2.setText(bundle.getString(findpage));
		tab2.setTooltip(new Tooltip("基于 Mark.js"));
		tab2.setClosable(false);
		Text lable1 = new Text("");
		lable1.setStyle("-fx-fill: #ff0000;");
		tab2.setGraphic(lable1);

		tabPane.getSelectionModel().selectedIndexProperty().addListener((observable, oldValue, newValue) -> {
			if(newValue.intValue()==1){
				if(tab2.getContent()==null)
					tab2.setContent(getSubpageContent());
			}
		});

		tabPane.setRotateGraphic(false);
		tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.SELECTED_TAB);
		tabPane.setSide(Side.TOP);
		tabPane.getTabs().addAll(tab1,tab2);
		tabPane.getStyleClass().add(TabPane.STYLE_CLASS_FLOATING);
		final String lvCss = HiddenSplitPaneApp.class.getResource("lvCss.css").toExternalForm();
		tabPane.getStylesheets().add(lvCss);

		tab1.setContent(getMainContent());

		VBox.setVgrow(tabPane, Priority.ALWAYS);

		getChildren().addAll(tabPane, statusBar);
	}

	private Node getSubpageContent() {
		VBox content = new VBox(
				make_simple_switcher(bundle, this, page_regex_case, opt.GetPageSearchCaseSensitive())
				,make_simple_seperator()
				,make_simple_switcher(bundle, this, pagewutsp, opt.GetPageWithoutSpace())
				,make_simple_switcher(bundle, this, ps_separate, opt.GetPageSearchSeparateWord())
		);
		ScrollPane sv = new ScrollPane(content);
		sv.setFitToWidth(true);
		return sv;
	}


	private Node getMainContent() {
		VBox content = new VBox(
			make_simple_switcher(bundle, this, regex_head, opt.GetRegexSearchEngineAutoAddHead())
			,make_simple_switcher(bundle, this, regex_case, opt.GetRegexSearchEngineCaseSensitive())
		);
		ScrollPane sv = new ScrollPane(content);
		sv.setFitToWidth(true);
		return sv;
	}


	@Override
	public void changed(ObservableValue observable, Object oldValue, Object newValue) {
		if(observable instanceof Property) {
			Property prop = (Property) observable;
			if(prop.getBean() instanceof Node) {
				Node node = (Node) prop.getBean();
				switch (node.getId()) {
					case ps_case:
						opt.SetPageSearchCaseSensitive((boolean) newValue);
					break;
					case ps_separate:
						opt.SetPageSearchSeparateWord((boolean) newValue);
					break;
					case regex_head:
						opt.SetRegexSearchEngineAutoAddHead((boolean) newValue);
					break;
					case regex_case:
						opt.SetRegexSearchEngineCaseSensitive((boolean) newValue);
					break;
					case page_regex_case:
						opt.SetPageSearchCaseSensitive((boolean) newValue);
					break;
					case pagewutsp:
						opt.SetPageWithoutSpace((boolean)newValue);
					break;
				}
			}
		}
	}

	public static final String onegine = "onegine";
	public static final String findpage = "findpage";

	public static final String pagewutsp = "pagewutsp";
	public static final String ps_case = "ps_case";
	public static final String ps_separate = "ps_separate";
	public static final String regex_head = "regex_head";
	public static final String regex_case = "regex_case";
	public static final String page_regex_case = "p_regex_case";
}

package com.knziha.plod.plaindict.javafx;

import com.knziha.plod.dictionary.SearchResultBean;
import com.knziha.plod.dictionarymodels.BookPresenter;
import com.knziha.plod.plaindict.PlainDictAppOptions;
import com.knziha.plod.plaindict.javafx.widgets.ObservableListmy;
import javafx.scene.control.ListView;
import javafx.scene.control.Tab;
import javafx.scene.text.Text;

import java.util.ArrayList;
import java.util.Timer;
import java.util.regex.Pattern;

import static com.knziha.plod.dictionary.SearchResultBean.SEARCHTYPE_SEARCHINNAMES;

public class AdvancedSearchLogicLayer extends com.knziha.plod.dictionary.mdict.AbsAdvancedSearchLogicLayer {
		final Tab chiefAmbassador;
		final Text statusBar;
		public final ArrayList<BookPresenter> md;
		final String Tag;
		final PlainDictAppOptions opt;
		Thread workerThread;
		Timer Ticker;
		private String msg;
		Pattern currentPattern;

		AdvancedSearchLogicLayer(PlainDictAppOptions opt, ArrayList<BookPresenter> md, Tab chiefAmbassador, Text statusBar, int type) {
			this.opt = opt;
			this.chiefAmbassador = chiefAmbassador;
			this.statusBar = statusBar;
			this.md = md;
			this.type = type;
			Tag=(type==1||type==-1)?UI.wildmatch:UI.fulltext;
		}

//		public ArrayList<Integer>[] getCombinedTree(int DX) {
//			if(combining_search_tree!=null && DX<combining_search_tree.size())
//				return combining_search_tree.get(DX);
//			return null;
//		}
//		public void setCombinedTree(int DX, ArrayList<Integer>[] _combining_search_tree) {
//			combining_search_tree.set(DX, _combining_search_tree);
//		}
//		public ArrayList<Integer>[] getInternalTree(com.knziha.plod.dictionary.mdict md){
//			return type==-1?md.combining_search_tree2:(type==-2?md.combining_search_tree_4:null);
//		}

		@Override
		public ArrayList<SearchResultBean>[] getTreeBuilding(Object book, int splitNumber) {
			BookPresenter presenter = (BookPresenter) book;
			if (presenter!=null) {
				if (type==SEARCHTYPE_SEARCHINNAMES) {
					if (presenter.combining_search_tree2==null || presenter.combining_search_tree2.length!=splitNumber) {
						presenter.combining_search_tree2=new ArrayList[splitNumber];
					}
					return presenter.combining_search_tree2;
				} else {
					if (presenter.combining_search_tree_4==null || presenter.combining_search_tree_4.length!=splitNumber) {
						presenter.combining_search_tree_4=new ArrayList[splitNumber];
					}
					return presenter.combining_search_tree_4;
				}
			}
			return null;
		}
		@Override
		public ArrayList<SearchResultBean>[] getTreeBuilt(Object book) {
			BookPresenter presenter = (BookPresenter) book;
			if (presenter!=null) {
				return type==SEARCHTYPE_SEARCHINNAMES?presenter.combining_search_tree2:presenter.combining_search_tree_4;
			}
			return null;
		}

		@Override
		public boolean getEnableFanjnConversion() {
			return false;
		}

		@Override
		public Pattern getBakedPattern() {
			return null;
		}

		@Override
		public String getPagePattern() {
			return null;
		}

		@Override
		public void setCurrentPhrase(String currentPhrase) {

		}

		@Override
		public int getSearchEngineType() {
			return 0;
		}

		ObservableListmy adapter;
		ListView<Integer> listView;

		public void Terminate(boolean Join) {
			IsInterrupted=true;
			if(Ticker!=null) {
				Ticker.cancel();
				Ticker=null;
				((Text)chiefAmbassador.getGraphic()).setText("");
			}
			if(Join)
			try {
				workerThread.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			else{
				adapter.rec.invalidate();
				refreshList(false);
			}
			workerThread=null;
		}

		public void bakeMessage() {
			msg = String.format(" %s \"%s\" %ss -> %d项", Tag, key, (System.currentTimeMillis() - st) * 1.f / 1000, adapter.rec.size());
		}

		public void refreshList(boolean bakePattern) {
			((Text)chiefAmbassador.getGraphic()).setText("");
			listView.setItems(null);
			listView.setItems(adapter);
			listView.scrollTo(0);
			statusBar.setText(msg);
			if(bakePattern){
				currentPattern=null;
				if(getTint())
				try {currentPattern=Pattern.compile(opt.GetRegexSearchEngineEnabled()?key:key.replace("*", ".*"), Pattern.CASE_INSENSITIVE);
				} catch (Exception ignored) { }
			}
		}

		boolean getTint() {
			return (type==-1||type==1)?opt.GetTintWildResult():opt.GetTintFullResult();
		}
	}

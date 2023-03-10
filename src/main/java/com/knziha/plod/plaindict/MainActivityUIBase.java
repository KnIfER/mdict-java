package com.knziha.plod.plaindict;

import com.knziha.plod.dictionary.Utils.IU;
import com.knziha.plod.dictionarymodels.BookPresenter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class MainActivityUIBase {

	public String fontFaces;
	public String plainCSS;
	public PDICMainAppOptions opt;

	public boolean getCommonAsset(String s) {
	}

	public static class LoadManager {
		public int md_size;
		public final ArrayList<BookPresenter> md = new ArrayList<>();
		public BookPresenter EmptyBook;
		String moduleName;
		public final PlainDictionary app;
		private HashMap<String, Integer> map = new HashMap<>();
		private int filterCount;

		public LoadManager(PlainDictionary plainDict) {
			this.app = plainDict;
		}

		public void reload(String moduleName) throws Exception {
			File setFile = new File(app.userDir, "CONFIG/" + moduleName);
			BufferedReader in = new BufferedReader(new FileReader(setFile), 4096);
			String line;
			int cc=0;
			filterCount=0;
			map.clear();
			HashSet<String> map = new HashSet<>(); //todo map
			ReadLines:
			while((line = in.readLine())!=null) {
				int flag = 0;
				boolean chair = true;
				if(line.startsWith("[:")){
					int idx = line.indexOf("]",2);
					if(idx>=2){
						String[] arr = line.substring(2, idx).split(":");
						line = line.substring(idx+1);
						for (String pI:arr) {
							switch (pI){
								case "F":
									flag|=0x1;
									filterCount++;
									chair = false;
									break;
								case "C":
									flag|=0x2;
									break;
								case "A":
									flag|=0x4;
									break;
								case "H":
									flag|=0x8;
									//lazyMan.hiddenCount++;
									chair = false;
									break;
								case "Z":
									flag|=0x10;
									break;
								case "S":
									int size = IU.parsint(line);
									if(size>0) md.ensureCapacity(size);
									continue ReadLines;
							}
						}
					}
				}
				if (map.add(line)) { // 避免重复
					BookPresenter bookPresenter = new BookPresenter(new File(line), app, -1);
					md.add(bookPresenter);
				}
			}
			in.close();
			CMN.Log("loaded::", md);
			md_size = md.size();
		}

		public BookPresenter md_get(int idx) {
			return md.get(idx);
		}

		public String md_getName(int idx) {
			return md_getName(idx, -1);
		}
		
		public String md_getName(int idx, int id) {
			return md.get(idx).getDictionaryName();
		}

		public BookPresenter getBookById(long textToNumberSixtwoLe) {
		}

		public BookPresenter md_getByName(String url) {
		}
	}


	public String handleWordMap() {
		return null;
	}
	
	
	
	
	
	
}

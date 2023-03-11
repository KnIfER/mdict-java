package com.knziha.plod.plaindict;

import com.knziha.plod.dictionary.UniversalDictionaryInterface;
import com.knziha.plod.dictionary.Utils.IU;
import com.knziha.plod.dictionary.Utils.SubStringKey;
import com.knziha.plod.dictionarymodels.BookPresenter;
import com.knziha.plod.dictionarymodels.PlainMdict;
import com.knziha.plod.dictionarymodels.PlainWeb;
import com.knziha.plod.ebook.Utils.BU;
import org.nanohttpd.protocols.http.HTTPSession;
import org.nanohttpd.protocols.http.response.Response;
import org.sqlite.SQLiteConfig;

import java.io.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class MainActivityUIBase {
	Connection database;
	private Statement statement;

	public static final String TABLE_BOOK_v2 = "book";
	public static final String FIELD_CREATE_TIME = "creation_time";
	public static final String FIELD_VISIT_TIME = "last_visit_time";
	public static final String FIELD_EDIT_TIME = "last_edit_time";

	public MainActivityUIBase(){
		try {
			userDir = System.getProperty("user.dir") + "\\" + "PlainDict";
			if (System.getProperty("app")!=null)
			{
//				File file = new File("D:\\test.log");
//				PrintStream stream = new PrintStream(new FileOutputStream(file, true));
//				System.setOut(stream);
//				CMN.Log("000", userDir);
			}
			SQLiteConfig config = new SQLiteConfig();
			config.setSharedCache(true);
			database = DriverManager.getConnection("jdbc:sqlite:" + userDir.replace("\\", "/") + "/database.db", config.toProperties());
			statement = database.createStatement();
			database.setAutoCommit(true);
			CMN.Log("123", System.getProperties());

			String sqlBuilder = "CREATE TABLE IF NOT EXISTS " +
					TABLE_BOOK_v2 +
					"(" +
					"id INTEGER PRIMARY KEY AUTOINCREMENT" +
					", name LONGVARCHAR UNIQUE" +
					", path LONGVARCHAR" +
					", options BLOB" +
					", creation_time INTEGER DEFAULT 0 NOT NULL" +
					")";
			statement.execute(sqlBuilder);
			statement.execute("CREATE INDEX if not exists book_name_index ON book (name)");
			CMN.Log("init done.");
//			database.close();

			opt = new PlainDictAppOptions();

			loadManager = new MainActivityUIBase.LoadManager(this);

			loadManager.reload("test.set.txt");
		} catch (Exception e) {
			CMN.Log(e);
		}
	}
	
	public String fontFaces;
	public String plainCSS;
	public PlainDictAppOptions opt;
	public LoadManager loadManager;

	public String userDir;
	BookPresenter currentDictionary;
	
	public Map<SubStringKey, String> serverHosts;
	public ArrayList<PlainWeb>  serverHostsHolder=new ArrayList();
	public HashMap<String, BookPresenter> mdict_cache = new HashMap<>();
	
	public String getCommonAsset(String s) {
		return null;
	}

	public Response decodeExp(HTTPSession session) {
		return null;
	}

	public String fileToString(String path) {
		return BU.fileToString(new File(path));
	}

	public static BookPresenter new_book(String pathFull, MainActivityUIBase THIS) throws IOException {
		File fullPath = pathFull.startsWith("/")?new File(pathFull):new File(THIS.opt.lastMdlibPath, pathFull);
		return new_book(fullPath, THIS);
	}

	public static BookPresenter new_book(File fullPath, MainActivityUIBase THIS) throws IOException {
		BookPresenter ret = THIS.mdict_cache.get(fullPath.getName());
		if (ret!=null) {
			return ret;
		}
		ret = new BookPresenter(fullPath, THIS, 0);
		THIS.mdict_cache.put(fullPath.getName(), ret);
		return ret;
	}
	
	public class LoadManager {
		public int md_size;
		public final ArrayList<BookPresenter> md = new ArrayList<>();
		public BookPresenter EmptyBook;
		String moduleName;
		public final MainActivityUIBase app;
		private HashMap<String, Integer> map = new HashMap<>();
		private int filterCount;

		public LoadManager(MainActivityUIBase plainDict) {
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
					BookPresenter bookPresenter = new BookPresenter(new File(line), app, 0);
//					BookPresenter bookPresenter = new BookPresenter(new PlainMdict(new File(line), app.opt));
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

		public BookPresenter getBookById(long bid) {
			BookPresenter ret = null;
			try {
				String fileName = null;
				UniversalDictionaryInterface impl = BookPresenter.bookImplsMap.get(bid);
				try {
//				CMN.Log("getDictionaryById::", bid, impl, currentDictionary.bookImpl.getDictionaryName(),
//						prepareHistoryCon().getBookID(null, currentDictionary.bookImpl.getDictionaryName())
//						, currentDictionary.bookImpl.getBooKID());
				} catch (Exception e) {
					CMN.debug(e);
				}
				if (impl!=null) {
					fileName = impl.getFile().getPath();
				} else {
					fileName = getBookPath(bid);
				}
				if (fileName!=null) {
					ret = new_book(fileName, MainActivityUIBase.this);
				}
			} catch (Exception e) {
				CMN.debug(e);
			}
			if(ret==null)
				ret=EmptyBook;
			return ret;
		}

		public BookPresenter md_getByName(String url) {
			return null;
		}
	}
	
	public String handleWordMap() {
		return null;
	}




	byte[] Hzh;

	/**
	 * 获取常规汉字的拼音首字母
	 */
	public byte[] getHzh() {
		if (Hzh == null) {
			try {
				File file = new File("D:\\Code\\PlainDictionary\\PLOD\\src\\main\\assets\\Hzh.dat");
				InputStream input = new FileInputStream(file);
				Hzh = new byte[(int) file.length()];
				input.read(Hzh);
			} catch (Exception e) {
				CMN.debug(e);
			}
		}
		return Hzh;
	}

	/**
	 * 生成具有一定确定性的ID
	 */
	public long GenIdStr(String nameKey) {
		long ret = -1;
		try {
			int idx = nameKey.lastIndexOf(".");
			if (idx > 0) nameKey = nameKey.substring(0, idx);
			nameKey = nameKey.replaceAll("\\(.*?\\)", "");
			nameKey = nameKey.replaceAll("\\[.*?\\]", "");
			nameKey = nameKey.replaceAll("\\{.*?\\}", "");
			StringBuilder keyName = new StringBuilder();
			int i = 0, len = nameKey.length();
			for (; i < len; i++) {
				char ch = nameKey.charAt(i);
				if (ch >= 0x4e00 && ch <= 0x9fa5) {
					keyName.append((char) getHzh()[ch - 0x4e00]);
					if (keyName.length() >= 4) {
						i++;
						break;
					}
				} else if (ch >= 'A' && ch <= 'Z') {
					keyName.append(ch);
					if (keyName.length() >= 4) {
						i++;
						break;
					}
				}
			}
			boolean b1 = nameKey.regionMatches(0, keyName.toString(), 0, Math.min(3, keyName.length()))
					|| len < 17;
			if (b1) {
				final int max = 9/*至多九位，争取最大特征*/;
				for (; i < len; i++) {
					char ch = nameKey.charAt(i);
					if (ch >= 0x4e00 && ch <= 0x9fa5) {
						keyName.append((char) getHzh()[ch - 0x4e00]);
						if (keyName.length() >= max) break;
					} else if (ch >= 'A' && ch <= 'Z' || ch >= 'a' && ch <= 'z' || ch >= '0' && ch <= '9') {
						keyName.append(ch);
						if (keyName.length() >= max) break;
					}
				}
			}
			b1 = !b1 || keyName.length() < len / 2/*给你机会你…*/;
			if (b1) { // 至多八位
				keyName.append(len % 10);
				IU.NumberToText_SIXTWO_LE(Math.abs(nameKey.hashCode()) % 0x800, keyName);
				if (keyName.length() > 8) keyName.setLength(8);
			}
			/*防止重复*/
			int cc = 0;
			len = keyName.length();
			int max = (int) (Math.pow(62, 10 - len) - 1);
			while (true) {
				ret = IU.TextToNumber_SIXTWO_LE(keyName);
//				preparedBookIdChecker.bindLong(1, ret);
//				try {
//					preparedBookIdChecker.simpleQueryForLong();
//				} catch (Exception e) {
//					// 不存在，生成的ID可用
//					break;
//				}
				ResultSet res = statement.executeQuery("select id from " + TABLE_BOOK_v2 + " where id=" + ret);
				boolean has = res.next();
				res.close();
				if (!has) {
					break;
				}
				// 计数累加，重新生成新的ID
				if (++cc > max) {
					// 超出最大计数
					if (len == 9) {
						len = 8;
						cc = 0;
					} else {
						ret = -1;
						break;
					}
				}
				keyName.setLength(len);
				IU.NumberToText_SIXTWO_LE(cc, keyName);
			}
			// CMN.debug("keyName::", keyName);
		} catch (Exception e) {
			CMN.debug(e);
		}
		return ret;
	}

	public String getBookPath(long bid) {
		String path = null;
		CMN.rt();
		try {
			ResultSet res = statement.executeQuery("select path from book where id=" + bid);
			if (res.next()) {
				path = res.getString(1);
			}
			res.close();
		} catch (Exception e) {
			CMN.Log(e);
		}
		return path;
	}
	
	public long getBookID(String fullPath, String bookName) {
		CMN.rt();
		long id = -1;
		String path = fullPath;
		try {
			boolean insertNew = true;
			ResultSet res = statement.executeQuery("select id,path from book where name=\"" + bookName+"\"");
			if (res.next()) {
				insertNew = false;
				id = res.getLong(1);
				path = res.getString(2);
			}
			res.close();
			if (insertNew) {
				id = GenIdStr(bookName);
				statement.execute("insert into book(id, name, path, creation_time) VALUES(" + id + ", \"" + bookName + "\", \"" + fullPath + "\", " + CMN.now() + ")");
				res = statement.getResultSet();
			} else if (path == null && fullPath != null) {
//				ContentValues values = new ContentValues();
//				values.put("path", fullPath);
//				database.update(TABLE_BOOK_v2, values, "id=?", new String[]{""+id});
			}
		} catch (Exception e) {
			CMN.Log(e);
		}
		return id;
	}



}

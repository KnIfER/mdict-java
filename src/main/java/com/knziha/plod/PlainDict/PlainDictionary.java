package com.knziha.plod.plaindict;

import com.knziha.plod.dictionary.Utils.IU;
import com.knziha.plod.dictionary.Utils.SU;
import com.knziha.plod.dictionary.mdict;
import org.sqlite.SQLiteConfig;
import test.CMN;

import java.io.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class PlainDictionary {
	Connection database;
	private Statement statement;

	public static final String TABLE_BOOK_v2 = "book";
	public static final String FIELD_CREATE_TIME = "creation_time";
	public static final String FIELD_VISIT_TIME = "last_visit_time";
	public static final String FIELD_EDIT_TIME = "last_edit_time";

	public String userDir;
	LoadManager loadManager;
	mdict currentDictionary;
	PlainDictionary() {
		try {
			currentDictionary = new mdict("D:\\assets\\mdicts\\OED2.mdx");
		} catch (Exception e) {
			SU.Log(e);
		}
		CMN.Log("App");
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

			loadManager = new LoadManager(this);
			
			loadManager.reload("test.set.txt");
		} catch (Exception e) {
			CMN.Log(e);
		}

		CMN.Log("init done !!!");
	}

	public static void main(String[] args) throws Exception {
		List<String> commands = new ArrayList<>();
		commands.add("D:\\Code\\FigureOut\\Textrument\\plugins\\DirectUILib\\bin\\PlainDict.exe");
		Process proc = new ProcessBuilder(commands).start();
		BufferedReader errorResult = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
		BufferedReader errorResult1 = new BufferedReader(new InputStreamReader(proc.getInputStream()));
		new Thread(() -> {
			while (proc.isAlive()) try { CMN.Log(errorResult.readLine());  }  catch (Exception e) { CMN.Log(e); } }).start();
		new Thread(() -> {
			while (proc.isAlive()) try { CMN.Log(errorResult1.readLine());  }  catch (Exception e) { CMN.Log(e); } }).start();
		proc.waitFor();
	}
	
	
	int getCount() {
		return (int) currentDictionary.getNumberEntries();
	}
	
	String getEntryAt(int posisiton) {
		return currentDictionary.getEntryAt(posisiton);
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
	private long GenIdStr(String nameKey) {
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

	public long getBookID(String fullPath, String bookName) {
		CMN.rt();
		long id = -1;
		String path = fullPath;
		try {
			boolean insertNew = true;
			ResultSet res = statement.executeQuery("select id,path from book where name=" + bookName);
			if (res.next()) {
				insertNew = false;
				id = res.getLong(0);
				path = res.getString(1);
			}
			res.close();
			if (insertNew) {
				id = GenIdStr(bookName);
				statement.execute("insert into book(id, name, path, creation_time) VALUES(" + id + ", " + bookName + ", " + fullPath + ", " + CMN.now() + ")");
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

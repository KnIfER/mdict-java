package com.knziha.plod.plaindict.db;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.Feature;
import com.knziha.plod.dictionary.Utils.IU;
import com.knziha.plod.dictionary.Utils.SU;
import org.nanohttpd.protocols.http.IHTTPSession;
import org.nanohttpd.protocols.http.request.Method;
import org.nanohttpd.protocols.http.response.Response;
import org.sqlite.SQLiteConfig;
import test.CMN;
import test.privateTest.wget.WGet;
import test.privateTest.wget.info.DownloadInfo;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.knziha.plod.plaindict.MdictServer.emptyResponse;
import static org.nanohttpd.protocols.http.response.Response.newFixedLengthResponse;

/** Sqlite Database helper */
public class FFDB {
	static FFDB instance;
	final Connection database;
	Statement statement;
	HashMap<String, PreparedStatement> preparedStatements = new HashMap<>();
	static final HashMap<String, FHDB> FHDBs = new HashMap<>();

	ExecutorService threadPool = Executors.newFixedThreadPool(5);
	boolean bUniqueTable;
	
	public static FFDB getInstance() {
		if (instance==null) {
			synchronized (FFDB.class) {
				try {
					if (instance==null)
						instance = new FFDB();
				} catch (SQLException e) {
					CMN.Log(e);
				}
			}
		}
		return instance;
	}

	FFDB() throws SQLException {
		SQLiteConfig config = new SQLiteConfig();
		config.setSharedCache(true);
		database = DriverManager.getConnection("jdbc:sqlite:D:/sample.db", config.toProperties());
		statement=database.createStatement();
		database.setAutoCommit(false);
	}

	void getInitMap(JSONObject json, StringBuilder sb) {
		Object obj;
		int idx=0;
		Set<String> keys = json.keySet();
		Iterator<String> iter = keys.iterator();
		String key;
		boolean ftd = false;
		while (iter.hasNext()) {
			key=iter.next();
			obj = json.get(key);
			if (ftd) {
				sb.append(" , ");
			} else {
				ftd = true;
			}
			idx++;
			if (key.equals("rowId")) {
				sb.append("id INTEGER PRIMARY KEY AUTOINCREMENT");
			}
			else if (obj instanceof Integer
					|| obj instanceof Long
					|| obj instanceof Short
			) {
				sb.append(key).append(" INTEGER");
				if (IU.parsint(obj+"", -1)!=-1) {
					sb.append(" DEFAULT ").append(obj);
				}
			}
			else if (obj instanceof String) {
				sb.append(key).append(" TEXT");
				if (!obj.toString().equals("-1")) {
					sb.append(" DEFAULT ").append("'").append(obj).append("'");
				}
			}
			else {
				CMN.Log("getInitMap::不支持::", idx, obj);
			}
		}
	}
	
	public void openTable(String table, JSONObject map, JSONObject indexed) throws SQLException {
		StringBuilder sb = new StringBuilder();
		sb.append("CREATE TABLE IF NOT EXISTS ")
			.append(table)
			.append("(");
		getInitMap(map, sb);
		String sql = sb.append(")").toString();
		CMN.Log("openTable::sql::", sql);
		statement.execute(sql);
		if (indexed!=null) {
			Iterator<String> iter = indexed.keySet().iterator();
			String key;
			boolean ftd = false;
			while (iter.hasNext()) {
				key=iter.next();
				sql="CREATE "+(IU.parsint(indexed.getInteger(key), 0)==1?" UNIQUE ":"")+"INDEX if not exists "+table+"_"+key+"_index ON "+table+"("+key+")";
				statement.execute(sql);
			}
		}
	}

	void getValNames(JSONObject json, StringBuffer sb, boolean kuohao) {
		Set<String> keys = json.keySet();
		Iterator<String> iter = keys.iterator();
		String key;
		if(kuohao) sb.append("(");
		boolean ftd = false;
		while (iter.hasNext()) {
			key=iter.next();
			if (ftd) {
				sb.append(",");
			} else {
				ftd = true;
			}
			sb.append(key);
		}
		if(kuohao) sb.append(")");
	}

	void getValMaps(JSONObject json, StringBuffer sb) {
		Set<String> keys = json.keySet();
		Iterator<String> iter = keys.iterator();
		String key;
		boolean ftd = false;
		while (iter.hasNext()) {
			key=iter.next();
			if (ftd) {
				sb.append(" & ");
			} else {
				ftd = true;
			}
			sb.append(key)
				.append("=");
			Object obj = json.get(key);
			if (true) {
				sb.append("?");
			} else {
				if (obj instanceof Integer
						|| obj instanceof Long
						|| obj instanceof Short
				) {
					sb.append(obj);
				}
				else if (obj instanceof String) {
					sb.append("'").append(obj).append("'");
				}
				else {
					CMN.Log("setValues::不支持::", key, obj);
				}
			} 
		}
	}
	
	void getValues(ResultSet set, JSONObject json) throws SQLException {
		Iterator<String> iter = json.keySet().iterator();
		Object obj;
		int idx=0;
		String key;
		while (iter.hasNext()) {
			key=iter.next();
			idx++;
			json.put(key, set.getObject(idx));
		}
	}
	
	void setValues(PreparedStatement prepared, JSONObject json) throws SQLException {
		prepared.clearParameters();
		Collection<Object> vals = json.values();
		Iterator<Object> iter = vals.iterator();
		Object obj;
		int idx=0;
		while (iter.hasNext()) {
			obj=iter.next();
			idx++;
			if (obj instanceof Integer) {
				prepared.setInt(idx, (Integer) obj);
			}
			else if (obj instanceof Long) {
				prepared.setLong(idx, (Long) obj);
			}
			else if (obj instanceof String) {
				prepared.setString(idx, (String) obj);
			}
			else if (obj instanceof Blob) {
				prepared.setBlob(idx, (Blob) obj);
			} else {
				CMN.Log("setValues::不支持::", idx, obj);
			} 
		}
	}
	
	public void putBatch(String table, JSONArray array) {
		try {
			//statement.execute("insert into TEST(rid, fav) VALUES("+rid+", "+fav+")");
			JSONObject json = array.getJSONObject(0);
			StringBuffer sb = new StringBuffer();
			sb.append("REPLACE INTO ").append(table);
			getValNames(json, sb, true);
			sb.append(" VALUES(");
			boolean ftd = false;
			for (int i = 0; i < json.size(); i++) {
				if (ftd) {
					sb.append(",");
				} else {
					ftd = true;
				}
				sb.append("?");
			}
			sb.append(")");
			String sql = sb.toString();
			synchronized (database) {
				PreparedStatement prepared = preparedStatements.get(sql);
				if (prepared==null || prepared.isClosed()) {
					preparedStatements.put(sql, prepared=database.prepareStatement(sql));
				}
				int cc= 0;
				try {
					prepared.clearBatch();
					cc = 0;
					for (int i = 0; i < array.size(); i++) {
						json = array.getJSONObject(i);
						prepared.clearParameters();
						setValues(prepared, json);
						prepared.addBatch();
						if (i%1024==0) {
							database.commit();
							cc += prepared.executeBatch().length;
							prepared.clearBatch();
						}
					}
					cc += prepared.executeBatch().length;
				} catch (SQLException e) {
					CMN.Log(e);
					database.rollback();
				}
				CMN.Log("executeBatch::", cc, array.size());
			}
		} catch (Exception e) {
			CMN.Log(e);
		}
	}

	public void put(String table, JSONObject json) {
		try {
			//statement.execute("insert into TEST(rid, fav) VALUES("+rid+", "+fav+")");
			StringBuffer sb = new StringBuffer();
			sb.append("REPLACE INTO ").append(table);
			getValNames(json, sb, true);
			sb.append(" VALUES(");
			boolean ftd = false;
			for (int i = 0; i < json.size(); i++) {
				if (ftd) {
					sb.append(",");
				} else {
					ftd = true;
				}
				sb.append("?");
			}
			sb.append(")");
			String sql = sb.toString();
			synchronized (database) {
				PreparedStatement prepared = preparedStatements.get(sql);
				if (prepared==null || prepared.isClosed()) {
					preparedStatements.put(sql, prepared=database.prepareStatement(sql));
				}
				try {
					setValues(prepared, json);
					prepared.execute();
					database.commit();
				} catch (SQLException e) {
					CMN.Log(e);
					database.rollback();
				}
			}
		} catch (Exception e) {
			CMN.Log(e);
		}
	}
	
	public JSONObject get(String table, JSONObject ret, JSONObject where) {
		//try {
		//	//ResultSet set = statement.executeQuery("select fav from TEST where rid="+rid+" limit 1");
		//	synchronized (database) {
		//		PreparedStatement prepared = database.prepareStatement("select fav from TEST where rid=? limit 1");
		//		prepared.setInt(1, rid);
		//		ResultSet set =  prepared.executeQuery();
		//		if (set.next()) {
		//			defVal = set.getInt(1);
		//		}
		//		set.close();
		//	}
		//} catch (Exception e) {
		//	CMN.Log(e);
		//}
		//return defVal;
		try {
			//ResultSet set = statement.executeQuery("select fav from TEST where rid="+rid+" limit 1");
			StringBuffer sb = new StringBuffer();
			sb.append("select ");
			getValNames(ret, sb, false); // 模板
			sb.append(" FROM ").append(table).append(" WHERE ");
			getValMaps(where, sb);
			sb.append(" limit 1");
			String sql = sb.toString();
			CMN.Log("get::sql::", sql);
			synchronized (database) {
				PreparedStatement prepared = preparedStatements.get(sql);
				if (prepared==null || prepared.isClosed()) {
					prepared=database.prepareStatement(sql);
				}
				setValues(prepared, where);
				ResultSet set = prepared.executeQuery();
				if (set.next()) {
					getValues(set, ret);
				}
				set.close();
			}
		} catch (Exception e) {
			CMN.Log(e);
		}
		return ret;
	}

	public JSONArray getBatch(String table, JSONObject temp, JSONArray array) {
		JSONObject where = array.getJSONObject(0);
		JSONArray ret = new JSONArray();
		try {
			//ResultSet set = statement.executeQuery("select fav from TEST where rid="+rid+" limit 1");
			StringBuffer sb = new StringBuffer();
			sb.append("select ");
			getValNames(temp, sb, false); // 模板
			sb.append(" FROM ").append(table).append(" WHERE ");
			getValMaps(where, sb);
			sb.append(" limit 1");
			String sql = sb.toString();
			CMN.Log("getBatch::sql::", sql);
			synchronized (database) {
				PreparedStatement prepared = preparedStatements.get(sql);
				if (prepared==null || prepared.isClosed()) {
					prepared=database.prepareStatement(sql);
				}
				for (int i = 0; i < array.size(); i++) {
					where = array.getJSONObject(i);
					setValues(prepared, where);
					ResultSet set = prepared.executeQuery();
					if (set.next()) {
						JSONObject templet = (JSONObject) temp.clone();
						getValues(set, templet);
						ret.add(templet);
					}
					set.close();
				}
			}
		} catch (Exception e) {
			CMN.Log(e);
		}
		CMN.Log("getBatch::", ret.size(), array.size());
		return ret;
	}

	public void downloadUnique(JSONObject temp) {
		CMN.Log("downloadUnique::", temp);
//		filename: ""
//		finalUrl: "https://"
//		referrer: "https://"
		String filename = temp.getString("filename");
		String finalUrl = temp.getString("finalUrl");
		int idx = finalUrl.indexOf("?");
		if (idx>0) finalUrl = finalUrl.substring(0, idx);
		String referrer = temp.getString("referrer");
		if (!bUniqueTable) {
			try {
				openTable("files", FFDB.parseObject("{url:'', name:'', fav:0}"), FFDB.parseObject("{url:1}"));
				bUniqueTable = true;
			} catch (SQLException ignored) { }
		}
		JSONObject ret = new JSONObject();
		ret.put("name", null);
		get("files", ret, FFDB.parseObject("{url:'"+finalUrl+"'}"));
		if (ret.get("name") != null) {
			CMN.Log("已经下载过了！");
		} else {
			CMN.Log("下载…");
			ret.put("name", filename);
			ret.put("url", finalUrl);
			ret.put("fav", 0);
			threadPool.execute(new Runnable() {
				@Override
				public void run() {
					try {
						DownloadInfo info = new DownloadInfo(new URL(temp.getString("finalUrl")));
						info.setReferer(new URL(temp.getString("referrer")));
						//String path = "C:\\Users\\TEST\\Downloads\\Video\\";
						String path = "F:\\life\\";
						if (!new File(path).exists()) {
							path = "G:\\life\\";
						}
						if (!new File(path).exists()) {
							path = "H:\\life\\";
						}
						String fn = filename;
						String fix = ".mp4";
						int idx = fn.lastIndexOf(".");
						if (idx>0) {
							fix = fn.substring(idx);
							fn = fn.substring(0, idx);
						}
						File file = new File(path+fn+fix);
						int cc=0;
						while (file.exists()) {
							file = new File(path+fn+"."+(++cc)+fix);
						}
						WGet wGet = new WGet(info, file);
						wGet.download();
						put("files", ret);
					} catch (Exception e) {
						CMN.Log(e);
					}
				}
			});
		} 
	}



	public static JSONObject parseObject(String text) {
		if (text==null) {
			return null;
		}
		boolean nord = (JSON.DEFAULT_PARSER_FEATURE & Feature.OrderedField.getMask())==0;
		JSON.DEFAULT_PARSER_FEATURE |= Feature.OrderedField.getMask();
		JSONObject ret = JSONObject.parseObject(text);
		if (nord) {
			JSON.DEFAULT_PARSER_FEATURE &= ~Feature.OrderedField.getMask();
		}
		return ret;
	}

	static class FHDB{
		final String name;
		final HashSet<String> FHash = new HashSet<>();
		final StringBuilder FStr = new StringBuilder();
		boolean FHashPrepared;
		FHDB(String name) {
			this.name = name;
		}
		public void prepareHashset() {
			if (!FHashPrepared) {
				synchronized (FHash) {
					if (!FHashPrepared) {
						FHashPrepared = true;
						try {
							String key;
							BufferedReader reader = new BufferedReader(new FileReader("D:\\"+name+".txt"));
							while ((key=reader.readLine())!=null) {
								FStr.append(key).append("\n");
								FHash.add(key);
							}
							reader.close();
						} catch (Exception e) {
							CMN.Log(e);
						}
					}
				}
			}
		}
		public void putHashSet(JSONArray array) {
			prepareHashset();
			ArrayList<String> toSave = new ArrayList<>(array.size());
			for (int i = 0; i < array.size(); i++) {
				String key = array.getString(i);
				if (!FHash.contains(key)) {
					toSave.add(key);
				}
			}
			if (toSave.size()>0) {
				synchronized (FHash) {
					try {
						BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream("D:\\"+name+".txt", true));
						for (String key:toSave) {
							FHash.add(key);
							FStr.append(key).append("\n");
							output.write(key.getBytes(StandardCharsets.UTF_8));
							output.write("\r\n".getBytes(StandardCharsets.UTF_8));
						}
						output.close();
					} catch (Exception e) {
						CMN.Log(e);
					}
				}
			}
		}
		public String getHashSet() {
			prepareHashset();
			return FStr.toString();
		}
	}


	public static Response handleRequest(IHTTPSession session) {
		if (Method.POST.equals(session.getMethod())) {
			try {
				session.parseBody(null);
				//SU.Log("DB.jsp::", session.getHeaders());
				SU.Log("DB.jsp::", session.getParameters(), session.getMethod());
				FFDB db = FFDB.getInstance();
				String text = session.getParameter("data");
				//SU.Log("text::", text, text.length());
				//text = URLDecoder.decode(text);
				Objects.requireNonNull(text);
				JSONObject data;
				if (text.startsWith("[")) {
					data = JSONArray.parseArray(text).getJSONObject(0);
				} else {
					data = FFDB.parseObject(text);
				}
				if (data.containsKey("dwnld")) {
					getInstance().downloadUnique(data);
				} else {
					JSONArray fSet = data.getJSONArray("fSet");
					String tableName = data.getString("table");
					if (fSet != null) {
						if (tableName==null) {
							tableName = "sample";
						}
						if (tableName.startsWith("sample") && !tableName.contains("\\") && !tableName.contains("/")) {
							FHDB fSetter;
							synchronized (FHDBs) {
								fSetter = FHDBs.get(tableName);
								if (fSetter==null) {
									fSetter = new FHDB(tableName);
								}
							}
							if (fSet.size() == 0) {
								return newFixedLengthResponse(fSetter.getHashSet()) ;
							} else {
								fSetter.putHashSet(fSet);
							}
						}
					}
					else {
						//CMN.Log(data);
						JSONObject json = data.getJSONObject("json");
						JSONArray batch = data.getJSONArray("batch");
						JSONObject where = data.getJSONObject("where");
						JSONObject indexed = data.getJSONObject("indexed");
						if (json==null && batch!=null) {
							json = batch.getJSONObject(0);
						}
						CMN.Log(tableName, json, where, indexed);
						Objects.requireNonNull(tableName);
						Objects.requireNonNull(json);
						if (indexed != null) { // open
							db.openTable(tableName, json, indexed);
						}
						else if (where != null) { // get
							String ret;
							if (batch != null) {
								JSONArray result = db.getBatch(tableName, json, batch);
								ret = result.toString();
								CMN.Log("getBatch::", result.size(), json.size());
							} else {
								ret = db.get(tableName, json, where).toString();
							}
							return newFixedLengthResponse(ret);
						}
						else { // set
							if (batch != null) {
								db.putBatch(tableName, batch);
							} else {
								db.put(tableName, json);
							}
						}
					}
				} 
			} catch (Exception e) {
				CMN.Log(e);
			}
		}
		return emptyResponse;
	}

	
}

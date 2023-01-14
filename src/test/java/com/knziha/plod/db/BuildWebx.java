package com.knziha.plod.db;

import com.alibaba.fastjson.JSONObject;
import com.google.javascript.jscomp.Compiler;
import com.google.javascript.jscomp.*;
import com.knziha.plod.dictionary.mdict;
import com.knziha.plod.dictionaryBuilder.mdictBuilder;
import com.knziha.plod.dictionaryBuilder.mdictResBuilder;
import com.knziha.plod.ebook.Utils.BU;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import test.CMN;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BuildWebx {

	public static int hashCode(String toHash) {
		return hashCode(toHash, 0);
	}

	public static int hashCode(String toHash, int start) {
		int h=0;
		int len = toHash.length();
		for (int i = start; i < len; i++) {
			h = 31 * h + Character.toLowerCase(toHash.charAt(i));
		}
		return h;
	}
	
	@Test
	public void test() throws IOException {
		ByteArrayOutputStream bin = new ByteArrayOutputStream();
		//.bin.write(98);
		bin.write("Dictionary".getBytes(StandardCharsets.UTF_8));

		byte[] data = bin.toByteArray();
		for (int i = 0; i < data.length; i++) {
			data[i] += i*31;
		}

		File f = new File("D:\\test.tmp");
		SaveToFile(new ByteArrayInputStream(data), f);
	}

	
	@Test
	public void compileSortJs() throws IOException {
		File f = new File("D:\\Code\\FigureOut\\chrome\\extesions\\chrome_send_to_plaindict\\js\\popup\\Sortable.js");
		String js = BU.fileToString(f);
		CMN.Log(compileJs(js, "sort.js", "sort.js"));
	}


//	{url:"https://translate-pa.googleapis.com/v1/supportedLanguages"}
//		,{url:"https://www.google.com/images/cleardot.gif"}
//		,{url:"http://translate.google.com/gen204x"}
//		,{url:"https://translate.google.cn/translate_a/element.js"}
//		,{urlv4:"https://translate.googleapis.com/_/translate_http/_/js/k=translate_http.tr.zh_CN.tbokbaq6GcI.O/am=AQ/d=1/exm=el_conf/ed=1/rs=AN8SPfpDwbSVWpQ6RLn8qBOWrZ8KB0pUCg/m=el_main"}


	static HashSet<String> addedFiles = new HashSet<>();
	static HashSet<String> exlcudeFiles = new HashSet<>();
	
	public static void main(String[] args) throws IOException {
		if(false) {
			String path = "G:\\New folder\\archive";
			File[] arr = new File(path).listFiles();
			for (File f : arr) {
				String name = f.getName();
				String newName = name.replaceAll("_ConvertNo.[0-9].mp4", ".mp4").replaceAll("_ConvertNo.[0-9].mp4", ".mp4").replaceAll("_ConvertNo.[0-9].mp4", ".mp4");
				CMN.Log("renameTo::", name, "==>", newName);
				f.renameTo(new File(f.getParent(), newName));
			}
			return;
		}
		if(false) {
			String path = "F:\\Video";
			File[] arr = new File(path).listFiles();
			for (File f : arr) {
				String name = f.getName();
				String newName = name.replaceAll("\"", "").replaceAll("“", "").replaceAll("”", "");
				CMN.Log("renameTo::", name, "==>", newName);
				f.renameTo(new File(f.getParent(), newName));
			}
			return;
		}
		// removeRemovedInDumped
		if(false) {
			File removed = new File("D:\\sample_remove.txt");
			File dumped = new File("D:\\sample_dump.txt"); // to modify this file
			HashSet<String> removeMap = new HashSet<>();
			try {
				String key;
				BufferedReader reader = new BufferedReader(new FileReader(removed));
				while ((key=reader.readLine())!=null) {
					removeMap.add(key);
				}
				reader.close();
			} catch (Exception e) {
				CMN.Log(e);
			}
			try {
				HashSet<String> dumpMap = new HashSet<>();
				String key;
				BufferedReader reader = new BufferedReader(new FileReader(dumped));
				StringBuilder newDumped = new StringBuilder();
				while ((key=reader.readLine())!=null) {
					if (!dumpMap.contains(key) && !removeMap.contains(key)) {
						newDumped.append(key).append("\n");
						dumpMap.add(key);
					}
				}
				reader.close();
				CMN.Log(newDumped.toString());
				BufferedWriter writer = new BufferedWriter(new FileWriter("D:\\sample_dump1.txt"));
				writer.write(newDumped.toString());
				writer.close();
			} catch (Exception e) {
				CMN.Log(e);
			}
			return;
		}
		packMdbR();
		
		mdictBuilder mdxDB = new mdictBuilder("","","UTF8"); //todo UTF-16LE 存在bug
//		mdxDB.setKeycaseSensitive(true); // 块分界单词处理方式不一致！
		exlcudeFiles.add("mdict_browser.html");
		exlcudeFiles.add(".favorites.json");
		exlcudeFiles.add("李白全集.0.txt");
		exlcudeFiles.add("webx.mdx");
		exlcudeFiles.add("merged_browser.html");
		exlcudeFiles.add("pdfjs");
		exlcudeFiles.add("Search");
		
		HashSet<String> 检查 = new HashSet<>();
		
		HashSet<String> files = new HashSet<>();
		ArrayList<File> filesArr = new ArrayList<File>() {};
		files.add("supportedLanguages");
		files.add("cleardot.gif");
		files.add("gen204x");
		files.add("element.js");
		files.add("m=el_main");
		//files.add("tapSch.js");

		File folder = new File("D:\\Code\\PlainDictionary\\PLOD\\src\\main\\assets\\");

		String[] arr = folder.list();

		for (String n : arr) {
			File file = new File(folder, n);
			if (n.endsWith(".web")) { // "谷歌翻译.web"
				String hash = n.substring(0, 3);
				if (!检查.add(hash)) throw new RuntimeException(n+"名字太简单了！");

				String rawJsonData = BU.fileToString(file);

				JSONObject rawJson = JSONObject.parseObject(rawJsonData);

				for(String key:rawJson.keySet()) {
					String keyText = key.toLowerCase();
					if (keyText.contains("js")
							|| keyText.equals("synthesis") 
							|| keyText.contains("pagetranslator")
							|| keyText.equals("randx")
					) {
						Object val = rawJson.get(key);
						if (val instanceof String) {
							String string = (String) val;
							string = string.replaceAll("debug\\(.*\\);", "");
							try {
								Pattern p = Pattern.compile("<script>(.*?)</script>");
								if (string.contains("<script>")) {
									Matcher m = p.matcher(string);
									StringBuffer sb = new StringBuffer(string.length());
									while (m.find()) {
										String js = m.group(1);
										js = compileJs(js, n, key);
										m.appendReplacement(sb, "");
										sb.append("<script>").append(js).append("</script>");
									}
									m.appendTail(sb);
									string = sb.toString();
								} else {
									string = compileJs(string, n, key);
								}
							} catch (Exception e) {
								CMN.Log("编译JS-->失败", e);
							}
							rawJson.put(key, string);
						}
					}
				}

				BU.printFile(rawJson.toString(
//				SerializerFeature.PrettyFormat
//				,SerializerFeature.WriteSlashAsSpecial
				).getBytes(StandardCharsets.UTF_8), new File("D:\\谷歌翻译BY.web"));

				mdxDB.insert(hash, rawJson.toString());
				//mdxDB.insert(hash, rawJsonData);
				addedFiles.add(file.getPath());
			} 
			else if (files.contains(n)) {
				String hash = n.substring(0, 2);
				if (!检查.add(hash)) throw new RuntimeException();
				mdxDB.insert(hash, file);
				filesArr.add(file);
				addedFiles.add(file.getPath());
			}
		}
		
//		mdxDB.insert("ta", new File(folder, ""));


		String path = "D:\\Code\\PlainDictionary\\PLOD\\src\\main\\assets_release\\webx";
//		path = "D:\\Code\\PlainDictionary5\\PLOD\\release\\webx";
		mdxDB.write(path);

		mdict md = new mdict(path);

		//FileUtils.copyFile(new File(path), new File("D:\\Code\\PlainDictionary\\PLOD\\src\\main\\assets\\webx"));

		//CMN.Log("test::webx::entry::"+md.getRecordAt(0));

		for (int i = 0; i < md.getNumberEntries(); i++) {
			String entry = md.getEntryAt(i);
			CMN.Log("test::webx::", entry, md.lookUp(entry, true), md.getKeyCaseSensitive());
		}

//		for (File f:filesArr) {
//			String n = f.getName();
//			//byte[] data = md.getRecordData(md.lookUp("" + hashCode(n)));
//			byte[] data = md.getRecordData(md.lookUp(n.substring(0, 2)));
//			byte[] data1 = BU.fileToByteArr(f);
//			if (data1.length!=data.length || !mdict.compareByteArrayIsPara(data1, 0, data)) {
//				CMN.Log(f, data1.length, data.length, f.length());
//				throw new RuntimeException();
//			}
//			
////			String data = md.getRecordData(md.lookUp(n.substring(0,2)));
////			String data1 = BU.fileToString(f);
////			if (!data.equals(data1)) {
////				CMN.Log(f, data1.length(), data.length(), f.length());
////				throw new RuntimeException();
////			}
//		}


		File from = new File("D:\\Code\\PlainDictionary\\PLOD\\src\\main\\assets\\");
		File to = new File("D:\\Code\\PlainDictionary\\PLOD\\src\\main\\assets_release\\");
		
		transferDir(from, to);
		
		CMN.Log("done.");
	}


	public static void packMdbR() throws IOException {
		mdictResBuilder builder = new mdictResBuilder("", "");
		File folder = new File("D:\\Code\\PlainDictionary\\PLOD\\src\\main\\assets\\");
		builder.insert("\\merged_browser.html", new File(folder, "merged_browser.html"));
		builder.insert("\\mdict_browser.html", new File(folder, "mdict_browser.html"));
		
		File from = new File(folder, "MdbR");
		int baseLen = folder.getPath().length();
		ArrayList<File> files = new ArrayList<>(Arrays.asList(from.listFiles()));
		String fromPath = from.getPath();
		for (int i = 0; i < files.size(); i++) {
			File f = files.get(i);
			if (f.isDirectory()) {
				files.addAll(Arrays.asList(f.listFiles()));
			} else if(!f.getName().contains("SocialIntro.html") && !f.getName().contains("spring_landscape.jpg")){
				builder.insert(f.getPath().substring(baseLen), f);
			}
		}

		builder.write("D:\\Code\\PlainDictionary\\PLOD\\src\\main\\assets_release\\MdbR.mdd");


		mdict md = new mdict("D:\\Code\\PlainDictionary\\PLOD\\src\\main\\assets_release\\MdbR.mdd");
		
		for (int i = 0; i < md.getNumberEntries(); i++) {
			String entry = md.getEntryAt(i);
			CMN.Log(entry, md.lookUp(entry, true), md.getKeyCaseSensitive());
		}

		deleteFolder(new File("D:\\Code\\PlainDictionary\\PLOD\\build\\intermediates\\merged_assets\\release\\out"));
	}

	static void deleteFolder(File file){
		File[] arr = file.listFiles();
		if(arr!=null)
		for (File subFile : arr) {
			if(subFile.isDirectory()) {
				deleteFolder(subFile);
			} else {
				subFile.delete();
			}
		}
		file.delete();
	}

	private static String compileJs(String string, String n, String key) {
		Compiler compiler = new Compiler();
		CompilerOptions opt = new CompilerOptions();
		CompilationLevel.SIMPLE_OPTIMIZATIONS.setOptionsForCompilationLevel(opt);
		opt.setRenamingPolicy(VariableRenamingPolicy.ALL, PropertyRenamingPolicy.OFF);
		opt.setOutputCharset(StandardCharsets.UTF_8);
		SourceFile source = SourceFile.fromCode("input.js", string);
		SourceFile extern = SourceFile.fromCode("extern.js","");

		Result res = compiler.compile(extern, source, opt);
		if(res.success) {
			string = compiler.toSource();
			CMN.Log("-->成功", n, key, string);
		} else {
			CMN.Log("-->失败", n, key, res.errors.toString());
		}
		return string;
	}

	static void transferDir(File from, File to) {
		ArrayList<File> files = new ArrayList<>(Arrays.asList(from.listFiles()));
		String fromPath = from.getPath();
		for (int i = 0; i < files.size(); i++) {
			File f = files.get(i);
			if (f.isDirectory()) {
				if(!f.getName().contains("Markjs")
						&& !f.getName().contains(".vscode")
						&& !exlcudeFiles.contains(f.getName())
				)
					files.addAll(Arrays.asList(f.listFiles()));
			} else if (!addedFiles.contains(f.getPath())
				&& !exlcudeFiles.contains(f.getName())
				&& !f.getName().toLowerCase().contains("test")
				&& (!f.getPath().contains("MdbR") || f.getName().contains("SocialIntro.html") || f.getName().contains("spring_landscape.jpg"))
			){
				File f2 = new File(to, f.getPath().replace(fromPath, ""));
				if (!f2.exists() || f2.lastModified()!=f.lastModified()) {
//					try(FileInputStream fin = new FileInputStream(f)) {
//						SaveToFile(fin, f2);
//					} catch (Exception e) {}
					try {
						FileUtils.copyFile(f, f2);
					} catch (IOException e) {
						CMN.Log(e);
					}
				}
			}
		}
	}

	public static void SaveToFile(InputStream inputStream, File f) throws IOException {
		byte[] buffer = new byte[4096];
		int len;
		FileOutputStream fout = new FileOutputStream(f);
		while ((len=inputStream.read(buffer))!=-1) {
			fout.write(buffer, 0, len);
		}
		fout.close();
	}
	
	@Test
	public void SkipHtmlTest() throws IOException {
		mdictBuilder mdxDB = new mdictBuilder("","","utf8");
		
		mdxDB.insert("HAPPY", "Vigne<b aria-label='happier'>TT</b>e <em1>P</em1><em2>HILIP</em2>");
		
		mdxDB.insert("Lab1", "La La Lab <b aria-label='foobar'> La Label </b> ");
		
		mdxDB.insert("Lab2", "La La Lab<b aria-label='foobar'></b>el ");
		
		
		mdxDB.insert("Lab3", "La La Labb aria-label='foobar'> e</b> l ");


		String path = "D:\\Code\\PlainDictionary\\PLOD\\src\\main\\assets_release\\SkipHtmlTest.mdx";
		mdxDB.write(path);
		
		mdict md = new mdict(path);

		CMN.Log("entry::"+md.getEntryAt(0));
	}


	@Test
	public void SkipHtmlTest_OED() throws IOException {
		CMN.rt();
		mdict md = new mdict("D:\\assets\\mdicts\\OED2.mdx");
		mdict.AbsAdvancedSearchLogicLayer layer = new mdict.AbsAdvancedSearchLogicLayer(){
			@Override
			public ArrayList<Integer>[] getCombinedTree(int DX) {
				return null;
			}
			@Override
			public void setCombinedTree(int DX, ArrayList<Integer>[] val) {

			}
			@Override
			public ArrayList<Integer>[] getInternalTree(mdict mdtmp) {
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
			public int getSearchType() {
				return 0;
			}
		};
		
		md.flowerFindAllContents("PHILIP", 0, layer);

		
		CMN.pt();
	}











}

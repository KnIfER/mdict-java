package com.knziha.plod.plaindict;

import com.knziha.plod.dictionary.Utils.SU;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class PlainDictionary extends MainActivityUIBase{
	public PlainDictionary() {
		super();
		CMN.Log("App");
		try {
			if (System.getProperty("app")!=null)
			{
//				File file = new File("D:\\test.log");
//				PrintStream stream = new PrintStream(new FileOutputStream(file, true));
//				System.setOut(stream);
//				CMN.Log("000", userDir);
			}
			currentDictionary = loadManager.md.get(0);
		} catch (Exception e) {
			SU.Log(e);
		}
		CMN.Log("init done !!!", loadManager.md_size, loadManager.md.size(), loadManager.md_getName(0));
	}

	public static void main(String[] args) throws Exception {
		List<String> commands = new ArrayList<>();
		commands.add("D:\\Code\\FigureOut\\Textrument\\plugins\\DirectUILib\\PlainDict\\bin\\PlainDict.exe");
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
		return (int) currentDictionary.bookImpl.getNumberEntries();
	}
	
	String getEntryAt(int posisiton) {
		return currentDictionary.bookImpl.getEntryAt(posisiton);
	}

}

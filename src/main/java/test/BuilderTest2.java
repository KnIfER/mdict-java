package test;

import com.knziha.plod.plaindict.CMN;

import java.io.*;

public class BuilderTest2 {
	
    public static void main(String[] args) throws IOException {
    	
    	String prefix = "vidd3nsexymap";
    	
    	File file = new File("D:\\"+prefix+".txt");

		BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
		String str = null;
		File basePath = new File("D:\\vidd");
		int cc=0;
		while((str=bufferedReader.readLine()) != null) {
			//System.out.println(str);
			CMN.Log(str);
			//str = str.trim();
			cc++;
			CMN.Log(new File(basePath, str).exists());
			CMN.Log(new File(basePath, str).renameTo(new File(basePath, prefix+"_"+cc+".mp4")));
		}
    	
    	
    	
    	
    }
}



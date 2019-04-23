package test;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

import com.knziha.plod.dictionary.mdict;
import com.knziha.plod.dictionaryBuilder.mdictBuilder;

/**
 * TEsts
 * @author KnIfER
 * @date 2018/06/02
 */
public class Example_build_irish2English_from_english2irish {
	
    static String source = "F:\\video_wrkst\\javaStation\\github\\mdict-parser-java\\assets\\En-Irish(by Pat Griffin 2007).mdx";
    public static void main(String[] args) throws IOException{
    	mdict md = new mdict(source);
    	//CMN.show(md.getEntryAt(1347));
    	//CMN.show(md.getRecordAt(1347));
    	//md.printDictInfo();
    	
    	if(true) {
        	mdictBuilder mdxBD = new mdictBuilder("Irish-En(converted from Pat Griffin's En-Irish by k)",
        			"Pat Griffin 2007-k 2018<br>k's dictionary factory 出品"
        			,"UTF-8");
        	///*
        	 OutputStreamWriter fOut = new OutputStreamWriter(new FileOutputStream("C:\\debug.txt"));
        	 //*/	
	    	for(int i=0;i<md.getNumberEntries();i++) {
	    		String res = md.getRecordAt(i);
	    		String res2 = res.substring(res.indexOf("`2`")+3, res.length());
	    		String[] gross = res2.split(",");
	    		String retroContent = md.getEntryAt(i);
	    		//CMN.show(retroContent);
				/*
    			if(res2.contains("`2`")) {
    				fOut.write(res);
    				//CMN.show("|"+i+"|"+res);
    				//CMN.show(entrI.trim());
    			}
				 */	

    			if(false) {
    				fOut.write(retroContent);
    				fOut.write("\n");
    				fOut.flush();
    				//CMN.show("|"+i+"|"+res);
    				//CMN.show(entrI.trim());
    			}
	    		for(String entrI:gross) {
	    			String entry = entrI.trim();
	    			if(!entry.equals(""))
	    				mdxBD.insert(entry, retroContent);
	    		}
	    		
	    	}
	    	mdxBD.write("C:\\Irish-En.mdx");
    	}
    	
    	mdict md2 = new mdict("C:\\Irish-En.mdx");
    	md2.printDictInfo();
    	
    	//CMN.show(md2.getEntryAt(1328));
    	CMN.show(md2.getRecordAt(13280));
    	//md.printAllKeys();//md2.lookUp("úth")
    	//TODO entry过长时会出错 record
    	//CMN.show(md2.getRecordAt(md2.lookUp("rite")));
    	//CMN.show(md2.getEntryAt(32000));//TODO add border check
    	
    }
}



package test;

import java.io.FileNotFoundException;
import java.io.IOException;

import com.knziha.plod.dictionary.mdict;
import com.knziha.plod.dictionaryBuilder.mdictBuilder;

/**
 * TEsts
 * @author KnIfER
 * @date 2018/05/31
 */
public class Builder_latin {
	
    static int d=0;
    public static void main(String[] args) throws IOException{
    	mdictBuilder mdxBD = new mdictBuilder("encarta2009_converted","converted by mdict-java builder from encarta2009","UTF-8");

		mdict mraw = new mdict("E:\\assets\\mdicts\\测试用\\encarta2009.mdx");

		
    	for(int i=0;i<mraw.getNumberEntries();i++) {
    		mdxBD.insert(mraw.getEntryAt(i), mraw.getRecordAt(i));
    	}
    	
    	
    	mdxBD.write("F:\\123Test.mdx");
    	
    	//mdict md = new mdict("F:\\123Test.mdx");//en-irish.mdx

    }
}



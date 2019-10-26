package test.Examples;

import com.knziha.plod.dictionary.mdict;
import com.knziha.plod.dictionaryBuilder.mdictBuilder;

import java.io.IOException;

/**
 * TEsts
 * @author KnIfER
 * @date 2019/10/25
 */
public class Rebuild_formation {
	
    static int d=0;
    public static void main(String[] args) throws IOException{
    	mdictBuilder mdxDB = new mdictBuilder("GoldenDict Hunspell en_US构词法规则库","converted by mdict-java builder to reduce entries of unwanted formation rules","UTF-8");

		mdict mraw = new mdict("D:\\assets\\mdicts\\构词法\\白鸽英语构词法.mdx");

    	for(int i=0;i<mraw.getNumberEntries();i++) {
    		String key = mraw.getEntryAt(i).trim();
    		String record = mraw.getRecordAt(i).trim();
    		if(startWith(record, key)){
				mdxDB.insert(key, record);
			}
    	}

		mdxDB.setZLibCompress(true);
    	mdxDB.write("D:\\assets\\mdicts\\构词法\\精简白鸽英语构词法.mdx");
    }

	private static boolean startWith(String record, String key) {
    	if(record.length()>=2 && key.length()>=2){
    		if(record.substring(0,2).equalsIgnoreCase(key.substring(0,2))){
    			return true;
			}
		}
		return false;
	}
}



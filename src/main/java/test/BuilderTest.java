package test;

import java.io.IOException;

import com.knziha.plod.dictionary.mdict;
import com.knziha.plod.dictionaryBuilder.Utils.CU;
import com.knziha.plod.dictionaryBuilder.mdictBuilder;

/**
 * TEsts
 * @author KnIfER
 * @date 2018/05/31
 */
public class BuilderTest {
	
    static int d=0;
    public static void main(String[] args) throws IOException{
    	mdictBuilder mdxBD = new mdictBuilder("hellowrld","ooooo","UTF-8");
    	mdxBD.insert("happy", "happy means I possess you");
    	mdxBD.insert("sad", "sad means I lose you");
    	mdxBD.insert("aasadad", "sad means I lose you");
    	mdxBD.insert("parenthesis", "sad means I lose you");
    	mdxBD.insert("dianthus", "sad means I lose you");
    	mdxBD.insert("dasdad", "sad means I lose you");
    	mdxBD.insert("eaasdd", "sad means I lose you");
    	mdxBD.insert("fsdad", "sad means I lose you");
    	mdxBD.insert("gasdad", "sad means I lose you");
    	mdxBD.insert("haasddd", "sad means I lose you");
    	mdxBD.insert("iaasdadd", "sad means I lose you");
    	mdxBD.insert("jaasdsadd", "sad means I lose you");
    	for(int i=0;i<1024;i++) mdxBD.insert(CU.getRandomString(10), "randomly lose heart megamegamega");
    	
    	
    	mdxBD.write("F:\\123Test.mdx");
    	
    	mdict md = new mdict("F:\\123Test.mdx");//en-irish.mdx
    	//md.printAllContents();
    	CMN.show(md.getRecordAt(md.lookUp("happy")));
    	CMN.show(md.getRecordAt(1024));
    	//CMN.show(md.getRecordAt(md.lookUp("zz7n5uibth")));
    	//CMN.show(md.getEntryAt(100));
    	//CMN.show(md.lookUp("happy")+"");
    	//CMN.show(mdxBD.data_tree.xxing(new myCpr("happy","")).getKey().key);
    	//CMN.show(md.getEntryAt(534));
    	//md.printAllContents();
    	//md.prepareItemByKeyInfo(7);
    }
}



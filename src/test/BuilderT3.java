package test;

import java.io.FileNotFoundException;
import java.io.IOException;

import com.knziha.plod.dictionary.CMN;
import com.knziha.plod.dictionary.mdict;
import com.knziha.plod.dictionary.mdict.myCpr;
import com.knziha.plod.dictionary.mdictRes;
import com.knziha.plod.dictionaryBuilder.mdictBuilder;
import com.knziha.plod.dictionaryBuilder.mdictResBuilder;

/**
 * TEsts
 * @author KnIfER
 * @date 2018/05/31
 */
public class BuilderT3 {
	
    static int d=0;
    public static void main(String[] args) throws IOException{
    	mdictResBuilder mddBD = new mdictResBuilder("hellowrld","ooooo");
    	
    	
    	//for(int i=0;i<10240;i++) mddBD.insert(CU.getRandomString2(10), "randomly lose heart megamegamega".getBytes());
    	 mddBD.insert("happy","01happy".getBytes());
    	 mddBD.insert("unhappy","02unhappy".getBytes());
    	
    	mddBD.write("F:\\123Test.mdd");
    	
    	mdictRes md = new mdictRes("F:\\123Test.mdd");//en-irish.mdx
    	//md.printDictInfo();
    	//mdictRes m22  =  new mdictRes("H:\\dictionary_wkst\\writemdict-master\\example_output\\mdd.mdd");
    	//md.printAllContents();
    	CMN.show(new String(md.getRecordAt(md.lookUp("unhappy")))+":"+md.lookUp("unhappy"));
    	//CMN.show(md.getRecordAt(1024));
    	//CMN.show(md.getRecordAt(md.lookUp("zz7n5uibth")));
    	//CMN.show(md.getEntryAt(100));
    	//CMN.show(md.lookUp("happy")+"");
    	//CMN.show(mdxBD.data_tree.xxing(new myCpr("happy","")).getKey().key);
    	//CMN.show(md.getEntryAt(534));
    	//md.printAllContents();
    	//md.prepareItemByKeyInfo(7);
    }
}



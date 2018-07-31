package test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.zip.DataFormatException;

import com.knziha.plod.dictionary.CMN;
import com.knziha.plod.dictionary.mdict;
import com.knziha.rbtree.RBTree_additive;



/**
 * TEsts
 * @author KnIfER
 * @date 2017/12/30
 */
public class MdTest {
	public static long stst;//static start time

	//f = new File("F:\\dictionary_wkst\\omidict-analysis-master\\简明英汉汉英词典.mdx");
    //f = new File("F:\\dictionary_wkst\\omidict-analysis-master\\古生物图鉴.mdx");
    //f = new File("F:\\mdict_wrkst\\mdict-js-master\\makingMDX\\mdd_file.mdx");
    //f = new File("C:\\antiquafortuna\\MDictPC\\doc\\neo\\拉丁语英语[31655](091009).mdx");
    //f = new File("C:\\antiquafortuna\\MDictPC\\doc\\neo\\英语拉丁语字典【匿名原创】【版本日期未注明】.mdx");
    //f = new File("C:\\antiquafortuna\\MDictPC\\doc\\古生物图鉴.mdx");
    //f = new File("C:\\antiquafortuna\\MDictPC\\doc\\NameOfPlants.mdx");
    //f = new File("C:\\antiquafortuna\\MDictPC\\doc\\有毒植物,J.Huang.mdx");
    
    static int d=0;
    public static void main(String[] args) throws IOException, DataFormatException  {
    //assign Mdx File here!
    	final mdict md =  new mdict("E:\\assets\\mdicts\\牛津高阶英汉双解词典.mdx");
    //A keyword to search!			//简明英汉汉英词典.mdx      古生物图鉴.mdx
    	String key = "happy";//abduco@拉丁语英语		马连鞍@有毒植物		happy@English-Chinese 
    	
    	//md.printDictInfo();
    	
    //A bunch of tests~

    	//![0]Basic query and extraction of contents
    	if(true)
    	{	/* false  true  */ 
    		CMN.show("\n\n——————————————————————basic query——————————————————————");
    		key = "happy";
	        //CMN.show("查询 "+key+" ： "+md.getEntryAt(md.lookUp(key)));
    		stst=System.currentTimeMillis();
	        CMN.show("结果html contents of "+key+" ： "+md.getRecordAt(md.lookUp(key)));
	        CMN.show("时耗 time used： "+(System.currentTimeMillis()-stst)+"ms"); 
    	}
        
    	//![1]
    	//md.printAllKeys();
        
        //![2]
        //md.printAllContents();
    	
    	//![3]match middle pattern
        if(false)
        {	/* false  true  */
	    	CMN.show("\r\n糊匹配测试START...");
	    	key = "stizo";
	    	stst=System.currentTimeMillis();
	        md.findAllKeys(key);
	        CMN.show("模糊匹配 Contain:"+key+" time used： "+(System.currentTimeMillis()-stst)+"ms"); 
        }       
        
        if(true)
        {	/* false  true  */
	    	CMN.show("\r\n糊匹配测试START...");
	    	key = "stizo";
	    	stst=System.currentTimeMillis();
	        md.findAllKeys(key);
	        CMN.show("模糊匹配 Contain:"+key+" time used： "+(System.currentTimeMillis()-stst)+"ms"); 
        }   
        
                
        //![5]
        if(false)
        {	/* false  true  */
	    	CMN.show("\r\n多线程全文搜索测试START...");
	    	key = "happy";
	    	stst=System.currentTimeMillis(); //获取STARTtime used 
	        md.findAllContents_MT(key);
	        CMN.show("多线程全文搜索测试 time usedA： "+(System.currentTimeMillis()-stst)+"ms"); 
        }   
        
        /*![6]Advanced mdicts conjunction search.*/
    	/*联合搜索测试*/
        if(false)
        {	/* false  true  */
	        CMN.show("\n\n——————————————————————searching in a bunch of dicts——————————————————————");
	        
	        ArrayList<mdict> mdxs = new ArrayList<mdict>();
	        
	        /*somewhat《English-Chinese essential》、《oxford English-Chinese advanced》*/
	        mdxs.add(new mdict("F:\\dictionary_wkst\\omidict-analysis-master\\简明英汉汉英词典.mdx"));
	        mdxs.add(new mdict("C:\\antiquafortuna\\MDictPC\\doc\\牛津高阶英汉双解词典.mdx"));
	    	
	        
	    	RBTree_additive combining_search_tree = new RBTree_additive();
	    	stst=System.currentTimeMillis();
	    	
	    	key = "happy";// absolute
	    	for(int i=0;i<mdxs.size();i++)
	    	{
	    		mdxs.get(i).size_confined_lookUp(key,combining_search_tree,i,30);
	    	}
	    	
	        CMN.show("时耗 Time used： "+(System.currentTimeMillis()-stst)+"ms"); 
	        CMN.show("联合搜索结果 results： "); 
	        combining_search_tree.inOrder();
        }  

        
        
}}



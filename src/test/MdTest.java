package test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.DataFormatException;

import com.knziha.plod.dictionary.CMN;
import com.knziha.plod.dictionary.mdict;
import com.knziha.plod.dictionary.mdict.findAllDoneListener;
import com.knziha.plod.dictionary.mdictRes;
import com.knziha.plod.dictionary.ripemd128;
import com.knziha.rbtree.RBTree_additive;
import com.knziha.rbtree.additiveMyCpr1;



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

    static ArrayList<additiveMyCpr1> fuzzy_search_result;
    static int d=0;
    public static void main(String[] args) throws IOException, DataFormatException  {
    	
    	
        
        //mdictRes m = new mdictRes("E:\\mdict\\MDictPC\\doc\\wordsmyth2018.mdd");
        //m.printAllKeys();
    	//CMN.show(":::>"+m.getEntryAt(m.lookUp("\\pic\\wolfSnow.jpg")));
    	//ripemd128.printBytes3(m.getRecordAt(m.lookUp("\\一.tif")),"一.tif");
        
    	
    	
    //assign Mdx File here!
    	final mdict md =  new mdict("E:\\assets\\mdicts\\诗经集注（注解两种）\\诗经集注（注解两种）.mdx");//牛津高阶英汉双解词典.mdx
    	//final mdict md =  new mdict("E:\\mdict\\MDictPC\\doc\\清华大学藏战国竹简（伍）.mdx");
    	
    //A keyword to search!			//简明英汉汉英词典.mdx      古生物图鉴.mdx
    	String key = "wolf";//abduco@拉丁语英语		马连鞍@有毒植物		happy@English-Chinese 
    	//CMN.show(md.getRecordAt(md.lookUp("身")));
    	//md.printDictInfo();
    	
    //A bunch of tests~

    	//![0]Basic query and extraction of contents
    	if(false)
    	{	/* false  true  */ 
    		CMN.show("\n\n——————————————————————basic query——————————————————————");
    		key = "happy";
	        //CMN.show("查询 "+key+" ： "+md.getEntryAt(md.lookUp(key)));
    		stst=System.currentTimeMillis();
	        CMN.show("结果html contents of "+key+" ： "+md.getRecordAt(md.lookUp(key)));
	        CMN.show("时耗 time used： "+(System.currentTimeMillis()-stst)+"ms"); 
    	}
        
    	
    	//![3]match middle pattern
        if(false)
        {	/* false  true  */
	    	CMN.show("\r\n糊匹配测试START...");
	    	key = "stizo";
	    	stst=System.currentTimeMillis();
	        md.findAllKeys(key);
	        CMN.show("模糊匹配 Contain:"+key+" time used： "+(System.currentTimeMillis()-stst)+"ms"); 
        }       
        
        if(false)
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

        if(false)
        {	/* false  true  */
        	long time = System.currentTimeMillis();
        	mdictRes md1 =  new mdictRes("H:\\antiquafortuna\\MDictPC\\doc\\mdd_file.mdd");
        	//md1.printAllKeys();
        	//md1.printAllContents();
        	md1.printAllKeys();
        	

        	CMN.show(md1.getEntryAt(24));
        	CMN.show(md1.lookUp("\\js\\Three.js")+"::"+md1.getRecordAt(md1.lookUp("\\033.jpg")));
        	CMN.show(";;;:"+(System.currentTimeMillis()-time));

        	
        }
        
        
        
    	//![3]match middle pattern
        if(false)
        {	/* false  true  */
        	final boolean isCombinedSearching=true;
        	final int adapter_idx=0;
        	final ArrayList<mdict> mdicts = new ArrayList<>();
        	mdicts.add(new mdict("E:\\assets\\mdicts\\牛津高阶英汉双解词典.mdx"));
        	
	    	CMN.show("\r\n糊匹配测试START...");
	    	
	    	final String keyf = "stizo";
	    	stst=System.currentTimeMillis();
	    	mdict.fod = new findAllDoneListener() {
	    		int fuzzy_task_count=-10086;
				@Override
				public void cancelBefore(long Time) {
					
				}
				@Override
				public void harvest(long Time) {
					for (mdict mdI:mdicts) {
						if(fuzzy_task_count==-10086){
							if(isCombinedSearching){
								fuzzy_task_count = mdicts.size();
							}else{
								fuzzy_task_count = 1;
							}
						}
						fuzzy_task_count--;
						if(fuzzy_task_count<=0){
							
							fuzzy_search_result = new ArrayList<additiveMyCpr1>();
							if(isCombinedSearching){
								for(int i=0;i<mdicts.size();i++){
			            			mdict mdtmp = mdicts.get(i);
			            			//if(mdtmp.combining_search_tree2!=null)
			                		for(int ti=0;ti<mdtmp.split_keys_thread_number;ti++){
			                			fuzzy_search_result.addAll(mdtmp.combining_search_tree2[ti]);
			                			mdtmp.combining_search_tree2[ti].clear();
			                		}
			                		mdtmp.combining_search_tree2 = null;
								}
							}else{//单独搜索
								mdict mdtmp;
									mdtmp = mdicts.get(adapter_idx);
			                		for(int ti=0;ti<mdtmp.split_keys_thread_number;ti++){
			                			fuzzy_search_result.addAll(mdtmp.combining_search_tree2[ti]);
			                			mdtmp.combining_search_tree2[ti].clear();
			                		}
								mdtmp.combining_search_tree2 = null;
							}
							for(additiveMyCpr1 k:fuzzy_search_result) {
								CMN.show(k.key);
							}
		            		CMN.show("模糊搜索完成! 耗时"+(System.currentTimeMillis()-stst)+"ms,共搜索到 "+fuzzy_search_result.size()+"个词条！");
		            		
							fuzzy_task_count=-10086;
						}
					}
			        CMN.show("模糊匹配 Contain:"+keyf+" time used： "+(System.currentTimeMillis()-stst)+"ms"); 
				}};
				
        }   
        
      //![3]fuzzy search
        if(false)
        {	/* false  true  */
        	final boolean isCombinedSearching=true;
        	final int adapter_idx=0;
        	final ArrayList<mdict> md1 = new ArrayList<>();
        	md1.add(new mdict("E:\\assets\\mdicts\\牛津高阶英汉双解词典.mdx"         ));
        	//md1.add(new mdict("E:\\assets\\mdicts\\新日漢大辭典.mdx"                 ));
        	//md1.add(new mdict("E:\\assets\\mdicts\\NameOfPlants.mdx"                 ));
        	//md1.add(new mdict("E:\\assets\\mdicts\\ode2_raw.mdx"                     ));
        	//md1.add(new mdict("E:\\assets\\mdicts\\Irish-En.mdx"));
        	//md1.add(new mdict("E:\\assets\\mdicts\\俄汉汉俄辞典.mdx"             ));
        	
	    	CMN.show("\r\n糊匹配测试START...");
	    	
	    	final String keyf = "stizo";
	    	stst=System.currentTimeMillis();
	    	
	    	//!!this's being here is very important, put it in the worker-thread will cause lag
			for(int i=0;i<md1.size();i++){//遍历所有词典
				mdict mdtmp = md1.get(i);
				if(mdtmp.combining_search_tree2==null) {
				}
				else
	    		for(int ti=0;ti<mdtmp.combining_search_tree2.length;ti++){//遍历搜索结果
	    			if(mdtmp.combining_search_tree2[ti]==null) {
	    				continue;
	    			}
	    			mdtmp.combining_search_tree2[ti].clear();
	    		}
				
			}
			stst=System.currentTimeMillis(); //获取开始时间 
	    	
			for(int i=0;i<md1.size();i++){
				try {
					md1.get(i).flowerFindAllKeys(key,i,30);
					//publisResults();
					//if(isCancelled()) break;
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
	    	
	    	
	    	
			int[] firstLookUpTable = new int[md1.size()];
			int size=0;
			int resCount=0;
			for(int i=0;i<md1.size();i++){//遍历所有词典
				mdict mdtmp = md1.get(i);
				if(mdtmp.combining_search_tree2==null) {
				}
				else
	    		for(int ti=0;ti<mdtmp.combining_search_tree2.length;ti++){//遍历搜索结果
	    			if(mdtmp.combining_search_tree2[ti]==null) {
	    				continue;
	    			}
	    			for(additiveMyCpr1 resI:mdtmp.combining_search_tree2[ti])
	    	    		CMN.show(resI.key);
	    			resCount+=mdtmp.combining_search_tree2[ti].size();
	    		}
				firstLookUpTable[i]=resCount;
				
			}
	    	
	    	
    		CMN.show("模糊搜索完成！ 耗时"+(System.currentTimeMillis()-stst)+"ms,共搜索到 "+resCount+"个词条！");
        }   
        
        
        //![6]
        if(false)
        {	/* false  true  */
	    	CMN.show("\r\n多线程全文搜索测试START...");
	    	key = "happy";
	    	md.stst=System.currentTimeMillis(); //获取STARTtime used 
	        md.findAllContents_MT(key);
	        CMN.show("多线程全文搜索测试 time usedA： "+(System.currentTimeMillis()-stst)+"ms");
	        
        }     
        
        //![8]
        if(true)
        {	/* false  true  */
        	mdict.stst=System.currentTimeMillis();
        	stst=System.currentTimeMillis();
	    	CMN.show("\r\n多线程全文搜索测试START...");
	    	key = "七";
	    	stst=System.currentTimeMillis(); //获取STARTtime used 
	        md.flowerFindAllContents(key,0,0);
	        CMN.show("11多线程全文搜索测试 time usedA： "+(System.currentTimeMillis()-stst)+"ms"); 
        }   
        
        //md.flowerFindAllKeys("happy", 0, 0);

        //md.prepareItemByKeyInfo(null, 19);
        

        
        
        
        
        
}}



package com.knziha.plod.dictionarymodels;

import java.util.ArrayList;
import java.util.List;

import com.knziha.plod.PlainDict.PlainDictionaryPcJFX;
import com.knziha.plod.dictionary.Utils.IU;
import javafx.scene.web.WebEngine;

public class resultRecorderScattered extends resultRecorderDiscrete {
	private List<mdict> md;
	private WebEngine engine;
	private int[] firstLookUpTable;
	private int size=0;
	
	@Override
	public void invalidate() {
		if(md.size()==0)
			return;
		if(firstLookUpTable.length<md.size())
			firstLookUpTable = new int[md.size()];

		int resCount=0;
		for(int i=0;i<md.size();i++){//遍历所有词典
			mdict mdtmp = md.get(i);
			//int baseCount=0;
			//if(i!=0)
			//	baseCount=firstLookUpTable[i-1];
			ArrayList<Integer>[] _combining_search_tree=SearchLauncher.getCombinedTree(i);
			if(_combining_search_tree==null)
				_combining_search_tree=SearchLauncher.getInternalTree(mdtmp);

			if(_combining_search_tree!=null)
    		for(int ti=0;ti<_combining_search_tree.length;ti++){//遍历搜索结果
    			if(_combining_search_tree[ti]==null) {
    				continue;
    			}
    			resCount+=_combining_search_tree[ti].size();
    		}
			firstLookUpTable[i]=resCount;
			
		}
		size=resCount;
	}
	
	@Override
	public void invalidate(int idx) {
		if(md.size()==0)
			return;
		if(firstLookUpTable.length<md.size())
			firstLookUpTable = new int[md.size()];

		int resCount=0;
		mdict mdtmp = md.get(idx);

		ArrayList<Integer>[] _combining_search_tree=SearchLauncher.getCombinedTree(idx);
		if(_combining_search_tree==null)
			_combining_search_tree=SearchLauncher.getInternalTree(mdtmp);

		if(_combining_search_tree!=null)
		for(int ti=0;ti<_combining_search_tree.length;ti++){//遍历搜索结果
			if(_combining_search_tree[ti]==null) {
				continue;
			}
			resCount+=_combining_search_tree[ti].size();
		}
		
		//firstLookUpTable[idx]=resCount;
		for(int i=0;i<firstLookUpTable.length;i++) {
			if(i<idx)
				firstLookUpTable[i] = 0;
			else
				firstLookUpTable[i] = resCount;
		}
		size=resCount;
	}
	
	public resultRecorderScattered(PlainDictionaryPcJFX.AdvancedSearchLogicLayer _SearchLauncher, WebEngine engine_){
		md=_SearchLauncher.md;
		engine=engine_;
		firstLookUpTable = new int[md.size()];
		SearchLauncher=_SearchLauncher;
	}
	
	
	@Override
	public String getResAt(int pos) {
		if(size<=0 || pos<0 || pos>size-1)
			return "!!! Error: code 1";
		int Rgn = binary_find_closest(firstLookUpTable,pos+1,md.size());
		if(Rgn<0 || Rgn>md.size()-1)
			return "!!! Error: code 2 Rgn="+Rgn+" size="+md.size();
		mdict mdtmp = md.get(Rgn);
		dictIdx=Rgn;
		if(Rgn!=0)
			pos-=firstLookUpTable[Rgn-1];
		int idxCount = 0;

		ArrayList<Integer>[] _combining_search_tree=SearchLauncher.getCombinedTree(Rgn);
		if(_combining_search_tree==null)
			_combining_search_tree=SearchLauncher.getInternalTree(mdtmp);

		if(_combining_search_tree!=null)
		for(int ti=0;ti<_combining_search_tree.length;ti++){
			if(_combining_search_tree[ti]==null)
				continue;
			int max = _combining_search_tree[ti].size();
			if(max==0)
				continue;
			if(pos-idxCount<max) {
				String tmp=mdtmp.getEntryAt(_combining_search_tree[ti].get(pos-idxCount),mflag);
				if(mdtmp.hasVirtualIndex()){
					int tailIdx=tmp.lastIndexOf(":");
					if(tailIdx>0)
						tmp=tmp.substring(0, tailIdx);
				}
				return tmp;
			}
			idxCount+=max;
		}
		return "!!! Error: code 3 ";
	}

	@Override
	public mdict getMdAt(int pos) {
		if(size<=0 || pos<0 || pos>size-1)
			return null;
		int Rgn = binary_find_closest(firstLookUpTable,pos+1,md.size());
		if(Rgn<0 || Rgn>md.size()-1)
			return null;
		return md.get(Rgn);
	}

	@Override
	public int getIndexAt(int pos) {
		if(size<=0 || pos<0 || pos>size-1)
			return -1;
		return binary_find_closest(firstLookUpTable,pos+1,md.size());
	}
	
	@Override
	public void renderContentAt(int pos, int selfAtIdx, boolean post) {
		if(size<=0 || pos<0 || pos>size-1)
			return;
		int Rgn = binary_find_closest(firstLookUpTable,pos+1,md.size());
		if(Rgn<0 || Rgn>md.size()-1)
			return;
		mdict mdtmp = md.get(Rgn);
		dictIdx=Rgn;
		if(Rgn!=0)
			pos-=firstLookUpTable[Rgn-1];
		int idxCount = 0;

		ArrayList<Integer>[] _combining_search_tree=SearchLauncher.getCombinedTree(Rgn);
		if(_combining_search_tree==null)
			_combining_search_tree=SearchLauncher.getInternalTree(mdtmp);

		if(_combining_search_tree!=null)
		for(int ti=0;ti<_combining_search_tree.length;ti++){
			if(_combining_search_tree[ti]==null)
				continue;
			int max = _combining_search_tree[ti].size();
			if(max==0)
				continue;
			if(pos-idxCount<max) {
				int renderIdx = _combining_search_tree[ti].get(pos-idxCount);
				StringBuilder basic = new StringBuilder();
				if(post) basic.append("postInit=function(){");
				basic.append("setDictAndPos(").append(selfAtIdx == -1 ? Rgn : selfAtIdx).append(",").append(renderIdx).append(");ClearAllPages();processContents('\\r").append(Rgn).append("@");
				if(mdtmp.hasVirtualIndex()){
					String tmp = mdtmp.getEntryAt(renderIdx);
					int tailIdx=tmp.lastIndexOf(":");
					if(tailIdx>0)
						basic.append(IU.parsint(tmp.substring(tailIdx+1),0));
					else
						basic.append(0);
					basic.append(":").append(renderIdx);// 0@0:16@17@18
				}else{
					basic.append(renderIdx);
				}
				basic.append("');pendingHL='").append(currentSearchTerm).append("'");
				if(post) basic.append("}; ScanInDicts();");
				engine.executeScript(basic.toString());
				return;
			}
			idxCount+=max;
		}
		return;
	};
	
	@Override
	public int size(){
		return size;
	};
	
	@Override
	public void shutUp() {
		size=0;
	}

    public static int  binary_find_closest(int[] array,int val,int iLen){
    	int middle = 0;
    	if(iLen==-1||iLen>array.length)
    		iLen = array.length;
    	int low=0,high=iLen-1;
    	if(array==null || iLen<1){
    		return -1;
    	}
    	if(iLen==1){
    		return 0;
    	}
    	if(val-array[0]<=0){
			return 0;
    	}else if(val-array[iLen-1]>0){
    		return iLen-1;
    	}
    	int counter=0;
    	long cprRes1,cprRes0;
    	while(low<high){
    		//CMN.show(low+"~"+high);
    		counter+=1;
    		//System.out.println(low+":"+high);
    		middle = (low+high)/2;
    		cprRes1=array[middle+1]-val;
        	cprRes0=array[middle  ]-val;
        	if(cprRes0>=0){
        		high=middle;
        	}else if(cprRes1<=0){
        		//System.out.println("cprRes1<=0 && cprRes0<0");
        		//System.out.println(houXuan1);
        		//System.out.println(houXuan0);
        		low=middle+1;
        	}else{
        		//System.out.println("asd");
        		//high=middle;
        		low=middle+1;//here
        	}
    	}
		return low;
    }
	
}

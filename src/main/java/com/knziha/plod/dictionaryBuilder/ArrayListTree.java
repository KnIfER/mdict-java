package com.knziha.plod.dictionaryBuilder;

import java.util.ArrayList;

import com.knziha.rbtree.RBTNode;
import com.knziha.rbtree.RBTree;
import com.knziha.rbtree.InOrderTodoAble;

public class ArrayListTree<T extends Comparable<T>> implements InOrderTodoAble {
	//wonderful!
	
	protected final ArrayList<T> data;


	public ArrayListTree(){
		data = new  ArrayList<>();
	}
	
	public int insert(T val){
		if(data.size()==0 || data.get(data.size()-1).compareTo(val)<=0) {
			data.add(data.size(),val);
			return data.size();
		}
		int idx=-1;
		if(data.get(0).compareTo(val)>=0)
			idx=0;
		else
			idx = reduce(val,0,data.size());
		
		if(val.compareTo(data.get(idx))==0) {
			while(idx<data.size()-1 && val.compareTo(data.get(idx+1))==0) {
				idx++;
			}
			if(idx<data.size()-1) idx++;
		}
		data.add(idx,val);
		return idx;
	}
	
	public int reduce(T val,int start,int end) {//via mdict-js
        int len = end-start;
        if (len > 1) {
          len = len >> 1;
          return val.compareTo(data.get(start + len - 1))>0
                    ? reduce(val,start+len,end)
                    : reduce(val,start,start+len);
        } else {
          return start;
        }
    }

	@Override
	public void SetInOrderDo(RBTree.inOrderDo inOrderDo) {
		inOrderDo_=inOrderDo;
	}

	public void inOrderDo() {
		for(T dataLet:data) {
			inOrderDo_.dothis(new RBTNode<T>(dataLet,false,null,null,null));
		}
	}

	@Override
	public void insertNode(Comparable node) {
		insert((T) node);
	}

	RBTree.inOrderDo inOrderDo_;

	public int getCountOf(T key) {
		if(data.size()==0 || data.get(data.size()-1).compareTo(key)<0) {
			return 0;
		}
		int idx = reduce(key,0,data.size());
		int cc=0;
		if(key.compareTo(data.get(idx))==0) {
			cc++;
			while(idx<data.size()-1 && key.compareTo(data.get(idx+1))==0) {
				idx++;cc++;
			}
		}
		return cc;
	}

	public int size() {
		return data.size();
	}

	public void add(T val) {
		data.add(val);
	}



}

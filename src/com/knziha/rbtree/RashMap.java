package com.knziha.rbtree;

import java.util.ArrayList;


/**
 * Java 语言: 红黑树
 *
 * @author skywang
 * @date 2013/11/07
 * @editor KnIfER
 * @date 2017/11/18
 */
class myCpr<T1 extends Comparable<T1>,T2> implements Comparable<myCpr<T1,T2>>{
	public T1 key;
	public T2 value;
	public myCpr(T1 k,T2 v){
		key=k;value=v;
	}
	public int compareTo(myCpr<T1,T2> other) {

		if(key.getClass()==String.class) {

			return ((String)key)
					//.toLowerCase().replace(" ",mdict.emptyStr).replace("-",mdict.emptyStr)
					.compareTo(((String)other.key)    					
					//.toLowerCase().replace(" ",mdict.emptyStr).replace("-",mdict.emptyStr)
							);
		}
		else
			return this.key.compareTo(other.key);
	}
	public String toString(){
		return key+"_"+value;
	}
}
public class RashMap<T1 extends Comparable<T1>,T2>
	extends RBTree<myCpr<T1,T2>> {
	
	int treeSize;
	
	public int size() {
		return treeSize;
	}
	
	@Override
	public void insert(myCpr<T1,T2> data) {
		//myCpr<T1,T2> data = new myCpr<T1,T2>(key,val);
		RBTNode<myCpr<T1,T2>> node=new RBTNode<myCpr<T1,T2>>(data,BLACK,null,null,null);
		int cmp;
        RBTNode<myCpr<T1,T2>> y = null;
        RBTNode<myCpr<T1,T2>> x = this.mRoot;

        // 1. 将红黑树当作一颗二叉查找树，将节点添加到二叉查找树中。
        while (x != null) {
            y = x;
            cmp = node.key.compareTo(x.key);
            if (cmp < 0)
                x = x.left;
            else if(cmp > 0)
                x = x.right;
            else return;
        }
        treeSize++;
        node.parent = y;
        if (y!=null) {
            cmp = node.key.compareTo(y.key);
            if (cmp < 0)
                y.left = node;
            else
                y.right = node;
        } else {
            this.mRoot = node;
        }

        // 2. 设置节点的颜色为红色
        node.color = RED;

        // 3. 将它重新修正为一颗二叉查找树
        insertFixUp(node);
        
	}

	@Override
    protected void remove(RBTNode node) {
        treeSize--;
        super.remove(node);
    }
	
	public T2 get(T1 key) {
		myCpr<T1, T2> preRet = searchT(new myCpr<T1,T2>(key,null));
		return preRet!=null?preRet.value:null;
	}
	
	public void put(T1 key,T2 value) {
		insert(new myCpr<T1,T2>(key,value));
	}
	
    public void keysAndValues(ArrayList<T1> k,ArrayList<T2> v){
        inOrderflattenKey(this.mRoot,k,v);
    }
    
    private void inOrderflattenKey(RBTNode<myCpr<T1,T2>> tree,ArrayList<T1> k,ArrayList<T2> v) {
        if(tree != null) {
        	inOrderflattenKey(tree.left,k,v);
            if(k!=null) k.add(tree.key.key);
            if(k!=null) v.add(tree.key.value);
            inOrderflattenKey(tree.right,k,v);
        }
    }
    
	
    RBTNode<myCpr<T1,T2>> lastSearchRes;
	
	public boolean contains(T1 key) {
		RBTNode<myCpr<T1,T2>> tmp = search(new myCpr<T1,T2>(key,null));
		if(tmp!=null) {
			lastSearchRes=tmp;
			return true;
		}
		return false;
	}

	public void remove(T1 key,T2 val) {
		RBTNode<myCpr<T1,T2>> tmp = search(new myCpr<T1,T2>(key,null));
		if(tmp!=null) {
			remove(tmp);
		}
	}
	
	public void removeLastSelected() {
		remove(lastSearchRes);
	}
	
	public void clear() {
        super.destroy(mRoot);
        mRoot = null;
        treeSize = 0;
    }
}
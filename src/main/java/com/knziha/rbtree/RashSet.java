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

public class RashSet<T1 extends Comparable<T1>>
	extends RBTree<T1> {
	
	int treeSize;
	
	public int size() {
		return treeSize;
	}
	
	RBTNode<T1> lastSearchRes;
	
	public boolean contains(T1 key) {
		RBTNode<T1> tmp = search(key);
		if(tmp!=null) {
			lastSearchRes=tmp;
			return true;
		}
		return false;
	}

	
	public void put(T1 key) {
		insert(key);
	}


	public void removeLastSelected() {
		remove(lastSearchRes);
	}
	
	public  RBTNode<T1> getLastSelected() {
		return lastSearchRes;
	}

	
	@Override
    public RBTNode<T1> xxing_samsara(T1 val){
        RBTNode<T1> tmpnode =downwardNeighbour_skipego(this.mRoot,val);
        
        //	return this.maximum(this.mRoot);
        return tmpnode;
    }
	
	@Override
    public RBTNode<T1> sxing_samsara(T1 val){
        RBTNode<T1> tmpnode =upwardNeighbour_skipego(this.mRoot,val);
        
        //	return this.minimum(this.mRoot);
        return tmpnode;
    }

	@Override
	public void insert(T1 data) {
		//T1 data = new T1(key,val);
		RBTNode<T1> node=new RBTNode<T1>(data,BLACK,null,null,null);
		int cmp;
        RBTNode<T1> y = null;
        RBTNode<T1> x = this.mRoot;

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
	
	public void clear() {
        super.destroy(mRoot);
        mRoot = null;
        treeSize = 0;
    }
	
}
package com.knziha.rbtree;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Java 语言: 红黑树
 *
 * @author skywang
 * @date 2013/11/07
 * @editor KnIfER
 * @date 2017/12/26
 */
public class RBTree_additive {

    private RBTNode<additiveMyCpr1> mRoot;public RBTNode<additiveMyCpr1> getRoot(){return mRoot;}
    
    private static final boolean RED   = false;
    private static final boolean BLACK = true;
    private final static String replaceReg = " |:|\\.|,|-|\'";
    private final static String emptyStr = "";

    
    public RBTree_additive() {
        mRoot=null;
    }

    private RBTNode<additiveMyCpr1> parentOf(RBTNode<additiveMyCpr1> node) {
        return node!=null ? node.parent : null;
    }
    private boolean colorOf(RBTNode<additiveMyCpr1> node) {
        return node!=null ? node.color : BLACK;
    }
    private boolean isRed(RBTNode<additiveMyCpr1> node) {
        return ((node!=null)&&(node.color==RED)) ? true : false;
    }
    private boolean isBlack(RBTNode<additiveMyCpr1> node) {
        return !isRed(node);
    }
    private void setBlack(RBTNode<additiveMyCpr1> node) {
        if (node!=null)
            node.color = BLACK;
    }
    private void setRed(RBTNode<additiveMyCpr1> node) {
        if (node!=null)
            node.color = RED;
    }
    private void setParent(RBTNode<additiveMyCpr1> node, RBTNode<additiveMyCpr1> parent) {
        if (node!=null)
            node.parent = parent;
    }
    private void setColor(RBTNode<additiveMyCpr1> node, boolean color) {
        if (node!=null)
            node.color = color;
    }

    /*
     * 前序遍历"红黑树"
     */
    private void preOrder(RBTNode<additiveMyCpr1> tree) {
        if(tree != null) {
            System.out.print(tree.key+" ");
            preOrder(tree.left);
            preOrder(tree.right);
        }
    }

    public void preOrder() {
        preOrder(mRoot);
    }

    /*
     * 中序遍历"红黑树"
     */
    private void inOrder(RBTNode<additiveMyCpr1> tree) {
        if(tree != null) {
            inOrder(tree.left);
            System.out.print("【"+tree.key+"】\r\n");
            inOrder(tree.right);
        }
    }

    public void inOrder() {
        inOrder(mRoot);
    }

    //mycode
    public int inorderCounter = 0;
    public int inorderCounter2 = 0;
    public int inorderCounter3 = 0;
    //![0]wrap
    public void inOrderDo() {
    	inorderCounter = 0;//important
    	inorderCounter2 = 0;//important
    	inorderCounter3 = 0;//important
        inOrderDo(mRoot);
    }
    //![1]设置接口
    public void SetInOrderDo(inOrderDo ido){
    	mInOrderDo = ido;
    }
    //![2]接口
    public interface inOrderDo{
    	void dothis(RBTNode node);
    }
    private inOrderDo mInOrderDo;
    //![3]中序递归
    private void inOrderDo(RBTNode<additiveMyCpr1> node) {
        if(node != null) {
        	inorderCounter2+=1;
        	inOrderDo(node.left);
        	mInOrderDo.dothis(node);
        	inorderCounter+=1;
            inOrderDo(node.right);
            inorderCounter2-=1;//嘿嘿老子是天才
            
        }
    }
    //![4]
    //![5]此处放大招！!
    //下行wrap :find node x,so that x.key=<val and no node with key greater that x.key satisfies this condition.
    public RBTNode<additiveMyCpr1> xxing(additiveMyCpr1 val){
    	RBTNode<additiveMyCpr1> tmpnode =downwardNeighbour(this.mRoot,val);
    	if (tmpnode!=null) return tmpnode;
    	else return this.minimum(this.mRoot);
    }
     ///情况二///cur///情况一/
    private RBTNode<additiveMyCpr1> downwardNeighbour(RBTNode<additiveMyCpr1> du,additiveMyCpr1 val) {
        int cmp;
        RBTNode<additiveMyCpr1> x = du;
        RBTNode<additiveMyCpr1> tmpnode = null;
        
        if (x==null)
            return null;

        cmp = val.compareTo(x.key);
        if (cmp < 0)//情况一
            return downwardNeighbour(x.left, val);
        else// if (cmp >= 0)//情况二
        	{
        	if(x.right==null ) return x;
        	tmpnode = downwardNeighbour(x.right, val);
        	if (tmpnode==null) return x;
        	else return tmpnode;
        	}
    }
    //上行wrap :find node x,so that x.key>=val and no node with key smaller that x.key satisfies this condition.
    public RBTNode<additiveMyCpr1> sxing(additiveMyCpr1 val){
    	RBTNode<additiveMyCpr1> tmpnode =upwardNeighbour(this.mRoot,val);
    	if (tmpnode!=null) return tmpnode;
    	else return this.maximum(this.mRoot);
    }
     ///情况一////cur///////情况二//
    private RBTNode<additiveMyCpr1> upwardNeighbour(RBTNode<additiveMyCpr1> du,additiveMyCpr1 val) {
        int cmp;
        RBTNode<additiveMyCpr1> x = du;
        RBTNode<additiveMyCpr1> tmpnode = null;
        
        if (x==null)
            return null;

        cmp = val.compareTo(x.key); 
        if (cmp > 0)//情况一
            return upwardNeighbour(x.right, val);
        else// if (cmp =< 0)//情况二
        	{
        	if(x.left==null ) return x;
        	tmpnode = upwardNeighbour(x.left, val);
        	if (tmpnode==null) return x;
        	else return tmpnode;
        	}
    }  
    
    //![END]
    public ArrayList<additiveMyCpr1> flatten(){
    	ArrayList<additiveMyCpr1> res = new ArrayList<additiveMyCpr1>();
    	inOrderflatten(this.mRoot,res);
    	return res;
    	
    }
    private void inOrderflatten(RBTNode<additiveMyCpr1> tree,ArrayList<additiveMyCpr1> res) {
        if(tree != null) {
        	inOrderflatten(tree.left,res);
        	res.add(tree.key);
            inOrderflatten(tree.right,res);
        }
    }
    
    /*
     * 后序遍历"红黑树"
     */
    private void postOrder(RBTNode<additiveMyCpr1> tree) {
        if(tree != null)
        {
            postOrder(tree.left);
            postOrder(tree.right);
            System.out.print(tree.key+" ");
        }
    }

    public void postOrder() {
        postOrder(mRoot);
    }


    /*
     * (递归实现)查找"红黑树x"中键值为key的节点
     */
    private RBTNode<additiveMyCpr1> search(RBTNode<additiveMyCpr1> x, additiveMyCpr1 key) {
        if (x==null)
            return x;

        int cmp = key.compareTo(x.key);
        if (cmp < 0)
            return search(x.left, key);
        else if (cmp > 0)
            return search(x.right, key);
        else
            return x;
    }

    public RBTNode<additiveMyCpr1> search(additiveMyCpr1 key) {
        return search(mRoot, key);
    }

    /*
     * (非递归实现)查找"红黑树x"中键值为key的节点
     */
    private RBTNode<additiveMyCpr1> iterativeSearch(RBTNode<additiveMyCpr1> x, additiveMyCpr1 key) {
        while (x!=null) {
            int cmp = key.compareTo(x.key);

            if (cmp < 0) 
                x = x.left;
            else if (cmp > 0) 
                x = x.right;
            else
                return x;
        }

        return x;
    }

    public RBTNode<additiveMyCpr1> iterativeSearch(additiveMyCpr1 key) {
        return iterativeSearch(mRoot, key);
    }

    /* 
     * 查找最小结点：返回tree为根结点的红黑树的最小结点。
     */
    private RBTNode<additiveMyCpr1> minimum(RBTNode<additiveMyCpr1> tree) {
        if (tree == null)
            return null;

        while(tree.left != null)
            tree = tree.left;
        return tree;
    }

    public additiveMyCpr1 minimum() {
        RBTNode<additiveMyCpr1> p = minimum(mRoot);
        if (p != null)
            return p.key;

        return null;
    }
     
    /* 
     * 查找最大结点：返回tree为根结点的红黑树的最大结点。
     */
    private RBTNode<additiveMyCpr1> maximum(RBTNode<additiveMyCpr1> tree) {
        if (tree == null)
            return null;

        while(tree.right != null)
            tree = tree.right;
        return tree;
    }

    public additiveMyCpr1 maximum() {
        RBTNode<additiveMyCpr1> p = maximum(mRoot);
        if (p != null)
            return p.key;

        return null;
    }

    /* 
     * 找结点(x)的后继结点。即，查找"红黑树中数据值大于该结点"的"最小结点"。
     */
    public RBTNode<additiveMyCpr1> successor(RBTNode<additiveMyCpr1> x) {
        // 如果x存在右孩子，则"x的后继结点"为 "以其右孩子为根的子树的最小结点"。
        if (x.right != null)
            return minimum(x.right);

        // 如果x没有右孩子。则x有以下两种可能：
        // (01) x是"一个左孩子"，则"x的后继结点"为 "它的父结点"。
        // (02) x是"一个右孩子"，则查找"x的最低的父结点，并且该父结点要具有左孩子"，找到的这个"最低的父结点"就是"x的后继结点"。
        RBTNode<additiveMyCpr1> y = x.parent;
        while ((y!=null) && (x==y.right)) {
            x = y;
            y = y.parent;
        }

        return y;
    }
     
    /* 
     * 找结点(x)的前驱结点。即，查找"红黑树中数据值小于该结点"的"最大结点"。
     */
    public RBTNode<additiveMyCpr1> predecessor(RBTNode<additiveMyCpr1> x) {
        // 如果x存在左孩子，则"x的前驱结点"为 "以其左孩子为根的子树的最大结点"。
        if (x.left != null)
            return maximum(x.left);

        // 如果x没有左孩子。则x有以下两种可能：
        // (01) x是"一个右孩子"，则"x的前驱结点"为 "它的父结点"。
        // (01) x是"一个左孩子"，则查找"x的最低的父结点，并且该父结点要具有右孩子"，找到的这个"最低的父结点"就是"x的前驱结点"。
        RBTNode<additiveMyCpr1> y = x.parent;
        while ((y!=null) && (x==y.left)) {
            x = y;
            y = y.parent;
        }

        return y;
    }

    /* 
     * 对红黑树的节点(x)进行左旋转
     *
     * 左旋示意图(对节点x进行左旋)：
     *      px                              px
     *     /                               /
     *    x                               y                
     *   /  \      --(左旋)-.           / \                #
     *  lx   y                          x  ry     
     *     /   \                       /  \
     *    ly   ry                     lx  ly  
     *
     *
     */
    private void leftRotate(RBTNode<additiveMyCpr1> x) {
        // 设置x的右孩子为y
        RBTNode<additiveMyCpr1> y = x.right;

        // 将 “y的左孩子” 设为 “x的右孩子”；
        // 如果y的左孩子非空，将 “x” 设为 “y的左孩子的父亲”
        x.right = y.left;
        if (y.left != null)
            y.left.parent = x;

        // 将 “x的父亲” 设为 “y的父亲”
        y.parent = x.parent;

        if (x.parent == null) {
            this.mRoot = y;            // 如果 “x的父亲” 是空节点，则将y设为根节点
        } else {
            if (x.parent.left == x)
                x.parent.left = y;    // 如果 x是它父节点的左孩子，则将y设为“x的父节点的左孩子”
            else
                x.parent.right = y;    // 如果 x是它父节点的左孩子，则将y设为“x的父节点的左孩子”
        }
        
        // 将 “x” 设为 “y的左孩子”
        y.left = x;
        // 将 “x的父节点” 设为 “y”
        x.parent = y;
    }

    /* 
     * 对红黑树的节点(y)进行右旋转
     *
     * 右旋示意图(对节点y进行左旋)：
     *            py                               py
     *           /                                /
     *          y                                x                  
     *         /  \      --(右旋)-.            /  \                     #
     *        x   ry                           lx   y  
     *       / \                                   / \                   #
     *      lx  rx                                rx  ry
     * 
     */
    
    public void rrt(){//hmm...just for test(may crash)
    	rightRotate(mRoot);
    }
    private void rightRotate(RBTNode<additiveMyCpr1> y) {
        // 设置x是当前节点的左孩子。
        RBTNode<additiveMyCpr1> x = y.left;

        // 将 “x的右孩子” 设为 “y的左孩子”；
        // 如果"x的右孩子"不为空的话，将 “y” 设为 “x的右孩子的父亲”
        y.left = x.right;
        if (x.right != null)
            x.right.parent = y;

        // 将 “y的父亲” 设为 “x的父亲”
        x.parent = y.parent;

        if (y.parent == null) {
            this.mRoot = x;            // 如果 “y的父亲” 是空节点，则将x设为根节点
        } else {
            if (y == y.parent.right)
                y.parent.right = x;    // 如果 y是它父节点的右孩子，则将x设为“y的父节点的右孩子”
            else
                y.parent.left = x;    // (y是它父节点的左孩子) 将x设为“x的父节点的左孩子”
        }

        // 将 “y” 设为 “x的右孩子”
        x.right = y;

        // 将 “y的父节点” 设为 “x”
        y.parent = x;
    }

    /*
     * 红黑树插入修正函数
     *
     * 在向红黑树中插入节点之后(失去平衡)，再调用该函数；
     * 目的是将它重新塑造成一颗红黑树。
     *
     * 参数说明：
     *     node 插入的结点        // 对应《算法导论》中的z
     */
    private void insertFixUp(RBTNode<additiveMyCpr1> node) {
        RBTNode<additiveMyCpr1> parent, gparent;

        // 若“父节点存在，并且父节点的颜色是红色”
        while (((parent = parentOf(node))!=null) && isRed(parent)) {
            gparent = parentOf(parent);

            //若“父节点”是“祖父节点的左孩子”
            if (parent == gparent.left) {
                // Case 1条件：叔叔节点是红色
                RBTNode<additiveMyCpr1> uncle = gparent.right;
                if ((uncle!=null) && isRed(uncle)) {
                    setBlack(uncle);
                    setBlack(parent);
                    setRed(gparent);
                    node = gparent;
                    continue;
                }

                // Case 2条件：叔叔是黑色，且当前节点是右孩子
                if (parent.right == node) {
                    RBTNode<additiveMyCpr1> tmp;
                    leftRotate(parent);
                    tmp = parent;
                    parent = node;
                    node = tmp;
                }

                // Case 3条件：叔叔是黑色，且当前节点是左孩子。
                setBlack(parent);
                setRed(gparent);
                rightRotate(gparent);
            } else {    //若“z的父节点”是“z的祖父节点的右孩子”
                // Case 1条件：叔叔节点是红色
                RBTNode<additiveMyCpr1> uncle = gparent.left;
                if ((uncle!=null) && isRed(uncle)) {
                    setBlack(uncle);
                    setBlack(parent);
                    setRed(gparent);
                    node = gparent;
                    continue;
                }

                // Case 2条件：叔叔是黑色，且当前节点是左孩子
                if (parent.left == node) {
                    RBTNode<additiveMyCpr1> tmp;
                    rightRotate(parent);
                    tmp = parent;
                    parent = node;
                    node = tmp;
                }

                // Case 3条件：叔叔是黑色，且当前节点是右孩子。
                setBlack(parent);
                setRed(gparent);
                leftRotate(gparent);
            }
        }

        // 将根节点设为黑色
        setBlack(this.mRoot);
    }

    /* 
     * 将结点插入到红黑树中
     *
     * 参数说明：
     *     node 插入的结点        // 对应《算法导论》中的node
     */
    //here
    private void insert(RBTNode<additiveMyCpr1> node) {
        int cmp;
        RBTNode<additiveMyCpr1> y = null;
        RBTNode<additiveMyCpr1> x = this.mRoot;

        // 1. 将红黑树当作一颗二叉查找树，将节点添加到二叉查找树中。
        while (x != null) {
            y = x;
            cmp = node.key.compareTo(x.key);
            if (cmp < 0)
                x = x.left;
            else if(cmp > 0)
                x = x.right;
            else{//key 相等，value数组叠加
            	
            	//for(Integer val:node.key.value)
            	//	x.key.value.add(val);
            	//x.key.value.add(val);
            	return;
            }
        }

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
    public ExecutorService fixedThreadPoolmy = Executors.newFixedThreadPool(1);
    //well,this is very..slow
    public void insert_synchronized(final String key,final int...val) {
    	fixedThreadPoolmy.execute(new Runnable(){
			@Override
			public void run() {
				insert(key,val);
			}
    	});
    }
    public void insert(String key,int...val) {
        int cmp;
        //key=key.toLowerCase().replaceAll(replaceReg,emptyStr);
        RBTNode<additiveMyCpr1> y = null;
        RBTNode<additiveMyCpr1> x = this.mRoot;

        // 1. 将红黑树当作一颗二叉查找树，将节点添加到二叉查找树中。
        while (x != null) {
            y = x;
            //cmp = key.toLowerCase().replace(" ",emptyStr).replace("'",emptyStr).compareTo(x.key.key.toLowerCase().replace(" ",emptyStr).replace("'",emptyStr));
            cmp = key.toLowerCase().replace(" ",emptyStr).replace("'",emptyStr).replace(":",emptyStr).replace(".",emptyStr).replace("-",emptyStr).replace(",",emptyStr).compareTo(x.key.key.toLowerCase().replace(" ",emptyStr).replace("'",emptyStr).replace(":",emptyStr).replace(".",emptyStr).replace("-",emptyStr).replace(",",emptyStr));
            
            if (cmp < 0)
                x = x.left;
            else if(cmp > 0)
                x = x.right;
            else{//key 相等，value数组叠加
        		for(int i:val) ((ArrayList<Integer>) x.key.value).add(i);
            	return;//here
            }
        }

        additiveMyCpr1 node_key = new additiveMyCpr1(key,new ArrayList<Integer>());
        for(int i:val) ((ArrayList<Integer>) node_key.value).add(i);//here
        RBTNode<additiveMyCpr1> node = new RBTNode<additiveMyCpr1>(node_key,BLACK,null,null,null);

        // 如果新建结点失败，则返回。
        if (node == null) return;
		
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
    /* 
     * 新建结点(key)，并将其插入到红黑树中
     *
     * 参数说明：
     *     key 插入结点的键值
     */
    public void insert(additiveMyCpr1 key) {
        RBTNode<additiveMyCpr1> node=new RBTNode<additiveMyCpr1>(key,BLACK,null,null,null);

        // 如果新建结点失败，则返回。
        if (node != null)
            insert(node);
    }


    /*
     * 红黑树删除修正函数
     *
     * 在从红黑树中删除插入节点之后(红黑树失去平衡)，再调用该函数；
     * 目的是将它重新塑造成一颗红黑树。
     *
     * 参数说明：
     *     node 待修正的节点
     */
    private void removeFixUp(RBTNode<additiveMyCpr1> node, RBTNode<additiveMyCpr1> parent) {
        RBTNode<additiveMyCpr1> other;

        while ((node==null || isBlack(node)) && (node != this.mRoot)) {
            if (parent.left == node) {
                other = parent.right;
                if (isRed(other)) {
                    // Case 1: x的兄弟w是红色的  
                    setBlack(other);
                    setRed(parent);
                    leftRotate(parent);
                    other = parent.right;
                }

                if ((other.left==null || isBlack(other.left)) &&
                    (other.right==null || isBlack(other.right))) {
                    // Case 2: x的兄弟w是黑色，且w的俩个孩子也都是黑色的  
                    setRed(other);
                    node = parent;
                    parent = parentOf(node);
                } else {

                    if (other.right==null || isBlack(other.right)) {
                        // Case 3: x的兄弟w是黑色的，并且w的左孩子是红色，右孩子为黑色。  
                        setBlack(other.left);
                        setRed(other);
                        rightRotate(other);
                        other = parent.right;
                    }
                    // Case 4: x的兄弟w是黑色的；并且w的右孩子是红色的，左孩子任意颜色。
                    setColor(other, colorOf(parent));
                    setBlack(parent);
                    setBlack(other.right);
                    leftRotate(parent);
                    node = this.mRoot;
                    break;
                }
            } else {

                other = parent.left;
                if (isRed(other)) {
                    // Case 1: x的兄弟w是红色的  
                    setBlack(other);
                    setRed(parent);
                    rightRotate(parent);
                    other = parent.left;
                }

                if ((other.left==null || isBlack(other.left)) &&
                    (other.right==null || isBlack(other.right))) {
                    // Case 2: x的兄弟w是黑色，且w的俩个孩子也都是黑色的  
                    setRed(other);
                    node = parent;
                    parent = parentOf(node);
                } else {

                    if (other.left==null || isBlack(other.left)) {
                        // Case 3: x的兄弟w是黑色的，并且w的左孩子是红色，右孩子为黑色。  
                        setBlack(other.right);
                        setRed(other);
                        leftRotate(other);
                        other = parent.left;
                    }

                    // Case 4: x的兄弟w是黑色的；并且w的右孩子是红色的，左孩子任意颜色。
                    setColor(other, colorOf(parent));
                    setBlack(parent);
                    setBlack(other.left);
                    rightRotate(parent);
                    node = this.mRoot;
                    break;
                }
            }
        }

        if (node!=null)
            setBlack(node);
    }

    /* 
     * 删除结点(node)，并返回被删除的结点
     *
     * 参数说明：
     *     node 删除的结点
     */
    private void remove(RBTNode<additiveMyCpr1> node) {
        RBTNode<additiveMyCpr1> child, parent;
        boolean color;

        // 被删除节点的"左右孩子都不为空"的情况。
        if ( (node.left!=null) && (node.right!=null) ) {
            // 被删节点的后继节点。(称为"取代节点")
            // 用它来取代"被删节点"的位置，然后再将"被删节点"去掉。
            RBTNode<additiveMyCpr1> replace = node;

            // 获取后继节点
            replace = replace.right;
            while (replace.left != null)
                replace = replace.left;

            // "node节点"不是根节点(只有根节点不存在父节点)
            if (parentOf(node)!=null) {
                if (parentOf(node).left == node)
                    parentOf(node).left = replace;
                else
                    parentOf(node).right = replace;
            } else {
                // "node节点"是根节点，更新根节点。
                this.mRoot = replace;
            }

            // child是"取代节点"的右孩子，也是需要"调整的节点"。
            // "取代节点"肯定不存在左孩子！因为它是一个后继节点。
            child = replace.right;
            parent = parentOf(replace);
            // 保存"取代节点"的颜色
            color = colorOf(replace);

            // "被删除节点"是"它的后继节点的父节点"
            if (parent == node) {
                parent = replace;
            } else {
                // child不为空
                if (child!=null)
                    setParent(child, parent);
                parent.left = child;

                replace.right = node.right;
                setParent(node.right, replace);
            }

            replace.parent = node.parent;
            replace.color = node.color;
            replace.left = node.left;
            node.left.parent = replace;

            if (color == BLACK)
                removeFixUp(child, parent);

            node = null;
            return ;
        }

        if (node.left !=null) {
            child = node.left;
        } else {
            child = node.right;
        }

        parent = node.parent;
        // 保存"取代节点"的颜色
        color = node.color;

        if (child!=null)
            child.parent = parent;

        // "node节点"不是根节点
        if (parent!=null) {
            if (parent.left == node)
                parent.left = child;
            else
                parent.right = child;
        } else {
            this.mRoot = child;
        }

        if (color == BLACK)
            removeFixUp(child, parent);
        node = null;
    }

    /* 
     * 删除结点(z)，并返回被删除的结点
     *
     * 参数说明：
     *     tree 红黑树的根结点
     *     z 删除的结点
     */
    public void remove(additiveMyCpr1 key) {
        RBTNode<additiveMyCpr1> node; 

        if ((node = search(mRoot, key)) != null)
            remove(node);
    }

    /*
     * 销毁红黑树
     */
    private void destroy(RBTNode<additiveMyCpr1> tree) {
        if (tree==null)
            return ;

        if (tree.left != null)
            destroy(tree.left);
        if (tree.right != null)
            destroy(tree.right);

        tree=null;
    }

    public void clear() {
        destroy(mRoot);
        mRoot = null;
    }

    /*
     * 打印"红黑树"
     *
     * key        -- 节点的键值 
     * direction  --  0，表示该节点是根节点;
     *               -1，表示该节点是它的父结点的左孩子;
     *                1，表示该节点是它的父结点的右孩子。
     */
    private void print(RBTNode<additiveMyCpr1> tree, additiveMyCpr1 key, int direction) {

        if(tree != null) {

            if(direction==0)    // tree是根节点
                System.out.printf("【%s(B)】 is root\n", tree.key.toString());
            else                // tree是分支节点
                System.out.printf("【%s(%s)】 is 【%s】's %6s child\n", tree.key.toString(), isRed(tree)?"R":"B", key.toString(), direction==1?"right" : "left");

            print(tree.left, tree.key, -1);
            print(tree.right,tree.key,  1);
        }
    }

    public void print() {
        if (mRoot != null)
            print(mRoot, mRoot.key, 0);
    }
}
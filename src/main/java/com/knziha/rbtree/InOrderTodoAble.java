package com.knziha.rbtree;

public interface InOrderTodoAble {
        void SetInOrderDo(RBTree.inOrderDo ido);
        void inOrderDo();
        void insertNode(Comparable node);
		int size();
}
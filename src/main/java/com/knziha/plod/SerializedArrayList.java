/*
 * Copyright (C) 2014 The Android Open Source Project
 * Copyright (c) 1997, 2013, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package com.knziha.plod;

import java.util.Arrays;
import java.util.Collection;

public class SerializedArrayList// implements List, RandomAccess, Cloneable, java.io.Serializable
{
	/** The Backup data. */
	protected byte[] elementData;
	/** The size of each element. */
	public final int elementStep;
    /** The size of the Backup data (the number of elements * elementStep). */
    protected int size;
    
	protected int modCount;
	
	protected byte[] dummyData;
	
    public SerializedArrayList(int stepSize, int initialCapacity) {
		this.elementStep = stepSize;
		this.elementData = new byte[initialCapacity*stepSize];
    }

    public void trimToSize() {
        modCount++;
        if (size < elementData.length) {
            elementData = (size == 0)
              ? DUMMY_DATA()
              : Arrays.copyOf(elementData, size);
        }
    }
	
	protected byte[] DUMMY_DATA() {
		return dummyData==null? dummyData =new byte[elementStep]: dummyData;
	}
	
	/**
     * 增加容量大小，使列表至少能够容纳minCapacity个元素。
     *
     * @param   minCapacity   the desired minimum capacity (per element)
     */
    public void ensureCapacity(int minCapacity) {
		minCapacity *= elementStep;
        if (minCapacity > elementData.length) {
            ensureExplicitCapacity(minCapacity);
        }
    }
	
	/**
	 * @param   minCapacity   the desired minimum capacity (per byte)
	 */
    private void ensureCapacityInternal(int minCapacity) {
		if (minCapacity > elementData.length) {
			ensureExplicitCapacity(minCapacity);
		}
    }

    private void ensureExplicitCapacity(int minCapacity) {
        modCount++;

        // overflow-conscious code
        if (minCapacity - elementData.length > 0)
            grow(minCapacity);
    }

    /**
     * The maximum size of array to allocate.
     * Some VMs reserve some header words in an array.
     * Attempts to allocate larger arrays may result in
     * OutOfMemoryError: Requested array size exceeds VM limit
     */
    private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;

    /**
     * Increases the capacity to ensure that it can hold at least the
     * number of elements specified by the minimum capacity argument.
     *
     * @param minCapacity the desired minimum capacity
     */
    private void grow(int minCapacity) {
        // overflow-conscious code
        int oldCapacity = elementData.length;
        int newCapacity = oldCapacity + (oldCapacity >> 1);
        if (newCapacity - minCapacity < 0)
            newCapacity = minCapacity;
        if (newCapacity - MAX_ARRAY_SIZE > 0)
            newCapacity = hugeCapacity(minCapacity);
        // minCapacity is usually close to size, so this is a win:
        elementData = Arrays.copyOf(elementData, newCapacity);
    }

    private static int hugeCapacity(int minCapacity) {
        if (minCapacity < 0) // overflow
            throw new OutOfMemoryError();
        return (minCapacity > MAX_ARRAY_SIZE) ?
            Integer.MAX_VALUE :
            MAX_ARRAY_SIZE;
    }

    /**
     * Returns the number of elements in this list.
     *
     * @return the number of elements in this list
     */
    public int size() {
        return size/elementStep;
    }

    /**
     * Returns <tt>true</tt> if this list contains no elements.
     *
     * @return <tt>true</tt> if this list contains no elements
     */
    public boolean isEmpty() {
        return size == 0;
    }

    /**
     * Returns <tt>true</tt> if this list contains the specified element.
     * More formally, returns <tt>true</tt> if and only if this list contains
     * at least one element <tt>e</tt> such that
     * <tt>(o==null&nbsp;?&nbsp;e==null&nbsp;:&nbsp;o.equals(e))</tt>.
     *
     * @param o element whose presence in this list is to be tested
     * @return <tt>true</tt> if this list contains the specified element
     */
    public boolean contains(byte[] o) {
        return indexOf(o) >= 0;
    }

    /**
     * Returns the index of the first occurrence of the specified element
     * in this list, or -1 if this list does not contain the element.
     * More formally, returns the lowest index <tt>i</tt> such that
     * <tt>(o==null&nbsp;?&nbsp;get(i)==null&nbsp;:&nbsp;o.equals(get(i)))</tt>,
     * or -1 if there is no such index.
     */
    public int indexOf(byte[] o) {
        if (o == null || o.length>elementStep) {
			return -1;
        } else {
            for (int i = 0; i < size-elementStep; i+=elementStep)
                if (fastCompare(elementData, i, o))
                    return i/elementStep;
        }
        return -1;
    }
	
	public boolean fastCompare(byte[] A,int offA,byte[] B){
		for(int i=0;i<elementStep;i++){
			if(A[offA+i]!=B[i])
				return false;
		}
		return true;
	}
	
    /**
     * Returns the index of the last occurrence of the specified element
     * in this list, or -1 if this list does not contain the element.
     * More formally, returns the highest index <tt>i</tt> such that
     * <tt>(o==null&nbsp;?&nbsp;get(i)==null&nbsp;:&nbsp;o.equals(get(i)))</tt>,
     * or -1 if there is no such index.
     */
    public int lastIndexOf(byte[] o) {
		if (o == null || o.length>elementStep) {
			return -1;
		} else {
            for (int i = size-elementStep; i >= 0; i-=elementStep)
				if (fastCompare(elementData, i, o))
					return i;
        }
        return -1;
    }

//    public byte[] get(int index) {
//        if (index >= size)
//            throw new IndexOutOfBoundsException(outOfBoundsMsg(index));
//
//        return (E) elementData[index];
//    }

    public void set(int index, byte[] element, byte[] oldValue) {
        if (index >= size/elementStep)
            throw new IndexOutOfBoundsException(outOfBoundsMsg(index));
		int tpos = index*elementStep;
		if (oldValue!=null) System.arraycopy(elementData, tpos, oldValue, 0, elementStep);
		System.arraycopy(element, 0, elementData, tpos, elementStep);
    }

    /**
     * Appends the specified element to the end of this list.
     *
     * @param e element to be appended to this list
     * @return <tt>true</tt> (as specified by {@link Collection#add})
     */
    public boolean add(byte[] e) {
        ensureCapacityInternal(size + elementStep);  // Increments modCount!!
		System.arraycopy(e, 0, elementData, size, elementStep);
		size+=elementStep;
        return true;
    }

    /**
     * Inserts the specified element at the specified position in this
     * list. Shifts the element currently at that position (if any) and
     * any subsequent elements to the right (adds one to their indices).
     *
     * @param index index at which the specified element is to be inserted
     * @param element element to be inserted
     * @throws IndexOutOfBoundsException {@inheritDoc}
     */
    public void add(int index, byte[] element) {
        if (index > size/elementStep || index < 0)
            throw new IndexOutOfBoundsException(outOfBoundsMsg(index));

        ensureCapacityInternal(size + elementStep);  // Increments modCount!!
		int tpos = index*elementStep;
        System.arraycopy(elementData, tpos, elementData, tpos+elementStep,
                         size - tpos);
		System.arraycopy(element, 0, elementData, tpos, elementStep);
        size+=elementStep;
    }

    /**
     * Removes the element at the specified position in this list.
     * Shifts any subsequent elements to the left (subtracts one from their
     * indices).
     *
     * @param index the index of the element to be removed
     * @return the element that was removed from the list
     * @throws IndexOutOfBoundsException {@inheritDoc}
     */
    public void remove(int index, byte[] oldValue) {
        if (index >= size/elementStep)
            throw new IndexOutOfBoundsException(outOfBoundsMsg(index));

        modCount++;
		int tpos = index*elementStep;
        if (oldValue!=null) System.arraycopy(elementData, tpos, oldValue, 0, elementStep);

        int numMoved = size - tpos - elementStep;
        if (numMoved > 0)
            System.arraycopy(elementData, tpos+elementStep, elementData, tpos,
                             numMoved);
	
		size -= elementStep;
    }

    /*
     * Private remove method that skips bounds checking and does not
     * return the value removed.
     */
    private void fastRemove(int index) {
		modCount++;
		int tpos = index*elementStep;
		int numMoved = size - tpos - elementStep;
		if (numMoved > 0)
			System.arraycopy(elementData, tpos+elementStep, elementData, tpos,
					numMoved);
		size -= elementStep;
    }

    /**
     * Removes all of the elements from this list.  The list will
     * be empty after this call returns.
     */
    public void clear() {
        modCount++;
        size = 0;
    }

    /**
     * Removes from this list all of the elements whose index is between
     * {@code fromIndex}, inclusive, and {@code toIndex}, exclusive.
     * Shifts any succeeding elements to the left (reduces their index).
     * This call shortens the list by {@code (toIndex - fromIndex)} elements.
     * (If {@code toIndex==fromIndex}, this operation has no effect.)
     *
     * @throws IndexOutOfBoundsException if {@code fromIndex} or
     *         {@code toIndex} is out of range
     *         ({@code fromIndex < 0 ||
     *          fromIndex >= size() ||
     *          toIndex > size() ||
     *          toIndex < fromIndex})
     */
    protected void removeRange(int fromIndex, int toIndex) {
        // Android-changed: Throw an IOOBE if toIndex < fromIndex as documented.
        // All the other cases (negative indices, or indices greater than the size
        // will be thrown by System#arrayCopy.
        if (toIndex < fromIndex) {
            throw new IndexOutOfBoundsException("toIndex < fromIndex");
        }

        modCount++;
		int fpos = fromIndex*elementStep;
		int tpos = toIndex*elementStep;
        int numMoved = size - tpos;
        System.arraycopy(elementData, tpos, elementData, fpos,
                         numMoved);

        size = size - (tpos-fpos);
    }

    /**
     * Constructs an IndexOutOfBoundsException detail message.
     * Of the many possible refactorings of the error handling code,
     * this "outlining" performs best with both server and client VMs.
     */
    private String outOfBoundsMsg(int index) {
        return "Index: "+index+", Size: "+size;
    }
	
	public byte[] getData() {
		return elementData;
	}
	
	public int getDataLength() {
		return size;
	}
	
	public void setData(byte[] data, int length) {
    	if (data!=null) {
			if (length<=data.length) {
				if (elementData!=data) {
					elementData = data;
				}
				size = length/elementStep*elementStep;
			}
		} else {
			if (length<=elementData.length) {
				size = length/elementStep*elementStep;
			}
		}
	}
}

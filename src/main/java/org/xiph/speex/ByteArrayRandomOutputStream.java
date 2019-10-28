/*
 * Copyright (c) 1994, 2013, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package org.xiph.speex;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;

public class ByteArrayRandomOutputStream extends OutputStream {
	protected byte buf[];
	
	protected int count;
	protected int offset;
	
	public ByteArrayRandomOutputStream() {
		this(32);
	}

	
	public ByteArrayRandomOutputStream(int size) {
		if (size < 0) {
			throw new IllegalArgumentException("Negative initial size: "
					+ size);
		}
		buf = new byte[size];
	}

	
	private void ensureCapacity(int minCapacity) {
		// overflow-conscious code
		if (minCapacity - buf.length > 0)
			grow(minCapacity);
	}

	
	private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;

	
	private void grow(int minCapacity) {
		// overflow-conscious code
		int oldCapacity = buf.length;
		int newCapacity = oldCapacity << 1;
		if (newCapacity - minCapacity < 0)
			newCapacity = minCapacity;
		if (newCapacity - MAX_ARRAY_SIZE > 0)
			newCapacity = hugeCapacity(minCapacity);
		buf = Arrays.copyOf(buf, newCapacity);
	}

	private static int hugeCapacity(int minCapacity) {
		if (minCapacity < 0) // overflow
			throw new OutOfMemoryError();
		return (minCapacity > MAX_ARRAY_SIZE) ?
				Integer.MAX_VALUE :
				MAX_ARRAY_SIZE;
	}

	public void seek(int i) {
		ensureCapacity((offset=i)+1);
		if(offset>=count)
			count = offset;
	}
	
	public synchronized void write(int b) {
		ensureCapacity(offset+1);
		buf[offset] = (byte) b;
		if(++offset>=count)
			count = offset;
	}
	
	public synchronized void write(byte b[], int off, int len) {
		if ((off < 0) || (off > b.length) || (len < 0) ||
				((off + len) - b.length > 0)) {
			throw new IndexOutOfBoundsException();
		}
		ensureCapacity(offset + len);
		System.arraycopy(b, off, buf, offset, len);
		if((offset += len)>=count)
			count = offset;
	}

	
	public synchronized void writeTo(OutputStream out) throws IOException {
		out.write(buf, 0, count);
	}

	
	public synchronized void reset() {
		offset = 0;
		count = 0;
	}

	
	public synchronized byte toByteArray()[] {
		return Arrays.copyOf(buf, count);
	}

	
	public synchronized int size() {
		return count;
	}

	
	public synchronized String toString() {
		return new String(buf, 0, count);
	}

	
	public synchronized String toString(String charsetName)
			throws UnsupportedEncodingException
	{
		return new String(buf, 0, count, charsetName);
	}

	
	@Deprecated
	public synchronized String toString(int hibyte) {
		return new String(buf, hibyte, 0, count);
	}

	
	public void close() throws IOException {
	}

}

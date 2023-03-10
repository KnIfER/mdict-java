package com.knziha.plod.dictionary.Utils;

import java.io.ByteArrayInputStream;

public class RecyclableByteArrayInputStream extends ByteArrayInputStream {
	public RecyclableByteArrayInputStream(byte[] buf) {
		super(buf);
	}
	
	public RecyclableByteArrayInputStream(byte[] buf, int offset, int length) {
		super(buf, offset, length);
	}
	
	public ByteArrayInputStream newInstance() {
		return new ByteArrayInputStream(buf);
	}
	
	public final void recycle() {
		this.pos = 0;
	}
}

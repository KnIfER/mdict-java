package com.knziha.plod.dictionary.Utils;

import org.apache.commons.io.input.CountingInputStream;

import java.io.*;

public class AutoCloseFileStream extends CountingInputStream {
	private final long length;
	private boolean closed;

	public AutoCloseFileStream(File ft) throws IOException {
		this(new FileInputStream(ft), ft.length());
	}

	public AutoCloseFileStream(FileInputStream in, long len) throws IOException {
		super(in);
		if(len<=0) {
			len=in.available();
		}
		length=len;
	}

	@Override
	protected synchronized void afterRead(int n) {
		super.afterRead(n);
		if(getByteCount()>=length) {
			try {
				closed=true;
				SU.Log("AutoCloseFileStream 关闭");
				close();
			} catch (IOException e) {
				//CMN.Log("AutoCloseFileStream", e);
			}
		}
	}

	@Override
	public int read(byte[] bts, int off, int len) throws IOException {
		if(closed) {
			return 0;
		}
		return super.read(bts, off, len);
	}
}

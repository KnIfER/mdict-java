package com.knziha.plod.dictionary.Utils;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

public class ReusableBufferedInputStream extends BufferedInputStream {
	public ReusableBufferedInputStream(InputStream in, int size) {
		super(in, size);
	}

	public void reset(InputStream _in) throws IOException {
		if(in!=null)
			in.close();
		in = _in;
		pos = 0;
		markpos = -1;
		count = 0;
	}

	public byte[] getBytes(){
		return buf;
	}

	public void abort() throws IOException {
		if(in!=null){
			in.close();
			in=null;
		}
	}
}

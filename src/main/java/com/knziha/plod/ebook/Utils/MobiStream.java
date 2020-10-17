package com.knziha.plod.ebook.Utils;

import com.knziha.plod.dictionary.Utils.ReusableByteOutputStream;
import com.knziha.plod.dictionary.Utils.SU;

public class MobiStream extends ReusableByteOutputStream {
	public MobiStream() {
	}

	public MobiStream(int size) {
		super(size);
	}
	
	public void writeBack(byte c) {
		count--;
	}

	
	public void backWrite(byte c) {
		count--;
		write(c);
		count--;
	}
	
	
	public void  moveForward(int len){
		ensureCapacity(count+len);
		count+=len;
	}

	public void back() {
		if(count>0)
			count--;
		else
			SU.Log("wrong backing !!!");
	}

}

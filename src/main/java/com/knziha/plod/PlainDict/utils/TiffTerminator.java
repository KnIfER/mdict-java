package com.knziha.plod.PlainDict.utils;

import java.io.InputStream;

public interface TiffTerminator{
	byte[] terminateTiff(byte[] data) throws Exception;
	byte[] terminateTiff(InputStream data) throws Exception;
}
package com.knziha.plod.PlainDict.utils;

import com.sun.media.jai.codec.ImageCodec;
import com.sun.media.jai.codec.ImageEncoder;
import com.sun.media.jai.codec.PNGEncodeParam;
import com.sun.media.jai.codec.SeekableStream;

import javax.media.jai.JAI;
import javax.media.jai.RenderedOp;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class JAIConverter implements TiffTerminator {
	@Override
	public byte[] terminateTiff(byte[] data) throws Exception {
		return terminateTiff(new ByteArrayInputStream(data));
	}

	@Override
	public byte[] terminateTiff(InputStream data) throws Exception {
		RenderedOp src = JAI.create("Stream", SeekableStream.wrapInputStream(data, true));
		PNGEncodeParam.Palette pngEncodeParam = new PNGEncodeParam.Palette();
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ImageEncoder enc = ImageCodec.createImageEncoder("PNG", bos, pngEncodeParam);
		enc.encode(src);
		return bos.toByteArray();
	}
}

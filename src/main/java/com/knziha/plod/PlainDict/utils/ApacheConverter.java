package com.knziha.plod.plaindict.utils;

import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.ImagingConstants;
import org.apache.commons.imaging.common.BufferedImageFactory;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class ApacheConverter implements TiffTerminator {
	@Override
	public byte[] terminateTiff(byte[] data) throws Exception {
		return terminateTiff(new ByteArrayInputStream(data));
	}

	@Override
	public byte[] terminateTiff(InputStream data) throws Exception {
		final Map<String, Object> params = new HashMap<>();

		// set optional parameters if you like
		params.put(ImagingConstants.BUFFERED_IMAGE_FACTORY,
				new ManagedImageBufferedImageFactory());
		final BufferedImage image = Imaging.getBufferedImage(data, params);
		ByteArrayOutputStream bos =new ByteArrayOutputStream();
		ImageIO.write(image,"png",bos);//png 为要保存的图片格式




		return bos.toByteArray();
	}


	public static class ManagedImageBufferedImageFactory implements
			BufferedImageFactory {

		@Override
		public BufferedImage getColorBufferedImage(final int width, final int height,
												   final boolean hasAlpha) {
			final GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
			final GraphicsDevice gd = ge.getDefaultScreenDevice();
			final GraphicsConfiguration gc = gd.getDefaultConfiguration();
			return gc.createCompatibleImage(width, height,
					Transparency.TRANSLUCENT);
		}

		@Override
		public BufferedImage getGrayscaleBufferedImage(final int width, final int height,
													   final boolean hasAlpha) {
			return getColorBufferedImage(width, height, hasAlpha);
		}
	}

}

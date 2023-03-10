package com.knziha.plod.plaindict.utils;

import org.junit.Test;
import test.CMN;

public class ColorUtils {

	public static int getComplementaryColor(int color) {
//		int r = 255 - (color>>16)&0xff;
//		int g = 255 - (color>>8)&0xff;
//		int b = 255 - color&0xff;
//		return color&0xff000000 | r<<16 | g<<8 | b;

//		float[] hsl = new float[3];
//		ColorUtils.colorToHSL(color, hsl);
//		hsl[0] = (hsl[0]+180)%360;
//		return ColorUtils.HSLToColor(hsl);
		int alpha = color&0xff000000;
		color = complimentary(rgb2ryb(color),255);
		return ryb2rgb(color)&alpha;

	}

	/** Red-green-blue system to Red-yellow-blue system.
	 * https://github.com/bahamas10/node-rgb2ryb/blob/master/rgb2ryb.js  */
	public static int rgb2ryb(int color) {
		int r = (color>>16)&0xff;
		int g = (color>>8)&0xff;
		int b = color&0xff;
		// Remove the whiteness from the color.
		int w = min(r, g, b);
		r -= w;
		g -= w;
		b -= w;

		int mg = max(r, g, b);

		// Get the yellow out of the red+green.
		int y = Math.min(r, g);
		r -= y;
		g -= y;

		// If this unfortunate conversion combines blue and green, then cut each in
		// half to preserve the value's maximum range.
		if (b!=0 && g!=0) {
			b /= 2.0;
			g /= 2.0;
		}

		// Redistribute the remaining green.
		y += g;
		b += g;

		// Normalize to values.
		int my = max(r, y, b);
		if (my!=0) {
			int n = mg / my;
			r *= n;
			y *= n;
			b *= n;
		}

		// Add the white back in.
		r += w;
		y += w;
		b += w;

		// And return back the ryb typed accordingly.
		return (r << 16) | (y << 8) | b;
	}

	static double cubicInt(double t, double A, double B){
		double weight = t*t*(3-2*t);
		return A + weight*(B-A);
	}

	static int getR(double iR, double iY, double iB) {
		// red
		double x0 = cubicInt(iB, 1.0, 0.163);
		double x1 = cubicInt(iB, 1.0, 0.0);
		double x2 = cubicInt(iB, 1.0, 0.5);
		double x3 = cubicInt(iB, 1.0, 0.2);
		double y0 = cubicInt(iY, x0, x1);
		double y1 = cubicInt(iY, x2, x3);
		return (int) Math.ceil (255 * cubicInt(iR, y0, y1));
	}

	static int getG(double iR, double iY, double iB) {
		// green
		double x0 = cubicInt(iB, 1.0, 0.373);
		double x1 = cubicInt(iB, 1.0, 0.66);
		double x2 = cubicInt(iB, 0.0, 0.0);
		double x3 = cubicInt(iB, 0.5, 0.094);
		double y0 = cubicInt(iY, x0, x1);
		double y1 = cubicInt(iY, x2, x3);
		return (int) Math.ceil (255 * cubicInt(iR, y0, y1));
	}

	static int getB(double iR, double iY, double iB) {
		// blue
		double x0 = cubicInt(iB, 1.0, 0.6);
		double x1 = cubicInt(iB, 0.0, 0.2);
		double x2 = cubicInt(iB, 0.0, 0.5);
		double x3 = cubicInt(iB, 0.0, 0.0);
		double y0 = cubicInt(iY, x0, x1);
		double y1 = cubicInt(iY, x2, x3);
		return (int) Math.ceil (255 * cubicInt(iR, y0, y1));
	}

	public static int ryb2rgb(int color){
		double r = ((color>>16)&0xff) / 255.0;
		double y = ((color>>8)&0xff) / 255.0;
		double b = (color&0xff) / 255.0;
		int R1 = getR(r, y, b);
		int G1 = getG(r, y, b);
		int B1 = getB(r, y, b);
		return (R1 << 16) | (G1 << 8) | B1;
	}

	public static int complimentary(int color, int limit) {
		int r = (color>>16)&0xff;
		int g = (color>>8)&0xff;
		int b = color&0xff;
		if(limit==0)limit = 255;
		return ((limit - r) << 16) | ((limit - g) << 8) | (limit - b);
	}

	private static int max(int r, int y, int b) {
		return Math.max(r, Math.max(y, b));
	}

	private static int min(int r, int y, int b) {
		return Math.min(r, Math.min(y, b));
	}


	@Test
	public void test1()  {
		//CMN.Log(new File("https://fonts.gstatic.com/s/roboto/v18/KFOlCnqEu92Fr1MmWUlfBBc-AMP6lQ.woff").getName());

		CMN.Log(Integer.toHexString(rgb2ryb(0x0000ff)));

		CMN.Log(Integer.toHexString(ryb2rgb(rgb2ryb(0x0000ff))));

	}
}

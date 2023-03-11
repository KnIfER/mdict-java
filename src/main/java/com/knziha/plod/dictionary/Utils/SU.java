/*  Copyright 2018 PlainDict author

	Licensed under the Apache License, Version 2.0 (the "License");
	you may not use this file except in compliance with the License.
	You may obtain a copy of the License at
	
	    http://www.apache.org/licenses/LICENSE-2.0
	
	Unless required by applicable law or agreed to in writing, software
	distributed under the License is distributed on an "AS IS" BASIS,
	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
	See the License for the specific language governing permissions and
	limitations under the License.
	
	Mdict-Java Query Library
*/

package com.knziha.plod.dictionary.Utils;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.text.Normalizer;


/**
 * @author KnIfER
 * @date 2018/05/31
 */
public class  SU{
	public static Object UniversalObject;
    public static boolean debug;//StringUtils

	public static String trimStart(String input) {
		int len = input.length();
        int st = 0;

        while ((st < len) && (input.charAt(st) <= ' ')) {
            st++;
        }
        
        return ((st > 0) || (len < input.length())) ? input.substring(st, len) : input;
    }
	
    public static int compareTo(String strA,String strB,int start, int lim) {
        int len1 = strA.length();
        int len2 = strB.length();
        int _lim = Math.min(Math.min(len1-start, len2-start),lim);

        int k = 0;
        while (k < _lim) {
            char c1 = strA.charAt(k+start);
            char c2 = strB.charAt(k+start);
            if (c1 != c2) {
                return c1 - c2;
            }
            k++;
        }
        return _lim==lim?0:len1 - len2;
    }


	public static void Log(Object... o) {
		String msg="fatal_log_mdict : ";
		if(o!=null)
			for(int i=0;i<o.length;i++) {
				if(Exception.class.isInstance(o[i])) {
					ByteArrayOutputStream s = new ByteArrayOutputStream();
					PrintStream p = new PrintStream(s);
					((Exception)o[i]).printStackTrace(p);
					msg+=s.toString();
				}
				msg+=o[i]+" ";
			}
		System.out.println(msg);
	}

	public static String alter_file_suffix(String path, String h) {
		int idx = path.indexOf(".");
		if(idx>0) {
			path = path.substring(0, idx);
		}
		path += h;
		return path;
	}
	
	public static String toHexRGB(int color) {
		color&=0xFFFFFF;
		if(color==0) {
			return null;
		}
		String val = Integer.toHexString(color);
		for (int i = val.length(); i < 6; i++) {
			val = "0"+val;
		}
		return val;
	}

	public static int hashCode(String toHash, int start, int len) {
		int h=0;
		len = Math.min(toHash.length(), len);
		for (int i = start; i < len; i++) {
			h = 31 * h + Character.toLowerCase(toHash.charAt(i));
		}
		return h;
	}

	public static String valueOf(CharSequence text) {
		return text == null ? null : text.toString();
	}
	
	//static net.jpountz.lz4.LZ4Factory factory;

	public static void Lz4_decompress(byte[] compressed, int offset, byte[] output, int out_offset, int decompressedLen) {
//		if (factory==null) {
//			factory = net.jpountz.lz4.LZ4Factory.fastestInstance();
//		}
//		factory.fastDecompressor().decompress(compressed, offset, output, out_offset, decompressedLen);
	}
	public static void Zstd_decompress(byte[] compressed, int offset, int length, byte[] output, int out_offset, int decompressedLen) {
		////new ZstdDecompressor().decompress(compressed, offset, length, output, out_offset, decompressedLen);
		//Zstd.decompressByteArray(output, out_offset, decompressedLen, compressed, offset, length);
	}

	public boolean CharsequenceEqual(CharSequence cs1, CharSequence cs2) {
		if(cs1!=null&&cs2!=null) {
			int len1=cs1.length();
			if(len1==cs2.length()) {
				for (int i = 0; i < len1; i++) {
					if(cs1.charAt(i)!=cs2.charAt(i)) {
						return false;
					}
				}
				return true;
			}
		}
		return false;
	}


	public static String removeDiacritics(String text) {
		if (!Normalizer.isNormalized(text, Normalizer.Form.NFD)) {
			text = Normalizer.normalize(text, Normalizer.Form.NFD);
			char[] chars = text.toCharArray();
			int j = 0;
			for (char c : chars) {
				chars[j] = c;
				if(c>'a'&&c<'z' || c>'A'&&c<'Z' || !IsMark(c)) j++;
			}
			text = new String(chars, 0, j);
		}
		return text;
	}

	private static boolean IsMark(char ch) {
		int gc = Character.getType(ch);
//
//		return gc == Character.NON_SPACING_MARK
//				|| gc == Character.ENCLOSING_MARK
//				|| gc == Character.COMBINING_SPACING_MARK;
		return gc>=Character.NON_SPACING_MARK&&gc<=Character.COMBINING_SPACING_MARK;
	}

}
	



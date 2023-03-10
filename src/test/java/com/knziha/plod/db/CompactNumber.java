package com.knziha.plod.db;

import com.knziha.plod.ebook.Utils.BU;
import com.knziha.plod.plaindict.CMN;

import java.nio.charset.StandardCharsets;
import java.util.Random;

public class CompactNumber {




	public static void main(String[] args) throws Exception {
		int seed = 137, length=100, max=1024;
		{
			Random rand = new Random(seed);
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < length; i++) {
				int val = rand.nextInt(max);
				//CMN.Log(val);
				if(sb.length()>0)
					sb.append('/');
				sb.append(val);
			}

			String text = sb.toString();
			byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
			CMN.Log(bytes.length);
			CMN.Log(BU.zlib_compress(bytes, 0, -1).length);
		}

		CMN.Log();
		
		{
			Random rand = new Random(seed);
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < length; i++) {
				int val = rand.nextInt(max);
				//CMN.Log(val);
				if(sb.length()>0)
					sb.append('0');
				sb.append(val);
			}

			String text = sb.toString();
			byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
			CMN.Log(bytes.length);
			CMN.Log(BU.zlib_compress(bytes, 0, -1).length);
		}
		
		
		
		
	}
	
	
}

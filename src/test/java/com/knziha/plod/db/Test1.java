package com.knziha.plod.db;

import com.alibaba.fastjson.JSONObject;
import com.knziha.plod.PlainDict.db.FFDB;
import com.knziha.plod.dictionary.Utils.IU;
import test.CMN;
import test.privateTest.wget.WGet;
import test.privateTest.wget.info.DownloadInfo;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterOutputStream;

public class Test1 {
	
	public static void main(String[] args) throws Exception {
//		FFDB db = FFDB.getInstance();
//		db.openTable("TEST", FFDB.parseObject("{rowId:0, rid:0, fav:0}"), FFDB.parseObject("{rid:1}"));
//		
//		db.put("TEST", FFDB.parseObject("{rid:1, fav:2}"));
//		db.put("TEST", FFDB.parseObject("{rid:1, fav:5}"));
//		db.put("TEST", FFDB.parseObject("{rid:3, fav:4}"));
//		JSONObject defVal = FFDB.parseObject("{fav:-1}");
//		CMN.Log(db.get("TEST", defVal, FFDB.parseObject("{rid:1}")));
//		CMN.Log(db.get("TEST", defVal, FFDB.parseObject("{rid:3}")));

//		ExecutorService threadPool = Executors.newFixedThreadPool(5);
//		threadPool.execute(new Runnable() {
//			@Override
//			public void run() {
//				try {
//					DownloadInfo info = new DownloadInfo(new URL("https://"));
//					File file = new File("D:\\test.mp4");
//					WGet wGet = new WGet(info, file);
//					wGet.download();
//				} catch (Exception e) {
//					CMN.Log(e);
//				}
//			}
//		});


		// 制作HZh
		if(false)
		{
			CMN.Log("\uD86D\uDE89");

			// \ u + 2B689

			int val = 0x1B689;
			//CMN.Log(Integer.toHexString(val/0x400+0xD800));
			CMN.Log(Integer.toHexString((val>>10)+0xD800));
			CMN.Log(Integer.toHexString((val&0b1111111111) + 0xDC00));


			CMN.Log(new StringBuilder().append((char)0xd86d).append((char)0xde89));

			CMN.Log(new String(new byte[]{(byte) 0xd8,0x6d, (byte) 0xde, (byte) 0x89}, "utf16"));

			String hanzi = "\uD86D\uDE89";
//		int highSurrogate = ((bytes[0]&0xff)<<16) | bytes[1], lowSurrogate = ((bytes[2]&0xff)<<16) | bytes[3];
			int highSurrogate = hanzi.charAt(0), lowSurrogate = hanzi.charAt(1);


			CMN.Log(Integer.toHexString(
					0x10000+((highSurrogate-0xD800)<<10)
							|	((lowSurrogate-0xDC00))
			));

			HashMap<Integer, Integer> map = new HashMap<>();

			BufferedReader reader = new BufferedReader(new FileReader(new File("C:\\python37\\Lib\\site-packages\\xpinyin\\Mandarin.dat")));
			String line;


			while ((line = reader.readLine())!=null) {
				String[] arr = line.split("\t");
				if (arr.length==2) {
					int code = Integer.parseInt(arr[0], 16);
					if (code>=0x4e00 && code<=0x9fa5) {
						map.put(code, (int) arr[1].toUpperCase().charAt(0));
					}
				}
			}
			reader.close();

			reader = new BufferedReader(new FileReader(new File("F:\\ship\\New Folder\\pinyin4j-2.5.0\\lib\\pinyindb\\unicode_to_hanyu_pinyin.txt")));
			while ((line = reader.readLine())!=null) {
				String[] arr = line.split(" ");
				if (arr.length==2) {
					int code = Integer.parseInt(arr[0], 16);
					if (code>=0x4e00 && code<=0x9fa5 && !arr[1].startsWith("(none")) {
						map.put(code, (int) arr[1].toUpperCase().charAt(1));
					}
				}
			}
			reader.close();

			StringBuilder sb = new StringBuilder();
			int cc=0;
			for (int i = 0x4e00; i <= 0x9fa5; i++) {
				if (!map.containsKey(i)) {
					CMN.Log("没有::", Integer.toHexString(i));
					cc++;
					sb.append(" ");
				} else {
					sb.append((char)(int)map.get(i));
				}
			}
			CMN.Log("没有::", cc);
			CMN.Log("结果::", sb);
		}

		CMN.Log("结果::".hashCode());

		CMN.Log(Short.MAX_VALUE, IU.NumberToText_SIXTWO_LE(Short.MAX_VALUE, null));
		CMN.Log(Long.MAX_VALUE, IU.NumberToText_SIXTWO_LE(Long.MAX_VALUE, null));
		CMN.Log(256, IU.NumberToText_SIXTWO_LE(256, null));
		CMN.Log(0x800, IU.NumberToText_SIXTWO_LE(0x800, null));
		CMN.Log(0x400, IU.NumberToText_SIXTWO_LE(0x400, null));
		CMN.Log();
		CMN.Log(IU.TextToNumber_SIXTWO_LE("z"), IU.TextToNumber_SIXTWO_LE("zz"));
		CMN.Log(IU.TextToNumber_SIXTWO_LE("2X"));
		CMN.Log(IU.TextToNumber_SIXTWO_LE("9W"));
		
		
		CMN.Log(IU.TextToNumber_SIXTWO_LE("ZZZZZZZZzz"));
		CMN.Log(IU.TextToNumber_SIXTWO_LE("7m85Y0n8LzA"));
		
		CMN.Log(IU.TextToNumber_SIXTWO_LE("HGGYCDNMY"));
		
		// 测试压缩url参数
		String url = "wGGFY_0-dOED_E_F_G_H_I_J_K_L_M_N_O_E_F_G_H_I_J_K_L_M_N_O-dLDYGCF_2_3_4_5_6_7_8_9_A_B_C_D_E_F_G-dFYGCF_B-dFYCXBHCD_0-dSLYYGCF_B-dJMYHHYCD_7_8_9_A_B_7_8_9_A_B_7_8_9_A_B_7_8_9_A_B-dIEP5DA_5-wA9KM_-wH1QI_-wI9GV_-wS9JD_-w8uO_-w4sI1_-w0KT_-w5tN_-wRMKJ_-wXRK_-wXSYD_-wBY_-wGQW_-wHD_-wBD_-wBDSS_-wBDXW_-wZH_-wKX_-wGGSS_-wGGFY_0-dJMYHHYCD_7_8_9_A_B_7_8_9_A_B_7_8_9_A_B_7_8_9_A_B-dZGGDZWWZJ1_0-dJCCK3_0-dLWSJYHYCD_4L-d2GW_9a_Aa_Ea-dYZYXCH_D1_E1_F1_G1-dS5r3_A-dDDWLSJ_iI2-dLLSFYCD_2_3_4-dLLSFYCD1_1_2_3-dXDHCD_2_3-dFHHFCD_W1_X1-dFYYYK_1_2_3_4_5-dZYRJ3lD_0_1_2_3_4-dPAKYHCD_0_1-dHYYHCD_5-dYHYZ7A4_k4_l4_m4-dELD4P5_7_8-dGDYY1L8_2-dXNJBetaV2_H_I_J_K-dNJGJYHSJC_0-dOED7EM_0_1-dOALD4_1_2_3-w9JM_0-wGGFY_0-dJMYHHYCD_7_8_9_A_B_7_8_9_A_B_7_8_9_A_B_7_8_9_A_B-wA9KM_-wH1QI_-wI9GV_-wS9JD_-w8uO_-w0KT_-w5tN_-wRMKJ_-wXRK_-wXSYD_-wBY_-wGQW_-wHD_-wBD_-wBDSS_-wBDXW_-wBDXW1_-wZH_-wKX_-wGGSS_-wGGFY_0-wGGFYDEBUG_-dJMYHHYCD_7_8_9_A_B_7_8_9_A_B_7_8_9_A_B_7_8_9_A_B-dOED_E_F_G_H_I_J_K_L_M_N_O_E_F_G_H_I_J_K_L_M_N_O-dCGJYCD1_0-wGGFY_0";

		byte[] data = url.getBytes(StandardCharsets.UTF_8);

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		
//		DeflaterOutputStream def = new DeflaterOutputStream(out);
//		def.write(data, 0, data.length);
//		def.close();
//		data = out.toByteArray();
		
		Deflater df = new Deflater();
		df.setLevel(9);
		df.setInput(data, 0, data.length);
		df.finish();
		byte[] buffer = new byte[1024];
		while (!df.finished()) {
			int n1 = df.deflate(buffer);
			out.write(buffer, 0, n1);
		}
		data = out.toByteArray();

		String compressed = new String(Base64.getEncoder().encode(data));
		CMN.Log(compressed, compressed.length(), url.length());
	}
	
	
	
}

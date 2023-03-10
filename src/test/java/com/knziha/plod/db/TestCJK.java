package com.knziha.plod.db;

import com.knziha.plod.plaindict.CMN;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class TestCJK {
	
	public static void main(String[] args) throws Exception {

		String url = "https://www.pgyer.com/apiv2/app/view";

		byte[] data = url.getBytes(StandardCharsets.UTF_8);
		String aso = new String(Base64.getEncoder().encode(data));
		CMN.Log(aso, aso.length()-url.length());


	}
	
	
	
}

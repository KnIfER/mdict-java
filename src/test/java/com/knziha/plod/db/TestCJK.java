package com.knziha.plod.db;

import com.knziha.plod.dictionary.Utils.IU;
import test.CMN;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.zip.Deflater;

public class TestCJK {
	
	public static void main(String[] args) throws Exception {

		String url = "https://www.pgyer.com/apiv2/app/view";

		byte[] data = url.getBytes(StandardCharsets.UTF_8);
		String aso = new String(Base64.getEncoder().encode(data));
		CMN.Log(aso, aso.length()-url.length());


	}
	
	
	
}

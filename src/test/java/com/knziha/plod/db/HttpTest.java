package com.knziha.plod.db;

import org.junit.jupiter.api.Test;
import com.knziha.plod.plaindict.CMN;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class HttpTest {
	
	@Test
	public void test() throws IOException {
		HttpURLConnection urlConnection = (HttpURLConnection) new URL("https://gitee.com/knziha/plaindict/raw/master/version.txt").openConnection();
		urlConnection.setRequestMethod("GET");
		urlConnection.setConnectTimeout(1000);
		urlConnection.setUseCaches(true);
		urlConnection.setDefaultUseCaches(true);
		urlConnection.connect();
		final InputStream input = urlConnection.getInputStream();
		String result = StreamToString(input);
		CMN.Log(result);
	}


	public static String StreamToString(InputStream input) throws IOException {
		int bufferSize = 1024;
		char[] buffer = new char[bufferSize];
		StringBuilder out = new StringBuilder();
		Reader in = new InputStreamReader(input, StandardCharsets.UTF_8);
		for (int numRead; (numRead = in.read(buffer, 0, buffer.length)) > 0; ) {
			out.append(buffer, 0, numRead);
		}
		input.close();
		return out.toString();
	}

}

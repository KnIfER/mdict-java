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
package com.knziha.plod.plaindict;

import com.knziha.plod.plaindict.utils.JAIConverter;
import com.knziha.plod.dictionary.Utils.IU;
import com.knziha.plod.dictionary.mdict;

import java.io.*;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.nanohttpd.protocols.http.response.Response.newFixedLengthResponse;


/**
 * Mdict Server
 * @author KnIfER
 * date 2020/06/02
 */

public class MdictServerLaptop extends MdictServer {
	public MainActivityUIBase a;
	private JAIConverter tiffConverter;

	public MdictServerLaptop(int port, MainActivityUIBase app) throws IOException {
		super(port, app);
		a = app;
		MdbResource = new mdict("D:\\Code\\tests\\recover_wrkst\\mdict-java\\src\\main\\java\\com\\knziha\\plod\\PlainDict\\MdbR.mdd");
		setOnMirrorRequestListener((uri, mirror) -> {
			if(uri==null)uri="";
			String[] arr = uri.split("&");
			HashMap<String, String> args = new HashMap<>(arr.length);
			for (int i = 0; i < arr.length; i++) {
				try {
					String[] lst = arr[i].split("=");
					args.put(lst[0], lst[1]);
				} catch (Exception ignored) { }
			}
			int pos=IU.parsint(args.get("POS"), a.currentDisplaying);
			int dx=IU.parsint(args.get("DX"), a.adapter_idx);
			String key=a.etSearch_getText();
			try {
				key=URLDecoder.decode(args.get("KEY"),"UTF-8");
			}catch(Exception ignored) {}
			String records=null;
			if(!mirror)
				records=args.get("CT");
			if(records==null)
				records=a.record_for_mirror;
			CMN.Log("sending1..."+records);
			{
				try {
					records=URLDecoder.decode(records, "UTF-8");
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			CMN.Log("sending2...");
			CMN.Log("sending2..."+records);
			return newFixedLengthResponse(constructDerivedHtml(key, pos, dx,records));
		});
	}
	
	protected InputStream convert_tiff_img(InputStream restmp) throws Exception {
		//CMN.Log("tif found!!!!!!!!!");
		return new ByteArrayInputStream(((tiffConverter==null?tiffConverter=new JAIConverter():tiffConverter).terminateTiff(restmp)));
		//BU.printFile(bos.toByteArray(), "F:\\htmldownload\\tmp"+key);
	}

	private String record_for_mirror() {
		return null;
	}
	
	@Override
	protected void handle_search_event(Map<String, List<String>> text, InputStream inputStream) {
//		CMN.Log("启动搜索 : ", text
//				, session.getHeaders().get("content-type")
//				, session.getHeaders().get("content-length")
//				, session.getHeaders().get("user-agent")
//				, session.getHeaders().get("content")
//				, session.getParameters().get("f")
//		);
		try {
			InputStream input = inputStream;
			CMN.Log(input.available());
			byte[] data = new byte[input.available()];
			input.read(data);
			String RealText = new String(data);
			CMN.Log(RealText);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	public static InputStream getRemoteServerRes(String key, boolean check) {
		InputStream ret = null;
		if(hasRemoteDebugServer/* && PDICMainAppOptions.debug()*/) {
//			try {
//				return testDebugServer(check?"192.168.0.100":remoteDebugServer, key, check);
//			} catch (Exception e) {
//				if (check) {
//					try {
//						return testDebugServer("192.168.0.102", key, check);
//					} catch (IOException ex) {
//						CMN.debug("getRemoteServerRes failed::"+e);
//						hasRemoteDebugServer = false;
//					}
//				}
//			}
		}
		return ret;
	}

	@Override
	protected InputStream OpenMdbResourceByName(String key) throws IOException {
		try {
			return new FileInputStream(new File("D:\\Code\\tests\\recover_wrkst\\mdict-java\\src\\main\\resources\\com\\knziha\\plod\\plaindict\\Mdict-browser", key));
		} catch (Exception e) {
			System.err.println(e);
		}
		try {
			return MdictServer.class.getResourceAsStream("Mdict-browser"+key);
		} catch (Exception e) {
			System.err.println(e);
		}
		return super.OpenMdbResourceByName(key);
	}
}
/*  Copyright 2018 KnIfER Zenjio-Kang

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
package com.knziha.plod.PlainDict;

import com.knziha.plod.PlainDict.utils.JAIConverter;
import com.knziha.plod.dictionary.Utils.IU;
import com.knziha.plod.dictionary.mdict;
import org.nanohttpd.protocols.http.IHTTPSession;
import test.CMN;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.util.HashMap;

import static org.nanohttpd.protocols.http.response.Response.newFixedLengthResponse;


/**
 * Mdict Server
 * @author KnIfER
 * date 2020/06/02
 */

public class MdictServerOyster extends MdictServer {
	public PlainDictionaryPcJFX a;
	private JAIConverter tiffConverter;

	public MdictServerOyster(int port, PlainDictionaryPcJFX _a, PlainDictAppOptions _opt) throws IOException {
		super(port, _opt);
		a = _a;
		MdbServerLet = _a;
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
			String key=a.etSearch.getText();
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
	protected void handle_search_event(String text, IHTTPSession session) {
		CMN.Log("启动搜索 : ", text
				, session.getHeaders().get("content-type")
				, session.getHeaders().get("content-length")
				, session.getHeaders().get("user-agent")
				, session.getHeaders().get("content")
				, session.getParameters().get("f")
		);
		try {
			InputStream input = session.getInputStream();
			CMN.Log(input.available());
			byte[] data = new byte[input.available()];
			input.read(data);
			String RealText = new String(data);
			CMN.Log(RealText);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
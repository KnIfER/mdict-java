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

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.knziha.plod.dictionary.Utils.SU;
import com.knziha.plod.dictionarymodels.mdict;
import com.knziha.rbtree.RBTree_additive;
import com.knziha.rbtree.additiveMyCpr1;

import javafx.scene.media.AudioClip;

import org.nanohttpd.protocols.http.NanoHTTPD;
import org.nanohttpd.protocols.http.IHTTPSession;
import org.nanohttpd.protocols.http.content.CookieHandler;
import org.nanohttpd.protocols.http.request.Method;
import org.nanohttpd.protocols.http.response.Response;
import org.nanohttpd.protocols.http.response.Status;
import static org.nanohttpd.protocols.http.response.Response.newFixedLengthResponse;


//import fi.iki.elonen.NanoHTTPD;
//import fi.iki.elonen.NanoHTTPD.Response.Status;

import org.xiph.speex.ByteArrayRandomOutputStream;
import org.xiph.speex.manyclass.JSpeexDec;
import test.CMN;


/**
 * Mdict Server
 * @author KnIfER
 * @date 2018/09/19
 */

public class MdictServer extends NanoHTTPD {
    final String SepWindows = "\\";
	private final PlainDictAppOptions opt;

	String baseHtml;
    public ArrayList<mdict> md = new ArrayList<>();
	public ArrayList<mdict> currentFilter = new ArrayList<>();
    int currentPage=0;
    RBTree_additive combining_search_tree = new RBTree_additive();
    StringBuilder sb = new StringBuilder();
    
	public MdictServer(int port, PlainDictAppOptions _opt) {
		super(port);
		opt=_opt;
	}
	
    @Override
    public Response serve(IHTTPSession session) {
    	int adapter_idx_ = 0;
    	String uri = session.getUri();
		//CMN.Log("serving with honor : ", uri);
    	Map<String, String> headerTags = session.getHeaders();
    	String Acc = headerTags.get("accept");
    	String usr = headerTags.get("user-agent");
    	String key = uri.replace("/", SepWindows);
    	//CMN.show("URI : "+uri+" usr:"+usr);
    	//CMN.show(Acc);
    	//CMN.show(session.getQueryParameterString());
    	if(uri.startsWith("/MdbR/")) {
    		//CMN.Log("[fetching internal res : ]", uri);
    		InputStream candi = MdictServer.class.getResourceAsStream("Mdict-browser"+uri);
			if(candi!=null) {
				//CMN.show(uri+" passed");
	    		String mime="*/*";
	    		if(uri.contains(".css")) mime = "text/css";
	    		if(uri.contains(".js")) mime = "text/js";
				try {
					return newFixedLengthResponse(Status.OK,mime,  candi, candi.available());
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
    	}
    	if(uri.startsWith("/MIRROR.jsp")) {
    		if(om!=null)
    			return om.onMirror(session.getQueryParameterString());
    		return newFixedLengthResponse(baseHtml);
    	}
    	
    	if(uri.startsWith("/MdbRSingleQuery/")) {
    		uri = uri.substring("/MdbRSingleQuery/".length());
			//CMN.show("MdbRSingleQuery: "+uri);
    		String[] list = uri.split("/");
    		adapter_idx_ = Integer.valueOf(list[0]);
    		return newFixedLengthResponse(Integer.toString(md.get(adapter_idx_).lookUp(Reroute(uri.substring(list[0].length()+1)),false)));
    	}else if(uri.startsWith("/MdbRJointQuery/")) {
    		uri = Reroute(uri.substring("/MdbRJointQuery/".length()));
    		//CMN.show("MdbRJointQuery: "+uri);
    		RBTree_additive combining_search_tree_ = new RBTree_additive();
    		StringBuilder sb_ = new StringBuilder();
			for(int i=0;i<md.size();i++){
				try {
					md.get(i).size_confined_lookUp5(uri,combining_search_tree_,i,30);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			ArrayList<additiveMyCpr1> combining_search_result = combining_search_tree_.flatten();
    		for(int i=0;i<combining_search_result.size();i++) {
    			additiveMyCpr1 resI = combining_search_result.get(i);
    			sb_.append(resI.key).append("\r");
    			ArrayList<Integer> result = (ArrayList<Integer>) resI.value;
    			int lastIdx=-1;
    			for(int ii=0;ii<result.size();ii+=2) {
    				int currIdx=result.get(ii);
    				if(lastIdx!=currIdx) {
    					if(lastIdx!=-1)
    						sb_.append("&");
    					sb_.append(currIdx);
    				}
    				lastIdx = currIdx;
    				sb_.append("@").append(result.get(ii+1));
    			}
    			if(i!=combining_search_result.size()-1)
    				sb_.append("\n");
    		}
    		CMN.show(sb_.toString());
    		return newFixedLengthResponse(sb_.toString());
    	}
    	////////////////////////////////////////////////////////
    	if(uri.startsWith("/base/")) {
    		uri = uri.substring("/base/".length());
    		String[] list = uri.split("/");
    		adapter_idx_ = Integer.valueOf(list[0]);
    		uri = uri.substring(list[0].length());
    		key = uri.replace("/", SepWindows);
    		if(list[1].equals("@@@")) {//  /base/0/@@@/name
    			key = uri.substring(list[0].length()+1+3);
    			CMN.show("rerouting..."+key);
        		try {
					return newFixedLengthResponse(md.get(adapter_idx_).getRecordsAt(md.get(adapter_idx_).lookUp(key)));//.replace("sound://", "sound/").replace("entry://", "entry/")
				} catch (IOException e) {
					e.printStackTrace();
				}
        		return null;
    		}
    	}
    	
    	if(uri.startsWith("/entry/")) {
			try {
	    		key=key.substring("/entry/".length());
	    		String res = md.get(adapter_idx_).getRecordsAt(md.get(adapter_idx_).lookUp(key));
	    		res = constructMdPage(Integer.toString(adapter_idx_), res);
				if(res!=null)
					return newFixedLengthResponse(res);
			} catch (IOException e) {
				e.printStackTrace();
			}
			return emptyResponse;
    	}
    	
    	
    	try {
        	if(uri.toLowerCase().endsWith(".js")) {
				//CMN.Log("candi",uri);
				File candi = new File(new File(md.get(adapter_idx_).getPath()).getParentFile(),new File(uri).getName());
				if(candi.exists())
					return newFixedLengthResponse(Status.OK,"application/x-javascript",  new FileInputStream(candi), candi.length());
					
			}
        	else if(uri.toLowerCase().endsWith(".css")) {
				//Log.e("candi",url);
				File candi = new File(new File(md.get(adapter_idx_).getPath()).getParentFile(),new File(uri).getName());
				//CMN.show("candi"+candi.getAbsolutePath());
				if(candi.exists()) {
					//CMN.show(uri+"```");
					return newFixedLengthResponse(Status.OK,"text/css",  new FileInputStream(candi), candi.length());
				}
			}
    	} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	
    	if(key.equals("\\dicts.json")) {
    		if(md.size()>0) {
        		StringBuilder sb = new StringBuilder();
        		for(mdict mdx:md)
        			sb.append(mdx._Dictionary_fName).append("\n");
        		sb.setLength(sb.length()-1);
        		return newFixedLengthResponse(sb.toString()) ;
    		}
			return emptyResponse;
    	}
    	else if(key.startsWith("\\MdbRSize\\")) {
    		if(md.size()>0) {
    			key = key.substring(10);
    			try {
					//key = URLDecoder.decode(key, "UTF-8");
				} catch (Exception e) {
					e.printStackTrace();
				}
        		long ret=0;
        		for(mdict mdx:md)
        			if(mdx._Dictionary_fName.equals(key))
        				ret=mdx.getNumberEntries();
        		//CMN.show("ret: "+ret+key);
        		return newFixedLengthResponse(ret+"") ;
    		}
			return emptyResponse;
    	}
    	else if(key.startsWith("\\MdbRGetEntries\\")) {
    		if(md.size()>0) {
    			key = key.substring(16);
    			//CMN.show(key);
    			String[] l = key.split("\\\\");
    			//CMN.show(l.length+"");
    			String ret="";
    			for(mdict mdx:md)
        			if(mdx._Dictionary_fName.equals(l[0])) {
        				StringBuilder sb = new StringBuilder();
        				//CMN.show("capacity "+l[2]);
        				int capacity=Integer.valueOf(l[2]);
        				int base = Integer.valueOf(l[1]);
        				for(int i=0;i<capacity;i++) {
        					ret += mdx.getEntryAt(base+i);
        					if(i<capacity-1)
        						ret+="\n";
        					//sb.append(mdx.getEntryAt(base+i)).append("\n");
        				}
        				//sb.setLength(sb.length()-1);
        				//ret = sb.toString();
        				break;
        			}
        		return newFixedLengthResponse(ret) ;
    		}
			return emptyResponse;
    	}

    	
    	if(!usr.contains("Java")) {
    		CMN.show("OUT OF HERE");
    		
    		//adapter_idx_ = 
    	}

    	if(uri.startsWith("/sound/")) {
			CMN.show("-sound requset format: "+Acc+" to "+uri);
			if(false && usr.contains("Java")) {
				uri=uri.substring("/sound".length());
				CMN.Log("playing right on the java-client");
				if(audioClip!=null)
					audioClip.stop();
				audioClip = new AudioClip("http://127.0.0.1:8080/JAVAUDIO"+uri);
				audioClip.play();
				return emptyResponse;
			}
			key=uri.substring(6).replace("/","\\");
    	}
    	if(uri.startsWith("/JAVAUDIO/"))
			key=uri.substring(9).replace("/","\\");
    		
    	//headerTags.get("accept").contains("text/html,application/xhtml+xml,application/xml;") && 
    	if(uri.equals("/")){
			CMN.show("iiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiii");
    		//new Audio('"+"\\sound\\asd.mp3"+"').play()
            //return newFixedLengthResponse(msg + "<img src='\\wordsmyth2018.png'></img><a href='\\sound\\asd.mp3'>asdasd</a></body></html>\n");
            //return newFixedLengthResponse("<img src='\\wordsmyth2018.png'></img>  <a onclick=\"function(){new Audio('\\sound\\123.mp3').play();}\" href='\\sound\\asd.mp3'>asdasd</a> ") ;
            //return newFixedLengthResponse("<img src='\\wordsmyth2018.png'></img>  <a onclick=\"new Audio('https://www.collinsdictionary.com/sounds/6/669/66956/66956.mp3').play();\">asdasd</a> ");
    		try {
            	//CMN.show(md.getRecordAt(currentPage));
            	if(true || baseHtml==null) {//rrr
            		//InputStream fin = new FileInputStream("E:\\eclispe_wrkst3\\PLOD2\\mdict-java-master\\src\\com\\knziha\\plod\\PDPC\\Mdict-browser\\mdict_browser.html");

					InputStream fin = SU.debug?new FileInputStream("D:\\Code\\tests\\recover_wrkst\\mdict-java\\src\\main\\java\\com\\knziha\\plod\\PlainDict\\Mdict-browser\\mdict_browser.html")
							:MdictServer.class.getResourceAsStream("Mdict-browser/mdict_browser.html");

					//CMN.show(""+this.getClass().getResource("Mdict-browser/mdict_browser.html").getFile());
            		byte[] data = new byte[fin.available()];
            		fin.read(data);
            		baseHtml = new String(data);
					fin.close();
            		//CMN.show(baseHtml);
            	}
				return newFixedLengthResponse(baseHtml);//md.getRecordAt(currentPage)
			} catch (IOException e) {
				e.printStackTrace();
			}
    	}

    	if(uri.startsWith("/about/")) {
    		//CMN.Log("about received : ", uri);//  /content/1@6
			uri = uri.substring(7);
			try {
				mdict mdTmp = md.get(Integer.parseInt(uri));
				return newFixedLengthResponse(mdTmp.getAboutString());
			} catch (Exception ignored) { }
		}

    	if(uri.startsWith("/content/")) {
    		//CMN.Log("content received : ", uri); //  /content/1@6
    		uri = uri.substring(9);
    		String[] list = uri.split("@");
    		if(!list[0].equals("")) {
				try {
					adapter_idx_ = Integer.valueOf(list[0]);
					int[] list2 = new int[list.length-1];
					for(int i=0;i<list.length-1;i++)
						list2[i]=Integer.valueOf(list[i+1]);
					return newFixedLengthResponse(constructMdPage(list[0],md.get(adapter_idx_).getRecordsAt(list2)));
					//CMN.show("<html><head><script src=\"/MdbR/iframeResizer.contentWindow.min.js\"></script>     </head><body  >"+md.get(adapter_idx_).getRecordAt(Integer.valueOf(list[1])).replace("sound://", "\\sound\\")+"</body></html>");
					//return newFixedLengthResponse("<!DOCTYPE html><html><head><script src=\"/MdbR/iframeResizer.contentWindow.min.js\"></script><base href=\"/base/"+list[0]+"/\" /> <base target=\"_blank\" /><script charset=\"utf-8\" type=\"text/javascript\" language=\"javascript\">function loadFragmentInToElement(fragment_url, element) { var xmlhttp = new XMLHttpRequest(\"\"); xmlhttp.open(\"POST\", fragment_url); xmlhttp.onreadystatechange = function() { if(xmlhttp.readyState == 4 && xmlhttp.status == 200) { var txtconent = xmlhttp.responseText; element.innerHTML = txtconent; } }; xmlhttp.send(null); };          </script></head><body onload=\"var val=this.document.body.innerHTML;if(new RegExp('^@@@LINK=').test(val)) loadFragmentInToElement('\\@@@\\\\'+val.substring(8), this.document.body);\">"+md.get(adapter_idx_).getRecordAt(Integer.valueOf(list[1])).replace("sound://", "\\sound\\")+"</body></html>");
					//return newFixedLengthResponse("<html><head><script src=\"/MdbR/iframeResizer.contentWindow.min.js\"></script>     </head><body  >"+md.get(adapter_idx_).getRecordAt(Integer.valueOf(list[1])).replace("sound://", "\\sound\\")+"</body></html>");
				} catch (Exception e) {
					e.printStackTrace();
				}
    		}
			return newFixedLengthResponse(constructMdPage(0+"","<div>ERROR FETCHING CONTENT:"+uri+"</div>"));
    	}
    	
    	

    	byte[] restmp = md.get(adapter_idx_).getResourceByKey(key);

    	if(restmp==null){
			return emptyResponse;
		}
    	
    	if(Acc.contains("javascript/") || uri.endsWith(".js")) {
			return newFixedLengthResponse(Status.OK,"application/x-javascript", 
					new ByteArrayInputStream(restmp),
					restmp.length);
    	}
    	
    	if(Acc.contains("/css") || uri.endsWith(".css")) {
			return newFixedLengthResponse(Status.OK,"text/css", 
					new ByteArrayInputStream(restmp),
					restmp.length);
    	}
    	
    	if(Acc.contains("/mp3") || uri.endsWith(".mp3")) {
    		CMN.show("mp3 : "+uri);
			return newFixedLengthResponse(Status.OK,"audio/mpeg",
					new ByteArrayInputStream(restmp),
					restmp.length);
    	}

    	if(uri.endsWith(".spx")) {
			CMN.show("spx : "+uri);
			InputStream rawIn = new ByteArrayInputStream(restmp);
			ByteArrayRandomOutputStream bos = new ByteArrayRandomOutputStream();
			JSpeexDec decoder = new JSpeexDec();
			try {
				decoder.decode(new DataInputStream(new ByteArrayInputStream(restmp)) , bos, JSpeexDec.FILE_FORMAT_WAVE);
				CMN.Log("success");
				return newFixedLengthResponse(Status.OK,"text/x-wav", new ByteArrayInputStream(bos.toByteArray()) , bos.size());
				//return newFixedLengthResponse(Status.OK,"audio/x-wav", new FileInputStream("D:\\assets\\mdicts\\朗文精排版\\GB_ld5_happy_eater3.wav") , new File("D:\\assets\\mdicts\\朗文精排版\\GB_ld5_happy_eater3.wav").length());
				//return newFixedLengthResponse(Status.OK,"audio/*", rawIn , restmp.length);
			} catch (Exception e) { e.printStackTrace(); }
			return newFixedLengthResponse(Status.OK,"audio/mpeg", rawIn , restmp.length);
    	}


    	
    	if(Acc.contains("image/") ) {
			return newFixedLengthResponse(Status.OK,"image/*", 
					new ByteArrayInputStream(restmp),
					restmp.length);
    	}
    	
    	return newFixedLengthResponse(Status.OK,"*/*", 
				new ByteArrayInputStream(restmp),
				restmp.length);
    }

	private String Reroute(String currentText) {
		CMN.Log(currentFilter.size(), "Reroute", currentText);
		try {
			for (mdict mdTmp:currentFilter) {
				Object rerouteTarget = mdTmp.ReRoute(currentText);
				if(rerouteTarget instanceof String){
					String text = (String) rerouteTarget;
					CMN.Log("Reroute",mdTmp._Dictionary_fName, text);
					if(text.trim().length()>0){
						currentText=text;
						break;
					}
				}
			}
		} catch (IOException ignored) { }
		return currentText;
	}

	int MdPageBaseLen=-1;
	String MdPage_fragment1,MdPage_fragment2, MdPage_fragment3="</html>";
	int MdPageLength=0;
	private String constructMdPage(String dictIdx,String record) {
		StringBuilder MdPageBuilder = new StringBuilder(MdPageLength+record.length()+5);
		String MdPage_fragment1=null,MdPage_fragment2=null, MdPage_fragment3="</html>";int MdPageBaseLen=-1; //rrr
		if(MdPageBaseLen==-1){
			try {
				BufferedReader in = new BufferedReader(new InputStreamReader(SU.debug?new FileInputStream("D:\\Code\\tests\\recover_wrkst\\mdict-java\\src\\main\\java\\com\\knziha\\plod\\PlainDict\\Mdict-browser\\MdbR\\subpage.html")
						:MdictServer.class.getResourceAsStream("Mdict-browser/MdbR/subpage.html")));
				String line;
				while ((line=in.readLine())!=null) {
					MdPageBuilder.append(line).append("\r\n");
					if(MdPage_fragment1==null){
						if(line.equals("<base href='/base//'/>")){
							MdPageBuilder.setLength(MdPageBuilder.length()-6);
							MdPage_fragment1=MdPageBuilder.toString();
							MdPageBuilder.setLength(0);
							MdPageBuilder.append("/'/>\r\n");
						}
					}
				}
				MdPage_fragment2=MdPageBuilder.toString();
				MdPageBaseLen=MdPage_fragment1.length();
				MdPageBuilder.setLength(0);
				MdPageLength=MdPage_fragment1.length()+MdPage_fragment2.length()+MdPage_fragment3.length();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		MdPageBuilder.append(MdPage_fragment1);

		MdPageBuilder
				.append(dictIdx)// "/base/0"
				.append(MdPage_fragment2)
				.append(record)
				.append(MdPage_fragment3)
				.toString()
				;
		return MdPageBuilder.toString();
	}
	AudioClip audioClip;
    Response emptyResponse = newFixedLengthResponse(Status.NO_CONTENT,"*/*", "");

    interface OnMirrorRequestListener{
    	public Response onMirror(String uri);
    }OnMirrorRequestListener om;
    
	public void setOnMirrorRequestListener(OnMirrorRequestListener om_) {
		om = om_;
	}

	
	

	int derBaseLen = -1;
	String restFragments;
	
	public String constructDerivedHtml(String key,int pos,int dictionaryIndice,String iframes) {
		StringBuilder derivedHtmlBase;
		if(true || derBaseLen == -1) {//rrr
			String insertsionPoint = "function postInit(){";
			int idx1=baseHtml.indexOf(insertsionPoint);
			//int idx2=baseHtml.indexOf("onscroll='dismiss_menu();'>",idx1);
			String f1 = baseHtml.substring(0,idx1+insertsionPoint.length()+1);
			//String f2 = baseHtml.substring(baseHtml.indexOf("}",idx1+insertsionPoint.length()),idx2);
			//String f3 = baseHtml.substring(baseHtml.indexOf("</div>",idx2));
			restFragments = baseHtml.substring(baseHtml.indexOf("}",idx1+insertsionPoint.length()));
			derivedHtmlBase = new StringBuilder(f1);
			//CMN.show("f1"+f1);
			derBaseLen = f1.length();
		}
		derivedHtmlBase.setLength(derBaseLen);
		if(opt.GetCombinedSearching()){
			derivedHtmlBase.append("document.getElementById('fileBtn').onclick();");
		}
		/** win10 ie 去掉\t会发生量子波动Bug */
		derivedHtmlBase.append("\teditText.value='").append(key).append("';");
		derivedHtmlBase.append("loookup();");
		if(iframes!=null){
			derivedHtmlBase.append("handleMirror('"+iframes.replace("\r","\\r")+"');");
		}
		//CMN.show("pengDingPos"+pos);
		derivedHtmlBase.append(restFragments);
		//CMN.show(derivedHtmlBase.toString());
		return derivedHtmlBase.toString();
	}

	static class VirtualHttpSession implements IHTTPSession {
		String url;
		public VirtualHttpSession(String _url){
			url=_url;
		}

		@Override
		public void execute() throws IOException {

		}

		@Override
		public CookieHandler getCookies() {
			return null;
		}

		@Override
		public Map<String, String> getHeaders() {
			HashMap<String, String> ret = new HashMap<>();
			ret.put("user-agent","Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/605.1 (KHTML, like Gecko) JavaFX/8.0 Safari/605.1");
			ret.put("accept","");
			return ret;
		}

		@Override
		public InputStream getInputStream() {
			return null;
		}

		@Override
		public Method getMethod() {
			return null;
		}

		@Override
		public Map<String, String> getParms() {
			return new HashMap<>();
		}

		//@Override
		public Map<String, List<String>> getParameters() {
			return null;
		}

		@Override
		public String getQueryParameterString() {
			return null;
		}

		@Override
		public String getUri() {
			int index = url.indexOf("/base/");
			if(index!=-1) url=url.substring(index);
			return url;
		}

		@Override
		public void parseBody(Map<String, String> files) throws IOException, ResponseException {

		}

		//@Override
		public String getRemoteIpAddress() {
			return null;
		}


		public String getRemoteHostName() {
			return null;
		}
	}
}
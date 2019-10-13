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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Map;

import com.knziha.plod.dictionary.Utils.SU;
import com.knziha.plod.dictionary.mdict;
import com.knziha.rbtree.RBTree_additive;
import com.knziha.rbtree.additiveMyCpr1;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import org.nanohttpd.protocols.http.NanoHTTPD;
import org.nanohttpd.protocols.http.IHTTPSession;
import org.nanohttpd.protocols.http.response.Response;
import org.nanohttpd.protocols.http.response.Status;
import test.CMN;

import static org.nanohttpd.protocols.http.response.Response.newChunkedResponse;
import static org.nanohttpd.protocols.http.response.Response.newFixedLengthResponse;

/**
 * Mdict Server
 * @author KnIfER
 * @date 2018/09/19
 */

public class MdictServer extends NanoHTTPD {
    final String SepWindows = "\\";
	private final PlainDictAppOptions opt;

	String baseHtml;
    ArrayList<mdict> md = new ArrayList<>();
    mdict currentDictionary;
    int adapter_idx;
    int currentPage=0;
    RBTree_additive combining_search_tree = new RBTree_additive();
    StringBuilder sb = new StringBuilder();
    
	public MdictServer(int port, PlainDictAppOptions _opt) {
		super(port);
		opt=_opt;
	}
	
    @Override
    public Response serve(IHTTPSession session) {
    	int adapter_idx_ = adapter_idx;
    	String uri = session.getUri();
		//CMN.show(uri);
    	Map<String, String> headerTags = session.getHeaders();
    	String Acc = headerTags.get("accept");
    	String usr = headerTags.get("user-agent");
    	String key = uri.replace("/", SepWindows);
    	//CMN.show("URI : "+uri+" usr:"+usr);
    	//CMN.show(Acc);
    	//CMN.show(session.getQueryParameterString());
    	if(uri.startsWith("/MdbR/")) {
    		CMN.Log("[fetching internal res : ]", uri);
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
    		if(om!=null) {
    			return om.onMirror(session.getQueryParameterString());
    		}
    		return newFixedLengthResponse(baseHtml);
    	}
    	
    	if(uri.startsWith("/MdbRSingleQuery/")) {
    		uri = uri.substring("/MdbRSingleQuery/".length());
    		CMN.show("MdbRSingleQuery: "+uri);
    		String[] list = uri.split("/");
    		adapter_idx_ = Integer.valueOf(list[0]);
    		return newFixedLengthResponse(Integer.toString(md.get(adapter_idx_).lookUp(uri.substring(list[0].length()+1),true)));
    	}else if(uri.startsWith("/MdbRJointQuery/")) {
    		uri = uri.substring("/MdbRJointQuery/".length());
    		CMN.show("MdbRJointQuery: "+uri);
    		
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
    			//sb.setLength(sb.length()-1);
    			if(i!=combining_search_result.size()-1)
    				sb_.append("\n");
    		}
    		//CMN.show(sb_.toString());
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
					return newFixedLengthResponse(md.get(adapter_idx_).getRecordsAt(md.get(adapter_idx_).lookUp(key)).replace("sound://", "sound/").replace("entry://", "entry/"));
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
				//Log.e("candi",url);
				File candi = new File(new File(md.get(adapter_idx).getPath()).getParentFile(),new File(uri).getName());
				if(candi.exists())
					return newFixedLengthResponse(Status.OK,"application/x-javascript",  new FileInputStream(candi), candi.length());
					
			}
        	else if(uri.toLowerCase().endsWith(".css")) {
				//Log.e("candi",url);
				File candi = new File(new File(md.get(adapter_idx).getPath()).getParentFile(),new File(uri).getName());
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
		
    	if(false)
    	if(uri.contains(".mp3"))
    	if(usr.contains("Java/") || !usr.contains("Java")){
    		//return_file_stream to java client,or the Chrome browser.
			CMN.show("returning file stream to java...");
    		try {
				return newFixedLengthResponse(Status.OK,"*/*", new FileInputStream("F:\\123.mp3"),new File("F:\\123.mp3").length());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
    	
    	//CMN.show("\n\n");for (Map.Entry<String, String> entry : headerTags.entrySet())  System.out.println(entry.getKey() + " : " + entry.getValue()); 

		if(uri.equals("/sound/"))
    		return emptyResponse;
    	
    	if(uri.contains(".mp3") && uri.startsWith("/sound/")) {
			CMN.show("-sound requset format: "+Acc+" to"+uri);
			if(false && usr.contains("Java")) {
				uri=uri.substring("/sound".length());
				if(mediaPlayer!=null)
            		mediaPlayer.stop();
				mediaPlayer = new MediaPlayer(new Media("http://127.0.0.1:8080/JAVAUDIO"+uri));
				return emptyResponse;
			}
			else {
				if(Acc.contains("text/html,application/xhtml+xml")) {
					//页面跳转拦截
					//intercepting page loading
					//CMN.show("eval href sound...");
					//browser.executeJavaScript("new Audio('"+"\\124.mp3"+"').play();");
					//return newFixedLengthResponse(Status.OK,"*/*", new FileInputStream("F:\\123.mp3"),new File("F:\\123.mp3").length());
					
	            	//mediaPlayer.play();
	        		return emptyResponse;
				}
			}
    	}
    	if(uri.startsWith("/JAVAUDIO/"))
			uri=uri.substring("/JAVAUDIO".length());
    		
    	//headerTags.get("accept").contains("text/html,application/xhtml+xml,application/xml;") && 
    	if(uri.equals("/")){												//new Audio('"+"\\sound\\asd.mp3"+"').play()
            //return newFixedLengthResponse(msg + "<img src='\\wordsmyth2018.png'></img><a href='\\sound\\asd.mp3'>asdasd</a></body></html>\n");
            //return newFixedLengthResponse("<img src='\\wordsmyth2018.png'></img>  <a onclick=\"function(){new Audio('\\sound\\123.mp3').play();}\" href='\\sound\\asd.mp3'>asdasd</a> ") ;
            //return newFixedLengthResponse("<img src='\\wordsmyth2018.png'></img>  <a onclick=\"new Audio('https://www.collinsdictionary.com/sounds/6/669/66956/66956.mp3').play();\">asdasd</a> ");
            CMN.show("iiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiii");
    		try {
            	//CMN.show(md.getRecordAt(currentPage));
            	if(baseHtml==null) {
            		//InputStream fin = new FileInputStream("E:\\eclispe_wrkst3\\PLOD2\\mdict-java-master\\src\\com\\knziha\\plod\\PDPC\\Mdict-browser\\mdict_browser.html");

					InputStream fin = SU.debug?new FileInputStream("D:\\Code\\tests\\recover_wrkst\\mdict-java\\src\\com\\knziha\\plod\\PlainDict\\Mdict-browser\\mdict_browser.html")
							:MdictServer.class.getResourceAsStream("Mdict-browser/mdict_browser.html");

					//CMN.show(""+this.getClass().getResource("Mdict-browser/mdict_browser.html").getFile());
            		byte[] data = new byte[fin.available()];
            		fin.read(data);
            		baseHtml = new String(data);
            		//CMN.show(baseHtml);
            	}
				return newFixedLengthResponse(baseHtml);//md.getRecordAt(currentPage)
			} catch (IOException e) {
				e.printStackTrace();
			}
    	}
    	
    	if(uri.startsWith("/content/")) {//  /content/1@6
			CMN.Log("content received : ", uri);
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
    	
    	

    	byte[] restmp = null;
    	if(md.get(adapter_idx_).mdd!=null)
		try {
    		int idx = md.get(adapter_idx_).mdd.lookUp(key);
    		if(idx==-1) { 
        		CMN.show("chrochro inter_ key is not find: "+key);
    		}else
    			restmp = md.get(adapter_idx_).mdd.getRecordAt(idx);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
    	if(restmp==null)
    		return emptyResponse;
    	
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
    		CMN.show("mp3"+uri);
			return newFixedLengthResponse(Status.OK,"audio/mpeg", 
					new ByteArrayInputStream(restmp),
					restmp.length);
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

    private String constructMdPage(String dictIdx,String record) {
		StringBuilder MdPageBuilder = new StringBuilder();
    	//return "<!DOCTYPE html><html><head>    <script>var cmy = function (ev) {var ev = ev || window.event; var target=ev.target || ev.srcElement; if(target.nodeName.toLocaleLowerCase() == 'a'){href=target.href;if (href) {/*处理页内跳转 alert(word); */idx = href.indexOf('entry/#');var word;if(idx!=-1){word = href.substring(idx+7);target.href='entry://#'+word;}else if(href.substring(0,9)=='entry://#')word=href.substring(9);if(word){console.log(word);alert(parent.document.getElementById('md_'+"+dictIdx+"));var toPos; var ele=document.getElementsByName(word); if(ele&&ele.length>0)toPos=ele[0].getBoundingClientRect().top;else {ele=document.getElementById(word); if(ele)toPos=ele.getBoundingClientRect().top; } alert(toPos); if(toPos)parent.document.getElementById('md_'+"+dictIdx+").scrollTop=0+'px';scrollBy(0,toPos);   }} } } </script>             <base href=\"/base/"+dictIdx+"/\" /> <base target=\"_self\" /><script charset=\"utf-8\" type=\"text/javascript\" language=\"javascript\">function loadFragmentInToElement(fragment_url, element) { var xmlhttp = new XMLHttpRequest(\"\"); xmlhttp.open(\"POST\", fragment_url); xmlhttp.onreadystatechange = function() { if(xmlhttp.readyState == 4 && xmlhttp.status == 200) { var txtconent = xmlhttp.responseText; element.innerHTML = txtconent; } }; xmlhttp.send(null); };</script></head><body    onclick=\"cmy();\"  onload=\"var val=this.document.body.innerHTML;if(new RegExp('^@@@LINK=').test(val)) loadFragmentInToElement('\\@@@\\\\'+val.substring(8), this.document.body);\">"+record.replace("sound://", "sound/").replace("entry://", "entry/")+"</body></html>";
    	//return "<!DOCTYPE html><html><head>    <script>var cmy = function (ev) {var ev = ev || window.event; var target=ev.target || ev.srcElement; if(target.nodeName.toLocaleLowerCase() == 'a'){href=target.href;if (href) {/*处理页内跳转 alert(word); */idx = href.indexOf('entry/#');var word;if(idx!=-1){word = href.substring(idx+7);target.href='entry://#'+word;}else if(href.substring(0,9)=='entry://#')word=href.substring(9);if(word){console.log(word);    window.location.href.replace('#'+word);     alert(window.location.href); }} } } </script>             <base href=\"/base/"+dictIdx+"/\" /> <base target=\"_self\" /><script charset=\"utf-8\" type=\"text/javascript\" language=\"javascript\">function loadFragmentInToElement(fragment_url, element) { var xmlhttp = new XMLHttpRequest(\"\"); xmlhttp.open(\"POST\", fragment_url); xmlhttp.onreadystatechange = function() { if(xmlhttp.readyState == 4 && xmlhttp.status == 200) { var txtconent = xmlhttp.responseText; element.innerHTML = txtconent; } }; xmlhttp.send(null); };</script></head><body    onclick=\"cmy();\"  onload=\"var val=this.document.body.innerHTML;if(new RegExp('^@@@LINK=').test(val)) loadFragmentInToElement('\\@@@\\\\'+val.substring(8), this.document.body);\">"+record.replace("sound://", "sound/").replace("entry://", "entry/")+"</body></html>";
    	return MdPageBuilder.append("<!DOCTYPE html><html><head> <style>.highlight { background-color: yellow }</style>")
				//.append("<script src=\"/MdbR/mark.js\"></script>")
				.append("<script> ")
				.append(" window.onclick=function(e){ parent.window.dismiss_menu(); }; ")
				.append("var bOnceHighlighted; function clearHighlights(){ if(bOnceHighlighted && MarkInst && MarkLoad) MarkInst.unmark({ done: function() { bOnceHighlighted=false; } }); } function highlight(keyword){ var b1=keyword==null; if(b1 && parent.window.app) keyword=parent.window.app.getCurrentPageKey(); if(keyword==null||b1&&keyword.trim().length==0) return; if(!MarkLoad){ loadJs('/MdbR/mark.js', function(){ MarkLoad=true; do_highlight(keyword); }); }else do_highlight(keyword); } function do_highlight(keyword){ if(!MarkInst) MarkInst = new Mark(document); MarkInst.unmark({ done: function() { bOnceHighlighted=false; MarkInst.mark(keyword, { separateWordSearch: true, done: function() { bOnceHighlighted=true; } }); } }); } function loadJs(url,callback){ var script=document.createElement('script'); script.type=\"text/javascript\"; if(typeof(callback)!=\"undefined\"){ if(script.readyState){ script.onreadystatechange=function(){ if(script.readyState == \"loaded\" || script.readyState == \"complete\"){ script.onreadystatechange=null; callback(); } } }else{ script.onload=function(){ callback(); } } } script.src=url; document.body.appendChild(script); }")//处理高亮
				.append("var MarkLoad,MarkInst; var cmy = function (ev) {var ev = ev || window.event; var target=ev.target || ev.srcElement; if(target.nodeName.toLocaleLowerCase() == 'a'){href=target.href;if (href) {/*处理页内跳转 alert(word); */idx = href.indexOf('entry/#');var word;if(idx!=-1){word = href.substring(idx+7);target.href='entry://#'+word;}else if(href.substring(0,9)=='entry://#')word=href.substring(9);if(word){console.log(\"wordword\"+word);     var toPos; var ele=document.getElementsByName(word); if(ele&&ele.length>0)toPos=ele[0].getBoundingClientRect().top;else {ele=document.getElementById(word); if(ele)toPos=ele.getBoundingClientRect().top; } if(toPos)if(!parent.scrollExpandAll){parent.document.getElementById('md_'+").append(dictIdx).append(").contentWindow.scrollTo(0,toPos);}else{/*自己转alert(123);*/parent.document.getElementById('defP').scrollTo(0, toPos+parent.document.getElementById('md_'+").append(dictIdx).append(").scrollTop)}            }} } } </script>")
				.append("<base href=\"/base/").append(dictIdx)// "/base/0"
				.append("/\" /> <base target=\"_self\" /><script charset=\"utf-8\" type=\"text/javascript\" language=\"javascript\">function loadFragmentInToElement(fragment_url, element) { var xmlhttp = new XMLHttpRequest(\"\"); xmlhttp.open(\"POST\", fragment_url); xmlhttp.onreadystatechange = function() { if(xmlhttp.readyState == 4 && xmlhttp.status == 200) { var txtconent = xmlhttp.responseText; element.innerHTML = txtconent; } }; xmlhttp.send(null); };</script></head>")
				.append("<body    onclick=\"cmy();\"  onload=\"console.log('mdpage loaded');  highlight(null) ;  if(!parent.scrollExpandAll){parent.document.getElementById('md_").append(dictIdx).append("').contentWindow.scrollTo(0,0);}else{parent.document.getElementById('defP').scrollTo(0, parent.document.getElementById('md_").append(dictIdx).append("').scrollTop)}var val=this.document.body.innerHTML;if(new RegExp('^@@@LINK=').test(val)) loadFragmentInToElement('\\@@@\\\\'+val.substring(8), this.document.body);\">")
				.append(record.replace("sound://", "sound/").replace("entry://", "entry/"))
				.append("</body></html>")
				.toString()
				;
	}
	MediaPlayer mediaPlayer;
    Response emptyResponse = newChunkedResponse(Status.NO_CONTENT,"*/*", null);

    interface OnMirrorRequestListener{
    	public Response onMirror(String uri);
    }OnMirrorRequestListener om;
    
	public void setOnMirrorRequestListener(OnMirrorRequestListener om_) {
		om = om_;
	}

	
	
	StringBuilder derivedHtmlBase;
	int derBaseLen = -1;
	String[] fragments;
	
	public String constructDerivedHtml(String key,int pos,int dictionaryIndice,String...iframes) {
		if(derBaseLen == -1) {
			//int idx1=baseHtml.indexOf("function postInit(){");
			String insertsionPoint = "function postInit(){";
			int idx1=baseHtml.indexOf(insertsionPoint);
			int idx2=baseHtml.indexOf("<div class='def' id='def'></div>",idx1);
			String f1 = baseHtml.substring(0,idx1+insertsionPoint.length()+1);
			String f2 = baseHtml.substring(baseHtml.indexOf("}",idx1),idx2);
			String f3 = baseHtml.substring(baseHtml.indexOf("</div>",idx2));
			fragments = new String[] {f2,f3};
			derivedHtmlBase = new StringBuilder(f1);
			//CMN.show("f1"+f1);
			derBaseLen = f1.length();
		}
		derivedHtmlBase.setLength(derBaseLen);
		if(opt.GetCombinedSearching()){
			derivedHtmlBase.append("document.getElementById('fileBtn').onclick();");
		}
		//derivedHtmlBase.append("alert(\"asd\");");
		//derivedHtmlBase.append("$(\"#word\").val(\"Happy\");");
		//CMN.show("key"+key);
		derivedHtmlBase.append("editText.value='"+key+"';");
		//derivedHtmlBase.append("pengDingPos="+pos+";");
		//derivedHtmlBase.append("lastSelection_="+pos+";");
		//derivedHtmlBase.append("pendingDX="+dictionaryIndice+";");
		//derivedHtmlBase.append("pendingDX="+dictionaryIndice+";");
		derivedHtmlBase.append("loookup();");
		CMN.show("pengDingPos"+pos);
		derivedHtmlBase.append(fragments[0]);
		
		
		for(String iI:iframes)
			derivedHtmlBase.append(iI);

		derivedHtmlBase.append(fragments[1]);
		return derivedHtmlBase.toString();
	}
	
	
}

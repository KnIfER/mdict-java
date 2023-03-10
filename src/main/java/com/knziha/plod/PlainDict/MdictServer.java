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

import com.knziha.plod.plaindict.db.FFDB;
import com.knziha.plod.dictionary.Utils.IU;
import com.knziha.plod.dictionary.Utils.SU;
import com.knziha.plod.dictionary.mdBase;
import com.knziha.plod.dictionarymodels.PlainMdict;
import com.knziha.rbtree.RBTree_additive;
import com.knziha.rbtree.additiveMyCpr1;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.knziha.metaline.Metaline;
import org.nanohttpd.protocols.http.IHTTPSession;
import org.nanohttpd.protocols.http.NanoHTTPD;
import org.nanohttpd.protocols.http.response.Response;
import org.nanohttpd.protocols.http.response.Status;
import org.xiph.speex.ByteArrayRandomOutputStream;
import org.xiph.speex.manyclass.JSpeexDec;
import test.CMN;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.nanohttpd.protocols.http.response.Response.newFixedLengthResponse;

//import fi.iki.elonen.NanoHTTPD;
//import fi.iki.elonen.NanoHTTPD.Response.Status;


/**
 * Mdict Server
 * @author KnIfER
 * @date 2018/09/19
 */

public abstract class MdictServer extends NanoHTTPD {
	interface AppOptions {
		boolean isCombinedSearching();
	}
	final Pattern nautyUrlRequest = Pattern.compile("src=(['\"])?(file://)?/");
	final String SepWindows = "\\";
	private final AppOptions opt;

	String baseHtml;
	public ArrayList<PlainMdict> currentFilter = new ArrayList<>();

	protected mdBase MdbResource;
	protected MdictServerLet MdbServerLet;
	private int md_size;

	public MdictServer(int port, AppOptions _opt) {
		super(port);
		opt=_opt;
	}

	@Override
	public Response handle(IHTTPSession session) throws IOException {
		int adapter_idx_ = -1;
		String uri = session.getUri();
		String rawURI = uri;
		SU.Log("serving with honor : ", uri);
		Map<String, String> headerTags = session.getHeaders();
		
		String Acc = headerTags.get("accept");
		if(Acc==null) Acc="";
		String EntrySchema = "text/html";
		boolean isEntryPage = Acc.contains(EntrySchema);
		
		String usr = headerTags.get("user-agent");
		String key = uri.replace("/", SepWindows);
		if(usr==null) return null;
		uri = uri.replaceAll("/{2,}", "");

		/* block:index */
		if(uri.equals("/")) {
			return newFixedLengthResponse(getBaseHtml());
		}
		/* block */
		if(uri.startsWith("/favicon.ico")) {
			return emptyResponse;
		}
		/* block */
		if(uri.startsWith("/MdbR/")) {
			//SU.Log("[fetching internal res : ]", uri);
			//InputStream candi = MdictServer.class.getResourceAsStream("Mdict-browser"+uri);
			InputStream candi = OpenMdbResourceByName(uri.replace("/", "\\"));
			if(candi!=null) {
				String mime="*/*";
				if(uri.contains(".css")) mime = "text/css";
				if(uri.contains(".js")) mime = "text/js";
				try {
					return newFixedLengthResponse(Status.OK,mime,  candi, candi.available());
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			return emptyResponse; 
		}
		/* block */
		if(uri.startsWith("/MIRROR.jsp")) {
			if(om!=null)
				return om.onMirror(session.getQueryParameterString(), true);
			return newFixedLengthResponse(getBaseHtml());
		}
		/* block */
		if(uri.startsWith("/READ.jsp")) {
			if(om!=null) {
				return om.onMirror(session.getQueryParameterString(), false);
			}
			return newFixedLengthResponse(getBaseHtml());
		}

		/* block */
		if(uri.startsWith("/MdbRSingleQuery/")) {
			uri = uri.substring("/MdbRSingleQuery/".length());
			//SU.Log("MdbRSingleQuery: "+uri);
			String[] list = uri.split("/");
			adapter_idx_ = Integer.parseInt(list[0]);
			SU.Log("/MdbRSingleQuery/:",uri.substring(list[0].length()+1),md_get(adapter_idx_).lookUp(Reroute(uri.substring(list[0].length()+1))));
			return newFixedLengthResponse(Integer.toString(md_get(adapter_idx_).lookUp(Reroute(uri.substring(list[0].length()+1)),false)));
		}
		/* block */
		else if(uri.startsWith("/MdbRJointQuery/")) {
			uri = Reroute(uri.substring("/MdbRJointQuery/".length()));
			//SU.Log("MdbRJointQuery: "+uri);
			RBTree_additive combining_search_tree_ = new RBTree_additive();
			StringBuilder sb_ = new StringBuilder();
			for(int i=0;i<md_size();i++){
				try {
					md_get(i).size_confined_lookUp5(uri,combining_search_tree_,i,30);
				} catch (Exception e) {
					SU.Log(md_getName(i), e);
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
			SU.Log(sb_.toString());
			return newFixedLengthResponse(sb_.toString());
		}
		

		String[] list = null;

		if(key.equals("\\DB.jsp")) {
			return FFDB.handleRequest(session);
		}
		
		/* block::internal */
		if(key.equals("\\dicts.json")) {
			if(md_size()>0) {
				StringBuilder sb = new StringBuilder();
				for (int i = 0; i < md_size; i++) {
					PlainMdict mdx = md_get(i);
					sb.append(mdx==null?"null":mdx.getDictionaryName());

					//sb.append(md_getName(i));
					//if(mdx.hasVirtualIndex())
					//	sb.append(":VI&VI");
					sb.append("\n");
				}
				sb.setLength(sb.length()-1);
				return newFixedLengthResponse(sb.toString()) ;
			}
			return emptyResponse;
		}
		/* block:internal */
		else if(key.startsWith("\\MdbRSize\\")) {
			if(md_size()>0) {
				key = key.substring(10);
				try {
					//key = URLDecoder.decode(key, "UTF-8");
				} catch (Exception e) {
					e.printStackTrace();
				}
				long ret=0;
				for (int i = 0; i < md_size; i++) {
					PlainMdict mdx = md_get(i);
					if(mdx!=null && mdx.getDictionaryName().equals(key))
						ret=mdx.getNumberEntries();
				}
				SU.Log("MdbRSize ret: "+ret+key);
				return newFixedLengthResponse(ret+"") ;
			}
			return emptyResponse;
		}
		/* block:internal */
		else if(key.startsWith("\\MdbRGetEntries\\")) {
			if(md_size()>0) {
				key = key.substring(16);
				//SU.Log(key);
				list = key.split("\\\\");
				StringBuilder ret= new StringBuilder();
				for (int i = 0; i < md_size; i++) {
					PlainMdict mdx = md_get(i);
					if(mdx._Dictionary_fName.equals(list[0])) {
						StringBuilder sb = new StringBuilder();
						//SU.Log("capacity "+l[2]);
						int capacity=Integer.parseInt(list[2]);
						int base = Integer.parseInt(list[1]);
						for(int j=0;j<capacity;j++) {
							ret.append(mdx.getEntryAt(base + j));
							if(j<capacity-1)
								ret.append("\n");
							//sb.append(mdx.getEntryAt(base+i)).append("\n");
						}
						//sb.setLength(sb.length()-1);
						//ret = sb.toString();
						break;
					}
				}

				return newFixedLengthResponse(ret.toString()) ;
			}
			return emptyResponse;
		}

		if(isEntryPage) {
			/* ::internal */
			if(uri.startsWith("/about/")) {
				//SU.Log("about received : ", uri);
				try {
					PlainMdict mdTmp = md_get(Integer.parseInt(uri.substring(7)));
					return newFixedLengthResponse(mdTmp.getAboutHtml());
				} catch (Exception ignored) { }
			}

			/* ::internal*/
			if(uri.startsWith("/content/")) {
				SU.Log("content received : ", uri); //  /content/1@6
				list = uri.substring(9).split("@");
				int length=list.length;
				if(length>1) {
					if(!list[0].equals("")) {
						String lit=list[list.length - 1];
						int lid = lit.lastIndexOf(":");
						if(lid!=-1){
							list[length - 1]=lit.substring(0,lid);
							lid=IU.parsint(lit.substring(lid+1), -1);
						}
						try {
							adapter_idx_ = Integer.parseInt(list[0]);
							PlainMdict mdTmp = md_get(adapter_idx_);
							int[] list2 = new int[length-1];
							for(int i=0;i<length-1;i++)
								list2[i]=Integer.parseInt(list[i+1]);
							return newFixedLengthResponse(constructMdPage(mdTmp, adapter_idx_,lid!=-1?mdTmp.getVirtualRecordsAt(list2):mdTmp.getRecordsAt(list2), true));
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
					return newFixedLengthResponse(constructMdPage(md_get(adapter_idx_), 0,"<div>ERROR FETCHING CONTENT:"+uri+"</div>", true));
				}
			}
		}

		/* block:application */
		if(uri.startsWith("/PLOD/")) {
			//SU.Log("about received : ", uri);
			handle_search_event(uri.substring(6), session);
			return emptyResponse;
		}
		////////////////////////////////////////////////////////
		boolean ReceiveText=Acc.contains("text/html");
		boolean IsCustomer=!usr.contains("Java");
		String EntryLookUp = null;
		String DIDNAMEACCESS=null;
		/* block::browser ( base schema ) */
		if(uri.startsWith("/base")) {
			if(uri.length()==5) {
				return baseGuildResponse();
			} else if(uri.charAt(5)=='/') {
				int split = uri.indexOf("/", 6);
				if(isEntryPage && split<0) { // 重定向
					if(uri.length()==6) {
						return baseGuildResponse();
					}
					return Redirect(rawURI+"/");
				}
				adapter_idx_ = IU.parsint(uri.substring(6, split));
				uri = key = uri.substring(split+1);
				if(adapter_idx_<0||adapter_idx_>md_size()) {
					return baseGuildResponse();
				}
			}
		}
		
		/* retrieve dictionary id for name */
		if(adapter_idx_<0) {
			int start = 1;
			int split = uri.indexOf('/', start);
			if(split>0) {
				DIDNAMEACCESS = uri.substring(start, split);
				uri = key = uri.substring(split+1);
				if(key.length()==0) {
					if(DIDNAMEACCESS.equals("ALLOYD")) {
						String json = "{\"OED2\":{\"host\":\"http://127.0.0.1:8080/OED2/%s.json\", \"title\":\"牛津英语词典(来自mdx伺服)\"}}";
						//json = StringEscapeUtils.escapeJson(json);
						ByteArrayInputStream bin = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
						return newFixedLengthResponse(Status.OK, "application/json", bin, bin.available());
					}
				}
				adapter_idx_ = MdbServerLet.md_get(DIDNAMEACCESS);
			} else { // 重定向
				return Redirect(rawURI+"/");
			}
		}
		
		if(adapter_idx_<0||adapter_idx_>md_size()) {
			return emptyResponse;
		}

		PlainMdict mdTmp = md_get(adapter_idx_);
		
		final String entry="/entry/";
		final String raw="/raw/";
		boolean b1;
		/* block::after X Deprecated*/
		if((b1=uri.startsWith(entry)) || uri.startsWith(raw))  {
			try {
				key=key.substring(b1?entry.length():raw.length());
				if(key.contains("#")){
					key=key.substring(key.indexOf("#"));
				}
				if(key.endsWith("\\"))
					key=key.substring(0, key.length()-1);
				SU.Log("jumping...", key);
				String res = mdTmp.getRecordsAt(mdTmp.lookUp(key));
				return newFixedLengthResponse(constructMdPage(mdTmp,
						DIDNAMEACCESS!=null?DIDNAMEACCESS:adapter_idx_, res, DIDNAMEACCESS==null&&b1));
			} catch (IOException e) {
				e.printStackTrace();
			}
			return emptyResponse;
		}


//		if(uri.startsWith("/sound/")) {
//			key=uri.substring(6).replace("/","\\");
//		}

		try {
			if(uri.toLowerCase().endsWith(".js")) {
				//SU.Log("candi",uri);
				File candi = new File(new File(mdTmp.getPath()).getParentFile(),new File(uri).getName());
				if(candi.exists())
					return newFixedLengthResponse(Status.OK,"application/x-javascript",  new FileInputStream(candi), candi.length());
			}
			else if(uri.toLowerCase().endsWith(".css")) {
				File candi = new File(new File(mdTmp.getPath()).getParentFile(),new File(uri).getName());
				SU.Log("candi_css",uri,candi.getAbsolutePath(), candi.exists());
				if(candi.exists()) {
					return newFixedLengthResponse(Status.OK,"text/css",  new FileInputStream(candi), candi.length());
				}
			}
		} 
		catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		InputStream restmp;

		key = PlainMdict.requestPattern.matcher(key).replaceAll("");
		
		/* block::browser (name schema) */
		if(DIDNAMEACCESS!=null) {
			CMN.Log("DIDNAMEACCESS_Acc", Acc);

			if(key.endsWith("favicon.png")) {
				CMN.Log("//todo /favicon.ico", DIDNAMEACCESS);
				File iconFile = new File(mdTmp.f().getParentFile(), DIDNAMEACCESS+".png");
				if(iconFile.exists()) {
					return newFixedLengthResponse(Status.OK, "image/*", new FileInputStream(iconFile), iconFile.length());
				}
			}
			
			if(key.endsWith(".json")) {
				key = key.substring(0, key.length()-5);
				EntrySchema = "application/x-javascript";
				isEntryPage = true;
				if(key.equals("update")) {
					return newFixedLengthResponse(Status.OK, "text/plain", mdTmp.checkForUpdate()?"update":"");
				}
			}
			
			if(isEntryPage) {
				EntryLookUp = key;
			}
			//todo 
		}

		/* 获取内容 (Get Contents for Browser)*/
		/* (by entry id or by entry key) */
		if(isEntryPage) {
			int directPageLoc=-1;
			if(uri.length()==0) {
				directPageLoc=0;
			} else {
				list = uri.split("/");
				if(list.length>0 && isPageToken(list[0])) {
					directPageLoc = IU.parsint(list[0].substring(2),-1);
				}
			}
			key = uri.replace("/", SepWindows);
			if(directPageLoc>=0){
				if(uri.endsWith("/")) { // 重定向
					return Redirect(rawURI.substring(0, rawURI.length()-1));
				}
				try {
					return newFixedLengthResponse(mdTmp.getRecordsAt(directPageLoc));
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			//@Deprecated
			else if(list.length>=1) {
				if(list[0].equals("@@@")) {//  /base/0/@@@/name
					key = uri.substring(list[0].length()+1+3);
					SU.Log("rerouting..."+key);
					try {
						return newFixedLengthResponse(mdTmp.getRecordsAt(mdTmp.lookUp(key)));
					} catch (IOException e) {
						e.printStackTrace();
					}
					return null;
				}
				else if(list[0].equals("VI") && mdTmp.hasVirtualIndex()) {//  /base/0/VI/0
					try {
						int VI = IU.parsint(list[1],-1);
						//SU.Log("virtual content..."+VI, mdTmp.getVirtualRecordAt(VI));
						return newFixedLengthResponse(mdTmp.getVirtualRecordAt(VI));
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}

			if(list.length==1) {
				if(uri.endsWith("/")) { // 重定向
					return Redirect(rawURI.substring(0, rawURI.length()-1));
				}
				int EntryID = mdTmp.lookUp(key, true);
				if(EntryID>=0) {
					if(mdTmp.hasStyleSheets()) {
						String data = mdTmp.getRecordsAt(EntryID);
						//data = StringEscapeUtils.unescapeHtml4(data);
						CMN.Log("返回data");

						restmp = new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
						return newFixedLengthResponse(Status.OK, NanoHTTPD.MIME_HTML, constructMdPage(mdTmp, DIDNAMEACCESS, data, false));
						//return newFixedLengthResponse(Status.OK, EntrySchema, restmp, restmp.available());
					} else {
						SU.Log("直接构造 字节！");
						InputStream[] input = new InputStream[]{
								mdTmp.getRecordDataStream(EntryID),
								OpenMdbResourceByName("\\MdbR\\simplepage.html"),
								new ByteArrayInputStream(("\\"+DIDNAMEACCESS+"\\").getBytes(StandardCharsets.UTF_8))
						};
						Vector<InputStream> streams = new Vector<>();
						streams.addAll(Arrays.asList(input));
						SequenceInputStream ret = new SequenceInputStream(streams.elements());
						return newFixedLengthResponse(Status.OK, "text/html; charset="+mdTmp.getCharset(), ret, ret.available());
					}
				}
			}
		}
		
		key = key.replace("/", SepWindows);
		if(!key.startsWith(SepWindows)) {
			key = SepWindows+key;
		}
		restmp = mdTmp.getResourceByKey(key);
		
		SU.Log("-----> /dictionary/", adapter_idx_, mdTmp._Dictionary_fName, mdTmp.hasMdd(), key, restmp==null?-1:restmp.available(), Acc);

		if(restmp==null) {
			return emptyResponse;
		}

		if(Acc.contains("javascript/") || uri.endsWith(".js")) {
			return newFixedLengthResponse(Status.OK,"application/x-javascript", restmp, restmp.available());
		}

		if(Acc.contains("text/html")) {
			return newFixedLengthResponse(Status.OK,"text/html", restmp, restmp.available());
		}
		
		if(Acc.contains("/css") || uri.endsWith(".css")) {
			return newFixedLengthResponse(Status.OK,"text/css", restmp, restmp.available());
		}

		if(Acc.contains("/mp3") || uri.endsWith(".mp3")) {
			//SU.Log("mp3 : "+uri);
			return newFixedLengthResponse(Status.OK,"audio/mpeg", restmp, restmp.available());
		}

		if(uri.contains(".pdf")) {
			return newFixedLengthResponse(Status.OK,"application/pdf", restmp, restmp.available());
		}

		if(uri.endsWith(".spx")) {
			//SU.Log("spx : "+uri);
			ByteArrayRandomOutputStream bos = new ByteArrayRandomOutputStream();
			JSpeexDec decoder = new JSpeexDec();
			try {
				decoder.decode(new DataInputStream(restmp) , bos, JSpeexDec.FILE_FORMAT_WAVE);
				return newFixedLengthResponse(Status.OK,"text/x-wav", new ByteArrayInputStream(bos.toByteArray()) , bos.size());
			} catch (Exception e) { e.printStackTrace(); }
			return newFixedLengthResponse(Status.OK,"audio/mpeg", restmp, restmp.available());
		}

		if(Acc.contains("image/") ) {
			SU.Log("Image request : ",Acc,key,mdTmp._Dictionary_fName);
			if(uri.endsWith(".tif")||uri.endsWith(".tiff"))
				try {
					restmp = convert_tiff_img(restmp);
					//CMN.pt("再编码耗时 : ");
				} catch (Exception e) { e.printStackTrace(); }

			return newFixedLengthResponse(Status.OK,(IsCustomer&&ReceiveText)?"text/plain":"image/*", restmp, restmp.available());
		}

		return newFixedLengthResponse(Status.OK,"*/*", restmp, restmp.available());
	}

	private Response Redirect(String location) {
		Response ret = newFixedLengthResponse(Status.REDIRECT, NanoHTTPD.MIME_HTML, "");
		ret.addHeader("Location", location);
		return ret;
	}

	private Response baseGuildResponse() {
		return newFixedLengthResponse(Status.ACCEPTED,"text/html", baseGuildResponseByteArr);
	}

	private boolean isPageToken(String arg1) {
		if(arg1.length()>2) {
			char c1=arg1.charAt(1);
			return (c1=='_'||c1=='-')&&((c1=arg1.charAt(0))=='P'||c1=='p');
		}
		return false;
	}

	protected abstract InputStream convert_tiff_img(InputStream restmp) throws Exception;

	protected abstract void handle_search_event(String text, IHTTPSession session);

	private String md_getName(int pos) {
		return MdbServerLet.md_getName(pos);
	}

	private PlainMdict md_get(int pos) {
		return MdbServerLet.md_get(pos);
	}

	private int md_size() {
		return md_size=MdbServerLet.md_getSize();
	}

	protected InputStream OpenMdbResourceByName(String key) throws IOException {
		InputStream ret = null;
		if(MdbResource instanceof com.knziha.plod.dictionary.mdict) {
			ret = ((com.knziha.plod.dictionary.mdict)MdbResource).getResourceByKey(key);
		} else {
			int id = MdbResource.lookUp(key);
			if(id>=0) {
				ret = MdbResource.getResourseAt(id);
			}
		}
		return ret;
	}

	private String Reroute(String currentText) {
		SU.Log(currentFilter.size(), "Reroute", currentText);
		try {
			for (PlainMdict mdTmp:currentFilter) {
				Object rerouteTarget = mdTmp.ReRoute(currentText);
				if(rerouteTarget instanceof String){
					String text = (String) rerouteTarget;
					SU.Log("Reroute",mdTmp._Dictionary_fName, text);
					if(text.trim().length()>0){
						currentText=text;
						break;
					}
				}
			}
		} catch (IOException ignored) { }
		return currentText;
	}

	/**
	 <script>
	 var postInit;
	 if(false)
	 if(window.addEventListener){
		 window.addEventListener('load',wrappedOnLoadFunc,false);
		 window.addEventListener('click',wrappedClickFunc);
	 }else if(window.attachEvent){ //ie
		 window.addEventListener('onload',wrappedOnLoadFunc);
		 window.addEventListener('onclick',wrappedClickFunc);
	 }else{
		 window.onload=wrappedOnLoadFunction;
		 window.onclick=wrappedClickFunc;
	 }
	 function wrappedOnLoadFunc(){
		 document.getElementById('view1').setAttribute('content', 'width=device-width, initial-scale=1, maximum-scale=1.0, user-scalable=no');
		 if(postInit) postInit();
	 }

	 var audio;
	 var regHttp=new RegExp("^https?://");
	 var regEntry=new RegExp("^entry://");
	 var regPdf=new RegExp("^pdf://");
	 var regSound=new RegExp("^sound://");

	 function hiPlaySound(e){
		 var cur=e.ur1?e:e.srcElement;
		 //console.log("hijacked sound playing : "+cur.ur1);
		 if(audio)
		 audio.pause();
		 else
		 audio = new Audio();
		 audio.src = cur.ur1;
		 audio.play();
	 }

	 function loadVI(pos){
		 console.log('loadVI/'+pos);
		 var req=new XMLHttpRequest();
		 req.open('GET','/VI/'+pos);
		 req.responseType='json';
		 req.onreadystatechange=function(e) {
		 if(req.readyState == 4 && req.status==200) {
		 console.log(req.responseText);
	 }
	 }
	 }

	 function wrappedClickFunc(e){
		 var cur=e.srcElement;
		 if(cur.href){
			 //console.log("1! found link : "+cur.href+" : "+regSound.test(cur.href));
			 if(regEntry.test(cur.href))
			 cur.href="entry/"+cur.href.substring(8);
			 else if(regSound.test(cur.href)){//拦截 sound 连接
				 var link="sound/"+cur.href.substring(8);
				 cur.href=link;
				 if(cur.onclick==undefined){
					 //console.log("1! found internal sound link : "+cur.href);
					 cur.ur1=cur.href;
					 cur.removeAttribute("href");
					 cur.onclick=hiPlaySound;
					 hiPlaySound(cur);
					 return false;
				 }
			 }
		 }
		 else if(cur.src && regEntry.test(cur.src)){
			 console.log("2! found link : "+cur.src);
			 return false;
		 }
		 else if(false && e.srcElement!=document.documentElement){ // immunize blank area out of body ( in html )
			 //console.log(e.srcElement+'')
			 //console.log(e)
			 var s = window.getSelection();
			 if(s.isCollapsed && s.anchorNode){ // don't bother with user selection
				 s.modify('extend', 'forward', 'word'); // first attempt
				 var an=s.anchorNode;
				 //console.log(s.anchorNode); console.log(s);
				 //if(true) return;
			
				 if(s.baseNode != document.body) {// immunize blank area
					 var text=s.toString(); // for word made up of just one character
					 var range = s.getRangeAt(0);
				
					 s.collapseToStart();
					 s.modify('extend', 'forward', 'lineboundary');
				
					 if(s.toString().length>=text.length){
						 s.empty();
						 s.addRange(range);
				
						 s.modify('move', 'backward', 'word'); // could be next line
						 s.modify('extend', 'forward', 'word');
					
						 if(s.getRangeAt(0).endContainer===range.endContainer&&s.getRangeAt(0).endOffset===range.endOffset){
							 // 字未央
							 text=s.toString();
						 }
				
						console.log(text); // final output
					}
				 }
				 //s.collapseToStart();
				 return false;
			 }
		 }
		 return true;
	 };
	 </script>

	 <base href='/base/*/
	@Metaline(trim = false)
	public static String SimplestInjection="ERROR!!!";
	/** /'/>
	 <base target="_self" />
	 */
	@Metaline(trim=false)
	String SimplestInjectionEnd=StringUtils.EMPTY;

	int MdPageBaseLen=-1;
	String MdPage_fragment1,MdPage_fragment2, MdPage_fragment3="</html>";
	int MdPageLength=0;
	private String constructMdPage(PlainMdict mdTmp, Object dictIdx, String record, boolean b1) {
		if(b1 && PlainMdict.fullpagePattern.matcher(record).find())
			b1=false;
		CMN.Log("constructMdPage 1", b1);
		//b1=true;
		if(b1) {
			StringBuilder MdPageBuilder = new StringBuilder(MdPageLength + record.length() + 5);
			String MdPage_fragment1 = null, MdPage_fragment2 = null, MdPage_fragment3 = "</html>";
			int MdPageBaseLen = -1; //rrr
			if (MdPageBaseLen == -1) {
				try {
					BufferedReader in = new BufferedReader(new InputStreamReader(OpenMdbResourceByName("\\MdbR\\subpage.html")));
					String line;
					while ((line = in.readLine()) != null) {
						MdPageBuilder.append(line).append("\r\n");
						if (MdPage_fragment1 == null) {
							if (line.equals("<base href='/base//'/>")) {
								MdPageBuilder.setLength(MdPageBuilder.length() - 6);
								MdPage_fragment1 = MdPageBuilder.toString();
								MdPageBuilder.setLength(0);
								MdPageBuilder.append("/'/>\r\n");
							}
						}
					}
					MdPage_fragment2 = MdPageBuilder.toString();
					MdPageBuilder.setLength(0);
					MdPageLength = MdPage_fragment1.length() + MdPage_fragment2.length() + MdPage_fragment3.length();
					MdPageBaseLen = MdPage_fragment1.length();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			MdPageBuilder.append(MdPage_fragment1);

			//作为建议，mdd资源请求应使用相对路径以减少处理过程。
			record = nautyUrlRequest.matcher(record).replaceAll("src=$1");

			MdPageBuilder
					.append(dictIdx)// "/base/0"
					.append(MdPage_fragment2)
					.append("<link rel='stylesheet' type='text/css' href='").append(mdTmp._Dictionary_fName).append(".css'/>")
					.append("</head>")
					.append(record)
					.append(MdPage_fragment3)
			;
			MdPageBuilder.append("<div class=\"_PDict\" style='display:none;'><p class='bd_body'/>");
			if(mdTmp.hasMdd()) MdPageBuilder.append("<p class='MddExist'/>");
			MdPageBuilder.append("</div>");
			return MdPageBuilder.toString();
		}
		else if(dictIdx instanceof Integer){
			if(true){
				int idx = record.indexOf("<head>");
				StringBuilder MdPageBuilder = new StringBuilder(MdPageLength + record.length() + 5);
				String start = idx==-1?"<head>":record.substring(0, idx+6);
				String end = idx==-1?record:record.substring(idx+6);

				MdPageBuilder
						.append(start)
						.append(SimplestInjection)
						.append(dictIdx)// "/base/0"
						.append(SimplestInjectionEnd)
						.append(idx==-1?"</head>":"")
						.append(end);
				return MdPageBuilder.toString();
			}
			return record;
		} else {
			String result=null;
			try {
				result = new BufferedReader(new InputStreamReader(OpenMdbResourceByName("\\MdbR\\simplepage.html")))
						.lines().collect(Collectors.joining("\n"));
			} catch (IOException e) {
				e.printStackTrace();
			}

			StringBuilder MdPageBuilder = new StringBuilder(MdPageLength + record.length() + 5);
			MdPageBuilder
					.append(record)
					.append(result)
					.append(dictIdx)
					.append("/'/>");
			return MdPageBuilder.toString();
		}
	}
	public static Response emptyResponse = newFixedLengthResponse(Status.NOT_FOUND,"*/*", "");
	
	/**<html lang="en" style="">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width,user-scalable=no,initial-scale=1,maximum-scale=1,minimum-scale=1">
<style>
h1, h2, h3, h4, h5, h6 {
    text-rendering: optimizelegibility;
    line-height: 1.7;
    margin: 0 0 15px;
	font-weight: 600;
	color:#0f0083;
}
h1 {
    font-size: 26px;
}
h3 {
    font-size: 22px;
}
quotation {
    display: block;
    padding: 20px;
    background-color: #f2f2f1;
    border-left: 6px solid #b3b3b3;
    word-break: break-word;
    font-size: 16px;
    font-weight: 400;
    line-height: 30px;
    margin: 0 0 20px;
    margin-block-start: 1em;
    margin-block-end: 1em;
    margin-inline-start: 40px;
    margin-inline-end: 40px;
}
a {
    color: #3194d0;
    text-decoration: none;
    background-color: transparent;
    outline: none;
    cursor: pointer;
    -webkit-transition: color .3s;
    -o-transition: color .3s;
    transition: color .3s;
    -webkit-text-decoration-skip: objects;
}
html {
    color: #333;
    background-color: #fcfaf2;
    padding: 40px 40px 80px;
    font-size: 16px;
    line-height: 1.75;
}
center {
	color:#362a95;
}
</style>
</head>
<body>
<h1 class="line" data-line="0">基础资源定位符</h1>
<quotation>
<p>Base Url 用于直接查询特定的词典，或者访问指定位置的页面。</p>
</quotation>
<h3 class="line" data-line="4">一、可接受的 Base Url：</h3>
<ul>
<li><p><a href="./0/@@@/keyword" target="_blank">http://host/base/0/@@@/keyword</a></p></li>
<li><p><a href="./0/P_0" target="_blank">http://host/base/0/P_0</a></p></li>
<li><p><a href="./0/" target="_blank">http://host/base/0/…</a></p></li>
</ul>
<h3 class="line" data-line="13">二、其中，紧跟 <code>/base/</code> 的第一个参数为词典ID（列表中第几个词典），第二个参数分三种情况：</h3>
<ul>
<li><p>@@@：查询关键词</p></li>
<li><p>P_0：直接访问第一个页面</p></li>
<li><p>/：资源文件</p></li>
</ul>
<br/><br/><br/>
<center>-&nbsp;平典服务器 2020/07/03&nbsp;-</center>
</body></html>
	 */
	@Metaline
	byte[] baseGuildResponseByteArr = ArrayUtils.EMPTY_BYTE_ARRAY;
	
	interface OnMirrorRequestListener{
		public Response onMirror(String uri, boolean mirror);
	}OnMirrorRequestListener om;

	public void setOnMirrorRequestListener(OnMirrorRequestListener om_) {
		om = om_;
	}




	int derBaseLen = -1;
	String restFragments;

	public String constructDerivedHtml(String key,int pos,int dictionaryIndice,String iframes) {
		StringBuilder derivedHtmlBase;
		if(true || derBaseLen == -1) {//rrr
			String insertsionPoint = "var postInit = function(){";
			String baseHtml = getBaseHtml();
			int idx1=baseHtml.indexOf(insertsionPoint);
			//int idx2=baseHtml.indexOf("onscroll='dismiss_menu();'>",idx1);
			String f1 = baseHtml.substring(0,idx1+insertsionPoint.length()+1);
			//String f2 = baseHtml.substring(baseHtml.indexOf("}",idx1+insertsionPoint.length()),idx2);
			//String f3 = baseHtml.substring(baseHtml.indexOf("</div>",idx2));
			restFragments = baseHtml.substring(baseHtml.indexOf("/**/}",idx1+insertsionPoint.length()));
			derivedHtmlBase = new StringBuilder(f1);
			//SU.Log("f1"+f1);
			derBaseLen = f1.length();
		}
		derivedHtmlBase.setLength(derBaseLen);
		if(opt.isCombinedSearching()){
			derivedHtmlBase.append("document.getElementById('fileBtn').onclick();");
		}
		/** win10 ie 去掉\t会发生量子波动Bug */
		derivedHtmlBase.append("\teditText.value=\"").append(key.replace("\"", "\\\"")).append("\";");
		derivedHtmlBase.append("loookup();");
		if(iframes!=null){
			derivedHtmlBase.append("handleMirror('"+iframes.replace("\r","\\r")+"');");
		}
		//SU.Log("pengDingPos"+pos);
		derivedHtmlBase.append(restFragments);
		//SU.Log(derivedHtmlBase.toString());
		return derivedHtmlBase.toString();
	}

	private String getBaseHtml() {
		if(true || baseHtml==null) {//rrr
			try {
				SU.Log("iiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiii");
				SU.Log(MdictServer.SimplestInjection);
				InputStream fin = OpenMdbResourceByName("\\mdict_browser.html");
				byte[] data = new byte[fin.available()];
				fin.read(data);
				baseHtml = new String(data);
				fin.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return baseHtml;
	}
}
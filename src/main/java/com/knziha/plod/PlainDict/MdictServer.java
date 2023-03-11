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

import static org.nanohttpd.protocols.http.response.Response.newChunkedResponse;
import static org.nanohttpd.protocols.http.response.Response.newFixedLengthResponse;
import static org.nanohttpd.protocols.http.response.Response.newRecyclableResponse;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.knziha.plod.dictionary.UniversalDictionaryInterface;
import com.knziha.plod.dictionary.Utils.BU;
import com.knziha.plod.dictionary.Utils.IU;
import com.knziha.plod.dictionary.Utils.SU;
import com.knziha.plod.dictionary.mdBase;
import com.knziha.plod.dictionary.mdict;
import com.knziha.plod.dictionarymodels.BookPresenter;
import com.knziha.plod.dictionarymodels.PlainWeb;
import com.knziha.rbtree.RBTree_additive;
import com.knziha.rbtree.additiveMyCpr1;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.knziha.metaline.Metaline;
import org.nanohttpd.protocols.http.HTTPSession;
import org.nanohttpd.protocols.http.NanoHTTPD;
import org.nanohttpd.protocols.http.request.Method;
import org.nanohttpd.protocols.http.response.Response;
import org.nanohttpd.protocols.http.response.Status;
import org.xiph.speex.ByteArrayRandomOutputStream;
import org.xiph.speex.manyclass.JSpeexDec;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

//import fi.iki.elonen.NanoHTTPD;
//import fi.iki.elonen.NanoHTTPD.Response.Status;


/**
 * Mdict Server
 * @author KnIfER
 * @date 2018/09/19
 */

public abstract class MdictServer extends NanoHTTPD {
	protected MainActivityUIBase app;
	/** strip bad design urls.
	  Should always use relative path while packaging mdx! */
	final Pattern nautyUrlRequest = Pattern.compile("src=(['\"]?)((file://)|(file:/))?/");
	final String SepWindows = "\\";
	
	public static boolean hasRemoteDebugServer = BuildConfig.isDebug;
	
	String baseHtml;
	public ArrayList<BookPresenter> currentFilter = new ArrayList<>();
	
	protected mdBase MdbResource;
	
	PlainWeb webResHandler;
	
	public MainActivityUIBase.LoadManager loadManager;
	
	public MdictServer(int port, MainActivityUIBase app) {
		super(port);
		this.app = app;
		loadManager = app.loadManager;
	}
	
	@Override
	public Response handle(HTTPSession session) throws IOException {
		BookPresenter presenter = null;
		String uri = session.getUri();
		//SU.Log("serving with honor : ", uri);
		Map<String, String> headerTags = session.getHeaders();
		String Acc = headerTags.get("accept");
		if(Acc==null) Acc= StringUtils.EMPTY;
		String usr = headerTags.get("user-agent");
		String key = uri.replace("/", SepWindows);
		if(usr==null) return null;
		
		if(uri.startsWith("/MdbR/")) {
			CMN.debug("[fetching internal res : ]", uri);
			//InputStream candi = MdictServer.class.getResourceAsStream("Mdict-browser"+uri);
			InputStream candi = OpenMdbResourceByName(uri.replace("/", "\\"));
			if(candi!=null) {
				String mime="*/*";
				if(uri.contains(".css")) mime = "text/css";
				if(uri.contains(".js")) mime = "text/js";
				if(uri.contains(".html")) mime = "text/html";
				try {
					return newChunkedResponse(Status.OK,mime,  candi);
					//return newFixedLengthResponse(Status.OK,mime,  candi, candi.available());
				} catch (Exception e) {
					CMN.debug(e);
				}
			}
		}
		if(uri.startsWith("/mirror.jsp")) {
//			if(om!=null)
//				return om.onMirror(session.getQueryParameterString(), true);
			return newFixedLengthResponse(getBaseHtml());
		}
		if(uri.startsWith("/read.jsp")) {
//			if(om!=null) {
//				return om.onMirror(session.getQueryParameterString(), false);
//			}
			return newFixedLengthResponse(getBaseHtml());
		}
		if(uri.startsWith("/merge.jsp")) {
			return getMergedBaseResponse(session.isProxy);
		}
		
		if(uri.startsWith("/MdbRSingleQuery/")) {
			int tmp = uri.indexOf("/", 17);
			BookPresenter book = md_getByURLPath(uri, 17, tmp);
			key=uri.substring(tmp);
			int pos=book.bookImpl.lookUp(Reroute(key));
			return newFixedLengthResponse(pos+"_"+book.bookImpl.getEntryAt(pos));
		}
		else if(uri.startsWith("/MdbRJointQuery/")) {
			uri = Reroute(uri.substring("/MdbRJointQuery/".length()));
			//SU.Log("MdbRJointQuery: "+uri);
			RBTree_additive treeBuilder = new RBTree_additive();
			StringBuilder sb_ = new StringBuilder();
			UniversalDictionaryInterface book;
			for(int i=0;i<loadManager.md_size;i++){
				try {
					book = loadManager.md_get(i).bookImpl;
					book.lookUpRange(uri, null, treeBuilder, book.getBooKID(), 30, null, false);
				} catch (Exception e) {
					SU.Log(loadManager.md_getName(i, -1), e);
				}
			}
			ArrayList<additiveMyCpr1> resultList = treeBuilder.flatten();
			for(int i=0;i<resultList.size();i++) {
				additiveMyCpr1 resI = resultList.get(i);
				sb_.append(resI.key).append("\r");
				ArrayList<Long> values = (ArrayList<Long>) resI.value;
				long bookId=-1;
				for(int ii=0;ii<values.size();ii+=2) {
					long currIdx=values.get(ii);
					if(currIdx!=bookId) {
						if(bookId!=-1) sb_.append("-");
						sb_.append("d");
						IU.NumberToText_SIXTWO_LE(currIdx, sb_);
					}
					bookId = currIdx;
					sb_.append("_");
					IU.NumberToText_SIXTWO_LE(values.get(ii+1), sb_);
				}
				if(i!=resultList.size()-1)
					sb_.append("\n");
			}
			//SU.Log(sb_.toString());
			return newFixedLengthResponse(sb_.toString());
		}
		////////////////////////////////////////////////////////
		boolean ReceiveText=Acc.contains("text/html");
		boolean IsCustomer=!usr.contains("Java");
		if(uri.startsWith("/base/")) {
			//SU.Log("requesting_frame::", uri);
			uri = uri.substring("/base/".length());
			String[] list = uri.split("/");
			String dn=list[0];
			presenter = md_getByURL(dn);
			//SU.Log("requesting_frame::presenter::", dn, presenter);
			uri = uri.substring(dn.length());
			key = uri.replace("/", SepWindows);
			if(list.length==1){
				try {
					int index = presenter.bookImpl.lookUp("index");
					return newFixedLengthResponse(presenter.bookImpl.getRecordsAt(null, index>=0?index:0));
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			else if(list[1].equals("@@@")) {//  /base/0/@@@/name
				key = uri.substring(dn.length()+1+3);
				SU.Log("rerouting..."+key);
				try {
					return newFixedLengthResponse(presenter.bookImpl.getRecordsAt(null, presenter.bookImpl.lookUp(key)));
				} catch (IOException e) {
					e.printStackTrace();
				}
				return null;
			}
			else if(list[1].equals("VI") && presenter.bookImpl.hasVirtualIndex()) {//  /base/0/VI/0
				try {
					int VI = IU.parsint(list[2],-1);
					//SU.Log("virtual content..."+VI, mdTmp.getVirtualRecordAt(VI));
					return newFixedLengthResponse(presenter.bookImpl.getVirtualRecordAt(this, VI));
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		//else SU.Log("!!! !!! !!!", uri);
		
		final String entry="/entry/";
		final String raw="/raw/";
		boolean b1 ,b2;
		if((b1=uri.startsWith(entry)) || uri.startsWith(raw))  {
			try {
				key=key.substring(b1?entry.length():raw.length());
				if(key.contains("#")){
					key=key.substring(0, key.indexOf("#"));
				}
				if(key.endsWith("\\"))
					key=key.substring(0, key.length()-1);
				if(key.contains("%"))
					key = URLDecoder.decode(key);
				SU.Log("jumping...", key);
				int pos = presenter.bookImpl.lookUp(key);
				String res = presenter.bookImpl.getRecordsAt(null, pos);
				return newFixedLengthResponse(constructMdPage(presenter, res, b1, pos, session));
			} catch (Exception e) {
				SU.Log(e);
			}
			return emptyResponse;
		}
		
		if(key.equals("\\DB.jsp"))
			return app.handleFFDB(session);
		
		if(key.equals("\\settings.json")) // todo deprecate, use jsBridge?
			return newFixedLengthResponse(app.getWebSettings()) ;
		
		if(key.equals("\\decodeExp.txt"))
			return app.decodeExp(session);
		
		if(key.equals("\\wordmap.json")) {
//			if (Method.POST.equals(session.getMethod()))
			{
				try {
//					session.parseBody(null);
//					List<String> ids = Arrays.asList(session.getParameters().get("ids").get(0).split(","));
					//SU.Log("dicts.json::", ids);
					return newFixedLengthResponse(app.handleWordMap()) ;
				} catch (Exception e) {
					return emptyResponse;
				}
			}
		}
		
		if(key.equals("\\dicts.json")) {
			SU.Log("dicts.json::", session.getParameters(), session.getMethod());
			if (Method.POST.equals(session.getMethod())) {
				try {
					session.parseBody(null);
					List<String> ids = Arrays.asList(session.getParameters().get("ids").get(0).split(","));
					//SU.Log("dicts.json::", ids);
					JSONArray dictsInfo = new JSONArray();
					for (int i = 0; i < ids.size(); i++) {
						BookPresenter book = md_getByURL(ids.get(i));
						if(book!=null) {
							dictsInfo.add(book.getDictInfo(null));
						}
					}
					return newFixedLengthResponse(dictsInfo.toString()) ;
				} catch (Exception e) {
					return emptyResponse;
				}
			}
			if(loadManager.md_size>0) {
				JSONArray dictsInfo = new JSONArray();
				for (int i = 0; i < loadManager.md_size; i++) {
					JSONObject json = new JSONObject();
					BookPresenter book = loadManager.md_get(i);
					if(book!=null) {
						book.getDictInfo(json);
					} else {
						json.put("name", loadManager.md_getName(i));
					}
					dictsInfo.add(json);
					//sb.append(md_getName(i));
					//if(mdx.hasVirtualIndex())
					//	sb.append(":VI&VI");
				}
				return newFixedLengthResponse(dictsInfo.toString()) ;
			}
			return emptyResponse;
		}
		else if(key.startsWith("\\MdbRSize\\")) {
			if(loadManager.md_size>0) {
				key = key.substring(10);
				try {
					//key = URLDecoder.decode(key, "UTF-8");
				} catch (Exception e) {
					e.printStackTrace();
				}
				long ret=0;
				for (int i = 0; i < loadManager.md_size; i++) {
					BookPresenter mdx = loadManager.md_get(i);
					if(mdx!=null && mdx.getDictionaryName().equals(key))
						ret=mdx.bookImpl.getNumberEntries();
				}
				//SU.Log("MdbRSize ret: "+ret+key);
				return newFixedLengthResponse(ret+"") ;
			}
			return emptyResponse;
		}
		else if(key.startsWith("\\MdbRGetEntries\\")) {
			if(loadManager.md_size>0) {
				key = key.substring(16);
				//SU.Log(key);
				String[] l = key.split("\\\\");
				StringBuilder ret= new StringBuilder();
				// to optimise
				try {
					presenter = md_getByURL(l[0]);
					if(presenter!=null) {
						//SU.Log("capacity "+l[2]);
						int capacity=Math.min(Integer.parseInt(l[2]), 50);
						int base = Integer.parseInt(l[1]);
						for(int j=0;j<capacity;j++) {
							ret.append(presenter.bookImpl.getEntryAt(base + j));
							if(j<capacity-1)
								ret.append("\n");
							//sb.append(mdx.getEntryAt(base+i)).append("\n");
						}
						//sb.setLength(sb.length()-1);
						//ret = sb.toString();
					}
				} catch (Exception e) {
					CMN.Log(e);
				}
				return newFixedLengthResponse(ret.toString()) ;
			}
			return emptyResponse;
		}
		
		if(uri.startsWith("/sound/")) {
			key=uri.substring(6).replace("/","\\");
		}
		
		if(uri.equals("/")) {
			return newFixedLengthResponse(getBaseHtml());
		}
		
		if(uri.startsWith("/PLOD/")) {
			SU.Log("PLOD received : ", uri);
			if (session.getParameters().get("copy")!=null) {
				return newFixedLengthResponse(getClipboard());
			} else {
				handle_search_event(session.getParameters(), session.getInputStream());
			}
			return emptyResponse;
		}
		
		if(uri.startsWith("/about/")) {
			//SU.Log("about received : ", uri);
			uri = uri.substring(7);
			try {
				BookPresenter mdTmp = md_getByURL(uri);
				return newFixedLengthResponse(StringEscapeUtils.unescapeHtml3(mdTmp.bookImpl.getRichDescription()));
			} catch (Exception ignored) { }
		}
		
		if(uri.startsWith("/content/")) {
			//SU.Log("content_received::", uri); //  /content/d5_JPA-d5_JPA
			//  //  /content/0_1
			int ed = uri.lastIndexOf("#");
			if (ed<=9) ed = uri.length(); // strip hash
			uri = uri.substring(9, ed);
			String[] list = uri.split("_");
			String dn=list[0];
			if(!dn.equals("")) {
				String lit=list[list.length - 1];
				int lid = lit.lastIndexOf(":");
				if(lid!=-1){
					list[list.length - 1]=lit.substring(0,lid);
					lid=IU.parsint(lit.substring(lid+1), -1);
				}
				try {
					boolean encoded=dn.charAt(0)=='d';
					presenter = md_getByURL(list[0]);
					if(presenter.getIsWebx()) {
						return newFixedLengthResponse(constructMdPage(presenter,presenter.getWebx().getSyntheticWebPage(), true, 0, session));
					}
					//SU.Log("content_received::presenter::", list[0], presenter, presenter.getId()); //  /content/d5_JPA
					long[] list2 = new long[list.length-1];
					for(int i=0;i<list.length-1;i++)
						list2[i]=encoded?IU.TextToNumber_SIXTWO_LE(list[i+1]):IU.parsint(list[i+1]);
					return newFixedLengthResponse(constructMdPage(presenter,lid!=-1?presenter.bookImpl.getVirtualRecordsAt(this, list2):presenter.bookImpl.getRecordsAt(null, list2), true, (int)list2[0], session));
				} catch (Exception e) {
					SU.Log(e);
				}
			}
			return newFixedLengthResponse(constructMdPage(presenter,"<div>ERROR FETCHING CONTENT:"+uri+"</div>", true, 0, session));
		}
		
		if(presenter==null)
			presenter = loadManager.EmptyBook;
		//CMN.debug("getPlugRes::", presenter, uri);
		boolean shouldLoadFiles = !presenter.isMergedBook() && (PDICMainAppOptions.getAllowPlugRes()||presenter.isHasExtStyle());
		// todo check session.getHeaders() nullable???
		if(shouldLoadFiles && (!PDICMainAppOptions.getAllowPlugResNone()||!presenter.bookImpl.hasMdd()||session.getHeaders().size()>0&&session.getHeaders().containsKey("f"))) {
			Response ret=getPlugRes(presenter, uri);
			if(ret!=null) return ret;
			shouldLoadFiles = false;
		}
		
		if (session.isProxy) {
			key = URLDecoder.decode(key);
		}
		key = mdict.requestPattern.matcher(key).replaceAll("");
		//BookPresenter mdTmp = md_get(adapter_idx_);
		InputStream restmp = presenter.bookImpl.getResourceByKey(key);
		//SU.Log("-----> /dictionary/", presenter.bookImpl.getDictionaryName(), key, restmp==null?-1:restmp.available());
		
		if(restmp==null){
			if(shouldLoadFiles) {
				Response ret=getPlugRes(presenter, uri);
				if(ret!=null) return ret;
			}
			return emptyResponse;
		}
		
		if(Acc.contains("javascript/") || uri.endsWith(".js")) {
			return newFixedLengthResponse(Status.OK,"application/x-javascript",restmp,restmp.available());
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
		
		if(uri.endsWith(".tif")||uri.endsWith(".tiff"))
			try {
				restmp = convert_tiff_img(restmp);
				//CMN.pt("再编码耗时 : ");
			} catch (Exception e) { e.printStackTrace(); }
		
		if(Acc.contains("image/svg")||uri.endsWith(".svg")) {
			//BU.printFileStream(restmp, new File("/sdcard/"+new File(uri).getName()));
				return newFixedLengthResponse(Status.OK, "image/svg+xml", restmp, restmp.available());
		}
		
		if(Acc.contains("image/")||uri.endsWith(".png")||uri.endsWith(".jpg")||uri.endsWith(".jpeg")||uri.endsWith(".webp")) {
			//SU.Log("Image request : ",Acc,key,presenter.bookImpl.getDictionaryName(), restmp.available());
			return newFixedLengthResponse(Status.OK,(IsCustomer&&ReceiveText)?"text/plain":"image/png", restmp, restmp.available());
		}
		
		return newFixedLengthResponse(Status.OK,"*/*", restmp, restmp.available());
	}
	
	private Response getPlugRes(BookPresenter presenter, String uri) {
//		CMN.debug("server::getPlugRes!", presenter.isHasExtStyle() , uri, presenter.getDictionaryName());
		try {
			if(uri.length()<32*5 && uri.length()>3) {
				int sid = uri.lastIndexOf(".");
				if(sid>0 && sid<uri.length()-2) {
					String decoded = null;
//					CMN.debug("同名CSS!", URLDecoder.decode(uri), presenter.getDictionaryName());
					if(presenter.isHasExtStyle() && uri.endsWith(".css")) {
						if (uri.contains("%")) {
							decoded = URLDecoder.decode(uri);
							sid = decoded.lastIndexOf(".");
						}
//						CMN.debug("isHasExtStyle::", decoded, presenter.getDictionaryName());
						if (decoded.regionMatches(1, presenter.getDictionaryName(), 0, sid-1)) {
							return newChunkedResponse(Status.OK, "text/css", presenter.getDebuggingResource(decoded));
						}
					}
					if(PDICMainAppOptions.getAllowPlugRes()) {
						if(PDICMainAppOptions.getAllowPlugResSame()) {
							String p = presenter.getPath();
							String d = presenter.getDictionaryName();
							int sep = p.lastIndexOf(File.separator, p.lastIndexOf(File.separator)-1)+1;
							if(sep>0) {
								if(p.regionMatches(true, sep, d, 0, Math.min(d.length(), 3))) {
//									CMN.debug("同名目录!");
									p=null;
								}
							}
							if(p!=null) {
								return null;
							}
						}
						int mid="jscssjpgpngwebpicosvgini".indexOf(uri.substring(sid+1));
//						CMN.debug("文件", uri, mid);
						if(mid>=0 && !(mid>=5&&mid<=18)) {
							if(decoded==null)
								decoded = uri.contains("%")?URLDecoder.decode(uri):uri;
							InputStream input = presenter.getDebuggingResource(decoded);
							if(input!=null) {
								String MIME = mid==0?"application/x-javascript"
										:mid==2?"text/css"
										:mid>=5&&mid<18?"img/*"
										:mid==18?"img/svg" //todo
										:"*/*"
										;
								return newChunkedResponse(Status.OK,MIME, input);
							}
						}
					}
					if(presenter.isHasExtStyle() && uri.endsWith(".css") && PDICMainAppOptions.getAllowPlugCss())
					{
						decoded = uri.contains("%")?URLDecoder.decode(uri):uri;
						if (decoded.regionMatches(1, presenter.getDictionaryName(), 0, sid-1)) {
							return newChunkedResponse(Status.OK, "text/css", presenter.getDebuggingResource(decoded));
						}
					}
				}
			}
		} catch (Exception e) {
			SU.Log(e);
		}
		return null;
	}
	
	protected abstract InputStream convert_tiff_img(InputStream restmp) throws Exception;
	
	protected abstract void handle_search_event(Map<String, List<String>> text, InputStream inputStream);
	
	
	public long getBookIdByURLPath(String url, int st, int ed) {
		CharSequenceKey key = new CharSequenceKey(url, st, ed);
		if (key.length()>1 && key.charAt(0) == 'd') {
			key.reset(st + 1);
			return IU.TextToNumber_SIXTWO_LE(key);
		}
		return -1;
	}
	
	public BookPresenter md_getByURLPath(String url, int st, int ed) {
		CharSequenceKey key = new CharSequenceKey(url, st, ed);
		if(key.length()>1 && key.charAt(0)=='d') {
			key.reset(st+1);
			return loadManager.getBookById(IU.TextToNumber_SIXTWO_LE(key));
		}
		url = key.toString();
		int pos=-1;
		try {
			pos = Integer.parseInt(url);
		} catch (Exception ignored) { }
		if(pos>=0) {
			return loadManager.md_get(pos);
		}
		return loadManager.md_getByName(url);
	}
	
	public BookPresenter md_getByURL(String url) {
		if(url.charAt(0)=='d') {
			return loadManager.getBookById(IU.TextToNumber_SIXTWO_LE(new CharSequenceKey(url, 1)));
		}
		int pos=-1;
		try {
			pos = Integer.parseInt(url);
		} catch (Exception ignored) { }
		if(pos>=0) {
			return loadManager.md_get(pos);
		}
		return loadManager.md_getByName(url);
	}
	
	protected InputStream OpenMdbResourceByName(String key) throws IOException {
		InputStream ret = null;
		if (MdbResource!=null) {
			if (!key.startsWith("\\")) {
				key = "\\"+key;
			}
			if(MdbResource instanceof com.knziha.plod.dictionary.mdict) {
				ret = ((com.knziha.plod.dictionary.mdict)MdbResource).getResourceByKey(key);
			} else {
				int id = MdbResource.lookUp(key);
				//SU.Log("lookUp::", key, id);
				if(id>=0) {
					ret = MdbResource.getResourseAt(id);
				}
			}
		}
		return ret;
	}
	
	private String Reroute(String currentText) {
		SU.Log(currentFilter.size(), "Reroute", currentText);
		try {
			for (BookPresenter mdTmp:currentFilter) {
				Object rerouteTarget = mdTmp.bookImpl.ReRoute(currentText);
				if(rerouteTarget instanceof String){
					String text = (String) rerouteTarget;
					SU.Log("Reroute",mdTmp.bookImpl.getDictionaryName(), text);
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
	 function wrappedOnLoadFunc() {
	 	document.getElementById('view1').setAttribute('content', 'width=device-width, initial-scale=1, maximum-scale=1.0, user-scalable=no');
	 	if(postInit) postInit();
		var imgs = document.getElementsByTagName('IMG');
		for(var i=0;i<imgs.length;i++){
			if(imgs[i].src.startsWith("file://"))
	 			imgs[i].src = imgs[i].src.substring(7);
			if(imgs[i].src.startsWith("/"))
	 			imgs[i].src = imgs[i].src.substring(1);
		}
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
	String SimplestInjection=StringUtils.EMPTY;
	/** /'/>
	 <base target="_self" />
	 */
	@Metaline(trim=false)
	String SimplestInjectionEnd=StringUtils.EMPTY;
	
	int MdPageBaseLen=-1;
	String MdPage_fragment1,MdPage_fragment2, MdPage_fragment3="</html>";
	int MdPageLength=0;
	private String constructMdPage(BookPresenter presenter, String record, boolean b1, int pos, HTTPSession session) {
		if(b1 && mdict.fullpagePattern.matcher(record).find())
			b1=false;
		CMN.debug("constructMdPage", session.isProxy, b1);
		b1=true;
		if(b1) {
			StringBuilder MdPageBuilder = new StringBuilder(MdPageLength + record.length() + 5);
			String SubPage = null, MdPage_fragment3 = "</html>";
			int SubPageLen = -1; //rrr
			if (SubPageLen == -1) {
				try {
					SubPage = BU.StreamToString(OpenMdbResourceByName("\\MdbR\\subpage.html"));
					SubPageLen = SubPage.length();
				} catch (IOException e) {
					SU.Log(e);
				}
			}
			
			MdPageBuilder.append(SubPage);
			MdPageBuilder.append("<base target=\"_self\" href=\"/base/")
					.append(presenter.idStr) // "/base/dOED"
					.append("/\" />");
			
			//mdd资源请求应使用相对路径以减少处理过程。
			record = nautyUrlRequest.matcher(record).replaceAll("src=$1");
			
			presenter.plugCssWithSameFileName(MdPageBuilder);
			MdPageBuilder.append("<script>if(window.app&&!frameElement)app.view(sid.get(),")
					.append("'").append(presenter.idStr).append("'")
					.append(",").append(pos)
					.append(",").append(0)
					.append(");")
					//.append("window.entryKey='").append(presenter.getBookEntryAt(pos)).append("';")
					.append("window.pos=").append(pos).append(";");
			if (session.isProxy) {
				MdPageBuilder.append(app.getCommonAsset("SUBPAGE.js"));  // todo check redu
			}
			MdPageBuilder.append("</script>");
			if (app.fontFaces!=null) {
				MdPageBuilder.append("<style class=\"_PDict\">");
				MdPageBuilder.append(app.fontFaces);
				MdPageBuilder.append("</style>");
			}
			if (app.plainCSS!=null) {
				String plainCSS = app.plainCSS;
				if (PDICMainAppOptions.debugCss()) {
					File cssFile = new File(app.opt.GetPathToMainFolder()+"plaindict.css");
					plainCSS = BU.fileToString(cssFile);
				}
				MdPageBuilder.append("<style class=\"_PDict\">");
				MdPageBuilder.append(plainCSS);
				MdPageBuilder.append("</style>");
			}
			if (presenter.padLeft() || presenter.padRight()) {
				MdPageBuilder.append("<style>body{");
				presenter.ApplyPadding(MdPageBuilder);
				MdPageBuilder.append("}</style>");
			}
			if (presenter.zhoAny()!=0) {
				MdPageBuilder.append("<style>");
				MdPageBuilder.append("html{min-height:").append(presenter.zhoHigh() ? "92%" : "100%")
						.append(";display:flex;")
						.append(presenter.zhoVer() ? "align-items:center;" : "")
						.append(presenter.zhoHor() ? "justify-content:center;" : "")
						.append("}");
				MdPageBuilder.append("</style>");
			}
			MdPageBuilder.append("</head>")
					.append(record)
					.append(MdPage_fragment3)
			;
			SU.Log("new_id::", presenter.getId(), IU.NumberToText_SIXTWO_LE(presenter.getId(), null));
			MdPageBuilder.append("<div class=\"_PDict\" style='display:none;'><p class='bd_body'/>");
			if(presenter.bookImpl.hasMdd()) MdPageBuilder.append("<p class='MddExist' id='MddExist'/>");
			MdPageBuilder.append("</div>");
			return MdPageBuilder.toString();
		}
		else{
			if(true){
				int idx = record.indexOf("<head>");
				StringBuilder MdPageBuilder = new StringBuilder(MdPageLength + record.length() + 5);
				String start = idx==-1?"<head>":record.substring(0, idx+6);
				String end = idx==-1?record:record.substring(idx+6);
				
				MdPageBuilder
						.append(start)
						.append(SimplestInjection)
						//.append(dictIdx)// "/base/0"
						.append("d").append(IU.NumberToText_SIXTWO_LE(presenter.getId(), null))// "/base/d0"
						.append(SimplestInjectionEnd)
							.append("<script>if(window.app&&!frameElement)app.view(sid.get(),")
							.append("'").append(presenter.idStr).append("'")
							.append(",").append(pos)
							.append(",").append(0)
							.append(");")
							//.append("window.entryKey='").append(presenter.getBookEntryAt(pos)).append("';")
							.append("window.pos=").append(pos).append(";");
				if (session.isProxy) {
					MdPageBuilder.append(app.getCommonAsset("SUBPAGE.js"));
				}
				MdPageBuilder.append("</script>")
						.append(idx==-1?"</head>":"")
						.append(end);
				return MdPageBuilder.toString();
			}
			return record;
		}
	}
	public static Response emptyResponse = newFixedLengthResponse(Status.NO_CONTENT,"*/*", "");
	
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
		//if(opt.isCombinedSearching())
		//{
		//	derivedHtmlBase.append("document.getElementById('fileBtn').onclick();");
		//}
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
	
	public String getClipboard() {
		return "";
	}
	
	private String getBaseHtml() {
		if(true || baseHtml==null) {//rrr
			try {
				InputStream fin = OpenMdbResourceByName("\\mdict_browser.html");
				baseHtml = BU.StreamToString(fin);
				fin.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return baseHtml;
	}
	
	Response mBaseResponse;
	private Response getMergedBaseResponse(boolean isProxy) {
		if (mBaseResponse==null || hasRemoteDebugServer) {
			mBaseResponse = newRecyclableResponse(Status.OK, NanoHTTPD.MIME_HTML, getMergedBaseHtml());
		}
		return mBaseResponse.newInstance(isProxy);
	}
	
	private String getMergedBaseHtml() {
		String ret=null;
		try {
			InputStream fin = OpenMdbResourceByName("\\merged_browser.html");
			if (fin!=null) {
				ret = BU.StreamToString(fin);
				fin.close();
			}
		} catch (Exception e) {
			//CMN.debug(ret);
		}
		if(ret==null)
			ret = getBaseHtml();
		return ret;
	}
	
	public static boolean isServerRunning = false;
}
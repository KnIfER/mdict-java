package test.Examples;

import com.knziha.plod.dictionary.mdictRes;
import com.knziha.plod.dictionaryBuilder.mdictBuilder;
import com.knziha.plod.dictionaryBuilder.mdictResBuilder;
import test.CMN;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import static test.Examples.TiffMddConverter.ProcessAllFiles;
import static test.Examples.TiffMddConverter.UnpackMdd;

public class DirectWebSpider_photos {


	public static void main(String[] args) throws IOException {
		// 下载预先保存好的资源链接
		mdictRes mddRaw = null;
		try {
			mddRaw = new mdictRes("D:\\photos\\photosRaw.mdd");
		} catch (IOException ignored) { }

		if(false) {/* false true */
			String sheet = "D:\\photos\\sheet";
			HashSet<String> prcossed = CMN.AdaptivelyGetAllLines(sheet+"Record", null);
			mdictResBuilder builder = new mdictResBuilder("", "");

			HashMap<String, FileOutputStream> loggers = new HashMap<>();
			byte[] buffer = new byte[4096];
			int tryCount = 0, failCount = 0;
			ArrayList<String> alllines = CMN.AdaptivelyGetAllLines(null, sheet);
			for(String line:alllines)
			try {
				ByteArrayOutputStream bos = new ByteArrayOutputStream();
				tryCount++;
				String key = line.substring("https:/".length()).replace("/", "\\");
				InetSocketAddress addr = new InetSocketAddress("http://127.0.0.1",8080);
				Proxy proxy = new Proxy(Proxy.Type.HTTP, addr); // http 代理
				if(!prcossed.contains(key)  && (mddRaw==null || mddRaw.lookUp(key)<0)) {
					CMN.Log(key, line);
					URL requestURL = new URL(line);

					HttpURLConnection urlConnection = (HttpURLConnection) requestURL.openConnection();
					urlConnection.setRequestMethod("GET");
					//urlConnection.setRequestProperty("Accept-Language", "zh-CN");
					//urlConnection.setRequestProperty("Referer", url.toString());
					urlConnection.setConnectTimeout(60000);
					urlConnection.setRequestProperty("Charset", "UTF-8");
					urlConnection.setRequestProperty("Connection", "Keep-Alive");
					urlConnection.setRequestProperty("User-agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/27.0.1453.94 Safari/537.36");
					urlConnection.connect();
					InputStream is = urlConnection.getInputStream();

					int len;
					while ((len = is.read(buffer)) > 0) {
						bos.write(buffer, 0, len);
					}
					is.close();
					urlConnection.disconnect();
					builder.insert(key, bos.toByteArray());
					prcossed.add(key);
					CMN.AdaptivelyLogFiles(loggers, false, sheet + "Record", key);
				}
				CMN.Log("succ");

				try {
					Thread.sleep(200);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			} catch (IOException e) {
				e.printStackTrace();
				failCount++;
				CMN.AdaptivelyLogFiles(loggers, false, sheet + "new", line);
			}

			builder.write("D:\\photos\\photosX.mdd");

			CMN.AdaptivelyLogFiles(loggers);
			CMN.Log("fileWrited...", failCount, " / ", tryCount);
		}


		/* 建立 index 首页 */
		if(false) {/* false true */
			mdictBuilder builder = new mdictBuilder("", "", "UTF-8");

			builder.insert("index", new File("D:\\photos\\Unpacked\\0"));

			builder.write("D:\\photos\\photos.mdx");
		}




		/* 从源资源 mdd 处理文件名称， 删除无用冗余 */
		if(true) {/* false  true  */
			mdictResBuilder builder = new mdictResBuilder("", "");
			mddRaw = new mdictRes("D:\\photos\\photosX.mdd");
			for (int i = 0; i < mddRaw.getNumberEntries(); i++) {
				String key = mddRaw.getEntryAt(i).replace("\\cdn.photosbody.com","").replace("\\www.photosbody.com","");
				int idx = key.indexOf("?");
				if(idx!=-1) key=key.substring(0,idx);
				builder.insert(key, mddRaw.getRecordData(i));
			}
			builder.write("D:\\photos\\photosXX.mdd");
		}

		/* 解包 */
		if(true) {/* false true */
			UnpackMdd("D:\\photos\\Unpacked", new mdictRes("D:\\photos\\photosXX.mdd") ,false);
		}


		/* 重新打包成处理过js文件等后的 mdd 文件 */
		if(false) {/* false true */
			mdictResBuilder builder = new mdictResBuilder("", "");
			int basePathLen = "D:\\photos\\Unpacked".length();
			File startPath=new File("D:\\photos\\Unpacked");
			ProcessAllFiles(startPath,
					fI -> builder.insert(fI.getAbsolutePath().substring(basePathLen), fI));
			builder.write("D:\\photos\\photos.mdd");
		}












	}






}

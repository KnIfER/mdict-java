package test.Examples;

import com.knziha.plod.dictionary.Utils.BU;
import com.knziha.plod.dictionary.Utils.ReusableByteOutputStream;
import com.knziha.plod.dictionary.mdict;
import com.knziha.plod.dictionaryBuilder.mdictBuilder;
import org.apache.commons.text.StringEscapeUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.knziha.metaline.Metaline;
import com.knziha.plod.plaindict.CMN;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.regex.Pattern;

import static com.knziha.plod.plaindict.CMN.emptyStr;

/**
 * TEsts
 * @author KnIfER
 * @date 2019/10/25
 */
public class Rebuild_jianming {
	/**
	 <style>
	 //词条名1 A &amp; E
	.DC{
		display:none;
	}
	//词条名2
	.YX{
		margin-top:4px;
		font-size:120%;
		color:black;
		font-weight:bold;
	}
	//词条名3,带上标
	.YD{
		color:black;
		margin-left:4px;
	}
	//音标
	.CB{
		color:sienna;
		margin-left:5
	}
	//拼音
	.PY{
		color:sienna;
		margin-left:5
	}
	//词性
	.DX{
		margin-left:8px;
		margin-right:8px;
		color:darkred;
		font-style:italic;
		font-weight:bold;
		text-decoration:underline;
		border:none;
		border-top:5px solid transparent;
		border-bottom: 2px solid transparent;
	}
	//英文解析
	.JX{
		margin-left:4px;
		color:#2b43c1
	}
	//汉语解析
	.GZ{
		margin-left:8px;
	}
	//例句(父)
	.LJ{
		margin-left:14px;
	}
	//例句(英)
	.LY{
	}
	//例句(汉)
	.LS{
		color:slategray;
		margin-left:8px;
	}
	.entryDot{
		font-weight:bold;
		color:#4F7FDF;
		font-weight:bold;
		margin-right:3px;
	}
	//区域标题
	.section_title{
		margin-left:5px;
		margin-top:4px;
		font-weight:bold;
	}
	//特殊格式
	.italic{
		font-style:italic;
	}
	.bold{
		font-weight:bold;
	}
	.superscript{
		vertical-align:super;
		font-size: 60%
	}
	.subscript{
		vertical-align:sub;
		font-size: 60%
	}
	//子条目链接
	.ref_title{
		margin-right:5;
		font-weight:bold;
		font-style:normal;
	}
	.reference{
		font-style:italic;
	}</style>
	 */
	@Metaline
	static final String styles="ST";
    static int d=0;
    public static void main(String[] args) throws IOException{
    	mdictBuilder mdxDB = new mdictBuilder("JM","Converted by Superfan。","UTF-8");
		mdict mdx = new mdict("D:\\assets\\mdicts\\简明英汉汉英词典.mdx");


		if(false) //词条太多，解包先 好久好久好久……
		for (int i = 0; i < mdx.getNumberEntries(); i++) {
			//mdxDB.insert(mdx.getEntryAt(i), mdx.getRecordAt(i));
			BU.printFile(mdx.getRecordData(i), "F:\\assets\\mdicts\\简明英汉汉英词典\\"+i);
		}

		//小试牛刀
		Document doc;
		int testStart=0;//mdx.lookUp("take", true);
		int testNum=(int) mdx.getNumberEntries();//testStart+102;//
		String path="F:\\assets\\mdicts\\简明英汉汉英词典\\";
		String path2="F:\\assets\\mdicts\\简明英汉汉英词典3\\";
		String EI;
		byte[] buffer = new byte[4096];
		ReusableByteOutputStream bos = new ReusableByteOutputStream();
		Charset _charset = Charset.forName("utf8");
		//(?s)\{.*?\}
		//.DC
		//.YX
		//.YD
		//.CB
		//.PY
		//.JS
		//.DX
		//.JX
		//.GZ
		//.LJ
		//.LY
		//.LS
		//.entryNum
		//.entryDot
		//.section_title
		//.italic
		//.bold
		//.superscript
		//.subscript
		//.ref_title
		//.reference
		/**/
		String commonpart="<link rel='stylesheet' type='text/css' href='sf_cb.css'>";
		String commonpart2="<link rel=\"stylesheet\" type=\"text/css\" href=\"sf_cb.css\">";
		ArrayList<String> classes = new ArrayList<>(Arrays.asList("DC", "YX", "YD", "CB", "PY", "JS", "DX", "JX", "GZ", "LJ", "LY", "LS", "section_title", "italic", "bold", "superscript", "subscript", "ref_title", "reference"));
		int s=classes.size();
		//woc 来一个个过一遍吧 吐血整理
		String text; StringBuilder sb=new StringBuilder(20);

		CMN.rt();

		if(false)
		for (int i = testStart; i < testNum; i++) {
			EI = BU.fileToString(path+i, buffer, bos, _charset);
			doc = Jsoup.parseBodyFragment(EI);
			for (String aClass : classes) {
				//doc.getElementsByClass(aClass).tagName("div");
				throw new RuntimeException();
			}
			doc.getElementsByClass("entryNum").remove();
			doc.getElementsByClass("DC").remove();
			int cc=0;
			Elements PhoSymbols = doc.getElementsByClass("CB");
			for(Element pI:PhoSymbols){
				text=pI.text();
				if(text.startsWith("D.J.:[")&&text.endsWith("]")){
					text = text.substring(5);
					pI.text(text);
					cc++;
				} else if(text.startsWith("K.K.:[")&&text.endsWith("]")){
					sb.setLength(0);
					text = sb.append("{").append(text, 6, text.length() - 1).append("}").toString();
					pI.text(text);
					cc++;
				}
				if(cc>=2) break;
			}
			EI=doc.body().html();
			EI=EI.replaceAll("\\s+"," ");
			EI=EI.replaceAll(" ?([<>]) ?","$1");
			if(EI.startsWith(commonpart2)){
				EI="`1`"+EI.substring(commonpart2.length());
			}else{
				CMN.Log("NONO", i);
			}
			BU.printFile(EI.getBytes(_charset), path2+i);
		}

		if(false) {
			CMN.pt("页面处理完毕……");
			CMN.rt();

			String _styles = StringEscapeUtils.escapeHtml3(styles);

			mdxDB._stylesheet = "1\n" + _styles + "\n\n";
			CMN.Log(_styles);
			//小试
			for (int i = testStart; i < testNum; i++) {
				mdxDB.insert(mdx.getEntryAt(i), new File(path2 + i));
				//mdxDB.insert("a"+i, "???");
			}
			CMN.pt("写入中……");
			CMN.rt();
			mdxDB.setRecordUnitSize(32);
			mdxDB.setCompressionType(2);

			mdxDB.write("D:\\assets\\mdicts\\T\\简明英汉汉英词典【修订】4.mdx");
			CMN.pt_mins("写入完成");//耗时15分钟
		}


		//去重 1
		if(true){/* false true */
			HashMap<String, Integer> duplicateKeys = new HashMap<>();
			mdictBuilder mdxDB_duplicate_entry = new mdictBuilder("JM","Converted by Superfan。","UTF-8");
			Pattern replaceReg = Pattern.compile("[ &:$/.,\\-'()\\[\\]#<>《》，!\\n]");
			for (int i = testStart; i < testNum; i++) {

				String raw_key = mdx.getEntryAt(i);
				String key = mdict.processText(raw_key);

				if(!duplicateKeys.containsKey(key)){
					duplicateKeys.put(key, i);
				}else{
					Integer fdp = duplicateKeys.get(key);
					if(fdp!=null){
						mdxDB_duplicate_entry.insert(mdx.getEntryAt(fdp)+":"+fdp, mdx.getRecordAt(fdp));
						duplicateKeys.put(key, null);
					}
					mdxDB_duplicate_entry.insert(raw_key+":"+i, mdx.getRecordAt(i));
				}

			}
			mdxDB_duplicate_entry.write("D:\\assets\\mdicts\\T\\简明英汉汉英词典【修订】5.duplicate.mdx");
		}

		//去重 2
		if(false){/* false true */
			mdict mdx_duplicate_entry = new mdict("D:\\assets\\mdicts\\简明英汉汉英词典.mdx");
			HashMap<String, Integer> duplicateKeys = new HashMap<>();
			HashSet<Integer> duplicateIds = new HashSet<>();
			mdictBuilder mdxDB_duplicate_entry = new mdictBuilder("JM","Converted by Superfan。","UTF-8");
			Pattern replaceReg = Pattern.compile("[ &:$/.,\\-'()\\[\\]#<>《》，!\\n]");
			for (int i = 0; i < mdx_duplicate_entry.getNumberEntries(); i++) {
				String raw_key = mdx_duplicate_entry.getRecordAt(i);
				String key = replaceReg.matcher(raw_key).replaceAll(emptyStr).toLowerCase();

				if(!duplicateKeys.containsKey(key)){
					duplicateKeys.put(key, i);
				}else{
					Integer fdp = duplicateKeys.get(key);
					if(fdp!=null){
						duplicateIds.add(fdp);
						duplicateKeys.put(key, null);
					}
					duplicateIds.add(i);
				}
			}

			for (int i = 0; i < mdx_duplicate_entry.getNumberEntries(); i++) {
				if(duplicateIds.contains(i)){
					mdxDB_duplicate_entry.insert(mdx_duplicate_entry.getEntryAt(i), mdx_duplicate_entry.getRecordAt(i));
				}
			}

			mdxDB_duplicate_entry.write("D:\\assets\\mdicts\\T\\简明英汉汉英词典【修订】5.2.duplicate.mdx");
		}


    }
}



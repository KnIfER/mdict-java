package com.knziha.plod.db;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.cjk.CJKAnalyzer;
import org.apache.lucene.analysis.cjk.CJKTokenizer;
import org.apache.lucene.analysis.cjk.CJKWidthFilter;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.util.StopwordAnalyzerBase;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.highlight.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.junit.jupiter.api.Test;
import org.knziha.metaline.Metaline;
import test.CMN;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

public class TestSch2 {
	private static Analyzer newCjkAnalyzer() {
		return new StopwordAnalyzerBase(Version.LUCENE_47){
			protected TokenStreamComponents createComponents(String fieldName, Reader reader) {
				//CMN.Log("createComponents...", fieldName, reader);
				Tokenizer source = new StandardTokenizer(this.matchVersion, reader);
				TokenStream result = new LowerCaseFilter(this.matchVersion, source);
				WordBreakFilter bwf = new WordBreakFilter(result);
				TokenStreamComponents ret = new TokenStreamComponents(source, new StopFilter(this.matchVersion, bwf, this.stopwords));
				bwf.component = ret;
				return ret;
				//return new TokenStreamComponents(source, new StopFilter(this.matchVersion, result, this.stopwords));
			}
		};
	}
	
	//分词效果
	@Test
	public void testTokenStream() throws Exception {
		Analyzer analyzer = newCjkAnalyzer();
		//Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_47);
		for (int i = 0; i < 2; i++) {
			int finalI = i;
			new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						CMN.Log("\n分析开始……");
						//获得tokenStream对象
						//第一个参数：域名，可以随便给一个
						//第二个参数：要分析的文本内容

						TokenStream tokenStream = null;
						//tokenStream = analyzer.tokenStream("test"+ finalI, "我是中国人 是我天山雪 开飞机造坦克 The Spring's Framework provides a spring programming and configuration model.");
						tokenStream = analyzer.tokenStream("test", "今天什么使你不开心");
						//添加一个引用，可以获得每个关键词
						CharTermAttribute charTermAttribute = tokenStream.addAttribute(CharTermAttribute.class);
						//添加一个偏移量的引用，记录了关键词的开始位置以及结束位置
						OffsetAttribute offsetAttribute = tokenStream.addAttribute(OffsetAttribute.class);
						//将指针调整到列表的头部
						tokenStream.reset();
						//遍历关键词列表，通过incrementToken方法判断列表是否结束
						while (tokenStream.incrementToken()) {
							CMN.Log(charTermAttribute
									+ " start->" + offsetAttribute.startOffset()
									+ " end->" + offsetAttribute.endOffset());
						}
						tokenStream.close();
						CMN.Log("\n分析结束……");
					} catch (IOException e) {
						CMN.Log(e);
					}
				}
			}).run();
		}
		Thread.sleep(200);
	}

	public static void main(String[] args) throws IOException, ParseException {
		Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_47);
		analyzer = newCjkAnalyzer();
		//analyzer = new CJKAnalyzer(Version.LUCENE_47);
		Directory index = FSDirectory.open(new File("G:/lucene-demo-index")); // MemoryIndex不太会用

		// indexing
		// 1 create index-writer
		String key = "中国";
		String[][] entries = new String[][]{
			new String[]{"0", "asd"}
			, new String[]{"1", "陆地中的国家 海洋中的国家"}
			, new String[]{"3", "陆地中的国家"}
			, new String[]{"4", "海洋中的国家"}
			, new String[]{"5", "我 是 中 国 人"}
				, new String[]{"2", "我是中国人"}
				, new String[]{"1", "陆地中(的国家) 海洋中(的国家)"}
		};

		key = "人民";
		entries = new String[][]{
			new String[]{"0", ""}
			, new String[]{"1", "人民可以得到更多实惠"}
			, new String[]{"2", "中国人民银行"}
			, new String[]{"2", "洛杉矶人，洛杉矶居民"}
			, new String[]{"2", "民族，人民"}
			, new String[]{"2", "工人居民"}
		};

		key = "开心";
		entries = new String[][]{
			new String[]{"0", ""}
//			, new String[]{"1", "磁心开关电路"}
//			, new String[]{"2", "定心圆锥式开卷机"}
//			, new String[]{"2", "最后笑的人笑得最开心。"}
			, new String[]{"clap", "clap clap1 /klæp/ verb (clapped, clapping) [with obj.] strike the palms of (one's hands) together repeatedly, typically in order to applaud 鼓掌： Agnes clapped her hands in glee 阿格尼丝开心地鼓掌 [no obj.] the crowd was clapping and cheering. 人群鼓掌喝彩。 ■ show approval of (a person or action) in this way 鼓掌赞成（人, 举动）。 ■ strike the palms of (one's hands) together once, especially as a signal 击掌（尤指示意）： the designer clapped his hands and the other girls exited the room. 设计师拍了一下手, 其他女孩便离开了房间。 ■ slap (someone) encouragingly on the back or shoulder 拍（某人的肩、背以示鼓励）： as they parted, he clapped Owen on the back. 分别时他拍了拍欧文的背。 ■ place (a hand) briefly against or over one's mouth or forehead as a gesture of dismay or regret （动作迅速）捂（嘴）; 抚摸（额角）（表示沮丧、悔恨）： he swore and clapped a hand to his forehead. 他一边咒骂一边沮丧地抚摸额角。 ■ (of a bird) flap (its wings) audibly （鸟）啪啦振（翅）。 noun 1. an act of striking together the palms of the hands, either once or repeatedly 拍手, 击掌, 鼓掌。 ■ a friendly slap or pat on the back or shoulder （肩、背上友好的）拍击。 2. an explosive sound, especially of thunder 爆裂声（尤指霹雳声）： a clap of thunder echoed through the valley. 雷鸣声在山谷间回荡。 短语 clap eyes on 见EYE. clap hold of informal grab someone or something roughly or abruptly 〈非正式〉猛然抓住（某人, 某物）。 clap someone in jail（或irons） put someone in prison (or in chains) 将某人投入监牢; 给某人戴上镣铐。 词源 Old English clappan 'throb, beat', of imitative origin. Sense 1 dates from late Middle English. 短语动词 clap something on abruptly impose a restrictive or punitive measure 强加（限制）; 强制执行（惩罚性措施）： most countries clapped on tariffs to protect their farmers. 大多数国家强征关税以保护本国农民利益。 "}
			, new String[]{"unrip", "unrip /ˌʌnˈrɪp/ verb （unripped, unripping）［with obj.］ rare open by ripping 〈罕〉撕开, 扯开： he carefully unripped one of the seams. 他小心地拆开其中一条缝。"}
		};
		
		// 2 write index
		if(true)
		{
			IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_47, analyzer);
			config.setOpenMode(OpenMode.CREATE);
			CMN.rt();
			IndexWriter writer = new IndexWriter(index, config);
			for (int i = 0; i < entries.length; i++) {
				Document doc = new Document();
				doc.add(new TextField("entry", entries[i][0], Field.Store.YES));
				doc.add(new StringField("bookName", "NAME", Field.Store.YES));
				// CMN.Log(text);
				doc.add(new TextField("content", entries[i][1], Field.Store.YES));
				writer.addDocument(doc);
			}
			writer.close();
			CMN.pt("索引时间::");
		}

		// search
		if(true)
		{
			CMN.rt();
			IndexReader reader = DirectoryReader.open(index);
			IndexSearcher searcher = new IndexSearcher(reader);
			
			Query query = new QueryParser(Version.LUCENE_47, "content", analyzer).parse(key);

			CMN.Log(styles);
			
			CMN.Log("query: ", query);

			int hitsPerPage = 100;
			// 3 do search
			TopDocs docs = searcher.search(query, hitsPerPage);
			ScoreDoc[] hits = docs.scoreDocs;

			CMN.Log("found " + hits.length + " results", docs.totalHits);


			QueryScorer scorer=new QueryScorer(query); //显示得分高的片段(摘要)
			Fragmenter fragmenter=new SimpleSpanFragmenter(scorer);
			SimpleHTMLFormatter simpleHTMLFormatter=new SimpleHTMLFormatter("<b><font color='red'>","</font></b>");
			Highlighter highlighter=new Highlighter(simpleHTMLFormatter,scorer);
			highlighter.setTextFragmenter(fragmenter);

			
			for(ScoreDoc hit : hits) {
				CMN.Log("<br/><br/>\r\n");
				int docId = hit.doc;
				Document doc = searcher.doc(docId);
				String text = doc.get("content");
				//CMN.Log("<h1 class='title'><a href=''>"+doc.get("entry")+"</a> </h1>");
				if(text!=null) {
					//if(false)
					try {
						String bookName = doc.get("bookName");
						//bookName = "简明英汉汉英词典";
						String dt = "<span class='dt'>"+bookName+"</span>";
						/*把权重高的显示出来*/
						TokenStream tokenStream=analyzer.tokenStream("desc", new StringReader(text));
						String str = highlighter.getBestFragment(tokenStream, text);
						CMN.Log("<div class='preview'>"/*+dt*/+str+(" ("+hit.score+") ")+"</div>");
						CMN.Log(searcher.explain(query, hit.doc));
						continue;
					} catch (InvalidTokenOffsetsException e) {
						e.printStackTrace();
					}
					CMN.Log("<br/>---15字::", text.substring(0, Math.min(15, text.length())));
				}
			}
			CMN.pt("搜索时间:");

		}
	}
	
	/**<meta name="viewport" content="width=device-width, initial-scale=1" />

<style>
.title {
    line-height: 22px;
	text-transform:capitalize
}
.preview {
    word-wrap: break-word;
    word-break: break-word;
	font-size: 18px;
}
.dt{
	    color: #717994;
}
.dt::after{
	content: ' — ';
	color: #717994;
}
body{
	padding-left:35px
}
</style> */
	@Metaline(trim = false)
	static String styles = "";

}


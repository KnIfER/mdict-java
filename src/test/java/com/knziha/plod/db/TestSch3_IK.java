package com.knziha.plod.db;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
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
import org.wltea.analyzer.core.IKSegmenter;
import org.wltea.analyzer.core.Lexeme;
import org.wltea.analyzer.lucene.IKAnalyzer;
import test.CMN;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

public class TestSch3_IK {
	private static Analyzer newCjkAnalyzer() {
		return new IKAnalyzer(false);
	}

	private static String token(String text, boolean isMaxWordLength)
			throws Exception {
		try {
			java.util.List<String> list = new java.util.ArrayList<String>();
			IKSegmenter ikSegmenter = new IKSegmenter(new StringReader(text), isMaxWordLength);
			Lexeme lexeme;
			while ((lexeme = ikSegmenter.next()) != null) {
				list.add(lexeme.getLexemeText());
			}
			return StringUtils.join(list, "|");
		} catch (Exception e) {
			throw e;
		}
	}
	
	@Test
	public void testTokenizer() throws Exception {
		String text = "希腊晋级16强不要奖金要基地 称是为人民而战 人民大会堂";
		// String text = "希腊总理：相信希腊人民希望留在欧元区";

		System.out.println(token(text, false));
		System.out.println(token(text, true));
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
						TokenStream tokenStream = analyzer.tokenStream("test"+ finalI, "我是中国人 是我天山雪 开飞机造坦克 The Spring's Frameworks was doing provides a spring programming and configuration model.");
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
		String[][] entries = new String[][]{
			new String[]{"0", "asd"}
			, new String[]{"1", "陆地中的国家 海洋中的国家"}
			, new String[]{"3", "陆地中的国家"}
			, new String[]{"4", "海洋中的国家"}
			, new String[]{"5", "我 是 中 国 人"}
				, new String[]{"2", "我是中国人"}
				, new String[]{"1", "陆地中(的国家) 海洋中(的国家)"}
		};
		
		entries = new String[][]{
			new String[]{"0", ""}
			, new String[]{"1", "人民可以得到更多实惠"}
			, new String[]{"2", "中国人民银行"}
			, new String[]{"2", "洛杉矶人，洛杉矶居民"}
			, new String[]{"2", "民族，人民"}
			, new String[]{"2", "工人居民"}
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
			
			Query query = new QueryParser(Version.LUCENE_47, "content", analyzer).parse("人民");

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


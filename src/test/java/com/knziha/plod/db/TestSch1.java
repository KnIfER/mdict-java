package com.knziha.plod.db;

import com.knziha.plod.dictionary.mdict;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.cjk.CJKBigramFilter;
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
import org.apache.lucene.index.*;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.search.highlight.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.junit.jupiter.api.Test;
import org.knziha.metaline.Metaline;
import com.knziha.plod.plaindict.CMN;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.file.Path;
import java.nio.file.Paths;

public class TestSch1 {
	//分词效果
	@Test
	public void testTokenStream() throws Exception {
		//创建一个标准分析器对象
		//Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_47);
		Analyzer analyzer = new StopwordAnalyzerBase(Version.LUCENE_47){
			protected TokenStreamComponents createComponents(String fieldName, Reader reader) {
				if (this.matchVersion.onOrAfter(Version.LUCENE_36)) {
					Tokenizer source = new StandardTokenizer(this.matchVersion, reader);
					TokenStream result = new CJKWidthFilter(source);
					result = new LowerCaseFilter(this.matchVersion, result);
					result = new CJKBigramFilter(result, 15, true);
					return new TokenStreamComponents(source, new StopFilter(this.matchVersion, result, this.stopwords));
				} else {
					Tokenizer source = new CJKTokenizer(reader);
					return new TokenStreamComponents(source, new StopFilter(this.matchVersion, source, this.stopwords));
				}
			}
		};
		
		//获得tokenStream对象
		//第一个参数：域名，可以随便给一个
		//第二个参数：要分析的文本内容
		TokenStream tokenStream = null;
		//tokenStream = analyzer.tokenStream("test", "我是中国人 The Spring Framework provides a spring programming and configuration model.");
		tokenStream = analyzer.tokenStream("test", "最后笑的人笑得最开心");
		//添加一个引用，可以获得每个关键词
		CharTermAttribute charTermAttribute = tokenStream.addAttribute(CharTermAttribute.class);
		//添加一个偏移量的引用，记录了关键词的开始位置以及结束位置
		OffsetAttribute offsetAttribute = tokenStream.addAttribute(OffsetAttribute.class);
		//将指针调整到列表的头部
		tokenStream.reset();
		//遍历关键词列表，通过incrementToken方法判断列表是否结束
		while(tokenStream.incrementToken()) {
			System.out.println(charTermAttribute
					+ " start->" + offsetAttribute.startOffset()
					+ " end->" + offsetAttribute.endOffset());
		}
		tokenStream.close();
	}
	
	public static void main(String[] args) throws IOException, ParseException {
		// create analyzer and directory
		Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_47);
		//analyzer = new org.apache.lucene.analysis.cn.smart.SmartChineseAnalyzer(Version.LATEST);
		Path path = Paths.get("F:/lucene-demo-index", new String[0]);
		//Directory index = FSDirectory.open(path);
		Directory index = FSDirectory.open(new File("F:/lucene-demo-index"));

		// indexing
		// 1 create index-writer

		mdict md = new mdict("D:\\assets\\mdicts\\OED2.mdx");
		
		// 2 write index
		if(true)
		{
			IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_47, analyzer);
			config.setOpenMode(OpenMode.CREATE);
			CMN.rt();
			IndexWriter writer = new IndexWriter(index, config);
			Html2Text html2Text = new Html2Text();
			for (int i = 0; i < md.getNumberEntries(); i++) {
				Document doc = new Document();
				doc.add(new TextField("entry", md.getEntryAt(i), Field.Store.YES));
				doc.add(new StringField("bookName", md.getDictionaryName(), Field.Store.YES));
				String html = md.getRecordAt(i);
				html2Text.parse(html);
				String text = html2Text.getText();
				// CMN.Log(text);
				doc.add(new TextField("content", text, Field.Store.YES));
				writer.addDocument(doc);
			}
			writer.close();
			CMN.pt("索引时间::");
		}

		// 2 remove index
		if(false)
		{
			IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_47, analyzer);
			config.setOpenMode(OpenMode.CREATE_OR_APPEND);
			CMN.rt();
			IndexWriter writer = new IndexWriter(index, config);
			String[] stringQuery;
			stringQuery = new String[]{md.getDictionaryName()};
			String[] fields = {"bookName"};

			BooleanClause.Occur[] occ={BooleanClause.Occur.MUST};

			Query query = MultiFieldQueryParser.parse(Version.LUCENE_47, stringQuery, fields, occ, analyzer);

			query = new QueryParser(Version.LUCENE_47, "bookName", analyzer).parse(md.getDictionaryName());
			Term term = new Term("bookName", md.getDictionaryName());
			query = new TermQuery(new Term("bookName", md.getDictionaryName()));


			IndexReader reader = DirectoryReader.open(index);
			IndexSearcher searcher = new IndexSearcher(reader);
			CMN.Log("totalHits=", searcher.search(query, 5).totalHits);

			writer.deleteDocuments(term);
			CMN.pt("个索引删除时间::");

			//writer.deleteUnusedFiles();
			
			//writer.flush();
			writer.commit();
		}

		// search
		if(true)
		{
			CMN.rt();
			IndexReader reader = DirectoryReader.open(index);
			IndexSearcher searcher = new IndexSearcher(reader);

			//要查找的字符串数组
			String[] stringQuery = {"dollar", "ship"};
			stringQuery = new String[]{"ship's", "dollar"};
			stringQuery = new String[]{"ship's", "dollar"};

			String[] fields = {"content", "content"};
			
			BooleanClause.Occur[] occ={BooleanClause.Occur.MUST, BooleanClause.Occur.MUST};

			Query query = MultiFieldQueryParser.parse(Version.LUCENE_47, stringQuery, fields, occ, analyzer);

			query = new QueryParser(Version.LUCENE_47, "content", analyzer).parse("美丽国");
			query = new QueryParser(Version.LUCENE_47, "content", analyzer).parse("使更加美丽");
			query = new QueryParser(Version.LUCENE_47, "content", analyzer).parse("使更加美丽");

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
				CMN.Log("<h1 class='title'><a href=''>"+doc.get("entry")+"</a> </h1>");
				if(text!=null) {
					//if(false)
					try {
						String bookName = doc.get("bookName");
						//bookName = "简明英汉汉英词典";
						String dt = "<span class='dt'>"+bookName+"</span>";
						/*把权重高的显示出来*/
						TokenStream tokenStream=analyzer.tokenStream("desc", new StringReader(text));
						String str = highlighter.getBestFragment(tokenStream, text);
						CMN.Log("<div class='preview'>"+dt+str+(" ("+hit.score+") ")+"</div>");
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


package com.knziha.plod.db;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
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
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

public class TestSch {
//	public static void main(String[] args) throws IOException, ParseException {
//		// create analyzer and directory
//		StandardAnalyzer analyzer = new StandardAnalyzer();
//		Path path = Paths.get("F:/lucene-demo-index", new String[0]);
//		Directory index = FSDirectory.open(path);
//
//		// indexing
//		// 1 create index-writer
//		IndexWriterConfig config = new IndexWriterConfig(analyzer);
//		config.setOpenMode(OpenMode.CREATE);
//		IndexWriter writer = new IndexWriter(index, config);
//		// 2 write index
//		addDoc(writer, "Lucene in Action", "193398817");
//		addDoc(writer, "Lucene for Dummies", "55320055Z");
//		addDoc(writer, "Managing Gigabytes", "55063554A");
//		addDoc(writer, "The Art of Computer Science", "9900333X");
//		writer.close();
//
//		// search
//		// 1 create query
//		String queryStr = "lucene";
//		Query q = new QueryParser("title", analyzer).parse(queryStr);
//		System.out.println("query: " + q.toString());
//
//		int hitsPerPage = 10;
//		// 2 create index-searcher
//		IndexReader reader = DirectoryReader.open(index);
//		IndexSearcher searcher = new IndexSearcher(reader);
//		// 3 do search
//		TopDocs docs = searcher.search(q, hitsPerPage);
//		ScoreDoc[] hits = docs.scoreDocs;
//
//		// display results
//		System.out.println("found " + hits.length + " results");
//		for(ScoreDoc hit : hits) {
//			int docId = hit.doc;
//			Document doc = searcher.doc(docId);
//			System.out.println(doc.get("title") + " - " + doc.get("isbn"));
//		}
//	}
//
//	private static void addDoc(IndexWriter writer, String title, String isbn) throws IOException {
//		Document doc = new Document();
//		doc.add(new TextField("title", title, Field.Store.YES));
//		doc.add(new StringField("isbn", isbn, Field.Store.YES));
//		doc.add(new TextField("asd", isbn, Field.Store.YES));
//		writer.addDocument(doc);
//	}
}


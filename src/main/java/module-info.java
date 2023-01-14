module com.knziha.plod.PlainDict { // A Poet!
	requires javafx.controls;  // very nice
	requires javafx.fxml; // very nice
	requires javafx.web;// very nice
	requires lzo.core;// very nice
	requires fastjson;// very nice
	requires org.apache.commons.text;// very nice
	requires org.jruby.jcodings;// very nice
	requires Metaline;// very nice
	requires com.github.luben.zstd_jni;// very nice
	requires org.apache.commons.lang3;// very nice
	requires com.springsource.javax.media.jai.codec;// very nice
	requires com.springsource.javax.media.jai.core;// very nice
	requires javafx.media;// very nice
	//requires java.datatransfer;// very nice
	requires java.desktop;
	requires commons.io;
	requires java.logging;
	requires jdk.unsupported;
	requires icafe;
	requires org.apache.httpcomponents.httpcore;
	requires org.apache.httpcomponents.httpclient;
	requires xstream;
	requires closure.compiler.v20211107;
	requires leveldb.api;
	requires leveldb;
	requires org.apache.pdfbox;
	requires org.apache.commons.imaging;
	requires jai.imageio;
	requires jdom;
	requires htmlcompressor;
	requires org.jsoup;
	requires sqlite.jdbc;
	requires java.sql;
	requires junit;

	//opens com.knziha.plod.PlainDict to javafx.fxml;
	opens com.knziha.plod.PlainDict to javafx.web;
	exports com.knziha.plod.PlainDict;
	exports com.knziha.plod.PlainDict.utils;
	exports test.privateTest.bilibili;
	
	
}
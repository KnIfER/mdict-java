package test.Examples;

import com.knziha.plod.dictionary.mdictRes;
import com.knziha.plod.dictionaryBuilder.mdictResBuilder;

import java.io.IOException;

/** Simplest Converter */
public class MddConverter {
	public static void main(String[] args) throws IOException {
		mdictRes mdRaw = new mdictRes("F:\\assets\\mdicts\\汉语\\文言快易通.mdd");
		mdictResBuilder builder = new mdictResBuilder("ooo","test.privateTest.tiff pics to png");
		//builder.setRecordUnitSize(32);
		//builder.setRecordBlockZipLevel(1);
		builder.setCompressionType(2,0);/* 不建议压缩，根本没效果…… */
		for (int i = 0; i < mdRaw.getNumberEntries(); i++) {
			builder.insert(mdRaw.getEntryAt(i).replace(".tif",".png"), mdRaw.getRecordData(i));
		}
		builder.write("F:\\assets\\mdicts\\汉语\\文言快易通.converted.mdd");
	}
}

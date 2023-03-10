package test;

import com.knziha.plod.plaindict.CMN;
import test.privateTest.BitsInputStream;

import java.io.IOException;

public class LSDUserDictionaryDecoder extends LSDDecoder{
	public LSDUserDictionaryDecoder(BitsInputStream _data_in) {
		super(_data_in);
	}

	@Override
	public void read() throws IOException {
		int prefexLen = data_in.readInt();
		//todo check max size
		CMN.Log("prefexLen", prefexLen);
		prefix = data_in.read_unicode(prefexLen, true);
		_article_symbols = data_in.read_symbols();
		_heading_symbols = data_in.read_symbols();
		CMN.Log(_article_symbols.length, _heading_symbols.length);
		_ltArticles = new Enchantable(data_in);
		_ltHeadings = new Enchantable(data_in);

		_ltPrefixLengths = new Enchantable(data_in);
		_ltPostfixLengths = new  Enchantable(data_in);

		_huffman1Number = data_in.read_bits(32);
		_huffman2Number = data_in.read_bits(32);
		_readed = true;
	}
}

package test;

import com.knziha.plod.dictionary.Utils.BU;
import test.privateTest.BitsInputStream;

import java.io.IOException;

public class LSDDecoder {
	public BitsInputStream data_in;
	public String prefix = "";
	public int[] _article_symbols;
	public int[] _heading_symbols;
	public Enchantable _ltArticles;
	public Enchantable _ltHeadings;
	public Enchantable _ltPrefixLengths;
	public Enchantable _ltPostfixLengths;
	public int _huffman1Number = 0;
	public int _huffman2Number = 0;
	public boolean _readed = false;

	LSDDecoder(BitsInputStream _data_in){
		data_in = _data_in;
	}

	public int decode_prefix_len() throws IOException {
		return _ltPrefixLengths.decode();
	}

	public int decode_postfix_len() throws IOException {
		return _ltPostfixLengths.decode();
	}

	public int read_reference1() throws IOException {
		return read_reference(_huffman1Number);
	}

	public int read_reference2() throws IOException {
		return read_reference(_huffman2Number);
	}

    public int read_reference(int huffman_number) throws IOException {
        int reference = 0;//' ';
        int code = data_in.read_bits(2);
        if( code == 3){
            data_in.read_bits(32);
            return reference;
		}
        int size = BU.bit_length(huffman_number);
        assert(size >= 2);
        return (code << (size - 2)) | data_in.read_bits(size - 2);
	}			

    public String decode_heading(int size) throws IOException {
        String res = "";
		for (int i = 0; i < size; i++) {
			int sym_idx = _ltHeadings.decode();
			int sym = _heading_symbols[sym_idx];
            assert(sym <= 0xffff); //LingvoEngine:2EAB84E8
            res += (char)sym;
		}
        return res;
	}

    public String decode_article(int size) throws IOException {
		// decode User and Abrv dict
		String  res = "";
        while( res.length() < size){
            int sym_idx = _ltArticles.decode();
            int sym = _article_symbols[sym_idx];
            if( sym >= 0x10000){
				int s;
				if( sym >= 0x10040){
					int start_idx = data_in.read_bits(BU.bit_length(size));
                    s = sym - 0x1003d;
                    res += res.substring(start_idx, start_idx + s);
				}else{
					int prefix_idx = data_in.read_bits(BU.bit_length(prefix.length()));
                    s = sym - 0xfffd;
                    res += prefix.substring(prefix_idx, prefix_idx + s);
				}
			} else {
				res += (char) sym;
			}
		}
        return res;
	}

    //need seek(data_in.header.dictionary_encoder_offset) befor call!
    public void read() throws IOException {
	}

	public void dump() {
		CMN.Log("Decoder:               %s" + getClass());
		if(_readed) {
			CMN.Log("    ArticleSymbols:    " + _article_symbols.length);
			CMN.Log("    HeadingSymbols:    " + _heading_symbols.length);
		}
		CMN.Log(_ltArticles,"Articles");
		CMN.Log(_ltHeadings,"Headings");
		CMN.Log(_ltPrefixLengths,"PrefixLengths");
		CMN.Log(_ltPostfixLengths,"PostfixLengths");
	}
}

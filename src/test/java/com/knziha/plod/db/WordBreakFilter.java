package com.knziha.plod.db;

import com.ibm.icu.text.BreakIterator;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.*;
import com.knziha.plod.plaindict.CMN;

import java.io.IOException;

public final class WordBreakFilter extends TokenFilter {
	private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
	private final TypeAttribute typeAtt = addAttribute(TypeAttribute.class);
	private final OffsetAttribute offsetAtt = addAttribute(OffsetAttribute.class);
	private final PositionIncrementAttribute posIncAtt = addAttribute(PositionIncrementAttribute.class);
	private final PositionLengthAttribute posLengthAtt = addAttribute(PositionLengthAttribute.class);
	public Analyzer.TokenStreamComponents component;

	public WordBreakFilter(TokenStream in) {
		super(in);
	}

	String text;
	BreakIterator breakIterator = BreakIterator.getWordInstance();
	int start = 0;
	@Override
	public boolean incrementToken() throws IOException {
		//CMN.Log("incrementToken", input);
		if(component.text!=null) {
			breakIterator.setText(text = component.text);
			component.text = null;
			start = 0;
		}
		int end = breakIterator.next();
		while (end != java.text.BreakIterator.DONE) {
			String term = text.substring(start, end).trim();
			int len = term.length();
			boolean deBigram = true;
			if (len>1 && len<termAtt.buffer().length) {
				//CMN.Log("term::", term);
				termAtt.setLength(len);
				char c;
				for (int i = 0; i < len; i++) {
					c = termAtt.buffer()[i] = term.charAt(i);
					if (deBigram 
							&& isBigram(c)
					) {
						deBigram = false;
					}
				}
			}
			if (!deBigram) {
				CMN.Log("term::", term);
//				termAtt.setEmpty();
//				termAtt.append(term);
//				termAtt.setLength(len);
				offsetAtt.setOffset(start, end);
				posIncAtt.setPositionIncrement(1);
				//posLengthAtt.setPositionLength(len);
				typeAtt.setType("<CJ>");
				start = end;
				return true;
			}
			start = end;
			end = breakIterator.next();
		}
		//return false;
		boolean ret = input.incrementToken();
		if (ret && termAtt.length()>2) {
			char[] buf = termAtt.buffer();
			if (buf[termAtt.length()-1]=='s' && buf[termAtt.length()-2]=='\'') {
				termAtt.setLength(termAtt.length() - 2);
				offsetAtt.setOffset(offsetAtt.startOffset(), offsetAtt.endOffset()-2);
			}
		}
		return ret;
	}

	/** 判断是否是合写语言，即中文那样不用空格断词的语言 */
	private boolean isBigram(char c) {
		final String block = Character.UnicodeBlock.of(c).toString();
		if (block.startsWith("CJK")) {
			return true;
		}
		switch (block) {
			case "HIRAGANA":
			case "KATAKANA":
			case "HANGUL_SYLLABLES":
			case "HANGUL_JAMO_EXTENDED_B":
			case "EGYPTIAN_HIEROGLYPHS":
			case "OLD_SOGDIAN":
			case "SOGDIAN":
			case "THAI":
			case "TAMIL":
			case "TAMIL_SUPPLEMENT":
			case "TIBETAN":
			case "BRAHMI":
			case "YI_SYLLABLES":
			case "YI_RADICALS":
				return true;
		}
		return false;
	}

	@Override
	public void end() throws IOException {
		super.end();
		breakIterator.setText("");
	}
}

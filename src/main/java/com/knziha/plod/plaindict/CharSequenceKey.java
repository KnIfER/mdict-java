package com.knziha.plod.plaindict;

import edu.umd.cs.findbugs.annotations.NonNull;
import org.apache.commons.lang3.StringUtils;

import java.io.File;

public class CharSequenceKey implements CharSequence {
	public static final CharSequenceKey Instance = new CharSequenceKey();
	@NonNull
	private String value;
	private int hash;
	private int start;
	private int end;
	private String valueSlim;
	
	public CharSequenceKey() {
			this(StringUtils.EMPTY,0,0);
		}
		
		public CharSequenceKey(@NonNull String val, int st) {
			value = val;
			start = st;
			end = value.length();
		}
	
		public CharSequenceKey(@NonNull String val, int st, int ed) {
			value = val;
			start = st;
			end = ed<0?value.length():ed;
		}
	
		public CharSequenceKey setAsMdName(@NonNull String fullpath) {
			valueSlim=null;
			hash=0;
			value=fullpath;
			start = fullpath.lastIndexOf(File.separatorChar)+1;
			end = fullpath.length();
			return this;
		}
		
		public CharSequenceKey setAsMdCleanName(String fullpath) {
			setAsMdName(fullpath);
			int tmpIdx = end-4;
			if(tmpIdx>0
					&& fullpath.charAt(tmpIdx)=='.' && fullpath.regionMatches(true, tmpIdx+1, "mdx" ,0, 3)){
				end-=4;
			}
			return this;
		}
	
		@Override
		public int length() {
			return end-start;
		}

		@Override
		public char charAt(int index) {
			return value.charAt(start+index);
		}

		@NonNull
		@Override
		public CharSequence subSequence(int start, int end) {
			return value.substring(this.start+start, this.start+end);
		}

		@Override
		public int hashCode() {
			int h = hash;
			if (h == 0) {
				int thisLen = length();
				for (int i = 0; i < thisLen; i++) {
					h = 31 * h + value.charAt(start+i);
				}
				hash = h;
			}
			return h;
		}

		@Override
		public boolean equals(Object obj) {
			if(obj instanceof CharSequence) {
				if(this==obj) {
					return true;
				}
				CharSequence other = (CharSequence) obj;
				int thisLen = length();
				if(other.length()==thisLen) {
					for (int i = 0; i < thisLen; i++) {
						if(value.charAt(start+i)!=other.charAt(i))
							return false;
					}
				}
				return true;
			}
			return false;
		}
	
	@NonNull
	@Override
	public String toString() {
		if(valueSlim==null) {
			valueSlim=value.substring(start, end);
		}
		return valueSlim;
	}
	
	public boolean equalsInternal(String pathname) {
		return value.equals(pathname);
	}
	
	public CharSequenceKey reset(int st, int ed) {
		valueSlim=null;
		this.start = st;
		this.end = ed<st?value.length():ed;
		return this;
	}
	
	public CharSequenceKey reset(int st) {
		valueSlim=null;
		this.start = st;
		return this;
	}
	
	public int getStart() {
		return start;
	}
	
	public int getEnd() {
		return end;
	}
}
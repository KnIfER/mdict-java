package com.knziha.plod.dictionary.Utils;

import java.io.IOException;
import java.util.regex.Pattern;

public class IU {

    public static Integer parseInt(Object o)
{
    /*
WARNING: This method may be invoked early during VM initialization
before IntegerCache is initialized. Care must be taken to not use
the valueOf method.
     */
	String s = String.valueOf(o);
    if (s == null) {
    	return null;
    }

    int result = 0;
    boolean negative = false;
    int i = 0, len = s.length();
    int limit = -Integer.MAX_VALUE;
    int multmin;
    int digit;

    if (len > 0) {
        char firstChar = s.charAt(0);
        if (firstChar < '0') { // Possible leading "+" or "-"
            if (firstChar == '-') {
                negative = true;
                limit = Integer.MIN_VALUE;
            } else if (firstChar != '+')
                return null;

            if (len == 1) // Cannot have lone "+" or "-"
            	 return null;
            i++;
        }
        multmin = limit / 10;
        while (i < len) {
            // Accumulating negatively avoids surprises near MAX_VALUE
            digit = Character.digit(s.charAt(i++),10);
            if (digit < 0) {
            	 return null;
            }
            if (result < multmin) {
            	 return null;
            }
            result *= 10;
            if (result < limit + digit) {
            	 return null;
            }
            result -= digit;
        }
    } else {
        return null;
    }
    return negative ? result : -result;
}


    public static int parsint(Object o){
        return parsint(o,-1);
    }

    public static int parsint(Object o, int val)
{
    /*
WARNING: This method may be invoked early during VM initialization
before IntegerCache is initialized. Care must be taken to not use
the valueOf method.
     */
	String s = String.valueOf(o);
    if (s == null) {
    	return val;
    }

    int result = 0;
    boolean negative = false;
    int i = 0, len = s.length();
    int limit = -Integer.MAX_VALUE;
    int multmin;
    int digit;

    if (len > 0) {
        char firstChar = s.charAt(0);
        if (firstChar < '0') { // Possible leading "+" or "-"
            if (firstChar == '-') {
                negative = true;
                limit = Integer.MIN_VALUE;
            } else if (firstChar != '+')
                return val;

            if (len == 1) // Cannot have lone "+" or "-"
            	 return val;
            i++;
        }
        multmin = limit / 10;
        while (i < len) {
            // Accumulating negatively avoids surprises near MAX_VALUE
            digit = Character.digit(s.charAt(i++),10);
            if (digit < 0) {
            	 return val;
            }
            if (result < multmin) {
            	 return val;
            }
            result *= 10;
            if (result < limit + digit) {
            	 return val;
            }
            result -= digit;
        }
    } else {
        return val;
    }
    return negative ? result : -result;
}


    public static int parseInteger(Object o, int val)
{
    return o instanceof Integer?(int)o:val;
}


	public static long parseLong(String s)
	{
		return parseLong(s, -1);
	}

	public static long parseLong(String s, long val)
	{
		if (s == null) {
			return val;
		}

		long result = 0;
		boolean negative = false;
		int i = 0, len = s.length();
		long limit = -Long.MAX_VALUE;
		long multmin;
		int digit;

		if (len > 0) {
			char firstChar = s.charAt(0);
			if (firstChar < '0') { // Possible leading "+" or "-"
				if (firstChar == '-') {
					negative = true;
					limit = Long.MIN_VALUE;
				} else if (firstChar != '+')
					return val;

				if (len == 1) // Cannot have lone "+" or "-"
					return val;
				i++;
			}
			multmin = limit / 10;
			while (i < len) {
				// Accumulating negatively avoids surprises near MAX_VALUE
				digit = Character.digit(s.charAt(i++),10);
				if (digit < 0
						|| result < multmin
						|| (result *= 10) < limit + digit) {
					if(i>(negative?1:0)) break;
					else return val;
				}
				result -= digit;
			}
		} else {
			return val;
		}
		return negative ? result : -result;
	}



	public static int reduce(String phrase,int start,int end) {//via mdict-js
        int len = end-start;
        if (len > 1) {
            len = len >> 1;

            //CMN.show("asdasd");
            return phrase.compareTo(numOrder[start + len - 1])>0
                    ? reduce(phrase,start+len,end)
                    : reduce(phrase,start,start+len);
        } else {
            return phrase.compareTo(numOrder[start])==0?start:-1;
        }
    }
    static final Pattern hanziDelimeter = Pattern.compile("[十百千万]",Pattern.DOTALL);
    public static final Pattern hanshuzi = Pattern.compile("[一七三两九二五八六四零十百千万]+",Pattern.DOTALL);
    public static final Pattern shuzi = Pattern.compile("[0-9]+",Pattern.DOTALL);
    static final Pattern supportedHanShuZi = Pattern.compile("[十百千万]",Pattern.DOTALL);
    static final String[] numOrder = {"一","七","三","两","九","二","五","八","六","四","零"};
    static final int[] Numbers = {1,7,3,2,9,2,5,8,6,4,0};
    static final int[] Levels = {1,10,100,1000,10000};


    public static int recurse1wCalc(String in,int start,int end,int CurrentLvMPlyer) {
        int _CurrentLvMultiplyer=CurrentLvMPlyer;
        int _CurrentLv=0;
        int ret=0;
        //CMN.show("\r\ncalcStart:"+in.substring(start, end+1));
        while(end>=start) {
            String levelCharacter = in.substring(end, end+1);
            //CMN.show(start+"~~"+end+"~~"+ret);//levelCharacter
            int res=reduce(levelCharacter,0,11);
            if(res==-1) {//是数位符
                int neoLv=0;
                if(levelCharacter.equals("十")) {
                    neoLv=1;
                }else if(levelCharacter.equals("百")) {
                    neoLv=2;
                }else if(levelCharacter.equals("千")) {
                    neoLv=3;
                }else {//if(levelCharacter.equals("万")) {
                    neoLv=4;
                }
                if(end==start&&neoLv==1) {//十几
                    _CurrentLvMultiplyer=_CurrentLvMultiplyer*Levels[neoLv];
                    return 1*_CurrentLvMultiplyer+ret;
                }else if(neoLv>_CurrentLv) {//正常
                    _CurrentLvMultiplyer=CurrentLvMPlyer*Levels[neoLv];
                    //CMN.show("正常"+_CurrentLvMultiplyer+"per"+CurrentLvMPlyer+"Levels[]"+Levels[neoLv]);
                    _CurrentLv=neoLv;
                }else {//递归求前置修饰数
                    //CMN.show("rererererererere");
                    return recurse1wCalc(in,start,end,1)*_CurrentLvMultiplyer+ret;
                }
            }else {//是数符
                ret+=Numbers[res]*_CurrentLvMultiplyer;
            }
            //CMN.show(start+"--"+end+"~~"+ret);//levelCharacter
            end--;
        }
        return ret;
    }

	/**
	 * 把阿拉伯数字转换为罗马数字
	 *
	 * @param number
	 * @return
	 */
	public static String a2r(int number) {
		String rNumber = "";
		int[] aArray = { 1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1 };
		String[] rArray = { "M", "CM", "D", "CD", "C", "XC", "L", "XL", "X",
				"IX", "V", "IV", "I" };
		if (number < 1 || number > 3999) {
			rNumber = "0x"+Integer.toHexString(number);
		} else {
			for (int i = 0; i < aArray.length; i++) {
				while (number >= aArray[i]) {
					rNumber += rArray[i];
					number -= aArray[i];
				}
			}
		}
		return rNumber;
	}
	
	public static long readLong(byte[] readBuffer, int start) {
		return  (((long)readBuffer[start+0] << 56) +
				((long)(readBuffer[start+1] & 255) << 48) +
				((long)(readBuffer[start+2] & 255) << 40) +
				((long)(readBuffer[start+3] & 255) << 32) +
				((long)(readBuffer[start+4] & 255) << 24) +
					  ((readBuffer[start+5] & 255) << 16) +
					  ((readBuffer[start+6] & 255) <<  8) +
					  ((readBuffer[start+7] & 255) <<  0));
	}
	
	public static final void writeLong(byte[] writeBuffer, int pad, long v) {
		writeBuffer[pad+0] = (byte)(v >>> 56);
		writeBuffer[pad+1] = (byte)(v >>> 48);
		writeBuffer[pad+2] = (byte)(v >>> 40);
		writeBuffer[pad+3] = (byte)(v >>> 32);
		writeBuffer[pad+4] = (byte)(v >>> 24);
		writeBuffer[pad+5] = (byte)(v >>> 16);
		writeBuffer[pad+6] = (byte)(v >>>  8);
		writeBuffer[pad+7] = (byte)(v >>>  0);
	}	/** 将数字转为62进制。小端，个位数在前。 */
	public static StringBuilder NumberToText_SIXTWO_LE(long number, StringBuilder sb)
	{
		//CMN.debug("NumberToText_SIXTWO_LE::", number);
		final char[] NumberToText_SIXTWO_ARR = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray();
		final int scale = 62;
		if(sb==null) sb = new StringBuilder(12);
		if(number==0) {
			sb.append("0");
			return sb;
		}
		boolean negative=number<0;
		if(negative) number=-number;
		if(number<0) {
			sb.append("8m85Y0n8LzA~");
			return sb;
		}
		//SU.Log("NumberToText_SIXTWO_LE", number, -(number+1));
		long remainder;
		while (number != 0) {
			remainder = number % scale;
			sb.append(NumberToText_SIXTWO_ARR[(int) remainder]);
			number = number / scale;
		}
		if(negative) sb.append('~');
		return sb;
	}

	/** 62进制字符串转为数字。小端，个位数在前。 */
	public static long TextToNumber_SIXTWO_LE(CharSequence text)
	{
		//CMN.debug("TextToNumber_SIXTWO_LE::", text);
		if("8m85Y0n8LzA~".contentEquals(text)) return Long.MIN_VALUE;
		final int scale = 62;
		long num = 0;
		int len=text.length(),i=len-1;
		if(len>0) {
			boolean negative=text.charAt(i)=='~';
			if(negative) i--;
			int index;
			char c;
			for(; i >= 0; i--)
			{
				c = text.charAt(i);
				if(c>='A'&&c<='Z') {
					index = c-'A'+10;
				} else if(c>='a'&&c<='z'){
					index = c-'a'+36;
				} else {
					index = (c-'0')%10;
				}
				num += (long)(index * (Math.pow(scale, i)));
			}
			if(negative) num=-num;
		}
		return num;
	}
}

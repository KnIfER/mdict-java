package com.knziha.plod.dictionary.Utils;

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
    static final Pattern hanziDelimeter = Pattern.compile("[十|百|千|万]",Pattern.DOTALL);
    public static final Pattern hanshuzi = Pattern.compile("[一七三两九二五八六四零十百千万]{1,}",Pattern.DOTALL);
    public static final Pattern shuzi = Pattern.compile("[0-9]{1,}",Pattern.DOTALL);
    static final Pattern supportedHanShuZi = Pattern.compile("[十|百|千|万]",Pattern.DOTALL);
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


}

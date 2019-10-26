package com.knziha.plod.dictionaryBuilder.Utils;
import java.io.IOException;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import test.CMN;
public class  CU{//CharacterUtils
	//static Pattern hanziDelimeter = Pattern.compile("[十|百|千|万]",Pattern.DOTALL);
	static Pattern supportedHanShuZi = Pattern.compile("^[十 百 千 万 一 七 三 两 九 二 五 八 六 四 零]{1,}$",Pattern.DOTALL);
    static final String[] numOrder = {"一","七","三","两","九","二","五","八","六","四","零"};
    static final int[] Numbers = {1,7,3,2,9,2,5,8,6,4,0};
    static final int[] Levels = {1,10,100,1000,10000};
	
	
	//一亿三千两百 一万四百一
	/*
	 * paramter end should be a valid index in the String.
	 * calculate a Chinese Number that is less that 1*10^8 (九千九百九十九万九千九百九十九). to extend to 亿  and 兆， one have to wrap this and introduce more complex condition judgment.
	 *but actually you can represent 1亿 by using some unusual notations such as '一万万' and my algorithm still recognize it well.
	 */
	public static int recurse1wCalc(String in) {
		if(supportedHanShuZi.matcher(in).find()==true)
			return recurse1wCalc(in,0,in.length()-1,1);
		return -1;
	}
    
	
	
	public static void main(String[] args) throws IOException {
		CMN.show(""+recurse1wCalc("三一"));
	}
	
	private static int recurse1wCalc(String in,int start,int end,int CurrentLvMPlyer) {
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
	
	
	private static long recurse1wCalcLong(String in,int start,int end,int CurrentLvMPlyer) {
		long _CurrentLvMultiplyer=CurrentLvMPlyer;
		long _CurrentLv=0;
		long ret=0;
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


	public static String getRandomString(int length){
		Random random=new Random();
		StringBuilder sb=new StringBuilder();
		for(int i=0; i<length; i++){
			switch(random.nextInt(3)){
				case 0:
					sb.append((char) Math.round(Math.random()*25+65));
					break;
				case 1: //a-z ASCII码
					sb.append((char) Math.round(Math.random()*25+97));
					break;
				case 2: //0-9 数字
					sb.append(new Random().nextInt(10));
					break;
			}
		}
		return sb.toString();
	}
}
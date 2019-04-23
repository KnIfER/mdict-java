package com.knziha.plod.dictionaryBuilder;

import java.io.IOException;

import com.knziha.plod.dictionary.Utils.SU;

import test.CMN;

public class ssCpr_cnnum<T2 extends Comparable<T2>> implements Comparable<ssCpr_cnnum<T2>>{
    	public String key;
    	public T2 value;
    	//private final String emptyStr = "";
    	public ssCpr_cnnum(String k,T2 v){
    		key=k;value=v;
    	}
    	public int compareTo(ssCpr_cnnum<T2> other) {
    		OUT:
    		if(this.key.endsWith(">")&&other.key.endsWith(">")) {
    			int lastIdx1=this.key.lastIndexOf("<");
    			if(lastIdx1==-1||lastIdx1<this.key.length()-20||lastIdx1==this.key.length()-2) {
    				break OUT;
    			}
    			int lastIdx2=other.key.lastIndexOf("<");
    			if(lastIdx2==-1||lastIdx2<other.key.length()-20||lastIdx2==other.key.length()-2) {
    				break OUT;
    			}
    			if(lastIdx1!=lastIdx2)
    				break OUT;

    			int cpr1 = SU.compareTo(this.key,other.key,0,lastIdx1);
    			if(cpr1!=0) break OUT;
    			
    			int num1 = CU.recurse1wCalc(this.key.substring(lastIdx1+1, this.key.length()-1));
    			if(num1==-1) break OUT;
    			int num2 = CU.recurse1wCalc(other.key.substring(lastIdx2+1, other.key.length()-1));
    			if(num2==-1) break OUT;
    			//CMN.show(num1+"~~"+num2);
    			return num1-num2;
    		}
    		
    		int ret = this.key.compareTo(other.key);
    		
    		
    		return ret==0?this.value.compareTo(other.value):ret;
    	}
    	public String toString(){
    		return key+"_"+value;
    	}
    	
    	
    	public static void main(String[] args) throws IOException {
    		CMN.show(""+new ssCpr_cnnum<>("哈市大家<十九>","").compareTo(new ssCpr_cnnum<>("哈市大家<二>","")));
    	}
    }
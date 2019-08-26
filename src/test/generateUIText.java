package test;

import com.knziha.rbtree.ParralelListTree;
import org.apache.commons.lang.StringEscapeUtils;

import java.util.ArrayList;

/**
 * 2019
 * @author KnIfER
 * @date 2019/08/26
 */
public class generateUIText {
    public static void main(String[] args){
		CMN.Log("view="+StringEscapeUtils.escapeJava("视图"));
		CMN.Log("open="+StringEscapeUtils.escapeJava("打开"));

		CMN.Log("advsearch="+StringEscapeUtils.escapeJava("高级搜索"));
		CMN.Log("wildmatch="+StringEscapeUtils.escapeJava("词条通配"));
		CMN.Log("hintwm="+StringEscapeUtils.escapeJava("通配符：. *"));
		CMN.Log("fulltext="+StringEscapeUtils.escapeJava("全文搜索"));
		CMN.Log("dict="+StringEscapeUtils.escapeJava("词典"));
		CMN.Log("set="+StringEscapeUtils.escapeJava("配置"));
		CMN.Log("manager="+StringEscapeUtils.escapeJava("词典管理中心 🚩"));
		CMN.Log("file="+StringEscapeUtils.escapeJava("文件"));
		CMN.Log("browser="+StringEscapeUtils.escapeJava("用浏览器打开..."));



    }

    static class StringEscapeUtilss{
		public static String escapeJava(String s) {
			return "";
		}
	}
}



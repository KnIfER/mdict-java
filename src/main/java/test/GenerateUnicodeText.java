package test;

import com.knziha.rbtree.ParralelListTree;
import javafx.scene.control.MenuItem;
import org.apache.commons.text.StringEscapeUtils;

import java.util.ArrayList;

/**
 * 2019
 * @author KnIfER
 * @date 2019/08/26
 */
public class GenerateUnicodeText {
    public static void main(String[] args){
    	CMN.ConfigLogFile("D:\\Code\\tests\\recover_wrkst\\mdict-java\\src\\main\\java\\UIText_zh.properties", false);
		CMN.Log("view="+ StringEscapeUtils.escapeJava("视图"));
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
		CMN.Log("searchpage="+StringEscapeUtils.escapeJava("页内查找"));
		CMN.Log("name="+StringEscapeUtils.escapeJava("名称"));
		CMN.Log("relpath="+StringEscapeUtils.escapeJava("相对路径"));
		CMN.Log("filesize="+StringEscapeUtils.escapeJava("大小(MB)"));
		CMN.Log("add="+StringEscapeUtils.escapeJava("添加"));
		CMN.Log("commit="+StringEscapeUtils.escapeJava("提交"));
		CMN.Log("reset="+StringEscapeUtils.escapeJava("重置"));
		CMN.Log("refresh="+StringEscapeUtils.escapeJava("刷新"));
		CMN.Log("rename="+StringEscapeUtils.escapeJava("重命名"));
		CMN.Log("saveas="+StringEscapeUtils.escapeJava("另存为"));
		CMN.Log("switchset="+StringEscapeUtils.escapeJava("切换分组"));
		CMN.Log("cancelmod="+StringEscapeUtils.escapeJava("取消更改"));
		CMN.Log("disable="+StringEscapeUtils.escapeJava("禁用"));
		CMN.Log("enable="+StringEscapeUtils.escapeJava("启用"));
		CMN.Log("remove="+StringEscapeUtils.escapeJava("移除"));
		CMN.Log("setasformation="+StringEscapeUtils.escapeJava("设为构词库"));
		CMN.Log("openlocation="+StringEscapeUtils.escapeJava("打开文件夹"));
		CMN.Log("mainfolder="+StringEscapeUtils.escapeJava("选择主文件夹"));
		CMN.Log("ow_browser="+StringEscapeUtils.escapeJava("覆写浏览器路径： "));
		CMN.Log("ow_search="+StringEscapeUtils.escapeJava("覆写浏览器搜索链接： "));
		CMN.Log("ow_bsrarg="+StringEscapeUtils.escapeJava("浏览器启动参数： "));
		CMN.Log("ow_pdf="+StringEscapeUtils.escapeJava("覆写Pdf阅读器路径： "));
		CMN.Log("ow_pdfarg="+StringEscapeUtils.escapeJava("覆写Pdf阅读器启动参数： "));
		CMN.Log("pdffolders="+StringEscapeUtils.escapeJava("管理PDF包含文件夹"));
		CMN.Log("setastree="+StringEscapeUtils.escapeJava("设为文件树"));
		CMN.Log("switchdict="+StringEscapeUtils.escapeJava("切换词典"));
		CMN.Log("settings="+StringEscapeUtils.escapeJava("设置"));
		CMN.Log("s_invalid="+StringEscapeUtils.escapeJava("选择失效项"));
		CMN.Log("s_disabled="+StringEscapeUtils.escapeJava("选择禁用项"));
		CMN.Log("s_all="+StringEscapeUtils.escapeJava("全选"));
		CMN.Log("s_none="+StringEscapeUtils.escapeJava("全不选"));
		//CMN.Log("settings="+StringEscapeUtils.escapeJava("关键词"));
		CMN.Log("pagewutsp="+StringEscapeUtils.escapeJava("通配符排除空格"));
		CMN.Log("tintwild="+StringEscapeUtils.escapeJava("为通配搜索结果染色"));
		CMN.Log("remwsize="+StringEscapeUtils.escapeJava("记忆窗口大小"));
		CMN.Log("remwpos="+StringEscapeUtils.escapeJava("记忆窗口位置"));
		CMN.Log("dirload="+StringEscapeUtils.escapeJava("直接读取分组配置"));
		CMN.Log("doclsset="+StringEscapeUtils.escapeJava("双击切换分组后关闭对话框"));
		CMN.Log("doclsdict="+StringEscapeUtils.escapeJava("双击切换词典后关闭对话框"));
		CMN.Log("dt_setting="+StringEscapeUtils.escapeJava("设置和主界面解耦"));
		CMN.Log("dt_advsrch="+StringEscapeUtils.escapeJava("高级搜索和主界面解耦"));
		CMN.Log("dt_dictpic="+StringEscapeUtils.escapeJava("切换词典和主界面解耦"));
		//CMN.Log("updateonpic="+StringEscapeUtils.escapeJava("切换词典立即更新网页"));
		CMN.Log("tintfull="+StringEscapeUtils.escapeJava("为全文搜索结果染色"));
		CMN.Log("autopaste="+StringEscapeUtils.escapeJava("激活界面时自动使用剪贴板"));
		CMN.Log("filterpaste="+StringEscapeUtils.escapeJava("不粘贴路径和链接"));
		CMN.Log("ow_search1="+StringEscapeUtils.escapeJava("浏览器搜索链接 (鼠标中键)： "));
		CMN.Log("ow_search2="+StringEscapeUtils.escapeJava("浏览器搜索链接 (鼠标右键)： "));
		CMN.Log("regex_enable="+StringEscapeUtils.escapeJava("启用正则搜索引擎"));
		CMN.Log("regex_config="+StringEscapeUtils.escapeJava("配置正则搜索引擎"));
		CMN.Log("ps_regex="+StringEscapeUtils.escapeJava("页内查找启用正则表达式"));
		CMN.Log("ps_separate="+StringEscapeUtils.escapeJava("按空格分割关键词"));
		CMN.Log("regex_head="+StringEscapeUtils.escapeJava("自动添加 .* 头"));
		CMN.Log("regex_case="+StringEscapeUtils.escapeJava("区分大小写"));
		CMN.Log("p_regex_case="+StringEscapeUtils.escapeJava("区分大小写"));
		CMN.Log("todo="+StringEscapeUtils.escapeJava("待定"));
		CMN.Log("onegine="+StringEscapeUtils.escapeJava("Oniguruma 正则引擎"));
		CMN.Log("findpage="+StringEscapeUtils.escapeJava("Webview 页内搜索"));
		CMN.Log("regex_fuzzy="+StringEscapeUtils.escapeJava("词条检索时启用"));
		CMN.Log("regex_full="+StringEscapeUtils.escapeJava("全文检索时启用"));
		//CMN.Log("ow_search2="+StringEscapeUtils.escapeJava("^和$在行间匹配(多行模式)"));
		//CMN.Log("ow_search2="+StringEscapeUtils.escapeJava(".匹配任意字符(包括换行符)"));
		CMN.Log("class_case="+StringEscapeUtils.escapeJava("旧版Mdict大小写转换"));
		CMN.Log("sr_inter="+StringEscapeUtils.escapeJava("中断搜索"));
		CMN.Log("sr_save="+StringEscapeUtils.escapeJava("保存搜索…"));
		CMN.Log("sr_new="+StringEscapeUtils.escapeJava("新建搜索域"));
		CMN.ConfigLogFile(null);
    }

    //生成生成代码
	//(.*)[ ](.*)
    //CMN.Log\("$2="+StringEscapeUtils.escapeJava\("$1"\)\);

	//生成变量
	//(.*)=(.*)
	//public static final String $1 = "$1";

	//生成switch
	//(.*)=(.*)
	//case $1:\r\nbreak;


    static class StringEscapeUtilss{
		public static String escapeJava(String s) {
			return "";
		}
	}
}



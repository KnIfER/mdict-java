package com.knziha.plod.PlainDict;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "opt")
public class PlainDictAppOptions
{
	public static String projectPath;
	public static String userPath;
	private long FirstFlag=0;
	private double searchBoxPercent=61;
	private int mScreenW=1250;
	private int mScreenH=810;
	private int mScreenX=-1;
	private int mScreenY=-1;
	String lastMdlibPath=null;
	String browserPathOverwrite=null;
	String browserArgs=null;
	String browserSearchUrlOverwrite=null;
	String browserSearchMiddle=null;
	String browserSearchRight=null;
	String PdfPath=null;
	String PdfArgs=null;
	String BsrArgs=null;
	String currentSetName="default";
	static String SearchUrlDefault = "https://www.baidu.com/#ie={inputEncoding}&wd=%s";
	public PlainDictAppOptions(){

	}


	@XmlElement(name = "topPercent")
	public double getSearchBoxPercent() {
		return searchBoxPercent;
	}

	public void setSearchBoxPercent(double val) {
		searchBoxPercent=val;
	}

	/////////////////////////////////////////start first flag/////////////////////////////////////////
	@XmlElement(name = "MFF")
	public long getFirstFlag() {
		return FirstFlag;
	}

	private void setFirstFlag(long val) {
		FirstFlag=val;
	}

	private void updateFFAt(long o, boolean val) {
		FirstFlag &= (~o);
		if(val) FirstFlag |= o;
		//defaultReader.edit().putInt("MFF",FirstFlag).commit();
	}


	public boolean GetCombinedSearching() {
		return (FirstFlag & 1) == 1;
	}
	public boolean SetCombinedSearching(boolean val) {
		updateFFAt(1,val);
		return val;
	}

	public boolean GetAdvCombinedSearching() {
		return (FirstFlag & 2) == 2;
	}
	public boolean SetAdvCombinedSearching(boolean val) {
		updateFFAt(2,val);
		return val;
	}
	public boolean GetSearchInPage() {
		return (FirstFlag & 0x4) == 0x4;
	}
	public boolean SetSearchInPage(boolean val) {
		updateFFAt(0x4,val);
		return val;
	}
	public boolean GetShowAdvanced() {
		return (FirstFlag & 0x8) == 0x8;
	}
	public boolean SetShowAdvanced(boolean val) {
		updateFFAt(0x8,val);
		return val;
	}

	public boolean GetBrowserPathOverwriteEnabled() {
		return (FirstFlag & 0x10) == 0x10;
	}
	public boolean SetBrowserPathOverwriteEnabled(boolean val) {
		updateFFAt(0x10,val);
		return val;
	}
	public boolean GetSearchUrlOverwriteEnabled() {
		return (FirstFlag & 0x20) == 0x20;
	}
	public boolean SetSearchUrlOverwritEnabled(boolean val) {
		updateFFAt(0x20,val);
		return val;
	}
	public boolean GetPdfOverwriteEnabled() {
		return (FirstFlag & 0x40) == 0x40;
	}
	public boolean SetPdfOverwriteEnabled(boolean val) {
		updateFFAt(0x40,val);
		return val;
	}
	public boolean GetPdfArgsOverwriteEnabled() {
		return (FirstFlag & 0x80) == 0x80;
	}
	public boolean SetPdfArgsOverwriteEnabled(boolean val) {
		updateFFAt(0x80,val);
		return val;
	}


	public boolean GetDirectSetLoad() {
		return (FirstFlag & 0x100) != 0x100;
	}
	public boolean SetDirectSetLoad(boolean val) {
		updateFFAt(0x100,!val);
		return val;
	}

	public boolean GetRemWindowPos() {
		return (FirstFlag & 0x200) != 0x200;
	}
	public boolean SetRemWindowPos(boolean val) {
		updateFFAt(0x200,!val);
		return val;
	}

	public boolean GetRemWindowSize() {
		return (FirstFlag & 0x400) != 0x400;
	}
	public boolean SetRemWindowSize(boolean val) {
		updateFFAt(0x400,!val);
		return val;
	}

	public boolean GetDoubleClickCloseSet() {
		return (FirstFlag & 0x800) == 0x800;
	}
	public boolean SetDoubleClickCloseSet(boolean val) {
		updateFFAt(0x800,val);
		return val;
	}

	public boolean GetDoubleClickCloseDict() {
		return (FirstFlag & 0x1000) != 0x1000;
	}
	public boolean SetDoubleClickCloseDict(boolean val) {
		updateFFAt(0x1000,!val);
		return val;
	}

	public boolean GetTintWildResult() {
		return (FirstFlag & 0x2000) != 0x2000;
	}
	public boolean SetTintWildResult(boolean val) {
		updateFFAt(0x2000,!val);
		return val;
	}

	//xxx
//	public boolean GetWildWithoutSpace() {
//		return (FirstFlag & 0x4000) == 0x4000;
//	}
//	public boolean SetWildWithoutSpace(boolean val) {
//		updateFFAt(0x4000,val);
//		return val;
//	}

//	public boolean GetFullWithoutSpace() {
//		return (FirstFlag & 0x8000) != 0x8000;
//	}
//	public boolean SetFullWithoutSpace(boolean val) {
//		updateFFAt(0x8000,!val);
//		return val;
//	}

	public boolean GetPageWithoutSpace() {
		return (FirstFlag & 0x10000) == 0x10000;
	}
	public int FetPageWithoutSpace() {
		return (int) (((FirstFlag & 0x10000)>>16)&1);
	}
	public boolean SetPageWithoutSpace(boolean val) {
		updateFFAt(0x10000,val);
		return val;
	}

	public boolean GetDetachSettings() {
		return (FirstFlag & 0x20000) == 0x20000;
	}
	public boolean SetDetachSettings(boolean val) {
		updateFFAt(0x20000,val);
		return val;
	}

	public boolean GetDetachAdvSearch() {
		return (FirstFlag & 0x40000) == 0x40000;
	}
	public boolean SetDetachAdvSearch(boolean val) {
		updateFFAt(0x40000,val);
		return val;
	}

	public boolean GetDetachDictPicker() {
		return (FirstFlag & 0x80000) == 0x80000;
	}
	public boolean SetDetachDictPicker(boolean val) {
		updateFFAt(0x80000,val);
		return val;
	}

	public boolean GetTintFullResult() {
		return (FirstFlag & 0x100000) == 0x100000;
	}
	public boolean SetTintFullResult(boolean val) {
		updateFFAt(0x100000,val);
		return val;
	}

	public boolean GetAutoPaste() {
		return (FirstFlag & 0x200000) == 0x200000;
	}
	public boolean SetAutoPaste(boolean val) {
		updateFFAt(0x200000,val);
		return val;
	}
	public boolean GetFilterPaste() {
		return (FirstFlag & 0x400000) != 0x400000;
	}
	public boolean SetFilterPaste(boolean val) {
		updateFFAt(0x400000,!val);
		return val;
	}

	public boolean GetRegexSearchEngineEnabled() {
		return (FirstFlag & 0x800000) != 0x800000;
	}
	public boolean SetRegexSearchEngineEnabled(boolean val) {
		updateFFAt(0x800000,!val);
		return val;
	}

	public boolean GetRegexSearchEngineAutoAddHead() {
		return (FirstFlag & 0x1000000) != 0x1000000;
	}
	public boolean SetRegexSearchEngineAutoAddHead(boolean val) {
		updateFFAt(0x1000000,!val);
		return val;
	}

	public boolean GetRegexSearchEngineCaseSensitive() {
		return (FirstFlag & 0x2000000) == 0x2000000;
	}
	public boolean SetRegexSearchEngineCaseSensitive(boolean val) {
		updateFFAt(0x2000000,val);
		return val;
	}

	public boolean GetPageSearchSeparateWord() {
		return (FirstFlag & 0x4000000) != 0x4000000;
	}
	public int FetPageSearchSeparateWord() {
		return (int) (~((FirstFlag & 0x4000000)>>26)&1);
	}
	public boolean SetPageSearchSeparateWord(boolean val) {
		updateFFAt(0x4000000,!val);
		return val;
	}

	public boolean GetPageSearchCaseSensitive() {
		return (FirstFlag & 0x8000000) == 0x8000000;
	}
	public int FetPageSearchCaseSensitive() {
		return (int) (((FirstFlag & 0x8000000)>>27)&1);
	}
	public boolean SetPageSearchCaseSensitive(boolean val) {
		updateFFAt(0x8000000,val);
		return val;
	}

	public boolean GetPageSearchUseRegex() {
		return (FirstFlag & 0x10000000) != 0x10000000;
	}
	public int FetPageSearchUseRegex() {
		return (int) (~((FirstFlag & 0x10000000)>>28)&1);
	}
	public boolean SetPageSearchUseRegex(boolean val) {
		updateFFAt(0x10000000,!val);
		return val;
	}


	public boolean GetClassicalKeyCaseStrategy() {
		return (FirstFlag & 0x20000000) == 0x20000000;
	}
	public boolean SetClassicalKeyCaseStrategy(boolean val) {
		updateFFAt(0x20000000,val);
		return val;
	}


	/////////////////////////////////////////end first flag/////////////////////////////////////////

	public int getScreenW(){
		return mScreenW;
	}
	public void setScreenW(int val){
		mScreenW=val;
	}
	public int getScreenH(){
		return mScreenH;
	}
	public void setScreenH(int val){
		mScreenH=val;
	}
	public int getScreenX(){
		return mScreenX;
	}
	public void setScreenX(int val){
		mScreenX=val;
	}
	public int getScreenY(){
		return mScreenY;
	}
	public void setScreenY(int val){
		mScreenY=val;
	}
	@Deprecated
	public String getLastMdlibPath(){
		return lastMdlibPath;
	}
	public String GetLastMdlibPath(){
		if(lastMdlibPath==null)
			return projectPath;
		return lastMdlibPath;
	}
	public void setLastMdlibPath(String val){
		lastMdlibPath=val;
	}
	public String getCurrentPlanName() {
		return currentSetName;
	}
	public void setCurrentPlanName(String val){
		currentSetName=val;
	}


	@XmlElement(name = "BrsExe")
	public String getBrowserPathOverwrite(){
		return browserPathOverwrite;
	}
	public void setBrowserPathOverwrite(String val){
		browserPathOverwrite=val;
	}
	@Deprecated
	@XmlElement(name = "SrhUrl")
	public String getSearchUrlOverwrite(){
		return browserSearchUrlOverwrite;
	}
	public String GetSearchUrlOverwrite(){
		return browserSearchUrlOverwrite==null?SearchUrlDefault:browserSearchUrlOverwrite;
	}
	public void setSearchUrlOverwrite(String val){
		browserSearchUrlOverwrite=val;
	}

	@Deprecated
	@XmlElement(name = "SrhUrl1")
	public String getSearchUrlMiddle(){
		return browserSearchMiddle;
	}
	public String GetSearchUrlMiddle(String SearchUrlDefault){
		return browserSearchMiddle==null?SearchUrlDefault:browserSearchMiddle;
	}
	public void setSearchUrlMiddle(String val){
		browserSearchMiddle=val;
	}

	@Deprecated
	@XmlElement(name = "SrhUrl2")
	public String getSearchUrlRight(){
		return browserSearchRight;
	}
	public String GetSearchUrlRight(String SearchUrlDefault){
		return browserSearchRight==null?SearchUrlDefault:browserSearchRight;
	}
	public void setSearchUrlRight(String val){
		browserSearchRight=val;
	}

	@Deprecated
	@XmlElement(name = "PdfExe")
	public String getPdfOverwrite(){
		return PdfPath;
	}
	public String GetPdfOverwrite(){
		return PdfPath==null?"acrobat":PdfPath;
	}
	public void setPdfOverwrite(String val){
		PdfPath=val;
	}
	@Deprecated
	@XmlElement(name = "PdfArgs")
	public String getPdfArgsOverwrite(){
		return PdfArgs;
	}
	public String GetPdfArgsOverwrite(){
		return PdfArgs==null?"/A&page=$P":PdfArgs;
	}
	public void setPdfArgsOverwrite(String val){
		PdfArgs=val;
	}
	@XmlElement(name = "BsrArgs")
	public String getBrowserArgs(){
		return BsrArgs;
	}
	public void setBrowserArgs(String val){
		BsrArgs=val;
	}
}
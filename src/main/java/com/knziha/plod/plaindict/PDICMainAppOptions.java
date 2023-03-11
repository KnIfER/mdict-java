package com.knziha.plod.plaindict;

public class PDICMainAppOptions {
	public static boolean getAllowPlugRes() {
		return false;
	}

	public static boolean getAllowPlugResNone() {
		return false;
	}

	public static boolean getAllowPlugResSame() {
		return false;
	}

	public static boolean getAllowPlugCss() {
		return false;
	}

	public static boolean debugCss() {
		return false;
	}

	public static boolean getNotificationEnabled() {
		return false;
	}

	String _rootPath = "D:\\assets\\";

	public static boolean allowMergeSytheticalPage() {
		return true;
	}

	public static boolean isSingleThreadServer() {
		return false;
	}

	public final String GetPathToMainFolder() {
		// _rootPath = rootPath + "/" + CMN.BrandName + "/";
		return _rootPath;
	}

	public String DarkModeIncantation() {
		return null;
	}
}

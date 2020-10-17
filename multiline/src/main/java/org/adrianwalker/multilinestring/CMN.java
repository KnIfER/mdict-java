package org.adrianwalker.multilinestring;

import com.sun.tools.javac.tree.JCTree;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;


//common
class CMN {
	public static void Log(Object... o) {
		StringBuilder msg= new StringBuilder();
		if(o!=null)
		for (Object o1 : o) {
			if(o1!=null) {
				if (Exception.class.isInstance(o1)) {
					ByteArrayOutputStream s = new ByteArrayOutputStream();
					PrintStream p = new PrintStream(s);
					((Exception) o1).printStackTrace(p);
					msg.append(s.toString());
				}
				
				String classname = o1.getClass().getName();
				if (classname.equals("[I")) {
					int[] arr = (int[]) o1;
					for (int os : arr) {
						if (msg.length() > 0) msg.append(", ");
						msg.append(os);
					}
					continue;
				} else if (classname.equals("[Ljava.lang.String;")) {
					String[] arr = (String[]) o1;
					for (String os : arr) {
						if (msg.length() > 0) msg.append(", ");
						msg.append(os);
					}
					continue;
				} else if (classname.equals("[S")) {
					short[] arr = (short[]) o1;
					for (short os : arr) {
						if (msg.length() > 0) msg.append(", ");
						msg.append(os);
					}
					continue;
				} else if (classname.equals("[B")) {
					byte[] arr = (byte[]) o1;
					for (byte os : arr) {
						if (msg.length() > 0) msg.append(", ");
						msg.append(Integer.toHexString(os));
					}
					continue;
				} else if (o1 instanceof JCTree) {
					msg.append(o1).append(" | ").append(o1.getClass()).append(" | ").append(((JCTree)o1).getTag());
					continue;
				}
			}
			if(msg.length()>0) msg.append(", ");
			msg.append(o1);
		}
		System.out.println("fatal poison "+msg.toString());
	}
}
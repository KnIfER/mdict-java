package test;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;


//common
public class CMN{
    public static String UniversalObject;
	public static void show(String val){System.out.println(val);}
    public final static String replaceReg = " |:|\\.|,|-|\'";
    public final static String emptyStr = "";
    public static void Log(Object... o) {
        StringBuilder msg= new StringBuilder();
        if(o!=null)
            for (Object o1 : o) {
                //android.util.Log.d("fatal",o[i].getClass().getName());
                if (o1 != null) {
                    if (o1 instanceof Exception) {
                        ByteArrayOutputStream s = new ByteArrayOutputStream();
                        PrintStream p = new PrintStream(s);
                        ((Exception) o1).printStackTrace(p);
                        msg.append(s.toString());
                        continue;
                    }

                    List oi = null;
                    String classname = o1.getClass().getName();
                    switch (classname) {
                        case "[I": {
                            int[] arr = (int[]) o1;
                            for (int os : arr) {
                                msg.append(os);
                                msg.append(", ");
                            }
                            continue;
                        }
                        case "[Ljava.lang.String;": {
                            String[] arr = (String[]) o1;
                            for (String os : arr) {
                                msg.append(os);
                                msg.append(", ");
                            }
                            continue;
                        }
                        case "[S": {
                            short[] arr = (short[]) o1;
                            for (short os : arr) {
                                msg.append(os);
                                msg.append(", ");
                            }
                            continue;
                        }
                        case "[B": {
                            byte[] arr = (byte[]) o1;
                            for (byte os : arr) {
                                msg.append(Integer.toHexString(os));
                                msg.append(", ");
                            }
                            continue;
                        }
                    }

                }


                msg.append(o1).append(o.length>1?" ":"");
            }
        if(fout!=null) {
            try {
                fout.write((msg+"\r\n").getBytes(StandardCharsets.UTF_8));
            } catch (IOException ignored) { }
        }
        System.out.println(msg.toString());
    }

    static FileOutputStream fout;
    public static void ConfigLogFile(String path, boolean...a) {
        try {
            if(fout!=null){
                fout.flush();
                fout.close();
                fout=null;
            }
            if(path!=null)
                fout=new FileOutputStream(path, a[0]);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static long stst;
    public static void rt() {
        stst = System.currentTimeMillis();
    }
    public static void pt(String...args) {
        CMN.Log(args,(System.currentTimeMillis()-stst));
    }
}
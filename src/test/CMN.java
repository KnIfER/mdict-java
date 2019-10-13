package test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;


//common
public class CMN{
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
        System.out.println(msg.toString());
    }
    
    
}
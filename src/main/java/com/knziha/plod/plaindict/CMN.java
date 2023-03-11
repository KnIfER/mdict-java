package com.knziha.plod.plaindict;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
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
        if(fout!=null) {
            try {
                fout.write((msg+"\r\n").getBytes(StandardCharsets.UTF_8));
            } catch (IOException ignored) { }
        }
        System.out.println(msg.toString());
    }
    public static Object hotTracingObject;

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

    static volatile long stst;
    public static void rt() {
        stst = System.currentTimeMillis();
    }
    public static void pt(String...args) {
        CMN.Log(args,(System.currentTimeMillis()-stst));
    }

    public static void pt_mins(String...args) {
        CMN.Log(args,((System.currentTimeMillis()-stst)/1000.f/60)+"m");
    }

    public static HashSet<String> AdaptivelyGetAllLines(String path, HashSet<String> proessed) {
        if(proessed==null) proessed=new HashSet<>();
        try {
            BufferedReader br = new BufferedReader(new FileReader(path));
            String line;
            while((line=br.readLine())!=null){
                line=line.trim();
                if(line.length()>0)
                    proessed.add(line);
            }
        } catch (Exception e) { }
        return proessed;
    }

    public static ArrayList<String> AdaptivelyGetAllLines(ArrayList<String> proessed, String path) {
        if(proessed==null) proessed=new ArrayList<>();
        try {
            BufferedReader br = new BufferedReader(new FileReader(path));
            String line;
            while((line=br.readLine())!=null){
                line=line.trim();
                if(line.length()>0)
                    proessed.add(line);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return proessed;
    }

    public static void AdaptivelyLogFiles(HashMap<String, FileOutputStream> cache, boolean append, String path, String msg) {
        try {
            FileOutputStream fin = cache.get(path);
            if(fin==null){
                fin = new FileOutputStream(path, append);
                cache.put(path, fin);
            }
            fin.write((msg+"\r\n").getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void AdaptivelyLogFiles(HashMap<String, FileOutputStream> cache) {
        try {
            for(String key:cache.keySet()){
                cache.get(key).flush();
                cache.get(key).close();
            }
            cache.clear();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static void inline(Object val) {
        System.out.print(val+", ");
    }

    public static void debug(Object... e) {
        CMN.Log(e);
    }

    public static long now() {
        return System.currentTimeMillis();
    }

    public static int id(Object e) {
        return System.identityHashCode(e);
    }
}
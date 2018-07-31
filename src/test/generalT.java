package test;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.zip.Deflater;
import java.util.zip.InflaterOutputStream;

import com.knziha.plod.dictionary.CMN;
import com.knziha.plod.dictionary.ripemd128;
import com.knziha.plod.dictionaryBuilder.mdictBuilder;

/**
 * TEsts
 * @author KnIfER
 * @date 2018/05/31a
 */
public class generalT {
	
	
	
	
	
    public static void main(String[] args) throws UnsupportedEncodingException{
    	
    	ByteBuffer sf = ByteBuffer.wrap(new byte[5*8]);
    	try {
			sf.putLong(10l);
			sf.putLong(10l);
			sf.putLong(10l);
			sf.putLong(10l);
			sf.putInt(10);
			
			sf.putLong(10l);
		} catch (BufferOverflowException e) {
			e.printStackTrace();
			CMN.show(sf.position()+"");
		}
    	CMN.show(sf.position()+"end");
    	
    	
    	Deflater df = new Deflater();
    	byte[] data = "asdfghvd".getBytes();
    	ripemd128.printBytes(data);
    	byte[] out = new byte[1024];
    	df.setInput(data, 0, data.length);
    	df.finish();
    	int ln = df.deflate(out);//压缩进去
    	ripemd128.printBytes(out, 0, ln);
    	byte[] de = zlib_decompress(out,0,ln);
    	ripemd128.printBytes(de);
    	
    	ripemd128.printBytes("哈".getBytes("GBK"));
    	
    	ripemd128.printBytes2("哈".getBytes("GBK"));
    	
    	CMN.show(""+("A".getBytes()[0]-"a".getBytes()[0]));
    	CMN.show(""+("Ж".getBytes()[0]-"ж".getBytes()[0]));
    	
    	ripemd128.printBytes2("Ж".getBytes("utf8"));
    	ripemd128.printBytes2("ж".getBytes("utf8"));

    	ripemd128.printBytes2("Ж".getBytes("utf8"));
    	ripemd128.printBytes2("ж".getBytes("utf8"));
    	ripemd128.printBytes2(" ".getBytes("utf8"));
    	
    	CMN.show(new String(new byte[] {(byte) 208,(byte)182,0,(byte) 208,(byte) 150},"utf8"));
    	
    	ripemd128.printBytes2("Ж".getBytes("utf16"));
    	ripemd128.printBytes2("ж".getBytes("utf16"));
    	ripemd128.printBytes2(" ".getBytes("utf16"));
    	String utf16str = new String(new byte[] {(byte)254,(byte)255,4,22,0,0,(byte)254,(byte)255,4,54,},"utf16");
    	CMN.show(utf16str.toLowerCase()+":"+utf16str.toLowerCase().indexOf(new String(new byte[] {0,0},"utf16")));
    	ripemd128.printBytes2(utf16str.getBytes("utf16"));
    	
    	
    	ripemd128.printBytes2("别树一帜".getBytes("UTF-16"));
    	ripemd128.printBytes2("别树一帜".getBytes("UTF-16LE"));
    	ripemd128.printBytes2("h".getBytes("UTF-16LE"));
    	
    	
    }
    public static byte[] zlib_decompress(byte[] encdata,int offset,int ln) {
	    try {
			    ByteArrayOutputStream out = new ByteArrayOutputStream(); 
			    InflaterOutputStream inf = new InflaterOutputStream(out); 
			    inf.write(encdata,offset, ln); 
			    inf.close(); 
			    return out.toByteArray(); 
		    } catch (Exception ex) {
		    	ex.printStackTrace(); 
		    	return "ERR".getBytes(); 
		    }
    }

}



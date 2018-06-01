package test;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
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
	
	
	
	
	
    public static void main(String[] args){
    	
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



package test;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.zip.Deflater;
import java.util.zip.InflaterOutputStream;

import com.knziha.plod.dictionary.mdict;
import com.knziha.plod.dictionary.Utils.BU;
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
    	BU.printBytes(data);
    	byte[] out = new byte[1024];
    	df.setInput(data, 0, data.length);
    	df.finish();
    	int ln = df.deflate(out);//压缩进去
    	BU.printBytes(out, 0, ln);
    	byte[] de = zlib_decompress(out,0,ln);
    	BU.printBytes(de);
    	
    	BU.printBytes("哈".getBytes("GBK"));
    	
    	BU.printBytes2("哈".getBytes("GBK"));
    	
    	CMN.show(""+("A".getBytes()[0]-"a".getBytes()[0]));
    	CMN.show(""+("Ж".getBytes()[0]-"ж".getBytes()[0]));
    	
    	BU.printBytes2("Ж".getBytes("utf8"));
    	BU.printBytes2("ж".getBytes("utf8"));

    	BU.printBytes2("Ж".getBytes("utf8"));
    	BU.printBytes2("ж".getBytes("utf8"));
    	BU.printBytes2(" ".getBytes("utf8"));
    	
    	CMN.show(new String(new byte[] {(byte) 208,(byte)182,0,(byte) 208,(byte) 150},"utf8"));
    	
    	BU.printBytes2("Ж".getBytes("utf16"));
    	BU.printBytes2("ж".getBytes("utf16"));
    	BU.printBytes2(" ".getBytes("utf16"));
    	String utf16str = new String(new byte[] {(byte)254,(byte)255,4,22,0,0,(byte)254,(byte)255,4,54,},"utf16");
    	CMN.show(utf16str.toLowerCase()+":"+utf16str.toLowerCase().indexOf(new String(new byte[] {0,0},"utf16")));
    	BU.printBytes2(utf16str.getBytes("utf16"));
    	
    	
    	BU.printBytes2("别树一帜".getBytes("UTF-16"));
    	BU.printBytes2("别树一帜".getBytes("UTF-16LE"));
    	BU.printBytes2("h".getBytes("UTF-16LE"));
    	
    	CMN.show(""+"\\js\\aa".compareTo("\\js\\b"));

    	
    	CMN.show(""+"\\js\\renderers\\CanvasRenderer.js".compareTo("\\js\\Three.js"));
    	CMN.show(""+"\\js\\renderers\\Projector.js".compareTo("\\js\\Three.js"));
    	
    	
    	
    	
    	CMN.show("asdasdasd"+"\\js\\renderers\\Projector.js".compareTo("\\js\\Three.js"));
    	
    	
    	String testData = "hello world word haWa垃圾 katsuki naku 和 乐 君 卿 寝室是真实的";
    	byte[] testDataArr = testData.getBytes(_encoding);
    	String key = "hellO".toLowerCase();
    	byte[][][] matcher = new byte[2][][];
    	matcher[0] = SanLieZhi(key);
    	String upperKey = key.toUpperCase();
    	if(!upperKey.equals(key))
    		matcher[1] = SanLieZhi(upperKey);
		for(byte[] xxx:matcher[0])
    		BU.printBytes(xxx);
		BU.printBytes(key.getBytes(_encoding));
    	
		CMN.show("bingStartWith"+bingStartWith(testDataArr,0,"haWa".getBytes(_encoding),0,-1,0));
    
		CMN.show("EntryStartWith"+EntryStartWith(testDataArr, 0, testDataArr.length, matcher));

		
	CMN.show("bbb"+binary_find_closest(new int[] {1,2,3,4,5,6,7,8,9,10,11,12,14,14},14,-1));
	
		String Fuzzykey = "haWa";
		//CMN.show("Fuzzykey"+EntryStartWith(testDataArr, 0, testDataArr.length, matcher));
		
		
		BU.printBytes("guppy".getBytes("utf8"));
		BU.printBytes("h".getBytes("utf8"));
		
		BU.printBytes("简".getBytes("utf8"));
		BU.printBytes("极简".getBytes("utf8"));
		try {
			mdict m = new mdict("H:\\antiquafortuna\\MDictPC\\doc\\简明英汉汉英词典.mdx");
			m.flowerFindAllContents("简", 0, 0);
			//BU.printBytes(m.getRecordAt(m.lookUp("简")).getBytes(s));
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		String i=null;
		
		
		CMN.show(new  StringBuilder().append(i).toString());
		
		
		BU.printBytes("七".getBytes("utf8"));
		
		
		BU.printBytes("七".getBytes());
		
		
    }
    
    

    public static int  binary_find_closest(int[] array,int val,int iLen){
    	int middle = 0;
    	if(iLen==-1||iLen>array.length)
    		iLen = array.length;
    	int low=0,high=iLen-1;
    	if(array==null || iLen<1){
    		return -1;
    	}
    	if(iLen==1){
    		return 0;
    	}
    	if(val-array[0]<=0){
			return 0;
    	}else if(val-array[iLen-1]>0){
    		return iLen-1;
    	}
    	int counter=0;
    	long cprRes1,cprRes0;
    	while(low<high){
    		//CMN.show(low+"~"+high);
    		counter+=1;
    		System.out.println(low+":"+high);
    		middle = (low+high)/2;
    		cprRes1=array[middle+1]-val;
        	cprRes0=array[middle  ]-val;
        	if(cprRes0>=0){
        		high=middle;
        	}else if(cprRes1<=0){
        		//System.out.println("cprRes1<=0 && cprRes0<0");
        		//System.out.println(houXuan1);
        		//System.out.println(houXuan0);
        		low=middle+1;
        	}else{
        		//System.out.println("asd");
        		//high=middle;
        		low=middle+1;//here
        	}
    	}
		return low;
    }
    
    
    //static String _encoding = "UTF-16";//fe,ff开头
    static String _encoding = "UTF-16LE";//无开头
    //static String _encoding = "GBK";//无开头
    
    private static byte[][] SanLieZhi(String str) throws UnsupportedEncodingException {
    	byte[][] res = new byte[str.length()][];
    	for(int i=0;i<str.length();i++){
    		String c = str.substring(i, i+1);
    		res[i] = c.getBytes(_encoding);
		}
		return res;
	}
    
    static boolean EntryStartWith(byte[] source, int sourceOffset, int sourceCount, byte[][][] matchers) {
		boolean Matched = false;
		int fromIndex=0;
		CMN.show("matching!");
    	for(int lexiPartIdx=0;lexiPartIdx<matchers[0].length;lexiPartIdx++) {
    		Matched = false;
    		for(byte[][] marchLet:matchers) {
    			if(marchLet==null) break;
    			if(bingStartWith(source,sourceOffset,marchLet[lexiPartIdx],0,-1,fromIndex)) {
    				Matched=true;
    			}
    		}
    		if(!Matched)
    			return false;
    		fromIndex+=matchers[0][lexiPartIdx].length;
    	}
    	return true;
    }


    static boolean bingStartWith(byte[] source, int sourceOffset,byte[] target, int targetOffset, int targetCount, int fromIndex) {
    	if (fromIndex >= source.length) {
    		return false;
        }
    	if(targetCount<=-1)
    		targetCount=target.length;
    	if(sourceOffset+targetCount>=source.length)
        	return false;
    	for (int i = sourceOffset + fromIndex; i <= sourceOffset+fromIndex+targetCount-1; i++) {
    		if (source[i] != target[targetOffset+i-sourceOffset-fromIndex]) 
    			return false;
    	}
    	return true;
    }


    static int bingIndexOf(byte[] source, int sourceOffset, int sourceCount, byte[][][] matcher) {
    	for(byte[][] marcherLet:matcher) {
    		
    		
    	}
    	return -1;
    }
    /*
     * https://stackoverflow.com/questions/21341027/find-indexof-a-byte-array-within-another-byte-array
     * Gustavo Mendoza's Answer*/
    static int indexOf(byte[] source, int sourceOffset, int sourceCount, byte[] target, int targetOffset, int targetCount, int fromIndex) {
        if (fromIndex >= sourceCount) {
            return (targetCount == 0 ? sourceCount : -1);
        }
        if (fromIndex < 0) {
            fromIndex = 0;
        }
        if (targetCount == 0) {
            return fromIndex;
        }

        byte first = target[targetOffset];
        int max = sourceOffset + (sourceCount - targetCount);

        for (int i = sourceOffset + fromIndex; i <= max; i++) {
            /* Look for first character. */
            if (source[i] != first) {
                while (++i <= max && source[i] != first)
                    ;
            }

            /* Found first character, now look at the rest of v2 */
            if (i <= max) {
                int j = i + 1;
                int end = j + targetCount - 1;
                for (int k = targetOffset + 1; j < end && source[j] == target[k]; j++, k++)
                    ;

                if (j == end) {
                    /* Found whole string. */
                    return i - sourceOffset;
                }
            }
        }
        return -1;
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



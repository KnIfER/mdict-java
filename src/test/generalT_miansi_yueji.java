package test;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.HashSet;
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
public class generalT_miansi_yueji {
	
	
	
	
	
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
    	
    	CMN.show(""+"\\js\\aa".compareTo("\\js\\b"));

    	
    	CMN.show(""+"\\js\\renderers\\CanvasRenderer.js".compareTo("\\js\\Three.js"));
    	CMN.show(""+"\\js\\renderers\\Projector.js".compareTo("\\js\\Three.js"));
    	
    	
    	
    	
    	CMN.show("asdasdasd"+"\\js\\renderers\\Projector.js".compareTo("\\js\\Three.js"));
    	
    	
    	String testData = "foolhardily absolute   absolutism happy lute h world word haWa垃圾 katsuki naku 和 乐 君 卿 寝室是真实的";
    	testData = "012happy   ";
    	byte[] testDataArr = testData.getBytes(_encoding);
    	String key = "happy".toLowerCase();

		byte[] plain_matcher=key.getBytes(_encoding);
    	
    	byte[][][] matcher = new byte[2][][];
    	matcher[0] = flowerSanLieZhi(key);
    	String upperKey = key.toUpperCase();
    	if(!upperKey.equals(key))
    		matcher[1] = flowerSanLieZhi(upperKey);
		for(byte[] xxx:matcher[0]) if(xxx!=null)
    		ripemd128.printBytes(xxx);
		ripemd128.printBytes(key.getBytes(_encoding));
		ripemd128.printBytes(testDataArr);
    	
    
		//CMN.show("wahaha: "+new String(new byte[] {0x68,0x00,0x61},_encoding));

		
		
		
		//CMN.show("asd: "+indexOf(testDataArr, 0, testDataArr.length, "e".getBytes(),0,1, -1)) ;
		int start = 0;
		int len = testDataArr.length-start;
		
		long st = System.currentTimeMillis();
		int try_count=10000000;
		for(int i=0;i>try_count;i++) {
			flowerIndexOf(testDataArr, start, len, matcher,0,0);
			

			//indexOf(testDataArr, start, len, plain_matcher,0,plain_matcher.length, 0);
		}
		long total = (System.currentTimeMillis()-st) ;
		CMN.show("fffuuuuzzzyyy time: "+ total +"平均: "+1.0*total/try_count) ;
		
		CMN.show("fffuuuuzzzyyy: "+flowerIndexOf(testDataArr, start, 4, matcher,0,0)) ;
		CMN.show("fffuuuuzzzyyy: "+flowerIndexOf(testDataArr, start, 5, matcher,0,0)) ;
		CMN.show("fffuuuuzzzyyy: "+flowerIndexOf(testDataArr, start, 6, matcher,0,0)) ;
		CMN.show("fffuuuuzzzyyy: "+flowerIndexOf(testDataArr, start, len, matcher,0,9)) ;//-1
		CMN.show("fffuuuuzzzyyy: "+flowerIndexOf(testDataArr, start, len, matcher,0,0)) ;
		
		
		
		//CMN.show("fffuuuuzzzyyy: "+matcher[0][0].length) ;
		byte[][] target = new byte[][]{"hapPy".getBytes(_encoding),"happy".getBytes(_encoding)};
		CMN.show("MinimalIndexOf: "+MinimalIndexOf(testDataArr,start , len, target,0,-1,0)) ;
		
		CMN.show(""+binary_find_closest(new long[] {10l,200003l,4035450l},1000l));
		
		
		
		
		try_count=100000000;
		String xx;
		Charset c = Charset.forName(_encoding);
		for(int i=0;i>try_count;i++) {
			xx = new String(testDataArr,_encoding);
			

			//indexOf(testDataArr, start, len, plain_matcher,0,plain_matcher.length, 0);
		}
		total = (System.currentTimeMillis()-st) ;
		CMN.show("new String time: "+ total +"平均: "+1.0*total/try_count) ;
		
		
		
    }
    static String _encoding = "UTF-16";//fe,ff开头
    //static String _encoding = "UTF-16LE";//无开头
    //static String _encoding = "GBK";//无开头
    //static String _encoding = "UTF-8";//无开头
    
    
    public static int  binary_find_closest(long[] array,long val){
    	int middle = 0;
    	int iLen = array.length;
    	int low=0,high=iLen-1;
    	if(array==null || iLen<1){
    		return -1;
    	}
    	if(iLen==1){
    		return 0;
    	}
    	if(val-array[0]<=0){
			return 0;
    	}else if(val-array[iLen-1]>=0){
    		return iLen-1;
    	}
    	int counter=0;
    	long cprRes1,cprRes0;
    	while(low<high){
    		counter+=1;
    		//System.out.println(low+":"+high);
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
        		high=middle;
        	}
    	}
		return low;
    }
    
    static int MinimalIndexOf(byte[] source, int sourceOffset, int sourceCount, byte[][] targets, int targetOffset, int targetCount, int fromIndex) {
        if (fromIndex >= sourceCount) {
            return (targetCount == 0 ? sourceCount : -1);
        }
        if (fromIndex < 0) {
            fromIndex = 0;
        }
        int targetLet_length = targets[0].length;
        if (targetCount == -1) {
            targetCount = targetLet_length;
        }
        if (targetCount == 0) {
            return fromIndex;
        }
        
        
        int max = sourceOffset + (sourceCount - targetCount);
        int cI;
        for (int i = sourceOffset + fromIndex; i <= max; i++) {
        	OUT:
        	for(byte[] targets_tI:targets) {//擦,这也行
        		for(cI=0;cI<targetLet_length;cI++) {//擦,这也行
        			if(targets_tI[cI]!=source[i+cI]) {
        				continue OUT;
        			}
        		}
        		return i-sourceOffset;
        	}
        }
        return -1;
    }
    
    
    static HashSet<Integer> miansi = new HashSet<>();//. is 免死金牌
    static HashSet<Integer> yueji = new HashSet<>();//* is 越级天才
    
static int flowerIndexOf(byte[] source, int sourceOffset, int sourceCount, byte[][][] matchers,int marcherOffest, int fromIndex) throws UnsupportedEncodingException 
{
	
	int lastSeekLetSize=0;
	while(fromIndex<sourceCount) {
		//CMN.show("==");
		int idx = -1;
		int fromIndex_=fromIndex;
		boolean isSeeking=true;
		boolean Matched = false;
		for(int lexiPartIdx=marcherOffest;lexiPartIdx<matchers[0].length;lexiPartIdx++) {
			//if(fromIndex_>sourceCount-1) return -1;
			//CMN.show("stst: "+sourceCount+"::"+(fromIndex_+seekPos)+" fromIndex_: "+fromIndex_+" seekPos: "+seekPos+" lexiPartIdx: "+lexiPartIdx);
	
			//CMN.show("seekPos: "+seekPos+" lexiPartIdx: "+lexiPartIdx+" fromIndex_: "+fromIndex_);
			if(miansi.contains(lexiPartIdx)) {
				if(lexiPartIdx==matchers[0].length-1) {
					if(fromIndex_>=sourceCount)
						return -1;
					continue;
				}//Matched=true
				//CMN.show("miansi: "+lexiPartIdx);
				//CMN.show("miansi: "+sourceCount+"::"+(fromIndex_+seekPos)+"sourceL: "+source.length);
				//CMN.show("jumpped c is: "+new String(source,fromIndex_+seekPos,Math.min(4, sourceCount-(fromIndex_+seekPos-sourceOffset)),_encoding).substring(0, 1));
				int newSrcCount = Math.min(4, sourceCount-(fromIndex_));
				if(newSrcCount<=0)
					return -1;
				String c = new String(source,sourceOffset+fromIndex_,newSrcCount,_encoding);
				int jumpShort = c.substring(0, 1).getBytes(_encoding).length;
				fromIndex_+=jumpShort;
				continue;
			}else if(yueji.contains(lexiPartIdx)) {
				if(lexiPartIdx==matchers[0].length-1) continue;
				if(flowerIndexOf(source, sourceOffset+fromIndex_,sourceCount-(fromIndex_), matchers,lexiPartIdx+1, 0)!=-1){
					return fromIndex-lastSeekLetSize;
				}
				return -1;
			}
			Matched = false;
			if(isSeeking) {		
				int seekPos=-1;
				int newSeekPos=-1;
				for(byte[][] marchLet:matchers) {
	    			//if(marchLet==null) break;
	    			if(newSeekPos==-1)
	    				newSeekPos = indexOf(source, sourceOffset, sourceCount, marchLet[lexiPartIdx],0,marchLet[lexiPartIdx].length, fromIndex_) ;
	    			else        				
	    				newSeekPos = indexOf(source, sourceOffset, newSeekPos, marchLet[lexiPartIdx],0,marchLet[lexiPartIdx].length, fromIndex_) ;
	    			//Lala=MinimalIndexOf(source, sourceOffset, sourceCount, new byte[][] {matchers[0][lexiPartIdx],matchers[1][lexiPartIdx]},0,-1,fromIndex_+seekPos);
	    			if(newSeekPos!=-1) {
	    				seekPos=newSeekPos;
						lastSeekLetSize=matchers[0][lexiPartIdx].length;
						Matched=true;
					}
				}
				//CMN.show("seekPos:"+seekPos+" fromIndex_: "+fromIndex_);
				if(!Matched)
					return -1;
				seekPos+=lastSeekLetSize;
				fromIndex=fromIndex_=seekPos;
				isSeeking=false;
				continue;
				}
			else {
				//CMN.show("deadline"+fromIndex_+" "+sourceCount);
				if(fromIndex_>sourceCount-1) {
					//CMN.show("deadline reached"+fromIndex_+" "+sourceCount);
					return -1;
				}
				for(byte[][] marchLet:matchers) {
					if(marchLet==null) break;
					if(bingStartWith(source,sourceOffset,marchLet[lexiPartIdx],0,-1,fromIndex_)) {
						Matched=true;
		    			//CMN.show("matchedHonestily: "+sourceCount+"::"+(fromIndex_+seekPos)+" fromIndex_: "+fromIndex_+" seekPos: "+seekPos);
						//CMN.show("matchedHonestily: "+lexiPartIdx);
					}
				}
			}
			if(!Matched) {
				//CMN.show("Matched failed this round: "+lexiPartIdx);
				break;
			}
			fromIndex_+=matchers[0][lexiPartIdx].length;
		}
		if(Matched)
			return fromIndex-lastSeekLetSize;
	}
	return -1;
}

    
    private static byte[][] flowerSanLieZhi(String str) throws UnsupportedEncodingException {
    	miansi.clear();
    	yueji.clear();
    	byte[][] res = new byte[str.length()][];
    	for(int i=0;i<str.length();i++){
    		String c = str.substring(i, i+1);
    		if(c.equals("."))
    			miansi.add(i);
    		else if(c.equals("*"))
    			yueji.add(i);
    		else
    			res[i] = c.getBytes(_encoding);
    	}
    	return res;
    }
    
    
    
    
    
    
    
    
    
    
    public static int  binary_find_closest(int[] array,int val){
    	int middle = 0;
    	int iLen = array.length;
    	int low=0,high=iLen-1;
    	if(array==null || iLen<1){
    		return -1;
    	}
    	if(iLen==1){
    		return 0;
    	}
    	if(val-array[0]<=0){
			return 0;
    	}else if(val-array[iLen-1]>=0){
    		return iLen-1;
    	}
    	int counter=0;
    	long cprRes1,cprRes0;
    	while(low<high){
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
        		low=middle+1;
        	}
    	}
		return low;
    }
    
    

    
    private static byte[][] SanLieZhi(String str) throws UnsupportedEncodingException {
    	byte[][] res = new byte[str.length()][];
    	for(int i=0;i<str.length();i++){
    		String c = str.substring(i, i+1);
    		res[i] = c.getBytes(_encoding);
		}
		return res;
	}
    
    static boolean EntryStartWith(byte[] source, int sourceOffset, int sourceCount, byte[][][] matchers) throws UnsupportedEncodingException {
		boolean Matched = false;
		int fromIndex=0;
		CMN.show("matching!");
    	for(int lexiPartIdx=0;lexiPartIdx<matchers[0].length;lexiPartIdx++) {
    		Matched = false;
    		if(miansi.contains(lexiPartIdx)) {
    			if(lexiPartIdx==matchers[0].length-1) continue;//Matched=true
    			CMN.show("miansi: "+lexiPartIdx);
    			int jumpShort = new String(source,fromIndex,8,_encoding).substring(0, 1).getBytes(_encoding).length;
    			fromIndex+=jumpShort;
    			continue;
    		}
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
    	if(targetCount<=-1)
    		targetCount=target.length;
    	if (sourceOffset+fromIndex+targetCount > source.length) {
    		return false;
        }
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



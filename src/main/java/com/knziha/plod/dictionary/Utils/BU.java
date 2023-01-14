/*  Copyright 2018 KnIfER Zenjio-Kang

	Licensed under the Apache License, Version 2.0 (the "License");
	you may not use this file except in compliance with the License.
	You may obtain a copy of the License at
	
	    http://www.apache.org/licenses/LICENSE-2.0
	
	Unless required by applicable law or agreed to in writing, software
	distributed under the License is distributed on an "AS IS" BASIS,
	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
	See the License for the specific language governing permissions and
	limitations under the License.
	
	Mdict-Java Query Library
*/

package com.knziha.plod.dictionary.Utils;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.zip.Adler32;
import java.util.zip.DeflaterInputStream;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterOutputStream;


/**
 * @author KnIfER
 * @date 2018/05/31
 */
public class  BU{//byteUtils

	public static int calcChecksum(byte[] bytes) {
        Adler32 a32 = new Adler32();
        a32.update(bytes);
		return (int) a32.getValue();
    }
	public static int calcChecksum(byte[] bytes,int off,int len) {
        Adler32 a32 = new Adler32();
        a32.update(bytes,off,len);
		return (int) a32.getValue();
    }
    //解压等utils
    @Deprecated
    public static byte[] zlib_decompress(byte[] encdata,int offset,int ln) {
	    try {
			    ByteArrayOutputStream out = new ByteArrayOutputStream();
			    InflaterOutputStream inf = new InflaterOutputStream(out); 
				if(ln==-1) ln = encdata.length - offset;
			    inf.write(encdata,offset, ln); 
			    inf.close(); 
			    return out.toByteArray(); 
		    } catch (Exception ex) {
		    	ex.printStackTrace(); 
		    	return "ERR".getBytes(); 
		    }
    }
    @Deprecated
    public static byte[] zlib_compress(byte[] encdata,int offset,int ln) {
	    try {
			    ByteArrayOutputStream out = new ByteArrayOutputStream();
			    DeflaterOutputStream inf = new DeflaterOutputStream(out); 
				if(ln==-1) ln = encdata.length - offset;
			    inf.write(encdata,offset, ln); 
			    inf.close(); 
			    return out.toByteArray(); 
		    } catch (Exception ex) {
		    	ex.printStackTrace(); 
		    	return "ERR".getBytes(); 
		    }
    }

    @Deprecated
    public static byte[] toLH(int n) {  
    	  byte[] b = new byte[4];  
    	  b[0] = (byte) (n & 0xff);  
    	  b[1] = (byte) (n >> 8 & 0xff);  
    	  b[2] = (byte) (n >> 16 & 0xff);  
    	  b[3] = (byte) (n >> 24 & 0xff);  
    	  return b;  
    	} 
    
    
    
    public static long toLong(byte[] buffer,int offset) {   
        long  values = 0;   
        for (int i = 0; i < 8; i++) {    
            values <<= 8; values|= (buffer[offset+i] & 0xff);   
        }   
        return values;  
     } 
    public static long toLongLE(byte[] buffer,int offset) {   
        long  values = 0;   
        for (int i = 7; i >= 0; i--) {    
            values <<= 8; values|= (buffer[offset+i] & 0xff);   
        }   
        return values;  
     } 
    public static int toInt(byte[] buffer,int offset) {   
        int  values = 0;   
        for (int i = 0; i < 4; i++) {    
            values <<= 8; values|= (buffer[offset+i] & 0xff);   
        }   
        return values;  
     }     
     
    public static int toIntLE(byte[] buffer,int offset) {   
        int  values = 0;   
        for (int i = 3; i >= 0; i--) {    
            values <<= 8; values|= (buffer[offset+i] & 0xff);   
        }   
        return values;  
     }     
    
    public static short toShortLE(byte[] buffer,int offset) {   
        short  values = 0;   
        for (int i = 1; i >= 0; i--) {    
            values <<= 8; values|= (buffer[offset+i] & 0xff);   
        }   
        return values;  
     }   
     
    public static void putShortLE(byte[] buffer, int offset, short value) {   
		buffer[offset] = (byte) (value&0xff);
		buffer[offset+1] = (byte) (value>>8&0xff);
     }     
    
    public static void putIntLE(byte[] buffer, int offset, int value) {   
		buffer[offset] = (byte) (value&0xff);
		buffer[offset+1] = (byte) (value>>8&0xff);
		buffer[offset+2] = (byte) (value>>16&0xff);
		buffer[offset+3] = (byte) (value>>24&0xff);
     }     
     
    public static void putLongLE(byte[] buffer, int offset, long value) {
		for (int i = 0; i < 8; i++) {
			buffer[offset+i] = (byte) (value&0xff);
			value>>=8;
		}
     }     
    
    
	static byte[] _fast_decrypt(byte[] data,byte[] key){ 
	    long previous = 0x36;
	    for(int i=0;i<data.length;i++){
	    	//INCONGRUENT CONVERTION FROM byte to int
	    	int ddd = data[i]&0xff;
	    	long t = (ddd >> 4 | ddd << 4) & 0xff;
	        t = t ^ previous ^ (i & 0xff) ^ (key[(i % key.length)]&0xff);
	        previous = ddd;
	        data[i] = (byte) t;
        }
	    return data;
    }
	
	public static byte[] _mdx_decrypt(byte[] comp_block) throws IOException{
		ByteArrayOutputStream data = new ByteArrayOutputStream() ;
		data.write(comp_block,4,4);
		data.write(ripemd128.packIntLE(0x3695));
	    byte[]  key = ripemd128.ripemd128(data.toByteArray());
	    data.reset();
	    data.write(comp_block,0,8);
	    byte[] comp_block2 = new byte[comp_block.length-8];
	    System.arraycopy(comp_block, 8, comp_block2, 0, comp_block.length-8);
	    data.write(_fast_decrypt(comp_block2, key));
	    return data.toByteArray();
    }


    @Deprecated
	public static void printBytes2(byte[] b) {
		for(int i=0;i<b.length;i++)
    		System.out.print((int)(b[i]&0xff)+",");
    	System.out.println();
	}
	public static void printBytes3(byte[] b){
		String val="";
		for(int i=0;i<b.length;i++)
			val+="0x"+byteTo16(b[i])+",";
		SU.Log(val);
	}
    @Deprecated
    public static void printBytes(byte[] b){
    	for(int i=0;i<b.length;i++)
    		System.out.print("0x"+byteTo16(b[i])+",");
    	System.out.println();
    }

	public static void LogBytes(byte[] b){
		LogBytes(b, 0, b.length);
	}

	public static void LogBytes(byte[] b,int off,int ln){
		String val="";
		ln+=off;
		for(int i=off;i<ln;i++)
			val+="0x"+byteTo16(b[i])+",";
		SU.Log(val);
	}
    @Deprecated
    public static void printBytes(byte[] b,int off,int ln){
    	for(int i=off;i<off+ln;i++)
    		System.out.print("0x"+byteTo16(b[i])+",");
    	System.out.println();
    }
	public static void printFile(byte[] b,int off,int ln,String path){
		printFile(b, off, ln, new File(path));
	}
    @Deprecated
    public static void printFile(byte[] b,int off,int ln,File path){
    	try {
			File p = path.getParentFile();
			if(!p.exists()) p.mkdirs();
			FileOutputStream fo = new FileOutputStream(path);
			fo.write(b);
			fo.flush();
			fo.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
    }
    @Deprecated
    public static void printFile(byte[] b, String path){
		printFile(b,0,b.length,new File(path));
    }

    @Deprecated
    public static void printFile(byte[] b, File path){
		printFile(b,0,b.length,path);
    }

    @Deprecated
    public static void printFileStream(InputStream b, File path){
		try {
			printStreamToFile(b,0,b.available(),path);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

    @Deprecated
    public static void printStreamToFile(InputStream b, int start, int end,  File path){
		try {
			if(start>0)
				b.skip(start);
			File p = path.getParentFile();
			if(!p.exists()) p.mkdirs();
			FileOutputStream fo = new FileOutputStream(path);
			byte[] data = new byte[4096];
			int len;
			while ((len=b.read(data))>0){
				fo.write(data, 0, len);
			}
			fo.flush();
			fo.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
    }

    public static String byteTo16(byte bt){
        String[] strHex={"0","1","2","3","4","5","6","7","8","9","a","b","c","d","e","f"};
        String resStr="";
        int low =(bt & 15);
        int high = bt>>4 & 15;
        resStr = strHex[high]+strHex[low];
        return resStr;
    }
    
    
    
    

    public static char toChar(byte[] buffer,int offset) {   
        char  values = 0;   
        for (int i = 0; i < 2; i++) {    
            values <<= 8; values|= (buffer[offset+i] & 0xff);   
        }   
        return values;  
    }


	public static ReusableByteInputStream fileToBytes(File f) {
		return new ReusableByteInputStream(fileToByteArr(f));
	}

	public static byte[] fileToByteArr(File f) {
		try {
			FileInputStream fin = new FileInputStream(f);
			byte[] data = new byte[(int) f.length()];
			fin.read(data);
			return data;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static String fileToString(File f) {
		try {
			FileInputStream fin = new FileInputStream(f);
			byte[] data = new byte[(int) f.length()];
			fin.read(data);
			fin.close();
			return new String(data, "utf8");
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}


	public static String fileToString(String path, byte[] buffer, ReusableByteOutputStream bo, Charset charset) {
		try {
			FileInputStream fin = new FileInputStream(path);
			bo.reset();
			int len;
			while((len = fin.read(buffer))>0)
				bo.write(buffer, 0, len);
			return new String(bo.data(),0, bo.size(), charset);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static String transStream(InputStream in) {
		byte[] data = new byte[4096];
		ReusableByteOutputStream bout = new ReusableByteOutputStream(8192);
		try{
			int len;
			while ((len=in.read(data))>0) {
				bout.write(data, 0 ,len);
			}
		} catch (Exception ignored) {  }
		return new String(bout.data(), 0, bout.size());
	}

	@Deprecated
    public long toLong1(byte[] b,int offset)
	{
		long l = 0;
		l = b[offset+0];
		l |= ((long) b[offset+1] << 8);
		l |= ((long) b[offset+2] << 16);
		l |= ((long) b[offset+3] << 24);
		l |= ((long) b[offset+4] << 32);
		l |= ((long) b[offset+5] << 40);
		l |= ((long) b[offset+6] << 48);
		l |= ((long) b[offset+7] << 56);
		return l;
	}
	
    
    public static String unwrapMdxName(String in) {
    	if(in.toLowerCase().endsWith(".mdx"))
    		return in.substring(0,in.length()-4);
    	return in;
    }
    public static String unwrapMddName(String in) {
    	if(in.toLowerCase().endsWith(".mdd"))
    		return in.substring(0,in.length()-4);
    	return in;
    }

	public static int bit_length(long num) {
		int res = 1;
		num >>= 1;
		while(num != 0) {
			res += 1;
			num >>= 1;
		}
		return res;
	}

	public static int readInt(InputStream bin) throws IOException {
		int ch1 = bin.read();
		int ch2 = bin.read();
		int ch3 = bin.read();
		int ch4 = bin.read();
		if ((ch1 | ch2 | ch3 | ch4) < 0)
			throw new EOFException();
		return ((ch1 << 24) + (ch2 << 16) + (ch3 << 8) + (ch4 << 0));
	}

	public static int readShort(InputStream bin) throws IOException {
		int ch1 = bin.read();
		int ch2 = bin.read();
		if ((ch1 | ch2) < 0)
			throw new EOFException();
		return (short)((ch1 << 8) + (ch2 << 0));
	}

	public static String parseFontName(ReusableBufferedInputStream bin) throws IOException {
		bin.skip(4);
		int numOfTables = readShort(bin);
		bin.skip(6);
		boolean found = false;
		byte[] buff = new byte[4];
		for (int i = 0; i < numOfTables; i++) {
			bin.read(buff,0,4);
			int checkSum = readInt(bin);
			int offset = readInt(bin);
			int length = readInt(bin);
			String tname = new String(buff, StandardCharsets.UTF_8);
			if ("name".equalsIgnoreCase(tname)) {
				int now = 12+16*(i+1);
				int toSkip=offset-now;
				//CMN.Log("name table found!!!", offset, now, toSkip);
				if(toSkip>=0){
					while(toSkip>0){
						toSkip-=bin.skip(toSkip);
					}
					//now=offset;
					int fSelector = readShort(bin);
					int nRCount = readShort(bin);
					int storageOffset = readShort(bin);
					//ArrayList<Integer> arr = new ArrayList<>(6);
					for (int j = 0; j < nRCount; j++) {
						int platformID = readShort(bin);
						int encodingID = readShort(bin);
						int languageID = readShort(bin);
						int nameID = readShort(bin);
						int stringLength = readShort(bin);
						int stringOffset = readShort(bin);
						//1 says that this is font name. 0 for example determines copyright info
						if(nameID==1){
							//arr.add(stringOffset);
							//arr.add(stringLength);
							//byte[] bf = bin.getBytes();
							byte[] bf = new byte[stringLength];
							offset =  now + stringOffset + storageOffset;
							now = now + 3*2 + 6*2*(j+1);
							toSkip=offset-now;
							//CMN.Log("font name found!!!", stringLength, offset, now, toSkip);
							if(toSkip>=0){
								while(toSkip>0){
									toSkip-=bin.skip(toSkip);
								}
								bin.read(bf, 0, stringLength);
								boolean utf8 = platformID==3 && (encodingID==0||encodingID==1||encodingID==10) || platformID==0 && encodingID>=0 && encodingID<=4;
								//CMN.Log(platformID, encodingID, utf8);
								return new String(bf, 0, stringLength, utf8?StandardCharsets.UTF_16:StandardCharsets.UTF_8);
							}
							break;
						}
					}
				}
				break;
			} else if (tname.length() == 0) {
				break;
			}
		}
		return null;
	}

	public static InputStream SafeSkipReam(InputStream fin, long toSkip) throws IOException {
		//CMN.Log("SafeSkipReam::", toSkip);
		int tryCount=0;long skipped;
		while(toSkip>0){
			skipped=fin.skip(toSkip);
			if(false && skipped==0&&tryCount>=3){
				break;
			} else {
				toSkip-=skipped;
				tryCount++;
			}
		}
		return fin;
	}
}
	



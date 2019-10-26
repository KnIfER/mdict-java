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


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;



/**
 * Java ripemd128 from python
 *
 * @converter KnIfER
 * @date 2017/11/21
 */
public class ripemd128 {
	
	static byte[] _fast_decrypt(byte[] data,byte[] key){
	    int previous = 0x36;
	    for(int i=0;i<data.length;i++){
	    	int t = (data[i] >> 4 | data[i] << 4) & 0xff;
	        t = t ^ previous ^ (i & 0xff) ^ key[(i+key.length) % key.length];
	        previous = data[i];
	        data[i] = (byte) t;
	        }

	    return data;
    }
    
    static long f(long j,long x,long y,long z){
    	assert(0 <= j && j < 64);
    	if(j < 16)
    		return x ^ y ^ z;
    	else if(j < 32)
    		return (x & y) | (z & ~x);
    	else if( j < 48)
    		return (x | (Long.valueOf("ffffffff",16) & ~y)) ^ z;
    	else
    		return (x & z) | (y & ~z);
    }
    
    static long K(long j){
    	assert(0 <= j && j < 64);
    	if (j < 16)
    		return (long) 0x00000000;
    	else if (j < 32)
    		return (long) 0x5a827999;
    	else if (j < 48)
    		return (long) 0x6ed9eba1;
    	else
    		return (long) 0x8f1bbcdc;
    }		
    static long  Kp(long j){
    	assert(0 <= j && j < 64);
    	if( j < 16)
    		return (long) 0x50a28be6;
    	else if ( j < 32)
    		return (long) 0x5c4dd124;
    	else if ( j < 48)
    		return (long) 0x6d703ef3;
    	else
    		return (long) 0x00000000;
    
    }
    

	static long[][] padandsplit(byte[] message) throws IOException{
		/*
		returns a two-dimensional array X[i][j] of 32-bit integers, where j ranges
		from 0 to 16.
		First pads the message to length in bytes is congruent to 56 (mod 64), 
		by first adding a byte 0x80, and then padding with 0x00 bytes until the
		message length is congruent to 56 (mod 64). Then adds the little-endian
		64-bit representation of the original length. Finally, splits the result
		up into 64-byte blocks, which are further parsed as 32-bit integers.
		*/
		//ByteBuffer sf = ByteBuffer.wrap(itemBuf);
		int origlen = message.length;
		//!!!INCONGRUENT CALCULATION METHOD
		int padlength = 64 - ((origlen - 56 + 64) % 64); //minimum padding is 1!
		ByteArrayOutputStream data = new ByteArrayOutputStream() ;
		data.write(message);
		data.write(new byte[]{ (byte) 0x80});

		for(int i=0;i<padlength - 1;i++) data.write(new byte[]{0x00});
		data.write(packLongLE(origlen*8));
		assert(data.size() % 64 == 0);
		//System.out.println("origlen"+origlen);
		
		//System.out.print("message");printBytes(data.toByteArray());
		ByteBuffer sf = ByteBuffer.wrap(data.toByteArray()).order(ByteOrder.LITTLE_ENDIAN);
		long[][] res = new long[data.size()/64][64];
		for(int i=0;i<data.size();i+=64){
			for(int j=0;j<64;j+=4)
				res[i/64][j/4] = sf.getInt(i+j);
		}
		return res;
	}

    public static byte[] packLongLE(long l){
    	return ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putLong((long)l).array();
    }
    static byte[] packLongBE(long l){
    	return ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN).putLong((long)l).array();
    }
    public static byte[] packIntLE(int l){
    	return ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(l).array();
    }
    static byte[] packIntBE(int l){
    	return ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(l).array();
    }   
    
    static long add(long... intArray){
    	long res = (long) 0;
        for(long i : intArray)  
            res+=i; 
    	return res & Long.valueOf("ffffffff",16);
    }
    static long rol(long s,long x){
    	assert(s < 32);
    	return (x << s | x >> (32-s)) & Long.valueOf("ffffffff",16);
    	//before JAVA8 we do not have unsigned int
    }
    
    static int[] r =  new int[]{ 0, 1, 2, 3, 4, 5, 6, 7, 8, 9,10,11,12,13,14,15,
            7, 4,13, 1,10, 6,15, 3,12, 0, 9, 5, 2,14,11, 8,
            3,10,14, 4, 9,15, 8, 1, 2, 7, 0, 6,13,11, 5,12,
            1, 9,11,10, 0, 8,12, 4,13, 3, 7,15,14, 5, 6, 2};
    static int[] rp = new int[]{ 5,14, 7, 0, 9, 2,11, 4,13, 6,15, 8, 1,10, 3,12,
            6,11, 3, 7, 0,13, 5,10,14,15, 8,12, 4, 9, 1, 2,
            15, 5, 1, 3, 7,14, 6, 9,11, 8,12, 2,10, 0, 4,13,
             8, 6, 4, 1, 3,11,15, 0, 5,12, 2,13, 9, 7,10,14};
    static int[] s =  new int[]{11,14,15,12, 5, 8, 7, 9,11,13,14,15, 6, 7, 9, 8,
            7, 6, 8,13,11, 9, 7,15, 7,12,15, 9,11, 7,13,12,
            11,13, 6, 7,14, 9,13,15,14, 8,13, 6, 5,12, 7, 5,
            11,12,14,15,14,15, 9, 8, 9,14, 5, 6, 8, 6, 5,12};
    static int[] sp = new int[]{ 8, 9, 9,11,13,15,15, 5, 7, 7, 8,11,14,14,12, 6,
            9,13,15, 7,12, 8, 9,11, 7, 7,12, 7, 6,15,13,11,
            9, 7,15,11, 8, 6, 6,14,12,13, 5,14,13,13, 7, 5,
           15, 5, 8,11,14,14, 6,14, 6, 9,12, 9,12, 5,15, 8};

    public static byte[] ripemd128(byte[] message) throws IOException{    	
    	long h0 = Long.valueOf("67452301",16);
    	long h1 = Long.valueOf("efcdab89",16);
    	long h2 = Long.valueOf("98badcfe",16);
    	long h3 = Long.valueOf("10325476",16);
    	long A,B,C,D,Ap,Bp,Cp,Dp;
    	long[][] X = padandsplit(message);
    	for(int i=0;i<X.length;i++){
    		A=h0;
    		B=h1;
    		C=h2;
    		D=h3;
    		Ap=h0;
    		Bp=h1;
    		Cp=h2;
    		Dp=h3;
    		Long T;
			for(int j=0;j<64;j++){
    			T = rol(s[j],  add(A, f(j,B,C,D), X[i][r[j]], K(j)));
    			//System.out.println("preT is: "+add(A, f(j,B,C,D))); 
    			//System.out.println("T is: "+T);  
    			A=D;
    			D=C;		
    			C=B;		
    			B=T;	
    			T = rol(sp[j],  add(Ap, f(63-j,Bp,Cp,Dp), X[i][rp[j]], Kp(j)));
    			//System.out.println("T2 is: "+T); 
    			Ap=Dp;//System.out.println("Ap is: "+Ap);
    			Dp=Cp;//System.out.println("Dp is: "+Dp);
    			Cp=Bp;//System.out.println("Cp is: "+Cp);
    			Bp=T;//System.out.println("Bp is: "+Bp);
    			}
    		T = add(h1,C,Dp);
    		//System.out.println("T3 is: "+T); 
    		h1 = add(h2,D,Ap);
    		h2 = add(h3,A,Bp);
    		h3 = add(h0,B,Cp);
    		h0 = T;
		}
    
		ByteArrayOutputStream data = new ByteArrayOutputStream() ;
		//struct.pack("<LLLL",h0,h1,h2,h3)
		data.write(packIntLE((int) h0));
		data.write(packIntLE((int) h1));
		data.write(packIntLE((int) h2));
    	data.write(packIntLE((int) h3));
    	return data.toByteArray();
    }




}



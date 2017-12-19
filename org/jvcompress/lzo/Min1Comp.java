/* LZOConstants.java -- various constants (Original file)

   This file is part of the LZO real-time data compression library.

   Copyright (C) 1999 Markus Franz Xaver Johannes Oberhumer
   Copyright (C) 1998 Markus Franz Xaver Johannes Oberhumer
   Copyright (C) 1997 Markus Franz Xaver Johannes Oberhumer
   Copyright (C) 1996 Markus Franz Xaver Johannes Oberhumer

   The LZO library is free software; you can redistribute it and/or
   modify it under the terms of the GNU General Public License as
   published by the Free Software Foundation; either version 2 of
   the License, or (at your option) any later version.

   The LZO library is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.

   You should have received a copy of the GNU General Public License
   along with the LZO library; see the file COPYING.
   If not, write to the Free Software Foundation, Inc.,
   59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.

   Markus F.X.J. Oberhumer
   <markus.oberhumer@jk.uni-linz.ac.at>
   http://wildsau.idv.uni-linz.ac.at/mfx/lzo.html
   

   Java Porting of minilzo.c (2.03) by
   Copyright (C) 2010 Mahadevan Gorti Surya Srinivasa <sgorti@gmail.com>
 */
package org.jvcompress.lzo;
import java.io.*;
import java.util.Arrays;
import java.util.Random;

import org.jvcompress.util.MInt;

/**  Sample test program for testing the basic sanity of ported minilzo.c.
 * 
 * 
 *  @author mahadevan.gss
 */

public class Min1Comp {
	private static final int IN_LEN=1024*1024;
	private static final int OUT_LEN=IN_LEN*2;
	
	private static void clearDict(int[] dict){
		Arrays.fill(dict, 0);
	}
	private static String R(long millis,long iBytes,long oBytes){
		millis++;
		oBytes++;
		return ", millis:"+millis+", MB/sec:"+((iBytes*1000)/millis)/(1000*1000)+", ratio:"+((oBytes*100)/iBytes);
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
                System.out.println("Usage: java [-DDICT=c:/words.txt] org.jvcompress.lzo.Min1Comp");
		int[] dict = new int[128*1024]; 
		byte[] in = new byte[IN_LEN];
		byte[] out = new byte[OUT_LEN];
		MInt outL=new MInt();
                // Try zero-filled data
		for(int ii=0;ii<10;ii++){
			clearDict(dict);
			long l1 = System.currentTimeMillis();
			int r = MiniLZO.lzo1x_1_compress(in,IN_LEN,out,outL,dict);
			long l10 = System.currentTimeMillis();
			System.out.println(ii+". Zero Data Compress ret="+r+", out_lenth:"+outL.v +
							",comp-millis:"+R(l10-l1,IN_LEN,outL.v)+
			                   ", Zero Data Decompressing ..." );
			MInt OL=new MInt();
			r = MiniLZO.lzo1x_decompress(out,(int)outL.v,in,OL);
			long l2 = System.currentTimeMillis();
			System.out.println(ii+". Zero Data Got decompressed length:"+OL.v+
					", Zero Data checking ... millis:" + R(l2-l10,OL.v,outL.v));
			for(int i=0;i<OL.v;i++){
				if( in[i]!= 0){
					throw new AssertionError(ii+". Zero Data Decompreesed values not matching to Zero @:"+i);
				}
			}
		}
       	
       	Random ran = new Random();
       	byte[] in_rand = new byte[IN_LEN];
       	int[] partial = new int[]{16,32,64,128,192,224,256};
       	
        // Then Try random-filled data
       	for(int j=0;j<partial.length;j++){
       		int lim=partial[j];
       		for(int ii=0;ii<10;ii++){
       			
       			clearDict(dict);
       			boolean repeatPattern=ii >= 5;
                        // For first 5 iteration pure Random data, then some repeated chars
       			fillPartillyRandom(lim, in_rand, ran,repeatPattern);
       			System.arraycopy(in_rand, 0, in, 0, in_rand.length);
       			long l1 = System.currentTimeMillis();
       			outL=new MInt();
       			int r = MiniLZO.lzo1x_1_compress(in,IN_LEN,out,outL,dict);
       			long l10 = System.currentTimeMillis();
       			System.out.println(ii+". Random Data("+lim+"/repeatPattern:"+repeatPattern+") Compress ret="+r+", outL:"+outL.v +
       					", millis:"+R(l10-l1,IN_LEN,outL.v));
       			MInt OL=new MInt();
       			r = MiniLZO.lzo1x_decompress(out,(int)outL.v,in,OL);
       			long l2 = System.currentTimeMillis();
       			System.out.println(ii+". Random Data("+lim+"/repeatPattern:"+repeatPattern+")Got decompressed length:"+OL.v+
       	                   ",millis:"+ R(l2-l10,OL.v,outL.v));
       			for(int i=0;i<OL.v;i++){
       				if( in[i]!= in_rand[i]){
       					throw new AssertionError(ii+". Random Data("+lim+"/repeatPattern:"+repeatPattern+")  Decompreesed values not matching to Zero @:"+i);
       				}
       			}
       		}
       	}
        // Then Try some user given data-file
       	BufferedInputStream fis=null;
       	try{
       		String file=System.getProperty("DICT","c:/words.txt");
       		fis = new BufferedInputStream(new FileInputStream(file));
       	   for(;;){
       		int read=fis.read(in_rand);
       		if(read < 0) break;
       		for(int ii=0;ii<10;ii++){
       			clearDict(dict);
       			System.arraycopy(in_rand, 0, in, 0, read);
       			outL=new MInt();
       			
       			long l1 = System.currentTimeMillis();
       			int r = MiniLZO.lzo1x_1_compress(in,read,out,outL,dict);
       			long l10 = System.currentTimeMillis();
       			System.out.println(ii+". Dict-File Data("+read+") Compress ret="+r+", outL:"+outL.v +
       					", millis:"+R(l10-l1,read,outL.v));
       			MInt OL=new MInt();
       			r = MiniLZO.lzo1x_decompress(out,(int)outL.v,in,OL);
       			long l2 = System.currentTimeMillis();
       			System.out.println(ii+". Dict-File Data("+read+")Got decompressed length:"+OL.v+
       	                   ", millis:"+R(l2-l10,OL.v,outL.v));
       			if(OL.v != read){
       				System.err.println("Dict-File Decompressed length does not match");
       			}
       			for(int i=0;i<OL.v;i++){
       				if( in[i]!= in_rand[i]){
       					throw new AssertionError(ii+". Dict-File Data("+read+")  Decompreesed values not matching to Zero @:"+i);
       				}
       			}
       		}
       	   }
	}catch(FileNotFoundException fnf){
       	}catch(Exception e){
       		e.printStackTrace();
       	}finally{
       		try {
			if(fis!=null)fis.close();
		} catch (IOException e) {}
       	}
       	
	}
	/*
	private static void fillPartillyRandom(int lim,byte[] in,Random ran){
		for(int i=0;i<in.length;i++){
			in[i]= (byte)ran.nextInt(lim);	
		}
	}
	*/
	private static void fillPartillyRandom(int lim,byte[] in,Random ran,boolean rpt){
		int i=0;
		do{
			int repeat = (rpt ? ran.nextInt(10): 1);
			byte b = (byte)ran.nextInt(lim);
			for(int j=0;j<repeat && i<in.length;j++){
				in[i++]= b;
			}
		}while(i<in.length);
	}
}

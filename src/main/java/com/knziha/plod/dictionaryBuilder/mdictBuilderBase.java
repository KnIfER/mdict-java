package com.knziha.plod.dictionaryBuilder;

import com.github.luben.zstd.Zstd;
import com.knziha.plod.dictionary.Utils.*;
import com.knziha.plod.dictionaryBuilder.Utils.ByteDataOutputStream;
import com.knziha.rbtree.InOrderTodoAble;
import com.knziha.rbtree.myAbsCprKey;
import io.airlift.compress.zstd.ZstdCompressor;
//import net.jpountz.lz4.LZ4Compressor;
//import net.jpountz.lz4.LZ4Factory;
import org.anarres.lzo.LzoCompressor1x_1;
import org.anarres.lzo.lzo_uintp;
import test.CMN;

import java.io.*;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.zip.Deflater;

/**
 * Author KnIfER
 * Date 2019/11/03
 */

abstract class mdictBuilderBase {
	File f;
	Charset _charset;
	String _encoding;
	String _Dictionary_Name;
	String _about;
	int _encrypt=0;
	int keyblockCompressionType=1;
	int recordblockCompressionType=1;

	public InOrderTodoAble data_tree;
	public HashMap<myAbsCprKey,File> fileTree = new HashMap<>();
	/** hard_coded record index */
	public HashMap<myAbsCprKey,myCpr<Long, Long>> indexTree = new HashMap<>();

	public HashMap<String,ArrayList<mdictBuilder.myCprKey>> bookTree = new HashMap<>();
	public IntervalTree privateZone;

	long _num_entries;

	key_info_struct[] _key_block_info_list;


	byte[] KeyBlockInfoData;
	long KeyBlockInfoDataLN;
	long KeyBlockInfoDataLN2;
	int KeyBlockInfoDataDeCompressedAlder32;

	int perKeyBlockSize_IE_IndexBlockSize = 32;
	int perRecordBlockSize = 32;
	int getPerRecordBlockSize = 32;
	long key_block_compressed_size_accumulator;
	long record_block_decompressed_size_accumulator;
	boolean bAbortOldRecordBlockOnOverFlow;

	/** 分key缓存模式 0:no chache; 1: use cache file; 2: use memory*/
	int bUseIndexTmpFile=2;

	/** 分key缓存模式 0:no chache; 1: use cache file; 2: use memory*/
	public void setBuilderIndexCacheMode(int chache_mode){
		bUseIndexTmpFile=chache_mode;
	}

	int [] offsets;
	int [] offsets1;
	//ArrayList<byte[]> values;
	ArrayList<myAbsCprKey> keys=new ArrayList<>();
	//Integer[] blockDataInfo_L;
	Integer[] blockInfo_L;

	private int zstdLevel = 7;
	
	int RecordBlockZipLevel=-1;
	byte[] mContentDelimiter;
	protected long WriteOffset;

	//final static byte[] contentDelimiterNormal=new byte[] {0x0d,0x0a,0};
	//final static byte[] contentDelimiterUTF16=new byte[] {0x0d,0,0x0a,0,0,0};

	public long getNumberEntries(){return _num_entries;}

	public void setRecordUnitSize(int val) {
		perRecordBlockSize=val;
	}

	public void setIndexUnitSize(int val) {
		perKeyBlockSize_IE_IndexBlockSize=val;
	}

	public void setRecordBlockZipLevel(int val) {
		RecordBlockZipLevel=val;
	}

	/** 0=no compression; 1=lzo; 2=zip */
	public void setCompressionType(int val) {
		setCompressionType(val, val);
	}

	/** 0=no compression; 1=lzo; 2=zip; 3=zstd */
	public void setCompressionType(int keyBlock, int recordBlock) {
		if(keyBlock>=0&&keyBlock<=4){
			keyblockCompressionType=keyBlock;
		}
		if(recordBlock>=0&&recordBlock<=4){
			recordblockCompressionType=recordBlock;
		}
	}

	public long write(String path) throws IOException {
		File dirP = (f=new File(path)).getParentFile();
		dirP.mkdirs();
		if(!dirP.exists() && dirP.isDirectory())
			throw new IOException("input path invalid");
		File outFn = new File(path);
		RandomAccessFile fOut = new RandomAccessFile(outFn, "rw");
		if(WriteOffset>0)
			fOut.seek(WriteOffset);
		else
			f.delete();
		//![1]write_header
		byte[] header_bytes = constructHeader().getBytes(StandardCharsets.UTF_16LE);
		fOut.writeInt(header_bytes.length);
		fOut.write(header_bytes);
		fOut.write(BU.toLH(BU.calcChecksum(header_bytes)));

		//![2]split Keys
		File tmpFn=null;
		ReusableByteOutputStream bos=null;
		int mUseIndexTmpFile = bUseIndexTmpFile;
		if(mUseIndexTmpFile==1){
			tmpFn = new File(outFn.getParent(), outFn.getName()+".index_tmp");
			RandomAccessFile indexTmp = new RandomAccessFile(tmpFn, "rw");
			splitKeys(indexTmp);
			indexTmp.close();
			//tmpFn.delete();
		}
		else if(mUseIndexTmpFile==2){
			bos = new ReusableByteOutputStream();
			splitKeys(new ByteDataOutputStream(bos));
		}
		else{
			splitKeys(null);
		}

		//![3] key block info
		long current=fOut.getFilePointer();
		writebBeforeKeyEntity(fOut);
		fOut.write(KeyBlockInfoData);

		if(tmpFn!=null){
			FileInputStream fin = new FileInputStream(tmpFn);
			byte[] buffer = new byte[4096]; int len;
			while((len=fin.read(buffer))>0){
				fOut.write(buffer, 0, len);
			}
			fin.close();
			tmpFn.delete();
		}
		else if(bos!=null){
			fOut.write(bos.data(), 0, bos.size());
		}
		else{
			splitKeys(fOut);
		}

		long next=fOut.getFilePointer();
		fOut.seek(current);
		writebBeforeKeyEntity(fOut);
		fOut.seek(next);

		//![4] Encoding_record_block_header
		/*numer of record blocks*/
		fOut.writeLong(blockInfo_L.length);
		fOut.writeLong(_num_entries);
		/*numer of record blocks' info size*/
		long num_rinfo = 16*blockInfo_L.length;
		fOut.writeLong(num_rinfo);

		//![5] 跳过info部分
		current = fOut.getFilePointer();
		fOut.seek(num_rinfo=(current+num_rinfo+8));

		//![6] 写入内容
		ArrayList<record_info_struct> eu_RecordblockInfo = new ArrayList<>(blockInfo_L.length);
		int baseCounter=0;
		int cc=0;
		for(int blockInfo_L_I:blockInfo_L) {
			CMN.show("writing recording block No."+(cc++));
			//写入记录块
			record_info_struct RinfoI = new record_info_struct(0,0,0,0);
			ByteArrayOutputStream data_raw = new ByteArrayOutputStream();
			//CMN.show(blockInfo_L[i]+":"+values.length);
			for(int entryC=0;entryC<blockInfo_L_I;entryC++) {//压入内容
				byte[] byteContent=keys.get(baseCounter+entryC).getBinVals();
				if(byteContent==null) {
					File inhtml = fileTree.get(keys.get(baseCounter+entryC));
					FileInputStream FIN = new FileInputStream(inhtml);
					byteContent = new byte[(int) inhtml.length()];
					FIN.read(byteContent);
					FIN.close();
				}
				data_raw.write(byteContent);
				if(mContentDelimiter!=null)data_raw.write(mContentDelimiter);
			}

			byte[] data_raw_out = data_raw.toByteArray();
			RinfoI.decompressed_size = data_raw_out.length;
			int CompressionType = recordblockCompressionType;
			if(CompressionType==0){
				fOut.write(new byte[]{0,0,0,0});
				fOut.writeInt(BU.calcChecksum(data_raw_out,0,(int) (RinfoI.compressed_size = RinfoI.decompressed_size)));
				fOut.write(data_raw_out,0,data_raw_out.length);
			}
			else if(CompressionType==1) {
				fOut.write(new byte[]{1,0,0,0});
				int in_len = data_raw_out.length;
				int out_len_preEmpt =  (in_len + in_len / 16 + 64+(mContentDelimiter==null?0:mContentDelimiter.length));
				byte[] record_block_data = new byte[out_len_preEmpt];
				lzo_uintp out_len = new lzo_uintp();
				new LzoCompressor1x_1().compress(data_raw_out, 0, in_len, record_block_data, 0, out_len);
				//xxx
				//CMN.show(BU.calcChecksum(data_raw_out,0,(int) RinfoI.decompressed_size)+"asdasd");
				RinfoI.compressed_size = out_len.value;
				fOut.writeInt(BU.calcChecksum(data_raw_out,0,(int) RinfoI.decompressed_size));
				fOut.write(record_block_data,0,out_len.value);
			}
			else if(CompressionType==2) {
				fOut.write(new byte[]{2,0,0,0});

				byte[] buffer = new byte[1024];
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				Deflater df = new Deflater();
				df.setInput(data_raw_out, 0, (int) RinfoI.decompressed_size);
				df.finish();
				while (!df.finished()) {
					int n1 = df.deflate(buffer);
					baos.write(buffer, 0, n1);
				}

//				GZIPOutputStream gzip;
//				gzip = new GZIPOutputStream(baos);
//				gzip.write(data_raw_out);
//				gzip.close();

				//ripemd128.printBytes(raw_data.array(),0, raw_data.position());
				//KeyBlockInfoDataLN = df.deflate(KeyBlockInfoData);
				RinfoI.compressed_size = baos.size();
				fOut.writeInt(BU.calcChecksum(data_raw_out,0,(int) RinfoI.decompressed_size));
				fOut.write(baos.toByteArray());
			}
			else if(CompressionType==3) {
				fOut.write(new byte[]{3,0,0,0});
				ZstdCompressor zstdCompressor = new ZstdCompressor();
				int maxCompressedLen = zstdCompressor.maxCompressedLength((int) RinfoI.decompressed_size);
				byte[] buffer = new byte[maxCompressedLen];
				//RinfoI.compressed_size = zstdCompressor.compress(data_raw_out, 0, (int) RinfoI.decompressed_size, buffer, 0, maxCompressedLen);
				RinfoI.compressed_size = Zstd.compressByteArray(buffer, 0, maxCompressedLen, data_raw_out, 0, (int) RinfoI.decompressed_size, zstdLevel, false);
				fOut.writeInt(BU.calcChecksum(data_raw_out,0,(int) RinfoI.decompressed_size));
				fOut.write(buffer, 0, (int) RinfoI.compressed_size);
			}
			else if(CompressionType==4) {//lz4
//				fOut.write(new byte[]{4,0,0,0});
//				LZ4Factory factory = LZ4Factory.fastestInstance();
//				LZ4Compressor compressor = factory.highCompressor(1);
//				int maxCompressedLen = compressor.maxCompressedLength((int) RinfoI.decompressed_size);
//				byte[] buffer = new byte[maxCompressedLen];
//				RinfoI.compressed_size = compressor.compress(data_raw_out, 0, (int) RinfoI.decompressed_size, buffer, 0, maxCompressedLen);
//				fOut.writeInt(BU.calcChecksum(data_raw_out,0,(int) RinfoI.decompressed_size));
//				fOut.write(buffer, 0, (int) RinfoI.compressed_size);
			}
			baseCounter+=blockInfo_L_I;
			eu_RecordblockInfo.add(RinfoI);
		}

		next = fOut.getFilePointer();

		//![7] 写入 info 部分。
		fOut.seek(current);
		/*numer of record blocks' size*/
		fOut.writeLong(next-num_rinfo);
		/*record block infos*/
		for(record_info_struct RinfoI:eu_RecordblockInfo) {
			fOut.writeLong(RinfoI.compressed_size+8);//INCONGRUNENTSVG unmarked
			fOut.writeLong(RinfoI.decompressed_size);//!!!INCONGRUNENTSVG unmarked
		}

		//![8] 完成
		fOut.setLength(next);
		fOut.close();
		return next;
	}

	byte[] _111_long=new byte[]{1,1,1,1, 1,1,1,1};

	void writebBeforeKeyEntity(RandomAccessFile fOut) throws IOException {
		ByteBuffer sf = ByteBuffer.wrap(new byte[5*8]);

		/*number of keyblock count*/
		sf.putLong(_key_block_info_list.length);
		/*number of entries count*/
		sf.putLong(_num_entries);

		constructKeyBlockInfoData();

		/*number of bytes of deccompressed key block info data*/
		sf.putLong(KeyBlockInfoDataLN2);
		/*number of bytes of key block info*/
		sf.putLong(KeyBlockInfoDataLN+4*2);
		/*number of bytes of key block*/
		sf.putLong(key_block_compressed_size_accumulator+8*_key_block_info_list.length);
		byte[] five_Number_Bytes = sf.array();

		//CMN.show("key_block_info_size="+(KeyBlockInfoDataLN+4*2));
		//CMN.show("key_block_info_decomp_size="+KeyBlockInfoDataLN2);

		fOut.write(five_Number_Bytes);
		fOut.writeInt(BU.calcChecksum(five_Number_Bytes));

		fOut.write(new byte[]{2,0,0,0});
		fOut.writeInt(KeyBlockInfoDataDeCompressedAlder32);//BU.calcChecksum(KeyBlockInfoData,0,(int) KeyBlockInfoDataLN)+4*2);
	}

	private void constructKeyBlockInfoData() throws UnsupportedEncodingException {
		ByteBuffer raw_data = ByteBuffer.wrap(new byte[_key_block_info_list.length*(8+(65535+2+2)*2+8*2)]);//INCONGRUENTSVG::3 not dyed version diff,interval not marked.
		for(key_info_struct infoI:_key_block_info_list) {
			raw_data.putLong(infoI.num_entries);
			byte[] hTextArray = infoI.headerKeyText;
			//CMN.show(hTextArray.length+"");
			raw_data.putChar((char) (_encoding.startsWith("UTF-16")?hTextArray.length/2:hTextArray.length));//TODO
			raw_data.put(hTextArray);
			hTextArray = infoI.tailerKeyText;
			if(!_encoding.startsWith("UTF-16")){
				raw_data.put(new byte[] {0});
			}else{
				raw_data.put(new byte[] {0,0});
			}
			raw_data.putChar((char) (_encoding.startsWith("UTF-16")?hTextArray.length/2:hTextArray.length));
			raw_data.put(hTextArray);
			if(!_encoding.startsWith("UTF-16")){
				raw_data.put(new byte[] {0});
			}else{
				raw_data.put(new byte[] {0,0});
			}
			raw_data.putLong(infoI.key_block_compressed_size+8);//INCONGRUENTSVG this val is faked
			raw_data.putLong(infoI.key_block_decompressed_size);
		}
		KeyBlockInfoDataLN2 = raw_data.position();

		byte[] buffer = new byte[1024];
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		Deflater df = new Deflater();
		df.setInput(raw_data.array(), 0, raw_data.position());
		KeyBlockInfoDataDeCompressedAlder32 = BU.calcChecksum(raw_data.array(), 0, raw_data.position());
		df.finish();
		//ripemd128.printBytes(raw_data.array(),0, raw_data.position());
		//KeyBlockInfoDataLN = df.deflate(KeyBlockInfoData);
		while (!df.finished()) {
			int n = df.deflate(buffer);
			baos.write(buffer, 0, n);
		}
		KeyBlockInfoData = baos.toByteArray();
		//ripemd128.printBytes(KeyBlockInfoData);
		KeyBlockInfoDataLN = KeyBlockInfoData.length;
		//ripemd128.printBytes(KeyBlockInfoData,0,(int) KeyBlockInfoDataLN);
		//byte[] key_block_info = BU.zlib_decompress(KeyBlockInfoData,0,(int) KeyBlockInfoDataLN);
		//ripemd128.printBytes(key_block_info);
	}

	abstract String constructHeader();

	private void splitKeys(DataOutput fOutTmp) throws IOException {
		CMN.rt();
		keys.ensureCapacity(data_tree.size());
		keys.clear();
		//final ArrayList<byte[]> valslist = new ArrayList<>(data_tree.size());
		data_tree.SetInOrderDo(node -> {
			keys.add(((myAbsCprKey)node.getKey()));
			//valslist.add(((myAbsCprKey)node.getKey()).getBinVals());
		});
		data_tree.inOrderDo();
		CMN.pt("splitKeys 拷贝耗时"); CMN.rt();
//		if(bookTree!=null) {
//			for (int i = 0; i < keyslist.size(); i++) {//扩充
//				myAbsCprKey keyNode = keyslist.get(i);
//				String key = keyNode.key;
//				if (key.endsWith("[<>]")) {
//					keyslist.remove(i);
//					valslist.remove(i);
//					int start = i;
//					String name = key.substring(0, key.length() - 4);
//					ArrayList<mdictBuilder.myCprKey> bookc = bookTree.get(name);
//					for (mdictBuilder.myCprKey xx : bookc) {
//						keyslist.add(i, createKVPair(xx.key, xx.value.getBytes(_charset)));
//					}
//					if (bookc.size() > 0) {
//						i--;
//						privateZone.addInterval(start, i, name);
//						CMN.show(name + " added  " + start + " :: " + i);
//					}
//				}
//			}
//		}
		long counter=_num_entries=keys.size();
		record_block_decompressed_size_accumulator=0;

		//calc record split
		offsets = new int[(int) _num_entries];
		//values = valslist;
		//keys = keyslist.toArray(new myAbsCprKey[] {});
		
		boolean hasHardcodedKeyId = indexTree.size()>0;

		//todo::more precise estimate

		if(hasHardcodedKeyId) {
			if(offsets1==null) offsets1 = new int[(int) _num_entries];
			while(counter>0) {
				myAbsCprKey keyNode = keys.get((int) (_num_entries-counter));
				myCpr<Long, Long> keyid = indexTree.get(keyNode);
				if(keyid!=null) {
					offsets[(int) (_num_entries-counter)] = Math.toIntExact(keyid.key);
					offsets1[(int) (_num_entries-counter)] = Math.toIntExact(keyid.value);
				}
				counter-=1;
			}
		} else 
		if(fOutTmp!=null){
			ArrayList<Integer> blockInfo = new ArrayList<>((int)(_num_entries/5));// number of bytes of all rec-blocks
			ArrayList<Integer> blockDataInfo = new ArrayList<>((int)(_num_entries/5));// number of entries of all rec-blocks
			while(counter>0) {
				int idx = blockInfo.size();
				blockDataInfo.add(0);//byte数量
				blockInfo.add(0);//条目数量
				while(true) {
					if(counter<=0) break;
					int recordLen;
					int preJudge;
					//1st, pull data.
					myAbsCprKey keyNode = keys.get((int) (_num_entries-counter));
					if(keyNode.getBinVals()!=null) {
						//从内存
						/* fetching record data from memory */
						byte[] record_data = keyNode.getBinVals();
						recordLen = record_data.length;
						preJudge = blockDataInfo.get(idx)+recordLen;
					} else {
						/* fetching record data from file */
						File inhtml = fileTree.get(keyNode);
						CMN.Log(inhtml, keyNode);
						recordLen = (int) inhtml.length();
						preJudge = blockDataInfo.get(idx)+recordLen;
					}

					//2nd, judge & save data.
					if(preJudge<1024*perRecordBlockSize) {
						/* PASSING */
						offsets[(int) (_num_entries-counter)] = (int) record_block_decompressed_size_accumulator+(mContentDelimiter==null?0:mContentDelimiter.length)*((int) (_num_entries-counter));//xxx+3*((int) (_num_entries-counter));
						record_block_decompressed_size_accumulator+=recordLen;
						blockDataInfo.set(idx, preJudge);/*offset+=preJudge*/
						blockInfo.set(idx, blockInfo.get(idx)+1);/*entry++*/
						counter-=1;
					}
					else if(recordLen>=1024*perRecordBlockSize) {
						/* MONO OCCUPYING */
						offsets[(int) (_num_entries-counter)] = (int) record_block_decompressed_size_accumulator+(mContentDelimiter==null?0:mContentDelimiter.length)*((int) (_num_entries-counter));//xxx+3*((int) (_num_entries-counter));
						record_block_decompressed_size_accumulator+=recordLen;
						if(bAbortOldRecordBlockOnOverFlow) {
							if (blockDataInfo.get(idx)!=0) {//新开一个recblock
								blockDataInfo.add(0);
								blockInfo.add(0);
								idx++;
							}
						}
						blockDataInfo.set(idx, recordLen);/*offset+=preJudge*/  //+3*((int) (_num_entries-counter))
						blockInfo.set(idx, blockInfo.get(idx)+1);/*entry++*/
						counter-=1;
						break;
					}
					else/* NOT PASSING */ break;
				}
			}

			//blockDataInfo_L = blockDataInfo.toArray(new Integer[] {});
			blockInfo_L = blockInfo.toArray(new Integer[] {});
		} 
		

		CMN.pt_mins("splitKeys record 分组耗时"); CMN.rt();

		//calc index split
		counter=_num_entries;
		ArrayList<key_info_struct> list = new ArrayList<>((int) (_num_entries / 2000));
		key_block_compressed_size_accumulator=0;
		int sizeLimit = 1024 * perKeyBlockSize_IE_IndexBlockSize;
		ByteBuffer key_block_data_wrap = ByteBuffer.wrap(new byte[sizeLimit]);
		while(counter>0) {//总循环
			key_block_data_wrap.clear();
			//TODO WTF???
			//if(globalCompressionType==0)
			//	key_block_data_wrap.putLong(0);
			key_info_struct infoI = new key_info_struct();
			//dict = new int[102400];//TODO reuse
			long number_entries_counter = 0;
			long baseCounter = _num_entries-counter;
			if(privateZone!=null && privateZone.container((int) (_num_entries-counter))!=null) {
				myCpr<Integer, Integer> interval = privateZone.container((int) (_num_entries-counter));
				//CMN.show(interval.key+" ~ "+interval.value+" via "+(int) (_num_entries-counter));
				for(int i=interval.key;i<=interval.value;i++) {
					//CMN.show("putting!.."+(_num_entries-counter));
					key_block_data_wrap.putLong(offsets[i]);//占位 offsets i.e. keyid
					if(hasHardcodedKeyId) key_block_data_wrap.putLong(offsets1[i]);
					myAbsCprKey keyNode = keys.get(i);
					key_block_data_wrap.put(keyNode.key.getBytes(_charset));
					//CMN.show(number_entries_counter+":"+keyslist.get((int) (_num_entries-counter)));
					if(_encoding.startsWith("UTF-16")){
						key_block_data_wrap.put(new byte[]{0,0});//INCONGRUENTSVG
					}else
						key_block_data_wrap.put(new byte[]{0});//INCONGRUENTSVG
					number_entries_counter++;
					counter--;
				}
				infoI.num_entries = number_entries_counter;
				//CMN.show(baseCounter+":"+number_entries_counter+":"+keyslist.size());
				String whatever = privateZone.names.get(interval.key)+"";
				//whatever = keyslist.get(interval.key);
				infoI.headerKeyText = whatever.getBytes(_charset);
				infoI.tailerKeyText = (whatever).getBytes(_charset);
				CMN.show(whatever);
			}
			else {
				while(true) {//常规压入entries
					if(counter<=0) break;
					int retPos = key_block_data_wrap.position();
					try {//必定抛出，除非最后一个block.
						if(privateZone!=null && privateZone.container((int) (_num_entries-counter))!=null) throw new BufferOverflowException();
						key_block_data_wrap.putLong(offsets[(int) (_num_entries-counter)]);//占位 offsets i.e. keyid
						if(hasHardcodedKeyId) key_block_data_wrap.putLong(offsets1[(int) (_num_entries-counter)]);
						myAbsCprKey keyNode = keys.get((int) (_num_entries-counter));
						key_block_data_wrap.put(keyNode.key.getBytes(_charset));
						//CMN.show(number_entries_counter+":"+keyslist.get((int) (_num_entries-counter)));
						if(_encoding.startsWith("UTF-16")){
							key_block_data_wrap.put(new byte[]{0,0});//INCONGRUENTSVG
						}else
							key_block_data_wrap.put(new byte[]{0});//INCONGRUENTSVG
						counter-=1;
						number_entries_counter+=1;//完整放入后才计数
					} catch (BufferOverflowException e) {
						//e.printStackTrace();
						key_block_data_wrap.position(retPos);//不完整放入则回退。
						break;
					}
				}
				infoI.num_entries = number_entries_counter;
				//CMN.show(baseCounter+":"+number_entries_counter+":"+keyslist.size());
				infoI.headerKeyText = bakeMarginKey(keys.get((int) baseCounter).key);
				infoI.tailerKeyText = bakeMarginKey(keys.get((int) (baseCounter+number_entries_counter-1)).key);
			}
			infoI.key_block_decompressed_size = key_block_data_wrap.position();
			int in_len = (int) infoI.key_block_decompressed_size;
			byte[] key_block_data = key_block_data_wrap.array();

			int CompressionType = keyblockCompressionType;
			//CompressionType=1;
			if(CompressionType==0){
				infoI.key_block_compressed_size=infoI.key_block_decompressed_size;
				if(fOutTmp!=null) {
					//CMN.Log(fOutTmp.getFilePointer(), "key_pos");
					fOutTmp.write(new byte[]{0,0,0,0});
					fOutTmp.writeInt(BU.calcChecksum(key_block_data,0, in_len));
					fOutTmp.write(key_block_data,0, in_len);
				}
			}
			else if(CompressionType==1) {//lzo
				//fOut.write(new byte[] {0,1,0,1,0,1,0,1,0,1,0,1,0,1,0,1,0,1,9,9,9,9,9});
				int out_len_preEmpt =  (in_len + in_len / 16 + 64 + (mContentDelimiter==null?0:mContentDelimiter.length));
				byte[] compressed_key_block_data = new byte[out_len_preEmpt];


				lzo_uintp out_len = new lzo_uintp();
				new LzoCompressor1x_1().compress(key_block_data, 0, in_len, compressed_key_block_data, 0, out_len);

				infoI.key_block_compressed_size = out_len.value;
				if(fOutTmp!=null){
					fOutTmp.write(new byte[]{1,0,0,0});
					fOutTmp.writeInt(BU.calcChecksum(key_block_data,0,(int) infoI.key_block_decompressed_size));
					fOutTmp.write(compressed_key_block_data,0,out_len.value);
				}
			}
			else if(CompressionType==2) {//zip
				byte[] buffer = new byte[1024];
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				Deflater df = new Deflater();
				if(RecordBlockZipLevel!=-1)df.setLevel(RecordBlockZipLevel);
				df.setInput(key_block_data, 0, in_len);

				df.finish();
				//ripemd128.printBytes(raw_data.array(),0, raw_data.position());
				//KeyBlockInfoDataLN = df.deflate(KeyBlockInfoData);

				while (!df.finished()) {
					int n1 = df.deflate(buffer);
					baos.write(buffer, 0, n1);
				}
				infoI.key_block_compressed_size = baos.size();
				if(fOutTmp!=null){
					fOutTmp.write(new byte[]{2,0,0,0});
					fOutTmp.writeInt(BU.calcChecksum(key_block_data,0,in_len));
					fOutTmp.write(baos.toByteArray());
				}
			}
			else if(CompressionType==3) {//zstd
				ZstdCompressor zstdCompressor = new ZstdCompressor();
				int maxCompressedLen = zstdCompressor.maxCompressedLength(in_len);
				byte[] buffer = new byte[maxCompressedLen];
				//infoI.key_block_compressed_size = zstdCompressor.compress(key_block_data, 0, in_len, buffer, 0, maxCompressedLen);
				infoI.key_block_compressed_size = Zstd.compressByteArray(buffer, 0, maxCompressedLen, key_block_data, 0, in_len, zstdLevel, false);
				if(fOutTmp!=null){
					fOutTmp.write(new byte[]{3,0,0,0});
					fOutTmp.writeInt(BU.calcChecksum(key_block_data,0,in_len));
					fOutTmp.write(buffer, 0, (int) infoI.key_block_compressed_size);
				}
			}
			else if(CompressionType==4) {//lz4
//				// compress data
//				LZ4Factory factory = LZ4Factory.fastestInstance();
//				LZ4Compressor compressor = factory.fastCompressor();
//				int maxCompressedLen = compressor.maxCompressedLength(in_len);
//				byte[] buffer = new byte[maxCompressedLen];
//				infoI.key_block_compressed_size = compressor.compress(key_block_data, 0, in_len, buffer, 0, maxCompressedLen);
//				if(fOutTmp!=null){
//					fOutTmp.write(new byte[]{4,0,0,0});
//					fOutTmp.writeInt(BU.calcChecksum(key_block_data,0,in_len));
//					fOutTmp.write(buffer, 0, (int) infoI.key_block_compressed_size);
//				}
			}

			//CMN.show("infoI key_block_data raw");
			//ripemd128.printBytes(key_block_data.array(),0, (int) infoI.key_block_decompressed_size);
			//CMN.show("infoI.key_block_data");
			//ripemd128.printBytes(infoI.key_block_data,0,(int) infoI.key_block_compressed_size);
			//CMN.show(infoI.key_block_decompressed_size+"~~"+infoI.key_block_compressed_size);
			/*
			byte[] key_block_data_return = new byte[(int) infoI.key_block_decompressed_size];
			MInt len = new MInt((int) infoI.key_block_decompressed_size);
			MiniLZO.lzo1x_decompress(infoI.key_block_data,(int) infoI.key_block_compressed_size,key_block_data_return,len);
			CMN.show("key_block_data_return");
			ripemd128.printBytes(key_block_data_return);
			*/
			list.add(infoI);
			key_block_compressed_size_accumulator += infoI.key_block_compressed_size;
		}
		//CMN.show("le"+list.size());

		CMN.pt_mins("splitKeys index 分组(写入)耗时");

		_key_block_info_list = list.toArray(new key_info_struct[] {});
	}

	abstract byte[] bakeMarginKey(String key);


}
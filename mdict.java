package plod;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.Collator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.Adler32;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterOutputStream;

import org.anarres.lzo.LzoAlgorithm;
import org.anarres.lzo.LzoDecompressor;
import org.anarres.lzo.LzoInputStream;
import org.anarres.lzo.LzoLibrary;
import org.jvcompress.lzo.MiniLZO;
import org.jvcompress.util.MInt;

import rbtree.RBTree;


/**
 * Java test...
 *	change the .mdx path and watch the output
 *  no o-o classes,
 *  just a straight forward test
 * @author KnIfER
 * @date 2017/11/22
 */

public class mdict {
	
	private static final int BlockID = 0;

	static String key = "马连鞍";//abduco@拉丁语英语		马连鞍@有毒植物		happy@简明英汉
    
    static File f;
    
    static void assignMdxFile(){
    //f = new File("F:\\dictionary_wkst\\omidict-analysis-master\\简明英汉汉英词典.mdx");
    //f = new File("F:\\dictionary_wkst\\omidict-analysis-master\\古生物图鉴.mdx");
    //f = new File("F:\\mdict_wrkst\\mdict-js-master\\makingMDX\\mdd_file.mdx");
    //f = new File("C:\\antiquafortuna\\MDictPC\\doc\\neo\\拉丁语英语[31655](091009).mdx");
    //f = new File("C:\\antiquafortuna\\MDictPC\\doc\\neo\\英语拉丁语字典【匿名原创】【版本日期未注明】.mdx");
    //f = new File("C:\\antiquafortuna\\MDictPC\\doc\\古生物图鉴.mdx");
    //f = new File("C:\\antiquafortuna\\MDictPC\\doc\\NameOfPlants.mdx");
    f = new File("C:\\antiquafortuna\\MDictPC\\doc\\有毒植物,J.Huang.mdx");
    }

    
    
/*    
    推荐文本编辑器：notepad++，使用 alt+2 技能键获得全局观~ 其他技能键：双击近战，ctrl+f洲际导弹。
    
    usage : getRecordAt(lookUp("happy"))
    
    笔记(给自己看的):initialize mdx 得到 _key_block_info_list 。相关文件部分有4个组成: headerKeyText (s)、 tailerKeyText (s)、 key_block_compressed_size (s)、 key_block_decompressed_size (s)
    
    *lookUp("key") SUMMARY: lookUp returns the position of best-matched entry to "key".
            first,根据“根据头部信息建立起来的Red-Black树： block_blockId_search_tree ”,使用我写的二叉树搜索算法xxing,获得最近匹配项（即最相近的块起始单词）所在的block id.
            second,根据 _key_block_info_list[block id] 存储的信息,将第block id个key_block解压出来。这事获得2000个词条左右的数组，一并放入_key_block_info_list[block id].key_list 中。
            最后调用 binary_find_closest2 ,作用于 key_list,得最近匹配项相对于key_list的位置，然后根据 num_entries_accumulator 换算得到最近匹配项相对于第一个入口词条的位置。
            
    *getEntryAt(pos): returns key_Text at position "pos".主要用于安卓listview adapter的实现
    *getRecordAt(pos): SUMMARY: returns the no.pos entry's Contents.
    
    [0]file header
    [0]key_block_info section 有4个组成
    根据头部信息建立 Red-Black树： block_blockId_search_tree ,结点形式：[key_block块i起始单词（shrinked text）,i] 且树根据起始单词的String排序建立。
    [0]key_block section:该文件部分存储所有入口词条。
    [2]record_block_info section
    Decode_record_block_header,根据头部信息建立 Red-Black树： accumulation_RecordB_tree ,
        结点形式：[ decompressed_size_accumulator,i] 且树根据decompressed_size_accumulator的int值大小排序建立。
        decompressed_size_accumulator:Record_block块i之前所有Record_block解压后的大小
    [2]record_block section:存储所有词条的具体内容。  

*/
    
	static int _encrypt=0;
	static String _encoding="";
	static String _passcode = "";
	static HashMap<Integer,String[]> _stylesheet = new HashMap<Integer,String[]>();
	static float _version;
	static int _number_width;
	static int _key_block_offset;
	static long _record_block_offset;
	static byte[] _key_block_compressed;
	static key_info_struct[] _key_block_info_list;
	static record_info_struct[] _record_info_struct_list;
	static long _num_entries;
    static long _num_key_blocks;
    static long _num_record_blocks;
    static RBTree<myCpr<Integer, Integer>> accumulation_blockId_tree = new RBTree<myCpr<Integer, Integer>>();
    static RBTree<myCpr<Long   , Integer>> accumulation_RecordB_tree = new RBTree<myCpr<Long   , Integer>>();
    static RBTree<myCpr<String , Integer>> block_blockId_search_tree = new RBTree<myCpr<String , Integer>>();
    static long accumulation_blockId_tree_TIME = 0;
    static long block_blockId_search_tree_TIME = 0;
    static String _Dictionary_Name;

    public static class myCpr<T1 extends Comparable<T1>,T2> implements Comparable<myCpr<T1,T2>>{
    	public T1 key;
    	public T2 value;
    	public myCpr(T1 k,T2 v){
    		key=k;value=v;
    	}
    	public int compareTo(myCpr<T1,T2> other) {
    		return this.key.compareTo(other.key);
    	}
    	public String toString(){
    		return key+"_"+value;
    	}
    }
    //store key_block's summary and itself
    public static class key_info_struct{
		public key_info_struct(String headerKeyText, String tailerKeyText,
    			long key_block_compressed_size_accumulator,
    			long key_block_decompressed_size) {
    		this.headerKeyText=headerKeyText;
    		this.tailerKeyText=tailerKeyText;		
    		this.key_block_compressed_size_accumulator=key_block_compressed_size_accumulator;		
    		this.key_block_decompressed_size=key_block_decompressed_size;		
    	}
    	public key_info_struct(long num_entries_,long num_entries_accumulator_) {
    		num_entries=num_entries_;
    		num_entries_accumulator=num_entries_accumulator_;
        }
		public String headerKeyText;
    	public String tailerKeyText;
    	public long key_block_compressed_size_accumulator;
    	public long key_block_compressed_size;
    	public long key_block_decompressed_size;
        public long num_entries;
        public long num_entries_accumulator;
        public String[] keys;
        public long[] key_offsets;
        public void ini(){
            keys =new String[(int) num_entries];
            key_offsets =new long[(int) num_entries];
        }
    }
    //store record_block's summary
    public static class record_info_struct{
    	public record_info_struct(long _compressed_size,long _compressed_size_accumulator,long _decompressed_size,long _decompressed_size_accumulator) {
    		 compressed_size=                  _compressed_size;
             compressed_size_accumulator=      _compressed_size_accumulator;
             decompressed_size=                _decompressed_size;
             decompressed_size_accumulator=    _decompressed_size_accumulator;
        
    	}
        public long compressed_size;
        public long compressed_size_accumulator;
    	public long decompressed_size;
    	public long decompressed_size_accumulator;
        public void ini(){
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
	
	static byte[] _mdx_decrypt(byte[] comp_block) throws IOException{
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

    //入口 这么长，我也不想啊。但大多数代码和python代码保持一致。不一致的用 INCONGRUENT 标明。
    public static void main(String[] args) throws IOException  {
//![]File in
    	assignMdxFile();
    	DataInputStream data_in =new DataInputStream(new FileInputStream(f));	
//![0]read_header 
    	// number of bytes of header text
    	byte[] itemBuf = new byte[4];
		data_in.read(itemBuf, 0, 4);
    	int header_bytes_size =getInt(itemBuf[0],itemBuf[1],itemBuf[2],itemBuf[3]);
    	byte[] header_bytes = new byte[header_bytes_size];
    	data_in.read(header_bytes,0, header_bytes_size); 
		// 4 bytes: adler32 checksum of header, in little endian
		itemBuf = new byte[4];
		data_in.read(itemBuf, 0, 4);
    	int alder32 = getInt(itemBuf[3],itemBuf[2],itemBuf[1],itemBuf[0]);
		assert alder32 == (calcChecksum(header_bytes)& 0xffffffff);
		_key_block_offset = 4 + header_bytes_size + 4;
		//data_in.close();
		//不必关闭文件流
		
		Pattern re = Pattern.compile("(\\w+)=\"(.*?)\"",Pattern.DOTALL);
		Matcher m = re.matcher(new String(header_bytes,"UTF-16LE"));
		HashMap<String,String> header_tag = new HashMap<String,String>();
		while(m.find()) {
			System.out.println("header tag: " + m.group(0)); 
			header_tag.put(m.group(1), m.group(2));
	      }				
		if(header_tag.containsKey("Title"))
			_Dictionary_Name=header_tag.get("Title");
		
		_encoding = header_tag.get("Encoding");
        // GB18030 > GBK > GB2312
        if(_encoding.equals("GBK")|| _encoding.equals("GB2312"))
        	_encoding = "GB18030";
        if(_encoding.equals("UTF-16"))
        	_encoding = "UTF-16LE"; //INCONGRUENT java charset          
        if (_encoding.equals("")){
        	_encoding = "ASCII";
        }
        // encryption flag
        //   0x00 - no encryption
        //   0x01 - encrypt record block
        //   0x02 - encrypt key info block
		if(!header_tag.containsKey("Encrypted") || header_tag.get("Encrypted").equals("0") || header_tag.get("Encrypted").equals("No"))
            _encrypt = 0;
		else if(header_tag.get("Encrypted") == "1")
            _encrypt = 1;
        else
            _encrypt = Integer.valueOf(header_tag.get("Encrypted"));

        // stylesheet attribute if present takes form of:
        //   style_number # 1-255
        //   style_begin  # or ''
        //   style_end    # or ''
        // store stylesheet in dict in the form of
        // {'number' : ('style_begin', 'style_end')}
        
        if(header_tag.containsKey("StyleSheet")){
            String[] lines = header_tag.get("StyleSheet").split("[\r\n \r \n]");
            for(int i=0;i<=lines.length-3;i+=3)
                _stylesheet.put(i,new String[]{lines[i+1],lines[i+2]});
        }
        // before version 2.0, number is 4 bytes integer
        // version 2.0 and above uses 8 bytes
        _version = Float.valueOf(header_tag.get("GeneratedByEngineVersion"));
        if(_version < 2.0)
            _number_width = 4;
            //self._number_format = '>I'
        else
            _number_width = 8;
            //self._number_format = '>Q'

        //return header_tag
//![0]HEADER 分析完毕 
//_read_keys START
        //size (in bytes) of previous 5 numbers (can be encrypted)
        int num_bytes;
        if(_version >= 2)
            num_bytes = 8 * 5;
        else
            num_bytes = 4 * 4;
		itemBuf = new byte[num_bytes];
		data_in.read(itemBuf, 0, num_bytes);
        ByteBuffer sf = ByteBuffer.wrap(itemBuf);
        //TODO: pureSalsa20.py decryption
        if(_encrypt==1){if(_passcode=="") throw new IllegalArgumentException("_passcode未输入");}
        _num_key_blocks = _read_number(sf);
        _num_entries = _read_number(sf);
        if(_version >= 2.0){long key_block_info_decomp_size = _read_number(sf);}
        
        long key_block_info_size = _read_number(sf);
        long key_block_size = _read_number(sf);
        
        // adler checksum of previous 5 numbers
        if(_version >= 2.0){
            int adler32 = calcChecksum(itemBuf);
    		itemBuf = new byte[4];
    		data_in.read(itemBuf, 0, 4);
            assert adler32 == (getInt(itemBuf[0],itemBuf[1],itemBuf[2],itemBuf[3])& 0xffffffff);
        }
        
        // read key block info, which comprises each key block's starting&&ending words、 textSize&&shrinkedText、compressed && decompressed size
		itemBuf = new byte[(int) key_block_info_size];
        System.out.println("key_block_info_size ="+key_block_info_size);  

		data_in.read(itemBuf, 0, (int) key_block_info_size);
        _key_block_info_list = _decode_key_block_info(itemBuf);
        assert(_num_key_blocks == _key_block_info_list.length);
        System.out.println("num_key_blocks ="+_num_key_blocks);  
        // read key block
		//itemBuf = new byte[(int) key_block_size];
		//data_in.read(itemBuf, 0, (int) key_block_size);
        //long start=System.currentTimeMillis(); //获取开始时间  
		//ArrayList<String[]> key_list = _decode_key_block2(itemBuf, _key_block_info_list);
		//long end=System.currentTimeMillis(); //获取结束时间
		//System.out.println("_decode_key_block时间： "+(end-start)+"ms"); 
		//System.out.println(key_list.size()+":"+_num_entries); 
        if(_version<2)
            _record_block_offset = _key_block_offset+4 * 4+key_block_info_size+key_block_size;
        else
        	_record_block_offset = _key_block_offset+num_bytes+4+key_block_info_size+key_block_size;

//_read_keys END
        
		for(myCpr<String,Integer> i:block_blockId_search_tree.flatten()){
			System.out.println(i);
		}
		//System.out.println(block_blockId_search_tree.sxing(new myCpr("rmt",1)) );
		//System.out.println(block_blockId_search_tree.sxing(new myCpr("rmt",1)).getKey().value );
		//System.out.println("accumulation_blockId_tree_TIME 建树时间="+accumulation_blockId_tree_TIME);
		//System.out.println("block_blockId_search_tree_TIME 建树时间="+block_blockId_search_tree_TIME); 

		_key_block_compressed = new byte[(int) key_block_size];
		data_in.read(_key_block_compressed, 0, (int) key_block_size);
//一、
		long start=System.currentTimeMillis(); //获取开始时间 
		for(int i=0;i<26;i++){
	        //lookUp(new String(new byte[]{(byte) (i+'a')}),_key_block_compressed, _key_block_info_list);
	        //System.out.println(new String(new byte[]{(byte) ('a'+i)})); 
        }
        System.out.println("平均查询时间： "+(System.currentTimeMillis()-start)*1.f/26+"ms"); 
        
//Decode_record_block_header
        DataInputStream data_in1 = new DataInputStream(new FileInputStream(f));
        data_in1.skipBytes((int) _record_block_offset);
        _num_record_blocks = _read_number(data_in1);
        long num_entries = _read_number(data_in1);
        System.out.println(num_entries); 
        System.out.println(_num_entries); 
        assert(num_entries == _num_entries);
        long record_block_info_size = _read_number(data_in1);
        long record_block_size = _read_number(data_in1);
        //record block info section
        _record_info_struct_list = new record_info_struct[(int) _num_record_blocks];
        int size_counter = 0;
        long compressed_size_accumulator = 0;
        long decompressed_size_accumulator = 0;
		for(int i=0;i<_num_record_blocks;i++){
            long compressed_size = _read_number(data_in1);
            long decompressed_size = _read_number(data_in1);
            _record_info_struct_list[i] = new record_info_struct(compressed_size, compressed_size_accumulator, decompressed_size, decompressed_size_accumulator);
            accumulation_RecordB_tree.insert(new myCpr<Long, Integer>(decompressed_size_accumulator,i));
            compressed_size_accumulator+=compressed_size;
            decompressed_size_accumulator+=decompressed_size;
            size_counter += _number_width * 2;
		}
System.out.println("_num_record_blocks: "+_num_record_blocks);
System.out.println("_num_key_blocks: "+_num_key_blocks);
        assert(size_counter == record_block_info_size);
        block_blockId_search_tree.print();
//二、
        show("_encoding is "+_encoding);
        long start1=System.currentTimeMillis(); //获取开始时间 
        System.out.println("查询 "+key+" ： "+getRecordAt(lookUp(key,_key_block_compressed, _key_block_info_list)));
        //getRecordAt(lookUp(key,_key_block_compressed, _key_block_info_list));
        System.out.println("查询 "+key+" 时间： "+(System.currentTimeMillis()-start1)+"ms"); 
        
}
    

    
    
    
    
    
    
    
    
    
   
    public static String getRecordAt(int position) throws IOException {
        int blockId = accumulation_blockId_tree.xxing(new mdict.myCpr(position,1)).getKey().value;
        key_info_struct infoI = _key_block_info_list[blockId];
        long start = infoI.key_block_compressed_size_accumulator;
        long compressedSize;
        show("blockId:"+blockId);
//decode record block
        DataInputStream data_in = new DataInputStream(new FileInputStream(f));
        // record block info section
        data_in.skipBytes( (int) (_record_block_offset+_number_width*4+_num_record_blocks*2*_number_width));
        
        // actual record block data
        int i = (int) (position-infoI.num_entries_accumulator);//处于当前record块的第几个
        //infoI.key_offsets[i] 获取Key_ID,即文件偏移
        record_info_struct RinfoI = _record_info_struct_list[accumulation_RecordB_tree.xxing(new mdict.myCpr(infoI.key_offsets[i],1)).getKey().value];
        data_in.skipBytes((int) RinfoI.compressed_size_accumulator);
        //whole section of record_blocks;
       // for(int i123=0; i123<record_block_info_list.size(); i123++){
        	long compressed_size = RinfoI.compressed_size;
        	long decompressed_size = RinfoI.decompressed_size;//用于验证
        	byte[] record_block_compressed = new byte[(int) compressed_size];
        	data_in.read(record_block_compressed);//+8 TODO optimize
            // 4 bytes indicates block compression type
        	byte[] record_block_type = new byte[4];
        	System.arraycopy(record_block_compressed, 0, record_block_type, 0, 4);
        	String record_block_type_str = new String(record_block_type);
        	// 4 bytes adler checksum of uncompressed content
        	ByteBuffer sf1 = ByteBuffer.wrap(record_block_compressed);
            int adler32 = sf1.order(ByteOrder.BIG_ENDIAN).getInt(4);
            byte[] record_block = new byte[1];
            // no compression
            if(record_block_type_str.equals(new String(new byte[]{0,0,0,0}))){
            	record_block = new byte[(int) (compressed_size-8)];
            	System.arraycopy(record_block_compressed, 8, record_block, 0, record_block.length-8);
            }
            // lzo compression
            else if(record_block_type_str.equals(new String(new byte[]{1,0,0,0}))){
                long st=System.currentTimeMillis(); //获取开始时间 
                record_block = new byte[(int) decompressed_size];
                MInt len = new MInt((int) decompressed_size);
                byte[] arraytmp = new byte[(int) compressed_size];
                System.arraycopy(record_block_compressed, 8, arraytmp, 0,(int) (compressed_size-8));
                MiniLZO.lzo1x_decompress(arraytmp,(int) compressed_size,record_block,len);
            	//System.out.println("get Record LZO decompressing key blocks done!") ;
                //System.out.println("解压Record耗时："+(System.currentTimeMillis()-st));
            }
            // zlib compression
            else if(record_block_type_str.equals(new String(new byte[]{02,00,00,00}))){
                record_block = zlib_decompress(record_block_compressed,8);
            }
            // notice not that adler32 return signed value
            assert(adler32 == (calcChecksum(record_block) ));
            assert(record_block.length == decompressed_size );
 //当前内容块解压完毕
            
            // split record block according to the offset info from key block
            //String key_text = key_list[i];
            long record_start = infoI.key_offsets[i]-RinfoI.decompressed_size_accumulator;
            long record_end;
            if (i < infoI.num_entries-1){
            	record_end = Long.valueOf(infoI.key_offsets[i+1])-RinfoI.decompressed_size_accumulator; 	
            }
            else{
            	record_end = record_block.length;
            }
            byte[] record = new byte[(int) (record_end-record_start)];         
            System.arraycopy(record_block, (int) (record_start), record, 0, record.length);

            String record_str;
            record_str = new String(record,_encoding); 	
            // substitute styles
            //if self._substyle and self._stylesheet:
            //    record = self._substitute_stylesheet(record);
            return	record_str;           	
    }
    
    //到底要不要将key entrys存储起来？？
    public static void prepareItemByKeyInfo(key_info_struct infoI,int blockId){
        if(infoI.keys==null){
            long start = infoI.key_block_compressed_size_accumulator;
            long compressedSize;
            long st=System.currentTimeMillis(); //获取开始时间 

        	infoI.ini();
            byte[] key_block = new byte[1];
            if(blockId==_key_block_info_list.length-1)
                compressedSize = _key_block_compressed.length - _key_block_info_list[_key_block_info_list.length-1].key_block_compressed_size_accumulator;
            else
                compressedSize = _key_block_info_list[blockId+1].key_block_compressed_size_accumulator-infoI.key_block_compressed_size_accumulator;
                
            String key_block_compression_type = new String(new byte[]{_key_block_compressed[(int) start],_key_block_compressed[(int) (start+1)],_key_block_compressed[(int) (start+2)],_key_block_compressed[(int) (start+3)]});
            int adler32 = getInt(_key_block_compressed[(int) (start+4)],_key_block_compressed[(int) (start+5)],_key_block_compressed[(int) (start+6)],_key_block_compressed[(int) (start+7)]);
            if(key_block_compression_type.equals(new String(new byte[]{0,0,0,0}))){
                //无需解压
                System.out.println("no compress!");
                key_block = new byte[(int) (_key_block_compressed.length-start-8)];
                System.arraycopy(_key_block_compressed, (int)(start+8), key_block, 0,(int) (_key_block_compressed.length-start-8));
            }else if(key_block_compression_type.equals(new String(new byte[]{1,0,0,0})))
            {
                //key_block = lzo_decompress(_key_block_compressed,(int) (start+_number_width),(int)(compressedSize-_number_width));
            	MInt len = new MInt((int) infoI.key_block_decompressed_size);
            	key_block = new byte[len.v];
                byte[] arraytmp = new byte[(int) compressedSize];
                show(arraytmp.length+"哈哈哈"+compressedSize);
                System.arraycopy(_key_block_compressed, (int)(start+8), arraytmp, 0,(int) (compressedSize-8));
            	MiniLZO.lzo1x_decompress(arraytmp,arraytmp.length,key_block,len);
                //System.out.println("look up LZO decompressing key blocks done!");
            }
            else if(key_block_compression_type.equals(new String(new byte[]{02,00,00,00}))){
                key_block = zlib_decompress(_key_block_compressed,(int) (start+8),(int)(compressedSize-8));
                //System.out.println("zip!");
            }
            //!!spliting curr Key block
            int key_start_index = 0;
            String delimiter;
            int width = 0,i1=0,key_end_index=0;
            int keyCounter = 0;
            ByteBuffer sf = ByteBuffer.wrap(key_block);//must outside of while...
            /*主要耗时步骤
            主要耗时步骤
            主要耗时步骤*/
            while(key_start_index < key_block.length){
            	long key_id;
            	if(_version<2)
            		key_id = sf.getInt(key_start_index);//Key_ID
            	else
            		key_id = sf.getLong(key_start_index);//Key_ID
                //show("key_id"+key_id);
                if(_encoding.startsWith("UTF-16")){//TODO optimize
                    width = 2;
                    key_end_index = key_start_index + _number_width;  
                    while(i1<key_block.length){
                    	if(key_block[key_end_index]==0 && key_block[key_end_index+1]==0)
                    		break;
                    	key_end_index+=width;
                    }
                }else{
                    width = 1;
                    key_end_index = key_start_index + _number_width;  
                    while(i1<key_block.length){
                    	if(key_block[key_end_index]==0)
                    		break;
                    	key_end_index+=width;
                    }
                }

                //show("key_start_index"+key_start_index);
                byte[] arraytmp = new byte[key_end_index-(key_start_index+_number_width)];
                System.arraycopy(key_block,key_start_index+_number_width, arraytmp, 0,arraytmp.length);
                
                String key_text = null;
				try {
					key_text = new String(arraytmp,_encoding);
				} catch (UnsupportedEncodingException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
                key_start_index = key_end_index + width;
                infoI.keys[keyCounter]=key_text;
                
                infoI.key_offsets[keyCounter]=key_id;
                keyCounter++;
            }
            assert(adler32 == (calcChecksum(key_block)));
            long e=System.currentTimeMillis(); //获取开始时间 
            System.out.println("建key表时间"+(e-st));
        }
    }
    
    //for android list view
	public String getEntryAt(int position) {
        int blockId = accumulation_blockId_tree.xxing(new mdict.myCpr(position,1)).getKey().value;
        key_info_struct infoI = _key_block_info_list[blockId];
        long start = infoI.key_block_compressed_size_accumulator;
        long compressedSize;
        prepareItemByKeyInfo(infoI,blockId);
        return infoI.keys[(int) (position-infoI.num_entries_accumulator)];
		
	}

    public static int lookUp(String keyword,
                            byte[] _key_block_compressed,
                            key_info_struct[] _key_block_info_list)
                            throws UnsupportedEncodingException
    {
    	keyword = keyword.toLowerCase().replaceAll("[: . , - ]","");
        int blockId = block_blockId_search_tree.sxing(new myCpr(keyword,1)).getKey().value;
        //show("blockId:"+blockId);
        while(blockId!=0 && _key_block_info_list[blockId].headerKeyText.compareTo(keyword)>0)
        	blockId--;
        
        key_info_struct infoI = _key_block_info_list[blockId];

        prepareItemByKeyInfo(infoI,blockId);
        int res;
        if(_encoding.equals("GB18030")){
            res = binary_find_closest2(infoI.keys,keyword);//keyword
        }else
        	res = binary_find_closest(infoI.keys,keyword);//keyword

        if (res==-1){
        	System.out.println("search failed!");
        	return -1;
        }
        else{
        	String KeyText= infoI.keys[res];
        	//show("match key "+KeyText+" at "+res);
        	long lvOffset = infoI.num_entries_accumulator+res;
        	long wjOffset = infoI.key_block_compressed_size_accumulator+infoI.key_offsets[res];
        	return (int) lvOffset;
        }   
    }
    
    //deprecate?
    public static int  binary_find_closest(String[] array,String val){
    	int middle = 0;
    	int iLen = array.length;
    	int low=0,high=iLen-1;
    	if(array==null || iLen<1){
    		return -1;
    	}
    	if(iLen==1){
    		return 0;
    	}
    	if(val.compareTo(array[0].toLowerCase().replace(" ","").replace("-",""))<=0){
			return 0;
    	}else if(val.compareTo(array[iLen-1].toLowerCase().replace(" ","").replace("-",""))>=0){
    		return iLen-1;
    	}
    	int counter=0;
    	int subStrLen1,subStrLen0,cprRes1,cprRes0,cprRes;String houXuan1,houXuan0;
    	while(low<high){
    		counter+=1;
    		System.out.println(low+":"+high);
    		middle = (low+high)/2;
    		houXuan1 = array[middle+1].toLowerCase().replaceAll("[: . , - ]","");
    		houXuan0 = array[middle  ].toLowerCase().replaceAll("[: . , - ]","");
    		cprRes1=houXuan1.compareTo(val);
        	cprRes0=houXuan0.compareTo(val);
        	if(cprRes1>0&&cprRes0>=0){
        		high=middle;
        	}else if(cprRes1<=0&&cprRes0<0){
        		//System.out.println("cprRes1<=0 && cprRes0<0");
        		//System.out.println(houXuan1);
        		//System.out.println(houXuan0);
        		low=middle+1;
        	}else if(cprRes1>=0 && cprRes0<0){
        		low=middle+1;
        	}else{
        		high=middle;
        	}
    	}
    	
    	int resPreFinal;
    	if(low==high) resPreFinal = high;
    	else{
    		resPreFinal = Math.abs(array[low].toLowerCase().replace(" ","").replace("-","").compareTo(val))>Math.abs(array[high].toLowerCase().replace(" ","").replace("-","").compareTo(val))?high:low;
    	}
		//System.out.println(resPreFinal);
		System.out.println("执行了几次："+counter);
    	houXuan1 = array[resPreFinal].toLowerCase().replace(" ","").replace("-","");
show("houXuan1"+houXuan1);
    	if(val.length()>houXuan1.length())
    		return -1;//判为矢匹配.
    	else{
    		if(houXuan1.substring(0,val.length()).compareTo(val)!=0)
    			return -1;//判为矢匹配.
    		else return resPreFinal;//
    	}
    }
    
    //binary_find_closest: with_charset! with_charset! with_charset!
    public static int  binary_find_closest2(String[] array,String val) throws UnsupportedEncodingException{
    	int middle = 0;
    	int iLen = array.length;
    	int low=0,high=iLen-1;
    	if(array==null || iLen<1){
    		return -1;
    	}
    	if(iLen==1){
    		return 0;
    	}
    	byte[] valBA = val.getBytes(_encoding);
    	
    	if(compareByteArray(valBA,array[0].toLowerCase().replaceAll("[: . , - ]","").getBytes(_encoding))<=0){
    		return 0;
    	}else if(compareByteArray(valBA,array[iLen-1].toLowerCase().replaceAll("[: . , - ]","").getBytes(_encoding))>=0){
    		System.out.println("执行了几asdas次：");
    		return iLen-1;
    	}
    	int counter=0;
    	int subStrLen1,subStrLen0,cprRes1,cprRes0,cprRes;
    	byte[] houXuan1BA,houXuan0BA;
    	while(low<high){
    		counter+=1;
    		//System.out.println(low+":"+high);
    		middle = (low+high)/2;
    		houXuan1BA = array[middle+1].toLowerCase().replaceAll("[: . , - ]","").getBytes(_encoding);
    		houXuan0BA = array[middle  ].toLowerCase().replaceAll("[: . , - ]","").getBytes(_encoding);
    		cprRes1=compareByteArray(houXuan1BA,valBA);
        	cprRes0=compareByteArray(houXuan0BA,valBA);
        	if(cprRes1>0&&cprRes0>=0){
        		high=middle;
        	}else if(cprRes1<=0&&cprRes0<0){
        		//System.out.println("cprRes1<=0 && cprRes0<0");
        		//System.out.println(houXuan1BA);
        		//System.out.println(houXuan0BA);
        		low=middle+1;
        	}else if(cprRes1>=0 && cprRes0<0){
        		low=middle+1;
        	}else{
        		high=middle;
        	}
    	}
    	
    	int resPreFinal;
    	if(low==high) resPreFinal = high;
    	else{
    		resPreFinal = Math.abs(compareByteArray(array[low].toLowerCase().replaceAll("[: . , - ]","").getBytes(_encoding),valBA))>Math.abs(compareByteArray(array[high].toLowerCase().replaceAll("[: . , - ]","").getBytes(_encoding),valBA))?high:low;
    	}
		//System.out.println(resPreFinal);
		//System.out.println("执行了几次："+counter);
    	String houXuan1 = array[resPreFinal].toLowerCase().replaceAll("[: . , - ]","");
//show("houXuan1"+houXuan1);
    	if(val.length()>houXuan1.length())
    		return -1;//判为矢匹配.
    	else{
    		if(houXuan1.substring(0,val.length()).compareTo(val)!=0)
    			return -1;//判为矢匹配.
    		else return resPreFinal;//
    	}
    }
    //per-byte comparing byte array
    static int compareByteArray(byte[] A,byte[] B){
    	int la = A.length,lb = B.length;
    	for(int i=0;i<Math.min(la, lb);i++){
    		int cpr = (int)(A[i]&0xff)-(int)(B[i]&0xff);
    		if(cpr==0)
    			continue;
    		return cpr;
    	}
    	if(la==lb)
    		return 0;
    	else return la>lb?1:-1;
    }
    
    private static key_info_struct[] _decode_key_block_info(byte[] key_block_info_compressed) throws UnsupportedEncodingException {
        key_info_struct[] _key_block_info_list = new key_info_struct[(int) _num_key_blocks];
    	byte[] key_block_info;
    	if(_version >= 2)
        {   //zlib压缩
    		byte[] asd = new byte[]{key_block_info_compressed[0],key_block_info_compressed[1],key_block_info_compressed[2],key_block_info_compressed[3]};
    		assert(new String(asd).equals(new String(new byte[]{2,0,0,0})));
            //处理 Ripe128md 加密的 key_block_info
    		if(_encrypt==2){try{
                key_block_info_compressed = _mdx_decrypt(key_block_info_compressed);
                } catch (IOException e) {e.printStackTrace();}}
			//!!!getInt CAN BE NEGTIVE ,INCONGRUENT to python CODE
    		//!!!MAY HAVE BUG
            int adler32 = getInt(key_block_info_compressed[4],key_block_info_compressed[5],key_block_info_compressed[6],key_block_info_compressed[7]);
            key_block_info = zlib_decompress(key_block_info_compressed,8);
            assert(adler32 == (calcChecksum(key_block_info) ));
        }
        else
            key_block_info = key_block_info_compressed;
    	// decoding……
        ByteBuffer sf = ByteBuffer.wrap(key_block_info);
        byte[] textbuffer = new byte[1];
        String headerKeyText,tailerKeyText;
        long key_block_compressed_size = 0,key_block_decompressed_size = 0;
        long start1,end1,start2,end2;
        int accumulation_ = 0,num_entries=0;//how many entries before one certain block.for construction of a list.
        int byte_width = 2,text_term = 1;//DECREPTING version1...bcz I am lazy
        if(_version<2)
        {byte_width = 1;text_term = 0;}
        //System.out.println("_version is"+_version+byte_width);
        //遍历blocks
        for(int i=0;i<_key_block_info_list.length;i++){
            // number of entries in current key block

            start1=System.currentTimeMillis(); //获取开始时间  
        	accumulation_blockId_tree.insert(new myCpr<Integer, Integer>(accumulation_,i));
            end1=System.currentTimeMillis(); //获取结束时间
            accumulation_blockId_tree_TIME+=end1-start1;
            if(_version<2)
            _key_block_info_list[i] = new key_info_struct(sf.getInt(),accumulation_);
            else
            _key_block_info_list[i] = new key_info_struct(sf.getLong(),accumulation_);

            key_info_struct infoI = _key_block_info_list[i];
            accumulation_ += infoI.num_entries;
            
            //![0] head word text
            int text_head_size;
            if(_version<2)
            	text_head_size = sf.get();
        	else
        		text_head_size = sf.getChar();
            if(!_encoding.startsWith("UTF-16")){
                textbuffer = new byte[text_head_size];
                sf.get(textbuffer, 0,text_head_size);
                if(_version>=2)
                sf.get();                
            }else{
                textbuffer = new byte[text_head_size*2];
                sf.get(textbuffer, 0, text_head_size*2);
                if(_version>=2)
                sf.get();if(_version>=2)sf.get();                
            }
            
            infoI.headerKeyText = new String(textbuffer,_encoding);
            show(infoI.headerKeyText);
            
            //![1]  tail word text
            int text_tail_size;
            if(_version<2)
            	text_tail_size = sf.get();
        	else
        		text_tail_size = sf.getChar();
            if(!_encoding.startsWith("UTF-16")){
                textbuffer = new byte[text_tail_size];
                sf.get(textbuffer, 0, text_tail_size);
                if(_version>=2)
                sf.get();         
            }else{
                textbuffer = new byte[text_tail_size*2];
                sf.get(textbuffer, 0, text_tail_size*2);
                if(_version>=2)
                sf.get();if(_version>=2)sf.get();             
            }
            
            infoI.tailerKeyText = new String(textbuffer,_encoding);
            show(infoI.tailerKeyText);
            
            //infoI = new key_info_struct(headerKeyText,
            //		tailerKeyText,
            //		key_block_compressed_size,
            //		key_block_decompressed_size);
            
            infoI.key_block_compressed_size_accumulator = key_block_compressed_size;
            if(_version<2){
            	key_block_compressed_size += sf.getInt();
            	infoI.key_block_decompressed_size = sf.getInt();
            }else{
            	key_block_compressed_size += sf.getLong();
            	infoI.key_block_decompressed_size = sf.getLong();
            }
            start2=System.currentTimeMillis(); //获取开始时间  
            block_blockId_search_tree.insert(new myCpr<String, Integer>(infoI.headerKeyText,i));
            //block_blockId_search_tree.insert(new myCpr<String, Integer>(tailerKeyText,i));
            end2=System.currentTimeMillis(); //获取结束时间
            block_blockId_search_tree_TIME+=end2-start2;
        }
        //assert(accumulation_ == self._num_entries)
        return _key_block_info_list;
	}
    

    //解压等utils
    public static byte[] zlib_decompress(byte[] encdata,int offset) {
	    try {
			    ByteArrayOutputStream out = new ByteArrayOutputStream(); 
			    InflaterOutputStream inf = new InflaterOutputStream(out); 
			    inf.write(encdata,offset, encdata.length-offset); 
			    inf.close(); 
			    return out.toByteArray(); 
		    } catch (Exception ex) {
		    	ex.printStackTrace(); 
		    	return "ERR".getBytes(); 
		    }
    }
    public static byte[] zlib_decompress(byte[] encdata,int offset,int size) {
	    try {
			    ByteArrayOutputStream out = new ByteArrayOutputStream(); 
			    InflaterOutputStream inf = new InflaterOutputStream(out); 
			    inf.write(encdata,offset, size); 
			    inf.close(); 
			    return out.toByteArray(); 
		    } catch (Exception ex) {
		    	ex.printStackTrace(); 
		    	return "ERR".getBytes(); 
		    }
    }    
 
  
    static void show(String val){System.out.println(val);}

	private static long _read_number(ByteBuffer sf) {
    	if(_number_width==4)
    		return sf.getInt();
    	else
    		return sf.getLong();
	}
	private static long _read_number(DataInputStream  sf) {
    	if(_number_width==4)
			try {
				return sf.readInt();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return 0;
			}
		else
			try {
				return sf.readLong();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return 0;
			}
	}

	private static int calcChecksum(byte[] bytes) {
        Adler32 a32 = new Adler32();
        a32.update(bytes);
        int sum = (int) a32.getValue();
        return sum;
    }
    
	public static short getShort(byte buf1, byte buf2) 
    {
        short r = 0;
        r |= (buf1 & 0x00ff);
        r <<= 8;
        r |= (buf2 & 0x00ff);
        return r;
    }
    
    public static int getInt(byte buf1, byte buf2, byte buf3, byte buf4) 
    {
        int r = 0;
        r |= (buf1 & 0x000000ff);
        r <<= 8;
        r |= (buf2 & 0x000000ff);
        r <<= 8;
        r |= (buf3 & 0x000000ff);
        r <<= 8;
        r |= (buf4 & 0x000000ff);
        return r;
    }
   
    public static String byteTo16(byte bt){
        String[] strHex={"0","1","2","3","4","5","6","7","8","9","a","b","c","d","e","f"};
        String resStr="";
        int low =(bt & 15);
        int high = bt>>4 & 15;
        resStr = strHex[high]+strHex[low];
        return resStr;
    }
}



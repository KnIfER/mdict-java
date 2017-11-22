  
package plod;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.Adler32;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterOutputStream;


/**
 * Java conversion
 *	change the .mdx path and watch the output
 *  no o-o classes,
 *  just a streight forward test
 * @author KnIfER
 * @date 2017/11/22
 */
public class mdict {
	//print key texts and content.
	static boolean doUWantuPrintEverything=false;
	
	static int _encrypt=0;
	static String _encoding="";
	static String _passcode = "";
	static HashMap<Integer,String[]> _stylesheet = new HashMap<Integer,String[]>();
	static float _version;
	static int _number_width;
	static int _key_block_offset;
	static long _record_block_offset;
	static long _num_entries;
	
	
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

    public static void main(String[] args) throws IOException  {
//![]File in
    	//byte[] asd = new byte[]{'s',2,3,4,1,2,3,4,1,2,3,4};NameOfPlants.mdx 简明英汉汉英词典.mdx
    	File f = new File("F:\\dictionary_wkst\\omidict-analysis-master\\NameOfPlants.mdx");
    	//FileInputStream data_in =new FileInputStream(f);	
    	DataInputStream data_in =new DataInputStream(new FileInputStream(f));	
//![0]read_header 
    	// number of bytes of header text
    	byte[] itemBuf = new byte[4];
		data_in.read(itemBuf, 0, 4);
    	int header_bytes_size =getInt(itemBuf[0],itemBuf[1],itemBuf[2],itemBuf[3]);
    	System.out.println("header_bytes_size is:"+header_bytes_size);
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
			//for(int i=0;i<=m.groupCount();i++)
	        // System.out.println("Found value: " + m.group(i)); 
			header_tag.put(m.group(1), m.group(2));
	      }
		if (_encoding==""){
			_encoding = header_tag.get("Encoding");
            // GB18030 > GBK > GB2312
            if(_encoding =="GBK"|| _encoding =="GB2312")
            	_encoding = "GB18030";
		}
        // encryption flag
        //   0x00 - no encryption
        //   0x01 - encrypt record block
        //   0x02 - encrypt key info block
		if(!header_tag.containsKey("Encrypted") || header_tag.get("Encrypted") == "0")
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
        // the following numbers could be encrypted
        int num_bytes;
        if(_version >= 2)
            num_bytes = 8 * 5;
        else
            num_bytes = 4 * 4;
		itemBuf = new byte[num_bytes];
		data_in.read(itemBuf, 0, num_bytes);
        if(_encrypt==1){
            if(_passcode=="")
            	 throw new IllegalArgumentException("_passcode未输入");
        //删除useless加密解密算法
        }
        ByteBuffer sf = ByteBuffer.wrap(itemBuf);
        long num_key_blocks = _read_number(sf);
        //System.out.println(num_key_blocks);
        _num_entries = _read_number(sf);
        //System.out.println(_num_entries);
        // number of bytes of key block info after decompression
        if(_version >= 2.0){
        	long key_block_info_decomp_size = _read_number(sf);
        }
        // number of bytes of key block info
        long key_block_info_size = _read_number(sf);
        // number of bytes of key block
        long key_block_size = _read_number(sf);
        
        // 4 bytes: adler checksum of previous 5 numbers
        if(_version >= 2.0){
            int adler32 = calcChecksum(itemBuf);
    		itemBuf = new byte[4];
    		data_in.read(itemBuf, 0, 4);
            assert adler32 == (getInt(itemBuf[0],itemBuf[1],itemBuf[2],itemBuf[3])& 0xffffffff);
        }
        
        // read key block info, which indicates key block's compressed and decompressed size
		itemBuf = new byte[(int) key_block_info_size];
		data_in.read(itemBuf, 0, (int) key_block_info_size);
        ArrayList<String[]> key_block_info_list = _decode_key_block_info(itemBuf);
        assert(num_key_blocks == key_block_info_list.size());

        // read key block
		itemBuf = new byte[(int) key_block_size];
		data_in.read(itemBuf, 0, (int) key_block_size);
        // extract key block]
        long start=System.currentTimeMillis(); //获取开始时间  
		ArrayList<String[]> key_list = _decode_key_block(itemBuf, key_block_info_list);

		long end=System.currentTimeMillis(); //获取结束时间  
		System.out.println("_decode_key_block时间： "+(end-start)+"ms");  
		System.out.println(key_list.size()+":"+_num_entries);  
		//for(String i:key_list.keySet())
		//System.out.println(i);
		//System.out.println(key_list.containsValue("a, ab"));
		
        _record_block_offset = _key_block_offset+num_bytes+4+key_block_info_size+key_block_size;
		//System.out.println(_record_block_offset);

        data_in.close();
//_read_keys END
//decode record block
        data_in =new DataInputStream(new FileInputStream(f));
        data_in.skipBytes((int) _record_block_offset);
        long num_record_blocks = _read_number(data_in);
        long num_entries = _read_number(data_in);
        assert(num_entries == _num_entries);
        long record_block_info_size = _read_number(data_in);
        long record_block_size = _read_number(data_in);

        // record block info section
        ArrayList<long[]> record_block_info_list = new ArrayList<long[]>();
        int size_counter = 0;
		for(int i=0;i<num_record_blocks;i++){
            long compressed_size = _read_number(data_in);
            long decompressed_size = _read_number(data_in);
            record_block_info_list.add(new long[]{compressed_size, decompressed_size});
            size_counter += _number_width * 2;			
		}
		//System.out.println("s"+record_block_info_list.get(0)[0]) ;
        assert(size_counter == record_block_info_size);
        // actual record block data
        int offset = 0;
        int i = 0;
        size_counter = 0;
        for(int i123=0; i123<record_block_info_list.size(); i123++){
        	long compressed_size = record_block_info_list.get(i123)[0];
        	long decompressed_size = record_block_info_list.get(i123)[1];
        	byte[] record_block_compressed = new byte[(int) compressed_size];
        	//System.out.println(compressed_size) ;
        	//System.out.println(decompressed_size) ;
        	data_in.read(record_block_compressed);
            // 4 bytes indicates block compression type
        	byte[] record_block_type = new byte[4];
        	System.arraycopy(record_block_compressed, 0, record_block_type, 0, 4);
        	String record_block_type_str = new String(record_block_type);
        	//ripemd128.printBytes(record_block_type);
        	// 4 bytes adler checksum of uncompressed content
        	ByteBuffer sf1 = ByteBuffer.wrap(record_block_compressed);
            int adler32 = sf1.order(ByteOrder.BIG_ENDIAN).getInt(4);
            byte[] record_block = new byte[1];
            // no compression
            if(record_block_type_str.equals(new String(new byte[]{0,0,0,0}))){
            	record_block = new byte[(int) (compressed_size-8)];
            	System.arraycopy(record_block_compressed, 8, record_block, 0, record_block.length-8);
            	//System.out.println(1) ;
            }
            // lzo compression
            else if(record_block_type_str.equals(new String(new byte[]{1,0,0,0}))){
            	System.out.print("lzo is None") ;
            }
            // zlib compression
            else if(record_block_type_str.equals(new String(new byte[]{02,00,00,00}))){
                // decompress
                record_block = zlib_decompress(record_block_compressed,8);
            }
            // notice not that adler32 return signed value
            assert(adler32 == (calcChecksum(record_block) ));
            assert(record_block.length == decompressed_size );
            // split record block according to the offset info from key block
            while (i < key_list.size() ){
                long record_start = Long.valueOf(key_list.get(i)[0]);
        		String key_text =key_list.get(i)[1];
                // reach the end of current record block
                if (record_start - offset >= record_block.length)
                    break;
                long record_end;
				// record end index
                if (i < key_list.size()-1){
                	record_end = Long.valueOf(key_list.get(i+1)[0]); 	
                }
                    
                else{
                	record_end = record_block.length + offset;
                }
                    
                i += 1;
                byte[] record = new byte[(int) (record_end-record_start)];
                System.arraycopy(record_block, (int) (record_start-offset), record, 0, record.length);
                // convert to utf-8
                String record_str = new String(record,_encoding);
                // substitute styles
                //if self._substyle and self._stylesheet:
                //    record = self._substitute_stylesheet(record);
                if(doUWantuPrintEverything)
                	System.out.println(key_text+record_str );           	
            }

            offset += record_block.length;
            size_counter += compressed_size;
        }

        assert(size_counter == record_block_size);

		
		
		
}


    
    
    
    
    private static ArrayList<String[]> _decode_key_block(byte[] key_block_compressed,ArrayList<String[]> key_block_info_list) throws UnsupportedEncodingException {
    	ArrayList<String[]> key_list = new ArrayList<String[]>();
        int i = 0;
        int dprssTime = 0;
        int splTime = 0;
        long blocktrieTime = 0;
	    ByteArrayOutputStream out = new ByteArrayOutputStream();
	    Inflater decompresser = new Inflater(); 
	    //InflaterOutputStream inf = new InflaterOutputStream(out); 
	    byte[] buf = new byte[10240];
        
		for(int idx=0;idx<key_block_info_list.size();idx++){//compressed_size, decompressed_size
            String[] infoI = key_block_info_list.get(idx);
            byte[] key_block = new byte[1];
            long start = i;
            long end = i + Long.valueOf(infoI[0]);
            // 4 bytes : compression type
            String key_block_type = new String(new byte[]{key_block_compressed[(int) start],key_block_compressed[(int) (start+1)],key_block_compressed[(int) (start+2)],key_block_compressed[(int) (start+3)]});
            
            // 4 bytes : adler checksum of decompressed key block
            int adler32 = getInt(key_block_compressed[(int) (start+4)],key_block_compressed[(int) (start+5)],key_block_compressed[(int) (start+6)],key_block_compressed[(int) (start+7)]);
            if(key_block_type.equals(new String(new byte[]{0,0,0,0}))){
            	System.out.println("no compress!");
            	key_block = new byte[(int) (key_block_compressed.length-start-8)];
            	System.arraycopy(key_block_compressed, (int)(start+8), key_block, 0,(int) (key_block_compressed.length-start-8));
            }else if(key_block_type.equals(new String(new byte[]{1,0,0,0})))
            {
                //if lzo is None:
            	System.out.println("LZO compression is not supported");
                break;
                // decompress key block
                //header = b"\xf0" + pack(">I", deinfoI[0])
                //key_block = lzo.decompress(header + key_block_compressed[start+8:end])
            }
            else if(key_block_type.equals(new String(new byte[]{02,00,00,00}))){
                // decompress key block
    	        long start2=System.currentTimeMillis(); //获取开始时间  zlib_decompress
                key_block = zlib_decompress(key_block_compressed,(int) (start+8));
                //key_block = zlib_decompress2(key_block_compressed,start+8);
    		   
                
    	        /*try {
    			    out.reset();
    			    InflaterOutputStream inf = new InflaterOutputStream(out);   

    			    inf.write(key_block_compressed,start+8, key_block_compressed.length-start-8); 
    			    inf.close();key_block = out.toByteArray(); 
    			    
    		    } catch (Exception ex) {
    		    	ex.printStackTrace(); 
    		    }*/
    			long end2=System.currentTimeMillis(); //获取结束时间  
    			dprssTime+=end2-start2;
            }
            // extract one single key block into a key list
        	//System.out.println("key_block.length is:"+key_block.length);
            //!!spliting Key block
            long start2=System.currentTimeMillis(); //获取开始时间  
            //key_list.putAll(_split_key_block(key_block));
            //INCONGRUENT putAll will consume 3* time.don't use that
            int key_start_index = 0;
            String delimiter;
            int width = 0;
            ByteBuffer sf = ByteBuffer.wrap(key_block);
            String key_block_str = new String(key_block,"ASCII");
        	//System.out.println("1,,,"+key_block_str.length());
        	//System.out.println("2,,,"+key_block.length);
            
            //System.out.println(ttt);
            //String ttt = new String(key_block,"ASCII");
			//TODO:摒弃mdx文件格式，引入保留字。
			//String[] list = ttt.split(new String(new byte[]{0}));
			//String[] list = ttt.split("("+"?<="+"["+"^"+new String(new byte[]{0})+"]"+")"+new String(new byte[]{0}));
			
			//for(String i11:ttt.split("("+"?<="+"["+"^"+new String(new byte[]{0})+"]"+")"+new String(new byte[]{0}))){
				
				//if(i11!=""){
					//ripemd128.printBytes(i11.getBytes());
					//System.out.println(i11.length());
					//System.out.println(i11);	
					//System.out.println(i11.substring(8));	
				//}

			//}
				//System.out.println(i11.getBytes());
            if(_encoding == "UTF-16"){
                //delimiter = "  ";
                width = 2;}else{
                	 width = 1;
                }

            //遍历整个block
            long start3=System.currentTimeMillis(); //获取开始时间  
            while(key_start_index < key_block.length){
                // the corresponding record"s offset in record block
            	//System.out.println("key_start_index"+key_start_index);
            	int i1;
                String key_id = String.valueOf(sf.getLong(key_start_index)) ;
                // key text ends with "\x00"
                int key_end_index = 0;
                //System.out.println(_encoding);
                if(_encoding == "UTF-16"){
                    //delimiter = "  ";
                    width = 2;
                    key_end_index = key_block_str.indexOf(' ');
                    i1 = key_start_index + _number_width;
                    /* while(i1 < key_block.length){
                        if(sf.getChar(i1) == ' '){
                            key_end_index = i1;
                            break;
                        }
                        i1 += width;
                    }*/
                    
                    
                }else{
                    //delimiter = " ";
                    width = 1;
                    i1 = key_start_index + _number_width;
                    key_end_index = key_block_str.indexOf(new String(new byte[]{0}),i1);
   
                   /* while(i1 < key_block.length){
                        if(key_block[i1] == new byte[]{0}[0]){
                            key_end_index = i1;
                            break;
                        }
                        i1 += width;
                    }*/
                }
                String key_text = new String(key_block,key_start_index+_number_width,key_end_index-(key_start_index+_number_width));//[key_start_index+_number_width:key_end_index]\
                  try {
    				key_text = new String(key_text.getBytes(),_encoding);
    			} catch (UnsupportedEncodingException e) {
    				e.printStackTrace();
    			}
                //  .decode(self._encoding, errors="ignore").encode("utf-8").strip()
                key_start_index = key_end_index + width;
                if(Long.valueOf(key_id)!=0)
                key_list.add(new String[]{key_id, key_text});
                //if(key_list.size()==122165)
                //System.out.println("key_start_index"+key_id+" "+key_text);
                
            }
            long end3=System.currentTimeMillis(); //获取结束时间  

            long end2=System.currentTimeMillis(); //获取结束时间  
            
            splTime += end2-start2;
            blocktrieTime += end3-start3;
            // notice that adler32 returns signed value
            assert(adler32 == (calcChecksum(key_block)));//& Long.valueOf("ffffffff",16)
            i += Long.valueOf(infoI[0]);
        }
		System.out.println("解压总时间： "+(dprssTime)+"ms");  
		System.out.println("split时间： "+(splTime)+"ms");  
		System.out.println("遍历block时间： "+(blocktrieTime)+"ms"); 
        return key_list;
	}

	private static HashMap<String,String> _split_key_block(byte[] key_block) {
		HashMap<String,String> key_list = new HashMap<String,String>();
        int key_start_index = 0,i = 0;
        String delimiter;
        int width = 0;
        ByteBuffer sf = ByteBuffer.wrap(key_block);
        while(key_start_index < key_block.length){
            // the corresponding record"s offset in record block
        	//System.out.println("key_start_index"+key_start_index);
            String key_id = String.valueOf(sf.getLong(key_start_index)) ;
            // key text ends with "\x00"
            int key_end_index = 0;
            //System.out.println(_encoding);
            if(_encoding == "UTF-16"){
                //delimiter = "  ";
                width = 2;
                while(i < key_block.length){
                    if(sf.getChar(i) == ' '){
                        key_end_index = i;
                        break;
                    }
                    i += width;
                }
            }else{
                //delimiter = " ";
                width = 1;
                i = key_start_index + _number_width;
                while(i < key_block.length){
                    if(key_block[i] == new byte[]{0}[0]){
                        key_end_index = i;
                        break;
                    }
                    i += width;
                }
            }
            //System.out.println("key_end_index"+key_end_index);

            String key_text = new String(key_block,key_start_index+_number_width,key_end_index-(key_start_index+_number_width));//[key_start_index+_number_width:key_end_index]\
              try {
				key_text = new String(key_text.getBytes(),_encoding);
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
            //  .decode(self._encoding, errors="ignore").encode("utf-8").strip()
            key_start_index = key_end_index + width;
            key_list.put(key_id, key_text);
            //System.out.println("key_start_index"+key_start_index);
        }
        return key_list;
	}

	private static ArrayList<String[]> _decode_key_block_info(byte[] key_block_info_compressed) {
    	byte[] key_block_info;
    	if(_version >= 2){// zlib compression
    		byte[] asd = new byte[]{key_block_info_compressed[0],key_block_info_compressed[1],key_block_info_compressed[2],key_block_info_compressed[3]};
    		assert(new String(asd).equals(new String(new byte[]{2,0,0,0})));
            // TODO: decrypt if needed
    		if(_encrypt==2){
                    try {
						key_block_info_compressed = _mdx_decrypt(key_block_info_compressed);
                    } catch (IOException e) {
						e.printStackTrace();
					}
    		}
			//!!!getInt CAN BE NEGTIVE ,INCONGRUENT to python CODE
    		//!!!MAY HAVE BUG
            int adler32 = getInt(key_block_info_compressed[4],key_block_info_compressed[5],key_block_info_compressed[6],key_block_info_compressed[7]);
           
            //System.out.println("asd"+adler32);
            // decompress
            key_block_info = zlib_decompress(key_block_info_compressed,8);
            assert(adler32 == (calcChecksum(key_block_info) ));
        }
        else// no compression
            key_block_info = key_block_info_compressed;
    	// decode
    	ArrayList<String[]> key_block_info_list = new ArrayList<String[]>();
        int num_entries = 0;
        int byte_width,text_term;
        int i = 0;
        //if (_version >= 2){
        byte_width = 2;
        text_term = 1;
    	//DECREPTING version1...
        ByteBuffer sf = ByteBuffer.wrap(key_block_info);
        while(i < key_block_info.length){
            // number of entries in current key block
            num_entries += sf.getLong(i);
            //System.out.println(i+"num_entries:"+num_entries);
            i += _number_width;
            // text head size
            int text_head_size = sf.getChar(i);
            //System.out.println(i+"text_head_size:"+text_head_size);
            i += byte_width;
            // text head
            if(_encoding != "UTF-16")
                i += text_head_size + text_term;
            else
                i += (text_head_size + text_term) * 2;
            // text tail size
            int text_tail_size = sf.getChar(i);
            //System.out.println(i+"text_tail_size:"+(text_tail_size));
            i += byte_width;
            // text tail
            if(_encoding != "UTF-16")
                i += text_tail_size + text_term;
            else
                i += (text_tail_size + text_term) * 2;
            // key block compressed size
            int key_block_compressed_size = (int) sf.getLong(i);
            i += _number_width;
            // key block decompressed size
            int key_block_decompressed_size = (int) sf.getLong(i);
            i += _number_width;
            key_block_info_list.add(new String[]{""+key_block_compressed_size, ""+key_block_decompressed_size});
    	}
        //assert(num_entries == self._num_entries)
        
        return key_block_info_list;
        

	}
    //解压
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
    public static byte[] zlib_decompress2(byte[] data,int offset) {  
        byte[] output = new byte[0];  
  
        Inflater decompresser = new Inflater();  
        decompresser.reset();  
        decompresser.setInput(data,offset,data.length-offset);  

        ByteArrayOutputStream o = new ByteArrayOutputStream(data.length);  
        try {  
            byte[] buf = new byte[1024];  
            while (!decompresser.finished()) {  
                int i = decompresser.inflate(buf);  
                o.write(buf, 0, i);  
            }  
            output = o.toByteArray();  
        } catch (Exception e) {  
            output = data;  
            e.printStackTrace();  
        } finally {  
            try {  
                o.close();  
            } catch (IOException e) {  
                e.printStackTrace();  
            }  
        }  
  
        decompresser.end();  
        return output;  
    }  
    public static byte[] gzip_decompress(byte[] bytes,int offset) {  
        if (bytes == null || bytes.length == 0) {  
            return null;  
        }  
        ByteArrayOutputStream out = new ByteArrayOutputStream();  
        ByteArrayInputStream in = new ByteArrayInputStream(bytes,offset, bytes.length-offset);  
        try {  
            GZIPInputStream ungzip = new GZIPInputStream(in);  
            byte[] buffer = new byte[256];  
            int n;  
            while ((n = ungzip.read(buffer)) >= 0) {  
                out.write(buffer, 0, n);  
            }  
        } catch (IOException e) {  
            e.printStackTrace();
        }  
  
        return out.toByteArray();  
    }  
    

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
	private static long calcChecksum2(byte[] bytes) {
        Adler32 a32 = new Adler32();
        a32.update(bytes);
        return a32.getValue();
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



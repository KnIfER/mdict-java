package com.knziha.plod.dictionary;
//store key_block's summary and itself
public class key_info_struct{
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
	public key_info_struct() {
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
	public byte[] key_block_data;
    public void ini(){
        keys =new String[(int) num_entries];
        key_offsets =new long[(int) num_entries];
    }
}
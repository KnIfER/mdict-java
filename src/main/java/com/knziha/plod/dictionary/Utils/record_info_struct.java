package com.knziha.plod.dictionary.Utils;
    public class record_info_struct{
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
    }
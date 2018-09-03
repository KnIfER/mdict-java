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

package com.knziha.plod.dictionary;
/*store key_block's summary*/
public class key_info_struct{
	public key_info_struct(byte[] headerKeyText, byte[] tailerKeyText,
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
	public byte[] headerKeyText;
	public byte[] tailerKeyText;
	public long key_block_compressed_size_accumulator;
	public long key_block_compressed_size;
	public long key_block_decompressed_size;
    public long num_entries;
    public long num_entries_accumulator;
    //public String[] keys;
    //public long[] key_offsets;
	//public byte[] key_block_data;
    public void ini(){
        //keys =new String[(int) num_entries];
        //key_offsets =new long[(int) num_entries];
    }
}
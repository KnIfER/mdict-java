package com.knziha.plod.dictionary;

public interface UniversalDictionaryInterface {
	public int lookUp(String keyword);

	byte[] getOptions();
	void setOptions(byte[] options);
	int getType();
	
	long getBooKID();
	void setBooKID(long id);

	//String getEntryAt(long position);
	
	long getNumberEntries();
}

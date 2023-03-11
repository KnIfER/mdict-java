package com.knziha.plod.dictionarymodels;

import com.knziha.plod.plaindict.CMN;
import com.knziha.plod.plaindict.MainActivityUIBase;
import com.knziha.plod.plaindict.PDICMainAppOptions;

import java.io.File;
import java.io.IOException;

/**
 For management of books.
 data:2020.01.12
 author:KnIfER
*/
public class MagentTransient extends BookPresenter {
	private final int bIsManagerAgent;
	protected File f;
	public String _Dictionary_fName_Internal;
	int TIFStamp;
	boolean keepOrgHolder=true;
	MainActivityUIBase context;
	
	private boolean changeMap=true;

	public void Rebase(File f){
		CMN.Log("MT0Rebase!!!");
	}


	public MagentTransient(MainActivityUIBase app, File newFile) throws IOException {
		this(app, newFile.getPath(), null, true);
	}

	public MagentTransient(MainActivityUIBase a, String PlaceHolderString, Integer isF, boolean bIsPreempter) throws IOException {
		super(new File(PlaceHolderString), a, 3);
		if(bookImpl==null) {
			File f = new File(PlaceHolderString);
			bookImpl = new DictionaryAdapter(f, null);
			Long bid = bookImplsNameMap.get(f.getName());
			if(bid==null) bid=-1L; // todo check
			bookImpl.setBooKID(bid);
		}
		bIsManagerAgent = 1;
		f = new File(PlaceHolderString);
		_Dictionary_fName_Internal = "."+f.getName();
		
		if (isF!=null) {
			TIFStamp=isF;
		}
		TIFStamp=TIFStamp;
		context = a;
	}
	
	public long getFirstFlag() {
//		if (!bReadConfig) {
//			try {
//				readConfigs(context, context.prepareHistoryCon());
//				FFStamp=firstFlag;
//			} catch (IOException e) { CMN.Log(e); }
//		}
		return FFStamp;
	}
	
	public boolean renameFileTo(MainActivityUIBase c, File to) {
		return false;
	}

	public boolean moveFileTo(MainActivityUIBase c, File to) {
		return false;
	}

	@Override
	public String getDictionaryName() {
//		String ret = mPhI.pathname.startsWith(CMN.AssetTag)?CMN.AssetMap.get(mPhI.pathname):null;
//		if(ret==null)
//			ret=mPhI.getName().toString();
		return f.getName().toString();
	}
}
package com.knziha.plod.dictionary.Utils;


import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class LinkastReUsageHashMap<K,V> extends LinkedHashMap<K,V> {
	int mCapacity;
	public AtomicInteger accommodation;
	private Field f_accessOrder;
	public static int BlockCacheSize=1024;
	public static final int BlockSize=4096;
	private int desiredTotalCacheSize;
	private int perblockSize;

	public LinkastReUsageHashMap(int Capacity) {
		super(Capacity, 1, true);
		mCapacity = Capacity-6;
	}
	
	public LinkastReUsageHashMap(int initialCapacity, int desiredTotalCacheSize, int perblockSize) {
		super(initialCapacity, 1, desiredTotalCacheSize != 0);
		this.desiredTotalCacheSize = desiredTotalCacheSize;
		this.perblockSize = perblockSize;
		this.mCapacity = desiredTotalCacheSize/perblockSize;
	}

	@Override
	protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
		if(mCapacity > 0) {
			if(BlockCacheSize!=desiredTotalCacheSize) {
				desiredTotalCacheSize = BlockCacheSize;
				this.mCapacity = desiredTotalCacheSize/perblockSize;
			}
			return size() > mCapacity;
		}
		return false;
	}
	
	public void setCapacity(int mCapacity) {
		this.mCapacity = mCapacity;
	}
	
	public boolean filled() {
		return size()>mCapacity;
	}

	public void syncAccommodationSize() {
		if(accommodation==null)
			accommodation = new AtomicInteger(0);
		accommodation.set(mCapacity-size());
	}

	public V getSafe(Object key) {
		try {
			if(f_accessOrder==null) {
				f_accessOrder = LinkedHashMap.class.getDeclaredField("accessOrder");
				f_accessOrder.setAccessible(true);
			}
			f_accessOrder.set(this, false);
			V val = get(key);
			f_accessOrder.set(this, true);
			return val;
		} catch (Exception ignored) {
			SU.Log(ignored);
		}
		return super.get(key);
	}
}

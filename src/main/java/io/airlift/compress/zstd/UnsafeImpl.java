package io.airlift.compress.zstd;


import com.knziha.plod.dictionary.Utils.BU;
import io.airlift.compress.IncompatibleJvmException;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.nio.Buffer;
import java.nio.ByteOrder;

import static java.lang.String.format;


public class UnsafeImpl {
	public static long ADDRESS_OFFSET;
	public final static int ARRAY_BYTE_BASE_OFFSET = 0;
	
	public UnsafeImpl() {
		ByteOrder order = ByteOrder.nativeOrder();
		if (!order.equals(ByteOrder.LITTLE_ENDIAN)) {
			throw new IncompatibleJvmException(format("Zstandard requires a little endian platform (found %s)", order));
		}
		ADDRESS_OFFSET = 0;
	}
	
	public long getLong(Object buffer, long addressOffset) {
		return BU.toLongLE((byte[])buffer, (int)addressOffset);
	}
	
	public byte getByte(Object inputBase, long inputAddress){
		return ((byte[])inputBase)[(int) (inputAddress)];
	}

	public int getInt(Object inputBase, long inputAddress) {
		return BU.toIntLE(((byte[])inputBase), (int) inputAddress);
	}
	
	public short getShort(Object inputBase, long inputAddress) {
		return BU.toShortLE((byte[])inputBase, (int) inputAddress);
	}
	
	public void putByte(Object outputBase, long outputAddress, byte symbol) {
		((byte[])outputBase)[(int) (outputAddress)] = symbol;
	}
	
	public void putShort(Object outputBase, long outputAddress, short value) {
		BU.putShortLE(((byte[])outputBase), (int) outputAddress, value);
	}
	
	public void copyMemory(Object inputBase, long inputAddress, Object output, long st, long size) {
		System.arraycopy((byte[])inputBase, (int)inputAddress, (byte[])output, (int)st, (int)size);
	}
	
	public void putLong(Object literalsBuffer, long outputAddress, long value) {
		BU.putLongLE((byte[])literalsBuffer, (int) outputAddress, value);
	}
	
	public void putInt(Object outputBase, long outputAddress, int value) {
		BU.putIntLE((byte[])outputBase, (int) outputAddress, value);
	}
}

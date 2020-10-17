package com.knziha.plod.dictionaryBuilder.Utils;

import com.knziha.plod.dictionary.Utils.ReusableByteOutputStream;

import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;

public class ByteDataOutputStream implements DataOutput {
    final DataOutput output;
    final ReusableByteOutputStream byteArrayOutputSteam;

    public ByteDataOutputStream(ReusableByteOutputStream byteArrayOutputSteam) {
        this.byteArrayOutputSteam = byteArrayOutputSteam;
        this.output = new DataOutputStream(byteArrayOutputSteam);
    }

    public void write(int b) {
        try {
            this.output.write(b);
        } catch (IOException var3) {
            throw new AssertionError(var3);
        }
    }

    public void write(byte[] b) {
        try {
            this.output.write(b);
        } catch (IOException var3) {
            throw new AssertionError(var3);
        }
    }

    public void write(byte[] b, int off, int len) {
        try {
            this.output.write(b, off, len);
        } catch (IOException var5) {
            throw new AssertionError(var5);
        }
    }

    public void writeBoolean(boolean v) {
        try {
            this.output.writeBoolean(v);
        } catch (IOException var3) {
            throw new AssertionError(var3);
        }
    }

    public void writeByte(int v) {
        try {
            this.output.writeByte(v);
        } catch (IOException var3) {
            throw new AssertionError(var3);
        }
    }

    public void writeBytes(String s) {
        try {
            this.output.writeBytes(s);
        } catch (IOException var3) {
            throw new AssertionError(var3);
        }
    }

    public void writeChar(int v) {
        try {
            this.output.writeChar(v);
        } catch (IOException var3) {
            throw new AssertionError(var3);
        }
    }

    public void writeChars(String s) {
        try {
            this.output.writeChars(s);
        } catch (IOException var3) {
            throw new AssertionError(var3);
        }
    }

    public void writeDouble(double v) {
        try {
            this.output.writeDouble(v);
        } catch (IOException var4) {
            throw new AssertionError(var4);
        }
    }

    public void writeFloat(float v) {
        try {
            this.output.writeFloat(v);
        } catch (IOException var3) {
            throw new AssertionError(var3);
        }
    }

    public void writeInt(int v) {
        try {
            this.output.writeInt(v);
        } catch (IOException var3) {
            throw new AssertionError(var3);
        }
    }

    public void writeLong(long v) {
        try {
            this.output.writeLong(v);
        } catch (IOException var4) {
            throw new AssertionError(var4);
        }
    }

    public void writeShort(int v) {
        try {
            this.output.writeShort(v);
        } catch (IOException var3) {
            throw new AssertionError(var3);
        }
    }

    public void writeUTF(String s) {
        try {
            this.output.writeUTF(s);
        } catch (IOException var3) {
            throw new AssertionError(var3);
        }
    }
}
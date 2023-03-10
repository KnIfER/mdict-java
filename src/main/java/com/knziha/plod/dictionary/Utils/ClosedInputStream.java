package com.knziha.plod.dictionary.Utils;

import java.io.InputStream;

public class ClosedInputStream extends InputStream {
    /**
     * A singleton.
     */
    public static final ClosedInputStream CLOSED_INPUT_STREAM = new ClosedInputStream();

    /**
     * Returns -1 to indicate that the stream is closed.
     *
     * @return always -1
     */
    @Override
    public int read() {
        return -1;
    }

}

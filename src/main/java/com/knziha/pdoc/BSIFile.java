/*
 * Copyright (c) 1994, 2013, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.knziha.pdoc;

import java.io.ByteArrayInputStream;

public class BSIFile extends ByteArrayInputStream {
	public BSIFile(byte[] buf, int offset, int length) {
		super(buf, offset, Math.min(length, buf.length-offset));
	}
	public void setPosition(int p) {
		pos = p;
	}
}

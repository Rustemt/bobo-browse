/**
 * Bobo Browse Engine - High performance faceted/parametric search implementation 
 * that handles various types of semi-structured data.  Written in Java.
 * 
 * Copyright (C) 2005-2007  spackle
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 * 
 * To contact the project administrators for the bobo-browse project, 
 * please go to https://sourceforge.net/projects/bobo-browse/, or 
 * contact owner@browseengine.com.
 */

package com.browseengine.bobo.service.data;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;


/**
 * Deals only with UTF-8 bytes for the underlying String.
 * 
 * @author spackle
 *
 */
public class TextData extends BinaryData {
	private String s;
	private Reader r = null;
		
	private static byte[] getEm(String s) {
		try {
			return s.getBytes("UTF-8");
		} catch (UnsupportedEncodingException uee) {
			throw new IllegalStateException("for some reason, \"UTF-8\" is not supported: "+uee, uee);
		}
	}
	
	public TextData() {
		super();
		this.s = null;
	}
	
	public TextData(InputStream is) {
		super(is);
	}
	
	public TextData(InputStream is, int contentLength) {
		super(is,contentLength);
	}
	
	public TextData(String s) {
		super(getEm(s));
		this.s = s;
	}
	
	public TextData(byte[] bytes) {
		super(bytes);
	}
	
	/**
	 * same as {@link #getInputStream()}, but returns a Reader wrapped around the stream.
	 * the same caveats apply.
	 * 
	 * @return
	 * @throws UnsupportedEncodingException
	 */
	public Reader getReader() throws UnsupportedEncodingException {
		InputStream is;
		if (r != null) {
			return r;
		} else if (s != null) {
			return r = new StringReader(s);
		} else if ((is = getInputStream()) != null) {
			return r = new InputStreamReader(is, "UTF-8");
		}
		return null;
	}

	public void close() throws IOException {
		try {
			if (r != null) {
				r.close();
			}
		} finally {
			r = null;
			s = null;
			closed = true;
			super.close();
		}
	}
	
	private boolean closed = false;
	
	/**
	 * same as {@link #getBytes}, but returns them as a String.
	 * the same caveats apply.
	 * 
	 * @return
	 * @throws IOException 
	 */
	public String getString() throws IOException {
		byte[] bytes = getBytes();
		return new String(bytes, "UTF-8");
	}
	
	public String toString() {
		if (s != null) {
			return s;
		} else if (closed) {
			return "closed";
		} else {
			return "stream-based";
		}
	}
	
	@Override
	public final String getContentType() {
		return "text/html; charset=UTF-8";
	}
	
}
/**
 * Bobo Browse Engine - High performance faceted/parametric search implementation 
 * that handles various types of semi-structured data.  Written in Java.
 * 
 * Copyright (C) 2005-2006  spackle
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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.browseengine.bobo.serialize.JSONSerializable;

/**
 * Represents a binary chunk of data that could be stored in a file, 
 * or rendered to the screen.
 * For use in only one thread.
 * 
 * @author spackle
 *
 */
public abstract class BinaryData implements JSONSerializable {
	private static Log logger=LogFactory.getLog(BinaryData.class);

	private InputStream is;
	private byte[] bytes;
	private int contentLength;
	private boolean notFound = false;
	
	protected BinaryData() {
		this.notFound = true;
	}
	
	protected BinaryData(InputStream is) {
		this.is = is;
		contentLength = -1;
		bytes = null;
	}

	protected BinaryData(InputStream is, int contentLength) {
		this.contentLength = contentLength;
		bytes = null;
		this.is = is;
	}
	
	protected BinaryData(byte[] bytes) {
		this.bytes = bytes;
		contentLength = bytes.length;
		is = null;
	}
	
	public boolean isNotFound() {
		return notFound;
	}
	
	public int getContentLength() {
		return contentLength;
	}
	
	/**
	 * Returns an InputStream representing the contents of this data.
	 * note that once you advance thru this stream, you can't 
	 * reset it.
	 * Calls to this method should generally be mutually exclusive from 
	 * calls to {@link #getBytes()}, 
	 * since the latter advances all the way thru the underlying 
	 * InputStream.
	 * 
	 * @return
	 */
	public InputStream getInputStream() {
		if (is != null) {
			return is;	
		} else if (bytes != null) {
			return is = new ByteArrayInputStream(bytes);
		}
		return null;
	}
		
	public void close() throws IOException {
		try {
			if (is != null) {
				is.close();
			}
		} finally {
			bytes = null;
			is = null;
			contentLength = -1;
		}
	}

	private static final int BUF_SIZE = 4096;

	/**
	 * You can only call this method once on an instance, since it 
	 * advances thru the underlying InputStream.  
	 * Calls to this method should generally be mutually exclusive from 
	 * calls to {@link #getInputStream()}, 
	 * since the latter allows the user to advance past bytes that 
	 * Note also that you 
	 * shouldn't call this method if you've advanced thru your InputStream 
	 * that you got by calling getInputStream(), since those bytes are 
	 * gone from the byte array constructed here.
	 * 
	 * @return
	 * @throws IOException
	 */
	public byte[] getBytes() throws IOException {
		if (is != null) {
			if (contentLength >= 0) {
				byte[] answer = new byte[contentLength];
				int idx = 0;
				int toRead;
				int numRead;
				while (idx < contentLength) {
					toRead = Math.min(contentLength-idx, BUF_SIZE);
					numRead = is.read(answer, idx, toRead);
					if (numRead < toRead) {
						String msg = "didn't read the expected number of bytes: "+numRead+" out of "+toRead;
						logger.error(msg);
						throw new IOException(msg);
					}
				}
				return answer;
			} else {
				byte[] buf = new byte[BUF_SIZE];
				byte[] answer = new byte[2*BUF_SIZE];
				byte[] tmp;
				int idx = 0;
				int numRead;
				while ((numRead = is.read(buf, 0, buf.length)) > 0) {
					// got some bytes
					// copy them into answer
					// make sure there's room!
					if (answer.length < idx+numRead) {
						tmp = new byte[answer.length*2];
						System.arraycopy(buf, 0, tmp, 0, idx);
						answer = tmp;
						tmp = null;
					}
					System.arraycopy(buf, 0, answer, idx, numRead);
					idx += numRead;
				}
				// compress
				if (idx != answer.length) {
					tmp = new byte[idx];
					System.arraycopy(answer, 0, tmp, 0, idx);
					answer = tmp;
					tmp = null;
				}
				return answer;
			}
		} else if (bytes != null) {
			// advance all the way thru the stream
			is = new InputStream() {
				public int read() {
					return -1;
				}
			};
			return bytes;
		}
		return null;
	}

	public abstract String getContentType();	
}

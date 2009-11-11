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

package com.browseengine.bobo.server.protocol;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.browseengine.bobo.service.data.BinaryData;
import com.browseengine.bobo.service.data.JPGData;
import com.browseengine.bobo.service.data.TextData;

/**
 * @author spackle
 *
 */
public class BinaryDataHandler {
	// TODO: use reflection to call the constructors
	private static Map<String,Class> map = new HashMap<String,Class>();
	private static final Pattern CONTENT_LENGTH = Pattern.compile("\\d{1,10}");
	
	/**
	 * Reads fully from the input stream, up to 16MB, creating the correct 
	 * BinaryData type.  The problem is that the client http method falls out of 
	 * scope, so we need to bleed the stream before we return; hence, we pull it 
	 * all into memory.
	 * 
	 * <p>
	 * If we have requirements for data larger than 16MB, we should consider using 
	 * temporary FS space and keeping the returned BinaryData object with a 
	 * reference to an InputStream from that file instead.
	 * 
	 * @param contentType
	 * @param contentLength
	 * @param is
	 * @return
	 * @throws IOException
	 */
	public static BinaryData readFully(String contentType, String contentLength, 
			InputStream is) throws IOException {
		int length = -1;
		if (contentLength != null) {
			Matcher m = CONTENT_LENGTH.matcher(contentLength);
			if (m.matches()) {
				length = Integer.parseInt(contentLength);
			}
		}
		if (contentType != null) {
			contentType = contentType.toLowerCase();
			if (is != null) {
				if (contentType.equals("image/jpeg") || contentType.startsWith("text/html")) {
					// read it all; we're pulling it all into memory for now!
					// throw an exception if more than 16MB, or if no contentLength set
					if (length > 16*1024*1024 || length < 0) {
						throw new IOException(length < 0 ? "no content length specified, playing it safe" : "Content-Length of "+length+" is more bytes than we accept");
					}
					int totBytes = 0;
					byte[] bytes;
					if (length == 0) {
						bytes = new byte[0];
					} else {
						bytes = new byte[length];
						int numRead;
						while ((numRead = is.read(bytes, totBytes, Math.min(1024,length-totBytes))) > 0) {
							totBytes += numRead;
						}
						if (length != totBytes) {
							throw new IOException("there was a discrepancy, Content-Length was "+length+" but we only got "+totBytes+" bytes");
						}
					}
					if (contentType.equals("image/jpeg")) {
						return new JPGData(bytes);
					} else if (contentType.startsWith("text/html")) {
						return new TextData(bytes);
					}
				}
			}
		}
		return null;
	}
	
	/**
	 * Writes the BinaryData to the specified OutputStream.  Note that this advances thru 
	 * the BinaryData's underlying InputStream; the object may therefore be exhausted.
	 * Neither the BinaryData instance, its underlying InputStream, nor the OutputStream, 
	 * are closed by this method.  The bytes are flushed to the OutputStream before the 
	 * method returns.
	 * 
	 * @param bd
	 * @param os
	 * @return the number of bytes written
	 * @throws IOException
	 */
	public static int writeFully(BinaryData bd, OutputStream out) throws IOException {
		byte[] buf = new byte[1024]; // 1KB buffer
		int totRead = 0;
		// stream it
		InputStream is = null;
		int numRead;
		is = bd.getInputStream();
		while ((numRead = is.read(buf, 0, buf.length)) > 0) {
			out.write(buf, 0, numRead);
			totRead += numRead;
		}
		out.flush();
		return totRead;
	}
}

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

import java.io.InputStream;

/**
 * Represents JPG image content bytes.
 * For use in only one thread.
 * 
 * @author spackle
 *
 */
public class JPGData extends BinaryData {
	public JPGData() {
		super();
	}
	public JPGData(InputStream is) {
		super(is);
	}

	public JPGData(InputStream is, int contentLength) {
		super(is,contentLength);
	}
	
	public JPGData(byte[] bytes) {
		super(bytes);
	}
	
	private static final String CONTENT_TYPE = "image/jpeg";
	public String getContentType() {
		return CONTENT_TYPE;
	}

}

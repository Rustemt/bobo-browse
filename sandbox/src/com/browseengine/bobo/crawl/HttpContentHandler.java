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
 * please go to https://sourceforge.net/projects/bobo-browse/.
 * <or other contact info for bobo-browse; snail mail/email/both>
 */

package com.browseengine.bobo.crawl;

import java.io.IOException;
import java.net.URL;

/**
 * @author spackle
 *
 */
public interface HttpContentHandler {
	/**
	 * Callback function for what the crawler should do with a block of 
	 * content retrieved from the specified URL, in basic float crawling.
	 * 
	 * @param url
	 * @param content
	 * @return true iff content was handled successfully
	 * @exception IOException if I/O trouble handling the content
	 */
	boolean handleContent(URL url, String content) throws IOException;
}

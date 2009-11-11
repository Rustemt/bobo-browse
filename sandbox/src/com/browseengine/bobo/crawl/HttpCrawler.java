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
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A simple crawler, with no special handling of cookies or anything, 
 * that can either follow some seed-list generator of links to 
 * crawl, or can just pull from a fixed set of URLs (as in the case of 
 * watching RSS feeds).
 * 
 * @author spackle
 *
 */
public class HttpCrawler {
    private static Log logger=LogFactory.getLog(HttpCrawler.class);

    private String _name;
    private long _millisPerRequest;
    private int _maxBytes;
    
    /**
     * 
     * @param botName
     * @param millisPerRequest to avoid getting blacklisted, this is the number of milliseconds to 
     *      take for each request
     * @param maxBytes maximum number of bytes to pull from each URL--this affects how much we want 
     *      to bring in memory, since for now, we pull the whole page into memory.
     */
    public HttpCrawler(String botName, long millisPerRequest, int maxBytes) {
        _name = botName;
        _millisPerRequest = millisPerRequest;
        _maxBytes = maxBytes;
    }
    
    /**
     * use a simple HttpClient to download all the <code>urls</code> specified, and 
     * passing their content over to the appropriate {@link HttpContentHandler}.
     * 
     * 
     * 
     * @param urls
     * @param handler
     * @return the list of urls that we were unable to crawl
     */
    public Collection<URL> crawlFlatURLList(Collection<URL> urls, HttpContentHandler handler) {
        HashSet<URL> unableToCrawl = new HashSet<URL>();
        HttpClient httpclient = new HttpClient();
        byte[] buf = new byte[4096];
        InputStream is = null;
        long prevRequest = System.currentTimeMillis();
        for (URL url : urls) {
            HttpMethod get = null;
            try {
                get = new GetMethod(url.toExternalForm());
                Header useragent = new Header("User-Agent", _name);
                get.addRequestHeader(useragent);
                // enumerate the request headers
                if (logger.isDebugEnabled()) {
                    StringBuilder strbuf = new StringBuilder();
                    Header[] headers = get.getRequestHeaders();
                    strbuf.append((headers != null ? ""+headers.length : null)).append(" headers");
                    if (headers != null) {
                        for (Header header : headers) {
                            strbuf.append('\n').append(header.toString());
                        }
                    }
                    logger.debug(strbuf.toString());
                }
                int returnCode = httpclient.executeMethod(get);
                if (returnCode == 503) {
                    logger.warn("503 code; skipping url: "+url);
                    unableToCrawl.add(url);
                    // BACK OFF! -- assume we are hitting the same site
                    // sleep at least 30 seconds, or at least 2x the normal backoff
                    long startTime = System.currentTimeMillis();
                    long millis = Math.max(2L*_millisPerRequest, 30*1000L);
                    sleepUntil(startTime+millis);
                } else if (returnCode == 200) {
                    logger.info("got response "+returnCode+" for url: "+url);
                    Header header = get.getResponseHeader("Content-Type");
                    String val;
                    // default HTTP encoding (doesn't mean it's right), source:
                    // http://www.w3.org/Protocols/rfc2616/rfc2616-sec3.html#sec3.7.1
                    String enc = "ISO-8859-1";
                    if (header != null && (val = header.getValue()) != null) {
                        Matcher m = CHARSET_PATTERN.matcher(val);
                        if (m.find()) {
                            val = m.group(1);
                            if (Charset.isSupported(val)) {
                                enc = val;
                            } else {
                                logger.warn("unsupported charset, our read might be bad: "+enc+", for url: "+url);
                            }
                        }
                    }
                    is = get.getResponseBodyAsStream();
                    StringBuilder strbuf = new StringBuilder();
                    int numRead = 0;
                    int totRead = 0;
                    while (totRead < _maxBytes && (numRead = is.read(buf)) > 0) {
                        strbuf.append(new String(buf, 0, numRead, enc));
                        totRead += numRead;
                    }
                    if (is.read() >= 0) {
                        logger.warn("we had to cut off the response at: "+url+" because it was a bit too long");
                    }
                    String content = strbuf.toString();
                    if (!handler.handleContent(url, content)) {
                        unableToCrawl.add(url);
                    } else {
                        logger.info("successfully processed content at url: "+url);
                    }
                } else {
                    logger.info("got possibly bad response "+returnCode+" for url: "+url);
                    unableToCrawl.add(url);
                }
            } catch (IOException ioe) {
                logger.warn("trouble with request: "+ioe.getMessage(), ioe);
                unableToCrawl.add(url);
            } finally {
                try {
                    if (is != null) {
                        is.close();
                    }
                } catch (IOException ioe) {
                    logger.warn("trouble closing input stream for url: "+url);
                } finally {
                    try {
                        if (get != null) {
                            get.releaseConnection();
                        }
                    } finally {
                        get = null;
                        is = null;
                    }
                }
            }
            prevRequest = sleepUntil(_millisPerRequest+prevRequest);
        }
        return unableToCrawl;
    }
    
    private long sleepUntil(long timestamp) {
        long current = System.currentTimeMillis();
        long sleepyTime = timestamp-current;
        while (sleepyTime > 0L) {
            try {
                Thread.sleep(sleepyTime);
            } catch (InterruptedException ie) {
                //
            }
            current = System.currentTimeMillis();
            sleepyTime = timestamp-current;
        }
        return current;
    }
    
    public static final Pattern CHARSET_PATTERN = Pattern.compile("charset=([\\-\\w]+)[\\s\\pPunct}&&[^\\-]]?", Pattern.CASE_INSENSITIVE);
    
}

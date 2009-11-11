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

package com.browseengine.bobo.service;

import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONObject;
import org.json.JSONException;

import com.browseengine.bobo.serialize.JSONSerializable;
import com.browseengine.bobo.serialize.JSONSerializer;
import com.browseengine.bobo.serialize.JSONSerializable.JSONSerializationException;
import com.browseengine.bobo.server.protocol.BinaryDataHandler;
import com.browseengine.bobo.api.BrowseException;
import com.browseengine.bobo.service.data.BinaryData;

/**
 * Generic remote client for a service.  You just define your subclass as 
 * implementing your service, provide a constructor that passes an URL to this 
 * super class, and have your implementation method(s) for the service, simply 
 * call this.send(...).  An instance of this class will handle opening a connection 
 * over http, and handles 
 * all the wire-line-over-http stuff.  Use in conjunction with a ServiceServlet on 
 * the server side, and all the JSON serialization/deserialization is handled for you.
 *
 * @author spackle
 *
 */
public abstract class RemoteServiceClient {
    private static Log logger=LogFactory.getLog(RemoteServiceClient.class);

    private URL _url;
    private HttpClient _httpClient;
    private Credentials _cred;

    protected RemoteServiceClient(URL url) {
        this(url, null, null);
    }
    
    protected RemoteServiceClient(URL url, String username, String password) {
        this(url, username, password, 40, 30);
    }
        
    protected RemoteServiceClient(URL url, String username, String password, int maxTotalConnections, int maxConnectionsPerHost) {
        this(url, username, password, 40, 30, 0, 0);
    }
    
    /**
     * Optionally specify the default connection timeout in milliseconds (to establish connections), and the 
     * default socket timeout in milliseconds (for each request to execute), to be used for this client.  
     * Setting either to &lt;= 0 uses the default value.
     * 
     * @param url
     * @param username
     * @param password
     * @param maxTotalConnections
     * @param maxConnectionsPerHost
     * @param connectTimeoutMillis
     * @param socketTimeoutMillis
     */
    protected RemoteServiceClient(URL url, String username, String password, int maxTotalConnections, int maxConnectionsPerHost, int connTimeoutMillis, int socketTimeoutMillis) {
        _url=url;
        _httpClient = new HttpClient();
        
        if (username!=null){
            _cred=new UsernamePasswordCredentials(username,password);
            /*
            AuthScope scope = new AuthScope(null,-1,null);
            _httpClient.getState().setCredentials(scope,_cred);
            */
            _httpClient.getState().setCredentials(null, null, _cred);
        }
        else{
            _cred=null;
        }
        
        MultiThreadedHttpConnectionManager connmgr =
            new MultiThreadedHttpConnectionManager();
        /*
        // TODO: migrate to commons-httpclient-3.0
        HttpConnectionManagerParams params = new HttpConnectionManagerParams();
        params.setMaxTotalConnections(maxTotalConnections);
        params.setDefaultMaxConnectionsPerHost(maxConnectionsPerHost);
        params.setStaleCheckingEnabled(true);
        if (connTimeoutMillis > 0) {
            params.setConnectionTimeout(connTimeoutMillis);
        }
        if (socketTimeoutMillis > 0) {
            params.setSoTimeout(socketTimeoutMillis);
        }
        connmgr.setParams(params);
        */
        connmgr.setMaxTotalConnections(maxTotalConnections);     // deprecated
        connmgr.setMaxConnectionsPerHost(maxConnectionsPerHost); // deprecated
        connmgr.setConnectionStaleCheckingEnabled(true);         // deprecated
        _httpClient.setHttpConnectionManager(connmgr);           // keep
        if (connTimeoutMillis > 0) {
            _httpClient.setConnectionTimeout(connTimeoutMillis); // deprecated
        }
        if (socketTimeoutMillis > 0) {
            _httpClient.setTimeout(socketTimeoutMillis);         // deprecated
        }
        _httpClient.getHostConfiguration().setHost(_url.getHost(),_url.getPort(), _url.getProtocol());
    }
        
    public void close() {
        _httpClient = null;
        _cred =  null;
        _url = null;
    }
        
    protected JSONSerializable send(String method,JSONSerializable req,Class result) throws BrowseException {
        return send(method,req, result, 0);
    }
    
    private JSONSerializable send(String method,JSONSerializable req,Class result,int socketTimeoutMillis) throws BrowseException {
        StringBuffer urlPath=new StringBuffer(_url.getPath());
                                
        HttpMethod httpMethod=null;
        try {
            JSONObject reqObj=JSONSerializer.serializeJSONObject(req);
            String reqStr = reqObj.toString();

            if (reqStr.length() > 1024) {
                // too big for get; POST instead
                urlPath.append('/').append(method).append("/?proto=json");
                httpMethod = new PostMethod(urlPath.toString());
                NameValuePair[] body = new NameValuePair[1];
                body[0] = new NameValuePair("req", reqStr.toString());
                ((PostMethod)httpMethod).setRequestBody(body);
            } else {
                String urlencodedReqStr=URLEncoder.encode(reqStr, "UTF-8");
                urlPath.append('/').append(method).append("/?proto=json&req=").append(urlencodedReqStr);
                httpMethod=new GetMethod(urlPath.toString());
            }
            
            /*
            // TODO: migrate to commons-httpclient-3.0
            if (socketTimeoutMillis > 0) {
                HttpMethodParams methodParams = new HttpMethodParams(_httpClient.getParams());
                methodParams.setSoTimeout(socketTimeoutMillis);             
                httpMethod.setParams(methodParams);
            }
            */
            
            if (_cred!=null){
                httpMethod.setDoAuthentication(true);
            }
            
            _httpClient.executeMethod(httpMethod);
            
            if (200 != httpMethod.getStatusCode()) {
                String statusText;
                throw new BrowseException("http status code: "+httpMethod.getStatusCode()+
                        ((statusText = httpMethod.getStatusText()) != null && statusText.length() > 0 ? 
                                ", status text: "+URLDecoder.decode(statusText, "UTF-8") : ""));
            }
            
            if (BinaryData.class.isAssignableFrom(result)) {
                Header[] headers = httpMethod.getResponseHeaders();
                String contentType = null;
                String contentLength = null;
                for (Header header : headers) {
                    if (header.getName().equalsIgnoreCase("Content-Type")) {
                        contentType = header.getValue();
                    } else if (header.getName().equalsIgnoreCase("Content-Length")) {
                        contentLength = header.getValue();
                    }
                }
                BinaryData data = BinaryDataHandler.readFully(contentType, contentLength, httpMethod.getResponseBodyAsStream());
                return data;
            } else {
                byte[] data=httpMethod.getResponseBody();
                String resp=new String(data,"UTF-8");
    
                JSONObject jsonObj=new JSONObject(resp);
            
                JSONSerializable res=(JSONSerializable)JSONSerializer.deSerialize(result, jsonObj);
                return res;
            }
        }
        catch (IOException ioe) {
            throw new BrowseException(new StringBuffer("Failed to contact servlet: ").append(ioe.getMessage()).toString().toString(), ioe);
        } catch (JSONSerializationException e) {
            throw new BrowseException(e.getMessage(),e);
        } catch (JSONException e) {
            throw new BrowseException(e.getMessage(),e);
        }
        finally{
            if (httpMethod!=null){
                httpMethod.releaseConnection();
            }
        }   
    }

    protected void finalize() {
        try {
            close();
        } catch (Exception e) {
            logger.error("failed to close: "+e, e);
        }
    }
}

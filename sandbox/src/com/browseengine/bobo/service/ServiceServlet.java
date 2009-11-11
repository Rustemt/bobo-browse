package com.browseengine.bobo.service;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.HashMap;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.browseengine.bobo.server.protocol.JOSSHandler;
import com.browseengine.bobo.server.protocol.JSONHandler;
import com.browseengine.bobo.server.protocol.ProtocolHandler;
import com.browseengine.bobo.server.protocol.BinaryDataHandler;
import com.browseengine.bobo.service.data.BinaryData;

public abstract class ServiceServlet extends HttpServlet {

	/**
     * default serialVersionUID
     */
    private static final long serialVersionUID = 1L;
    
    private static Log logger=LogFactory.getLog(ServiceServlet.class);
	private Object _service;
	
	private HashMap<String,Method> _methodRegistry;
	
	static{
		ProtocolHandler.registerProtocolHandler(new JOSSHandler());
		ProtocolHandler.registerProtocolHandler(new JSONHandler());
	}
	
	protected ServiceServlet() {
		super();
	}
	
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		_service=getServiceInstance(config);
		if (_service!=null){
			_methodRegistry=new HashMap<String,Method>();
			Method[] methods=getSupportedMethods();
			for (int i=0;i<methods.length;++i){
				_methodRegistry.put(methods[i].getName(),methods[i]);
			}
			logger.debug("Servlet initialized.");
		}		
		else{
			logger.debug("Service currently not available.");
		}
	}
	
	/**
	 * returns true iff this method was handled as a supported protocol.
	 * 
	 * @param method
	 * @param req
	 * @param res
	 * @return
	 * @throws ServletException
	 * @throws IOException
	 */
	protected void handleRequest(String protocol,Method method, HttpServletRequest req, HttpServletResponse res) 
	throws ServletException, IOException {
		try {
			if (protocol != null){
				ProtocolHandler protoHandler=ProtocolHandler.getProtocolHandler(protocol);
				
				if (protoHandler==null) throw new ServletException("Unsupported protocol: "+protocol);
				
				Class<?>[] classes=method.getParameterTypes();
				
				Object request=null;
				
				if (classes!=null && classes.length>0){
					request=protoHandler.deserializeRequest(classes[0],req);
				}
				Object result=null;
				try{
					result=method.invoke(_service, request==null ? null : new Object[]{request});
				}
				catch(Exception e){
					Throwable cause=e.getCause();
					logger.error("handleRequest exception: "+e, e);
					if (cause==null) cause=e;
					// message doesn't give you the class name, which particularly with 
					// indirection through reflection helps us a great deal
					res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, cause.toString());
					return;
				}
				
				byte[] resBytes;
				if (result instanceof BinaryData) {
					BinaryData bd = null;
					try {
						bd = (BinaryData)result;
						if (bd.isNotFound()) {
							// send a 404
							res.sendError(HttpServletResponse.SC_NOT_FOUND, "data not found");
							return;
						}
						res.setContentType(bd.getContentType());
						if (bd.getContentLength() >= 0) {
							res.setContentLength(bd.getContentLength());
						}
						// write it out
						BinaryDataHandler.writeFully(bd,res.getOutputStream());
					} finally {
						try {
							if (bd != null) {
								bd.close();
							}
						} finally {
							bd = null;
						}
					}
				} else {
					resBytes=protoHandler.serializeResult(result);
				
					res.setCharacterEncoding("UTF-8");
					OutputStream out=res.getOutputStream();
					out.write(resBytes);
					out.flush();
				}
			} else {
				throw new ServletException("servlet method did nothing, since no \"proto\" request parameter was specified");
			}
		} catch (Exception e) {
			logger.error("handleRequest exception: "+e, e);
			throw new ServletException(e.getMessage(),e);
		}
	}
	
	public final void doGet(HttpServletRequest req, HttpServletResponse resp)
	throws IOException, ServletException {
		service(req, resp);
	}

	public final void doPost(HttpServletRequest req, HttpServletResponse resp)
		throws IOException, ServletException {
		service(req, resp);
	}
	
	private String getMethod(String path) throws ServletException{
		try{
			if (null == path) {
				return "";
			}
			String[] parts=path.split("/");		
			if (parts.length>1){
				return parts[1];
			}
			else{
				return "";
			}
		}
		catch(Exception e){
			throw new ServletException(e.getMessage(),e);
		}
	}
	
	@Override
	protected void service(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
		if (_service!=null){
			String methodName=getMethod(req.getPathInfo());
			
			if (methodName==null || methodName.length()==0) throw new ServletException("No or invalid method name specified.");
			
			Method method=_methodRegistry.get(methodName);
			if (method!=null){
				String protocol=req.getParameter("proto");
				handleRequest(protocol,method,req,res);
			}
			else{
				throw new ServletException("method :"+methodName+" is not supported.");
			}
		}
		else{
			logger.warn("Service is currently not available");
		}
	}
	

	@Override
	public void destroy() {
		super.destroy();
		shutdownService(_service);
	}

	protected abstract Object getServiceInstance(ServletConfig config) throws ServletException;
	protected abstract void shutdownService(Object service); 
	protected abstract Method[] getSupportedMethods();
	
}

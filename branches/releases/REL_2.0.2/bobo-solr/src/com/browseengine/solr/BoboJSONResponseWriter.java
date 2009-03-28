package com.browseengine.solr;

import java.io.IOException;
import java.io.Writer;

import org.apache.solr.request.QueryResponseWriter;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.request.SolrQueryResponse;
import org.apache.solr.util.NamedList;

import com.browseengine.bobo.api.BrowseResult;
import com.browseengine.bobo.server.protocol.BrowseJSONSerializer;

public class BoboJSONResponseWriter implements QueryResponseWriter{
	public String getContentType(SolrQueryRequest request,
			SolrQueryResponse response) {
		return CONTENT_TYPE_XML_UTF8;
	}

	public void init(NamedList args) {
		// TODO Auto-generated method stub
	}

	public void write(Writer writer, SolrQueryRequest request,
			SolrQueryResponse response) throws IOException {
		NamedList vals=response.getValues();
		
		BrowseResult res=(BrowseResult)vals.get(BoboRequestHandler.BOBORESULT);
		if (res!=null){
			String val=BrowseJSONSerializer.serialize(res);
			writer.write(val);
		}
	}
}

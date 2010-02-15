package com.browseengine.bobo.demo.client;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import com.browseengine.bobo.gwt.svc.BoboFacetSpec;
import com.browseengine.bobo.gwt.svc.BoboRequest;
import com.browseengine.bobo.gwt.svc.BoboResult;
import com.browseengine.bobo.gwt.svc.BoboSearchServiceAsync;
import com.browseengine.bobo.gwt.svc.BoboSelection;
import com.browseengine.bobo.gwt.widgets.AbstractFacetView;
import com.browseengine.bobo.gwt.widgets.CheckBoxFacetView;
import com.browseengine.bobo.gwt.widgets.FacetSelectionListener;
import com.browseengine.bobo.gwt.widgets.FacetValue;
import com.browseengine.bobo.gwt.widgets.FacetValueSelectionEvent;
import com.browseengine.bobo.gwt.widgets.TagCloudFacetView;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

public class BoboDemoPanel extends Composite implements FacetSelectionListener{
    interface MyUiBinder extends UiBinder<Widget, BoboDemoPanel> {}
    private static MyUiBinder uiBinder = GWT.create(MyUiBinder.class);
    private final BoboSearchServiceAsync  _searchSvc;	
    private final BoboRequest _req;
    
    private final Map<String,AbstractFacetView> _viewMap;

    @UiField CheckBoxFacetView colorView;
    //@UiField TagCloudFacetView tagsView;
    @UiField CheckBoxFacetView categoryView;
    @UiField TextBox queryInput;
    @UiField SpanElement hitcountLabel;
    @UiField SpanElement searchtimeLabel;
    @UiField Button searchButton;
    
    public BoboDemoPanel(BoboSearchServiceAsync searchSvc) {
    	_searchSvc = searchSvc;
    	_req = new BoboRequest();
    	Map<String,BoboFacetSpec> facetSpecMap = new HashMap<String,BoboFacetSpec>();
    	
    	BoboFacetSpec colorSpec = new BoboFacetSpec();
    	colorSpec.setMax(10);
    	colorSpec.setExpandSelection(true);
    	colorSpec.setOrderByHits(true);
    	
    	BoboFacetSpec categorySpec = new BoboFacetSpec();
    	categorySpec.setMax(10);
    	categorySpec.setExpandSelection(true);
    	categorySpec.setOrderByHits(true);
    	
    	
    	initWidget(uiBinder.createAndBindUi(this));
    	
    	colorView.addFacetSelectionListener(this);
    	facetSpecMap.put(colorView.getName(), colorSpec);
    	
    	categoryView.addFacetSelectionListener(this);
    	facetSpecMap.put(categoryView.getName(), categorySpec);
    	
    	_req.setFacetSpecMap(facetSpecMap);
    	
    	_viewMap = new HashMap<String,AbstractFacetView>();
    	_viewMap.put(colorView.getName(), colorView);
    	_viewMap.put(categoryView.getName(), categoryView);
    	
       // tagsView.addFacetSelectionListener(this);
       // tagsView.updateSelections(list,null);
        executeSearch();
	}
    
    

    public void handleSelectedEvent(FacetValueSelectionEvent event) {
    	Map<String,BoboSelection> selMap = _req.getSelections();
    	
		if (selMap==null){
			selMap=new HashMap<String,BoboSelection>();
			_req.setSelections(selMap);
		}
		
		AbstractFacetView view = event.getSource();
    	String name = view.getName();
    	
		BoboSelection sel = selMap.get(name);
		if (sel==null){
			sel = new BoboSelection();
			selMap.put(name, sel);

		}
		sel.addValue(event.getFacetValue());	
		
		executeSearch();
	}
	
	public void handleUnSelectedEvent(FacetValueSelectionEvent event) {
		Map<String,BoboSelection> selMap = _req.getSelections();
    	AbstractFacetView view = event.getSource();
    	String name = view.getName();
		if (selMap!=null){
			BoboSelection sel = selMap.get(name);
			if (sel!=null){
				List<String> vals = sel.getValues();
				if (vals!=null && vals.size()>0){
				  vals.remove(event.getFacetValue());
				  executeSearch();
				}
			}
		}
		
	}
	  
	public void handleClearSelections(AbstractFacetView view){
		Map<String,BoboSelection> selMap = _req.getSelections();
		if (selMap!=null){
			selMap.remove(view.getName());
		}
		executeSearch();
	}
	
	void refreshViews(BoboResult res){
		int numDocs = res.getNumHits();
		int totalDocs = res.getTotalDocs();
		hitcountLabel.setInnerHTML(numDocs+" / "+totalDocs);
		searchtimeLabel.setInnerHTML(String.valueOf(res.getTime()));
		
		Map<String,List<FacetValue>> valMap = res.getFacetResults();
		Set<Entry<String,List<FacetValue>>> entrySet = valMap.entrySet();
		for (Entry<String,List<FacetValue>> entry : entrySet){
			String facetName = entry.getKey();
			AbstractFacetView view = _viewMap.get(facetName);
			if (view!=null){
			  view.updateSelections(entry.getValue());
			}
		}
	}
	
	private void executeSearch(){
		_searchSvc.search(_req, new AsyncCallback<BoboResult>() {
			
			public void onSuccess(BoboResult result) {
				if (result!=null){
					refreshViews(result);
				}
				else{
					Window.alert("null result!");
				}
			}
			
			public void onFailure(Throwable caught) {
				Window.alert("server error: "+caught.getMessage());
			}
		});
	}
}

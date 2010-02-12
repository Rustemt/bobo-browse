package com.browseengine.bobo.demo.client;

import java.util.Arrays;
import java.util.List;

import com.browseengine.bobo.gwt.svc.BoboSearchServiceAsync;
import com.browseengine.bobo.gwt.widgets.AbstractFacetView;
import com.browseengine.bobo.gwt.widgets.CheckBoxFacetView;
import com.browseengine.bobo.gwt.widgets.FacetSelectionListener;
import com.browseengine.bobo.gwt.widgets.FacetValue;
import com.browseengine.bobo.gwt.widgets.FacetValueSelectionEvent;
import com.browseengine.bobo.gwt.widgets.TagCloudFacetView;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;

public class BoboDemoPanel extends Composite implements FacetSelectionListener{
  //  interface MyUiBinder extends UiBinder<Widget, BoboDemoPanel> {}
  //  private static MyUiBinder uiBinder = GWT.create(MyUiBinder.class);
    private final BoboSearchServiceAsync  _searchSvc;
	
    public BoboDemoPanel(BoboSearchServiceAsync searchSvc) {
    	_searchSvc = searchSvc;
    //	initWidget(uiBinder.createAndBindUi(this));
    	
    	CheckBoxFacetView checkbox = new CheckBoxFacetView("color");
        checkbox.addFacetSelectionListener(this);
        FacetValue[] facets = new FacetValue[]{new FacetValue("red",1234),new FacetValue("green",510)};
        //String[] facets = new String[]{"red","green"};
        List<FacetValue> list = Arrays.asList(facets);
        checkbox.updateSelections(list,null);
        Label label = new Label();
        label.setText("test");
        VerticalPanel panel = new VerticalPanel();
        panel.add(label);
        panel.add(checkbox);
        
        TagCloudFacetView TagCloudFacetView = new TagCloudFacetView("color");
        TagCloudFacetView.addFacetSelectionListener(this);
        TagCloudFacetView.updateSelections(list,null);
        panel.add(TagCloudFacetView);
        initWidget(panel);
	}
    
    

    public void handleSelectedEvent(FacetValueSelectionEvent event) {
		Window.alert(event.getSource().getName()+":"+event.getFacetValue()+" -> selected");
	}
	
	public void handleUnSelectedEvent(FacetValueSelectionEvent event) {
		Window.alert(event.getSource().getName()+":"+event.getFacetValue()+" -> unselected");
	}
	  
	public void handleClearSelections(AbstractFacetView view){
		Window.alert("selections cleared: "+view.getName());
	}
}

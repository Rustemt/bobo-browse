package com.browseengine.bobo.demo.client;

import java.util.Arrays;
import java.util.List;

import com.browseengine.bobo.gwt.widgets.AbstractFacetView;
import com.browseengine.bobo.gwt.widgets.CheckBoxFacetView;
import com.browseengine.bobo.gwt.widgets.FacetSelectionListener;
import com.browseengine.bobo.gwt.widgets.FacetValue;
import com.browseengine.bobo.gwt.widgets.FacetValueSelectionEvent;
import com.browseengine.bobo.gwt.widgets.TagCloudFacetView;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.VerticalPanel;

public class BoboDemo implements EntryPoint,FacetSelectionListener {
  /**
   * The message displayed to the user when the server cannot be reached or
   * returns an error.
   */
  private static final String SERVER_ERROR = "An error occurred while "
      + "attempting to contact the server. Please check your network "
      + "connection and try again.";

  //private final SearchServiceAsync searchService = GWT.create(SearchService.class);
  //private final JMXAdminServiceAsync jmxService = GWT.create(JMXAdminService.class);
  
  /**
   * This is the entry point method.
   */
  public void onModuleLoad() {
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
    RootPanel.get("toptab").add(panel);
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

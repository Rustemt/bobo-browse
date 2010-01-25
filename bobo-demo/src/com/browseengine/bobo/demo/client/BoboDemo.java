package com.browseengine.bobo.demo.client;

import java.util.LinkedList;
import java.util.List;

import com.browseengine.bobo.gwt.widgets.CheckBoxSelection;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.VerticalPanel;

public class BoboDemo implements EntryPoint {
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
    CheckBoxSelection checkbox = new CheckBoxSelection("color");
    
    //BrowseFacet[] facets = new BrowseFacet[]{new BrowseFacet("red",1234),new BrowseFacet("green",510)};
    //String[] facets = new String[]{"red","green"};
    List<String> list = new LinkedList<String>();
    list.add("red");
    list.add("green");
    checkbox.updateSelections(list);
    Label label = new Label();
    label.setText("test");
    VerticalPanel panel = new VerticalPanel();
    panel.add(label);
    panel.add(checkbox);
    RootPanel.get("toptab").add(panel);
  }
}

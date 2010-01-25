package com.browseengine.bobo.gwt.wigets;

import java.util.List;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Composite;

public class CheckBoxSelection extends Composite implements ClickHandler {

	public CheckBoxSelection() {
		// TODO Auto-generated constructor stub
	}
	
	public void updateSelections(List<String> selections){
		
	}

	public void onClick(ClickEvent event) {
		Window.alert(event.toString());
	}

}

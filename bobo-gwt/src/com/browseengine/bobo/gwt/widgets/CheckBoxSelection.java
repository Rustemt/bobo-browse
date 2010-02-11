package com.browseengine.bobo.gwt.widgets;

import java.util.List;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.VerticalPanel;

public class CheckBoxSelection extends Composite implements ClickHandler {
    private final VerticalPanel _panel;
    private final String _name;
	public CheckBoxSelection(String name) {
		_panel = new VerticalPanel();
		_name = name;
		initWidget(_panel);
	}
	
	public void updateSelections(List<String> selections){
		_panel.clear();
		if (selections!=null){
			for (String facet : selections){
				CheckBox sel = new CheckBox();
				//String val = facet.getValue();
				String val = facet;
				sel.setFormValue(val);
				StringBuilder buf = new StringBuilder();
				buf.append(val).append(" (").append(100).append(")");
				sel.setText(buf.toString());
				sel.addClickHandler(this);
				_panel.add(sel);
			}
		}
	}

	public void onClick(ClickEvent event) {
		Object src = event.getSource();
		if (src instanceof CheckBox){
			CheckBox sel = (CheckBox)src;
			Window.alert(sel.getFormValue());
		}
	}
}

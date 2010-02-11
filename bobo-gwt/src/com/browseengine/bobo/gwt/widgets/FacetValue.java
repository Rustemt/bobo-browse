package com.browseengine.bobo.gwt.widgets;

import com.google.gwt.user.client.ui.Label;

public class FacetValue {
	private final String _value;
	private final int _count;
	
	public FacetValue(String value,int count){
		_value = value;
		_count = count;
	}
	
	public String getValue(){
		return _value;
	}
	
	public int getHitcount(){
		return _count;
	}
	
	@Override
	public String toString(){
		StringBuilder buf = new StringBuilder();
		buf.append(_value).append(" (").append(_count).append(")");
		return buf.toString();
	}
	
	public Label buildLabel(){
		Label label = new Label();
		label.setText(toString());
		return label;
	}
}

package com.browseengine.bobo.gwt.widgets;

import com.google.gwt.user.client.rpc.IsSerializable;
import com.google.gwt.user.client.ui.Label;

public class FacetValue implements IsSerializable{
	private String _value;
	private int _count;
	
	

	public void setValue(String value) {
		_value = value;
	}

	public void setCount(int count) {
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

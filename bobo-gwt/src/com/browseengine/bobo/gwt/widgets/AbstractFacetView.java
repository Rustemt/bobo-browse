package com.browseengine.bobo.gwt.widgets;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.google.gwt.user.client.ui.Composite;

public abstract class AbstractFacetView extends Composite{
	protected final String _name;
	private final List<FacetSelectionListener> _lsnrs;
	
	public static final String FACET_HEADING_STYLE="gwt-bobo-facet-heading";
	protected AbstractFacetView(String name){
		_name = name;
		_lsnrs = new LinkedList<FacetSelectionListener>();
	}
	
	public String getName(){
		return _name;
	}
	
	public void addFacetSelectionListener(FacetSelectionListener lsnr){
		synchronized(_lsnrs){
		  _lsnrs.add(lsnr);
		}
	}
	
	public void fireFacetSelectionEvent(String value,boolean selected){
		FacetValueSelectionEvent event = new FacetValueSelectionEvent(this, value);
		for (FacetSelectionListener lsnr : _lsnrs){
			if (selected){
			  lsnr.handleSelectedEvent(event);
			}
			else{
			  lsnr.handleUnSelectedEvent(event);
			}
		}
	}
	
	public void fireFacetSelectionClearedEvent(){
		for (FacetSelectionListener lsnr : _lsnrs){
			lsnr.handleClearSelections(this);
		}
	}
	
	abstract public void updateSelections(List<FacetValue> selections,Set<String> selected);
}

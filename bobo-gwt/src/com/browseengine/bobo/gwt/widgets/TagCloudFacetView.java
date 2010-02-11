package com.browseengine.bobo.gwt.widgets;

import java.util.List;
import java.util.Set;

import com.google.gwt.visualization.client.DataTable;
import com.google.gwt.visualization.client.AbstractDataTable.ColumnType;

public class TagCloudFacetView extends AbstractFacetView {

	public TagCloudFacetView(String name){
		super(name);
	}
	
	@Override
	public void updateSelections(List<FacetValue> selections,Set<String> selected) {
		DataTable dataTable = DataTable.create();
		
		dataTable.addColumn(ColumnType.STRING, "Label");
		dataTable.addColumn(ColumnType.NUMBER, "Value");

		dataTable.addRows(selections.size());
		int i=0;
		for (FacetValue facet : selections){
			dataTable.setValue(i, 0, facet.getValue());
			dataTable.setValue(i, 1, facet.getHitcount());
			i++;
		}
	}

}

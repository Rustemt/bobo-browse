package com.browseengine.bobo.gwt.widgets;

import java.util.List;
import java.util.Map;

import com.browseengine.bobo.gwt.svc.BoboHit;
import com.google.gwt.uibinder.client.UiConstructor;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.visualization.client.DataTable;
import com.google.gwt.visualization.client.VisualizationUtils;
import com.google.gwt.visualization.client.AbstractDataTable.ColumnType;
import com.google.gwt.visualization.client.events.PageHandler;
import com.google.gwt.visualization.client.events.SortHandler;
import com.google.gwt.visualization.client.visualizations.Table;
import com.google.gwt.visualization.client.visualizations.Table.Options;
import com.google.gwt.visualization.client.visualizations.Table.Options.Policy;

public class ResultTableView extends Composite{
	private Table _table;
	private final String[] _fieldNames;
	private int _numRows = 10;
	private VerticalPanel _panel;
	public ResultTableView(String... fieldNames){
		_fieldNames = fieldNames;
		_panel = new VerticalPanel();
		Runnable onLoadCallback = new Runnable() {
		      public void run() {
		    	  _table = new Table();
		    	  _panel.add(_table);
		    	  _table.addSortHandler(new ResultSortHandler());
		  		  _table.addPageHandler(new ResultPageHandler());
		      }
		 };
		initWidget(_panel);
		VisualizationUtils.loadVisualizationApi(onLoadCallback, Table.PACKAGE);
	}
	
	public @UiConstructor ResultTableView(String fieldNames) {
		  this(fieldNames.split("[, ]+"));
	} 
	
	private static String buildString(String[] values){
		if (values!=null){
			if (values.length == 0) return "";
			if (values.length == 1) return values[0];
			StringBuilder buf = new StringBuilder();
			for (int i=0;i<values.length;++i){
				if (i!=0){
					buf.append(",");
				}
				buf.append(values[i]);
			}
			return buf.toString();
		}
		else{
			return null;
		}
	}
	
	private static String[] parseString(String value){
		if (value == null) return null;
		if (value.length() == 0) return new String[0];
		return value.split(",");
	}
	
	public void setPageSize(int pageSize){
		_numRows = pageSize;
	}
	
	public void updateResults(List<BoboHit> hitList){
		if (hitList==null){
			Window.alert("null hit list");
		}
		DataTable dataTable = DataTable.create();
		for (String fieldName : _fieldNames){
			dataTable.addColumn(ColumnType.STRING, fieldName);
		}
		dataTable.addColumn(ColumnType.NUMBER,"score");
		dataTable.addRows(hitList.size());
		
		int row = 0;
		for (BoboHit hit : hitList){
			Map<String,String[]> fields = hit.getFields();
			float score = hit.getScore();
			int index = 0;
			for (String fieldName:_fieldNames){
			  String val = buildString(fields.get(fieldName));
			  dataTable.setCell(row, index, val, val, null);
			  index++;
			}
			dataTable.setCell(row,index, score,String.valueOf(score),null);
			row++;
		}
		Options options = Options.create();
		options.setShowRowNumber(true);
		options.setOption("alternatingRowStyle", true);
		options.setPage(Policy.EVENT);
		options.setSort(Policy.EVENT);
		options.setPageSize(_numRows);
		options.setOption("width","100%");
		_table.draw(dataTable,options);
	}
	
	private class ResultSortHandler extends SortHandler{

		@Override
		public void onSort(SortEvent event) {
			boolean reverse = !event.isAscending();
			int col = event.getColumn();
			Window.alert("sort clicked");
		}
	}
	
	private class ResultPageHandler extends PageHandler{

		@Override
		public void onPage(PageEvent event) {
			Window.alert("paging clicked");
		}
		
	}
}

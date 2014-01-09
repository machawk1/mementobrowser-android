package dev.memento;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.TreeSet;

import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;

public class LocationAutoCompleteAdapter extends ArrayAdapter<String> implements Filterable {
	
	private TreeSet<String> mResultListSet;
    private ArrayList<String> mResultList;
    
    public LocationAutoCompleteAdapter(Context context, int textViewResourceId) {
        super(context, textViewResourceId);
        
        mResultListSet = new TreeSet<String>();
    }
    
    public LocationAutoCompleteAdapter(Context context, int textViewResourceId, String[] defaultSites) {
    	super(context, textViewResourceId, defaultSites);
    	
    	mResultListSet = new TreeSet<String>();
    	mResultListSet.addAll(Arrays.asList(defaultSites));
    	
    	mResultList = new ArrayList<String>();
    	mResultList.addAll(Arrays.asList(defaultSites));
    }
    
    private String shortenUrl(String url) {
    	// Remove "http://"
    	url = url.replaceFirst("https?://", "");
    	
    	// If only a single / then remove it so it looks nicer
    	if (url.indexOf("/") == url.length() - 1)
    		url = url.substring(0, url.length() - 1);
    	
    	return url;
    }
    
    @Override
    public void add(String url) {
    	
    	url = shortenUrl(url);    	    	
    	mResultListSet.add(url);
    	
    	// Rebuild list
    	mResultList.clear();
    	mResultList.addAll(mResultListSet);
    }
          
	@Override
	public void remove(String url) {
		url = shortenUrl(url); 
		if (mResultListSet.remove(url)) {		
			// Rebuild list
	    	mResultList.clear();
	    	mResultList.addAll(mResultListSet);
		}
	}

	@Override
    public int getCount() {
        return mResultList.size();
    }

    @Override
    public String getItem(int index) {
    	return mResultList.get(index);
    }

    @Override
    public Filter getFilter() {
        Filter filter = new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults filterResults = new FilterResults();
                if (constraint != null) {
                    // Remove beginning http://
                	constraint = constraint.toString().replaceFirst("^https?://", "");
                	
                	// Build a new list matching only the items that match the typed text
                	mResultList.clear();
                	
                	for (String url : mResultListSet) {
                		if (url.contains(constraint))
                			mResultList.add(url);
                	}                    	
                }
                
                filterResults.values = mResultList;
                filterResults.count = mResultList.size();     
                                    
                return filterResults;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                if (results != null && results.count > 0) {
                    notifyDataSetChanged();
                }
                else {
                    notifyDataSetInvalidated();
                }
            }};
        return filter;
    }

}

package dev.memento;

import java.util.ArrayList;
import java.util.SortedSet;
import java.util.TreeSet;

import android.util.Log;


public class MementoList extends ArrayList<Memento> {

	private static final long serialVersionUID = 1L;

	public static final String LOG_TAG = "MementoBrowser_tag";
	
	private SortedSet <String> mYearList;
	
	// The currently selected memento
	private int mCurrent = -1;
	
	public MementoList() {
		mYearList = new TreeSet<String>();		
	}
	
	@Override
	public void add(int index, Memento memento) {
		String year = Integer.toString(memento.getDateTime().getYear());
		mYearList.add(year);
		super.add(index, memento);
	}

	@Override
	public boolean add(Memento memento) {

		String year = Integer.toString(memento.getDateTime().getYear());
		mYearList.add(year);
		return super.add(memento);
	}

	@Override
	public void clear() {
		mYearList.clear();
		super.clear();
	}	
	
	public Memento getCurrent() {
		if (mCurrent >= 0 && mCurrent < size())
			return get(mCurrent);
		else
			return null;
	}
	
	
	/**
	 * Return the index of the Mememento with the given date, -1 if it
	 * could not be found.
	 * @param date
	 * @return
	 */
	public int getIndex(SimpleDateTime date) {
    	for (int i = 0; i < size(); i++) {
    		if (get(i).getDateTime().equals(date))
    			return i;
    	}
    	
    	return -1;
	}
	
	public int getIndexByDate(SimpleDateTime date) {
    	for (int i = 0; i < size(); i++) {
    		if (get(i).getDateTime().equalsDate(date))
    			return i;
    	}
    	
    	return -1;
	}
	
	public int getIndex(String datetime) {
		// Find the index of this datetime by searching through all the mementos 
		for (int i = 0; i < size(); i++) {
    		if (get(i).getDateTime().dateAndTimeFormatted().equals(datetime)) {
    			return i;
    		}
    	}
		return -1;
	}
	
	public void setCurrentIndex(int index) {
		mCurrent = index;
	}
	
	public int getCurrentIndex() {
		return mCurrent;
	}
	
	public Memento getFirst() {
		return get(0);
	}
	
	public Memento getLast() {		
		return get(size() - 1);
	}
		
	/**
	 * Return the next memento and update internal pointer.
	 * Return null if we're at the end of the list or there
	 * are no mementos.
	 */
	public Memento getNext() {
		if (mCurrent >= 0 && mCurrent < this.size() - 1) {
			mCurrent++;
			return get(mCurrent);
		}
		else
			return null;
	}
	
	public Memento getPrevious() {
		if (mCurrent > 0 && mCurrent < this.size()) {
			mCurrent--;
			return get(mCurrent);
		}
		else {
			Log.d(LOG_TAG, "getPrevious: Unable to find previous. mCurrent=" + mCurrent);
			return null;
		}			
	}
	
	@SuppressWarnings("unchecked")
	public CharSequence[] getAllDates() {
		
		// Must use clone in case the list is still being built.  If using the original,
		// a ConcurrentModificationException to be thrown.
		ArrayList<Memento> copy = (ArrayList<Memento>) this.clone();
		
		CharSequence[] dates = new CharSequence[size()];
		int i = 0;
    	for (Memento m : copy) { 
    		dates[i] = m.getDateTime().dateAndTimeFormatted();
    		i++;
    	}
    	
    	return dates;
	}
	
	public CharSequence[] getAllYears() {
						
		CharSequence[] years = new CharSequence[mYearList.size()];
		mYearList.toArray(years);		    	
    	return years;
	}
	
	@SuppressWarnings("unchecked")
	public CharSequence[] getDatesForYear(int year) {
				
		// NOTE: This is certainly not very efficient, but it works for the time being!
		
		// Must use clone in case the list is still being built.  If using the original,
		// a ConcurrentModificationException to be thrown.
		ArrayList<Memento> copy = (ArrayList<Memento>) this.clone();
		
		ArrayList<Memento> list = new ArrayList<Memento>();
					
    	for (Memento m : copy) { 
    		if (m.getDateTime().getYear() == year) 
    			list.add(m);    			
    	}
    	
    	CharSequence[] dates = new CharSequence[list.size()];
    	int i = 0;
    	for (Memento m : list) { 
    		dates[i] = m.getDateTime().dateAndTimeFormatted();
			i++;
    	}   	
    	
    	return dates;
	}

	public boolean isFirst(SimpleDateTime date) {
		if (size() > 0)
			return get(0).getDateTime().equals(date);
		else
			return false;				
	}
	
	public boolean isLast(SimpleDateTime date) {
		if (size() > 0)
			return get(size() - 1).getDateTime().equals(date);
		else
			return false;				
	}
	
	public void displayAll() {
		System.out.println("All mementos:");
    	int i = 1;
    	for (Memento m : this) {
    		System.out.println(i + ". " + m);
    		i++;
    	}
	}	
}

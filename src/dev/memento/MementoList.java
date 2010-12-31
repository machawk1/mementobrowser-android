/**
 * MementoList.java
 * 
 * Copyright 2010 Frank McCown
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  
 *  This keeps a list of all the Mementos which are known for a given URL.
 */

package dev.memento;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.SortedSet;
import java.util.TreeSet;

import android.util.Log;


public class MementoList extends ArrayList<Memento> implements Serializable {

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
	
	/**
	 * Set the current index to the given integer if the number is >= -1 
	 * (for the case of "un-setting" the index up to the size-1.
	 * @param index
	 */
	public void setCurrentIndex(int index) {
		if (index >= -1 && index < size())
			mCurrent = index;
	}
	
	/**
	 * Return the index of the current Memento in the list (0 to size-1).
	 * @return
	 */
	public int getCurrentIndex() {
		return mCurrent;
	}
	
	/**
	 * Returns the first Memento in the list or null if there are no Mementos.
	 * @return
	 */
	public Memento getFirst() {
		if (this.size() > 0)
			return get(0);
		else
			return null;
	}
	
	/**
	 * Returns the last Memento in the list or null if there are no Mementos.
	 * @return
	 */
	public Memento getLast() {	
		if (this.size() > 0)
			return get(size() - 1);
		else
			return null;
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

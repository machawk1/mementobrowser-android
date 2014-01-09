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
import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;


public class MementoList extends ArrayList<Memento> implements Serializable {

	private static final long serialVersionUID = 1L;

	public static final String LOG_TAG = "MementoBrowser_tag";
	
	// Unique, ordered list of dates found in the mementos
	//private SortedSet <String> mYearList;
	
	private TreeMap<Integer, TreeSet<Memento>> mYearList;
	
	// Should be used very infrequently. www.cnn.com uses it because there are
	// thousands of mementos per year in some cases.
	private TreeMap<Integer, ArrayList<ArrayList<Memento>>> mYearToMonthList;
	
	// The currently selected memento
	private int mCurrent = -1;
	
	public MementoList() {
		//mYearList = new TreeSet<String>();		
		mYearList = new TreeMap<Integer, TreeSet<Memento>>();
		mYearToMonthList = new TreeMap<Integer, ArrayList<ArrayList<Memento>>>();
	}
	
	@Override
	public void add(int index, Memento memento) {
		//String year = Integer.toString(memento.getDateTime().getYear());
		//mYearList.add(year);
		int year = memento.getDateTime().getYear();
		addMementoToYearList(year, memento);		
		super.add(index, memento);
	}

	@Override
	public boolean add(Memento memento) {

		//String year = Integer.toString(memento.getDateTime().getYear());
		//mYearList.add(year);
		int year = memento.getDateTime().getYear();
		addMementoToYearList(year, memento);	
		return super.add(memento);
	}

	private void addMementoToYearList(int year, Memento memento) {
		TreeSet<Memento> list = mYearList.get(year);
		if (list != null) {
			list.add(memento);
		}
		else {
			list = new TreeSet<Memento>();
			list.add(memento);
			mYearList.put(year, list);
		}
	}	
		
	@Override
	public void clear() {
		mYearList.clear();
		mYearToMonthList.clear();
		mCurrent = -1;
		super.clear();
	}	
	
	/**
	 * Returns the Memento which is currently displayed or null if there
	 * is no current Memento.
	 * @return
	 */
	public Memento getCurrent() {
		if (mCurrent >= 0 && mCurrent < size())
			return get(mCurrent);
		else
			return null;
	}
	
	
	/**
	 * Returns the memento that is closest to the supplied date.
	 * Returns null if there are no mementos.
	 * @return
	 */
	public Memento getClosestDate(SimpleDateTime date) {
		
		int i = 0;
		while (i < size() && get(i).getDateTime().compareTo(date) < 0) 
    		i++;
		
		if (size() == 0)
			return null;
		else if (i == size())
			return getLast();
		else if (i == 0 || get(i).getDateTime().compareTo(date) == 0)
			return get(i);
		else {
			// See if date is closer to i or i-1
			Date newerDate = get(i).getDateTime().getDate();
			Date olderDate = get(i-1).getDateTime().getDate();
						
			int diffInDays1 = (int)Math.round((newerDate.getTime() - date.getDate().getTime()) 
	                 / (1000 * 60 * 60 * 24));			
			
			int diffInDays2 = (int)Math.round((date.getDate().getTime() - olderDate.getTime()) 
	                 / (1000 * 60 * 60 * 24));			
			
			if (diffInDays1 < diffInDays2)
				return get(i);
			else
				return get(i-1);
		}			
	}
	
	
	/**
	 * Return the index of the Memento with the given datetime, -1 if it
	 * could not be found.
	 * @param date
	 * @return
	 */
	public int getIndex(SimpleDateTime datetime) {
    	for (int i = 0; i < size(); i++) {
    		if (get(i).getDateTime().equals(datetime))
    			return i;
    	}
    	
    	return -1;
	}
	
	/**
	 * Return the index of the Memento with the given date, -1 if it
	 * could not be found.
	 * @param date
	 * @return
	 */
	public int getIndexByDate(SimpleDateTime date) {
    	for (int i = 0; i < size(); i++) {   		
    		if (get(i).getDateTime().equalsDate(date))
    			return i;
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
	 * Return the index of the "current" Memento in the list (0 to size-1) or
	 * -1 if there is no current Memento.
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
	
	/**
	 * Get the next memento after the given date. Return null if
	 * we're at the end of the list or there are no mementos.
	 * @param date
	 * @return
	 */
	public Memento getNext(SimpleDateTime date) {
		
		// Search through the mementos
		int i = 0;
		while (i < size() && get(i).getDateTime().compareTo(date) < 0) 
    		i++;
				
		if (i == size())
			return null;    // At the end of the list!
		else if (get(i).getDateTime().compareTo(date) == 0) {
			// Found the exact one
			i++;
			if (i == size())
				return null;   // At the end of the list!
			else
				return get(i);
		}
		else {
			// Memento with date not found so return this memento which
			// has a newer date
			return get(i);
		}
	}
	
	public Memento getPrevious() {
		if (mCurrent > 0 && mCurrent < this.size()) {
			mCurrent--;
			return get(mCurrent);
		}
		else {
			if (Log.LOG) Log.d(LOG_TAG, "getPrevious: Unable to find previous. mCurrent=" + mCurrent);
			return null;
		}			
	}
	
	/**
	 * Get the previous memento before the given date. Return null if
	 * we're at the end of the list or there are no mementos.
	 * @param date
	 * @return
	 */
	public Memento getPrevious(SimpleDateTime date) {
		
		// Search through the mementos
		int i = 0;
		while (i < size() && get(i).getDateTime().compareTo(date) < 0) 
    		i++;
				
		if (i == 0)
			return null;    // At the beginning of the list
		else if (i < size() && get(i).getDateTime().compareTo(date) == 0) {
			i--;
			if (i < 0)
				return null;   // At the beginning of the list!
			else
				return get(i);
		}
		else {
			// Memento with date not found so return the previous memento which
			// has an older date
			return get(i - 1);
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
	
	/**
	 * Return the years and number of dates available for each year for all the 
	 * mementos. 
	 * @return
	 */
	public TreeMap<Integer,Integer> getAllYears() {
						
		TreeMap<Integer,Integer> years = new TreeMap<Integer, Integer>();
		
		int totalMementos = 0;
		for (Map.Entry<Integer,TreeSet<Memento>> entry : mYearList.entrySet()) {
			int year = entry.getKey();
			TreeSet<Memento> mementos = entry.getValue();
			years.put(year, mementos.size());	
			totalMementos += mementos.size();
		}
		
		if (Log.LOG) Log.d(LOG_TAG, "totalMementos = " + totalMementos + "  size = " + this.size());
    	return years;
	}
	
	/**
	 * Returns the available months for the mementos given the year. 
	 * @param year
	 * @return
	 */
	public LinkedHashMap<CharSequence,Integer> getMonthsForYear(int year) {
			
		LinkedHashMap<CharSequence,Integer> months = new LinkedHashMap<CharSequence,Integer>();
		
		if (mYearToMonthList.size() == 0 || !mYearToMonthList.containsKey(year)) {
			// Need to build list from scratch		
						
			TreeSet<Memento> mementos =  mYearList.get(year);
			if (mementos == null) {
				return months;
			}
			
			// Array of all months and their mementos for this year
			ArrayList<ArrayList<Memento>> monthList = mYearToMonthList.get(year);
			if (monthList == null) {
				monthList = new ArrayList<ArrayList<Memento>>();
				
				// List contains no mementos for 12 months
				for (int mon = 0; mon < 12; mon++) {
					monthList.add(null);
				}
				
				mYearToMonthList.put(year, monthList);
			}
			
			for (Memento m : mementos) {
								
				//String month = m.getDateTime().getMonthName();
				int month = m.getDateTime().getMonth();
								
				// List of all mementos for this year and month
				ArrayList<Memento> mementoList = monthList.get(month - 1);
				if (mementoList == null) {
					mementoList = new ArrayList<Memento>();
					monthList.add(month - 1, mementoList);	
				}
				
				mementoList.add(m);									
			}
		}

		
		// Return month counts, but ignore months with no dates
		DateFormatSymbols df = new DateFormatSymbols();
		months = new LinkedHashMap<CharSequence,Integer>();
		ArrayList<ArrayList<Memento>> monthList = mYearToMonthList.get(year);
		for (int mon = 0; mon < 12; mon++) {
			//System.out.println("month = " + mon);
			if (monthList.get(mon) != null) {
				String monthName = df.getMonths()[mon];
				months.put(monthName, monthList.get(mon).size());	
				//System.out.println("  size = " + monthList.get(mon).size());
			}
		}
	    	
		return months;
	}
	
	public CharSequence[] getDatesForYear(int year) {
				    	
		TreeSet<Memento> mementos = mYearList.get(year);
		if (mementos == null) {
			if (Log.LOG) Log.e(LOG_TAG, "getDatesForYear: No mementos could be found for the selected year " + year);
			return new CharSequence[0];
		}
		
		CharSequence[] dates = new CharSequence[mementos.size()];
		int i = 0;
		for (Memento m : mementos) {
			dates[i] = m.getDateAndTimeFormatted();
			i++;
		}
		
		//return mementos.toArray(new CharSequence[0]);
		return dates;    	
	}
	
	public Memento[] getByYear(int year) {
		
		TreeSet<Memento> mementos = mYearList.get(year);
		if (mementos == null) {
			return new Memento[0];
		}
				
		return mementos.toArray(new Memento[0]);
	}
	
	public Memento[] getByMonthAndYear(int month, int year) {
				
		if (month < 1 || month > 12)
			return null;
		
		ArrayList<ArrayList<Memento>> monthList = mYearToMonthList.get(year);
		
		if (monthList == null) {
		
			// This could happen if mYearToMonthList wasn't populated yet
			getMonthsForYear(year);
			
			monthList = mYearToMonthList.get(year);
			if (monthList == null) {
				// Now we definitely can't find this year
				return new Memento[0];
			}
		}		
		
		ArrayList<Memento> mlist = monthList.get(month - 1);
		if (mlist == null)
			return new Memento[0];
		
		return mlist.toArray(new Memento[0]);
	}
	
	public CharSequence[] getDatesForMonthAndYear(int month, int year) {
		
		if (month < 1 || month > 12)
			return null;
		
		ArrayList<ArrayList<Memento>> monthList = mYearToMonthList.get(year);
		
		if (monthList == null) {
		
			// This could happen if mYearToMonthList wasn't populated yet
			getMonthsForYear(year);
			
			monthList = mYearToMonthList.get(year);
			if (monthList == null) {
				// Now we definitely can't find this year
				return new CharSequence[0];
			}
		}		
		
		ArrayList<Memento> mlist = monthList.get(month - 1);
		if (mlist == null)
			return new CharSequence[0];
		
		CharSequence[] dates = new CharSequence[mlist.size()];
		int i = 0;
    	for (Memento m : mlist) { 
    		dates[i] = m.getDateAndTimeFormatted();
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

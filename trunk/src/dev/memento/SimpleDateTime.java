package dev.memento;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.SimpleTimeZone;
import java.util.TimeZone;

public class SimpleDateTime implements Comparable<SimpleDateTime> {
	
	// Sun, 06 Nov 1994 08:49:37 GMT
	public final static String PATTERN_RFC1123 = "EEE, dd MMM yyyy HH:mm:ss zzz";
	
	public final static String PATTERN_AMERICAN_SHORT = "MM-dd-yyyy";
	
	public final TimeZone GMT = TimeZone.getTimeZone("GMT");
	
	private int mDay;
	private int mMonth;
	private int mYear;
	private Date mDate;
	
	// There should be only one format that all use
	public static DateFormat mDateFormat = null;
	public static DateFormat mTimeFormat = null;
	
	public SimpleDateTime() {
		mDate = new Date();
		
		Calendar c = Calendar.getInstance();
		mYear = c.get(Calendar.YEAR);
		mMonth = c.get(Calendar.MONTH) + 1;
		mDay = c.get(Calendar.DAY_OF_MONTH);
	}
	
	/**
	 * Set to the current date/time.
	 */
	public SimpleDateTime(DateFormat dateFormat, DateFormat timeFormat) {    
		this();
		
		mDateFormat = dateFormat;
		mTimeFormat = timeFormat;
	}
	
	public SimpleDateTime(SimpleDateTime date) {
		mDate = (Date) date.mDate.clone();
		mDay = date.mDay;
		mMonth = date.mMonth;
		mYear = date.mYear;
	}
	
	/**
	 * Create a new SimpleDateTime based on the given date in RFC 1123 format. 
	 * Example: Sat, 22 Dec 2007 09:05:17 GMT
	 * @param date
	 */
	public SimpleDateTime(String date) {
		setDateRfc1123(date);		
	}
	
	//public SimpleDateTime(String date) {
	//	setDateRfc1123(date);
	//}
	
	public SimpleDateTime(int day, int month, int year) {		
		setDate(day, month, year);		
	}
	
	public void setDateFormat(DateFormat dateFormat) {
		mDateFormat = dateFormat;
	}
	
	public void setTimeFormat(DateFormat timeFormat) {
		mTimeFormat = timeFormat;
	}
	
	private void setDate(int day, int month, int year) {
		SimpleDateFormat sdf = new SimpleDateFormat(PATTERN_AMERICAN_SHORT); 
		try {
			mDate = sdf.parse(month + "-" + day + "-" + year);
		} catch (ParseException e) {
			e.printStackTrace();
		}

		mDay = day;
		mMonth = month;
		mYear = year;
	}
	
	public static SimpleDateTime clone(SimpleDateTime date) {
		return new SimpleDateTime(date);		 
	}
	
	/*
	 * date is in mm-dd-yyyy format (e.g., 12-31-2001).
	 */
	public static SimpleDateTime parseShortDate(String date) {
		
		SimpleDateFormat formatter = new SimpleDateFormat(PATTERN_AMERICAN_SHORT, Locale.US);
        //formatter.setTimeZone(GMT);            
        try {
        	//Log.d(LOG_TAG, "date is [" + date + "]");
        	Date d = (Date)formatter.parse(date);				
			
			Calendar cal = Calendar.getInstance();
			cal.setTime(d);
			int day = cal.get(Calendar.DATE);
			int month = cal.get(Calendar.MONTH) + 1;
			int year = cal.get(Calendar.YEAR);
			
			return new SimpleDateTime(day, month, year);
			
		} catch (ParseException e) {
			e.printStackTrace();
		}     
		
		return null;		
	}
	
	public int getDay() {
		return mDay;
	}

	public void setDay(int day) {
		setDate(day, mMonth, mYear);
	}

	public int getMonth() {
		return mMonth;
	}

	public void setMonth(int month) {
		setDate(mDay, month, mYear);
	}

	public int getYear() {
		return mYear;
	}

	public void setYear(int year) {
		setDate(mDay, mMonth, year);
	}
	
	public Date getDate() {
		return mDate;
	}
		
	
	// Incoming date is in RFC 1123 format. 
	// Example: Sat, 22 Dec 2007 09:05:17 GMT
	public void setDateRfc1123(String date) {
		SimpleDateFormat formatter = new SimpleDateFormat(PATTERN_RFC1123, Locale.US);
        //formatter.setTimeZone(GMT);            
        try {
        	//Log.d(LOG_TAG, "date is [" + date + "]");
        	mDate = formatter.parse(date);				
			
			Calendar cal = Calendar.getInstance();
			cal.setTime(mDate);
			mDay = cal.get(Calendar.DATE);
			mMonth = cal.get(Calendar.MONTH) + 1;
			mYear = cal.get(Calendar.YEAR);
			
			//Log.d(LOG_TAG, "date is " + toString());
			
		} catch (ParseException e) {
			e.printStackTrace();
		}            
	}
	
	/**
	 * Return the date in dd-mm-yyyy format (e.g., 12-31-2001).
	 * @return
	 */
	public CharSequence dateFormatted() {    	
    	//return new StringBuilder()
        //       .append(mMonth).append("-").append(mDay).append("-").append(mYear);
		
		//if (mDateFormat == null)
		//	mDateFormat = DateFormat.getDateInstance();
		
		return mDateFormat.format(mDate);
    } 
	
	public CharSequence dateAndTimeFormatted() {
		
		//if (mTimeFormat == null)
		//	mTimeFormat = DateFormat.getDateInstance();
		
		return dateFormatted() + " " + mTimeFormat.format(mDate);
	}
	
	public String longDateFormatted() {
		 
		SimpleDateFormat formatter = new SimpleDateFormat(PATTERN_RFC1123);
		
		java.util.Calendar cal = Calendar.
		getInstance(new SimpleTimeZone(0, "GMT"));
		formatter.setCalendar(cal);
		       //java.util.Date date = format.parse("2003-01-25 00:15:30");
		        
		String date = formatter.format(mDate);
		
		// Remove ugly ending
		if (date.endsWith("+00:00"))
			date = date.replace("+00:00", "");
		
		return date;
		
		/*
		Date date = null;
		SimpleDateFormat formatter = new SimpleDateFormat("MM-dd-yyyy");
		try {
			date = (Date)formatter.parse(dateFormatted().toString());
			formatter = new SimpleDateFormat(PATTERN_RFC1123, Locale.US);
            formatter.setTimeZone(GMT);
		} catch (ParseException e) {
			e.printStackTrace();
		}			
		    		
        return formatter.format(date);
        */
        
        /*
		String s = null;
		try {
	         DateFormat formatter ; 
	         Date date ;    
	         formatter = new SimpleDateFormat("E, dd MMMM yyyy hh:mm:ss");
	         date = (Date)formatter.parse(dateFormatted().toString());    
	         s = formatter.format(date);
	         System.out.println("Today is " + s);    	         
	    } 
		catch (ParseException e)
	    {
	    	System.out.println("Exception :" + e);    
	    } 
		return s;
		*/
	}
	
	/**
	 * Change the hour of this SimpleDateTime to 23:59.
	 */
	public void setToLastHour() {
		Calendar cal = Calendar.getInstance();
		cal.setTime(mDate);
		cal.set(Calendar.HOUR_OF_DAY, 23);
		cal.set(Calendar.MINUTE, 59);
		mDate = cal.getTime();
	}
	
	@Override
	public String toString() {
		return dateFormatted().toString();
	}

	/**
	 * Compare only the date, not the hour, minutes, and seconds.
	 */
	@Override
	public int compareTo(SimpleDateTime date) {
		//System.out.println("Comparing " + mDate.toString() + " to " + date.mDate);
		//return mDate.compareTo(date.mDate);
		
		// Compare with our equals() which only compares dates, not times
		if (date.equals(this))
			return 0;
		else {
			// Since the dates aren't thre same, we can use the regular compareTo()
			// which does compare time info
			return mDate.compareTo(date.mDate);
		}
	}
	
	/**
	 * Return true if the dates are the same (ignore hours, minutes, seconds).
	 */
	@Override
	public boolean equals(Object o) {		
		if (o instanceof SimpleDateTime) {
			SimpleDateTime d = (SimpleDateTime)o;
			//if (d.mDay == mDay && d.mMonth == mMonth && d.mYear == mYear)
			
			// Compare entire thing
			if (d.longDateFormatted().equals(this.longDateFormatted()))
				return true;
			else
				return false;
		}
		else
			return super.equals(o);
	}
	
	public boolean equalsDate(SimpleDateTime d) {
		return (d.getDay() == mDay && d.getMonth() == mMonth && d.getYear() == mYear);
	}
}

package dev.memento;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.StringTokenizer;
import java.util.TimeZone;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.widget.Toast;


public class Settings extends PreferenceActivity {
	
	private ListPreference mTimegateListPref;
	private ListPreference mTimegateDeleteList;
	
	private EditTextPreference mNewTimegate;
	
	private ArrayList<String> mTimegateUris;
	
	private SharedPreferences mPrefs;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
		
		
		mPrefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		
		// If preferences haven't been saved then use the default preferences
		// (from arrays.xml).  Otherwise use what's in the saved prefs.		
		// Note that timegate URLs are saved as a space-delimited string because
		// prefs don't have the ability to save arrays.
		
		String timegates = mPrefs.getString("timegates", null);
		if (timegates == null) {
			System.out.println("timegates == null");
			String[] list = getResources().getStringArray(R.array.listTimegates);
			mTimegateUris = new ArrayList<String>(list.length);
			for (String s : list)
				mTimegateUris.add(s);  
		}
		else {			
			System.out.println("timegates != null");
			mTimegateUris = convertStringToArray(timegates);
			System.out.println("mTimegateUris = " + timegates);
		}
				
		
		mTimegateListPref = (ListPreference) findPreference("defaultTimegate");		
		mTimegateDeleteList = (ListPreference) findPreference("deleteTimegate");

		updateTimegateLists();
		
		mTimegateDeleteList.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				
				// Remove this preference from the list
				System.out.println("newValue = " + newValue);
				
				CharSequence[] values = mTimegateListPref.getEntryValues();
				
				if (newValue.equals(values[0])) {
					showToast("The first timegate may not be deleted.");
					return false;
				}
				
				mTimegateUris.remove(newValue);
				
				// If this was the default timegate, select the first one as the default
				if (mTimegateListPref.getValue().equals(newValue)) {
					System.out.println("SELECTED DEFAULT, so resetting");					
					mTimegateListPref.setValue(values[0].toString());
					showToast("New default timegate is\n" + values[0].toString());
				}
				updateTimegateLists();
				
				// Don't allow the lanl timegate from being deleted
				if (mTimegateUris.size() == 1) 
					mTimegateDeleteList.setEnabled(false);				
				
				SharedPreferences.Editor ed = mPrefs.edit();
				ed.putString("timegates", convertArrayToString(mTimegateUris));
				ed.commit();
				
				return false;				
			}
			
		});
		
		// Don't allow the lanl timegate to be deleted
		if (mTimegateUris.size() == 1) {
			mTimegateDeleteList.setEnabled(false);
		}
		
		// Get the custom preference
		Preference dateSetting = (Preference) findPreference("dateAndTimeSettings");
		dateSetting.setSummary(getTimeFormatString());
		dateSetting.setOnPreferenceClickListener(new OnPreferenceClickListener() {

			@Override
			public boolean onPreferenceClick(Preference preference) {
								
				/*
				SharedPreferences customSharedPreference = getSharedPreferences(
						"myCustomSharedPrefs", Activity.MODE_PRIVATE);
				SharedPreferences.Editor editor = customSharedPreference
						.edit();
				editor.putString("myCustomPref",
						"The preference has been clicked");
				editor.commit();
				*/
				
				startActivityForResult(new Intent(android.provider.Settings.ACTION_DATE_SETTINGS), 0);
				
				return true;
			}

		});
		
		mNewTimegate = (EditTextPreference) findPreference("addTimegate");
		
		mNewTimegate.setOnPreferenceClickListener(new OnPreferenceClickListener() {

			@Override
			public boolean onPreferenceClick(Preference preference) {
				System.out.println("onPreferenceClick");
				
				// This won't be set immediately... not sure how to do that
				mNewTimegate.setText("http://");
				
				return true;
			}

		});	
		
		mNewTimegate.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				String text = mNewTimegate.getEditText().getText().toString();
				System.out.println("text = " + text);
				
				if ((text.startsWith("http://") && text.length() > 7) ||						
					(text.startsWith("https://") && text.length() > 8)) {
																		
					// Add new URL to list of timegates
					mTimegateUris.add(text);
										
					SharedPreferences.Editor ed = mPrefs.edit();
					ed.putString("timegates", convertArrayToString(mTimegateUris));
					ed.commit();
					
					updateTimegateLists();
					
					// Now there's at least 2 available, so allow user to delete it
					mTimegateDeleteList.setEnabled(true);					
					
					return false;
				}
				else {
					showToast("Please provide a valid URL.");
					return false;
				}
			}
			
		});
	}
	
	private void showToast(String message) {
    	Toast.makeText(getBaseContext(), message, Toast.LENGTH_LONG).show();
    }
	
	private CharSequence[] convertArrayToCharSeqArray(ArrayList<String> list) {
		CharSequence[] cs = new CharSequence[list.size()];
		int i = 0;
		for (String s : list) {
			cs[i] = s;
			i++;
		}
		return cs;
	}
	
	private void updateTimegateLists() {
		CharSequence[] tg = convertArrayToCharSeqArray(mTimegateUris);
		mTimegateListPref.setEntries(tg);
		mTimegateListPref.setEntryValues(tg);
		mTimegateDeleteList.setEntries(tg);
		mTimegateDeleteList.setEntryValues(tg);
	}
	
	
	private String convertArrayToString(List<String> items) {
		StringBuffer buffer = new StringBuffer();
		
		for (String url : items) {
			buffer.append(url);
			buffer.append(" ");
		}
		buffer.replace(buffer.length()-1, buffer.length(), "");
		
		System.out.println("URLS: [" + buffer.toString() + "]");
		return buffer.toString();
	}
	
	private ArrayList<String> convertStringToArray(String list) {
		ArrayList<String> timegates = new ArrayList<String>();
		
		StringTokenizer st = new StringTokenizer(list);
		while (st.hasMoreTokens()) {
		    String url = st.nextToken();
		    timegates.add(url);
		}
		return timegates;
	}
		
	
	@Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		
		System.out.println("onActivityResult resultCode = " + resultCode);
		
				
		System.out.println("New format = " + getTimeFormatString());
			
		Preference dateSetting = (Preference) findPreference("dateAndTimeSettings");
		dateSetting.setSummary(getTimeFormatString());
			
		
        // If subactivity has resulted in a date & time selection, update the time
        //if (resultCode == RESULT_OK) 
        //    finish();
        
    }   
	
	/**
	 * Return a string formatted to Android's date and time settings that shows
	 * Dec 31, 2010 at 6:30 pm.
	 * @return The properly formatted string.
	 */
	private String getTimeFormatString() {
		String timeDateFormat;
		//Date date = new Date();
				
		DateFormat df = android.text.format.DateFormat.getDateFormat(getApplicationContext());
		TimeZone tz = df.getTimeZone();
		
		Calendar cal = new GregorianCalendar(tz);
		cal.set(Calendar.HOUR_OF_DAY, 18);
		cal.set(Calendar.MINUTE, 30);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		cal.set(Calendar.DAY_OF_MONTH, 31);
		cal.set(Calendar.MONTH, Calendar.DECEMBER);
		cal.set(Calendar.YEAR, 2010);
		Date date = cal.getTime();		  
		
    	timeDateFormat = df.format(date);
    	df = android.text.format.DateFormat.getTimeFormat(getApplicationContext());
    	timeDateFormat += " " + df.format(date);
    	timeDateFormat += ", " + tz.getDisplayName();
    	return timeDateFormat;
	}	
}
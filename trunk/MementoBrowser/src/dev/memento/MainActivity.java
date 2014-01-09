/**
 * MementoBrowser.java
 * 
 * Copyright 2013 Frank McCown
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
 *  This is the Memento Browser activity which houses a customized web browser for
 *  performing http queries using Memento.
 *  
 *  Learn more about Memento:
 *  http://mementoweb.org/
 */

package dev.memento;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Stack;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff.Mode;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.Html;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnKeyListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebBackForwardList;
import android.webkit.WebChromeClient;
import android.webkit.WebHistoryItem;
import android.webkit.WebIconDatabase;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
	
	public final static String LOG_TAG = "MementoBrowser_tag";
	
	// Used to send info to MementoDetailActivity
	public final static String EXTRA_ORIGINAL_URL = "dev.memento.ORIGINAL_URL";
	public final static String EXTRA_MEMENTO_URL = "dev.memento.MEMENTO_URL";
	public final static String EXTRA_MEMENTO_DATE = "dev.memento.MEMENTO_DATE";	
	
	// Note that dialogs have been depricated in favor of DialogFragments, 
	// but I'm going to leave it the way it is for the time being.
	static final int DIALOG_DATE = 0;
    static final int DIALOG_ERROR = 1;
    static final int DIALOG_MEMENTO_DATES = 2;
    static final int DIALOG_MEMENTO_YEARS = 3;
    static final int DIALOG_MEMENTO_MONTHS = 4;
    
	private String mDefaultTimegateUri;
	
	// Keeps track of URLs the user enters for autocomplete
	private LocationAutoCompleteAdapter mLocationAdapter = null;
	
	// Web browser
	private WebView mWebview;
	private FrameLayout webViewPlaceholder;
	
	// URL viewed in web browser
	private AutoCompleteTextView mLocation;
	
	// Navigating through list of Mementos
	private ImageButton mNextButton;
	private ImageButton mPreviousButton;
	private Button mNowButton;
			
	// For showing the page loading progress
	private ProgressBar mPageLoadingProgressBar;
	
	// For showing progress loading mementos
	private ProgressBar mMementoProgressBar;
	
	private AsyncTask<Void, Void, Void> mFindMementos;
	
	private TextView mDateChosenButton;
	private TextView mDateDisplayedView;
    private SimpleDateTime mDateChosen;
    private SimpleDateTime mDateDisplayed;    
    private SimpleDateTime mToday;
        
    private TimeBundle mTimeBundle;
    private HashSet<TimeMap> mTimeMaps;
    private Memento mFirstMemento;
    private Memento mLastMemento;
    private MementoList mMementos;
    private Memento mCurrentMemento;   // The memento currently being viewed
    
    private boolean mUserPressedBack;
    
    // Used to determine which dialog boxes 
    private final int MAX_NUM_MEMENTOS_IN_LIST = 20;
    private final int MAX_NUM_MEMENTOS_PER_MONTH = 50;
        
    // Used when selecting a memento
    int mSelectedYear = -1;
    int mSelectedMonth = -1;
    
    // Used in http requests
    public String mUserAgent;
    
    // Hold favicons for certain websites.  This can be removed when we figure out
    // how to access the favicons for the WebView.  This is an outstanding problem
    // that I've solicited for help on StackOverflow:
    // http://stackoverflow.com/questions/3462582/display-the-android-webviews-favicon
    private HashMap<String,Bitmap> mFavicons;
    
    // The original URL that we are visiting
    private String mOriginalUrl;
    
    // The URL currently displayed in the browser
    private String mCurrentUrl;
    
    // Title of the currently displayed page
    private String mPageTitle;
    
    // Used to notify user of problems
    private CharSequence mErrorMessage;
    
    private Stack<ExWebHistoryItem> mWebHistory;
    
    // Need handler for callbacks to the UI thread
    final Handler mHandler = new Handler();
        
    // Runnable for updating UI on the UI thread
    final Runnable mUpdateResults = new Runnable() {
        public void run() {
            updateResultsInUi();
        }
    };
    
    // Runnable for showing error messages or disabling Next and Prev buttons
    // on the UI thread
    final Runnable mUpdateNextPrev = new Runnable() {
        public void run() {
       	
        	if (mErrorMessage == null) {
        		setEnableForNextPrevButtons();
        	}
        	else {
        		mMementoProgressBar.setVisibility(View.GONE);
        		refreshDisplayedDate();
        		displayError(mErrorMessage.toString());
        	}        	   	
        }       	
        
    };
    
    /**
     * Set all buttons back to originial configuration
     */
    private void resetMementoButtons() {
    	mMementoProgressBar.setVisibility(View.GONE);
    	mToday = new SimpleDateTime();
    	setChosenDate(mToday);
    	setDisplayedDate(mToday);
    	setImageButtonEnabled(false, mNextButton, R.drawable.next_item);
    	setImageButtonEnabled(false, mPreviousButton, R.drawable.previous_item);
		mNowButton.setEnabled(false);
		mDateChosenButton.setText(getText(R.string.button_date));
    }
    	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
                
        mUserPressedBack = false;
                
        setContentView(R.layout.activity_main);            
               
        mUserAgent = getApplicationContext().getText(R.string.user_agent).toString();
                       
        mWebHistory = new Stack<ExWebHistoryItem>();
        
        // Set the date and time format
        SimpleDateTime.mDateFormat = android.text.format.DateFormat.getDateFormat(getApplicationContext());
        SimpleDateTime.mTimeFormat = android.text.format.DateFormat.getTimeFormat(getApplicationContext());        
        
        // Holds all the timemaps for the web page being viewed
        mTimeMaps = new HashSet<TimeMap>();
                
        // Set the current date
        mToday = new SimpleDateTime();   
        
    	mCurrentUrl = getApplicationContext().getText(R.string.homepage).toString();
    	mOriginalUrl = mCurrentUrl;
    	        	     
    	mDateChosen = mToday;       
    	mDateDisplayed = mToday;
        
        mMementos = new MementoList();       
        
        // Add some favicons of web archives used by proxy server
        mFavicons = new HashMap<String, Bitmap>();        
        mFavicons.put("ia", BitmapFactory.decodeResource(getResources(), 
	              R.drawable.ia_favicon));
        mFavicons.put("webcite", BitmapFactory.decodeResource(getResources(), 
	              R.drawable.webcite_favicon));
        mFavicons.put("national-archives", BitmapFactory.decodeResource(getResources(), 
	              R.drawable.national_archives_favicon));
        
               
        initUI();        
    }    
    
    private void initUI() {
    	
    	mDateChosenButton = (Button) findViewById(R.id.dateChosen);
        mDateDisplayedView = (TextView) findViewById(R.id.dateDisplayed);  
        mNowButton = (Button) findViewById(R.id.nowButton);
        mNowButton.setEnabled(false);
        
    	mPageLoadingProgressBar = (ProgressBar) findViewById(R.id.pageLoadProgressBar);
        mPageLoadingProgressBar.setVisibility(View.GONE);
        
        mMementoProgressBar = (ProgressBar) findViewById(R.id.loadMementosProgressBar);
        mMementoProgressBar.setVisibility(View.GONE);
                                       
        mLocation = (AutoCompleteTextView) findViewById(R.id.locationEditText);
        
        // Load list of popular URLs so they are easier to enter 
        String[] defaultSites = getResources().getStringArray(R.array.defaultWebsites);        
        if (mLocationAdapter == null)
        	mLocationAdapter = new LocationAutoCompleteAdapter(this, 
        		android.R.layout.simple_dropdown_item_1line, defaultSites);        
        
        mLocation.setAdapter(mLocationAdapter);
        mLocation.setFocusableInTouchMode(true);
        mLocation.setOnKeyListener(new OnKeyListener() {
			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
								
				// Go to URL if user presses enter
				if (event.getAction() == KeyEvent.ACTION_DOWN &&
						keyCode == KeyEvent.KEYCODE_ENTER) {
									
					String url = Utilities.fixUrl(mLocation.getText().toString());
					if (Utilities.isArchiveUrl(url)){
						// Don't do anything but load the URL
						if (Log.LOG) Log.d(LOG_TAG, "Loading archive URL " + url);
					}
					else if (Utilities.isValidUrl(url)) {
						// Always bounce to the present when the user types a URL
						
						mOriginalUrl = url;
						if (Log.LOG) Log.d(LOG_TAG, "Browsing to NOW " + url);
					
						mLocationAdapter.add(url);										            	
	            		resetMementoButtons();
	            		mCurrentMemento = null;		            		        			            		
	            		
	            		// Clear since we are visiting a different page in the present
	            		mMementos.clear();
	            		
	            		surfToUrl(mOriginalUrl);
		            			            	
		            	// Hide the virtual keyboard
		            	((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE))  
		                	.hideSoftInputFromWindow(mLocation.getWindowToken(), 0);  
		            	
		            	mWebview.requestFocus();
		            	
						return true;
					}					
					else {
						MainActivity.this.showToast("Please enter a valid URL.");
						
						// Put focus back in text box
						mHandler.postDelayed(new Runnable() {
			                @Override
			                public void run() {                 
			                	mLocation.requestFocus();

			                    // Select all text
			                	mLocation.setSelection(0, mLocation.getText().length());                  
			                }                   
			            }, 200);
						
						return true;
					}
				}
					
				return false;
			}        	
        });            
                     
        
        mNextButton = (ImageButton) findViewById(R.id.next);
        setImageButtonEnabled(false, mNextButton, R.drawable.next_item);
        mNextButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	// Advance to next Memento
            	Memento nextMemento = null;
            	
            	// This could happen if the index has not been set yet
            	if (mMementos.getCurrentIndex() < 0) {
            		int index = mMementos.getIndexByDate(mDateDisplayed);
            		if (index < 0) {
            			if (Log.LOG) Log.d(LOG_TAG, "Could not find Memento with date " + mDateDisplayed + 
            					" (" + mMementos.size() + " mementos). Try to find next another way.");
            			
            			// Try alternative way of getting the next memento
            			nextMemento = mMementos.getNext(mDateDisplayed);
            		}
            		else {
            			mMementos.setCurrentIndex(index);
            			nextMemento = mMementos.getNext();
            		}
            	}
            	else {       	
            		// Locate the next Memento in the list
            		nextMemento = mMementos.getNext();
            	}
            	            	
            	if (nextMemento == null) {
            		// This could happen if we got redirected to the memento and didn't know
            		// it was the last memento, so the Next button was not disabled
            		if (Log.LOG) Log.d(LOG_TAG, "Still could not find next Memento.");
            		if (Log.LOG) Log.d(LOG_TAG, "Current index is " + mMementos.getCurrentIndex());
            		setImageButtonEnabled(false, mNextButton, R.drawable.next_item);
            	}
            	else {
            		SimpleDateTime date = nextMemento.getDateTime();
            		//setChosenDate(nextMemento.getDateTime());
            		showToast("Time traveling to next Memento on " + date.dateFormatted());
            		if (Log.LOG) Log.d(LOG_TAG, "Going to next Memento on " + date);
            		
            		mDateDisplayed = date;
            		
            		mCurrentMemento = nextMemento;
					String redirectUrl = nextMemento.getUrl();
					surfToUrl(redirectUrl);
					
					// Just in case it wasn't already enabled
					//mPreviousButton.setEnabled(true);
					MainActivity.this.setImageButtonEnabled(true, mPreviousButton, R.drawable.previous_item);
					
					// If this is the last memento, disable button
					if (mMementos.isLast(date))
						setImageButtonEnabled(false, mNextButton, R.drawable.next_item);
            	}
            }
        });
        
        mPreviousButton = (ImageButton) findViewById(R.id.previous);
        setImageButtonEnabled(false, mPreviousButton, R.drawable.previous_item);
        mPreviousButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	// Advance to previous Memento
            	Memento prevMemento = null;
            	
            	// This could happen if the index has not been set yet
            	if (mMementos.getCurrentIndex() < 0) {
	            	int index = mMementos.getIndexByDate(mDateDisplayed);
	        		if (index < 0) {
	        			if (Log.LOG) Log.d(LOG_TAG, "Could not find Memento with date " + mDateDisplayed + 
            					" (" + mMementos.size() + " mementos). Try to find previous" +
	        					" another way.");

	        			// Try alternative way of getting the pervious memento
	        			prevMemento = mMementos.getPrevious(mDateDisplayed);
	        		}
	        		else {
	        			mMementos.setCurrentIndex(index);
	        			prevMemento = mMementos.getPrevious();
	        		}
            	}
            	else {   	
            		// Locate the prev Memento in the list
            		prevMemento = mMementos.getPrevious();
            	}
            	            	            	
            	if (prevMemento == null) {
            		if (Log.LOG) Log.d(LOG_TAG, "Still could not find previous Memento!");
            		if (Log.LOG) Log.d(LOG_TAG, "Current index is " + mMementos.getCurrentIndex());
            		setImageButtonEnabled(false, mPreviousButton, R.drawable.previous_item);
            	}
            	else {
            		SimpleDateTime date = prevMemento.getDateTime();
            		
            		showToast("Time traveling to previous Memento on " + date.dateFormatted());
            		if (Log.LOG) Log.d(LOG_TAG, "Going to previous Memento on " + date);
            		
            		mDateDisplayed = date;
            		
            		mCurrentMemento = prevMemento;
					String redirectUrl = prevMemento.getUrl();
					surfToUrl(redirectUrl);
					
					// Just in case it wasn't already enabled
					setImageButtonEnabled(true, mNextButton, R.drawable.next_item);
					
					// If this is the first memento, disable button
					if (mMementos.isFirst(date))
						setImageButtonEnabled(false, mPreviousButton, R.drawable.previous_item);
            	}
            }
        }); 
               
        
        // Idea to use placeholder and handle orientation changes ourself is from here:
        // http://www.devahead.com/blog/2012/01/preserving-the-state-of-an-android-webview-on-screen-orientation-change/
        webViewPlaceholder = ((FrameLayout)findViewById(R.id.webViewPlaceholder));
        if (mWebview == null) {
	        mWebview = new WebView(this);
	        mWebview.setLayoutParams(new ViewGroup.LayoutParams(LayoutParams.MATCH_PARENT, 
	        		LayoutParams.MATCH_PARENT));
	        mWebview.getSettings().setSupportZoom(true);
	        mWebview.getSettings().setBuiltInZoomControls(true);
	        mWebview.getSettings().setLoadWithOverviewMode(true);
	        
	        // Setting to true allows the zoom-in to work, but problems moving around.
	        // Sometimes the underlying webview library also set faults and crashes the app
	        // http://stackoverflow.com/questions/17187338/android-fatal-signal-11-sigsegv-in-webviewcorethre
	        // Safer to leave off although zoom won't work. 
	        //mWebview.getSettings().setUseWideViewPort(true);
	        	        
	        mWebview.setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);
	        mWebview.setScrollbarFadingEnabled(true);
	        mWebview.getSettings().setLoadsImagesAutomatically(true);
	        mWebview.getSettings().setJavaScriptEnabled(true);
	        mWebview.setWebViewClient(new MementoWebViewClient()); 
	        mWebview.setWebChromeClient(new MementoWebChromClient());
	        mWebview.getSettings().setUserAgentString(mUserAgent);
	        
	        
	        // Must be declared before favicons will be received
	        // http://stackoverflow.com/questions/3462582/display-the-android-webviews-favicon	        
	        WebIconDatabase.getInstance().open(getDir("icons", MODE_PRIVATE).getPath());
	                
	        surfToUrl(mCurrentUrl); 
	        
	        // Get focus away from location field
	        mWebview.requestFocus();
        }               
        
        webViewPlaceholder.addView(mWebview);
                
        mWebview.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {		
															
				// Hide the virtual keyboard
            	((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE))  
                	.hideSoftInputFromWindow(mLocation.getWindowToken(), 0);  
				
				return false;
			}        	
        });
    }
    
    @Override
	public void onConfigurationChanged(Configuration newConfig) {
    	
    	if (Log.LOG) Log.d(LOG_TAG, "-- onConfigurationChanged");
    	
		if (mWebview != null) {
			// Remove the WebView from the old placeholder
			webViewPlaceholder.removeView(mWebview);
		}

		super.onConfigurationChanged(newConfig);

		// Load the layout resource for the new configuration
		setContentView(R.layout.activity_main);

		// Reinitialize the UI
		initUI();
		
		refreshChosenDate();
		refreshDisplayedDate();
		
		if (!mToday.equalsDate(mDateDisplayed)) {
    		mNowButton.setEnabled(true);
    		setEnableForNextPrevButtons();
    	} 
		
		mLocation.setText(mWebview.getUrl());
	}
        
    @Override
    public void onResume() {
    	super.onResume();
    	                
    	// Get default timegate that was selected in the settings
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String[] timegateUris = getResources().getStringArray(R.array.listTimegates);
        mDefaultTimegateUri = prefs.getString("defaultTimegate", timegateUris[0]);
        
        if (Log.LOG) Log.d(LOG_TAG, "mDefaultTimegateUri = " + mDefaultTimegateUri);
    }
    
    @Override
    protected void onSaveInstanceState(Bundle outState) {		
    	super.onSaveInstanceState(outState);
    		
    	if (Log.LOG) Log.d(LOG_TAG, "-- onSaveInstanceState");
    	
    	mWebview.saveState(outState);
    	outState.putSerializable("mDateChosen", mDateChosen);
    	outState.putSerializable("mDateDisplayed", mDateDisplayed);
    	outState.putString("mCurrentUrl", mCurrentUrl);
    	outState.putString("mOriginalUrl", mOriginalUrl);
    	outState.putString("mPageTitle", mPageTitle); 	
    	outState.putSerializable("mMementos", mMementos);
    }

    
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
    	super.onRestoreInstanceState(savedInstanceState);
    	    	
    	if (Log.LOG) Log.d(LOG_TAG, "-- onRestoreInstanceState");
    	
    	mWebview.restoreState(savedInstanceState);
    	mDateChosen = (SimpleDateTime) savedInstanceState.getSerializable("mDateChosen");
    	mDateDisplayed = (SimpleDateTime) savedInstanceState.getSerializable("mDateDisplayed");
    	mOriginalUrl = savedInstanceState.getString("mOriginalUrl");
    	mPageTitle = savedInstanceState.getString("mPageTitle");
    	mMementos = (MementoList) savedInstanceState.getSerializable("mMementos");
    	
    	if (mMementos != null) {
    		mCurrentMemento = mMementos.getCurrent();
    		mFirstMemento = mMementos.getFirst();
    		mLastMemento = mMementos.getLast();    		
    	}
    	
    	// Only enable buttons if viewing Mementos
    	if (!mToday.equalsDate(mDateDisplayed)) {
    		mNowButton.setEnabled(true);
    		setEnableForNextPrevButtons();
    	}    		
    }

    @Override 
    public boolean onCreateOptionsMenu(Menu menu) { 
         super.onCreateOptionsMenu(menu);         	    
         MenuInflater inflater = getMenuInflater();
         inflater.inflate(R.menu.options_menu, menu);
         return true;
    } 
    
    
    // Launch the DatePicker dialog box.  Unfortunately this button could be pressed twice
    // very quickly, and multiple dialog boxes will be displayed.  Tried to disable the dialog
    // immediately so it could not be pressed again, but that didn't work.  Let's just hope
    // the user doesn't press it twice very quickly.
    public void showDatePicker(View v) {
    	    	
    	// Date picker's months range from 0-11
    	final DatePickerDialog dateDialog = new DatePickerDialog(this, null,
    			mDateChosen.getYear(), 
				mDateChosen.getMonth() - 1, mDateChosen.getDay());
    	
    	// Have to set positive and negative buttons because only a positive button
    	// will be displayed by default, and there is a bug in Android that causes
    	// the Back button (which should normally be used to cancel) to cause
    	// the onDateTime event handler to be triggered.  
    	// http://stackoverflow.com/questions/11444238/jelly-bean-datepickerdialog-is-there-a-way-to-cancel
    	    	
    	// If either of the buttons are set with .setButton(), the OnDateSetListener is 
    	// not triggered on my Galaxy Tab (Android 4.0.4), so implement listening here instead.
    	
    	dateDialog.setButton(DialogInterface.BUTTON_POSITIVE, getString(R.string.button_set), 
    		new DialogInterface.OnClickListener() {
	    	    public void onClick(DialogInterface dialog, int which) {
	    	    	DatePicker dp = dateDialog.getDatePicker();
	    	    	dateSelected(dp.getDayOfMonth(), dp.getMonth(), dp.getYear());
	     	    }
     	});
    	
    	dateDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(R.string.button_cancel), 
    		new DialogInterface.OnClickListener() {
	    	    public void onClick(DialogInterface dialog, int which) {
	    	    	// Nothing to do
	    	    	if (Log.LOG) Log.d(LOG_TAG, "Cancel was pressed");
	    	    }
    	}); 
    	
    	dateDialog.show();   	
    }
    
    /**
     * Called when the user selects a date from the DatePicker
     * @param day
     * @param month
     * @param year
     */
    private void dateSelected(int day, int month, int year) {
    	
    	SimpleDateTime date = new SimpleDateTime(day, month + 1, year);
    	
    	if (Log.LOG) Log.d(LOG_TAG, "Date selected: " + year + "-" + (month+1) + "-" + day);
     
    	if (mToday.equalsDate(date)) {              		
    		returnToPresent();
    	}
    	else if (mToday.compareDateTo(date) < 0) {
    		showToast("Future dates cannot be shown.");
    	}
    	else {
    		setChosenDate(date);
    		showToast("Time traveling to " + mDateChosen.dateFormatted());
    		mNowButton.setEnabled(true);
    		
    		// It's possible that the list of mementos has already been populated
    		if (mMementos.size() == 0)
    			makeMementoRequests();
    		else {
    			Memento closest = mMementos.getClosestDate(mDateChosen);
    			SimpleDateTime closestDate = closest.getDateTime();
    			int index = mMementos.getIndexByDate(closestDate);
        		if (index < 0) {
        			if (Log.LOG) Log.d(LOG_TAG, "Could not find closest Memento with date " + closest);

        			showToast("There was a problem retrieving this memento... I'll keep looking...");
        			
        			// Let's start the process over
        			makeMementoRequests();
        		}
        		else {
        			// If we couldn't load the exact requested date, show the date 
            		// that's being loaded.
        			if (!closestDate.equalsDate(mDateChosen)) {
        				showToast("Closest available is " + closestDate.dateFormatted());  
        				MainActivity.this.refreshDisplayedDate();
        			}
        	
        			mMementos.setCurrentIndex(index);        			
        			mCurrentMemento = mMementos.getCurrent();
        			setEnableForNextPrevButtons();
        			setDisplayedDate(closestDate);
        			mNowButton.setEnabled(true);
        			
        			surfToUrl(mCurrentMemento.getUrl());
        		}
    		}
    	}
 
    }
    
    
    /**
     * Return to the current web page and set all buttons to their proper state.
     */
    private void returnToPresent() {
    	
    	showToast("Returning to the present.");
    	resetMementoButtons();
    	
    	// It's possible if the user was going back a page to 
		// be viewing an archived page.  This is just a hack for IA pages,
		// so a more comprehensive solution should be implemented.
		mOriginalUrl = Utilities.getUrlFromArchiveUrl(mOriginalUrl);
		
		surfToUrl(mOriginalUrl);			
    	mMementos.setCurrentIndex(-1);
    	mCurrentMemento = null;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.menu_settings:
        	         						
			startActivityForResult(new Intent(this, SettingsActivity.class), 0);
			
            return true;
            
        case R.id.menu_list:
        	
        	// If there aren't any mementos yet, fetch the list
        	if (mMementos.size() == 0) {
        		//showToast("Let me fetch them...");        		
        		findMementos();        		
        	}
        	// We don't want to overwhelm the user with too many choices
        	else if (mMementos.size() > MAX_NUM_MEMENTOS_IN_LIST)
        		showDialog(DIALOG_MEMENTO_YEARS);
        	else
        		showDialog(DIALOG_MEMENTO_DATES);
        	
            return true;
            
        case R.id.menu_details:
        	
        	// Launch details activity
        	
        	Memento m = mCurrentMemento;
        	if (m == null) {
        		showToast("First select " + this.getString(R.string.button_date) + " or " + 
        				this.getString(R.string.menu_list));
        	}
        	else {        	
        		if (Log.LOG) Log.d(LOG_TAG, "View details of " + mCurrentMemento);
	        	Intent intent = new Intent(this, MementoDetailActivity.class);
	            intent.putExtra(EXTRA_ORIGINAL_URL, mOriginalUrl); 
	            intent.putExtra(EXTRA_MEMENTO_URL, m.getUrl());
	            intent.putExtra(EXTRA_MEMENTO_DATE, m.getDateTimeString());
	            startActivity(intent);
        	}
        	
        	return true;
            
        case R.id.menu_refresh:
        	
        	mWebview.reload();
        	return true;
        	
        case R.id.menu_help:        	
        	// Open a browser to the project's Help page
        	String url = getApplicationContext().getText(R.string.help_page).toString();
        	Uri uri = Uri.parse(url);
			startActivity(new Intent(Intent.ACTION_VIEW, uri));
			
        	return true;
        }
        
        return false;
    }
    
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	if (Log.LOG) Log.d(LOG_TAG, "--onActivityResult requestCode = " + requestCode);
    	
        if (requestCode == 0) {
        	// If the date/time settings were changed            
            SimpleDateTime.mDateFormat = android.text.format.DateFormat.getDateFormat(getApplicationContext());
            SimpleDateTime.mTimeFormat = android.text.format.DateFormat.getTimeFormat(getApplicationContext());
            
            refreshChosenDate();
            refreshDisplayedDate();
        }
    }
      

	public Object fetchUrl(String address) throws MalformedURLException,IOException {
		URL url = new URL(address);
		Object content = url.getContent();
		return content;
	}
    
    private void setEnableForNextPrevButtons() {
    	
    	// Making these use equalsDate instead of equals could mean that the buttons
    	// are disabled when there are multiple mementos with the same date at the
    	// front or back of the list, but there's no great way to set this otherwise
    	// since we are dealing with date granularity at times.
    	
    	// Make prev and next enabled only if we're not viewing the first and last mementos
		if (mFirstMemento != null)
			//mPreviousButton.setEnabled(!mFirstMemento.getDateTime().equals(mDateDisplayed));
			setImageButtonEnabled(!mFirstMemento.getDateTime().equalsDate(mDateDisplayed), 
					mPreviousButton, R.drawable.previous_item);
		else {
			if (Log.LOG) Log.d(LOG_TAG, "mFirstMemento is null !!");
		}
		
		if (mLastMemento != null)
			//mNextButton.setEnabled(!mLastMemento.getDateTime().equals(mDateDisplayed));
			setImageButtonEnabled(!mLastMemento.getDateTime().equalsDate(mDateDisplayed), 
					mNextButton, R.drawable.next_item);
		else {
			if (Log.LOG) Log.d(LOG_TAG, "mLastMemento is null !!");
		}
    }
        
    private void setChosenDate(SimpleDateTime date) {
    	mDateChosen = date;
    	refreshChosenDate();
    }   
          
    private void refreshChosenDate() {
    	if (mToday.equalsDate(mDateChosen)) 
    		mDateChosenButton.setText(getText(R.string.button_date));
    	else    	
    		mDateChosenButton.setText(mDateChosen.dateFormatted());
    }
    
    private void setDisplayedDate(SimpleDateTime date) {
    	mDateDisplayed = date;
    	refreshDisplayedDate();    	
    }    
    
    private void refreshDisplayedDate() {
    	mDateDisplayedView.setText(
    			Html.fromHtml(getText(R.string.display_date) + "\n<b>" + 
    			mDateDisplayed.dateFormatted() + "</b>"));
    }
       
    /**
     * Load url into webview.  Keeps track of browser history in case user presses Back.
     * @param url
     */
    private void surfToUrl(String url) {
    	
    	if (Log.LOG) Log.d(LOG_TAG, "Sending browser to " + url);
    	mWebview.loadUrl(url);    	
    }
        
    /**
     * Start http requests on a new thread to retrieve the Mementos for the current URL. 
     */
    protected void makeMementoRequests() {

    	// Indicate we are getting mementos
    	mMementoProgressBar.setVisibility(View.VISIBLE);
    	mDateDisplayedView.setText("Fetching\nMementos...");
    	
        // Fire off a thread to do some work that we shouldn't do directly in the UI thread
        Thread t = new Thread() {
            public void run() {
            	            	
            	makeHttpRequests(mOriginalUrl, true);
            	
            	// Enable or disable Next and Previous buttons
            	mHandler.post(mUpdateNextPrev);
            } 
        };
        t.start();
    }

    /**
     * Triggered from other threads to update the UI.  Shows a dialog box if there's an error. 
     */
    private void updateResultsInUi() {

    	mMementoProgressBar.setVisibility(View.GONE);
    	
    	// Back in the UI thread        
    	if (mErrorMessage == null) {
    					
    		// If we couldn't load the exact requested date, show the date 
    		// that's being loaded.
    		if (!mDateDisplayed.equalsDate(mDateChosen)) {
    			showToast("Closest available is " + mDateDisplayed.dateFormatted());  
    			
    			refreshDisplayedDate();
    		}    		
    	}
    	else
    		showDialog(DIALOG_ERROR);
    } 
    
    /**
     * Find all mementos for the current URL, then display them to the user so one
     * can be chosen.
     */
    private void findMementos() {

    	mFindMementos = new AsyncTask<Void, Void, Void>() {

    		private ProgressDialog pd;

    		@Override
    		protected void onPreExecute() {
    			pd = new ProgressDialog(MainActivity.this);
    			pd.setTitle("Fetching Mementos...");
    			pd.setMessage("Please wait.");
    			pd.setCancelable(true);
    			pd.setOnCancelListener(new DialogInterface.OnCancelListener(){
    		          public void onCancel(DialogInterface dialog) {
    		        	  
    		        	  // Get rid of dialog box but allow fetching to continue
    		        	  mFindMementos.cancel(false);
    		          }
    		    });

    			pd.setIndeterminate(true);
    			pd.show();
    		}

    		@Override
    		protected Void doInBackground(Void... arg0) {
    			
    			// Just in case an archive URL was being viewed
    			mOriginalUrl = Utilities.getUrlFromArchiveUrl(mOriginalUrl);
    			
    			// Load the Timemap directly.  I'm hard-coded the timemap URLs for 
    			// which unfortunately may need to be changed over time.
    			String timemapUrl = "http://mementoproxy.lanl.gov/aggr/timemap/link/1/";    			
    			if (mDefaultTimegateUri.startsWith("http://mementoproxy.cs.odu"))
    				timemapUrl = "http://mementoproxy.cs.odu.edu/aggr/timemap/link/";
    				
    			timemapUrl = "<" + timemapUrl + 
    					mOriginalUrl + ">;rel=\"timemap\";type=\"application/link-format\"";
    			Link link = new Link(timemapUrl);
    			mTimeMaps.clear();
    			mTimeMaps.add(new TimeMap(link));
    			if (!accessTimeMap() && mErrorMessage == null)
					mErrorMessage = "There were problems accessing the Memento's TimeMap. " +
							"Please try again later.";
    			
    			return null;
    		}

    		@Override
    		protected void onPostExecute(Void result) {
    			pd.dismiss();
    			
    			if (mErrorMessage != null)
    				displayError(mErrorMessage.toString());
    			else if (mMementos.size() == 0) {
    				if (Log.LOG) Log.d(LOG_TAG, "!! No mementos for " + mOriginalUrl);
    				displayError("Sorry, there are no Mementos for this web page.");
    			}
    			else if (mMementos.size() > MAX_NUM_MEMENTOS_IN_LIST)
            		showDialog(DIALOG_MEMENTO_YEARS);
            	else
            		showDialog(DIALOG_MEMENTO_DATES);
    		}
    	};
    	
    	mFindMementos.execute((Void[])null);
    }

    /**
     * Make http requests to the Timegate at the proxy server to obtain a Memento 
     * and its TimeMap.  This is done in a background thread so the UI is not locked up.
     * If an error occurs, mErrorMessage is set to an error message which is shown
     * to the user.
     * @param initUrl The URL whose Memento is to be discovered
     * @param loadMemento Set to true if you want the Memento discovered to be loaded
     * into the web browser.
     */
    private void makeHttpRequests(String initUrl, boolean loadMemento) {
    	
    	// Contact Memento proxy with chosen Accept-Datetime:
    	// http://mementoproxy.lanl.gov/aggr/timegate/http://example.com/
    	// Accept-Datetime: Tue, 24 Jul 2001 15:45:04 GMT    	   	
        
    	HttpClient httpclient = new DefaultHttpClient();
    	
    	// Disable automatic redirect handling so we can process the 302 ourself 
    	httpclient.getParams().setParameter(ClientPNames.HANDLE_REDIRECTS, false);
 
    	String url = mDefaultTimegateUri + initUrl;
        HttpGet httpget = new HttpGet(url);
        
        // Change the request date to 23:00:00 if this is the first memento.
        // Otherwise we'll be out of range.
        
        String acceptDatetime;
        
        if (mFirstMemento != null && mFirstMemento.getDateTime().equals(mDateChosen)) {
        	if (Log.LOG) Log.d(LOG_TAG, "Changing chosen time to 23:59 since datetime matches first Memento.");
        	SimpleDateTime dt = new SimpleDateTime(mDateChosen);
        	dt.setToLastHour();
        	acceptDatetime = dt.longDateFormatted();
        }
        else {
        	acceptDatetime = mDateChosen.longDateFormatted(); 
        }
        
        httpget.setHeader("Accept-Datetime", acceptDatetime);
        httpget.setHeader("User-Agent", mUserAgent);
                
        if (Log.LOG) Log.d(LOG_TAG, "Accessing: " + httpget.getURI());
        if (Log.LOG) Log.d(LOG_TAG, "Accept-Datetime: " + acceptDatetime);

        HttpResponse response = null;
		try {			
			response = httpclient.execute(httpget);
			
			if (Log.LOG) Log.d(LOG_TAG, "Response code = " + response.getStatusLine());
			
		} catch (Exception e) {
			mErrorMessage = "Sorry, we are having problems contacting the server. Please " +
					"try again later.";
			if (Log.LOG) Log.e(LOG_TAG, Utilities.getExceptionStackTraceAsString(e));
			return;		
		} finally {
			// Deallocate all system resources
	        httpclient.getConnectionManager().shutdown(); 
		}
        
        // Get back:
		// 300 (TCN: list with multiple Mementos to choose from)
		// or 302 (TCN: choice) 
		// or 404 (no Mementos for this URL)
    	// or 406 (TCN: list with only first and last Mementos)
		
		int statusCode = response.getStatusLine().getStatusCode(); 
		if (statusCode == 300) {
			// TODO: Implement.  Right now the lanl proxy doesn't appear to be returning this
			// code, so let's just ignore it for now.
			if (Log.LOG) Log.d(LOG_TAG, "Pick a URL from list - NOT IMPLEMENTED");			
		}
		else if (statusCode == 302) {
			// Send browser to Location header URL
			// Note that the date/time of this memento is not given in the Location but can
			// be found when parsing the Link header.
			
			Header[] headers = response.getHeaders("Location");
			if (headers.length == 0) {
				mErrorMessage = "Sorry, but there was an unexpected error that will " +
					"prevent the Memento from being displayed. Try again in 5 minutes.";
				if (Log.LOG) Log.e(LOG_TAG, "Error: Location header not found in response headers.");
			}
			else {					
				final String redirectUrl = headers[0].getValue();
				
				// We can't update the view directly since we're running
				// in a thread, so use mUpdateResults to show a toast message
				// if accessing a different date than what was requested.
				
				//mHandler.post(mUpdateResults);
				
				// Parse various Links
				headers = response.getHeaders("Link");
				if (headers.length == 0) {
					if (Log.LOG) Log.e(LOG_TAG, "Error: Link header not found in response headers.");
					mErrorMessage = "Sorry, but the Memento could not be accessed. Try again in 5 minutes.";
				}
				else {
					String linkValue = headers[0].getValue();
											
					mTimeMaps.clear();
			    	mTimeBundle = null;
			    	mMementos.clear();
			    	
			    	// Get the datetime of this mememnto which should be supplied in the
			    	// Link: headers
			    	// Do not add the mementos to the global list of mementos because
			    	// the global list will be created when we process the timemap later.
			    	Memento memento = parseCsvLinks(linkValue, false);
			    	
			    	if (loadMemento) {			    		
			    		mDateDisplayed = memento.getDateTime();
			    		if (Log.LOG) Log.v(LOG_TAG, "Closest to " + mDateChosen + " is " + mDateDisplayed);
							    						
			    		if (Log.LOG) Log.d(LOG_TAG, "Redirecting browser to " + redirectUrl);
						mCurrentMemento = memento;
						
						// Can't call WebView methods except on UI thread!
						mHandler.post(new Runnable() {	
							@Override
							public void run() {
								surfToUrl(redirectUrl);						
							}					
						});
						
			    		// Now that we know the date, update the UI to reflect it
			    		mHandler.post(mUpdateResults);
			    	}
			    	
					if (mTimeMaps.size() > 0)
						if (!accessTimeMap() && mErrorMessage == null)
							mErrorMessage = "There were problems accessing the Memento's TimeMap. " +
									"Please try again later.";
				}
			}
		}		
		else if (statusCode == 404) {
			if (Log.LOG) Log.d(LOG_TAG, "Received 404 from proxy so no mementos for " + initUrl);
			mErrorMessage = "Sorry, there are no Mementos for this web page.";
		}
		else if (statusCode == 406) {
														
			// Parse various Links
			Header[] headers = response.getHeaders("Link");
			
			if (headers.length == 0) {
				if (Log.LOG) Log.d(LOG_TAG, "Error: Link header not found in 406 response headers.");
				//mErrorMessage = "Sorry, but there was an error in retreiving this Memento.";
				
				// The lanl proxy has it wrong.  It should return 404 when the URL is not
				// present, so we'll just pretend this is a 404.
				mErrorMessage = "Sorry, but there are no Mementos for this URL.";						
			}
			else {
				String linkValue = headers[0].getValue();
				
				mTimeMaps.clear();
		    	mTimeBundle = null;
		    	mMementos.clear();
		    	
				parseCsvLinks(linkValue, false);		
		    	
				if (mTimeMaps.size() > 0)
					accessTimeMap();
				
				if (mFirstMemento == null || mLastMemento == null) {
					if (Log.LOG) Log.e(LOG_TAG, "Could not find first or last Memento in 406 response for " + url);
					mErrorMessage = "Sorry, but there was an error in retreiving this Memento.";
				}
				else {			
					if (Log.LOG) Log.d(LOG_TAG, "Not available in this date range (" + mFirstMemento.getDateTimeSimple() +
							" to " + mLastMemento.getDateTimeSimple() + ")");
					
					// According to Rob Sanderson (LANL), we will only get 406 when the date is too
					// early, so redirect to first Memento
										
					mDateDisplayed = new SimpleDateTime(mFirstMemento.getDateTime());
					final String redirectUrl = mFirstMemento.getUrl();
					if (Log.LOG) Log.d(LOG_TAG, "Loading memento " + redirectUrl);
					
					// Can't call WebView methods except on UI thread!
					mHandler.post(new Runnable() {	
						@Override
						public void run() {
							surfToUrl(redirectUrl);						
						}					
					});
					
					mHandler.post(mUpdateResults);
				}
			}
		}
		else {
			mErrorMessage = "Sorry, but there was an unexpected error that will " +
				"prevent the Memento from being displayed. Try again in 5 minutes.";
			if (Log.LOG) Log.e(LOG_TAG, "Unexpected response code in makeHttpRequests = " + statusCode);
		}               
    }  
     
    /**
     * Makes sure that this link contains a timemap that has not already been seen.
     * @param link
     * @return true if the timemap's URL already exists in the list of timemaps, false otherwise.
     */
    private boolean timeMapAlreadyExists(Link link) {
    	for (TimeMap tm : mTimeMaps) {
			if (tm.getUrl().equals(link.getUrl())) {
				if (Log.LOG) Log.d(LOG_TAG, "Link contains a duplicate timemap URL that is being " +
						"ignored: " + link.toString());
				return true;
			}
    	}
    	
    	return false;
    }
    
    /**
     * Parse the links in CSV format and return the date of the last item with rel="memento" since
     * this information is needed when getting a 302 and needing to find the resource's datetime.
     * 
     * Example data:
     * 	 <http://mementoproxy.lanl.gov/aggr/timebundle/http://www.harding.edu/fmccown/>;rel="timebundle",
     * 	 <http://www.harding.edu/fmccown/>;rel="original",
     * 	 <http://web.archive.org/web/20010724154504/www.harding.edu/fmccown/>;rel="first memento";datetime="Tue, 24 Jul 2001 15:45:04 GMT",
     * 	 <http://web.archive.org/web/20010910203350/www.harding.edu/fmccown/>;rel="memento";datetime="Mon, 10 Sep 2001 20:33:50 GMT",
     * 
     * Another example:
     *   <http://mementoproxy.lanl.gov/google/timebundle/http://www.digitalpreservation.gov/>;rel="timebundle",
     *   <http://www.digitalpreservation.gov/>;rel="original",
     *   <http://mementoproxy.lanl.gov/google/timemap/link/http://www.digitalpreservation.gov/>;rel="timemap";type="application/link-format",
     *   <http://webcache.googleusercontent.com/search?q=cache:http://www.digitalpreservation.gov/>;rel="first last memento";datetime="Tue, 07 Sep 2010 11:54:29 GMT"
     *   
     * @param links
     */
    public Memento parseCsvLinks(String links, boolean addToMementoList) {
    	    	
    	mFirstMemento = null;
    	mLastMemento = null;
    	
    	Memento returnMemento = null;
    	    	
    	// Dump to file for debugging
    	//dumpToFile(links);
    	    	
		String[] linkStrings = links.split("\"\\s*,");				
		if (Log.LOG) Log.d(LOG_TAG, "Start parsing " + linkStrings.length + " links");
		
		int mementoLinks = 0;
		
    	// Place all Links into the array and then sort it based on date
    	for (String linkStr : linkStrings) {
			    		
			// Add back "
			if (!linkStr.endsWith("\""))
				linkStr += "\"";
						
			linkStr = linkStr.trim();
			
			Link link = new Link(linkStr);
			
			String rel = link.getRel();
			if (rel.contains("memento")) {
				mementoLinks++;
				Memento m = new Memento(link);
				
				// There may be just one memento in the links, so it should be returned
				if (returnMemento == null)
					returnMemento = m;
				
				if (addToMementoList)
					mMementos.add(m);				
				
				// Peel out all values in rel which are separated by white space
				String[] items = link.getRelArray();
				for (String r : items) {						
					r = r.toLowerCase();
										
					// First and last should be reported in 302 response
					if (r.contains("first")) {
						mFirstMemento = m;
					}
					if (r.contains("last")) {
						mLastMemento = m;
					}
				}		
			}
			else if (rel.equals("timemap")) {
				// See if this is really a new timemap (server could be mistaken, and
				// we don't want to be caught in an infinite loop
				
				if (!timeMapAlreadyExists(link)) {
					if (Log.LOG) Log.d(LOG_TAG, "Adding new timemap " + link.toString());
					mTimeMaps.add(new TimeMap(link));
				}
			}
			else if (rel.equals("timebundle")) {
				mTimeBundle = new TimeBundle(link);
			}
		}    	
    	    	
    	// Sorting can take a long time.  If there are just a few (like from a TimeGate), 
    	// go ahead and sort since they are not usually listed in order.  But a large 
    	// listing from a TimeMap is already sorted by the LANL proxy.
    	if (addToMementoList && mMementos.size() < 5) {
    		if (Log.LOG) Log.d(LOG_TAG, "Sorting short Memento list...");
    		Collections.sort(mMementos);
    	}
    	
    	if (Log.LOG) Log.d(LOG_TAG, "Finished parsing, found " + mementoLinks + " Memento links");		
    	if (Log.LOG) Log.d(LOG_TAG, "Total mementos: " + mMementos.size());
				
		// If these aren't set then this is likely a timemap 
		if (mFirstMemento == null)
			mFirstMemento = mMementos.getFirst();
		if (mLastMemento == null)
			mLastMemento = mMementos.getLast();    
		
		return returnMemento;
    }  
    
    /**
     * Display toast message.
     * @param message to display
     */
    private void showToast(String message) {
    	Toast.makeText(getBaseContext(), message, Toast.LENGTH_LONG).show();
    }
    
    /**
     * Show error message in a dialog box.
     * @param errorMsg
     */
    private void displayError(String errorMsg) {
    	mErrorMessage = errorMsg;
    	showDialog(DIALOG_ERROR);
    }
    
   
    /**
     * Get the favicon and title from the web page.
     *
     */
    private class MementoWebChromClient extends WebChromeClient {
    	
    	@Override
    	public void onReceivedIcon(WebView view, Bitmap icon) {    		
    		displayFavicon(icon);           		
    	}
    	
    	@Override
    	public void onReceivedTitle (WebView view, String title) {
    		// Title of the web page
    		mPageTitle = title;
    	}    	
    }
    
    /**
     * Callbacks for state changes in the WebView.
     *
     */
    private class MementoWebViewClient extends WebViewClient {
    	
    	/**
    	 * Called when the user clicks on a new link or when the browser is
    	 * redirecting because of a 3xx response.
    	 * Note: This method is *not* called when calling WebView's loadUrl().
    	 */
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
        	
        	// Fix partial URLs
        	if (!url.startsWith("http://") && !url.startsWith("https://"))
        		url = "http://" + url;
        	
        	if (Log.LOG) Log.d(LOG_TAG, "Click on link " + url); 
        	        	
        	// Only time travel if selected date is in the past!
        	if (mToday.compareDateTo(mDateChosen) <= 0) {
        		view.loadUrl(url);
        	
        		// It's possible if the user was going back a page to 
        		// be viewing an archived page.  This is just a hack for IA pages,
        		// so a more comprehensive solution should be implemented.
        		url = Utilities.getUrlFromArchiveUrl(url);
        		
        		mCurrentUrl = url;
        		
        		if (Log.LOG) Log.d(LOG_TAG, "mOriginalUrl = " + mOriginalUrl); 
        		        		
        		if (!mOriginalUrl.equals(url)) {
        			if (Log.LOG) Log.d(LOG_TAG, "-- Clearing all Mementos for new URL " + url);
        			mMementos.clear();
        			resetMementoButtons();
        		}
        			
        		mOriginalUrl = url;
        	}
        	else {        				
        		// User has clicked on a URL in an archived page (or the archive is
        		// redirecting to another archived page), so we need the original
        		// URL so we can find all its mementos
        		url = Utilities.getUrlFromArchiveUrl(url);        	
        		if (Log.LOG) Log.d(LOG_TAG, "Converted archived URL = " + url); 
        		
        		// Don't get new mementos if this is just a redirect to another archive page
        		// Note that IA might redirect to an archived page with a different date
        		// than what we thought the memento had, but let's not worry about updating
        		// our time because it's a very unusual thing to have happen.
        		if (!mOriginalUrl.equalsIgnoreCase(url)) {
        			if (Log.LOG) Log.d(LOG_TAG, "mOriginalUrl was " + mOriginalUrl);
        			mOriginalUrl = url;        		
        			if (Log.LOG) Log.d(LOG_TAG, "mOriginalUrl now is " + mOriginalUrl);
        			makeMementoRequests();
        		}
        	}        	          
    		
            return true;
        }

        /**
         * Errors can be for numerous reasons like 404, too many redirects, timeouts, etc.
         */
        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {

        	if (Log.LOG) Log.e(LOG_TAG, "WebViewClient Error: [code=" + errorCode + "] " + description +
        			" [URL=" + failingUrl + "]");
        	
        	// No need to keep bad URLs in the location history
        	mLocationAdapter.remove(failingUrl);
        }
        
        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
        	if (Log.LOG) Log.d(LOG_TAG, "-- onPageStarted");

        	mCurrentUrl = url;
        	mLocation.setText(url);

        	mPageLoadingProgressBar.setVisibility(View.VISIBLE);
        	
        	// Show date here because it can take a long time before it finishes downloading
        	if (Log.LOG) Log.d(LOG_TAG, "mDateDisplayed: " + mDateDisplayed.dateFormatted());
        	refreshDisplayedDate();
        	        	
        	if (favicon == null) {
        		// Display a favicon for some of the web archives.        		
        		// Use our built-in favicons since many of these archives don't have one
        		if (url.startsWith("http://webcitation.org")) 
        			favicon = mFavicons.get("webcite");      
        		else if (url.startsWith("http://api.wayback.archive.org") ||
        				url.startsWith("http://wayback.archive-it.org")) 
        			favicon = mFavicons.get("ia");
        		else if (url.startsWith("http://webarchive.nationalarchives")) 
        			favicon = mFavicons.get("national-archives");  		
        	}      	
        	
        	displayFavicon(favicon);    
        }
        
        @Override
        public void onPageFinished(WebView view, String url) {
        	mPageLoadingProgressBar.setVisibility(View.GONE);
        	
        	if (!mLocation.isSelected())
        		mLocation.setText(url);
        	
        	if (Log.LOG) Log.d(LOG_TAG, "-- onPageFinished... mDateDisplayed: " + mDateDisplayed.dateFormatted());
        	if (Log.LOG) Log.d(LOG_TAG, "   url = " + url);       	
        	
        	mCurrentUrl = url;
        	
        	if (mUserPressedBack) {
        		mUserPressedBack = false;
        		if (Log.LOG) Log.d(LOG_TAG, "!! User pressed back");
        	}
        	else {
	        	// Keep track in case user presses Back button 
	        	ExWebHistoryItem item = new ExWebHistoryItem();
	        	item.setMemento(mCurrentMemento);
	        	item.setOriginalUrl(mOriginalUrl);
	        	
	        	if (!mWebHistory.empty() && mWebHistory.peek().equals(item)) {
	        		// This happens because onPageFinished is often triggered twice
	        		// for the same URL.  Nice.
	        		if (Log.LOG) Log.v(LOG_TAG, "!! Looks like a duplicate");	        		
	        	}
	        	else {
		        	mWebHistory.push(item);
		        	if (Log.LOG) Log.d(LOG_TAG, "!! Storing history " + item);
	        	}	        	
	        	
	        	if (Log.LOG) {
		        	WebBackForwardList list = mWebview.copyBackForwardList();
		        	int curr = list.getCurrentIndex();
		        	Log.d(LOG_TAG, "   getCurrentIndex = " + curr);
	        	}
        	}
        	
        	// Show favicon if present
        	Bitmap favicon = view.getFavicon();
        	if (favicon != null) {
        		displayFavicon(favicon);        	     		
        	}        	
        }
    }
    
    private void displayFavicon(Bitmap favicon) {
    	BitmapDrawable bd = null;
    	if (favicon != null) {
    		bd = new BitmapDrawable(this.getResources(), favicon);
    		bd = resizeImage(bd, 32);
    	}
		mLocation.setCompoundDrawablesWithIntrinsicBounds(bd, null, null, null);
    }
    
    private BitmapDrawable resizeImage(BitmapDrawable image, int size) {
        Bitmap b = image.getBitmap();
        if (b.getWidth() < size) {
        	Bitmap bitmapResized = Bitmap.createScaledBitmap(b, size, size, false);
        	return new BitmapDrawable(this.getResources(), bitmapResized);
        }
        else {
        	// Don't worry about resizing if large enough
        	return image;
        }
    }
    
    @Override
    protected Dialog onCreateDialog(int id) {
    	
    	Dialog dialog = null;
    	AlertDialog.Builder builder = new AlertDialog.Builder(this);
    	
        switch (id) {
        	
        case DIALOG_ERROR:
        	builder.setMessage("error message")
	       		.setCancelable(false)
	       		.setPositiveButton("OK", null);
        	dialog = builder.create();     
        	break;    
        	
        case DIALOG_MEMENTO_YEARS:
        	builder.setTitle(R.string.select_year);
        	final TreeMap<Integer,Integer> yearCount = mMementos.getAllYears();
        	if (Log.LOG) Log.d(LOG_TAG, "Dialog: num of years = " + yearCount.size());
        	
        	// This shouldn't happen, but just in case
        	if (yearCount.size() == 0) {
        		showToast("There are no years to choose from... something is wrong.");
        		if (Log.LOG) Log.d(LOG_TAG, "Num of mementos: " + mMementos.size());
        		return null;
        	}
        	
        	// Build a list that shows how many dates are available for each year
        	final CharSequence[] yearText = new CharSequence[yearCount.size()];  
        	
        	// Parallel arrays used to determine which entry was selected.
        	// Could also have used a regular expression.
        	final int years[] = new int[yearCount.size()];
        	final int count[] = new int[yearCount.size()];
        	
        	int selectedYear = -1;        	
        	int displayYear = mDateDisplayed.getYear();
        	int i = 0;
        	for (Map.Entry<Integer,Integer> entry : yearCount.entrySet()) {
        		Integer year = entry.getKey();
        	
        		// Select the year of the Memento currently displayed
        		if (displayYear == year) 
        			selectedYear = i;
        		
        		years[i] = year;
        		count[i] = entry.getValue();
        		yearText[i] = Integer.toString(year) + " (" + entry.getValue() + ")";
        		i++;
        	}        	
        	
        	builder.setSingleChoiceItems(yearText, selectedYear, new DialogInterface.OnClickListener() {
        	    public void onClick(DialogInterface dialog, int item) {
        	    	dialog.dismiss();
        	    	
        	    	mSelectedYear = years[item];
        	    	int numItems = count[item];

        	    	if (numItems > MAX_NUM_MEMENTOS_PER_MONTH)
        	    		showDialog(DIALOG_MEMENTO_MONTHS);
        	    	else
        	    		showDialog(DIALOG_MEMENTO_DATES);
        	    }
        	});
        	
        	dialog = builder.create(); 
        	
        	// Cause the dialog to be freed whenever it is dismissed.
        	// This is necessary because the items are dynamic.  
        	dialog.setOnDismissListener(new OnDismissListener() {
				@Override
				public void onDismiss(DialogInterface arg0) {
        	    	removeDialog(DIALOG_MEMENTO_YEARS);        	    	
				}        		
        	});
        	
        	break;
        	
        case DIALOG_MEMENTO_MONTHS:
        	builder.setTitle(R.string.select_month);
        	final LinkedHashMap<CharSequence, Integer> monthCount = mMementos.getMonthsForYear(mSelectedYear);

        	// This shouldn't happen, but just in case
        	if (monthCount.size() == 0) {
        		showToast("There are no months to choose from... something is wrong.");
        		if (Log.LOG) Log.d(LOG_TAG, "Num of mementos: " + mMementos.size());
        		return null;
        	}
        	        	
        	// Build a list that shows how many dates are available for each month
        	final CharSequence[] monthText = new CharSequence[monthCount.size()];  
        	        	
        	int selectedMonth = mDateDisplayed.getMonth() - 1; 
        	i = 0;
        	for (Map.Entry<CharSequence,Integer> entry : monthCount.entrySet()) {
        		CharSequence month = entry.getKey();
        	        		
        		monthText[i] = month + " (" + entry.getValue() + ")";
        		i++;
        	}        	
        		
        	builder.setSingleChoiceItems(monthText, selectedMonth, new DialogInterface.OnClickListener() {
        	    public void onClick(DialogInterface dialog, int item) {
        	    	dialog.dismiss();
        	
        	    	// Pull out month name so we can map it back to a number.
        	    	// This is ugly, but it's necessary because the LinkedHashMap doesn't
        	    	// give back the order of its keys.
        	    	
        	    	Pattern r = Pattern.compile("^(.+) ");
        	    	Matcher m = r.matcher(monthText[item]);
        	    	if (m.find()) {
	        	    	String month = m.group(1);
	        	    	
	        	    	mSelectedMonth = Utilities.monthStringToInt(month);
	        	    	showDialog(DIALOG_MEMENTO_DATES);
        	    	}
        	    	else {
        	    		if (Log.LOG) Log.e(LOG_TAG, "Could not find month in [" + monthText[item] + "]");
        	    	}
        	    }
        	});
        	
        	dialog = builder.create(); 
        	
        	// Cause the dialog to be freed whenever it is dismissed.
        	// This is necessary because the items are dynamic.  
        	dialog.setOnDismissListener(new OnDismissListener() {
				@Override
				public void onDismiss(DialogInterface arg0) {
        	    	removeDialog(DIALOG_MEMENTO_MONTHS);      	    	
				}        		
        	});
        	
        	break;

        	
        case DIALOG_MEMENTO_DATES:     
        	
        	builder.setTitle(R.string.select_day);
        	
        	// Which radio button is selected?
        	int selected = -1;
        	
        	final CharSequence[] dates;
        	
        	if (Log.LOG) Log.d(LOG_TAG, "mSelectedMonth = " + mSelectedMonth);
        	if (Log.LOG) Log.d(LOG_TAG, "mSelectedYear = " + mSelectedYear);
        	
        	final Memento[] mementoList;
        	
        	// See if there is a month/year filter 
        	if (mSelectedMonth != -1 || mSelectedYear != -1) {
        		
        	   	if (mSelectedMonth != -1)        	   		
        	   		mementoList = mMementos.getByMonthAndYear(mSelectedMonth, mSelectedYear);   
        	   	else 
        	   		mementoList = mMementos.getByYear(mSelectedYear);
        	   	
        	   	if (Log.LOG) Log.d(LOG_TAG, "Number of dates = " + mementoList.length);
        	   	
        	   	// Get dates for selected mementos
        	   	dates = new CharSequence[mementoList.length];
        		i = 0;
            	for (Memento m : mementoList) { 
            		dates[i] = m.getDateAndTimeFormatted();
            		i++;
            	}
        	   	
        	   	// See if any of these items match.  This could take a little while if
        	   	// there are a large number of items unfortunately.
        		Memento m = mMementos.getCurrent();
        		if (m != null) {
        			CharSequence searchDate = m.getDateAndTimeFormatted();
	        		for (i = 0; i < dates.length; i++) {
	    				if (searchDate.equals(dates[i])) {
	    					selected = i;
	        				break;
	        			}
	        		}
        		}    		
	        }
        	else {
        		// No filter, so get all available mementos
        		dates = mMementos.getAllDates();
        		if (Log.LOG) Log.d(LOG_TAG, "Number of dates = " + dates.length);
        		selected = mMementos.getCurrentIndex();
        		mementoList = mMementos.toArray(new Memento[0]);
        	}
                	    
        	
        	if (Log.LOG) Log.d(LOG_TAG, "Selected index = " + selected);
        	
        	// Reset for future selections
        	mSelectedYear = -1;        	  
	    	mSelectedMonth = -1;
	    	
        	builder.setSingleChoiceItems(dates, selected, new DialogInterface.OnClickListener() {
        	    public void onClick(DialogInterface dialog, int item) {
        	    	dialog.dismiss();
        	    	        	    	       	    	        	    	        	    	
        	    	// Display this Memento
    	    		Memento m = mementoList[item]; 
    	    		mCurrentMemento = m;
    	    		final SimpleDateTime dateSelected = m.getDateTime();
            		mDateDisplayed = dateSelected;    
            		setChosenDate(mDateDisplayed);
            		if (Log.LOG) Log.d(LOG_TAG, "User selected Memento with date " + dateSelected.dateFormatted());
            		showToast("Time traveling to " + mDateDisplayed.dateFormatted());
            		refreshDisplayedDate();
            		            	   	
            	   	// Load memento into the browser           		
					String redirectUrl = m.getUrl();					
					surfToUrl(redirectUrl);
					
					setEnableForNextPrevButtons();
					mNowButton.setEnabled(true);
					
					// Potentially lengthly operation
					
					new Thread() {
			            public void run() {			            	
		                	int index = mMementos.getIndex(dateSelected);  
		                	if (index == -1) {
		                		// This should never happen
		                		if (Log.LOG) Log.e(LOG_TAG, "!! Couldn't find " + dateSelected + " in the memento list!");
		                	}
		                	else
		                		mMementos.setCurrentIndex(index);
			            } 
			        }.start();
                }        	              	    	
        	    
        	});
        	
        	dialog = builder.create();   
        	
        	// Cause the dialog to be freed whenever it is dismissed.
        	// This is necessary because the items are dynamic.  I couldn't find
        	// a better way to solve this problem.
        	dialog.setOnDismissListener(new OnDismissListener() {
				@Override
				public void onDismiss(DialogInterface arg0) {
        	    	removeDialog(DIALOG_MEMENTO_DATES);					
				}        		
        	});
        	
        	break;
        }
        	        	
        return dialog;
    }
    
    
    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {
        super.onPrepareDialog(id, dialog);

        switch (id) {       
        case DIALOG_ERROR:        		
    		AlertDialog ad = (AlertDialog) dialog;
    		ad.setMessage(mErrorMessage);
    		mErrorMessage = null;
            break;            
        }
    }
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
    	    	
    	// Back button goes back to previous page or memento
        if ((keyCode == KeyEvent.KEYCODE_BACK) && mWebview.canGoBack()) {
        	
        	// Get the previous URL to update our internal copy 
        	
        	WebBackForwardList list = mWebview.copyBackForwardList();
        	int curr = list.getCurrentIndex();
        	WebHistoryItem item = list.getItemAtIndex(curr - 1);
        	String url = item.getUrl();
        	
        	if (Log.LOG) Log.d(LOG_TAG, "GO BACK TO " + url); 

            mWebview.goBack();
            
            // Reset to today because it might be confusing to set to other dates
            setChosenDate(mToday);
            
            // Just in case
            mMementoProgressBar.setVisibility(View.GONE);
            
            if (Log.LOG) Log.d(LOG_TAG, "mWebHistory size = " + mWebHistory.size());
            if (mWebHistory.isEmpty()) {
            	if (Log.LOG) Log.e(LOG_TAG, "Stack maintaining page dates is empty!");
            }
            else {
	            ExWebHistoryItem item2 = mWebHistory.pop();   // Remove current
	            if (Log.LOG) Log.d(LOG_TAG, "!! Removed item " + item2 + " from stack");
	            
	            if (!mWebHistory.isEmpty()) {
	            	item2 = mWebHistory.peek();   // Previous
	            	if (Log.LOG) Log.d(LOG_TAG, "!! Top item is " + item2);
	            }
	            
	            mCurrentMemento = item2.getMemento();
	            if (Log.LOG) Log.d(LOG_TAG, "mCurrentMemento = " + mCurrentMemento);
	            if (mCurrentMemento == null)
	            	mDateDisplayed = mToday;
	            else
	            	mDateDisplayed = mCurrentMemento.getDateTime();
	            
	            if (Log.LOG) Log.d(LOG_TAG, "!! Back to date " + mDateDisplayed);
	            refreshDisplayedDate();
	            
	            // Indicate the user pressed the back button
	            mUserPressedBack = true;
	            
	            // Should be able to press if it's not today
	            mNowButton.setEnabled(mToday.compareDateTo(mDateDisplayed) != 0);
	            
	            //if (mCurrentMemento != null && !mCurrentMemento.getUrl().equals(url) && 
	            //		!url.equals(mOriginalUrl)) {
	            if (!mOriginalUrl.equals(item2.getOriginalUrl())) {
	            	mOriginalUrl = item2.getOriginalUrl();
	            	if (Log.LOG) Log.d(LOG_TAG, "** Clearing list of old mementos");
	            	if (Log.LOG) Log.d(LOG_TAG, "mOriginalUrl = " + mOriginalUrl);
	        		mMementos.clear();
	            }
            }
            
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
            
    /**
     * Return a timemap that has not been downloaded yet.
     * 
     * @return
     */
    private TimeMap getTimemapToDownload() {
    	if (Log.LOG) {
    		Log.d(LOG_TAG, "All " + mTimeMaps.size() + " timemaps:");
    		for (TimeMap tm : mTimeMaps) {
        		Log.d(LOG_TAG, tm.toString());
        	}
    	}    	
    	
    	for (TimeMap tm : mTimeMaps) {
    		if (!tm.isDownloaded()) 
    			return tm;
    	}
		return null;    	
    }
    
    /**
     * Retrieve the TimeMap from the Web and parse out the Mementos.
     * Currently this only recognizes TimeMaps using CSV formats. 
     * Other formats to be implemented: RDF/XML, N3, and HTML.
     * Supports paging timemaps where a timemap includes references
     * to other timemaps.
     * 
     * @return true if TimeMap was successfully retreived, false otherwise.
     */
    private boolean accessTimeMap() {    	   	
        
    	HttpClient httpclient = new DefaultHttpClient();
    	
    	TimeMap tm = getTimemapToDownload();
    	
    	// Access every timemap that has been discovered
    	while (tm != null) {
    		    	
    		tm.setDownloaded(true);
	    	String url = tm.getUrl();
	        HttpGet httpget = new HttpGet(url);
	        httpget.setHeader("User-Agent", mUserAgent);
	                
	        if (Log.LOG) Log.d(LOG_TAG, "Accessing TimeMap: " + httpget.getURI());
	
	        HttpResponse response = null;
			try {			
				response = httpclient.execute(httpget);				
				if (Log.LOG) Log.d(LOG_TAG, "Response code = " + response.getStatusLine());				
			} catch (Exception e) {
				if (Log.LOG) Log.e(LOG_TAG, Utilities.getExceptionStackTraceAsString(e));
				return false;                
			}
			
	        // Should get back 200 unless something is really wrong			
			int statusCode = response.getStatusLine().getStatusCode(); 
			if (statusCode == 200) {
				
				// See if MIME type is the same as Type		
				Header type = response.getFirstHeader("Content-Type");
				if (type == null) {
					if (Log.LOG) Log.w(LOG_TAG, "Could not find the Content-Type for " + url);
				}
				else if (!type.getValue().contains(tm.getType())) {
					if (Log.LOG) Log.w(LOG_TAG, "Content-Type is [" + type.getValue() + "] but TimeMap type is [" +
							tm.getType() + "] for " + url);
				}
				
				// Timemap MUST be "application/link-format", but leave csv for
				// backwards-compatibility with earlier Memento implementations
				if (tm.getType().equals("text/csv") ||
						tm.getType().equals("application/link-format")) {
					try {
						String responseBody = EntityUtils.toString(response.getEntity());
						parseCsvLinks(responseBody, true); 
					} catch (Exception ex) {
						if (Log.LOG) Log.e(LOG_TAG, Utilities.getExceptionStackTraceAsString(ex));
						httpclient.getConnectionManager().shutdown();
						return false;
					} 
				}
				else {
					if (Log.LOG) Log.e(LOG_TAG, "Unable to handle TimeMap type " + tm.getType());
					httpclient.getConnectionManager().shutdown();
					return false;
				}
			}		
			else if (statusCode == 404) {
				if (Log.LOG) Log.d(LOG_TAG, "404 response means no mementos");
				httpclient.getConnectionManager().shutdown();
				mErrorMessage = "Sorry, there are no Mementos for this web page.";
				return false;
			}
			else {
				if (Log.LOG) Log.d(LOG_TAG, "Unexpected response code in accessTimeMap = " + statusCode);
				httpclient.getConnectionManager().shutdown();
				return false;
			}        
			
			tm = getTimemapToDownload();
    	}
    	
		// Deallocate all system resources
        httpclient.getConnectionManager().shutdown();
        
        return true;
    }      
    
    public void nowButtonClick(View v) {
    	    	
    	// Bring us back to the current time
    	returnToPresent();
    }
    
    /**
     * Sets the specified image button to the given state and grays-out the icon.
     * Code adapted from:
     * http://stackoverflow.com/questions/8196206/disable-an-imagebutton
     * 
     * @param enabled The state of the button
     * @param item The button to modify
     * @param iconResId The button's icon ID
     */
    private void setImageButtonEnabled(boolean enabled, ImageButton item,
            int iconResId) {
        item.setEnabled(enabled);
        Drawable originalIcon = this.getResources().getDrawable(iconResId);
        Drawable icon = enabled ? originalIcon : convertDrawableToGrayScale(originalIcon);
        item.setImageDrawable(icon);
    }
    
    /**
     * Converts the given drawable to a gray image. This method may be 
     * used to simulate the color of disable icons in Honeycomb's ActionBar.
     * 
     * @return a version of the given drawable with a color filter applied.
     */
    private Drawable convertDrawableToGrayScale(Drawable drawable) {
        if (drawable == null) 
            return null;
        
        Drawable res = drawable.mutate();
        res.setColorFilter(Color.GRAY, Mode.SRC_IN);
        return res;
    }    
      
}
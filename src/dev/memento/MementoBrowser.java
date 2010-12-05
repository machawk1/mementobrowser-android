/**
 * MementoBrowser.java
 * 
 *  Programmed by Frank McCown at Harding University, 2010.
 *  
 *  This is the Memento Browser activity which houses a customized web browser for
 *  performing http queries using Memento.
 *  
 *  Learn more about Memento:
 *  http://mementoweb.org/
 */

package dev.memento;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnDismissListener;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.View.OnKeyListener;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebBackForwardList;
import android.webkit.WebChromeClient;
import android.webkit.WebHistoryItem;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class MementoBrowser extends Activity {
	
	public static final String LOG_TAG = "MementoBrowser_tag";
	
	static final int DIALOG_DATE = 0;
    static final int DIALOG_ERROR = 1;
    static final int DIALOG_MEMENTO_DATES = 2;
    static final int DIALOG_MEMENTO_YEARS = 3;
    static final int DIALOG_HELP = 4;
    
	private String mDefaultTimegateUri;
	private String[] mTimegateUris;
	
	private WebView mWebview;
	
	private TextView mLocation;
	private Button mGo;
	
	private Button mNextButton;
	private Button mPreviousButton;
	
	// To fix a bug described here: http://www.zunisoft.com/?p=1140 
	private DateFormat longDateFormat = new SimpleDateFormat("EEEE, MMMM d, yyyy");
	
	// For showing the page loading progress
	private ProgressBar mProgressBar;
	
	private TextView mDateChosenButton;
	private TextView mDateDisplayedView;
    private SimpleDateTime mDateChosen;
    private SimpleDateTime mDateDisplayed;    
    private SimpleDateTime mToday;
        
    private TimeBundle mTimeBundle;
    private TimeMap mTimeMap;
    private Memento mFirstMemento;
    private Memento mLastMemento;
    private MementoList mMementos;
    
    private final int MAX_NUM_MEMENTOS_IN_LIST = 20;
    
    // Used when selecting a memento
    int mSelectedYear = 0;
    
    // Used in http requests
    public String mUserAgent;
    
    // Hold favicons for certain websites.  This can be removed when we figure out
    // how to access the favicons for the WebView.  This is an outstanding problem
    // that I've solicited for help on StackOverflow:
    // http://stackoverflow.com/questions/3462582/display-the-android-webviews-favicon
    private HashMap<String,Bitmap> mFavicons;
    
    // The original URL that we are visiting
    private String mOriginalUrl;
    
   // private String mRedirectUrl;
    
    private CharSequence mErrorMessage;
    
    // Need handler for callbacks to the UI thread
    final Handler mHandler = new Handler();
    
    // Create runnable for posting
    final Runnable mUpdateResults = new Runnable() {
        public void run() {
            updateResultsInUi();
        }
    };
    
    final Runnable mUpdateNextPrev = new Runnable() {
        public void run() {
       	
        	if (mErrorMessage == null) {
        		setEnableForNextPrevButtons();
        	}
        	else {
        		mNextButton.setEnabled(false);
        		mPreviousButton.setEnabled(false);
        		displayError(mErrorMessage.toString());
        	}
        	
        	// Since making requests are over, hide progress bar
        	// BUT... the page may still be downloading, so don't hide
        	//mProgressBar.setVisibility(View.GONE);        	
        }       	
        
    };
    
    	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        
        setContentView(R.layout.main);             
        
        mUserAgent = getApplicationContext().getText(R.string.user_agent).toString();
        mOriginalUrl = getApplicationContext().getText(R.string.homepage).toString();
        
        mTimegateUris = getResources().getStringArray(R.array.listTimegates);
        
        // Add some favicons of web archives used by proxy server
        mFavicons = new HashMap<String, Bitmap>();        
        mFavicons.put("ia", BitmapFactory.decodeResource(getResources(), 
	              R.drawable.ia_favicon));
        mFavicons.put("webcite", BitmapFactory.decodeResource(getResources(), 
	              R.drawable.webcite_favicon));
        mFavicons.put("national-archives", BitmapFactory.decodeResource(getResources(), 
	              R.drawable.national_archives_favicon));
        
        // Set the date and time format
        SimpleDateTime.mDateFormat = android.text.format.DateFormat.getDateFormat(getApplicationContext());
        SimpleDateTime.mTimeFormat = android.text.format.DateFormat.getTimeFormat(getApplicationContext());        
        
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        mProgressBar.setVisibility(View.GONE);
        
        mMementos = new MementoList();
                        
        mLocation = (TextView) findViewById(R.id.locationEditText);
        mLocation.setSelectAllOnFocus(true);
        /*mLocation.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				// Select all text
				//mLocation.
				return false;
			}        	
        });*/
        
        mLocation.setOnKeyListener(new OnKeyListener() {
			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				//Log.d(LOG_TAG, "keyCode = " + keyCode + "   event = " + event.getAction());
				
				if (event.getAction() == KeyEvent.ACTION_DOWN &&
						keyCode == KeyEvent.KEYCODE_ENTER) {
										
					mOriginalUrl = mLocation.getText().toString();
					mOriginalUrl = fixUrl(mOriginalUrl);
					
					// Access live version if date is today or in the future
	            	if (mToday.compareTo(mDateChosen) <= 0) {
	            		Log.d(LOG_TAG, "Browsing to " + mOriginalUrl);
	            		mWebview.loadUrl(mOriginalUrl);
	            		
	            		// Clear since we are visiting a different page in the present
	            		mMementos.clear();
	            	}
	            	else {
	            		makeMementoRequests();
	            	}
	            	
	            	// Hide the virtual keyboard
	            	((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE))  
	                	.hideSoftInputFromWindow(mLocation.getWindowToken(), 0);  
					return true;
				}
					
				return false;
			}
        	
        });
        
        
        // TEST        
        /*
        Context context = getBaseContext();
        Drawable image = getImage(context, "http://web.archive.org/favicon.ico");
        if (image == null) {
        	System.out.println("image is null !!");
        }
        else {
        	//image.setBounds(5, 5, 5, 5);
			//ImageView imgView = new ImageView(context);
			//ImageView imgView = (ImageView)findViewById(R.id.imagetest);
			//imgView.setImageDrawable(image);
	        mLocation.setCompoundDrawablesWithIntrinsicBounds(image, null, null, null);
        }
		*/
        
        mGo = (Button) findViewById(R.id.goButton);
        mGo.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	mOriginalUrl = mLocation.getText().toString();
            	mOriginalUrl = fixUrl(mOriginalUrl);
            	
            	// Access live version if date is today or in the future
            	if (mToday.compareTo(mDateChosen) <= 0) {
            		Log.d(LOG_TAG, "Browsing to " + mOriginalUrl);
            		mWebview.loadUrl(mOriginalUrl);
            		
            		// Hide the virtual keyboard
	            	((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE))  
	                	.hideSoftInputFromWindow(mLocation.getWindowToken(), 0); 
            	}
            	else {
            		makeMementoRequests();
            	}
            }
        });
                
        
        mNextButton = (Button) findViewById(R.id.next);
        mNextButton.setEnabled(false);
        mNextButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	// Advance to next Memento
            	
            	// This could happen if the index has not been set yet
            	if (mMementos.getCurrentIndex() < 0) {
            		int index = mMementos.getIndex(mDateDisplayed);
            		if (index < 0) {
            			Log.d(LOG_TAG, "Could not find next Memento after date " + mDateDisplayed);
            			return;
            		}
            		else 
            			mMementos.setCurrentIndex(index);            		
            	}
            	            	           	
            	// Locate the next Memento in the list
            	Memento nextMemento = mMementos.getNext();
            	            	
            	if (nextMemento == null) {
            		Log.d(LOG_TAG, "Still could not find next Memento!");
            		Log.d(LOG_TAG, "Current index is " + mMementos.getCurrentIndex()); 
            	}
            	else {
            		SimpleDateTime date = nextMemento.getDateTime();
            		setChosenDate(nextMemento.getDateTime());
            		showToast("Time travelling to next Memento on " + mDateChosen.dateFormatted());
            		
            		mDateDisplayed = date;
            		
					String redirectUrl = nextMemento.getUrl();
					Log.d(LOG_TAG, "Sending browser to " + redirectUrl);
					mWebview.loadUrl(redirectUrl);
					
					// Just in case it wasn't already enabled
					mPreviousButton.setEnabled(true);
					
					// If this is the last memento, disable button
					if (mMementos.isLast(date))
						mNextButton.setEnabled(false);
            	}
            }
        });
        mPreviousButton = (Button) findViewById(R.id.previous);
        mPreviousButton.setEnabled(false);
        mPreviousButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	// Advance to previous Memento
            	
            	// This could happen if the index has not been set yet
            	if (mMementos.getCurrentIndex() < 0) {
	            	int index = mMementos.getIndex(mDateDisplayed);
	        		if (index < 0) {
	        			Log.d(LOG_TAG, "Could not find previous Memento before date " + mDateDisplayed);
	        			return;
	        		}
	        		else 
	        			mMementos.setCurrentIndex(index);
            	}
        		            	
            	// Locate the prev Memento in the list
            	Memento prevMemento = mMementos.getPrevious();
            	            	            	
            	if (prevMemento == null) {
            		Log.d(LOG_TAG, "Still could not find previous Memento!");
            		Log.d(LOG_TAG, "Current index is " + mMementos.getCurrentIndex()); 
            	}
            	else {
            		SimpleDateTime date = prevMemento.getDateTime();
            		setChosenDate(date);
            		showToast("Time travelling to previous Memento on " + mDateChosen.dateFormatted());
            	            		
            		mDateDisplayed = date;
            		
					String redirectUrl = prevMemento.getUrl();
					Log.d(LOG_TAG, "Sending browser to " + redirectUrl);
					mWebview.loadUrl(redirectUrl);
					
					// Just in case it wasn't already enabled
					mNextButton.setEnabled(true);
					
					// If this is the first memento, disable button
					if (mMementos.isFirst(date))
						mPreviousButton.setEnabled(false);
            	}
            }
        }); 

        mDateChosenButton = (Button) findViewById(R.id.dateChosen);
        mDateDisplayedView = (TextView) findViewById(R.id.dateDisplayed);

        // add a click listener to the button
        mDateChosenButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	Date date = new Date();
            	DateFormat dateFormat = DateFormat.getDateTimeInstance();
            	TimeZone tz = dateFormat.getTimeZone();
                System.out.println("Time: " + dateFormat.format(date));
                System.out.println("Time zone: " + tz.getDisplayName());
                
                DateFormat df = android.text.format.DateFormat.getDateFormat(getApplicationContext());
                System.out.println("Time2: " + df.format(date));
                
                showDialog(DIALOG_DATE);
                
                // Launches date & time settings               
                //startActivity(new Intent(android.provider.Settings.ACTION_DATE_SETTINGS));
                
                // Problems:
                // 1) We don't want the user to have to modify system settings
                // 2) Not all options are really needed
                
                // Solution: Build our own that is similar
                // But this is a real pain!!
                	
            }
        });

        // Set the current date
        mToday = new SimpleDateTime();        
        setChosenDate(mToday);       
        setDisplayedDate(mToday);
        
        mWebview = (WebView) findViewById(R.id.webview);
        mWebview.setWebViewClient(new MementoWebViewClient()); 
        mWebview.setWebChromeClient(new MementoWebChromClient());
        mWebview.getSettings().setJavaScriptEnabled(true);
        mWebview.loadUrl(mOriginalUrl);        
        
       
        //testMementos();
    }
    
    @Override
    public void onResume() {
    	super.onResume();
    	                
    	// Get default timegate that was selected in the settings
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        mDefaultTimegateUri = prefs.getString("defaultTimegate", mTimegateUris[0]);
        
        Log.d(LOG_TAG, "mDefaultTimegateUri = " + mDefaultTimegateUri);
    }
    
    @Override 
    public boolean onCreateOptionsMenu(Menu menu) { 
         super.onCreateOptionsMenu(menu);         	    
         MenuInflater inflater = getMenuInflater();
         inflater.inflate(R.menu.options_menu, menu);
         return true;
    } 
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        // Only enable the "List" menu if there are Memento dates to display
        MenuItem item = menu.findItem(R.id.menu_list);
        item.setEnabled(mMementos.size() != 0);
        
        // Only enable "Return to Present" if we are not viewing the present
        item = menu.findItem(R.id.menu_off);
        item.setEnabled(!mToday.equalsDate(mDateChosen));

        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.menu_off:
        	// First make sure mToday is accurate
        	mToday = new SimpleDateTime();
        	
        	// Go back to today
        	setChosenDate(mToday);
        	setDisplayedDate(mToday);
        	showToast("Returning to the present.");
        	mNextButton.setEnabled(false);
        	mPreviousButton.setEnabled(false);
        	mWebview.loadUrl(mOriginalUrl);					
        	
            return true;
        case R.id.menu_settings:
        	         						
			startActivityForResult(new Intent(this, Settings.class), 0);
			
            return true;
        case R.id.menu_list:
        	
        	// We don't want to overwhelm the user with too many choices
        	if (mMementos.size() > MAX_NUM_MEMENTOS_IN_LIST)
        		showDialog(DIALOG_MEMENTO_YEARS);
        	else
        		showDialog(DIALOG_MEMENTO_DATES);
        	
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
    	Log.d(LOG_TAG, "--onActivityResult requestCode = " + requestCode);
    	
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
			mPreviousButton.setEnabled(!mFirstMemento.getDateTime().equals(mDateDisplayed));
		else
			Log.d(LOG_TAG, "mFirstMemento is null !!");
		
		if (mLastMemento != null)
			mNextButton.setEnabled(!mLastMemento.getDateTime().equals(mDateDisplayed));
		else
			Log.d(LOG_TAG, "mLastMemento is null !!");        
    }
    
    private String fixUrl(String url) {
    	if (!url.startsWith("http://") && !url.startsWith("https://"))
    		url = "http://" + url;
    	return url;
    }
    
    private void setChosenDate(SimpleDateTime date) {
    	mDateChosen = date;
    	mDateChosenButton.setText(mDateChosen.dateFormatted());
    }   
       
    /**
     * Set the chosen (request) date button.
     * @param day
     * @param month
     * @param year
     */
    private void setChosenDate(int day, int month, int year) {
    	Log.d(LOG_TAG, "setChosenDate: " + day + ":" + month + ":" + year);
    	setChosenDate(new SimpleDateTime(day, month, year));    	
    }
    
    private void refreshChosenDate() {
    	mDateChosenButton.setText(mDateChosen.dateFormatted());
    }
    
    private void setDisplayedDate(SimpleDateTime date) {
    	mDateDisplayed = date;
    	mDateDisplayedView.setText(mDateDisplayed.dateFormatted());
    }    
    
    private void refreshDisplayedDate() {
    	mDateDisplayedView.setText(mDateDisplayed.dateFormatted());
    }
       
    
    /**
     * Start http requests on a new thread to retrieve the Mementos for the current URL. 
     */
    protected void makeMementoRequests() {

    	// Ideally we should show this, but it's hard to synchronize with the
    	// browser which is downloading pages.
    	//mProgressBar.setVisibility(View.VISIBLE);
    	
        // Fire off a thread to do some work that we shouldn't do directly in the UI thread
        Thread t = new Thread() {
            public void run() {
            	            	
            	makeHttpRequests();
            	
            	// Enable or disable Next and Previous buttons
            	mHandler.post(mUpdateNextPrev);           	
            } 
        };
        t.start();
    }

    /**
     * Ran from other threads to update the UI.  Shows a dialog box if there's an error. 
     */
    private void updateResultsInUi() {

    	// Back in the UI thread        
    	if (mErrorMessage == null) {
    					
    		// If we couldn't load the exact requested date, show the date 
    		// that's being loaded.
    		if (!mDateDisplayed.equalsDate(mDateChosen)) {
    			showToast("Closest available is " + mDateDisplayed.dateFormatted());  
    			
    			this.refreshDisplayedDate();
    		}    		
    	}
    	else
    		showDialog(DIALOG_ERROR);
    } 
    
    
    /**
     * Make http requests using the Memento protocol to obtain a Memento or list
     * of Mementos. 
     */
    private void makeHttpRequests() {
    	
    	// Contact Memento proxy with chosen Accept-Datetime:
    	// http://mementoproxy.lanl.gov/aggr/timegate/http://example.com/
    	// Accept-Datetime: Tue, 24 Jul 2001 15:45:04 GMT    	   	
        
    	HttpClient httpclient = new DefaultHttpClient();
    	
    	// Disable automatic redirect handling so we can process the 302 ourself 
    	httpclient.getParams().setParameter(ClientPNames.HANDLE_REDIRECTS, false);
 
    	String url = mDefaultTimegateUri + mOriginalUrl;
        HttpGet httpget = new HttpGet(url);
        
        // Change the request date to 23:00:00 if this is the first memento.
        // Otherwise we'll be out of range.
        
        String acceptDatetime;
        
        if (mFirstMemento != null && mFirstMemento.getDateTime().equals(mDateChosen)) {
        	Log.d(LOG_TAG, "Changing chosen time to 23:59 since datetime matches first Memento.");
        	SimpleDateTime dt = new SimpleDateTime(mDateChosen);
        	dt.setToLastHour();
        	acceptDatetime = dt.longDateFormatted();
        }
        else {
        	acceptDatetime = mDateChosen.longDateFormatted(); 
        }
        
        httpget.setHeader("Accept-Datetime", acceptDatetime);
        httpget.setHeader("User-Agent", mUserAgent);
        
        //Log.d(LOG_TAG, getHeadersAsString(response.getAllHeaders()));
        
        Log.d(LOG_TAG, "Accessing: " + httpget.getURI());
        Log.d(LOG_TAG, "Accept-Datetime: " + acceptDatetime);

        HttpResponse response = null;
		try {			
			response = httpclient.execute(httpget);
			
			Log.d(LOG_TAG, "Response code = " + response.getStatusLine());
			
			//Log.d(LOG_TAG, getHeadersAsString(response.getAllHeaders()));
		} catch (ClientProtocolException e) {
			mErrorMessage = "Unable to contact proxy server. ClientProtocolException exception.";
			Log.e(LOG_TAG, getExceptionStackTraceAsString(e));
			return;
		} catch (IOException e) {
			mErrorMessage = "Unable to contact proxy server. IOException exception.";
			Log.e(LOG_TAG, getExceptionStackTraceAsString(e));
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
			Log.d(LOG_TAG, "Pick a URL from list");			
		}
		else if (statusCode == 302) {
			// Send browser to Location URL
			// Note that the date/time of this memento is not given in the Location.
			
			Header[] headers = response.getHeaders("Location");
			if (headers.length == 0) {
				mErrorMessage = "Sorry, but there was an unexpected error that will " +
					"prevent the Memento from being displayed. Try again in 5 minutes.";
				Log.e(LOG_TAG, "Error: Location header not found in response headers.");
			}
			else {
				String redirectUrl = headers[0].getValue();
				
				// Find out the datetime of this resource
				/*SimpleDateTime d = getResourceDatetime(redirectUrl);
				if (d != null)
					mDateDisplayed = d;
					*/
				
				Log.d(LOG_TAG, "Sending browser to " + redirectUrl);
				mWebview.loadUrl(redirectUrl);				
									
				// We can't update the view directly since we're running
				// in a thread, so use mUpdateResults to show a toast message
				// if accessing a different date than what was requested.
				
				//mHandler.post(mUpdateResults);
				
				// Parse various Links
				headers = response.getHeaders("Link");
				if (headers.length == 0) {
					Log.e(LOG_TAG, "Error: Link header not found in response headers.");
					mErrorMessage = "Sorry, but the Memento could not be accessed. Try again in 5 minutes.";
				}
				else {
					String linkValue = headers[0].getValue();
											
					mTimeMap = null;
			    	mTimeBundle = null;
			    	
			    	// Get the datetime of this mememnto which should be supplied in the
			    	// Link: headers
			    	mDateDisplayed = parseCsvLinks(linkValue);
					
					// Now that we know the date, update the UI to reflect it
					mHandler.post(mUpdateResults);
			    	
					if (mTimeMap != null)
						if (!accessTimeMap())
							mErrorMessage = "There were problems accessing the Memento's TimeMap.";
				}
			}
		}		
		else if (statusCode == 404) {
			mErrorMessage = "Sorry, but there are no Mementos for this URL.";
		}
		else if (statusCode == 406) {
														
			// Parse various Links
			Header[] headers = response.getHeaders("Link");
			
			if (headers.length == 0) {
				Log.d(LOG_TAG, "Error: Link header not found in 406 response headers.");
				//mErrorMessage = "Sorry, but there was an error in retreiving this Memento.";
				
				// The lanl proxy has it wrong.  It should return 404 when the URL is not
				// present, so we'll just pretend this is a 404.
				mErrorMessage = "Sorry, but there are no Mementos for this URL.";
				
				//Log.d(LOG_TAG, "BODY: " + EntityUtils.toString(response.getEntity());							
			}
			else {
				String linkValue = headers[0].getValue();
				
				mTimeMap = null;
		    	mTimeBundle = null;
		    	
				parseCsvLinks(linkValue);		
		    	
				if (mTimeMap != null)
					accessTimeMap();
				
				if (mFirstMemento == null || mLastMemento == null) {
					Log.e(LOG_TAG, "Could not find first or last Memento in 406 response for " + url);
					mErrorMessage = "Sorry, but there was an error in retreiving this Memento.";
				}
				else {			
					Log.d(LOG_TAG, "Not available in this date range (" + mFirstMemento.getDateTimeSimple() +
							" to " + mLastMemento.getDateTimeSimple() + ")");
					
					// According to Rob Sanderson (LANL), we will only get 406 when the date is too
					// early, so redirect to first Memento
										
					mDateDisplayed = new SimpleDateTime(mFirstMemento.getDateTime());
					String redirectUrl = mFirstMemento.getUrl();
					Log.d(LOG_TAG, "Sending browser to " + redirectUrl);
					mWebview.loadUrl(redirectUrl);	
					
					mHandler.post(mUpdateResults);
				}
			}
		}
		else {
			mErrorMessage = "Sorry, but there was an unexpected error that will " +
				"prevent the Memento from being displayed. Try again in 5 minutes.";
			Log.e(LOG_TAG, "Unexpected response code in makeHttpRequests = " + statusCode);
		}               
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
     * @return The datetime of the last item marked rel="memento"
     */
    public SimpleDateTime parseCsvLinks(String links) {
    	mMementos.clear();    	
    	mFirstMemento = null;
    	mLastMemento = null;
    	
    	SimpleDateTime date = null;
    	
    	// Use a temporary list instead of the actual mMemento list so that we don't 
    	// show a list of available dates until they have all been parsed.
    	MementoList tempList = new MementoList();
    	
    	Log.d(LOG_TAG, "Start parsing links");
		String[] linkStrings = links.split("\",");
				
    	// Place all Links into the array and then sort it based on date
    	for (String linkStr : linkStrings) {
			    		
			// Add back "
			if (!linkStr.endsWith("\""))
				linkStr += "\"";
			
			//Log.d(LOG_TAG, linkStr);	
			
			linkStr = linkStr.trim();
			
			Link link = new Link(linkStr);
			String rel = link.getRel();
			if (rel.contains("memento")) {
				Memento m = new Memento(link);
				tempList.add(m);
				
				//Log.d(LOG_TAG, "Added memento " + m.toString());
				
				// Peel out all values in rel which are separated by white space
				String[] items = link.getRelArray();
				for (String r : items) {						
					r = r.toLowerCase();
					
					//Log.d(LOG_TAG, "Processing rel [" + r + "]");
					
					// Change the Showing date to the memento's date
					//if (link.mRel.equals("first-memento"))
					if (r.contains("first")) {
						mFirstMemento = m;
					}
					if (r.contains("last")) {
						mLastMemento = m;
					}
					if (r.equals("memento")) {
						date = link.getDatetime();
					}
				}					
			}
			else if (rel.equals("timemap")) {
				mTimeMap = new TimeMap(link);
			}
			else if (rel.equals("timebundle")) {
				mTimeBundle = new TimeBundle(link);
			}
		}
    	
    	    	
    	// Sorting can take a time.  Since the Lanl proxy already sorts them, let's
    	// comment this out for now.
    	//Log.d(LOG_TAG, "Starting sort...");
		//Collections.sort(tempList);
    	
		Log.d(LOG_TAG, "Finished parsing links");
		
		synchronized (mMementos) {
			mMementos = tempList;
		}
				
		if (date != null)
			Log.d(LOG_TAG, "parseCsvLinks returning " + date.toString());
		else
			Log.d(LOG_TAG, "parseCsvLinks returning null");
		
		return date;
    }
        
    /**
     * Callback when the user sets the date
     */
    private DatePickerDialog.OnDateSetListener mDateSetListener =
            new DatePickerDialog.OnDateSetListener() {

                public void onDateSet(DatePicker view, int year, 
                                      int monthOfYear, int dayOfMonth) {
                	setChosenDate(dayOfMonth, monthOfYear + 1, year);
                	                	
                	if (mToday.equals(mDateChosen)) {
                		showToast("Returning to the present.");
                		setDisplayedDate(mDateChosen);
                		mWebview.loadUrl(mOriginalUrl);
                	}
                	else if (mToday.compareTo(mDateChosen) < 0) {
                		showToast("We can't see the future.\nHow about the present?");
                		
                		setDisplayedDate(mToday);
                		mWebview.loadUrl(mOriginalUrl);
                	}
                	else {
                		showToast("Time travelling to " + mDateChosen.dateFormatted());                	
                		makeMementoRequests();
                	}
                }
            };
    
    /**
     * Display toast message.
     * @param message To display
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
     * Change IA URLs back to their original.
     * 
     * Example of IA URLs: 
     * 
     * http://www.foo.org.wstub.archive.org/links.html
     * http://web.archive.org/web/20071222090517/http://www.foo.org/
     * http://web.archive.org/web/20070127071850rn_1/www.harding.edu/USER/fmccown/WWW/
     */
    private String convertIaUrlBack(String iaUrl) {
    	String url = iaUrl;
    	
    	url = url.replace(".wstub.archive.org", "");
    	
    	String pattern = "^http://web.archive.org/.+\\d{14}.*?/";
    	
		// Create a Pattern object
		Pattern r = Pattern.compile(pattern);

		// Now create matcher object.
		Matcher m = r.matcher(iaUrl);
		if (m.find()) {
			System.out.println("Found value: " + m.group(0));
			url = m.replaceFirst("");
		}
		
		if (!url.startsWith("http://"))
			url = "http://" + url;
		
		return url;
    }
    
    /**
     * This is used to get the favicon from the web page, but it is not working...
     * the onReceivedIcon() method is never called.
     *
     */
    private class MementoWebChromClient extends WebChromeClient {
    	
    	@Override
    	public void onReceivedIcon(WebView view, Bitmap icon) {
    		Log.d(LOG_TAG, "onReceivedIcon icon = " + icon.toString());
    	}
    }
    
    /**
     * Callbacks for state changes in the WebView.
     *
     */
    private class MementoWebViewClient extends WebViewClient {
    	
    	// Note: this method is *not* called when calling WebView's loadUrl().
    	
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
        	Log.d(LOG_TAG, "-- shouldOverrideUrlLoading");
                	
        	// Fix partial URLs
        	if (!url.startsWith("http://") && !url.startsWith("https://"))
        		url = "http://" + url;
        	
        	Log.d(LOG_TAG, "Click on link " + url); 
        	        	
        	// Only time travel if selected date is in the past!
        	if (mToday.compareTo(mDateChosen) <= 0) {
        		view.loadUrl(url);
        		
        		Log.d(LOG_TAG, "mOriginalUrl = " + mOriginalUrl); 
        		
        		//if (!mMementos.getAssociatedUrl().equals(url)) {
        		//	Log.d(LOG_TAG, "(1) Clearing all Mementos for new URL " + url);
        		//	mMementos.clear();
        		//}
        		
        		if (!mOriginalUrl.equals(url)) {
        			Log.d(LOG_TAG, "(2) Clearing all Mementos for new URL " + url);
        			mMementos.clear();
        		}
        			
        		mOriginalUrl = url;
        	}
        	else {        				
        		// User has clicked on a URL in an archived page, so we need the original
        		// URL so we can find all its mementos
        		url = convertIaUrlBack(url);        	
        	
        		mOriginalUrl = url;
        		Log.d(LOG_TAG, "mOriginalUrl = " + mOriginalUrl); 
        		makeMementoRequests();
        	}        	          
    		
            return true;
        }
        
        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
        	//displayError(description);
        	Log.d(LOG_TAG, "WebViewClient Error: [code=" + errorCode + "] " + description +
        			" [URL=" + failingUrl + "]");
        }
        
        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
        	Log.d(LOG_TAG, "-- onPageStarted");
        	
        	mProgressBar.setVisibility(View.VISIBLE);

        	mLocation.setText(url);
        	
        	// Show date here because it can take a long time before it finishes downloading
        	Log.d(LOG_TAG, "mDateDisplayed: " + mDateDisplayed.dateFormatted());
        	mDateDisplayedView.setText(mDateDisplayed.dateFormatted());
        	
        	/* THIS WAS A HACK TO GET THE FAVICON, BUT I DO NOT SUGGEST USING IT
        	 * SINCE THERE ARE MANY DIFFERENT METHODS USED TO PUBLISH FAVICONS, AND
        	 * THIS METHOD IS NOT USING CACHING.
        	 * 
        	// Grab website favicon
        	Context context = view.getContext();
            Drawable image = getImage(context, getBaseUrl(url) + "favicon.ico");
            if (image == null) {
            	System.out.println("image is null !!");
            }
            else {
    	        mLocation.setCompoundDrawablesWithIntrinsicBounds(image, null, null, null);
            }
            */
        	
        	if (favicon == null) {
        		// Display a favicon for some of the web archives
        		
        		Log.d(LOG_TAG, "No favicon - null");
        		
        		// Use our built-in favicons since this isn't working
        		if (url.startsWith("http://webcitation.org")) 
        			favicon = mFavicons.get("webcite");      
        		else if (url.startsWith("http://web.archive.org")) 
        			favicon = mFavicons.get("ia");
        		else if (url.startsWith("http://webarchive.nationalarchives")) 
        			favicon = mFavicons.get("national-archives");
        		else
        			mLocation.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);  
        		
        		if (favicon != null) {
        			BitmapDrawable bd = new BitmapDrawable(favicon);        	
            		mLocation.setCompoundDrawablesWithIntrinsicBounds(bd, null, null, null);  
        		}
        			
        	}
        	else {
        		Log.d(LOG_TAG, "favicon h and w = " + favicon.getHeight() + " " + favicon.getWidth());
        		BitmapDrawable bd = new BitmapDrawable(favicon);        	
        		mLocation.setCompoundDrawablesWithIntrinsicBounds(bd, null, null, null);        		
        	}
        	
        	/*  THIS CODE IS NOT WORKING EITHER
        	favicon = view.getFavicon();
        	if (favicon == null) {
        		Log.d(LOG_TAG, "No favicon from getFavicon - null");
        	}
        	else {
        		Log.d(LOG_TAG, "getFavicon favicon h and w = " + favicon.getHeight() + " " + favicon.getWidth());
        		BitmapDrawable bd = new BitmapDrawable(favicon);        	
        		mLocation.setCompoundDrawablesWithIntrinsicBounds(bd, null, null, null);        		
        	}
        	*/
        }
        
        @Override
        public void onPageFinished(WebView view, String url) {
        	mProgressBar.setVisibility(View.GONE);
        	
        	//if (mLocation.isInEditMode()) {
        	if (mLocation.isSelected())
        		Log.d(LOG_TAG, "Editor is in edit mode, so don't erase text!");        	
        	else
        		mLocation.setText(url);
        	
        	Log.d(LOG_TAG, "-- onPageFinished... mDateDisplayed: " + mDateDisplayed.dateFormatted());
        	
        	/*
        	 * THIS CODE IS NOT WORKING EITHER.
        	Bitmap favicon = view.getFavicon();
        	if (favicon == null) {
        		Log.d(LOG_TAG, "No favicon in onPageFinished - null");
        	}
        	else {
        		Log.d(LOG_TAG, "onPageFinished favicon h and w = " + favicon.getHeight() + " " + favicon.getWidth());
        		BitmapDrawable bd = new BitmapDrawable(favicon);        	
        		mLocation.setCompoundDrawablesWithIntrinsicBounds(bd, null, null, null);        		
        	}
        	*/
        }
    }
    
    @Override
    protected Dialog onCreateDialog(int id) {
    	
    	Dialog dialog = null;
    	AlertDialog.Builder builder = new AlertDialog.Builder(this);
    	
        switch (id) {
        case DIALOG_DATE:
        	dialog = new DatePickerDialog(this, mDateSetListener,
                        mDateChosen.getYear(), mDateChosen.getMonth() - 1, mDateChosen.getDay());
        	break;
        	
        case DIALOG_ERROR:
        	builder = new AlertDialog.Builder(this);
        	builder.setMessage("error message")
	       		.setCancelable(false)
	       		.setPositiveButton("OK", null);
        	dialog = builder.create();     
        	break;    
        	
        case DIALOG_MEMENTO_YEARS:
        	builder = new AlertDialog.Builder(this);
        	final CharSequence[] years = mMementos.getAllYears();
        	
        	// Select the year of the Memento currently displayed
        	int selectedYear = -1;
        	
        	for (int i = 0; i < years.length; i++) {
        		if (mDateDisplayed.getYear() == Integer.parseInt(years[i].toString())) {
        			selectedYear = i;
        			break;
        		}
        	}
        		
        	builder.setSingleChoiceItems(years, selectedYear, new DialogInterface.OnClickListener() {
        	    public void onClick(DialogInterface dialog, int item) {
        	    	dialog.dismiss();
        	    	
        	    	mSelectedYear = Integer.parseInt(years[item].toString());
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
        	
        case DIALOG_MEMENTO_DATES:        	
        	builder = new AlertDialog.Builder(this);
        	
        	final CharSequence[] dates;
        	
        	if (mSelectedYear == 0)
        		dates = mMementos.getAllDates();
        	else
        		dates = mMementos.getDatesForYear(mSelectedYear);
        	
        	Log.d(LOG_TAG, "Number of dates = " + dates.length);
        	
        	int selected = -1;
        	
        	// Select the radio button for the current Memento if it's in the selected year.
        	if (mSelectedYear == 0 || mSelectedYear == mDateDisplayed.getYear()) {
        		
        		int index = mMementos.getIndex(mDateDisplayed);
        		if (index < 0) 
        			Log.d(LOG_TAG, "Could not find Memento in the list with date " + mDateDisplayed);
        		else 
        			mMementos.setCurrentIndex(index);
        		
        		Memento m = mMementos.getCurrent();
        		if (m != null) {
        			for (int i = 0; i < dates.length; i++) {
        				if (m.getDateTime().dateAndTimeFormatted().equals(dates[i])) {	        				
	        				selected = i;
	        				break;
	        			}
	        		}
        		}
        		else
        			Log.w(LOG_TAG, "Memento is null!");
        	}
        	
        	Log.d(LOG_TAG, "Selected index = " + selected);
        	        	
        	builder.setSingleChoiceItems(dates, selected, new DialogInterface.OnClickListener() {
        	    public void onClick(DialogInterface dialog, int item) {
        	    	dialog.dismiss();
        	    	        	
        	    	int index = mMementos.getIndex(dates[item].toString());
        	    	Memento m = mMementos.get(index);
        	    	if (m == null) {
            			Log.e(LOG_TAG, "Could not find Memento with date " + mDateChosen + ".");
            			displayError("The date selected could not be accessed. Please select another.");
        	    	}
        	    	else {
	        	    	// Display this Memento
        	    		
        	    		Log.d(LOG_TAG, "index for [" + dates[item] + "] is " + index);
        	    		        	    		
	                	SimpleDateTime d = m.getDateTime();
	                	setChosenDate(d);
	                	
	                	if (index == mMementos.getCurrentIndex()) {
	                		showToast("Memento is already displayed.");
	                	}
	                	else {
	                		mMementos.setCurrentIndex(index);
	                		showToast("Time travelling to " + mDateChosen.dateFormatted());
	                	   	
	                	   	// Find the Memento URL for the selected date                		
	                		
    	            	   	mDateDisplayed = new SimpleDateTime(mDateChosen);
    						String redirectUrl = m.getUrl();
    						Log.d(LOG_TAG, "Sending browser to " + redirectUrl);
    						mWebview.loadUrl(redirectUrl);
    						
    						setEnableForNextPrevButtons();	                    	
	                	}
        	    	}  	           	    	
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
        	
        case DIALOG_HELP:        	
        	  
        	Context context = getApplicationContext();            	
        	LayoutInflater inflater = (LayoutInflater) context.getSystemService(LAYOUT_INFLATER_SERVICE);
        	View layout = inflater.inflate(R.layout.about_dialog, null);         	
			builder.setView(layout);
			builder.setPositiveButton("OK", null);	
			dialog = builder.create(); 
        	break;
        }
        	
        return dialog;
    }
    
    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {
        super.onPrepareDialog(id, dialog);

        switch (id) {
        case DIALOG_DATE:        	
        	// To fix a bug described here: http://www.zunisoft.com/?p=1140 
        	DatePickerDialog dlg = (DatePickerDialog) dialog;
        	dlg.setTitle(longDateFormat.format(mDateChosen.getDate()));        	
        	dlg.updateDate(mDateChosen.getYear(), mDateChosen.getMonth() - 1, 
        			mDateChosen.getDay());
        	break;
        case DIALOG_ERROR:        		
    		AlertDialog ad = (AlertDialog) dialog;
    		ad.setMessage(mErrorMessage);
    		mErrorMessage = null;
            break;            
        }
    }
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK) && mWebview.canGoBack()) {
        	
        	// Get the previous URL to update our internal copy 
        	
        	WebBackForwardList list = mWebview.copyBackForwardList();
        	int curr = list.getCurrentIndex();
        	WebHistoryItem item = list.getItemAtIndex(curr - 1);
        	Bitmap favicon = item.getFavicon();
        	
        	if (favicon == null)
        		Log.d(LOG_TAG, "No favicon in WebHistoryItem - null");
        	else
        		Log.d(LOG_TAG, "WebHistoryItem favicon W = " + favicon.getWidth());
        	
        	mOriginalUrl = item.getUrl();
        	Log.d(LOG_TAG, "GO BACK TO " + mOriginalUrl); 
        	
            mWebview.goBack();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
            
    
    /**
     * Retrieve the TimeMap from the Web and parse out the Mementos.
     * Currently this only recognizes TimeMaps using CSV formats. 
     * Other formats to be implemented: RDF/XML, N3, and HTML.
     * @return true if TimeMap was successfully retreived, false otherwise.
     */
    private boolean accessTimeMap() {    	   	
        
    	HttpClient httpclient = new DefaultHttpClient();
    	
    	String url = mTimeMap.getUrl();
        HttpGet httpget = new HttpGet(url);
        httpget.setHeader("User-Agent", mUserAgent);
                
        Log.d(LOG_TAG, "Accessing TimeMap: " + httpget.getURI());

        HttpResponse response = null;
		try {			
			response = httpclient.execute(httpget);
			
			Log.d(LOG_TAG, "Response code = " + response.getStatusLine());
			
			//Log.d(LOG_TAG, getHeadersAsString(response.getAllHeaders()));
		} catch (ClientProtocolException e) {
			Log.e(LOG_TAG, getExceptionStackTraceAsString(e));
			return false;
		} catch (IOException e) {
			Log.e(LOG_TAG, getExceptionStackTraceAsString(e));
			return false;
		}                
        
        // Should get back 200
		
		int statusCode = response.getStatusLine().getStatusCode(); 
		if (statusCode == 200) {
			
			// See if MIME type is the same as Type		
			Header type = response.getFirstHeader("Content-Type");
			if (type == null)
				Log.w(LOG_TAG, "Could not find the Content-Type for " + url);
			else if (!type.getValue().contains(mTimeMap.getType()))
				Log.w(LOG_TAG, "Content-Type is [" + type.getValue() + "] but TimeMap type is [" +
						mTimeMap.getType() + "] for " + url);
			
			// Timemap MUST be "application/link-format", but leave csv for
			// backwards-compatibility with earlier Memento implementations
			if (mTimeMap.getType().equals("text/csv") ||
				mTimeMap.getType().equals("application/link-format")) {
				try {
					String responseBody = EntityUtils.toString(response.getEntity());
					parseCsvLinks(responseBody);
				} catch (ParseException e) {
					Log.e(LOG_TAG, getExceptionStackTraceAsString(e));
					return false;
				} catch (IOException e) {
					Log.e(LOG_TAG, getExceptionStackTraceAsString(e));
					return false;
				}
			}
			else {
				Log.e(LOG_TAG, "Unable to handle TimeMap type " + mTimeMap.getType());
				return false;
			}
		}		
		else {
			Log.d(LOG_TAG, "Unexpected response code in accessTimeMap = " + statusCode);
			return false;
		}        
        
		// Deallocate all system resources
        httpclient.getConnectionManager().shutdown();
        
        return true;
    }
    
    public static String getExceptionStackTraceAsString(Exception exception) {
    	StringWriter sw = new StringWriter();
    	exception.printStackTrace(new PrintWriter(sw));
    	return sw.toString();
    }
    
    /**
     * Purely for testing.
     */
    @SuppressWarnings("unused")
	private void testMementos() {
    	
    	String[] urls = {
    			"http://www.foo.org.wstub.archive.org/links.html",
    			"http://web.archive.org/web/20071222090517/http://www.foo.org/",
    			"http://web.archive.org/web/20070127071850rn_1/www.harding.edu/USER/fmccown/WWW/"
    	};
    	
    	for (String u : urls) {
    		System.out.println("Convert " + u + " to " + convertIaUrlBack(u));
    	}
    	    		
    	SimpleDateTime date = new SimpleDateTime();
    	System.out.println("date = " + date);
    	
    	SimpleDateTime date2 = new SimpleDateTime();
    	System.out.println("date2 = " + date2);
    	
    	int comp = date.compareTo(date2);
    	System.out.println("compareTo = " + comp);
    	
    	date = new SimpleDateTime(31, 12, 2010);
    	date.setDateFormat(android.text.format.DateFormat.getDateFormat(getApplicationContext()));
    	date.setTimeFormat(android.text.format.DateFormat.getTimeFormat(getApplicationContext()));
    	System.out.println("date formatted = " + date.dateFormatted());
    	System.out.println("date and time formatted = " + date.dateAndTimeFormatted());
    	System.out.println("long formatted = " + date.longDateFormatted());
    	
    	
    	//System.exit(0);
    	//this.finish();
    	
    	String url = "<http://web.archive.org/web/20010910203350/www.harding.edu/fmccown/>;rel=\"memento\";datetime=\"Mon, 10 Sep 2001 20:33:50 GMT\"";
    	Memento m1 = new Memento(new Link(url));
    	System.out.println("m1=" + m1.toString());
    	System.out.println("getDateTimeString: " + m1.getDateTimeString());
    	System.out.println("getDateTimeSimple: " + m1.getDateTimeSimple());
    	
    	Memento m2 = new Memento(new Link(url));
    	
    	System.out.println("m2=" + m2.toString());
    	System.out.println("getDateTimeString: " + m2.getDateTimeString());
    	System.out.println("getDateTimeSimple: " + m2.getDateTimeSimple());
    	
    	System.out.println("\ncompare m1,m2: " + m1.compareTo(m2));
    	System.out.println("\ncompare m2,m1: " + m2.compareTo(m1));
    	
    	String newDatetime = "Sun, 09 Sep 2001 20:33:50 GMT";
    	m2.setDateTime(newDatetime);
    	
    	System.out.println("getDateTimeString: " + m2.getDateTimeString());
    	System.out.println("getDateTimeSimple: " + m2.getDateTimeSimple());
    	
    	System.out.println("\ncompare m1,m2: " + m1.compareTo(m2));
    	System.out.println("\ncompare m2,m1: " + m2.compareTo(m1));
    	
    	m2.getDateTime().setToLastHour();
    	
    	System.out.println("New value for getDateTimeString: " + m2.getDateTimeString());
    	
    	SimpleDateTime d1 = m1.getDateTime();
    	SimpleDateTime d2 = SimpleDateTime.parseShortDate("09-09-2001");
    	System.out.println("Comparing " + d1.toString() + " with " + d2.toString());
    	if (d1.equals(d2))
    		System.out.println("Same date.");
    	else
    		System.out.println("Not equal dates.");

    	d2 = SimpleDateTime.parseShortDate("9-10-2001");
    	System.out.println("Comparing " + d1.toString() + " with " + d2.toString());
    	if (d1.equals(d2))
    		System.out.println("Same date.");
    	else
    		System.out.println("Not equal dates.");
    	
    	    	
    	String links = 
    		"<http://mementoproxy.lanl.gov/aggr/timebundle/http://www.harding.edu/fmccown/>;rel=\"timebundle\"," +
    		"<http://www.harding.edu/fmccown/>;rel=\"original\",<http://mementoproxy.lanl.gov/aggr/timemap/link/http://www.harding.edu/fmccown/>;rel=\"timemap\";type=\"text/csv\"," +
    		"<http://web.archive.org/web/20010724154504/www.harding.edu/fmccown/>;rel=\"first prev memento\";datetime=\"Tue, 24 Jul 2001 15:45:04 GMT\"," +
    		"<http://web.archive.org/web/20071222090517/www.harding.edu/fmccown/>;rel=\"last memento\";datetime=\"Sat, 22 Dec 2007 09:05:17 GMT\"," +
    		"<http://web.archive.org/web/20020104194811/www.harding.edu/fmccown/>;rel=\"next memento\";datetime=\"Fri, 04 Jan 2002 19:48:11 GMT\"," +
    		"<http://web.archive.org/web/20010910203350/www.harding.edu/fmccown/>;rel=\"memento\";datetime=\"Mon, 10 Sep 2001 20:33:50 GMT\"," + 
    		"<http://webcache.googleusercontent.com/search?q=cache:http://www.digitalpreservation.gov/>;rel=\"first last memento\";datetime=\"Tue, 07 Sep 2010 11:54:29 GMT\"";
    	
    	parseCsvLinks(links);
    	mMementos.displayAll();
    	
    	System.exit(0);
    	
    	System.out.println(mTimeMap.toString());
    	System.out.println(mTimeBundle.toString());
    	
    	System.out.println("\nAll years:");
    	for (CharSequence year : mMementos.getAllYears()) {
    		System.out.println(year);
    	}
    	
    	System.out.println("\nAll for 2001:");
    	for (CharSequence year : mMementos.getDatesForYear(2001)) {
    		System.out.println(year);
    	}
    	

    	System.out.println("\nAll for 2000:");
    	for (CharSequence year : mMementos.getDatesForYear(2000)) {
    		System.out.println(year);
    	}
    	
    	//date = getResourceDatetimeForWebcite("http://webcitation.org/query?id=1218127693715930");
    	//System.out.println("Date returned from getResourceDatetimeForWebcite: " + date.toString());
    	
    	//accessTimeMap();
    }   
}
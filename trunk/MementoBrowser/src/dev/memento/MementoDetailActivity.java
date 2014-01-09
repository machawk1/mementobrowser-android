package dev.memento;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;

public class MementoDetailActivity extends Activity {

	public final static String LOG_TAG = MainActivity.LOG_TAG;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	    setContentView(R.layout.activity_memento_detail);
	    
	    getActionBar().setDisplayHomeAsUpEnabled(true);
		
		// Get the details from the activity that started me
	    Intent intent = getIntent();
	    String orignialUrl = intent.getStringExtra(MainActivity.EXTRA_ORIGINAL_URL);
	    
	    String mementoUrl = intent.getStringExtra(MainActivity.EXTRA_MEMENTO_URL);
	    String mementoDate = intent.getStringExtra(MainActivity.EXTRA_MEMENTO_DATE);
	    
	    TextView orignialUrlView = (TextView) findViewById(R.id.originalUrl); 
	    orignialUrlView.setText(orignialUrl);
	    
	    TextView mementoUrlView = (TextView) findViewById(R.id.mementoUrl); 
	    mementoUrlView.setText(mementoUrl);
	    
	    TextView mementoDateView = (TextView) findViewById(R.id.mementoDate); 
	    mementoDateView.setText(mementoDate);
	    
	    TextView archiveNameView = (TextView) findViewById(R.id.archiveName);
	    TextView archiveUrlView = (TextView) findViewById(R.id.archiveUrl);
	    
	    // Figure out archive based on URL
	    if (mementoUrl.startsWith("http://api.wayback.archive.org") ||
	    		mementoUrl.startsWith("http://web.archive.org")) {
	    	archiveNameView.setText("Internet Archive");
	    	archiveUrlView.setText("http://archive.org/");
	    }
	    else if (mementoUrl.startsWith("http://webcitation.org")) {
	    	archiveNameView.setText("WebCite");
	    	archiveUrlView.setText("http://webcitation.org/");
	    }
	    else if (mementoUrl.startsWith("http://webarchive.nationalarchives")) {
	    	archiveNameView.setText("UK National Archives");
	    	archiveUrlView.setText("http://www.nationalarchives.gov.uk/webarchive/");
	    }
	    else if (mementoUrl.startsWith("http://wayback.archive-it.org")) {
	    	archiveNameView.setText("Archive-It");
	    	archiveUrlView.setText("http://www.archive-it.org/");
	    }
	    else if (mementoUrl.startsWith("http://webarchive.loc.gov")) {
	    	archiveNameView.setText("Library of Congress Web Archives");
	    	archiveUrlView.setText("http://webarchive.loc.gov/");
	    }
	    else if (mementoUrl.startsWith("http://en.wikipedia.org")) {
	    	archiveNameView.setText("Wikipedia");
	    	archiveUrlView.setText("http://en.wikipedia.org/");
	    }
	    else {
	    	archiveNameView.setText("Other");
	    	archiveUrlView.setText("");
	    }

	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
}

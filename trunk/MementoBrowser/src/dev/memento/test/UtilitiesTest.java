package dev.memento.test;

import dev.memento.Utilities;
import junit.framework.TestCase;

public class UtilitiesTest extends TestCase {

	protected static void setUpBeforeClass() throws Exception {
	}

	protected void setUp() throws Exception {
		super.setUp();
	}

	/*
	public void testGetBaseUrl() {
		fail("Not yet implemented");
	}*/

	public void testGetUrlFromArchiveUrl() {
		String[] urls = {
				"http://www.foo.org/not_a_ia_url.html",
				"http://web.archive.org/web/20071222090517/http://www.blah.org/",
				"http://web.archive.org/web/20070127071850rn_1/www.harding.edu/USER/fmccown/WWW/",
				"http://api.wayback.archive.org/memento/20071222090517/http://www.foo.org/",
				"http://wayback.archive-it.org/all/20120117015404/http://www.cnn.com/",
				"http://web.archive.org/web/20010410213930/http://www.cnn.com/cnnfn/2001/04/10/markets/markets_newyork/?s=2"
	    	};
			
			String[] correctUrls = {
				"http://www.foo.org/not_a_ia_url.html",
				"http://www.blah.org/",
				"http://www.harding.edu/USER/fmccown/WWW/",
				"http://www.foo.org/",
				"http://www.cnn.com/",
				"http://www.cnn.com/cnnfn/2001/04/10/markets/markets_newyork/?s=2"
			};
	    			
	    	for (int i = 0; i < urls.length; i++) {
	    		String expected = correctUrls[i];
	    		String actual = Utilities.getUrlFromArchiveUrl(urls[i]);
	    		assertEquals(expected, actual);
	    	}
	}

	public void testFixUrl() {
		String[] urls = {
				"foo.org",
				"http://www.foo.org/",
				"http://www.foo.org"
    	};
		
		String[] correctUrls = {
				"http://foo.org/",
				"http://www.foo.org/",
				"http://www.foo.org/"
		};
    			
    	for (int i = 0; i < urls.length; i++) {
    		String expected = correctUrls[i];
    		String actual = Utilities.fixUrl(urls[i]);
    		assertEquals(expected, actual);
    	}
	}

	public void testIsValidUrl() {
		boolean actual = Utilities.isValidUrl("");
		assertEquals(false, actual);
		
		/*  WORK FOR STRICTER UrlValidator
		actual = Utilities.isValidUrl("http://foo");
		assertEquals(false, actual);
		
		actual = Utilities.isValidUrl("http://foo.BLAH");
		assertEquals(false, actual);
		
		actual = Utilities.isValidUrl("http://foo.org");
		assertEquals(true, actual);
		
		actual = Utilities.isValidUrl("http:/foo.org/");
		assertEquals(false, actual);
		
		actual = Utilities.isValidUrl("http://foo.org/");
		assertEquals(true, actual);
		
		actual = Utilities.isValidUrl("http://");
		assertEquals(false, actual);
		
		actual = Utilities.isValidUrl("http://api.wayback.archive.org/memento/20071222090517/http://www.foo.org/");
		assertEquals(true, actual);
		*/
		
		actual = Utilities.isValidUrl("http://foo");
		assertEquals(true, actual);
		
		actual = Utilities.isValidUrl("http://foo.BLAH");
		assertEquals(true, actual);
		
		actual = Utilities.isValidUrl("http://foo.org");
		assertEquals(true, actual);
		
		actual = Utilities.isValidUrl("http:/foo.org/");
		assertEquals(false, actual);
		
		actual = Utilities.isValidUrl("http://foo.org/");
		assertEquals(true, actual);
		
		actual = Utilities.isValidUrl("http://");
		assertEquals(false, actual);
		
		actual = Utilities.isValidUrl("http://api.wayback.archive.org/memento/20071222090517/http://www.foo.org/");
		assertEquals(true, actual);
	
	}

}

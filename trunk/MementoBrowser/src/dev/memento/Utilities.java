package dev.memento;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormatSymbols;


/**
 * Contains numerous utility functions that may be used throughout the app.
 * 
 * @author fmccown
 *
 */
public class Utilities {
 
    /**
     * Return the base URL from the given URL.  Example:
     * http://foo.org/abc.html -> http://foo.org/
     * @param surl
     * @return The base URL.
     */
    public static String getBaseUrl(String surl) {
    	URL url;
		try {
			url = new URL(surl);
			System.out.println("getHost: " + url.getHost());
			return "http://" + url.getHost() + "/";
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
    	return null;
    }
    
    /**
     * Grab the URL from the back of an archive's URL. Returns the URL unchanged 
     * if it doesn't detect one of the archive URL patterns.
     * 
     * Example URLs: 
     * 
     * http://web.archive.org/web/20071222090517/http://www.foo.org/
     * http://web.archive.org/web/20070127071850rn_1/www.harding.edu/USER/fmccown/WWW/
     * http://api.wayback.archive.org/memento/20071222090517/http://www.foo.org/
     * http://api.wayback.archive.org/web/20071222090517/http://www.foo.org/
     * http://webarchive.nationalarchives.gov.uk/20100402191416/http://mementoweb.org/
     */
    public static String getUrlFromArchiveUrl(String archiveUrl) {
    	    	
    	String url = archiveUrl;
    	if (archiveUrl.startsWith("http://web.archive.org"))
    		url = archiveUrl.replaceFirst("^http://web.archive.org/web/\\d+.*?/", "");
    	else if (archiveUrl.startsWith("http://api.wayback.archive.org/memento"))
    		url = archiveUrl.replaceFirst("^http://api.wayback.archive.org/memento/\\d+/", "");
    	else if (archiveUrl.startsWith("http://api.wayback.archive.org/web"))
    		url = archiveUrl.replaceFirst("^http://api.wayback.archive.org/web/\\d+/", "");
    	else if (archiveUrl.startsWith("http://wayback.archive-it"))
    		url = archiveUrl.replaceFirst("^http://wayback.archive-it.org/all/\\d+/", "");
    	else if (archiveUrl.startsWith("http://webarchive.nationalarchives.gov.uk"))
    		url = archiveUrl.replaceFirst("^http://webarchive.nationalarchives.gov.uk/\\d+/", "");
				
    	if (!url.startsWith("http://") && !url.startsWith("https://"))
			url = "http://" + url;
    	
		return url;
    }
    
    /**
     * Returns true if the given URL looks like it is from one of the web archives.
     * @param url
     * @return
     */
    public static boolean isArchiveUrl(String url) {
    	return (url.startsWith("http://web.archive.org") ||
    			url.startsWith("http://api.wayback.archive") ||
    			url.startsWith("http://wayback.archive-it"));
    }
    
    
    /**
     * Make sure URL starts with http:// or https:// and has a slash
     * for the path if the path is missing.  Examples:
     * 
     * foo.org -> http://foo.org/
     * http://foo.org -> http://foo.org/
     * 
     * @param url
     * @return
     */
    public static String fixUrl(String url) {
    	
    	if (!url.startsWith("http://") && !url.startsWith("https://"))
    		url = "http://" + url;
    	
    	// Make sure there are at least three slashes (two in http://)
    	int count = 0;
        for (int i = 0; i < url.length() && count < 3; i++)
        {
            if (url.charAt(i) == '/')
                 count++;
        }
        
        if (count == 3)
        	return url;
        else
        	return url + "/";
    }
    
    /**
     * Return true if the URL appears to be syntactically valid.
     * 
     * @param url
     * @return
     */
    public static boolean isValidUrl(String url) {
    	
    	// org.apache.commons.validator.routines.UrlValidator is a little too 
    	// strict... doesn't accept Wayback URLs
    	//String[] schemes = {"http", "https"};
    	//UrlValidator urlValidator = new UrlValidator(schemes);
        //return urlValidator.isValid(url);
    	
    	// Can't believe URLUtil lets this one pass
    	if (url.equals("http://") || url.equals("https://"))
    		return false;
    	
    	// android.webkit.URLUtil.isValidUrl is a bit weak but OK
    	return android.webkit.URLUtil.isValidUrl(url);  	
    	
    }
    
    /**
     * Converts a month title (like "December") into its equivalent number (12).
     * @param month The month to convert.
     * @return The month equivalent or -1 if month does not match any known month names.
     */
    public static int monthStringToInt(String month) {
    	
    	DateFormatSymbols df = new DateFormatSymbols();
    	int i = 0;
    	for (String mon : df.getMonths()) {
    		if (mon.equalsIgnoreCase(month))
    			return i + 1;
    		i++;
    	}
    	return -1;
    }
    
    /**
     * Converts an exception to a string.
     * @param exception
     * @return
     */
    public static String getExceptionStackTraceAsString(Exception exception) {
    	StringWriter sw = new StringWriter();
    	exception.printStackTrace(new PrintWriter(sw));
    	return sw.toString();
    }
}

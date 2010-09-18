package dev.memento;

import java.net.MalformedURLException;
import java.net.URL;

import org.apache.http.Header;



public class Utilities {
 
	 /**
     * Print http headers.  Useful for debugging.
     * @param headers
     */
    public static String getHeadersAsString(Header[] headers) {
    	
    	StringBuffer s = new StringBuffer("Headers:");
    	s.append("------------");
    	for (Header h : headers) 
    		s.append(h.toString());    	
    	s.append("------------");
    	return s.toString();
    }
    
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
}

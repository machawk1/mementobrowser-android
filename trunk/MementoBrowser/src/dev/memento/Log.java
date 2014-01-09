package dev.memento;

/**
 * Custom log class allows logging calls to be easily removed from compiled code
 * by setting LOG to false.  This is necessary because Android Market apps must 
 * not contain any logging calls.
 * @author fmccown
 *
 */
public class Log {
	
	// Set to true to enable logging
    static final boolean LOG = false;

    public static void i(String tag, String string) {
        if (LOG) android.util.Log.i(tag, string);
    }
    
    public static void e(String tag, String string) {
        if (LOG) android.util.Log.e(tag, string);
    }
    
    public static void d(String tag, String string) {
        if (LOG) android.util.Log.d(tag, string);
    }
    
    public static void v(String tag, String string) {
        if (LOG) android.util.Log.v(tag, string);
    }
    
    public static void w(String tag, String string) {
        if (LOG) android.util.Log.w(tag, string);
    }
}

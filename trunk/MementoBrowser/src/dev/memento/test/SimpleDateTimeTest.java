package dev.memento.test;

import dev.memento.SimpleDateTime;
import junit.framework.TestCase;

public class SimpleDateTimeTest extends TestCase {

	protected static void setUpBeforeClass() throws Exception {
	}

	protected void setUp() throws Exception {
		super.setUp();
	}

	public void testCompareTo() {
		SimpleDateTime date1 = new SimpleDateTime();    	
    	SimpleDateTime date2 = new SimpleDateTime();
    	
    	int actual = date1.compareTo(date2);
    	int expected = 0;    	
		assertEquals(expected, actual);
		
		// Make the two dates off by an hour but the same day
		date1 = new SimpleDateTime("Sat, 22 Dec 2007 09:05:17 GMT");
		date2 = new SimpleDateTime("Sat, 22 Dec 2007 10:05:17 GMT");
		
		actual = date1.compareTo(date2);
		expected = 0;    	
		assertEquals(expected, actual);
		
		
		date1 = new SimpleDateTime("Sat, 22 Dec 2007 09:05:17 GMT");
		date2 = new SimpleDateTime("Sun, 23 Dec 2007 10:05:17 GMT");
		actual = date1.compareTo(date2);
		assertTrue(actual < 0);
		
		date1 = new SimpleDateTime("Sat, 22 Dec 2007 09:05:17 GMT");
		date2 = new SimpleDateTime("Fri, 21 Dec 2007 10:05:17 GMT");
		actual = date1.compareTo(date2);
		assertTrue(actual > 0);
	}
}

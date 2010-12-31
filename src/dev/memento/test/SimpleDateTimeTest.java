/**
 * SimpleDateTime.java
 * 
 * Copyright 2010 Frank McCown
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
 *  JUnit tests for the SimpleDateTime class.
 */

package dev.memento.test;

import static org.hamcrest.CoreMatchers.*;

import java.text.DateFormat;

import static org.junit.Assert.*;

import org.junit.Test;

import dev.memento.SimpleDateTime;

public class SimpleDateTimeTest {

	protected void setUp() {
		SimpleDateTime.mDateFormat = DateFormat.getInstance();
	}
	
	@Test
	public void testSimpleDateTime() {
		fail("Not yet implemented");
	}

	@Test
	public void testSimpleDateTimeDateFormatDateFormat() {
		fail("Not yet implemented");
	}

	@Test
	public void testSimpleDateTimeSimpleDateTime() {
		fail("Not yet implemented");
	}

	@Test
	public void testSimpleDateTimeString() {
		fail("Not yet implemented");
	}

	@Test
	public void testSimpleDateTimeIntIntInt() {
		fail("Not yet implemented");
	}

	@Test
	public void testCompareTo() {
		String dateTime = "Sun, 09 Sep 2001 20:33:50 GMT";
		String dateTime2 = "Sun, 09 Sep 2001 25:33:51 GMT";
		SimpleDateTime d1 = new SimpleDateTime(dateTime);
		SimpleDateTime d2 = new SimpleDateTime(dateTime2);

		int answer = d1.compareTo(d2);
		assertTrue(answer == 0);
		
		d2 = SimpleDateTime.parseShortDate("09-10-2001");				
		answer = d1.compareTo(d2);
		assertTrue(answer < 0);
		
		d2 = SimpleDateTime.parseShortDate("09-08-2001");
		answer = d1.compareTo(d2);
		assertTrue(answer > 0);
	}

	@Test
	public void testEqualsObject() {
		
		// equals() is comparing date and time
		
		String dateTime = "Sun, 09 Sep 2001 20:33:50 GMT";
		SimpleDateTime d1 = new SimpleDateTime(dateTime);
		
		// Will have time of 12:00 am
    	SimpleDateTime d2 = SimpleDateTime.parseShortDate("09-09-2001");
    	
    	System.out.println("Comparing " + d1.toString() + " with " + d2.toString());
    	
    	assertThat("The date/times should NOT be equal", d1, not(d2));
    	
    	d2 = new SimpleDateTime(dateTime);    	
    	assertEquals("The date/times should be equal", d1, d2);
	}

	@Test
	public void testEqualsDate() {
		
		// equalsDate() is comparing only the date, not the time
		
		String dateTime = "Sun, 09 Sep 2001 20:33:50 GMT";
		SimpleDateTime d1 = new SimpleDateTime(dateTime);
		SimpleDateTime d2 = SimpleDateTime.parseShortDate("09-09-2001");
		
		boolean answer = d1.equalsDate(d2);
		assertTrue(answer);
		
		d2 = SimpleDateTime.parseShortDate("09-10-2001");
		answer = d1.equalsDate(d2);
		assertFalse(answer);
	}

}

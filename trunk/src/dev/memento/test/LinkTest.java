package dev.memento.test;

import static org.junit.Assert.*;

import org.junit.Test;

import dev.memento.Link;
import dev.memento.SimpleDateTime;

public class LinkTest {

	private String[] linkStrings = {
			"<http://webcache.googleusercontent.com/search?q=cache:http://www.digitalpreservation.gov/>;rel=\"first-memento last-memento memento\";datetime=\"Tue, 07 Sep 2010 11:54:29 GMT\"",
			"<http://mementoproxy.lanl.gov/aggr/timebundle/http://www.harding.edu/fmccown/>;rel=\"timebundle\",",
			"<http://www.harding.edu/fmccown/>;rel=\"original\",",
			"<http://mementoproxy.lanl.gov/aggr/timemap/link/http://www.harding.edu/fmccown/>;rel=\"timemap\";type=\"text/csv\",",
			"<http://web.archive.org/web/20010724154504/www.harding.edu/fmccown/>;rel=\"first-memento prev-memento\";datetime=\"Tue, 24 Jul 2001 15:45:04 GMT\",",
			"<http://web.archive.org/web/20010910203350/www.harding.edu/fmccown/>;rel=\"memento\";datetime=\"Mon, 10 Sep 2001 20:33:50 GMT\","
	};
	

	
	@Test
	public void testLink() {
						
		Link link = new Link(linkStrings[0]);
		String url = "http://webcache.googleusercontent.com/search?q=cache:http://www.digitalpreservation.gov/";
		assertEquals(url, link.getUrl());
		
		String expectedRel = "first-memento last-memento memento";
		assertEquals(expectedRel, link.getRel());
		
		SimpleDateTime expectedDatetime = new SimpleDateTime("Tue, 07 Sep 2010 11:54:29 GMT");
		assertEquals(expectedDatetime, link.getDatetime());
	
		// timebundle
		link = new Link(linkStrings[1]);
		url = "http://mementoproxy.lanl.gov/aggr/timebundle/http://www.harding.edu/fmccown/";
		assertEquals(url, link.getUrl());
		assertEquals("timebundle", link.getRel());
		assertNull(link.getDatetime());
				
		// original
		link = new Link(linkStrings[2]);
		url = "http://www.harding.edu/fmccown/";
		assertEquals(url, link.getUrl());
		assertEquals("original", link.getRel());
		assertNull(link.getDatetime());
		assertNull(link.getType());
		
		// timemap
		link = new Link(linkStrings[3]);
		url = "http://mementoproxy.lanl.gov/aggr/timemap/link/http://www.harding.edu/fmccown/";
		assertEquals(url, link.getUrl());
		assertEquals("timemap", link.getRel());
		assertEquals("text/csv", link.getType());
		assertNull(link.getDatetime());
	}

	@Test
	public void testGetRelArray() {

		Link link = new Link(linkStrings[0]);
		
		String[] rels = link.getRelArray();
		assertEquals("first-memento", rels[0]);
		assertEquals("last-memento", rels[1]);
		assertEquals("memento", rels[2]);
	}

	

}

package dev.memento;

public class Memento implements Comparable<Memento> {
	
	private String mUrl;
	private String mRel;
	private SimpleDateTime mDatetime;
	
	
	public Memento(Link link) {
		mUrl = link.getUrl();
		mRel = link.getRel();
		mDatetime = link.getDatetime();	
	}

	public String getUrl() {
		return mUrl;
	}
	
	public void setUrl(String url) {
		this.mUrl = url;
	}
	
	public String getRel() {
		return mRel;
	}
	
	public void setRel(String rel) {
		this.mRel = rel;
	}
	
	public SimpleDateTime getDateTime() {
		return mDatetime;
	}
	
	public String getDateTimeString() {
		return mDatetime.longDateFormatted();
	}
	
	public void setDateTime(String datetime) {
		this.mDatetime = new SimpleDateTime(datetime);
	}
	
	public String getDateTimeSimple() {
		return mDatetime.dateFormatted().toString();
	}
	
	@Override
	public String toString() {
		return "Memento: url=[" + mUrl + "] rel=[" + mRel + "]" + 
			" datetime=[" + mDatetime + "]";
	}

	@Override
	public int compareTo(Memento memento) {			
		return mDatetime.compareTo(memento.mDatetime);
	}    	
}

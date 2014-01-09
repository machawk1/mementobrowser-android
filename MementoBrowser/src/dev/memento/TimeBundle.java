package dev.memento;

public class TimeBundle {

	private String mUrl;
	private String mRel;
	
	public TimeBundle(Link link) {
		mUrl = link.getUrl();
		mRel = link.getRel();
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
	
	@Override
	public String toString() {
		return "TimeBundle: url=[" + mUrl + "] rel=[" + mRel + "]";
	}
}

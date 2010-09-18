package dev.memento;

public class TimeMap {

	private String mUrl;
	private String mRel;
	private String mType;
	
	public TimeMap(Link link) {
		mUrl = link.getUrl();
		mRel = link.getRel();
		mType = link.getType();
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
	
	public String getType() {
		return mType;
	}
	
	public void setType(String type) {
		this.mType = type;
	}
	
	@Override
	public String toString() {
		return "TimeMap: url=[" + mUrl + "] rel=[" + mRel + "]" + 
			" type=[" + mType + "]";  
	}
}

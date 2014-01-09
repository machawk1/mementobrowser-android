package dev.memento;

public class TimeMap {

	private String mUrl;
	private String mRel;
	private String mType;
	private boolean mDownloaded;
	
	public TimeMap(Link link) {
		mUrl = link.getUrl();
		mRel = link.getRel();
		mType = link.getType();
		mDownloaded = false;
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
	
	public boolean isDownloaded() {
		return mDownloaded;
	}

	public void setDownloaded(boolean downloaded) {
		this.mDownloaded = downloaded;
	}

	@Override
	public String toString() {
		return "TimeMap: url=[" + mUrl + "] rel=[" + mRel + "]" + 
			" type=[" + mType + "] downloaded=[" + mDownloaded + "]";  
	}
}

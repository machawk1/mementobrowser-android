package dev.memento;


public class ExWebHistoryItem {

	
	private String mOriginalUrl;
	private Memento mMemento;
	
	@Override
	public String toString() {
		return "ExWebHistoryItem [mOriginalUrl="
				+ mOriginalUrl + ", mMemento=" + mMemento + "]";
	}

	public ExWebHistoryItem() {		
	}

	public String getOriginalUrl() {
		return mOriginalUrl;
	}

	public void setOriginalUrl(String originalUrl) {
		this.mOriginalUrl = originalUrl;
	}

	public Memento getMemento() {
		return mMemento;
	}

	public void setMemento(Memento memento) {
		this.mMemento = memento;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((mMemento == null) ? 0 : mMemento.hashCode());
		result = prime * result
				+ ((mOriginalUrl == null) ? 0 : mOriginalUrl.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ExWebHistoryItem other = (ExWebHistoryItem) obj;
		if (mMemento == null) {
			if (other.mMemento != null)
				return false;
		} else if (!mMemento.equals(other.mMemento))
			return false;
		if (mOriginalUrl == null) {
			if (other.mOriginalUrl != null)
				return false;
		} else if (!mOriginalUrl.equals(other.mOriginalUrl))
			return false;
		return true;
	}
}

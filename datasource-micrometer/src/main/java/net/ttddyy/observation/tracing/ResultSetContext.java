package net.ttddyy.observation.tracing;

/**
 * @author Tadaya Tsuyukubo
 */
public final class ResultSetContext extends DataSourceBaseContext {

	private int count;

	public void incrementCount() {
		this.count++;
	}

	public int getCount() {
		return this.count;
	}

	public void setCount(int count) {
		this.count = count;
	}
}

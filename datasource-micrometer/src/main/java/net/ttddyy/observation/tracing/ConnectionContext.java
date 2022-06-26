package net.ttddyy.observation.tracing;

import java.time.Instant;

import io.micrometer.common.lang.Nullable;

/**
 * @author Tadaya Tsuyukubo
 */
public final class ConnectionContext extends DataSourceBaseContext {
	private Instant commitAt;

	private Instant rollbackAt;

	@Nullable
	public Instant getCommitAt() {
		return this.commitAt;
	}

	public void setCommitAt(Instant commitAt) {
		this.commitAt = commitAt;
	}

	@Nullable
	public Instant getRollbackAt() {
		return this.rollbackAt;
	}

	public void setRollbackAt(Instant rollbackAt) {
		this.rollbackAt = rollbackAt;
	}
}

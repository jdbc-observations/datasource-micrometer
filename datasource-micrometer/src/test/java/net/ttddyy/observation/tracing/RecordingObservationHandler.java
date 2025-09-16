package net.ttddyy.observation.tracing;

import io.micrometer.common.lang.Nullable;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationHandler;
import io.micrometer.observation.ObservationView;
import org.assertj.core.data.Index;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RecordingObservationHandler implements ObservationHandler<Observation.Context> {

	List<ObservationOperation> operations = new ArrayList<>();

	@Override
	public boolean supportsContext(Observation.Context context) {
		return context instanceof DataSourceBaseContext;
	}

	@Override
	public void onStart(Observation.Context context) {
		this.operations.add(new ObservationOperation(OperationType.OB_START, context));
	}

	@Override
	public void onError(Observation.Context context) {
		this.operations.add(new ObservationOperation(OperationType.OB_ERROR, context));
	}

	@Override
	public void onEvent(Observation.Event event, Observation.Context context) {
		this.operations.add(new ObservationOperation(OperationType.OB_EVENT, context));
	}

	@Override
	public void onScopeOpened(Observation.Context context) {
		this.operations.add(new ObservationOperation(OperationType.SCOPE_OPENED, context));
	}

	@Override
	public void onScopeClosed(Observation.Context context) {
		this.operations.add(new ObservationOperation(OperationType.SCOPE_CLOSED, context));
	}

	@Override
	public void onScopeReset(Observation.Context context) {
		this.operations.add(new ObservationOperation(OperationType.SCOPE_RESET, context));
	}

	@Override
	public void onStop(Observation.Context context) {
		this.operations.add(new ObservationOperation(OperationType.OB_STOP, context));
	}

	public List<ObservationOperation> getOperations() {
		return this.operations;
	}

	public void clear() {
		this.operations.clear();
	}

	enum OperationType {

		OB_START, OB_ERROR, OB_STOP, OB_EVENT, SCOPE_OPENED, SCOPE_CLOSED, SCOPE_RESET

	}

	void verify(int index, OperationType operationType, String operationName, @Nullable String parentName) {
		assertThat(this.operations).satisfies((operation) -> {
			assertThat(operation.getOperationType()).isEqualTo(operationType);
			assertThat(operation.getContext().getName()).isEqualTo(operationName);
			if (parentName != null) {
				assertThat(operation.getContext().getParentObservation()).extracting(ObservationView::getContextView)
					.extracting(Observation.ContextView::getName)
					.isEqualTo(parentName);
			}
		}, Index.atIndex(index));

	}

	static class ObservationOperation {

		OperationType operationType;

		Observation.Context context;

		public ObservationOperation(OperationType operationType, Observation.Context context) {
			this.operationType = operationType;
			this.context = context;
		}

		public OperationType getOperationType() {
			return operationType;
		}

		public void setOperationType(OperationType operationType) {
			this.operationType = operationType;
		}

		public Observation.Context getContext() {
			return context;
		}

		public void setContext(Observation.Context context) {
			this.context = context;
		}

	}

}

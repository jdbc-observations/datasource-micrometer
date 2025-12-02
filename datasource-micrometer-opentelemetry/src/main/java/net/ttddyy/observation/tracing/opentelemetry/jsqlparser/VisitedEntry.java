/*
 * Copyright 2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.ttddyy.observation.tracing.opentelemetry.jsqlparser;

import java.util.Objects;

public class VisitedEntry {

	public enum ValueType {

		OPERATION, COLLECTION

	}

	private final ValueType type;

	private final String data;

	public static VisitedEntry operation(String operationName) {
		return new VisitedEntry(ValueType.OPERATION, operationName);
	}

	public static VisitedEntry collection(String collectionName) {
		return new VisitedEntry(ValueType.COLLECTION, collectionName);
	}

	VisitedEntry(ValueType type, String data) {
		this.type = type;
		this.data = data;
	}

	public ValueType getType() {
		return this.type;
	}

	public String getData() {
		return this.data;
	}

	public boolean isOperation() {
		return this.type == ValueType.OPERATION;
	}

	public boolean isCollection() {
		return this.type == ValueType.COLLECTION;
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || getClass() != o.getClass())
			return false;

		VisitedEntry that = (VisitedEntry) o;
		return this.type == that.type && Objects.equals(this.data, that.data);
	}

	@Override
	public int hashCode() {
		int result = this.type.hashCode();
		result = 31 * result + Objects.hashCode(this.data);
		return result;
	}

	@Override
	public String toString() {
		return "VisitedEntry{" + "type=" + this.type + ", data='" + this.data + "'}";
	}

}

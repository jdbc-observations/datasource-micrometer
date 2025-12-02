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

import net.sf.jsqlparser.expression.BooleanValue;
import net.sf.jsqlparser.expression.DateValue;
import net.sf.jsqlparser.expression.DoubleValue;
import net.sf.jsqlparser.expression.HexValue;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.NullValue;
import net.sf.jsqlparser.expression.SignedExpression;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.TimeValue;
import net.sf.jsqlparser.expression.TimestampValue;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.util.deparser.ExpressionDeParser;

/**
 * An {@link ExpressionDeParser} to sanitize query.
 *
 * @author Tadaya Tsuyukubo
 * @since 1.3.0
 **/
public class JSqlParserSanitizingExpressionDeParser extends ExpressionDeParser {

	@Override
	public <S> StringBuilder visit(LongValue longValue, S context) {
		return builder.append("?");
	}

	@Override
	public <S> StringBuilder visit(DoubleValue doubleValue, S context) {
		return builder.append("?");
	}

	@Override
	public <S> StringBuilder visit(StringValue stringValue, S context) {
		return builder.append("?");
	}

	@Override
	public <S> StringBuilder visit(HexValue hexValue, S context) {
		return builder.append("?");
	}

	@Override
	public <S> StringBuilder visit(BooleanValue booleanValue, S context) {
		return builder.append("?");
	}

	@Override
	public <S> StringBuilder visit(DateValue dateValue, S context) {
		return builder.append("?");
	}

	@Override
	public <S> StringBuilder visit(TimestampValue timestampValue, S context) {
		return builder.append("?");
	}

	@Override
	public <S> StringBuilder visit(TimeValue timeValue, S context) {
		return builder.append("?");
	}

	@Override
	public <S> StringBuilder visit(NullValue nullValue, S context) {
		return builder.append("?");
	}

	@Override
	public <S> StringBuilder visit(Column column, S context) {
		// TODO: handle $$ quoted values for postgres
		return super.visit(column, context);
	}

	@Override
	public <S> StringBuilder visit(SignedExpression signedExpression, S context) {
		// ignore sign('+','-', '~')
		signedExpression.getExpression().accept(this, context);
		return builder;
	}

	@Override
	public <S> StringBuilder visit(InExpression inExpression, S context) {
		// TODO: combine (?,?..) in the right expression
		return super.visit(inExpression, context);
	}

}

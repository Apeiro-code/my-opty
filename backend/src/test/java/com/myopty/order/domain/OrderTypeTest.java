package com.myopty.order.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * The order type is a customer-visible choice that the database also refuses to
 * second-guess: {@code order_type} carries a check constraint listing the values
 * it accepts. Nothing in Java ties the enum to that list, so a constant could be
 * added that the database would then reject on every insert. These tests read the
 * migration and compare it with the enum instead of trusting a comment to stay
 * true.
 */
class OrderTypeTest {

	private static final String MIGRATION = "/db/migration/V8__order_create_progressive_order_table.sql";

	@Test
	void everyOrderTypeIsAllowedByTheCheckConstraint() {
		assertThat(namesOf(OrderType.values())).isEqualTo(allowedValuesOf("chk_progressive_order_type"));
	}

	/**
	 * The same drift in the other direction is just as bad: a constraint naming a
	 * value the application can never send makes that column unusable.
	 */
	@Test
	void theCheckConstraintNamesNoOrderTypeTheApplicationCannotSend() {
		assertThat(allowedValuesOf("chk_progressive_order_type")).isNotEmpty()
			.allSatisfy(value -> assertThat(OrderType.valueOf(value)).isNotNull());
	}

	@Test
	void everyStatusIsAllowedByTheCheckConstraint() {
		assertThat(namesOf(OrderStatus.values())).isEqualTo(allowedValuesOf("chk_progressive_order_status"));
	}

	/**
	 * The workflow the module documentation describes, in order. A status that
	 * jumps or repeats would be reported as out of order, so the sequence is part
	 * of the contract rather than an implementation detail.
	 */
	@Test
	void theStatusFollowsTheDocumentedWorkflow() {
		assertThat(namesOf(OrderStatus.values())).containsExactly("PENDING_REVIEW", "APPROVED", "PROCESSING", "READY",
				"DISPATCHED");
	}

	@ParameterizedTest
	@EnumSource(value = OrderType.class, names = { "BIFOCAL", "PROGRESSIVE" })
	void multiVisionLensesRequireANearAddition(OrderType orderType) {
		assertThat(orderType.requiresNearAddition()).isTrue();
	}

	@ParameterizedTest
	@EnumSource(value = OrderType.class, names = "SINGLE_VISION")
	void singleVisionLensesNeedNoNearAddition(OrderType orderType) {
		assertThat(orderType.requiresNearAddition()).isFalse();
	}

	@ParameterizedTest
	@EnumSource(OrderType.class)
	void readsAsEnglishInAValidationMessage(OrderType orderType) {
		assertThat(orderType.label()).endsWith(" lens").doesNotContain("_").isEqualTo(orderType.name().toLowerCase()
			.replace('_', ' ') + " lens");
	}

	private static Set<String> namesOf(Enum<?>[] values) {
		return Arrays.stream(values).map(Enum::name).collect(Collectors.toCollection(LinkedHashSet::new));
	}

	/**
	 * Pulls the permitted values out of a {@code CHECK (column IN ('A', 'B'))}
	 * clause, so the assertion above is about what the database will actually
	 * accept rather than about what this test happens to have typed out.
	 */
	private static Set<String> allowedValuesOf(String constraint) {
		Matcher matcher = Pattern.compile(constraint + "\\s+CHECK\\s*\\(\\s*\\w+\\s+IN\\s*\\(([^)]*)\\)\\)")
			.matcher(migrationSql());
		if (!matcher.find()) {
			throw new IllegalStateException("No CHECK constraint named " + constraint + " in " + MIGRATION);
		}
		return Arrays.stream(matcher.group(1).split(","))
			.map(value -> value.trim().replace("'", ""))
			.collect(Collectors.toCollection(LinkedHashSet::new));
	}

	private static String migrationSql() {
		try (InputStream in = OrderTypeTest.class.getResourceAsStream(MIGRATION)) {
			if (in == null) {
				throw new IllegalStateException("Migration " + MIGRATION + " is not on the test classpath");
			}
			return new String(in.readAllBytes(), StandardCharsets.UTF_8);
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

}

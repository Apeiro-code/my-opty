package com.myopty.order.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
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
 * migrations and compare them with the enum instead of trusting a comment to stay
 * true.
 *
 * <p>Both the order table and its status are covered, and both are read from every
 * migration that touches them, because V9 replaces the status constraint outright.
 */
class OrderTypeTest {

	/**
	 * The status list the module documents, in the order the shop works through it.
	 */
	private static final List<String> FORWARD_PATH = List.of("PENDING_REVIEW", "APPROVED", "PROCESSING", "READY",
			"DISPATCHED");

	private static final List<String> MIGRATIONS = List.of("/db/migration/V8__order_create_progressive_order_table.sql",
			"/db/migration/V9__order_add_order_rejection.sql");

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
	 * The workflow the module documents, in order.
	 *
	 * <p>A status that jumps or repeats would be reported as out of order, so the
	 * sequence is part of the contract rather than an implementation detail.
	 * {@code REJECTED} is excluded because it is not a step in this path; it is the
	 * one state outside it, which the next test pins down.
	 */
	@Test
	void theStatusFollowsTheDocumentedWorkflow() {
		assertThat(new ArrayList<>(namesOfExcept(OrderStatus.values(), OrderStatus.REJECTED)))
			.isEqualTo(FORWARD_PATH);
	}

	/**
	 * Rejection is the single exception to the forward path, and it is declared
	 * last so the enum reads as the happy path followed by the way out of it.
	 * Asserting the size as well as the tail means a second exception state cannot
	 * be added without this test noticing.
	 */
	@Test
	void rejectionIsTheOnlyStatusOutsideTheForwardPath() {
		assertThat(namesOf(OrderStatus.values()).stream().toList()).endsWith(OrderStatus.REJECTED.name())
			.hasSize(FORWARD_PATH.size() + 1);
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

	private static Set<String> namesOfExcept(Enum<?>[] values, Enum<?> excluded) {
		return Arrays.stream(values)
			.map(Enum::name)
			.filter(name -> !name.equals(excluded.name()))
			.collect(Collectors.toCollection(LinkedHashSet::new));
	}

	/**
	 * Pulls the permitted values out of a {@code CHECK (column IN ('A', 'B'))}
	 * clause, so the assertions above are about what the database will actually
	 * accept rather than about what this test happens to have typed out.
	 *
	 * <p>The <em>last</em> definition wins, not the first. A later migration can
	 * drop a constraint and re-add it with different values, and that is what V9
	 * does to {@code chk_progressive_order_status} when it adds {@code REJECTED}.
	 * Taking the first match would compare the enum against a constraint the
	 * database no longer has, and the failure would read as if the enum were wrong
	 * rather than as a stale lookup.
	 */
	private static Set<String> allowedValuesOf(String constraint) {
		Matcher matcher = Pattern.compile(constraint + "\\s+CHECK\\s*\\(\\s*\\w+\\s+IN\\s*\\(([^)]*)\\)\\)")
			.matcher(migrationsSql());
		String allowed = null;
		while (matcher.find()) {
			allowed = matcher.group(1);
		}
		if (allowed == null) {
			throw new IllegalStateException("No CHECK constraint named " + constraint + " in " + MIGRATIONS);
		}
		return Arrays.stream(allowed.split(","))
			.map(value -> value.trim().replace("'", ""))
			.collect(Collectors.toCollection(LinkedHashSet::new));
	}

	private static String migrationsSql() {
		StringBuilder sql = new StringBuilder();
		for (String migration : MIGRATIONS) {
			try (InputStream in = OrderTypeTest.class.getResourceAsStream(migration)) {
				if (in == null) {
					throw new IllegalStateException("Migration " + migration + " is not on the test classpath");
				}
				sql.append(new String(in.readAllBytes(), StandardCharsets.UTF_8)).append('\n');
			}
			catch (IOException ex) {
				throw new UncheckedIOException(ex);
			}
		}
		return sql.toString();
	}

}

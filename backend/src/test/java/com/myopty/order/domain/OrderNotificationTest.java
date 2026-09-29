package com.myopty.order.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

/**
 * A notification records the two statuses its order moved between, and the column
 * it is stored in is a {@code VARCHAR} that the database cannot second-guess
 * without being told to.
 *
 * <p>Nothing in Java ties {@link OrderStatus} to that list, so a constant could be
 * added that the column would then reject on every write, and the failure would
 * surface as a customer not being told about their order rather than as a broken
 * build. These tests read the migration and compare, the same way
 * {@code OrderTypeTest} does for the order table.
 */
class OrderNotificationTest {

	private static final String MIGRATION = "/db/migration/V10__order_create_notification_table.sql";

	/**
	 * Both ends of the move are checked, not just the new one. A notification
	 * records a pair, and a row whose {@code from_status} held something the enum
	 * could not name would be unreadable rather than merely wrong.
	 */
	@Test
	void everyStatusTheOrderCanReachIsAllowedInBothColumns() {
		Set<String> statuses = Arrays.stream(OrderStatus.values())
			.map(Enum::name)
			.collect(Collectors.toCollection(LinkedHashSet::new));

		assertThat(allowedValuesOf("chk_order_notification_from_status")).isEqualTo(statuses);
		assertThat(allowedValuesOf("chk_order_notification_to_status")).isEqualTo(statuses);
	}

	/**
	 * The two columns are constrained identically on purpose, and a migration that
	 * later adds a status to one of them has almost certainly broken the other. This
	 * is the check that says so.
	 */
	@Test
	void bothColumnsAcceptTheSameStatuses() {
		assertThat(allowedValuesOf("chk_order_notification_to_status"))
			.isEqualTo(allowedValuesOf("chk_order_notification_from_status"));
	}

	/**
	 * The vocabulary is read from the same place the order table's is, so the two
	 * cannot drift: a notification describing a move the order table would refuse to
	 * record would be describing something that did not happen.
	 */
	@Test
	void acceptsExactlyTheStatusesTheOrderTableAccepts() {
		String orderMigration = "/db/migration/V9__order_add_order_rejection.sql";

		assertThat(allowedValuesOf("chk_order_notification_to_status"))
			.isEqualTo(allowedValuesOfIn(orderMigration, "chk_progressive_order_status"));
	}

	/**
	 * Pulls the permitted values out of a {@code CHECK (column IN ('A', 'B'))}
	 * clause, so the assertions above are about what the database will accept rather
	 * than about what this test happened to type out.
	 */
	private static Set<String> allowedValuesOf(String constraint) {
		return allowedValuesOfIn(MIGRATION, constraint);
	}

	private static Set<String> allowedValuesOfIn(String migration, String constraint) {
		Matcher matcher = Pattern.compile(constraint + "\\s+CHECK\\s*\\(\\s*\\w+\\s+IN\\s*\\(([^)]*)\\)\\)")
			.matcher(sqlOf(migration));
		String allowed = null;
		while (matcher.find()) {
			allowed = matcher.group(1);
		}
		if (allowed == null) {
			throw new IllegalStateException("No CHECK constraint named " + constraint + " in " + migration);
		}
		return Arrays.stream(allowed.split(","))
			.map(value -> value.trim().replace("'", ""))
			.collect(Collectors.toCollection(LinkedHashSet::new));
	}

	private static String sqlOf(String migration) {
		try (InputStream in = OrderNotificationTest.class.getResourceAsStream(migration)) {
			if (in == null) {
				throw new IllegalStateException("Migration " + migration + " is not on the test classpath");
			}
			return new String(in.readAllBytes(), StandardCharsets.UTF_8);
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	/**
	 * Every column the mapper reads has to exist, or the notification read fails at
	 * runtime rather than in a test. Written as a list so a renamed column is a
	 * failure naming the column.
	 */
	@Test
	void theMigrationDefinesEveryColumnTheEntityReads() {
		String sql = sqlOf(MIGRATION);

		assertThat(List.of("notification_id", "order_id", "customer_id", "from_status", "to_status", "message",
				"created_at")).allSatisfy(column -> assertThat(sql).as("column %s", column).contains(column));
	}

	/**
	 * {@code created_at} has no default, so the application always supplies it. A
	 * default added here would quietly let a row be written with the wrong time,
	 * which is the one column a customer-facing list sorts on.
	 */
	@Test
	void createdAtHasNoDefaultSoTheApplicationAlwaysSuppliesIt() {
		assertThat(sqlOf(MIGRATION)).doesNotContain("DEFAULT").doesNotContain("CURRENT_TIMESTAMP");
	}

}

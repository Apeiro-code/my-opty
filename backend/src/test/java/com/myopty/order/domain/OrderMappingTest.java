package com.myopty.order.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.data.jdbc.core.mapping.JdbcMappingContext;
import org.springframework.data.relational.core.mapping.RelationalPersistentEntity;
import org.springframework.data.relational.core.mapping.RelationalPersistentProperty;

/**
 * Guards the contract between the entity and the migrations that define
 * {@code progressive_order}.
 *
 * <p>A column-name drift between the two is invisible until the application runs
 * against a real database, so the mapping is compared with the migration files
 * themselves, without needing one.
 */
class OrderMappingTest {

	private final JdbcMappingContext context = new JdbcMappingContext();

	OrderMappingTest() {
		this.context.setInitialEntitySet(Set.of(Order.class));
		this.context.afterPropertiesSet();
	}

	/**
	 * {@code order} is a reserved word in SQL, so the table is named
	 * {@code progressive_order} to match the module EER diagram without quoting
	 * every statement that touches it.
	 */
	@Test
	void mapsToTheProgressiveOrderTable() {
		assertThat(this.context.getRequiredPersistentEntity(Order.class).getTableName().getReference())
			.isEqualTo("progressive_order");
	}

	/**
	 * {@code RelationalPersistentProperty.getColumnName()} is what Spring Data JDBC's
	 * {@code SqlGenerator} reads and it honours {@code @Column}, so it is the method
	 * that decides whether a generated statement works against the migration.
	 */
	@Test
	void mapsEveryPropertyToAColumnThatExistsInTheMigration() {
		assertThat(new LinkedHashSet<>(columnsOfEntity().values())).isEqualTo(columnsOfMigration());
	}

	/**
	 * The equality above would pass just as happily if the parser quietly dropped
	 * every {@code ALTER TABLE}, because the entity would be compared against a
	 * shorter set and the comparison is exact in both directions. This asserts the
	 * column V9 added is actually seen, so a parser that forgets {@code ADD COLUMN}
	 * fails here rather than looking like a correct mapping.
	 */
	@Test
	void readsColumnsIntroducedByAlteringMigrations() {
		assertThat(columnsOfMigration()).contains("rejection_reason");
	}

	/**
	 * The clauses that surround an {@code ALTER TABLE} name no column, so reading
	 * their first token would invent columns that do not exist.
	 */
	@Test
	void doesNotMistakeAlterClausesForColumns() {
		assertThat(columnsOfMigration()).doesNotContain("alter", "add", "drop", "constraint", "check");
	}

	@Test
	void exposesTheIdAsTheSpringDataIdentifier() {
		RelationalPersistentEntity<?> entity = this.context.getRequiredPersistentEntity(Order.class);

		assertThat(entity.getRequiredIdProperty().getName()).isEqualTo("orderId");
	}

	/**
	 * The repository derives its reverse lookup from this property name, so a
	 * rename here would not fail compilation but would fail at runtime with a query
	 * that cannot be derived.
	 */
	@Test
	void keepsTheLinkToThePrescriptionOnThePropertyTheRepositoryDerivesFrom() {
		assertThat(columnsOfEntity()).containsEntry("prescriptionId", "prescription_id");
	}

	@Test
	void doesNotTreatThePrescriptionAsANestedAggregate() {
		RelationalPersistentEntity<?> entity = this.context.getRequiredPersistentEntity(Order.class);

		assertThat(entity.getRequiredPersistentProperty("prescriptionId").isEntity()).isFalse();
	}

	private Map<String, String> columnsOfEntity() {
		Map<String, String> columns = new LinkedHashMap<>();
		RelationalPersistentEntity<?> entity = this.context.getRequiredPersistentEntity(Order.class);
		for (RelationalPersistentProperty property : entity) {
			columns.put(property.getName(), property.getColumnName().getReference().toLowerCase(Locale.ROOT));
		}
		return columns;
	}

	/**
	 * The columns of {@code progressive_order} as a whole, which means every
	 * migration that creates or alters the table rather than just the one that
	 * created it. V9 adds {@code rejection_reason} with an {@code ALTER TABLE}, so
	 * reading V8 alone would report a column the entity has and the parser would
	 * miss, and the comparison below would fail on a perfectly correct entity.
	 *
	 * <p>Order matters: a migration that later replaced a column would need its
	 * earlier definition dropped from the set, which is what
	 * {@code OrderTypeTest} does for the status constraint.
	 */
	private static final List<String> MIGRATIONS = List.of("/db/migration/V8__order_create_progressive_order_table.sql",
			"/db/migration/V9__order_add_order_rejection.sql");

	private static Set<String> columnsOfMigration() {
		return migrationsSql().lines()
			.map(String::trim)
			.filter(line -> !line.isEmpty() && !line.startsWith("--"))
			.filter(line -> !isTableLevelClause(line))
			.map(OrderMappingTest::columnNameOf)
			.collect(Collectors.toCollection(LinkedHashSet::new));
	}

	/**
	 * Lines that shape the table without naming a column: keys, indexes,
	 * constraints and the {@code ALTER}/{@code CREATE} statements themselves.
	 *
	 * <p>{@code CHECK} has to be listed separately because a replacement constraint
	 * is written across two lines, with the {@code CHECK (column IN (...))} part
	 * on its own line where it no longer starts with {@code ADD CONSTRAINT}.
	 */
	private static boolean isTableLevelClause(String line) {
		return line.startsWith(")") || line.startsWith("PRIMARY KEY") || line.startsWith("UNIQUE KEY")
				|| line.startsWith("INDEX") || line.startsWith("CONSTRAINT") || line.startsWith("ENGINE")
				|| line.startsWith("DEFAULT") || line.startsWith("COLLATE") || line.startsWith("CREATE TABLE")
				|| line.startsWith("ALTER TABLE") || line.startsWith("DROP CHECK") || line.startsWith("CHECK")
				|| line.startsWith("ADD CONSTRAINT");
	}

	/**
	 * A column is either declared on its own line inside {@code CREATE TABLE} or
	 * introduced later by {@code ADD COLUMN}, so the prefix is stripped before the
	 * name is taken.
	 */
	private static String columnNameOf(String line) {
		String declaration = line.startsWith("ADD COLUMN") ? line.substring("ADD COLUMN".length()).trim() : line;
		return declaration.split("\\s+", 2)[0].toLowerCase(Locale.ROOT);
	}

	private static String migrationsSql() {
		StringBuilder sql = new StringBuilder();
		for (String migration : MIGRATIONS) {
			try (InputStream in = OrderMappingTest.class.getResourceAsStream(migration)) {
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

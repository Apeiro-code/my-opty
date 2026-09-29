package com.myopty.order.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.data.jdbc.core.mapping.JdbcMappingContext;
import org.springframework.data.relational.core.mapping.RelationalPersistentEntity;
import org.springframework.data.relational.core.mapping.RelationalPersistentProperty;

/**
 * Guards the contract between the entity and
 * {@code V8__order_create_progressive_order_table.sql}.
 *
 * <p>A column-name drift between the two is invisible until the application runs
 * against a real database, so the mapping is compared with the migration file
 * itself, without needing one.
 */
class OrderMappingTest {

	private static final String MIGRATION = "/db/migration/V8__order_create_progressive_order_table.sql";

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
	 * Reads the column definitions out of the migration. Table-level clauses and
	 * comments are skipped; every remaining line starts with a column name.
	 */
	private static Set<String> columnsOfMigration() {
		return migrationSql().lines()
			.map(String::trim)
			.filter(line -> !line.isEmpty() && !line.startsWith("--"))
			.filter(line -> !line.startsWith(")") && !line.startsWith("PRIMARY KEY") && !line.startsWith("UNIQUE KEY")
					&& !line.startsWith("INDEX") && !line.startsWith("CONSTRAINT") && !line.startsWith("ENGINE")
					&& !line.startsWith("DEFAULT") && !line.startsWith("COLLATE")
					&& !line.startsWith("CREATE TABLE"))
			.map(line -> line.split("\\s+", 2)[0].toLowerCase(Locale.ROOT))
			.collect(Collectors.toCollection(LinkedHashSet::new));
	}

	private static String migrationSql() {
		try (InputStream in = OrderMappingTest.class.getResourceAsStream(MIGRATION)) {
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

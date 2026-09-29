package com.myopty.order.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.myopty.order.domain.OrderType;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

/**
 * Wiring of the lab configuration. The whole application context cannot start in
 * this environment (it needs MySQL), so the module's own properties are bound in
 * isolation: a rename of a key in {@code application.yaml} has to be caught here
 * rather than at runtime on a first approval.
 */
class LabPropertiesTest {

	private final ApplicationContextRunner runner = new ApplicationContextRunner()
		.withUserConfiguration(LabPropertiesConfiguration.class)
		.withPropertyValues("myopty.lab.lead-days.SINGLE_VISION=7", "myopty.lab.lead-days.BIFOCAL=10",
				"myopty.lab.lead-days.PROGRESSIVE=14");

	@Test
	void bindsTheLeadTimePerOrderType() {
		this.runner.run(context -> {
			LabProperties properties = context.getBean(LabProperties.class);
			assertThat(properties.leadDaysFor(OrderType.SINGLE_VISION)).isEqualTo(7);
			assertThat(properties.leadDaysFor(OrderType.BIFOCAL)).isEqualTo(10);
			assertThat(properties.leadDaysFor(OrderType.PROGRESSIVE)).isEqualTo(14);
		});
	}

	/**
	 * The keys are written in enum-name form in {@code application.yaml}, so the
	 * binding has to key by {@code OrderType} rather than leaving a map of strings
	 * that every caller would have to look up by hand.
	 */
	@Test
	void keysTheLeadTimesByTheOrderTypeTheyBelongTo() {
		this.runner.run(context -> {
			LabProperties properties = context.getBean(LabProperties.class);

			assertThat(properties.leadDays()).containsEntry(OrderType.PROGRESSIVE, 14)
				.containsEntry(OrderType.SINGLE_VISION, 7);
		});
	}

	@Test
	void fallsBackWhenTheShopHasNotConfiguredAType() {
		this.runner.withPropertyValues("myopty.lab.lead-days=").run(context -> {
			assertThat(context).hasNotFailed();
			assertThat(context.getBean(LabProperties.class).leadDaysFor(OrderType.SINGLE_VISION)).isEqualTo(7);
		});
	}

	/**
	 * A negative lead time would quote a receive date before the order was even
	 * approved, so a nonsense entry is treated as absent rather than trusted.
	 */
	@Test
	void ignoresANegativeLeadTime() {
		this.runner.withPropertyValues("myopty.lab.lead-days.PROGRESSIVE=-3").run(context -> {
			assertThat(context.getBean(LabProperties.class).leadDaysFor(OrderType.PROGRESSIVE)).isEqualTo(7);
		});
	}

	@Test
	void acceptsAZeroLeadTimeAsSameDayService() {
		this.runner.withPropertyValues("myopty.lab.lead-days.SINGLE_VISION=0").run(context -> {
			assertThat(context.getBean(LabProperties.class).leadDaysFor(OrderType.SINGLE_VISION)).isZero();
		});
	}

	@Test
	void buildsAClockForTheEstimateToRead() {
		new ApplicationContextRunner().withUserConfiguration(LabPropertiesConfiguration.class, LabClockConfig.class)
			.run(context -> assertThat(context).hasSingleBean(java.time.Clock.class));
	}

	/**
	 * The other tests in this class hand the binder values directly, which means
	 * they would pass unchanged if {@code application.yaml} spelled the keys
	 * wrongly. This one loads the real file, so a typo there, or a type the shop
	 * forgot, fails here instead of quietly leaving every order on the fallback.
	 */
	@Test
	void theShippedConfigurationSuppliesALeadTimeForEveryOrderType() {
		new ApplicationContextRunner().withInitializer(new ConfigDataApplicationContextInitializer())
			.withUserConfiguration(LabPropertiesConfiguration.class)
			.run(context -> {
				assertThat(context).hasNotFailed();
				LabProperties properties = context.getBean(LabProperties.class);
				for (OrderType type : OrderType.values()) {
					assertThat(properties.leadDays()).as("lead days for %s", type).containsKey(type);
				}
				assertThat(properties.leadDaysFor(OrderType.PROGRESSIVE))
					.isGreaterThan(properties.leadDaysFor(OrderType.SINGLE_VISION));
			});
	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties(LabProperties.class)
	static class LabPropertiesConfiguration {

	}

}

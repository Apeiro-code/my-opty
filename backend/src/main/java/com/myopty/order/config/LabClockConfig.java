package com.myopty.order.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The clock the module reads dates from.
 *
 * <p>A receive date is a calendar date in the shop's own timezone, so it is
 * derived with a zone-aware {@link Clock} rather than from a UTC instant. A
 * single bean also means the estimate can be asserted in a test against a fixed
 * date: reading the system clock inside the service would make the expected value
 * wrong for one run a day, at the hour the date rolls over.
 */
@Configuration(proxyBeanMethods = false)
public class LabClockConfig {

	/**
	 * @return the system clock in the default zone. A shop in one timezone is not a
	 *         problem yet; {@code myopty.lab.zone} is where that would be configured
	 *         if the module ever serves shops in more than one.
	 */
	@Bean
	Clock clock() {
		return Clock.systemDefaultZone();
	}

}

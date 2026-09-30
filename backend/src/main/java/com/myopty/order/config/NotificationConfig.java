package com.myopty.order.config;

import com.myopty.order.service.notification.LoggingNotificationSender;
import com.myopty.order.service.notification.NotificationSender;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * How notifications reach a customer, until a real transport exists.
 *
 * <p>Follows the shape of {@code MinioClientConfig} next door, which does the same
 * thing for document storage: a default that is declared in one place and can be
 * replaced by declaring another bean.
 *
 * <p>The condition is here rather than on {@link LoggingNotificationSender} for a
 * reason that is easy to get wrong. Two {@link NotificationSender} beans would
 * leave the injection point in
 * {@code OrderNotificationServiceImpl} ambiguous and fail at startup, so adding a
 * real sender has to displace this one. A {@code @ConditionalOnMissingBean} on a
 * scanned {@code @Component} depends on the order components happen to be scanned
 * in, which is not something to rely on; on a {@code @Bean} in a configuration
 * class it is evaluated before the scan reaches the components that might replace
 * it.
 *
 * <p>The ordering is still real, and it is the part a real transport has to be
 * careful about: the condition only sees beans already defined, so a replacement
 * has to be registered <em>before</em> this configuration is processed. Declaring
 * the sender in a configuration class registered ahead of this one is the reliable
 * way; {@code NotificationConfigTest.stepsAsideForARealSender} pins that ordering
 * rather than assuming it.
 */
@Configuration
public class NotificationConfig {

	/**
	 * The fallback sender. Declared {@code @ConditionalOnMissingBean} so that adding
	 * a real transport is a matter of declaring one, with no edit here and no
	 * {@code @Primary} to remember.
	 */
	@Bean
	@ConditionalOnMissingBean(NotificationSender.class)
	NotificationSender notificationSender() {
		return new LoggingNotificationSender();
	}

}

package com.myopty.order.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.myopty.order.domain.OrderNotification;
import com.myopty.order.service.notification.LoggingNotificationSender;
import com.myopty.order.service.notification.NotificationSender;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * How the sender bean gets there, and how a real one takes over.
 *
 * <p>Worth a test of its own because the failure would be a startup failure, not a
 * wrong answer: two {@link NotificationSender} beans leave the injection point in
 * the notification service ambiguous, and the application refuses to start. The
 * condition exists to stop that happening when a real transport is added, and this
 * is what proves the condition is in the right place.
 */
class NotificationConfigTest {

	@Test
	void providesExactlyOneSenderSoTheInjectionPointIsUnambiguous() {
		withFallbackOnly().run(context -> assertThat(context).hasSingleBean(NotificationSender.class));
	}

	@Test
	void logsUntilSomethingBetterIsWiredIn() {
		withFallbackOnly()
			.run(context -> assertThat(context.getBean(NotificationSender.class)).isInstanceOf(LoggingNotificationSender.class));
	}

	/**
	 * The back-off itself, which cannot be inferred from the other two. A real
	 * transport declared alongside has to <em>replace</em> the log one rather than
	 * sit beside it, because beside it is a startup failure.
	 *
	 * <p>The real sender is registered first, which is what makes the outcome
	 * dependable: a {@code @ConditionalOnMissingBean} sees only the beans defined
	 * before it. That ordering caveat is the reason this lives in a configuration
	 * class rather than as a condition on a scanned component, and it is what
	 * {@code NotificationConfig} asks a real transport to preserve.
	 */
	@Test
	void stepsAsideForARealSender() {
		new ApplicationContextRunner().withUserConfiguration(RealTransportConfig.class, NotificationConfig.class)
			.run(context -> {
				assertThat(context).hasSingleBean(NotificationSender.class);
				assertThat(context.getBean(NotificationSender.class)).isInstanceOf(RealTransport.class);
			});
	}

	private static ApplicationContextRunner withFallbackOnly() {
		return new ApplicationContextRunner().withUserConfiguration(NotificationConfig.class);
	}

	@Configuration
	static class RealTransportConfig {

		@Bean
		NotificationSender realNotificationSender() {
			return new RealTransport();
		}

	}

	/**
	 * Stands in for the mail provider the shared module has not got yet.
	 */
	static class RealTransport implements NotificationSender {

		@Override
		public void send(OrderNotification notification) {
		}

	}

}

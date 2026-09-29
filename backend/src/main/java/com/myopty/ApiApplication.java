package com.myopty;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Application bootstrap.
 *
 * <p>It lives in {@code com.myopty} (not in a module package) so that component
 * scanning, Spring Data repository scanning and auto-configuration packages all
 * cover every feature module ({@code com.myopty.catalog}, {@code com.myopty.order},
 * {@code com.myopty.workflow}, {@code com.myopty.billing}, {@code com.myopty.shared}).
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class ApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(ApiApplication.class, args);
	}

}

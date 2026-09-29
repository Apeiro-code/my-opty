package com.myopty.order.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import org.junit.jupiter.api.Test;

/**
 * The declared shape of the receive-date body.
 *
 * <p>Unlike the other request records this one carries no bean validation, because
 * every rule about the date needs the shop's current date and the order's status,
 * and both are only known in the service. What is worth pinning down here is that
 * the field stays optional: withdrawing an estimate is done by sending a body with
 * no date, so adding a {@code @NotNull} to this record would turn a documented
 * operation into a 400 without any test of its own noticing.
 */
class ReceiveDateRequestTest {

	private final ObjectMapper mapper = JsonMapper.builder().addModule(new JavaTimeModule()).build();

	@Test
	void readsAnIsoDate() throws Exception {
		ReceiveDateRequest request = this.mapper.readValue("{\"receiveDate\":\"2026-10-27\"}",
				ReceiveDateRequest.class);

		assertThat(request.receiveDate()).isEqualTo(LocalDate.of(2026, 10, 27));
	}

	@Test
	void readsAnAbsentDateAsNoDate() throws Exception {
		ReceiveDateRequest request = this.mapper.readValue("{}", ReceiveDateRequest.class);

		assertThat(request.receiveDate()).isNull();
	}

	@Test
	void readsAnExplicitNullAsNoDate() throws Exception {
		ReceiveDateRequest request = this.mapper.readValue("{\"receiveDate\":null}", ReceiveDateRequest.class);

		assertThat(request.receiveDate()).isNull();
	}

	/**
	 * A date in some other format is a 400 at the edge rather than a guess, so a
	 * client that sends one is told instead of the server picking a date for it.
	 */
	@Test
	void refusesADateItCannotRead() {
		assertThat(cannotRead("{\"receiveDate\":\"27/10/2026\"}")).isTrue();
	}

	/**
	 * The withdrawal call has to reach the service, so an empty body cannot be
	 * rejected before the controller runs.
	 */
	@Test
	void acceptsAWhollyEmptyBody() {
		assertThat(cannotRead("{}")).isFalse();
	}

	private boolean cannotRead(String json) {
		try {
			this.mapper.readValue(json, ReceiveDateRequest.class);
			return false;
		}
		catch (Exception ex) {
			return true;
		}
	}

}

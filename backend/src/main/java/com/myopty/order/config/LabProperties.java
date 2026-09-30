package com.myopty.order.config;

import java.util.Map;

import com.myopty.order.domain.OrderType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * How long the lab takes to turn an order, bound from {@code myopty.lab.*}.
 *
 * <p>The estimate a customer is shown is a promise the shop has to keep, so it is
 * configuration rather than a constant in the service: the same code has to work
 * for a shop that cuts single-vision lenses in a week and one that runs a lab
 * queue of a fortnight.
 *
 * <p>This is deliberately only part of what the documented estimate depends on.
 * The estimate is meant to account for lens type, stock and lab time, but stock
 * belongs to the catalog module ({@code V2__catalog_create_frame_table.sql}) and
 * that table does not exist yet, so there is nothing to read. Lead time per order
 * type is the part this module can know on its own, and it is the seam the stock
 * check is added to once the catalog lands.
 *
 * @param leadDays days to add to the approval date, per order type
 */
@ConfigurationProperties(prefix = "myopty.lab")
public record LabProperties(Map<OrderType, Integer> leadDays) {

	private static final Logger logger = LoggerFactory.getLogger(LabProperties.class);

	/**
	 * Used when a type is missing from configuration or has a nonsensical value.
	 * Falling back rather than failing matters because this is read while approving
	 * an order: a config gap should not stop the shop taking work, it should just
	 * quote a rougher date.
	 */
	private static final int DEFAULT_LEAD_DAYS = 7;

	public LabProperties {
		leadDays = leadDays == null ? Map.of() : Map.copyOf(leadDays);
	}

	/**
	 * @return the configured lead time for this order type, or the default when the
	 *         shop has not set one. A null or negative entry is treated as unset
	 *         rather than trusted, since a negative lead time would quote a receive
	 *         date before the order was even approved.
	 */
	public int leadDaysFor(OrderType orderType) {
		Integer configured = orderType == null ? null : this.leadDays.get(orderType);
		if (configured == null || configured < 0) {
			logger.warn("No usable myopty.lab.lead-days entry for {}; quoting {} days instead",
					orderType == null ? "an unknown order type" : orderType.name(), DEFAULT_LEAD_DAYS);
			return DEFAULT_LEAD_DAYS;
		}
		return configured;
	}

}

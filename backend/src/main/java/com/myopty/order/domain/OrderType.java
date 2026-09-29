package com.myopty.order.domain;

import java.util.Locale;

/**
 * The kind of lens the customer is ordering, and therefore the workflow the
 * order enters once the shop has reviewed it.
 *
 * <p>This is deliberately separate from {@code prescription.is_progressive}. That
 * flag answers "does this prescription carry a near addition", which the shop
 * needs before it can even verify the values; the order type answers "what is
 * being manufactured". A progressive order on a prescription that has no addition
 * would produce a lens the lab cannot cut, so the service refuses the combination
 * rather than letting the two columns drift apart.
 *
 * <p>The names are asserted against the {@code chk_progressive_order_type} check
 * constraint by {@code OrderTypeTest}, so a constant added here without a matching
 * migration fails the build rather than failing an insert in production.
 */
public enum OrderType {

	/**
	 * One distance correction per eye. The default a customer gets when they do
	 * not need anything more.
	 */
	SINGLE_VISION,

	/**
	 * A distance correction with a separate near segment. Carries a near addition
	 * on both eyes, like {@link #PROGRESSIVE}, but is cut from a visible segment
	 * rather than a seamless gradient.
	 */
	BIFOCAL,

	/**
	 * Seamless lens that blends distance, intermediate and near vision. Needs a
	 * near addition on both eyes and takes longer in the lab, which is why it is
	 * routed as its own workflow rather than treated as a single-vision order.
	 */
	PROGRESSIVE;

	/**
	 * Whether the lab needs a near addition on both eyes to cut this lens. A lens
	 * ordered without one is not cheaper, it is unmanufacturable, so the service
	 * checks this against the prescription rather than accepting the order and
	 * discovering it at the bench.
	 */
	public boolean requiresNearAddition() {
		return this == BIFOCAL || this == PROGRESSIVE;
	}

	/**
	 * @return the type phrased for a person rather than a column, e.g.
	 *         {@code "progressive lens"}, so validation messages read as English
	 *         without each caller inventing its own wording
	 */
	public String label() {
		return name().toLowerCase(Locale.ROOT).replace('_', ' ') + " lens";
	}

}

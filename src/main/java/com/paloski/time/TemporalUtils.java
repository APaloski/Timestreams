package com.paloski.time;

import java.time.temporal.TemporalAmount;
import java.time.temporal.TemporalUnit;

/**
 * A static utility class that contains methods for working with Temporal
 * objects.
 * 
 * @author apaloski
 *
 */
public class TemporalUtils {

	/**
	 * Obtains an estimation of the number of a given TemporalUnit in a given
	 * TemporalAmount. This is calculated by taking the total sum of all of the
	 * units that make up a {@link TemporalAmount}, and then converting them
	 * into {@code unit}.
	 * <p>
	 * Note, this is called estimated because TemporalUnit allows a class to
	 * have an estimated value via {@link TemporalUnit#isDurationEstimated()}.
	 * 
	 * @param amount
	 *            A TemporalAmount that will be converted into a given temporal
	 *            unit
	 * @param unit
	 *            A TemporalUnit that this type should be converted into.
	 * @return A double representing the fractional number of {@code unit} that
	 *         is represented by {@code amount}.
	 */
	public static double getEstimatedNumberOfUnits(TemporalAmount amount, TemporalUnit unit) {
		final long nanosInUnit = unit.getDuration().toNanos();
		double totalUnit = 0;
		for (TemporalUnit unitOfAmount : amount.getUnits()) {
			long numberOf = amount.get(unitOfAmount);
			totalUnit += (double) (unitOfAmount.getDuration().toNanos() * numberOf) / (double) nanosInUnit;
		}
		return totalUnit;
	}

}

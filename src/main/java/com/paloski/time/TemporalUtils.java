/*
 * The MIT License (MIT)
 * 
 * Copyright (c) 2015 Adam Paloski
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * allcopies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
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

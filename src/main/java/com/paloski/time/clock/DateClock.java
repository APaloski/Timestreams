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
package com.paloski.time.clock;

import java.io.Serializable;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Date;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * An abstract subclass of Clock that is backed by Date objects instead of being
 * tied to Instant or the System clock
 * <p>
 * This class provides the time that's accessible via {@link #millis()} and
 * {@link #instant()} by interpreting the current time from {@link Date} objects
 * provided to it from {@link #getDate()}. As this Clock is directly linked to
 * the dates returned by {@code getDate} it can be made to act like any of the
 * Clocks provided by the {@link Clock} class.
 * <p>
 * Note that the Date objects returned from {@link #getDate()} <em>must</em> be
 * convertible to an {@link Instant}, as such a {@link java.sql.Time} object is
 * not a valid return value.
 * <p>
 * In a normal use case, this class can be used from an existing Date source in
 * the following way:
 * <p>
 * <pre>
 * {@code
 * 	//Get the current date based time source...
 * 	ExistingTimeSource existingSource = getExistingTimeSource();
 * 	Clock dateClock = DateClock.ofSupplier(existingSource::getCurrentDate());
 * 	//Call into any of the Temporal implementations that take a Clock to their now() function
 * 	LocalDate date = LocalDate.now(dateClock);
 * }
 * </pre>
 * <p>
 * Subclasses should ensure that they document immutability, Serializability and
 * thread safety, as described in {@link Clock}. Additionally, subclases must
 * <em>never</em> modify Date objects returned from {@link #getDate()}, and should clone it
 * before changing it.
 *
 * @author Adam Paloski
 */
public abstract class DateClock extends Clock {

	/**
	 * Constructor accessible by subclasses.
	 */
	protected DateClock() {
		// Do nothing
	}

	/**
	 * {@inheritDoc}
	 *
	 * @implNote The implementation of this method in DateClock will <b>not</b>
	 * modify the Date object returned by {@link #getDate()}
	 */
	@Override
	public final long millis() {
		return getDate().getTime();
	}

	/**
	 * {@inheritDoc}
	 *
	 * @implNote The implementation of this method in DateClock will <b>not</b>
	 * modify the Date object returned by {@link #getDate()}
	 */
	@Override
	public final Instant instant() {
		return getDate().toInstant();
	}

	/**
	 * Obtains a Date object representing the time of this clock. Note that the
	 * value return by this method <em>must</em> not be null and must not throw
	 * when {@link Date#toInstant()} is invoked, if it does, that exception will
	 * be propagated to the caller of {@link #instant()}. Due to this, a
	 * {@link java.sql.Time} object is not a suitable return type for this
	 * method.
	 * <p>
	 * Any invokers of this method should take care <b>not</b> to alter the
	 * returned Date object, as it has no requirements of immutability and may
	 * be a single object returned repeatedly, such as in a fixed clock.
	 *
	 * @return A non-null Date object representing the current point on the
	 * clock.
	 */
	protected abstract Date getDate();

	/**
	 * Obtains a new DateClock that has its Date values supplied by a Supplier
	 * with a given ZoneId.
	 * <p>
	 * The Supplier passed to this function <b>must</b> not return null when
	 * {@link Supplier#get()} is invoked and the returned values must not throw
	 * when {@link Date#toInstant()} is called. Doing either will result in a
	 * call to {@link #millis()} or {@link #instant()} propagating an exception
	 * up to the caller. Due to this fact, a Supplier returning a
	 * {@link java.sql.Time} object is not suitable for this method, as it
	 * cannot be converted to an Instant.
	 * <p>
	 * The returned implementation is immutable, thread-safe and
	 * {@code Serializable} providing that the underlying Supplier is.
	 *
	 * @param dateSupplier
	 * 		A non-null Supplier that returns only non-null date objects. <em>This
	 * 		Supplier must be thread safe to fulfill the contract of Clock</em>
	 * @param zoneId
	 * 		A non-null ZoneId that this cloak is situated in.
	 *
	 * @return A non-null DateClock that will return Instants based upon the
	 * Dates returned by {@code dateSupplier}.
	 *
	 * @implNote The Date objects supplied to any Clock produced by this method
	 * will <i>not</i> be modified.
	 */
	public static DateClock ofSupplier(Supplier<Date> dateSupplier, ZoneId zoneId) {
		return new DateSupplierClock(dateSupplier, zoneId);
	}

	/**
	 * A Private Clock class used by {@link #ofSupplier(Supplier, ZoneId)}
	 *
	 * @author Adam Paloski
	 */
	private static class DateSupplierClock extends DateClock implements Serializable {

		private static final long serialVersionUID = 3513511693670455875L;

		private final Supplier<Date> mDateSupplier;
		private final ZoneId mZoneId;

		/* Package */DateSupplierClock(Supplier<Date> supplier, ZoneId zoneId) {
			mDateSupplier = Objects.requireNonNull(supplier, "The date supplier may not be null");
			mZoneId = Objects.requireNonNull(zoneId, "The zoneid may not be null");
		}

		@Override
		public Date getDate() {
			return Objects.requireNonNull(mDateSupplier.get(), "The date supplier, " + mDateSupplier + " may not supply a null value.");
		}

		@Override
		public ZoneId getZone() {
			return mZoneId;
		}

		@Override
		public Clock withZone(ZoneId zone) {
			// Intentionally cause an NPE here if zone is null
			if (zone.equals(mZoneId)) {
				return this;
			} else {
				return new DateSupplierClock(mDateSupplier, zone);
			}
		}

	}

}

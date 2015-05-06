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
 * tied to Instant or the System clock. In general this class should
 * <em>not</em> be the preferred class over one of the standard subclasses of
 * Clock, instead it should be used in the case where an existing Date time
 * source is already in place and doing a full conversion on it would be too
 * expensive.
 * <p>
 * This class provides the current time that's accessible via {@link #millis()}
 * and {@link #instant()} by interpreting the current time from {@link Date}
 * objects and the functionality of this class is entirely based upon the Date
 * objects provided to it from {@link #getDate()}. If Date objects that
 * represent the same point in time are repeatedly returned this class will
 * function like {@link Clock#fixed(Instant, ZoneId)}. Note that the Date
 * objects returned from {@link #getDate()} <em>must</em> be convertible to an
 * {@link Instant}, as such a {@link java.sql.Time} object is not a valid return
 * value.
 * <p>
 * In a normal use case, this class can be used from an existing Date source in
 * the following way:
 * 
 * <pre>
 * {@code
 * //Get the current date based time source...
 * ExistingTimeSource existingSource = getExistingTimeSource();
 * Clock dateClock = DateClock.ofSupplier(existingSource::getCurrentDate());
 * //Call into any of the Temporal implementations that take a Clock to their now() function
 * LocalDate date = LocalDate.now(dateClock);
 * }
 * </pre>
 * <p>
 * Subclasses should ensure that they document immutability, Serializability and
 * thread safety, as described in {@link Clock}. Note that all subclasses should
 * <em>never</em> modify Date objects returned from {@link #getDate()}.
 * 
 * 
 * @author Adam Paloski
 *
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
	 *           modify the Date object returned by {@link #getDate()}
	 */
	@Override
	public final long millis() {
		return getDate().getTime();
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @implNote The implementation of this method in DateClock will <b>not</b>
	 *           modify the Date object returned by {@link #getDate()}
	 */
	@Override
	public final Instant instant() {
		return getDate().toInstant();
	}

	/**
	 * Obtains a Date object representing the current point in time of this
	 * clock. Note that the value return by this method <em>must</em> not be
	 * null and must not throw when {@link Date#toInstant()} is invoked, if it
	 * does, that exception will be propagated to the caller of
	 * {@link #instant()} because of this, a {@link java.sql.Time} object is not
	 * a suitable return type for this method.
	 * <p>
	 * Any invokers of this method should take care <b>not</b> to alter the
	 * returned Date object, as it has no requirements of immutability and may
	 * be a single object returned repeatedly, such as in a fixed clock.
	 * 
	 * @return A non-null Date object representing the current point on the
	 *         clock.
	 */
	protected abstract Date getDate();

	/**
	 * Creates a new, concrete DateClock with {@code zoneId} as the value it
	 * returns from {@link #getZone()} and using {@code dateSupplier} as the
	 * underlying source of Date objects to provide Instants via
	 * {@link #instant()}.
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
	 *            A non-null Supplier that returns only non-null date objects.
	 * @param zoneId
	 *            A non-null ZoneId that this cloak is situated in.
	 * @return A non-null DateClock that will return Instants based upon the
	 *         Dates returned by {@code dateSupplier}.
	 * @implNote The Date objects supplied to any Clock produced by this method
	 *           will <i>not</i> be modified.
	 */
	public static DateClock ofSupplier(Supplier<Date> dateSupplier, ZoneId zoneId) {
		return new DateSupplierClock(dateSupplier, zoneId);
	}

	/**
	 * A Private Clock class used by {@link #ofSupplier(Supplier, ZoneId)}
	 * 
	 * @author Adam Paloski
	 *
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
			return Objects.requireNonNull(mDateSupplier.get(), "The date supplier, " + mDateSupplier
					+ " may not supply a null value.");
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

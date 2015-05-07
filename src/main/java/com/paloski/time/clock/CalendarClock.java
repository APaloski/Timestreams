package com.paloski.time.clock;

import java.io.Serializable;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * An abstract subclass of Clock that is backed by Calendar objects instead of
 * being tied to Instant or the System clock.
 * <p>
 * This class provides the current time that's accessible via {@link #millis()}
 * and {@link #instant()} by interpreting the current time from {@link Calendar}
 * objects that are provided to it from {@link #getCalendar()}. As this Clock is
 * directly linked to the calendars returned by {@code getCalendar} it can be
 * made to act like any of the Clocks provided by the {@link Clock} class.
 * <p>
 * In a normal use case, this class can be used from an existing Calendar source
 * in the following way:
 * 
 * <pre>
 * {@code
 * //Get the current date based time source...
 * ExistingTimeSource existingSource = getExistingTimeSource();
 * Clock calendarClock = CalendarClock.ofSupplier(existingSource::getCurrentCalendar());
 * //Call into any of the Temporal implementations that take a Clock to their now() function
 * LocalDate date = LocalDate.now(calendarClock);
 * }
 * </pre>
 * <p>
 * Subclasses should ensure that they document immutability, Serializability and
 * thread safety, as described in {@link Clock}. Subclasses must <em>never</em>
 * modify Calendar objects returned from {@link #getCalendar()}, and should
 * clone it before changing it.
 * 
 * 
 * @author Adam Paloski
 *
 */
public abstract class CalendarClock extends Clock {

	/**
	 * Constructor accessible by subclasses.
	 */
	protected CalendarClock() {
		// Do nothing
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @implSpec All implementations of this method in CalendarClock will
	 *           <b>not</b> modify the Calendar object returned by
	 *           {@link #getCalendar()}.
	 */
	@Override
	public final long millis() {
		return getCalendar().getTimeInMillis();
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @implNote All implementations of this method in CalendarClock will
	 *           <b>not</b> modify the Calendar object returned by
	 *           {@link #getCalendar()}
	 */
	@Override
	public final Instant instant() {
		return Instant.ofEpochMilli(millis());
	}

	/**
	 * Obtains a Calendar object representing the current point in time of this
	 * clock. Note that the value return by this method <em>must</em> not be
	 * null.
	 * <p>
	 * Any invokers of this method should take care <b>not</b> to alter the
	 * returned Calendar object, as it has no requirements of immutability and
	 * may be a single object returned repeatedly, such as in the case of a
	 * fixed clock.
	 * 
	 * @return A non-null Calendar object representing the current point on the
	 *         clock.
	 */
	protected abstract Calendar getCalendar();

	/**
	 * Obtains a new CalendarClock that has its Calendar values supplied by a
	 * Supplier with a given ZoneId.
	 * <p>
	 * The Supplier passed to this function <b>must</b> not return null when
	 * {@link Supplier#get()} is invoked. To do so will result in a
	 * {@code NullPointerException} being thrown.
	 * <p>
	 * Note that as UTC time from the calendars returned from
	 * {@code calendarSupplier} is used to create the Instants produced by this
	 * class the TimeZone of the Calendar is ignored and the calendars entry is
	 * always converted to UTC.
	 * <p>
	 * The returned implementation is immutable, thread-safe and
	 * {@code Serializable} providing that the underlying Supplier is.
	 * 
	 * @param calendarSupplier
	 *            A non-null Supplier that returns only non-null Calendar
	 *            objects.
	 * @param zoneId
	 *            A non-null ZoneId that this cloak is situated in.
	 * @return A non-null CalendarClock that will return Instants based upon the
	 *         Calendars returned by {@code calendarSupplier}.
	 * @implNote The Calendar objects supplied to any Clock produced by this
	 *           method will <em>not</em> be modified.
	 */
	public static CalendarClock ofSupplier(Supplier<Calendar> calendarSupplier, ZoneId zoneId) {
		return new CalendarSupplierClock(calendarSupplier, zoneId);
	}

	/**
	 * A Private Clock class used by {@link #ofSupplier(Supplier, ZoneId)}
	 * 
	 * @author Adam Paloski
	 *
	 */
	private static class CalendarSupplierClock extends CalendarClock implements Serializable {

		private static final long serialVersionUID = 8735703337457423549L;
		private final Supplier<Calendar> mCalendarSupplier;
		private final ZoneId mZoneId;

		/* Package */CalendarSupplierClock(Supplier<Calendar> supplier, ZoneId zoneId) {
			mCalendarSupplier = Objects.requireNonNull(supplier, "The calendar supplier may not be null");
			mZoneId = Objects.requireNonNull(zoneId, "The zoneid may not be null");
		}

		@Override
		public Calendar getCalendar() {
			return Objects.requireNonNull(mCalendarSupplier.get(), "The date supplier, " + mCalendarSupplier
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
				return new CalendarSupplierClock(mCalendarSupplier, zone);
			}
		}

	}

}

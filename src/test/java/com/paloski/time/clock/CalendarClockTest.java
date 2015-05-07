package com.paloski.time.clock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.Theory;
import org.junit.rules.ExpectedException;

public class CalendarClockTest {

	/**
	 * Performs tests on the
	 * {@link CalendarClock#ofSupplier(java.util.function.Supplier, ZoneId)}
	 * methods.
	 * 
	 * @author Adam Paloski
	 *
	 */
	public static class OfSupplierTests {

		@Rule
		public ExpectedException expected = ExpectedException.none();

		@Test
		public void nullSupplierThrows() {
			expected.expect(NullPointerException.class);
			expected.expectMessage("The calendar supplier may not be null");
			CalendarClock.ofSupplier(null, ZoneId.systemDefault());
		}

		@Test
		public void nullZoneIdThrows() {
			expected.expect(NullPointerException.class);
			expected.expectMessage("The zoneid may not be null");
			CalendarClock.ofSupplier(() -> Calendar.getInstance(), null);
		}

		@Test
		public void nullDateSupplierCausesNPE() {
			// No message, we let failure in null throw on its own, we clearly
			// state it must return non-null.
			expected.expect(NullPointerException.class);
			CalendarClock.ofSupplier(() -> null, ZoneId.systemDefault()).instant();
		}

		/**
		 * A test that ensures that any caching is done supplier side, not on
		 * ours.
		 */
		@Test
		public void testSupplierIsQueriedEveryInstantCall() {
			AtomicInteger callCount = new AtomicInteger();
			Supplier<Calendar> dateSupplier = () -> {
				callCount.incrementAndGet();
				return new Calendar.Builder()
									.setInstant(Instant.EPOCH.toEpochMilli())
								   .build();
			};
			Clock clock = CalendarClock.ofSupplier(dateSupplier, ZoneId.systemDefault());

			for (int x = 0; x < 10; ++x) {
				clock.instant();
			}

			assertEquals(10, callCount.get());
		}

		@Test
		public void testSupplierIsQueriedEveryMillisCall() {
			AtomicInteger callCount = new AtomicInteger();
			Supplier<Calendar> dateSupplier = () -> {
				callCount.incrementAndGet();
				return new Calendar.Builder()
									.setInstant(Instant.EPOCH.toEpochMilli())
								   .build();
			};
			Clock clock = CalendarClock.ofSupplier(dateSupplier, ZoneId.systemDefault());

			for (int x = 0; x < 10; ++x) {
				clock.millis();
			}

			assertEquals(10, callCount.get());
		}

		@Test
		public void clockIsSerializableIfSupplierIs() throws IOException {
			Supplier<Calendar> calendarSupplier = (Serializable & Supplier<Calendar>) () -> Calendar.getInstance();
			ObjectOutputStream oos = new ObjectOutputStream(new ByteArrayOutputStream());
			try {
				oos.writeObject(calendarSupplier);
			} catch (ObjectStreamException exp) {
				fail("Precondition failed: could not serialize supplier");
			}
			
			CalendarClock clock = CalendarClock.ofSupplier(calendarSupplier, ZoneId.systemDefault());
			
			try {
				oos.writeObject(clock);
			} catch (ObjectStreamException exp) {
				fail("Could not serialize CalendarSupplierClock even though the Supplier was serializable");
			}
		}
		
		@Test
		public void clockIsNotSerializableIfSupplierIsnt() throws IOException {
			Supplier<Calendar> calendarSupplier = (Supplier<Calendar>) () -> Calendar.getInstance();
			ObjectOutputStream oos = new ObjectOutputStream(new ByteArrayOutputStream());
			boolean serializationFailed = false;
			try {
				oos.writeObject(calendarSupplier);
			} catch (ObjectStreamException exp) {
				serializationFailed = true;
			}
			
			if(!serializationFailed) {
				fail("Test precondition failed: Lambda was successfully serialized");
			}
			
			CalendarClock clock = CalendarClock.ofSupplier(calendarSupplier, ZoneId.systemDefault());
			
			try {
				oos.writeObject(clock);
			} catch (ObjectStreamException exp) {
				//Serialization succeeded, return out.
				return;
			}
			fail("Successfully serialized clock based upon non-serializable lambda, this should not succeed");
		}
		
	}

	public static class GetDateTests {

		@Rule
		public ExpectedException expected = ExpectedException.none();

		/**
		 * A simple test date clock used in tests of this class
		 */
		private static abstract class TestCalendarClock extends CalendarClock {
			@Override
			public ZoneId getZone() {
				return ZoneId.systemDefault();
			}

			@Override
			public Clock withZone(ZoneId zone) {
				throw new UnsupportedOperationException();
			}
		}

		@Test
		public void returningNullFromGetDateThrows() {
			CalendarClock clock = new TestCalendarClock() {
				@Override
				protected Calendar getCalendar() {
					return null;
				}
			};
			expected.expect(NullPointerException.class);
			clock.instant();
		}

		/**
		 * Tests that our final methods at least don't modify the calendars
		 * returned, we can't check users.
		 */
		@Test
		public void dateReturnedByGetDateIsNotModified() {
			Calendar original = GregorianCalendar.from(ZonedDateTime.ofInstant(Instant.ofEpochMilli(34644612), ZoneOffset.UTC));
			Calendar originalClone = (Calendar) original.clone();
			// Make sure we're equal before in case someone did something weird
			// in the JVM
			assertEquals(original, originalClone);
			CalendarClock clock = new TestCalendarClock() {
				@Override
				protected Calendar getCalendar() {
					return original;
				}
			};
			clock.instant();
			clock.millis();
			assertEquals(original, originalClone);
		}

		@DataPoints
		public static Instant[] instants() {
			return new Instant[] { Instant.ofEpochMilli(0), Instant.ofEpochSecond(122231), Instant.ofEpochSecond(-12233),
					Instant.ofEpochSecond(1000000000) };
		}

		@Theory
		public void instantFromDateIsEqualToInstantGoingIn(Instant in) {
			Calendar original = GregorianCalendar.from(ZonedDateTime.ofInstant(in, ZoneOffset.UTC));
			CalendarClock clock = new TestCalendarClock() {
				@Override
				protected Calendar getCalendar() {
					return original;
				}
			};
			assertEquals(clock.instant(), in);
			assertEquals(clock.millis(), in.toEpochMilli());
		}

	}
	
}

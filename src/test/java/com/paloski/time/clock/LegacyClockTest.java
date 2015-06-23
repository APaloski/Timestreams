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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.TimeZone;

import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

/**
 * A set of Junit tests that test the {@link LegacyClock} class.
 * 
 * @author Adam Paloski
 *
 */
@RunWith(Enclosed.class)
public class LegacyClockTest {

	/**
	 * Tests Javadoc contracts to ensure that they are enforced.
	 */
	public static class TestContracts {

		@Rule
		public ExpectedException expectedException = ExpectedException.none();

		/**
		 * Tests the contract that this clock is serializable if the underlying
		 * clock is
		 */
		@Test
		public void testSerializableIfOtherIs() throws IOException {
			Clock clock = Clock.systemUTC();
			// Ensure this does NOT throw
			ObjectOutputStream oos = new ObjectOutputStream(new ByteArrayOutputStream());
			// Clock.systemUTC is required by contract to be serializable
			try {
				oos.writeObject(clock);
			} catch (Exception exp) {
				fail("Unit test attempted to run with invalid test case, raw clock was not serializable");
			}

			LegacyClock outsideClock = LegacyClock.of(clock);

			// This will throw if serialization fails
			oos.writeObject(outsideClock);
		}

		/**
		 * Tests the contract this this clock is only serializable if the
		 * underlying clock is
		 */
		@Test
		public void testNotSerializableIfOtherIsnt() throws IOException {
			Clock clock = SupplierClock.ofMillisecondSupplier(System::currentTimeMillis, ZoneId.systemDefault());
			// Ensure this does NOT throw
			ObjectOutputStream oos = new ObjectOutputStream(new ByteArrayOutputStream());
			boolean serializationFailed = false;

			try {
				oos.writeObject(clock);
			} catch (ObjectStreamException exp) {
				serializationFailed = true;
			}

			if (!serializationFailed) {
				fail("Unit test attempted to run with an invalid test case: raw clock was serializable when it was expected to not be");
			}

			expectedException.expect(ObjectStreamException.class);

			LegacyClock outsideClock = LegacyClock.of(clock);

			oos.writeObject(outsideClock);
		}

		/**
		 * Tests the requirement that the clock passed into
		 * {@link LegacyClock#of(Clock)} must not be null.
		 */
		@Test
		public void testNullClockExceptionsOnCreation() {
			expectedException.expect(NullPointerException.class);
			LegacyClock.of(null);
		}

	}

	/**
	 * Tests date time conversions to ensure that they behave as appropriate.
	 */
	@RunWith(Theories.class)
	public static class TestDateTime {

		@DataPoints
		public static long[] generateDataPoints() {
			return new long[] { 100, 9600, 34512, 48521, -95641, 1531, 1, 0, -1 };
		}

		@DataPoints
		public static String[] zones() {
			return ZoneId.getAvailableZoneIds().toArray(new String[0]);
		}

		/**
		 * Tests that the Date object created by {@link LegacyClock#toDate()} is
		 * equal to {@link Clock#instant()} on both the underlying and legacy
		 * clock
		 * 
		 * 
		 * @param offsetFromEpoch
		 *            The number of milliseconds offset from the Epoch this is.
		 * @param timeZone
		 *            A string constant representing the TimeZone that this
		 *            should use.
		 */
		@Theory
		public void testDateProducedIsEqual(long offsetFromEpoch, String timeZone) {
			Clock fixedPointUnderlyingClock = Clock.fixed(Instant.ofEpochMilli(offsetFromEpoch), ZoneId.of(timeZone));
			LegacyClock fixedLegacyClock = LegacyClock.of(fixedPointUnderlyingClock);

			// The date must equal the top level clocks instant
			assertEquals(fixedLegacyClock.instant(), fixedLegacyClock.toDate().toInstant());
			assertEquals(fixedPointUnderlyingClock.instant(), fixedLegacyClock.toDate().toInstant());
		}

		/**
		 * Tests that the Timestamp object created by
		 * {@link LegacyClock#toTimestamp()} is equal to {@link Clock#instant()}
		 * on both the underlying and legacy clock
		 * 
		 * 
		 * @param offsetFromEpoch
		 *            The number of milliseconds offset from the Epoch this is.
		 * @param timeZone
		 *            A string constant representing the TimeZone that this
		 *            should use.
		 */
		@Theory
		public void testTimestampProducedIsEqual(long offsetFromEpoch, String timeZone) {
			Clock fixedPointUnderlyingClock = Clock.fixed(Instant.ofEpochMilli(offsetFromEpoch), ZoneId.of(timeZone));
			LegacyClock fixedLegacyClock = LegacyClock.of(fixedPointUnderlyingClock);

			assertEquals(fixedLegacyClock.instant(), fixedLegacyClock.toTimestamp().toInstant());
			assertEquals(fixedPointUnderlyingClock.instant(), fixedLegacyClock.toTimestamp().toInstant());
		}

		/**
		 * Tests that the Calendar object created by
		 * {@link LegacyClock#toUTCCalendar()()} is equal to
		 * {@link Clock#instant()} on both the underlying and legacy clock and
		 * has a time zone of UTC
		 * 
		 * 
		 * @param offsetFromEpoch
		 *            The number of milliseconds offset from the Epoch this is.
		 * @param timeZone
		 *            A string constant representing the TimeZone that this
		 *            should use.
		 */
		@Theory
		public void noTimeZoneCalendarIsEqualToInstant(long offsetFromEpoch, String timeZone) {
			Clock fixedPointUnderlyingClock = Clock.fixed(Instant.ofEpochMilli(offsetFromEpoch), ZoneId.of(timeZone));
			LegacyClock fixedLegacyClock = LegacyClock.of(fixedPointUnderlyingClock);

			assertEquals(fixedLegacyClock.instant(), fixedLegacyClock.toUTCCalendar().toInstant());
			assertEquals(fixedPointUnderlyingClock.instant(), fixedLegacyClock.toUTCCalendar().toInstant());
			// Test the calendar TimeZone
			assertEquals(fixedLegacyClock.toUTCCalendar().getTimeZone(), TimeZone.getTimeZone("UTC"));
		}

		/**
		 * Tests that the Calendar object created by
		 * {@link LegacyClock#toZonedCalendar()} is equal to
		 * {@link Clock#instant()} on both the underlying and legacy clock
		 * 
		 * 
		 * @param offsetFromEpoch
		 *            The number of milliseconds offset from the Epoch this is.
		 * @param timeZone
		 *            A string constant representing the TimeZone that this
		 *            should use.
		 */
		@Theory
		public void clockTimeZoneCalendarIsEqualToInstant(long offsetFromEpoch, String timeZone) {
			Clock fixedPointUnderlyingClock = Clock.fixed(Instant.ofEpochMilli(offsetFromEpoch), ZoneId.of(timeZone));
			LegacyClock fixedLegacyClock = LegacyClock.of(fixedPointUnderlyingClock);

			assertEquals(fixedLegacyClock.instant(), fixedLegacyClock.toUTCCalendar().toInstant());
			assertEquals(fixedPointUnderlyingClock.instant(), fixedLegacyClock.toUTCCalendar().toInstant());
			// Test the calendar TimeZone
			assertEquals(fixedLegacyClock.toZonedCalendar().getTimeZone(), TimeZone.getTimeZone(fixedLegacyClock.getZone()));
		}

		/**
		 * Tests that given an object the EXACT same results as those of the
		 * underlying object are used.
		 * 
		 * @param offsetFromEpoch
		 *            The number of milliseconds offset from the Epoch this is.
		 * @param timeZone
		 *            A string constant representing the TimeZone that this
		 *            should use.
		 */
		@Theory
		public void testUnderlyingObjectIsUsed(long offsetFromEpoch, String timeZone) {
			Clock fixedPointUnderlyingClock = Clock.fixed(Instant.ofEpochMilli(offsetFromEpoch), ZoneId.of(timeZone));
			LegacyClock fixedLegacyClock = LegacyClock.of(fixedPointUnderlyingClock);

			assertEquals(fixedLegacyClock.getZone(), fixedPointUnderlyingClock.getZone());
			assertEquals(fixedLegacyClock.instant(), fixedPointUnderlyingClock.instant());
			assertEquals(fixedLegacyClock.millis(), fixedPointUnderlyingClock.millis());
		}

		/**
		 * Tests that the TimeZone returned by {@link LegacyClock#getTimeZone()}
		 * matches the ZoneId returned by {@link LegacyClock#getZone()}
		 * 
		 * @param offsetFromEpoch
		 *            The number of milliseconds offset from the Epoch this is.
		 * @param timeZone
		 *            A string constant representing the TimeZone that this
		 *            should use.
		 */
		@Theory
		public void testGetTimeZoneIsCorrect(long offsetFromEpoch, String timeZone) {
			Clock fixedPointUnderlyingClock = Clock.fixed(Instant.ofEpochMilli(offsetFromEpoch), ZoneId.of(timeZone));
			LegacyClock fixedLegacyClock = LegacyClock.of(fixedPointUnderlyingClock);

			assertEquals(fixedLegacyClock.getTimeZone().toZoneId(), fixedLegacyClock.getZone());
		}

		/**
		 * Tests that the ZoneId of the clock is correct after setting it via
		 * {@link LegacyClock#withTimeZone(TimeZone)}
		 * 
		 * @param offsetFromEpoch
		 *            The number of milliseconds offset from the Epoch this is.
		 * @param timeZone
		 *            A string constant representing the TimeZone that this
		 *            should use.
		 */
		@Theory
		public void testWithTimeZone(long offsetFromEpoch, String timeZone) {
			Clock fixedPointUnderlyingClock = Clock.fixed(Instant.ofEpochMilli(offsetFromEpoch), ZoneId.of("UTC"));
			LegacyClock fixedLegacyClock = LegacyClock.of(fixedPointUnderlyingClock);

			ZoneId zoneIdOfParam = ZoneId.of(timeZone);
			LegacyClock newTimeZonedClock = fixedLegacyClock.withTimeZone(TimeZone.getTimeZone(zoneIdOfParam));

			assertEquals(zoneIdOfParam, newTimeZonedClock.getZone());
		}
	}

}

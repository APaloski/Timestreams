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
package com.paloski.time.stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.Period;
import java.time.Year;
import java.time.YearMonth;
import java.time.temporal.TemporalAmount;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

/**
 * Contains tests for the {@link TemporalStreams} class.
 * 
 * Each static method or static class should have its own subclass in here that
 * preforms tests for that method/class.
 * 
 * @author Adam Paloski
 *
 */
@RunWith(Enclosed.class)
public class TemporalStreamsTest {

	/**
	 * A class container for tests relating to the
	 * {@link TemporalStreams.Builder} class.
	 * 
	 * TODO These tests May need to be expanded.
	 */
	@RunWith(Theories.class)
	public static class TestBuilder {

		@Rule
		public ExpectedException expectedException = ExpectedException.none();
		
		/* ----- Tests that nulls are not accepted ----- */
		/**
		 * Checks that {@link TemporalStreams.TemporalStreamBuilder#every(java.time.temporal.TemporalAmount)} null pointers on a null argument 
		 */
		@Test
		public void testEveryDoesntAcceptNulls() {
			expectedException.expect(NullPointerException.class);
			TemporalStreams.builder().every(null);
		}
		
		/**
		 * Checks that {@link TemporalStreams.TemporalStreamBuilder#from(java.time.temporal.Temporal)} null pointers on a null argument 
		 */
		@Test
		public void testFromDoesntAcceptNulls() {
			expectedException.expect(NullPointerException.class);
			TemporalStreams.builder().from(null);
		}
		
		/**
		 * Checks that {@link TemporalStreams.TemporalStreamBuilder#until(java.time.temporal.Temporal)} null pointers on a null argument 
		 */
		@Test
		public void testUntilDoesntAcceptNulls() {
			expectedException.expect(NullPointerException.class);
			TemporalStreams.builder().until(null);
		}
		
		@DataPoints
		public static LocalDate[] getDateDataPoints() {
			return new LocalDate[] {
				LocalDate.of(1991, 1, 2),
				LocalDate.of(1993, 6, 12),
				LocalDate.of(2000, 1, 1),
				LocalDate.of(1999, 12, 31)
			};
		}
		
		@DataPoints
		public static TemporalAmount[] getAmountDataPoints() {
			return new TemporalAmount[] {
					Period.ofDays(1),
					Period.ofWeeks(2),
					Period.ofDays(6),
					Period.ofYears(1)
			};
		}
		
		@Theory
		public void testNonParallelizedStreamCreatesExpectedEntries(LocalDate start, LocalDate end, TemporalAmount incrementAmount) {
			assumeTrue(start.isBefore(end));
			List<LocalDate> datesFromStream = new TemporalStreams.TemporalStreamBuilder<LocalDate>()
																   .every(incrementAmount)
																   .from(start)
																   .until(end)
																   .stream()
																   .collect(Collectors.toList());
			List<LocalDate> datesFromIteration = new ArrayList<>();
			for(LocalDate currentPt = start; end.isAfter(currentPt); currentPt = currentPt.plus(incrementAmount)) {
				datesFromIteration.add(currentPt);
			}
			
			assertEquals(datesFromIteration, datesFromStream);

		}
		
		@Theory
		public void testParallelizedStreamCreatesExpectedEntries(LocalDate start, LocalDate end, TemporalAmount incrementAmount) {
			assumeTrue(start.isBefore(end));
			List<LocalDate> datesFromStream = new TemporalStreams.TemporalStreamBuilder<LocalDate>()
																   .every(incrementAmount)
																   .from(start)
																   .until(end)
																   .parallelStream()
																   .collect(Collectors.toList());
			List<LocalDate> datesFromIteration = new ArrayList<>();
			for(LocalDate currentPt = start; end.isAfter(currentPt); currentPt = currentPt.plus(incrementAmount)) {
				datesFromIteration.add(currentPt);
			}
			assertEquals(datesFromIteration, datesFromStream);
		}
	}

	/**
	 * Contains tests for the {@link TemporalStreams#everyDayInYear(Year)}
	 * method.
	 * 
	 * @author Adam Paloski
	 *
	 */
	@RunWith(Theories.class)
	public static class TestEveryDayInYear {

		@DataPoints
		public static int[] getYears() {
			return new int[] { 1000, 1996, 2000, 1984, 1963, 1972, 1954 };
		}

		@Theory
		public void testNonLeapYearsHave365Entries(int year) {
			Year yearAsObj = Year.of(year);
			assumeTrue(!yearAsObj.isLeap());
			assertEquals(365, TemporalStreams.everyDayInYear(yearAsObj).count());
		}

		@Theory
		public void testLeapYearsHave366Entries(int year) {
			Year yearAsObj = Year.of(year);
			assumeTrue(yearAsObj.isLeap());
			assertEquals(366, TemporalStreams.everyDayInYear(yearAsObj).count());
		}

		/**
		 * This test ensures that when collected, proper order is maintained for
		 * the stream.
		 */
		@Theory
		public void testAllDaysAreSequentialWithoutParallel(int year) {
			Year yearAsObj = Year.of(year);
			List<LocalDate> knownValues = getAllDatesInYearAsList(yearAsObj);
			List<LocalDate> resultValues = TemporalStreams.everyDayInYear(yearAsObj).collect(Collectors.toList());
			assertEquals(knownValues, resultValues);
		}

		@Theory
		public void testAllDaysAreSequentialInParallel(int year) {
			Year yearAsObj = Year.of(year);
			List<LocalDate> knownValues = getAllDatesInYearAsList(yearAsObj);
			List<LocalDate> resultValues = TemporalStreams.everyDayInYear(yearAsObj).parallel()
					.collect(Collectors.toCollection(() -> Collections.synchronizedList(new ArrayList<>())));
			assertEquals(knownValues, resultValues);
		}

		/**
		 * Tests that a null argument produces an exception
		 */
		@Test(expected = NullPointerException.class)
		public void testInvalidArgumentCheck() {
			getAllDatesInYearAsList(null);
		}

		private static List<LocalDate> getAllDatesInYearAsList(Year yearAsObj) {
			List<LocalDate> knownValues = new ArrayList<>(366);
			LocalDate lastDay = yearAsObj.plusYears(1).atDay(1);
			for (LocalDate current = yearAsObj.atDay(1); !current.equals(lastDay); current = current.plusDays(1)) {
				knownValues.add(current);
			}
			return knownValues;
		}
	}

	/**
	 * Contains tests for the {@link TemporalStreams#allMonths()} method.
	 * 
	 * @author Adam Paloski
	 *
	 */
	public static class TestAllMonths {

		@Test
		public void testStreamHas12Elements() {
			assertEquals(12, TemporalStreams.allMonths().count());
		}

		/**
		 * This test ensures that as long as the {@link Month} enumeration is in
		 * proper order, so are we, because we match their order.
		 */
		@Test
		public void testMonthOrderMatchesEnumerationOrder() {
			assertTrue(
					"The ordering of the months in the Month enumeration must match the order coming out of the TemporalStreams allMonths stream",
					Arrays.equals(Month.values(), TemporalStreams.allMonths().toArray()));
		}

	}

	/**
	 * Contains tests for the {@link TemporalStreams#everyMonthInYear(Year)} method.
	 * 
	 * @author Adam Paloski
	 *
	 */
	@RunWith(Theories.class)
	public static class TestEveryMonthInYear {

		@DataPoints
		public static int[] getYears() {
			return new int[] { 832, 2050, 2020, 2017, 1944, 1958, 1933, 2015, 2016, 2014 };
		}

		@Theory
		public void testEveryYearHas12Months(int year) {
			assertEquals(12, TemporalStreams.everyMonthInYear(Year.of(year)).count());
		}

		@Theory
		public void testAllMonthsAreInOrderAndPresent(int year) {
			List<YearMonth> monthsFromIteration = new ArrayList<YearMonth>();
			for (YearMonth current = Year.of(year).atMonth(Month.JANUARY); current.isBefore(Year.of(year + 1).atMonth(Month.JANUARY)); current = current
					.plusMonths(1)) {
				monthsFromIteration.add(current);
			}
			List<YearMonth> monthsFromStream = TemporalStreams.everyMonthInYear(Year.of(year)).collect(Collectors.toList());
			Iterator<YearMonth> monthIter = monthsFromStream.iterator();
			Month prev = monthIter.next().getMonth();
			while (monthIter.hasNext()) {
				Month next = monthIter.next().getMonth();
				assertEquals(String.format("Months must be in order: %s came before %s in ordering", prev, next), prev.getValue() + 1,
						next.getValue());
				prev = next;
			}
			assertEquals(monthsFromIteration, monthsFromStream);
			assertEquals(monthsFromStream.get(0).getMonth(), Month.JANUARY);
			assertEquals(monthsFromStream.get(12 - 1 /* Account for 0 indexing */).getMonth(), Month.DECEMBER);
		}

		@Theory
		public void testEachYearMonthIsOfExpectedYear(final int year) {
			assertTrue("All MonthDays in everyMonthInYear must match the passed in year", TemporalStreams.everyMonthInYear(Year.of(year))
					.allMatch(streamYearMonth -> streamYearMonth.getYear() == year));
		}

	}

	/**
	 * Contains tests for the {@link TemporalStreams#allHoursOnDay()} method.
	 * 
	 * @author Adam Paloski
	 *
	 */
	@RunWith(Theories.class)
	public static class TestAllHoursOnDay {

		@DataPoints
		public static LocalDate[] getLocalDates() {
			return new LocalDate[] { LocalDate.of(100, 6, 19), LocalDate.of(1996, 8, 1), LocalDate.of(2015, 12, 12),
					LocalDate.of(2019, 6, 22), LocalDate.of(1985, 5, 25), LocalDate.of(1852, 11, 5), LocalDate.of(1654, 6, 9),
					LocalDate.of(1532, 7, 7), LocalDate.of(1985, 2, 17), LocalDate.of(1974, 1, 19) };
		}

		@Theory
		public void testEachDayHasTwentyFourHours(LocalDate day) {
			assertEquals(24, TemporalStreams.allHoursInDay(day).count());
		}

		@Theory
		public void testEachHourIsSequential(LocalDate day) {
			//Create a list of known values from normal iteration.
			List<LocalDateTime> iterationTimes = new ArrayList<>();
			for(LocalDateTime dateTime = day.atStartOfDay();dateTime.isBefore(day.plusDays(1).atStartOfDay()); dateTime = dateTime.plusHours(1)) {
				iterationTimes.add(dateTime);
			}
			
			List<LocalDateTime> streamTimes = TemporalStreams.allHoursInDay(day).collect(Collectors.toList());
			//This performs a test on them being sequential because the iterationTimes list provided a sequential list and list equality guarantees this does also.
			assertEquals(iterationTimes, streamTimes);
		}
		
		@Theory
		public void testEachHourIsAtStart(LocalDate day) {
			TemporalStreams.allHoursInDay(day).forEach(dateTime -> {
				assertEquals(0, dateTime.getMinute());
				assertEquals(0, dateTime.getSecond());
				assertEquals(0, dateTime.getNano());
			});
		}

	}

	/**
	 * Contains tests for the {@link TemporalStreams#allHoursInAnyDay()} method.
	 * 
	 * @author Adam Paloski
	 *
	 */
	public static class TestAllHoursInDay {

		@Test
		public void test24HoursAreProduced() {
			assertEquals(24, TemporalStreams.allHoursInAnyDay().count());
		}

		@Test
		public void testStartsAtMidnight() {
			assertEquals(LocalTime.MIDNIGHT, TemporalStreams.allHoursInAnyDay().findFirst().get());
		}
		
		@Test
		public void testEachHourIsAtStart() {
			TemporalStreams.allHoursInAnyDay().forEach(time -> {
				assertEquals(0, time.getSecond());
				assertEquals(0, time.getMinute());
				assertEquals(0, time.getNano());
			});
		}

	}

}

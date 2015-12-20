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

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.Period;
import java.time.Year;
import java.time.YearMonth;
import java.time.chrono.IsoChronology;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAmount;
import java.time.temporal.TemporalUnit;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * A class that contains a number of static utilities for creating a Stream of
 * Temporal Objects.
 * <p>
 * The Streams created from the static methods in this class, or from a
 * {@link TemporalStreamBuilder builder}, will all contain the following
 * properties, unless otherwise specified:
 * <ul>
 * <li>
 * The streams will create elements lazily. That is to say that elements
 * will be created on an as-needed basis, so operations that terminate early
 * such as {@link Stream#findFirst()} may generate only as many items as needed.
 * </li>
 * <p>
 * <li>
 * The streams will be ordered chronologically, such that when running an
 * ordered Stream, element {@code n} will be the next
 * element chronologically to element {@code n-1} and will be the preceding
 * chronological element to element {@code n+1}. So for instance in
 * {@link #everyDayInYear(Year)} July 4th will always directly follow July 3rd and be followed by
 * July 5th.
 * </li>
 * </ul>
 *
 * @author Adam Paloski
 */
public class TemporalStreams {

	/**
	 * Returns a lazily populated Stream whose elemnts are the days of this year, in the ISO
	 * chronology, from January 1st, {@code year} until December 31st, {@code year}.
	 * <p>
	 * The first element of the resulting stream will be a LocalDate object
	 * representing January 1st, {@code year}, and the last element will be
	 * December 31st, {@code year}, with each element between being the
	 * sequential next day of the year.
	 *
	 * @param year
	 * 		The year to stream over, this must not be null.
	 *
	 * @return A new Stream of LocalDate's that contains every day in the year
	 * {@code year}
	 */
	public static Stream<LocalDate> everyDayInYear(Year year) {
		return new TemporalStreamBuilder<LocalDate>().every(Period.ofDays(1))
													 .from(year.atDay(1))
													 .until(year.plusYears(1).atDay(1))
													 .stream();
	}

	/**
	 * Returns an eagerly populated Stream whose elements are the set of every month contained
	 * within the {@link IsoChronology}, as represented by the {@link Month}
	 * enumeration.
	 * <p>
	 * This method is logically equivalent to {@code Arrays.stream(Month.values())}, but allows all
	 * Streams relating to Date & Time to be accessed from one place.
	 *
	 * @return A new Stream that iterates over every month in a standard ISO
	 * year.
	 */
	public static Stream<Month> allMonths() {
		return Arrays.stream(Month.values());
	}

	/**
	 * Returns a lazily populated Stream that whose elements are the set of every month contained
	 * in
	 * {@link Month}, associated with {@code year} to create a YearMonth object.
	 * <p>
	 * The resulting stream will iterate over the set of YearMonth objects
	 * occurring between January in {@code year} and January in {@code year + 1}.
	 *
	 * @param year
	 * 		The year in which the return stream should create YearMonth objects
	 *
	 * @return A new Stream that iterates over each Month in the year
	 * {@code year}.
	 */
	public static Stream<YearMonth> everyMonthInYear(Year year) {
		return new TemporalStreamBuilder<YearMonth>().every(Period.ofMonths(1))
													 .from(YearMonth.of(year.getValue(), Month.JANUARY))
													 .until(YearMonth.of(year.getValue() + 1, Month.JANUARY))
													 .stream();
	}

	/**
	 * Returns a lazily populated Stream whose elements are LocalDateTimes of each hour in {@code
	 * day}
	 * <p>
	 * The created Stream iterators from midnight on {@code day} until midnight
	 * of the next day (excluded from stream).
	 *
	 * @param day
	 * 		The day that should have its hours iterated over.
	 *
	 * @return A new Stream that iterates over all hours in a given day.
	 *
	 * @see #allHoursInDay for the LocalTime equivalent to this method
	 */
	public static Stream<LocalDateTime> allHoursInDay(LocalDate day) {
		return new TemporalStreamBuilder<LocalDateTime>().every(Duration.ofHours(1))
														 .from(day.atStartOfDay())
														 .until(day.plusDays(1).atStartOfDay())
														 .stream();
	}

	/**
	 * Returns a lazily populated Stream that iterates over all hours in any day of the ISO
	 * Chronology.
	 * <p>
	 * The created Stream iterates (in order) from Midnight to Midnight
	 * (excluded from the stream).
	 *
	 * @return A new Stream that iterates over all hours in any day.
	 */
	public static Stream<LocalTime> allHoursInAnyDay() {

		/*
		 * Implementation note: we do this because we want to use the property
		 * of not being forced into a range, and because if we set both to
		 * midnight it will not work as midnight == midnight.
		 * 
		 * So we end at 23 59 instead of midnight, meaning the last node is
		 * AFTER 11pm, but before the next full hour, which gets it included.
		 */
		return new TemporalStreamBuilder<LocalTime>().every(Duration.ofHours(1))
													 .from(LocalTime.MIDNIGHT)
													 .until(LocalTime.of(23, 59))
													 .stream();

	}

	/**
	 * Creates a new TemporalStreamBuilder that can be used to build a Stream
	 * that evaluates all values between two points in time, using a set
	 * incremental amount to determine which entries should be found.
	 *
	 * @param <T>
	 * 		The implementation of Temporal that the Stream should be based
	 * 		in.
	 *
	 * @return A new TemporalStreamBuilder with no initialized values.
	 *
	 * @see TemporalStreamBuilder for more details.
	 */
	public static <T extends Temporal & Comparable<? super T>> TemporalStreamBuilder<T> builder() {
		return new TemporalStreamBuilder<>();
	}

	/**
	 * A Builder class for creating a Stream that ranges between two points in
	 * Time, incrementing a set amount for each entry.
	 * <p>
	 * This builder has three essential methods that <em>must</em> be called
	 * before a building operation can be called:
	 * <ul>
	 * <li>{@link #every(TemporalAmount)} to set the amount of time between each
	 * entry the stream can take</li>
	 * <li>{@link #from(Temporal)} to set the starting point of the stream, this
	 * value is <em>inclusive</em></li>
	 * <li>{@link #until(Temporal)} to set the ending point of the stream, this
	 * value is <em>exclusive</em></li>
	 * </ul>
	 * After the above methods have been satisfied with valid criteria then the
	 * builder may have one of the two following building operations called:
	 * <ul>
	 * <li>{@link #stream()} to create a Stream that is not parallelized</li>
	 * <li>{@link #parallelStream()} to create a Stream that is parallelized</li>
	 * </ul>
	 * <p>
	 * Note that if the amount of time between the values set in {@code from}
	 * and {@code until} cannot be equally divided by the amount set in
	 * {@code every} then the amount will be added to from until it reaches a
	 * value greater than or equal to until. So in the case of:
	 * <p>
	 * <pre>
	 * {@code
	 * 	List<LocalDate> dates = TemporalStreams.builder()
	 * 			.every(Period.ofDays(5))
	 * 			.from(LocalDate.now())
	 * 			.until(LocalDate.now().plus(1, ChronoUnit.WEEKS))
	 * 			.stream()
	 * 			.collect(Collectors.toList());
	 * }
	 * </pre>
	 * <p>
	 * dates would contain 2 entries, {@link LocalDate#now() now} and 5 days
	 * from now, ignoring the last 2 days because they did not fall inside of
	 * the next 5 day period.
	 * <p>
	 * Typical use of this class amounts to like the following, which creates a
	 * list of all dates that are not Saturday from today until a year from day:
	 * <p>
	 * <pre>
	 * {@code
	 * 	List<LocalDate> dates = TemporalStreams.builder()
	 * 			.every(Period.ofDays(1))
	 * 			.from(LocalDate.now())
	 * 			.until(LocalDate.now().plus(1, ChronoUnit.YEARS))
	 * 			.parallelStream()
	 * 			.filter(date -> date.getDayOfWeek() != DayOfWeek.SATURDAY)
	 * 			.collect(Collectors.toList());
	 * }
	 * </pre>
	 * <h3>Duration vs Period vs Other implementations of {@link TemporalAmount}
	 * </h3>
	 * TemporalStreamBuilder allows, in the same way the normal Temporal API
	 * does, a generic passing of a TemporalAmount instead of requiring a Period
	 * or Duration to be passed, depending on if the Temporal object is date or
	 * time based. This can cause two issues:
	 * <ol>
	 * <li>It can fail when {@link Temporal#plus(TemporalAmount)} is invoked if
	 * all of the {@link TemporalAmount#getUnits() units} of the TemporalAmount
	 * are not valid for the invocation of plus. This case is handled at Stream
	 * construction time.</li>
	 * <p>
	 * <li>It can fail in subtle ways, because of round off issues. For
	 * instance, when obtaining the duration from
	 * {@link TemporalUnit#getDuration()}, which may or may not be estimated
	 * based upon {@link TemporalUnit#isDurationEstimated()}. This applies to
	 * all Durations obtained from a {@link ChronoUnit} above
	 * {@link ChronoUnit#DAYS Days}. The Years chrono unit for instance reports
	 * 365.2425, which is on average correct, but is less accurate than using
	 * {@code Period.ofYears(1)}</li>
	 * </ol>
	 * In general, most issues can be avoided by attempting to as closely as
	 * possible match your TemporalAmount to the streaming type you wish to
	 * stream upon.
	 * <p>
	 * <p>
	 * <h3>Dealing with Circular Temporals</h3>
	 * Circular temporal objects, or objects that loop from one value back into
	 * itself like {@link LocalTime} does can be problematic. Internally, a
	 * TemporalStream uses {@link Comparable natural ordering} to determine its
	 * place in the streaming, because of this if you attempt to start and end
	 * on the same point you will receive a Stream with 0 elements. So
	 * attempting to view every hour of the day, as is done in
	 * {@link TemporalStreams#allHoursInAnyDay()} cannot start and end at
	 * {@link LocalTime#MIDNIGHT}, the end must occur <em>before</em> the
	 * following midnight, or else it will evaluate the start to be equal to the
	 * end and report 0 elements. To deal with this in the general case, prefer
	 * to use a Date and Time based object such as {@link LocalDateTime}, then
	 * map to a the equivalent Time object.
	 * <p>
	 *
	 * @param <T>
	 * 		The type of Temporal that the Stream produced by this Builder
	 * 		should iterate over. This type must adhere to the following
	 * 		requirements:
	 * 		<ul>
	 * 		<li> Note that this <em>must</em> be a type that can be
	 * 		handled by the TemporalAmount set in
	 * 		{@link #every(TemporalAmount)}. See {@link #stream()} for more
	 * 		details.</li>
	 * 		<p>
	 * 		<li> <em>This type is assumed to be Immutable, as per the
	 * 		recommendation in {@link Temporal}, when using this class with
	 * 		a value that is not immutable the value passed to
	 * 		{@link #from(Temporal)} and {@link #until(Temporal)} must not
	 * 		be changed, or unpredictable results will occur</em></li>
	 * 		<p>
	 * 		<li>This type must be Comparable, as described in the contract
	 * 		of {@link Temporal}</li>
	 * 		</ul>
	 *
	 * @author Adam Paloski
	 */
	public static class TemporalStreamBuilder<T extends Temporal & Comparable<? super T>> {

		private T mStartingPoint;
		private T mEndingPoint;
		private TemporalAmount mIncrementAmount;

		/**
		 * Sets the amount that each element of the constructed stream will be
		 * separated by, for instance calling this method with
		 * {@code Period.ofDays(1)} will cause an element to be present in the
		 * stream for every day between the value set at {@link #from(Temporal)}
		 * and the value set at {@link #until(Temporal)}.
		 * <p>
		 * If this value is not set when {@link #stream()} or
		 * {@link #parallelStream()} is called, an exception will be thrown.
		 * <p>
		 * <em>If type of {@code amount} is not immutable and changes during
		 * streaming operations the behavior of the stream is undefined.</em>
		 *
		 * @param amount
		 * 		A non-null TemporalAmount that can be added to the generic
		 * 		Temporal type of this TemporalStreamBuilder.
		 *
		 * @return {@code this} object for use in chained calls.
		 */
		public TemporalStreamBuilder<T> every(TemporalAmount amount) {
			mIncrementAmount = Objects.requireNonNull(amount);
			return this;
		}

		/**
		 * Sets the starting point of the Stream produced by this builder, the
		 * value set in this function is <em>inclusive</em> and will be the
		 * first element of the returned stream.
		 * <p>
		 * If this value is not set when {@link #stream()} or
		 * {@link #parallelStream()} is called, an exception will be thrown.
		 *
		 * @param startPoint
		 * 		A non-null Temporal value that represents the starting
		 * 		point in the Stream.
		 *
		 * @return {@code this} object for use in chained calls.
		 */
		public TemporalStreamBuilder<T> from(T startPoint) {
			mStartingPoint = Objects.requireNonNull(startPoint);
			return this;
		}

		/**
		 * Sets the ending point of the Stream produced by this builder, the
		 * value set in this function is <em>exclusive</em>, and will not be
		 * included in the stream.
		 * <p>
		 * If this value is not set when {@link #stream()} or
		 * {@link #parallelStream()} is called, an exception will be thrown.
		 *
		 * @param endingPoint
		 * 		A non-null Temporal value that represents the ending limit
		 * 		of the stream, this value and any values that would occur
		 * 		passed it in chronological order will <em>not</em> occur
		 * 		in the stream.
		 *
		 * @return {@code this} object for use in chained calls.
		 */
		public TemporalStreamBuilder<T> until(T endingPoint) {
			mEndingPoint = Objects.requireNonNull(endingPoint);
			return this;
		}

		/**
		 * Creates a new, non-parallel Stream from the parameters set via
		 * {@link #every(TemporalAmount)}, {@link #from(Temporal)} and
		 * {@link #until(Temporal)}.
		 * <p>
		 * This method will throw if one of two cases are true:
		 * <ul>
		 * <li>If the methods {@code from}, {@code until} or {@code every} were
		 * never set</li>
		 * <li>If the value set for {@code every} throws when
		 * {@link TemporalAmount#addTo(Temporal)} with the type of T throws</li>
		 * </ul>
		 *
		 * @return A new Stream that streams values between the values set in
		 * {@link #from(Temporal)} (inclusive), and
		 * {@link #until(Temporal)} (exclusive).
		 */
		public Stream<T> stream() {
			return StreamSupport.stream(new TemporalSpliterator<>(mStartingPoint, mEndingPoint, mIncrementAmount), false);
		}

		/**
		 * Creates a new, parallel Stream from the parameters set via
		 * {@link #every(TemporalAmount)}, {@link #from(Temporal)} and
		 * {@link #until(Temporal)}.
		 * <p>
		 * This method will throw if one of two cases are true:
		 * <ul>
		 * <li>If the methods {@code from}, {@code until} or {@code every} were
		 * never set</li>
		 * <li>If the value set for {@code every} throws when
		 * {@link TemporalAmount#addTo(Temporal)} with the type of T throws</li>
		 * </ul>
		 *
		 * @return A new, parallel, Stream that streams values between the
		 * values set in {@link #from(Temporal)} (inclusive), and
		 * {@link #until(Temporal)} (exclusive).
		 */
		public Stream<T> parallelStream() {
			return StreamSupport.stream(new TemporalSpliterator<>(mStartingPoint, mEndingPoint, mIncrementAmount), true);
		}

	}

}

package com.paloski.time.stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.Period;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAmount;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Spliterator;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.hamcrest.core.IsNot;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

@RunWith(Enclosed.class)
public class TemporalSpliteratorTest {

	/**
	 * A Set of tests that make sure that factory construction methods for a
	 * TemporalSpliterator all properly check their nullity contracts.
	 */
	public static class TemporalSpliteratorNullPointerTests {
		@Rule
		public ExpectedException expectedThrownException = ExpectedException.none();

		/**
		 * Tests that if the first value passed into
		 * {@link TemporalSpliterator#sortedSpliterator(java.time.temporal.Temporal, java.time.temporal.Temporal, java.time.temporal.TemporalAmount)}
		 * is null the function call throws a {@link NullPointerException}
		 */
		@Test
		public void ensureNullStartValueForSortedSpliteratorNoExplicitComparatorThrows() {
			expectedThrownException.expect(NullPointerException.class);
			expectedThrownException.expectMessage("The starting point of a TemporalSpliterator may not be null");
			new TemporalSpliterator<LocalDate>(null, LocalDate.now().plus(1, ChronoUnit.DAYS), Duration.ofHours(1));
		}

		/**
		 * Tests that if the second value passed into
		 * {@link TemporalSpliterator#sortedSpliterator(java.time.temporal.Temporal, java.time.temporal.Temporal, java.time.temporal.TemporalAmount)}
		 * is null the function call throws a {@link NullPointerException}
		 */
		@Test
		public void ensureNullEndValueForSortedSpliteratorNoExplicitComparatorThrows() {
			expectedThrownException.expect(NullPointerException.class);
			expectedThrownException.expectMessage("The ending point of a TemporalSpliterator may not be null");
			new TemporalSpliterator<LocalDate>(LocalDate.now(), null, Duration.ofHours(1));
		}

		/**
		 * Tests that if the third value passed into
		 * {@link TemporalSpliterator#sortedSpliterator(java.time.temporal.Temporal, java.time.temporal.Temporal, java.time.temporal.TemporalAmount)}
		 * is null the function call throws a {@link NullPointerException}
		 */
		@Test
		public void ensureNullIncrementAmountForSortedSpliteratorNoExplicitComparatorThrows() {
			expectedThrownException.expect(NullPointerException.class);
			expectedThrownException.expectMessage("The incrementing amount of a TemporalSpliterator may not be null");
			new TemporalSpliterator<LocalDate>(LocalDate.now(), LocalDate.now().plus(1, ChronoUnit.DAYS), null);
		}

	}

	/**
	 * A Set of tests that checks that a TemporalSpliterator hits all values
	 * expected to be hit for differing types of Temporal objects
	 * 
	 * @author Adam Paloski
	 */
	@RunWith(Theories.class)
	public static class TemporalSpliteratorIncrementTests {

		@DataPoints
		public static LocalDate[] getLocalDataPoints() {
			return new LocalDate[] { LocalDate.of(1169, 1, 15), LocalDate.of(1263, 6, 4),
					LocalDate.of(2015, 5, 3),
					LocalDate.of(2016, 6, 9)};
		}

		@DataPoints
		public static LocalTime[] getLocalTimeDataPoints() {
			return new LocalTime[] { 
				LocalTime.MIDNIGHT,
				LocalTime.NOON,
				LocalTime.of(6, 32),
				LocalTime.of(12, 23),
				LocalTime.of(17, 00),
				LocalTime.of(19, 36)
			};
		}
		
		@DataPoints
		public static YearMonth[] getYearMonthDataPoints() {
			return new YearMonth[] {
				YearMonth.of(1963, Month.JANUARY),
				YearMonth.of(2015, Month.SEPTEMBER),
				YearMonth.of(2111, Month.AUGUST),
				YearMonth.of(1911, Month.FEBRUARY),
				YearMonth.of(1111, Month.MAY)
			};
		}
		
		
		@Theory
		public void testAllDaysBetween(LocalDate start, LocalDate end) {
			Assume.assumeTrue(start.isBefore(end));
			final Period oneDay = Period.ofDays(1);
			final TemporalSpliterator<LocalDate> spliterator = new TemporalSpliterator<LocalDate>(start, end, oneDay);
			List<LocalDate> fromNormalIteration = new ArrayList<>();

			for (LocalDate current = start; current.isBefore(end); current = current.plus(oneDay)) {
				fromNormalIteration.add(current);
			}
			List<LocalDate> fromSpliterator = new ArrayList<>();
			spliterator.forEachRemaining(fromSpliterator::add);

			// Now then, these MUST be equal, then we can check ordering on the
			// list from the spliterator
			Assert.assertEquals(fromNormalIteration, fromSpliterator);

			// Test the ordering of each date
			LocalDate previous = start.minus(oneDay);
			for (LocalDate current : fromSpliterator) {
				Assert.assertTrue(String.format("Previous date [%s] must be BEFORE the current date [%s]", previous, current), previous.isBefore(current));
				previous = current;
			}

			Assert.assertEquals(fromSpliterator.get(0), start);
			Assert.assertEquals(fromSpliterator.get(fromSpliterator.size() - 1).plus(oneDay), end);
		}

		@Theory
		public void testAllTimesBetween(LocalTime start, LocalTime end) {
			Assume.assumeTrue(start.isBefore(end));
			final Duration oneMinute = Duration.ofMinutes(1);
			final TemporalSpliterator<LocalTime> spliterator = new TemporalSpliterator<LocalTime>(start, end, oneMinute);
			List<LocalTime> fromNormalIteration = new ArrayList<>();

			for (LocalTime current = start; current.isBefore(end); current = current.plus(oneMinute)) {
				fromNormalIteration.add(current);
			}
			List<LocalTime> fromSpliterator = new ArrayList<>();
			
			spliterator.forEachRemaining(fromSpliterator::add);

			// Now then, these MUST be equal, then we can check ordering on the
			// list from the spliterator
			Assert.assertEquals(fromNormalIteration, fromSpliterator);

			// Test the ordering of each date
			LocalTime previous = null;
			for (LocalTime current : fromSpliterator) {
				//Skip the first entry
				if(previous != null) {
					Assert.assertTrue(String.format("Previous time [%s] must be BEFORE the current time [%s]", previous, current), previous.isBefore(current));
					
				}
				previous = current;
			}

			Assert.assertEquals(fromSpliterator.get(0), start);
			Assert.assertEquals(fromSpliterator.get(fromSpliterator.size() - 1).plus(oneMinute), end);
		}
		
		@Theory
		public void testAllYearMonthsBetween(YearMonth start, YearMonth end) {
				Assume.assumeTrue(start.isBefore(end));
				final TemporalAmount oneMonth = Period.ofMonths(1);
				final TemporalSpliterator<YearMonth> spliterator = new TemporalSpliterator<YearMonth>(start, end, oneMonth);
				List<YearMonth> fromNormalIteration = new ArrayList<>();

				for (YearMonth current = start; current.isBefore(end); current = current.plus(oneMonth)) {
					fromNormalIteration.add(current);
				}
				List<YearMonth> fromSpliterator = new ArrayList<>();
				spliterator.forEachRemaining(fromSpliterator::add);

				// Now then, these MUST be equal, then we can check ordering on the
				// list from the spliterator
				Assert.assertEquals(fromNormalIteration, fromSpliterator);

				// Test the ordering of each date
				YearMonth previous = null;
				for (YearMonth current : fromSpliterator) {
					//Skip the first entry
					if(previous != null) {
						Assert.assertTrue(String.format("Previous time [%s] must be BEFORE the current time [%s]", previous, current), previous.isBefore(current));
						
					}
					previous = current;
				}

				Assert.assertEquals(fromSpliterator.get(0), start);
				Assert.assertEquals(fromSpliterator.get(fromSpliterator.size() - 1).plus(oneMonth), end);
		}
		
	}

	/**
	 * Tests below this point test that TemporalSpliterator conforms to the
	 * contracts required of it by Spliterator, based upon the properties it
	 * reports
	 * 
	 * @author Adam Paloski
	 *
	 */
	@RunWith(Theories.class)
	public static class SpliteratorContractTests {

		@DataPoints
		public static TemporalMap<?>[] getTemporalMapDataPoints() {
			return new TemporalMap<?>[] {
					new TemporalMap<LocalDate>(LocalDate.now(), LocalDate.now().plusDays(11), Period.ofDays(1)),
					new TemporalMap<LocalTime>(LocalTime.MIDNIGHT, LocalTime.MIDNIGHT.minusHours(1), Duration.ofHours(1)),
					new TemporalMap<LocalDateTime>(LocalDateTime.of(LocalDate.of(1992, 12, 23), LocalTime.NOON), LocalDateTime.of(
							LocalDate.of(1993, 5, 12), LocalTime.of(6, 32)), Duration.ofHours(3)),
					new TemporalMap<LocalDate>(LocalDate.of(1990, 1, 1), LocalDate.of(1990, 1, 8), Period.ofDays(3)) };
		}

		/* ------------------ GENERAL CHARACTERISTIC TESTS ------------------ */

		@Theory
		public void nonNullCharacteristicMeansNonNullElements(TemporalMap<?> map) {
			Spliterator<?> spliter = map.toSpliterator();
			Assume.assumeTrue(spliter.hasCharacteristics(Spliterator.NONNULL));
			spliter.forEachRemaining(element -> Assert.assertNotNull(element));
		}

		@Theory
		public void distinctCharacteristicMeansDistinctElements(TemporalMap<?> map) {
			Spliterator<?> spliter = map.toSpliterator();
			Assume.assumeTrue(spliter.hasCharacteristics(Spliterator.SORTED));
			Set<Object> elements = new HashSet<>();
			spliter.forEachRemaining(element -> assertTrue("Each element must not have been added to the set yet", elements.add(element)));
		}

		/* ------------------------ GET COMPARATOR --------------------------- */

		/**
		 * A test prepared for the case where we support a non-sorted
		 * spliterator, not used for now.
		 */
		@Ignore
		@Theory
		public void ifSpliteratorIsNotSortedThrows(TemporalMap<?> map) {
			Spliterator<?> spliter = map.toSpliterator();
			Assume.assumeFalse(spliter.hasCharacteristics(Spliterator.SORTED));
			try {
				spliter.getComparator();
			} catch (IllegalStateException ex) {
				return;
			}
			fail();
		}

		/**
		 * A test that sees if a spliterator generated for natural ordering
		 * produces a comparator, which it should not. That also checks that if
		 * you pass a comparator in it will succeed, and finally checks that no
		 * exceptions occur for a SORTED spliterator.
		 */
		@Theory
		public void ifSpliteratorIsSortedComparatorMeetsContract(TemporalMap<?> map) {

			// Natural ordering returns null -- default uses natural ordering.
			Spliterator<?> spliter = map.toSpliterator();
			Assume.assumeTrue(spliter.hasCharacteristics(Spliterator.SORTED));
			Assert.assertNull(spliter.getComparator());
		}

		/* ------------------------------- TRY SPLIT --------------------------- */

		@Theory
		public void noOverlappingEntiresAfterSplittingOnce(TemporalMap<?> map) {
			Spliterator<? extends Temporal> spliterator = map.toSpliterator();
			Spliterator<? extends Temporal> spawned = spliterator.trySplit();
			Assume.assumeNotNull(spawned);
			Set<Temporal> entires = new HashSet<>((int) (spliterator.estimateSize() + spawned.estimateSize()));
			Consumer<Temporal> assertionConsumer = entry -> assertTrue("No duplicate entires may be processed by forked spliterators",
					entires.add(entry));
			spawned.forEachRemaining(assertionConsumer);
			spliterator.forEachRemaining(assertionConsumer);
		}

		@Theory
		public void noOverlappingEntiresAfterSplittingToMax(TemporalMap<?> map) {
			@SuppressWarnings("unchecked")
			Spliterator<Temporal> spliterator = (Spliterator<Temporal>) map.toSpliterator();

			// Keep trying to spawn off different spliterators off all of them
			// until we can't get any more
			Set<Spliterator<Temporal>> spliteratorSet = splitUntilNull(spliterator);

			Set<Temporal> entires = new HashSet<>();
			Consumer<Temporal> assertionConsumer = entry -> assertTrue("No duplicate entires may be processed by forked spliterators",
					entires.add(entry));
			for (Spliterator<Temporal> spliteratorToParse : spliteratorSet) {
				spliteratorToParse.forEachRemaining(assertionConsumer);
			}
		}

		@SuppressWarnings({ "rawtypes", "unchecked" })
		@Theory
		public void trySplitProducesStrictPrefix(TemporalMap<?> map) {
			Spliterator<? extends Temporal> spliterator = map.toSpliterator();
			Assume.assumeTrue(spliterator.hasCharacteristics(Spliterator.SORTED));
			Assume.assumeTrue(spliterator.estimateSize() > 0);
			Comparator spliteratorComparator = spliterator.getComparator();
			if (spliteratorComparator == null) {
				spliteratorComparator = Comparator.naturalOrder();
			}
			// We need a second variable so that this can be final for the
			// lambda
			final Comparator comparator = spliteratorComparator;

			Spliterator<? extends Temporal> spawned = spliterator.trySplit();

			// Get the first entry of the spawning spliterator and put it into a
			// simple (mutable) container, a one element array
			Temporal[] temporalHolder = new Temporal[1];

			spliterator.tryAdvance(t -> temporalHolder[0] = t);
			// We know the types work, simply ignore raw types for simplicity of
			// the test.
			// If we ever fail due to class cast it's my fault, I'll live.
			spawned.forEachRemaining(entry -> {
				assertTrue(String.format("Each entry in the spawned set must come before the entires of the spawnee !(%s < %s)", entry,
						temporalHolder[0]), comparator.compare(entry, temporalHolder[0]) < 0);
			});
		}

		/**
		 * Checks that the contract of {@link Spliterator#trySplit()} that
		 * requires that both the spawning and spawned Spliterator MUST have a
		 * size that sums to that of the spawning spliterator before the split.
		 */
		@Theory
		public void afterSplitSizeSumsToTotalBeforeSplit(TemporalMap<?> map) {
			Spliterator<?> spliterator = map.toSpliterator();
			Assume.assumeTrue(spliterator.hasCharacteristics(Spliterator.SUBSIZED));

			for (int x = 0; x < 1000; ++x) {
				long beforeSplitSize = spliterator.estimateSize();
				Spliterator<?> spawned = spliterator.trySplit();
				// We hit the end of the line for splitting, fail out.
				if (spawned == null) {
					return;
				}
				long afterSplitSize = spawned.estimateSize() + spliterator.estimateSize();
				assertEquals(String.format("The subsized characteristic requires a total size be maintained after splitting %d != %d",
						beforeSplitSize, afterSplitSize), beforeSplitSize, afterSplitSize);
			}

		}

		/**
		 * Checks that the contract of {@link Spliterator#trySplit()} that
		 * requires that both the spawning and spawned Spliterator MUST have a
		 * size less than or equal to the spawner before the split is
		 * maintained.
		 */
		@Theory
		public void trySplitReducesOrMaintainsEstimatedSize(TemporalMap<?> map) {
			Spliterator<?> spliterator = map.toSpliterator();

			for (int x = 0; x < 1000; ++x) {
				long beforeSplitSize = spliterator.estimateSize();
				Spliterator<?> spawned = spliterator.trySplit();
				// We hit the end of the line for splitting, fail out.
				if (spawned == null) {
					return;
				}
				assertTrue("forked spliterators size must be less than or equal to that of the parent spliterator before the split",
						beforeSplitSize >= spawned.estimateSize());
				assertTrue("Spawning spliterators size must be less than or equal to that of the parent spliterator before the split",
						beforeSplitSize >= spliterator.estimateSize());
			}
		}

		@Theory
		public void trySplitReturnsNullEventuallyIfNotInfinite(TemporalMap<?> map) {
			Spliterator<?> spliterator = map.toSpliterator();
			Assume.assumeThat(spliterator.estimateSize(), IsNot.not(Long.MAX_VALUE));
			Assume.assumeTrue(spliterator.hasCharacteristics(Spliterator.SIZED));
			// It's sized so the estimateSize has to be accurate
			while (spliterator.estimateSize() > 1) {
				if (spliterator.trySplit() == null) {
					return;
				}
			}
			// One last chance when it's size is less than or equal to one, as
			// that's the base case
			Assert.assertNull(spliterator.trySplit());

		}

		// @formatter:off
		/*
		 * ------------------------------ ESTIMATE SIZE --------------------------
		 */
		// @formatter:on

		/**
		 * A theory that states that if our Spliterator is defined as being
		 * {@link Spliterator#SIZED} that it then, must, hold to the contract on
		 * {@link Spliterator#getExactSizeIfKnown()} and
		 * {@link Spliterator#estimateSize()}.
		 * <p>
		 * This tests that it holds for a Spliterator that <b>HAS NOT YET BEEN
		 * STARTED</b>
		 * 
		 * @param map
		 *            A datapoints provided parameter
		 */
		@Theory
		public void estimateSizeEstimatesCorrectlyOnUnstartedSpliterator(TemporalMap<?> map) {
			Spliterator<?> spliterator = map.toSpliterator();
			Assume.assumeTrue(spliterator.hasCharacteristics(Spliterator.SIZED));
			long expectedSize = spliterator.getExactSizeIfKnown();
			assertTrue(expectedSize != -1);

			// Okay so an atomic here is super overkill, but we need a mutable
			// integer container and I'm lazy...
			AtomicLong sizeCounter = new AtomicLong(0);
			// For this test we don't *really* care what it says to be honest...
			spliterator.forEachRemaining(obj -> sizeCounter.incrementAndGet());
			assertEquals(expectedSize, sizeCounter.intValue());
		}

		/**
		 * This theory maintains, in conjunction with
		 * {@link #estimateSizeEstimatesCorrectlyOnUnstartedSpliterator(TemporalMap)}
		 * the contract that getExactSizeIfKnown must return the exact size of
		 * the spliterator if the SIZED characteristic is reported.
		 * <p>
		 * This tests that it holds for a Spliterator that is in all states
		 * between started and finished.
		 * 
		 * <p>
		 * DO NOT RUN THIS TEST WITHOUT
		 * {@link #estimateSizeEstimatesCorrectlyOnUnstartedSpliterator(TemporalMap)}.
		 * 
		 * @param map
		 */
		@Theory
		public void estimateSizeEstimatesCorrectlyOnStartedSpliterator(TemporalMap<?> map) {
			Spliterator<?> spliterator = map.toSpliterator();
			Assume.assumeTrue(spliterator.hasCharacteristics(Spliterator.SIZED));

			// From the above test, we can assume that this function is correct
			// when operating on an unstarted spliterator
			long expectedSize = spliterator.getExactSizeIfKnown();

			for (long index = 0; index <= expectedSize; ++index) {
				assertEquals(expectedSize - index, spliterator.getExactSizeIfKnown());
				// Progress by one...
				spliterator.tryAdvance(obj -> {
				});
			}
			assertTrue(!spliterator.tryAdvance(obj -> {
				/* Do nothing */
			}));
		}

	}

	/**
	 * A simple test helper class that contains the three values needed as a
	 * core for a TemporalSpliterator. This allows construction of a
	 * TemporalSpliterator directly while storing data within an immutable
	 * container that will then pass that to the Spliterator.
	 * 
	 * @author Adam Paloski
	 *
	 * @param <T>
	 *            The temporal type of the spliterator.
	 */
	private static class TemporalMap<T extends Temporal & Comparable<? super T>> implements Supplier<Spliterator<T>> {
		public T start;
		public T end;
		public TemporalAmount amount;

		public TemporalMap(T start, T end, TemporalAmount amount) {
			this.start = start;
			this.end = end;
			this.amount = amount;
		}

		/**
		 * Returns a spliterator that uses natural sorting, this method
		 * <b>MUST</b> always return a logically equivalent spliterator.
		 * 
		 * <p>
		 * This spliterator uses natural ordering instead of a comparator
		 */
		public Spliterator<T> toSpliterator() {
			return new TemporalSpliterator<T>(start, end, amount);
		}

		@Override
		public Spliterator<T> get() {
			return toSpliterator();
		}

	}
	
	/**
	 * A private helper method for preforming repeated splitting until no
	 * more splitting can happen.
	 */
	private static <T> Set<Spliterator<T>> splitUntilNull(Spliterator<T> parent) {
		Set<Spliterator<T>> splitSet = new HashSet<>();
		
		while (true) {
			Spliterator<T> child = parent.trySplit();
			if (child != null) {
				splitSet.addAll(splitUntilNull(child));
			} else {
				splitSet.add(parent);
				return splitSet;
			}
		}
	}

}

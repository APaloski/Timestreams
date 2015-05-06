package com.paloski.time.stream;

import java.time.temporal.Temporal;
import java.time.temporal.TemporalAmount;
import java.time.temporal.TemporalUnit;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.Spliterator;
import java.util.function.Consumer;

import com.paloski.time.TemporalUtils;

/**
 * A Spliterator implementation that is specialized for usage with Temporal
 * objects.
 * <p>
 * A TemporalSpliterator creates its objects Lazily, calculating them on an
 * as-needed basis instead of pre-creating them all. This extends to
 * Spliterators created via {@link #trySplit()}. Due to this, if this
 * Spliterator is used as the backing for a Stream that only invokes
 * {@link #tryAdvance(Consumer)} for each element that's needed, the number of
 * objects created can be severely reduced.
 * 
 * <h3>Inexact point breaking</h3>
 * TemporalSpliterators can be created with an inexact amount and will treat that inexact section 
 * 
 * 
 * , for instance
 * starting on June 5th, 2015 and ending on June 14th, 2015, incrementing by
 * three days. This will result in an inexact break, where the 5th, 8th and 12th
 * are included, but no future days are because the 14th is <em>not</em> at
 * least three days away.
 * 
 * 
 * @author apaloski
 *
 * @param <T>
 *            A subclass of Temporal that defines the type of all range points
 *            used by this Spliterator. Note that this type is required to be
 *            Comparable, as required by {@link Temporal}
 */
// TODO Temporal is required to be Comparable, should we simply require it here
// or leave it as a deeper implementation detail?
public final class TemporalSpliterator<T extends Temporal & Comparable<? super T>> implements Spliterator<T> {

	private final T mStartingPoint;
	private final T mEndingPoint;
	private T mCurrentPoint;
	private final TemporalUnit mIncrementAmountsSmallestUnit;
	private final TemporalAmount mIncrementAmount;

	/**
	 * A protected constructor that forces users of this class to go through
	 * factory methods instead so that we can indirectly expose
	 * comparable/non-comparable spliterators.
	 * 
	 * @param startingPointInclusive
	 *            The temporal starting point of this Spliterator, this value is
	 *            inclusive.
	 * @param endingPointExclusive
	 *            The ending point of this Spliterator, this value is exclusive,
	 *            and will <em>not</em> be included.
	 * @param increment
	 *            The amount to increment the current time point each time that
	 *            {@link #tryAdvance(Consumer)} is called.
	 */
	protected TemporalSpliterator(T startingPointInclusive, T endingPointExclusive, TemporalAmount increment) {
		mStartingPoint = Objects.requireNonNull(startingPointInclusive,
				"The starting point of a TemporalSpliterator may not be null");
		mEndingPoint = Objects.requireNonNull(endingPointExclusive,
				"The ending point of a TemporalSpliterator may not be null");
		mIncrementAmount = Objects.requireNonNull(increment,
				"The incrementing amount of a TemporalSpliterator may not be null");

		/*
		 * First we need to validate our arguments besides the normal non-null
		 * check.
		 */

		// Find the smallest temporal unit that has a value in our increment
		// amount.
		// we require our temporal amounts to at least support that.
		List<TemporalUnit> units = increment.getUnits();
		TemporalUnit smallestPrecisionInIncrement = null;
		for (ListIterator<TemporalUnit> li = units.listIterator(units.size()); li.hasPrevious();) {
			TemporalUnit unit = li.previous();
			// We try to find the smallest one possible...
			if (increment.get(unit) != 0 && smallestPrecisionInIncrement == null) {
				smallestPrecisionInIncrement = unit;
				break;
			}
		}
		// And make sure every unit is supported by our temporal types, we
		// will be reusing them.
		if (!startingPointInclusive.isSupported(smallestPrecisionInIncrement)
				|| !endingPointExclusive.isSupported(smallestPrecisionInIncrement)) {
			throw new IllegalArgumentException(
					"The type of starting point or ending point does not support the temporal unit "
							+ smallestPrecisionInIncrement + " that makes up part of increment");
		}

		mIncrementAmountsSmallestUnit = smallestPrecisionInIncrement;

		// Check we have some smallest precision point...
		if (smallestPrecisionInIncrement == null) {
			throw new IllegalArgumentException("TemporalAmount " + increment
					+ " must have a value for a unit it supports");
		}
		mCurrentPoint = mStartingPoint;
	}

	@Override
	public boolean tryAdvance(Consumer<? super T> action) {
		Comparator<? super T> comparator = Comparator.naturalOrder();
		if (comparator.compare(mCurrentPoint, mEndingPoint) >= 0) {
			return false;
		}
		action.accept(mCurrentPoint);
		mCurrentPoint = calculateNextCurrentPoint();
		return true;
	}

	@Override
	public Spliterator<T> trySplit() {

		T newSpliteratorsStartingPosition = mCurrentPoint;

		// Below dupes code in estimate size, but we need each of these
		// variables
		long durationUntilEndInSmallestUnitAmount = mIncrementAmountsSmallestUnit.between(mCurrentPoint, mEndingPoint);
		double amountOfSmallestUnitInIncrement = TemporalUtils.getEstimatedNumberOfUnits(mIncrementAmount,
				mIncrementAmountsSmallestUnit);

		// For more information on why we ceil this, see estimateSize
		long numberOfEntiresUntilEnd = (long) Math.ceil(durationUntilEndInSmallestUnitAmount
				/ amountOfSmallestUnitInIncrement);

		if (numberOfEntiresUntilEnd <= 1) {
			return null;
		}

		T splitPoint = mIncrementAmountsSmallestUnit.addTo(mCurrentPoint,
				Math.round(amountOfSmallestUnitInIncrement * (numberOfEntiresUntilEnd / 2)));

		// If the half duration until the end is less than the increment amount,
		// we have hit our base case for splitting and should return null

		mCurrentPoint = splitPoint;

		return new TemporalSpliterator<T>(newSpliteratorsStartingPosition, splitPoint, mIncrementAmount);
	}

	@Override
	public long estimateSize() {
		long durationUntilEnd = mIncrementAmountsSmallestUnit.between(mCurrentPoint, mEndingPoint);
		double amountOfSmallestUnitInIncrement = TemporalUtils.getEstimatedNumberOfUnits(mIncrementAmount,
				mIncrementAmountsSmallestUnit);
		double numberOfEntiresUntilEnd = durationUntilEnd / amountOfSmallestUnitInIncrement;

		// @formatter:off
		/*
		 * This functionality needs to use Math.ciel to handle the partial case
		 * because the above calculates are calculating the number of spaces in
		 * time in the set, not the number of points, which are what we care
		 * about. Due to this, we run into the case where an evenly dividing set
		 * works fine, but a non-even set breaks.
		 * 
		 * Take the case of we start on day 0, and go until day 4 (exclusive)
		 * |---|---|---|---| 0 1 2 3 4
		 * 
		 * The number of openings in the set is equal to the number of points,
		 * not counting the excluded day 4 or it's open section.
		 * 
		 * However, if we instead start on day 0 and go until day 4 (exclusive)
		 * every other day, we run into this case.
		 * 
		 * |---.---|---.---| 0 1 2 3 4
		 * 
		 * The fact that day four is excluded means that we calculate 1.5 as our
		 * total, (we stop at day 3 meaning we have a partial break from 2 ->
		 * 3). If we were to floor this we would calculate having one entry,
		 * when we actually have two. In this case rounding would work, however
		 * in the case of a smaller division we could have .17 of a break, but
		 * we still need to count the POINTs of it, we are always going to be
		 * off by one if we round down. Thus we need to take the ceil to ensure
		 * that any partial break is always treated as if it is full. *
		 */
		// @formatter:on
		return /* This cast should be precision safe */(long) Math.ceil(numberOfEntiresUntilEnd);
	}

	@Override
	public int characteristics() {
		return IMMUTABLE | NONNULL | SIZED | SUBSIZED | ORDERED | DISTINCT | SORTED;
	}

	@Override
	public Comparator<? super T> getComparator() {
		// We are ALWAYS sorted, so always return null
		return null;
	}

	/**
	 * Internalized progressing to the next current point into a method so that
	 * it can be called uniformly from multiple locations.
	 * 
	 * @return The next value of {@link #mCurrentPoint}
	 */
	// This needs to be unchecked, however the contract of Temporal.plus
	// requires the same type to be returned.
	@SuppressWarnings("unchecked")
	private T calculateNextCurrentPoint() {
		// This special logic is needed to handle the case of cyclical cases,
		// such as going every hour from midnight to midnight. There may be a
		// better implementation but this gets the job done.
		if (mIncrementAmountsSmallestUnit.between(mCurrentPoint, mEndingPoint) < TemporalUtils
				.getEstimatedNumberOfUnits(
						mIncrementAmount, mIncrementAmountsSmallestUnit)) {
			return mEndingPoint;
		} else {
			return (T) mCurrentPoint.plus(mIncrementAmount);
		}
	}
}

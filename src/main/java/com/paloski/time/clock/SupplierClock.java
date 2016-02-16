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
import java.util.Objects;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/**
 * A Clock that is specialized to use an underlying {@link Supplier} or
 * {@link LongSupplier} as its time data source.
 * <p>
 * A SupplierClock is entirely based upon the Supplier passed in from the
 * {@link #ofInstantSupplier(Supplier, ZoneId)} or
 * {@link #ofMillisecondSupplier(LongSupplier, ZoneId)} methods. All of the
 * times obtained from this clock are based upon the return values of those
 * suppliers. As the core of this class is based upon a Supplier, that Supplier
 * <b>must</b> ensure thread safety to hold to the contract of {@link Clock}.
 * <p>
 * Any Supplier can be used as the data source to this method, so it is easy to
 * recreate Clocks such as {@link Clock#system(ZoneId)} by calling
 * <p>
 * <pre>
 * {@code SupplierClock.ofMillisecondSupplier(System::currentTimeMillis, ZoneId);}
 * </pre>
 * <p>
 * or {@link Clock#fixed(Instant, ZoneId)} by calling
 * <p>
 * <pre>
 * {@code SupplierClock.ofMillisecondSupplier(() -> 3500L, ZoneId);}
 * </pre>
 * <p>
 * This implementation of Clock is immutable, thread-safe and
 * {@code Serializable}, provided that the underlying Supplier is.
 *
 * @author Adam Paloski
 */
public final class SupplierClock extends Clock implements Serializable {

	/**
	 * Serialization Id generated on 4/29/2015
	 */
	private static final long serialVersionUID = -3909967180515408474L;

	private final LongSupplier mMillisSupplier;
	private final Supplier<Instant> mInstantSupplier;
	private final ZoneId mZoneId;

	/**
	 * Private constructor that initializes the millisecond supplier to be an
	 * argument and leaves the instant supplier to be null.
	 *
	 * @param millisecondSupplier
	 * 		A non-null LongSupplier, if this is null an
	 * 		IllegalStateException will be thrown.
	 * @param zoneId
	 * 		A non-null ZoneId, if this is null a NullPointerException will
	 * 		be thrown.
	 */
	private SupplierClock(LongSupplier millisecondSupplier, ZoneId zoneId) {
		if (millisecondSupplier == null) {
			throw new IllegalStateException(
					"A SupplierClock cannot be instantiated without either a millisecond supplier or an instant supplier");
		}
		mInstantSupplier = null;
		mMillisSupplier = millisecondSupplier;
		mZoneId = Objects.requireNonNull(zoneId, "The ZoneId of this Clock cannot be null");
	}

	/**
	 * Private constructor that initializes the instant supplier to be an
	 * argument and leaves the millisecond supplier to be null.
	 *
	 * @param instantSupplier
	 * 		A non-null Supplier, if this is null an IllegalStateException
	 * 		will be thrown.
	 * @param zoneId
	 * 		A non-null ZoneId, if this is null a NullPointerException will
	 * 		be thrown.
	 */
	private SupplierClock(Supplier<Instant> instantSupplier, ZoneId zoneId) {
		if (instantSupplier == null) {
			throw new IllegalStateException(
					"A SupplierClock cannot be instantiated without either a millisecond supplier or an instant supplier");
		}
		mInstantSupplier = instantSupplier;
		mMillisSupplier = null;
		mZoneId = Objects.requireNonNull(zoneId, "The ZoneId of this Clock cannot be null");
	}

	@Override
	public long millis() {
		if (mMillisSupplier != null) {
			return mMillisSupplier.getAsLong();
		} else {
			return instant().getEpochSecond();
		}
	}

	@Override
	public ZoneId getZone() {
		return mZoneId;
	}

	@Override
	public SupplierClock withZone(ZoneId zone) {
		// Easy early out
		if (zone.equals(getZone())) {
			return this;
		}

		if (mMillisSupplier != null) {
			return new SupplierClock(mMillisSupplier, mZoneId);
		} else {
			return new SupplierClock(mInstantSupplier, mZoneId);
		}
	}

	@Override
	public Instant instant() {
		if (mInstantSupplier != null) {
			Instant value = mInstantSupplier.get();
			if (value == null) {
				throw new IllegalStateException(
						"The Supplier of instants within a SupplierClock returned null. This violates the contract on the method Clock.instant()");
			}
			return value;
		} else {
			return Instant.ofEpochMilli(millis());
		}
	}

	/**
	 * Creates a new Clock that has the time it returns based upon a Supplier
	 * that returns a number of milliseconds since the epoch.
	 * <p>
	 * Clocks created by this function return values from {@link #millis()} and
	 * {@link #instant()} that are dependent upon the value returned by
	 * {@code millisecondSupplier}. Because of this, these functions will throw
	 * any RuntimeException that is thrown by {@code millisecondSupplier} when
	 * {@link LongSupplier#getAsLong()} is invoked.
	 * <p>
	 * The returned implementation is immutable, thread-safe and
	 * {@code Serializable} providing that the underlying LongSupplier is.
	 *
	 * @param millisecondSupplier
	 * 		A non-null LongSupplier that supplies the number of
	 * 		milliseconds since the epoch that this clock will return when
	 * 		queried via {@link #millis()} and {@link #instant()}. <em>This
	 * 		LongSupplier must be thread safe to fulfill the contract of Clock</em>
	 * @param zoneId
	 * 		The ZoneId of this Clock, as returned by {@link #getZone()}.
	 *
	 * @return A new Clock that will obtain the milliseconds it returns in
	 * {@link #millis()} from a {@code millisecondSupplier}.
	 */
	public static Clock ofMillisecondSupplier(LongSupplier millisecondSupplier, ZoneId zoneId) {
		return new SupplierClock(Objects.requireNonNull(millisecondSupplier, "The millisecond supplier cannot be null"), zoneId);
	}

	/**
	 * Creates a new Clock that has the time it returns based upon a Supplier
	 * that supplies Instant values.
	 * <p>
	 * Clocks created by this function return values from {@link #millis()} and
	 * {@link #instant()} that are dependent upon the value returned by
	 * {@code instantSupplier}. Because of this, these functions will throw any
	 * RuntimeException that is thrown by {@code millisecondSupplier} when
	 * {@link Supplier#get()} is invoked, or will throw an
	 * {@link IllegalStateException} if {@code instantSupplier} returns an
	 * {@code null} value.
	 * <p>
	 * The returned implementation is immutable, thread-safe and
	 * {@code Serializable} providing that the underlying Supplier is.
	 *
	 * @param instantSupplier
	 * 		A non-null Supplier that supplies the Instant that will be
	 * 		returned when {@link #millis()} and {@link #instant()}.  <em>This
	 * 		Supplier must be thread safe to fulfill the contract of Clock</em>
	 * @param zoneId
	 * 		The ZoneId of this Clock, as returned by {@link #getZone()}.
	 *
	 * @return A new Clock that will obtain the milliseconds it returns in
	 * {@link #millis()} from a {@code millisecondSupplier}.
	 */
	public static Clock ofInstantSupplier(Supplier<Instant> instantSupplier, ZoneId zoneId) {
		return new SupplierClock(Objects.requireNonNull(instantSupplier, "The instant supplier cannot be null"), zoneId);
	}

}

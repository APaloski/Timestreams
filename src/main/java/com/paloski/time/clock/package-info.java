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

/**
 * Provides Clock objects that help to bridge the gap between the old java time
 * APIs ({@link java.util.Calendar} and {@link java.util.Date}) and the new {@code java.time} APIs.
 * <p>
 * All classes in this package are subclassses of the {@link java.time.Clock}
 * class, and are an attempt to simplify the transition from Legacy time APIs to
 * the {@code java.time} APIs. The Clock class is preferred for this, as the
 * majority of ISO Temporal implementations have a method with a signature
 * similar to {@code TemporalImp.now(Clock)}, which uses the current time on the
 * Clock to obtain the equivalent Temporal object. Every clock within this
 * package strictly adheres to the implementation specification provided by
 * {@link java.time.Clock}.
 * <p>
 * <h2>Legacy -&gt; Java time</h2>
 * Classes in this category are used to convert from legacy Time APIs, such as
 * {@link java.util.Date}, {@link java.sql.Timestamp} and
 * {@link java.util.Calendar} directly to the Java Time APIs, each one provides
 * a method styled as {@code Clock.ofSupplier(Supplier<LegacyAPIType>, ZoneId)}
 * which can be used to produce a concrete clock that utilizes a Supplier of
 * legacy values as its source.
 * <h2>Java Time &gt;-&lt; Legacy</h2>
 * The {@link com.paloski.time.clock.LegacyClock} class is provided for bridging
 * the gap between Java Time and back, by providing direct access methods to all
 * levels of the Java time types. This clock is a wrapper clock, in that it uses
 * a backing Clock as its provider of the current time, as such it can be used
 * in conjunction with any of the standard Clocks provided by {@link java.time.Clock}, or
 * with any of the other Clocks mentioned here.
 * <h2>Externalized Clock</h2>
 * The {@link com.paloski.time.clock.SupplierClock} class is provided for
 * creating a Clock that is based from an externalized millisecond (in long
 * form) or Instant provider.
 *
 * @author Adam Paloski
 */
package com.paloski.time.clock;
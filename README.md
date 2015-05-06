# Java-8-Time-Utils
A small set of utilities for Java 8 that help with working with legacy time code through implementations of Java 8 Clocks and allow streaming of Java 8 Temporal objects.

Clocks
------
This library provides Clock objects for working with Legacy date/time Java objects, such as Date and Calendar.

First and foremost, it provides the ability to wrap a current Date, Calendar, or Millisecond provider within a Clock, which can then be used to directly create Temporal objects without needing to remember convoluted conversion methods.


Generating a LocalDate from a Calendar transforms from

```java
public void newerMethod() {
  Calendar externalCalendar = ExternalSource.getCurrentTime();
  LocalDateTime dateTime = LocalDateTime.ofInstant(externalCalendar.toInstant(), ZoneId.of(externalCalendar.getTimeZone()));
  //Use the local date time object...
}
```
to
```java
//This can be set at some global singleton level
Clock globalTimeProvider = CalendarClock.ofSupplier(ExternalSource::getCurrentTime, ZoneId.systemDefault());

public void newerMethod() {
  LocalDateTime dateTime = LocalDateTime.now(globalTimeProvider);
  //Use the local date time object...
}
```

This can further expanded by using a LegacyClock if you have one global point of access for your time provider, but make use of java.time classes, Date and Calendar over varying ages of code. A LegacyClock allows access to a Calendar, Date, Timestamp or any java.time object providing a now(Clock) method directly, without having to have additional helper classes for conversion. So, using a LegacyClock you can do the following

```java
LegacyClock legacyClock = LegacyClock.of(CalendarClock.ofSupplier(ExternalSource::getCurrentTime, ZoneId.systemDefault()));

public void reallyOldDateBasedMethod() {
    Date currentDate = legacyClock.toDate();
    //Use the Date object...
}

public void midlyOldCalendarMethod() {
    Calendar currentDateTime = legacyClock.toZonedCalendar();
    //Use the Calendar object
}

public void newDateMethod() {
  LocalDateTime ldt = LocalDateTime.now(legacyClock);
  //Use the local date time...
}
```

Temporal Streams
----------------
Temporal Streams allow the use of the Java 8 Streaming API over arbitary time distances with arbitary increments. So with Temporal Streaming, I can create a Stream that iterates over every day in a year, or every week in a year. The end result is that so long as the underlying Temporal API allows for use of the unit with the boundries, you can stream it.

But why stream? Efficency, first and foremost. The Streams created by this library will only create the Temporal objects as they are requested, so in any case where the underlying API determines it only needs to visit the first few objects (such as when invoking findAny or findFirst on a stream), only those will be generated.

The main entry point in temporal streaming is the TemporalStreams class, which provides static factory methods for creating some common streams, such as streaming over every day in a year, and a builder for creating an arbitary stream.

For example: A simple stream that iterates over every day in year, from now until next year on today.

```java
  new TemporalStreamBuilder<LocalDate>().every(Period.ofDays(1))
                                        .from(LocalDate.now())
                                        .until(LocalDate.now().plusYears(1))
                                        .stream();
```

And it's as simple as that, you now have a stream that you can map, filter, and reduce to your hearts content.

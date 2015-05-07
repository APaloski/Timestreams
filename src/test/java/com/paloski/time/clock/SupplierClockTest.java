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
import java.util.function.LongSupplier;
import java.util.function.Supplier;

import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

@RunWith(Enclosed.class)
public class SupplierClockTest {

	/**
	 * Preforms tests that ensures that the contracts set forth in Javadoc are held.
	 */
	public static class ContractTests {
		@Rule
		public ExpectedException mExpectedException = ExpectedException.none();
		
		@Test
		public void testNullZoneIdWithLongSupplierThrows() {
			mExpectedException.expect(NullPointerException.class);
			mExpectedException.expectMessage("cannot be null");
			SupplierClock.ofMillisecondSupplier(System::currentTimeMillis, null);
		}
		
		@Test
		public void testNullZoneIdWithInstantSupplierThrows() {
			mExpectedException.expect(NullPointerException.class);
			mExpectedException.expectMessage("cannot be null");
			SupplierClock.ofInstantSupplier(Clock.systemUTC()::instant, null);
		}
		
		@Test
		public void testNullLongSupplierThrows() {
			mExpectedException.expect(NullPointerException.class);
			mExpectedException.expectMessage("millisecond supplier cannot be null");
			SupplierClock.ofMillisecondSupplier(null, ZoneId.systemDefault());
		}
		
		@Test
		public void testNullInstantSupplierThrows() {
			mExpectedException.expect(NullPointerException.class);
			mExpectedException.expectMessage("instant supplier cannot be null");
			SupplierClock.ofInstantSupplier(null, ZoneId.systemDefault());
		}
		
		/**
		 * Tests that the contract that a non-serializable supplier means the clock is not serializable is held.
		 */
		@Test
		public void testSerializationWithNonSerializableSupplier() throws IOException {
			mExpectedException.expect(ObjectStreamException.class);
			
			boolean serializationFailed = false;
			ObjectOutputStream oos = new ObjectOutputStream(new ByteArrayOutputStream());
			
			//Check that the supplier is NOT serializable
			Supplier<Instant> supplier = () -> null;
			
			try {
				oos.writeObject(supplier);
			} catch (ObjectStreamException ex) {
				serializationFailed = true;
			}
			
			if(!serializationFailed) {
				fail("Test precondition failed: A supplier expected to be non-serializable was successfully serialized");
			}
			
			oos.writeObject(SupplierClock.ofInstantSupplier(supplier, ZoneId.systemDefault()));
		}
		
		/**
		 * Tests that the contract that a non-serializable supplier means the clock is not serializable is held.
		 */
		@Test
		public void testSerializationWithSerializableSupplier() throws IOException {
			ObjectOutputStream oos = new ObjectOutputStream(new ByteArrayOutputStream());
			
			//Check that the supplier is NOT serializable
			Supplier<Instant> supplier =  (Supplier<Instant> & Serializable) () -> null;
			
			try {
				oos.writeObject(supplier);
			} catch (ObjectStreamException ex) {
				fail("Test precondition failed: A supplier expected to be Serializable was not successfully serialized");
			}
			
			oos.writeObject(SupplierClock.ofInstantSupplier(supplier, ZoneId.systemDefault()));
		}
	}
	
	/**
	 * A Test wrapper class for tests on non-preconditional checks
	 */
	public static class SupplierTests {
		
		@Rule
		public ExpectedException expectedException = ExpectedException.none();
		
		@Test
		public void testFixedPointLongSupplier() {
			LongSupplier longSupplier = () -> 3500L;
			
			Clock clock = SupplierClock.ofMillisecondSupplier(longSupplier, ZoneId.systemDefault());
			assertEquals(longSupplier.getAsLong(), clock.millis());
		}
		
		@Test
		public void testFixedPointInstantSupplier() {
			Instant fixedPt = Clock.systemUTC().instant();
			Supplier<Instant> instantSupplier = () -> fixedPt;
			
			Clock clock = SupplierClock.ofInstantSupplier(instantSupplier, ZoneId.systemDefault());
			assertEquals(instantSupplier.get(), clock.instant());
		}
		
		@Test
		public void nullSupplierThrowsWithInstantMethod() {
			Supplier<Instant> instantSupplier = () -> null;
			
			expectedException.expect(IllegalStateException.class);
			expectedException.expectMessage("The Supplier of instants within a SupplierClock returned null");
			
			Clock clock = SupplierClock.ofInstantSupplier(instantSupplier, ZoneId.systemDefault());
			clock.instant();
		}
		
		@Test
		public void nullSupplierThrowsWithMillisMethod() {
			Supplier<Instant> instantSupplier = () -> null;
			
			expectedException.expect(IllegalStateException.class);
			expectedException.expectMessage("The Supplier of instants within a SupplierClock returned null");
			
			Clock clock = SupplierClock.ofInstantSupplier(instantSupplier, ZoneId.systemDefault());
			clock.instant();
		}
		
		@Test
		public void clockIsSerializableIfInstantSupplierIs() throws IOException {
			Supplier<Instant> instantSupplier = (Serializable & Supplier<Instant>) () -> Instant.now();
			ObjectOutputStream oos = new ObjectOutputStream(new ByteArrayOutputStream());
			try {
				oos.writeObject(instantSupplier);
			} catch (ObjectStreamException exp) {
				fail("Precondition failed: could not serialize supplier");
			}
			
			Clock clock = SupplierClock.ofInstantSupplier(instantSupplier, ZoneId.systemDefault());
			
			try {
				oos.writeObject(clock);
			} catch (ObjectStreamException exp) {
				fail("Could not serialize CalendarSupplierClock even though the Supplier was serializable");
			}
		}
		
		@Test
		public void clockIsNotSerializableIfInstantSupplierIsnt() throws IOException {
			Supplier<Instant> instantSupplier = (Supplier<Instant>) () -> Instant.now();
			ObjectOutputStream oos = new ObjectOutputStream(new ByteArrayOutputStream());
			boolean serializationFailed = false;
			try {
				oos.writeObject(instantSupplier);
			} catch (ObjectStreamException exp) {
				serializationFailed = true;
			}
			
			if(!serializationFailed) {
				fail("Test precondition failed: Lambda was successfully serialized");
			}
			
			Clock clock = SupplierClock.ofInstantSupplier(instantSupplier, ZoneId.systemDefault());
			
			try {
				oos.writeObject(clock);
			} catch (ObjectStreamException exp) {
				//Serialization succeeded, return out.
				return;
			}
			fail("Successfully serialized clock based upon non-serializable lambda, this should not succeed");
		}
		
		@Test
		public void clockIsSerializableIfMillisSupplierIs() throws IOException {
			LongSupplier milliSupplier = (Serializable & LongSupplier) System::currentTimeMillis;
			ObjectOutputStream oos = new ObjectOutputStream(new ByteArrayOutputStream());
			try {
				oos.writeObject(milliSupplier);
			} catch (ObjectStreamException exp) {
				fail("Precondition failed: could not serialize supplier");
			}
			
			Clock clock = SupplierClock.ofMillisecondSupplier(milliSupplier, ZoneId.systemDefault());
			
			try {
				oos.writeObject(clock);
			} catch (ObjectStreamException exp) {
				fail("Could not serialize CalendarSupplierClock even though the Supplier was serializable");
			}
		}
		
		@Test
		public void clockIsNotSerializableIfMillisSupplierIsnt() throws IOException {
			LongSupplier milliSupplier = (LongSupplier) System::currentTimeMillis;
			ObjectOutputStream oos = new ObjectOutputStream(new ByteArrayOutputStream());
			boolean serializationFailed = false;
			try {
				oos.writeObject(milliSupplier);
			} catch (ObjectStreamException exp) {
				serializationFailed = true;
			}
			
			if(!serializationFailed) {
				fail("Test precondition failed: Lambda was successfully serialized");
			}
			
			Clock clock = SupplierClock.ofMillisecondSupplier(milliSupplier, ZoneId.systemDefault());
			
			try {
				oos.writeObject(clock);
			} catch (ObjectStreamException exp) {
				//Serialization succeeded, return out.
				return;
			}
			fail("Successfully serialized clock based upon non-serializable lambda, this should not succeed");
		}
		
	}
	
	
}

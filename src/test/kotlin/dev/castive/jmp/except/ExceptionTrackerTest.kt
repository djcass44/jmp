package dev.castive.jmp.except

import org.junit.jupiter.api.Test

class ExceptionTrackerTest {
	/**
	 * Add an entry to the tracker
	 * Ensure that it's actually added
	 */
	@Test
	fun testAdd() {
		val tracker = ExceptionTracker(false)
		tracker.onExceptionTriggered(Exception("Test"))
		assert(tracker.getData().isNotEmpty())
	}

	/**
	 * Add an entry to the tracker with a specific time
	 * Ensure that the specific time is still there
	 */
	@Test
	fun testAddTime() {
		val tracker = ExceptionTracker(false)
		val time = System.currentTimeMillis()
		tracker.onExceptionTriggered(Exception("Test"), time)
		val res = tracker.getData()
		assert(res[0].second == time)
	}

	/**
	 * Add an entry
	 * Ensure that the entry has the correct name
	 */
	@Test
	fun testInsecureAdd() {
		val except = NullPointerException("This is a test!")
		val tracker = ExceptionTracker(false)
		tracker.onExceptionTriggered(except)
		val res = tracker.getData()
		assert(res[0].first == except::class.java.name)
	}

	/**
	 * Add an entry
	 * Ensure that the entry isn't using the actual name
	 */
	@Test
	fun testSecureAdd() {
		val except = NullPointerException("This is a test!")
		val tracker = ExceptionTracker(true)
		tracker.onExceptionTriggered(except)
		val res = tracker.getData()
		assert(res[0].first != except::class.java.name)
		assert(res[0].first == tracker.generic)
	}
	@Test
	fun testSecureAddCustomGeneric() {
		val except = NullPointerException("This is a test!")
		val tracker = ExceptionTracker(true, "Test!")
		tracker.onExceptionTriggered(except)
		val res = tracker.getData()
		assert(res[0].first != except::class.java.name)
		assert(res[0].first == tracker.generic)
	}
	@Test
	fun testVeryOld() {
		val tracker = ExceptionTracker(false)
		tracker.onExceptionTriggered(Exception("Test"), 0)
		assert(tracker.getData().isEmpty())
	}
}
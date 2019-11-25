package dev.castive.jmp.except

import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.not
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class ExceptionTrackerTest {
	private val dummyText = "This is a test!"
	/**
	 * Add an entry to the tracker
	 * Ensure that it's actually added
	 */
	@Test
	fun `adding an item to the list`() {
		val tracker = ExceptionTracker(false)
		tracker.onExceptionTriggered(Exception("Test"))
		assertTrue(tracker.getData().isNotEmpty())
	}

	/**
	 * Add an entry to the tracker with a specific time
	 * Ensure that the specific time is still there
	 */
	@Test
	fun `adding an item to the list with a specific time`() {
		val tracker = ExceptionTracker(false)
		val time = System.currentTimeMillis()
		tracker.onExceptionTriggered(Exception("Test"), time)
		val res = tracker.getData()
		assertThat(res[0].second, `is`(time))
	}

	/**
	 * Add an entry
	 * Ensure that the entry has the correct name
	 */
	@Test
	fun `(blockLeak false) adding an item retains its className`() {
		val except = NullPointerException(dummyText)
		val tracker = ExceptionTracker(false)
		tracker.onExceptionTriggered(except)
		val res = tracker.getData()
		assertThat(res[0].first, `is`(except::class.java.name))
	}

	/**
	 * Add an entry
	 * Ensure that the entry isn't using the actual name
	 */
	@Test
	fun `(blockLeak true) adding an item uses a generic className`() {
		val except = NullPointerException(dummyText)
		val tracker = ExceptionTracker(true)
		tracker.onExceptionTriggered(except)
		val res = tracker.getData()
		assertThat(res[0].first, not(except::class.java.name))
		assertThat(res[0].first, `is`(tracker.generic))
	}
	@Test
	fun `(blockLeak true) adding an item uses a specified className`() {
		val except = NullPointerException(dummyText)
		val tracker = ExceptionTracker(true, "Test!")
		tracker.onExceptionTriggered(except)
		val res = tracker.getData()
		assertThat(res[0].first, not(except::class.java.name))
		assertThat(res[0].first, `is`(tracker.generic))
	}
	@Test
	fun `stale item is not returned`() {
		val tracker = ExceptionTracker(false)
		tracker.onExceptionTriggered(Exception("Test"), 0)
		assertTrue(tracker.getData().isEmpty())
	}
}

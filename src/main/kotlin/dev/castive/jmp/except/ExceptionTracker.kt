package dev.castive.jmp.except

import dev.castive.log2.Log
import java.util.*
import java.util.concurrent.TimeUnit

class ExceptionTracker(private val blockLeak: Boolean = true) {
	private val items = arrayListOf<Pair<String, Long>>()

	private val generic = Exception::class.java.name

	fun onExceptionTriggered(e: Throwable, time: Long = System.currentTimeMillis()) {
		Log.d(javaClass, "Adding exception item")
		items.add(Pair(e::class.java.name, time))
		Log.d(javaClass, Arrays.toString(items.toTypedArray()))
	}
	fun getData(timeframe: Long = TimeUnit.MINUTES.toMillis(5)): ArrayList<Pair<String, Long>> {
		val currentTime = System.currentTimeMillis()
		val results = arrayListOf<Pair<String, Long>>()
		for (i in items.size - 1 downTo 0) {
			val dx = currentTime - items[i].second
			Log.d(javaClass, "Comparing dx: $dx to timeframe: $timeframe")
			if(dx < timeframe) {
				// Don't reveal exception information unless allowed to
				results.add(if(blockLeak) Pair(generic, items[i].second) else items[i])
			}
		}
		Log.d(javaClass, "Items: ${Arrays.toString(items.toTypedArray())}")
		Log.d(javaClass, "Results: ${Arrays.toString(results.toTypedArray())}")
		return results
	}
}
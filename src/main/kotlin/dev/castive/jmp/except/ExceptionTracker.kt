package dev.castive.jmp.except

import dev.castive.log2.Log
import dev.castive.log2.logd
import dev.castive.log2.logv
import java.util.*
import java.util.concurrent.TimeUnit

class ExceptionTracker(
	private val blockLeak: Boolean = true,
	internal val generic: String = Exception::class.java.name
) {
	private val items = arrayListOf<Pair<String, Long>>()

	fun onExceptionTriggered(e: Throwable, time: Long = System.currentTimeMillis()) {
		"Adding exception item: ${e::class.java.name}".logv(javaClass)
		items.add(e::class.java.name to time)
		items.toTypedArray().contentToString().logd(javaClass)
	}
	fun getData(timeframe: Long = TimeUnit.MINUTES.toMillis(5)): ArrayList<Pair<String, Long>> {
		val currentTime = System.currentTimeMillis()
		val results = arrayListOf<Pair<String, Long>>()
		for (i in items.size - 1 downTo 0) {
			val dx = currentTime - items[i].second
			Log.d(javaClass, "Comparing dx: $dx to timeframe: $timeframe")
			if(dx < timeframe) {
				// Don't reveal exception information unless allowed to
				results.add(if(blockLeak) (generic to items[i].second) else items[i])
			}
		}
		Log.d(javaClass, "Items: ${items.toTypedArray().contentToString()}")
		Log.d(javaClass, "Results: ${results.toTypedArray().contentToString()}")
		return results
	}
}

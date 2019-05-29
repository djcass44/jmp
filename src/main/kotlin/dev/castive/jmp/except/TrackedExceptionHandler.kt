package dev.castive.jmp.except

import dev.castive.log2.Log

class TrackedExceptionHandler: Thread.UncaughtExceptionHandler {
	override fun uncaughtException(t: Thread?, e: Throwable?) {
		Log.v(javaClass, "${t?.name} has an uncaught exception: $e")
	}
}
/*
 *    Copyright 2019 Django Cass
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package dev.castive.jmp.util.checks

import dev.castive.jmp.except.LowEntropyException
import dev.castive.log2.Log
import dev.castive.log2.logw
import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.HealthIndicator
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

@Component
class EntropyHealthIndicator: HealthIndicator {

	internal fun getEntropyPool(): Int {
		val os = System.getProperty("os.name").toLowerCase()
		return if(os.contains("nix") || os.contains("nux") || os.contains("aix")) {
			// This is Linux, probably has 'cat'
			try {
				val process = ProcessBuilder("cat", "/proc/sys/kernel/random/entropy_avail").start()
				val result = process.inputStream.bufferedReader(StandardCharsets.UTF_8).use {
					val text = it.readText().replace("\n", "")
					Log.d(javaClass, "Entropy size: '$text'")
					return@use text.toIntOrNull() ?: -1
				}
				process.waitFor(1, TimeUnit.SECONDS)
				result
			}
			catch (e: Exception) {
				Log.e(javaClass, "Failed to read entropy pool, this may cause blocking issues [$e]")
				-1
			}
		}
		else -1
	}

	override fun health(): Health {
		val entropy = getEntropyPool()
		return when {
			entropy in 1..999 -> {
				Health.down().withDetail("Reason", "0 < $entropy < 1000")
				Log.w(javaClass, "Entropy pool is low, this will cause issues when using strong cryptography")
				throw LowEntropyException()
			}
			entropy <= 0 -> {
				"Entropy pool could not be determined, this may cause blocking issues when using strong cryptography".logw(javaClass)
				Health.unknown()
			}
			else -> Health.up()
		}.build()
	}
}

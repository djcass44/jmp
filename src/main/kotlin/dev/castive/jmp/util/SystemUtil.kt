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

package dev.castive.jmp.util

import com.django.log2.logging.Log
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

object SystemUtil {
    fun getEntropyPool(): Int {
        val os = System.getProperty("os.name").toLowerCase()
        return if(os.contains("nix") || os.contains("nux") || os.contains("aix")) {
            // This is Linux, probably has 'cat'
            try {
                val process = ProcessBuilder("cat", "/proc/sys/kernel/random/entropy_avail").start()
                val result = process.inputStream.bufferedReader(StandardCharsets.UTF_8).use {
                    val text = it.readText().strip()
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
}
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

import com.django.log2.logging.Log
import dev.castive.jmp.util.SystemUtil

class EntropyCheck: StartupCheck("Entropy pool") {
    override fun runCheck(): Boolean {
        val entropy = SystemUtil.getEntropyPool()
        return when {
            entropy in 1..999 -> {
                onFail()
                Log.w(javaClass, "Entropy pool is low, this will cause issues when using strong cryptography")
                false
            }
            entropy <= 0 -> {
                onWarning()
                Log.w(javaClass, "Entropy pool could not be determined, this may cause blocking issues when using strong cryptography")
                true
            }
            else -> {
                onSuccess()
                true
            }
        }
    }
}
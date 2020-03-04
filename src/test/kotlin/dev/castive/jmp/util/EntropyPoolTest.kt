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

import dev.castive.jmp.util.checks.EntropyHealthIndicator
import dev.castive.log2.Log
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class EntropyPoolTest {
    @Test
    fun getLocalPool() {
        val pool = EntropyHealthIndicator().getEntropyPool()
        Log.d(javaClass, "Result: $pool")
        assertNotNull(pool)
        assert(pool > 0)
    }
}

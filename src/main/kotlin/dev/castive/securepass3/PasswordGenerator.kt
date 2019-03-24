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

package dev.castive.securepass3

import java.security.SecureRandom
import java.util.*

class PasswordGenerator {
    companion object {
        private lateinit var generator: PasswordGenerator

        fun getInstance(): PasswordGenerator {
            if(!this::generator.isInitialized)
                generator = PasswordGenerator()
            return generator
        }
    }
    private val vowels = "aeiou".toCharArray()
    private val consonants = "bchfghjklmnpqrstvwxyz".toCharArray()

    private val pairs = arrayListOf<String>()

    init {
        for (v in vowels) for (c in consonants) pairs.add("" + v + c)
    }
    @Deprecated (message = "Not recommended", replaceWith = ReplaceWith("get(length)"))
    fun getInsecure(length: Int): String {
        return get(length).toString()
    }
    fun get(length: Int, strong: Boolean = false): CharArray {
        val result = CharArray(length)
        val random = if(!strong) SecureRandom() else SecureRandom.getInstanceStrong()
        for (i in 0 until (length / 2)) {
            val pair = pairs[random.nextInt(pairs.size - 1)]
            result[i * 2] = pair[0]
            result[(i * 2) + 1] = pair[1]
        }
        val s5 = random.nextBoolean()
        return if(s5) {
            val r = result.copyOfRange(0, result.size - 1)
            Arrays.fill(result, '0') // Clear the old array from memory
            r
        }
        else result
    }
}
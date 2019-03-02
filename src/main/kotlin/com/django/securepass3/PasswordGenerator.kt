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

package com.django.securepass3

import java.util.*
import kotlin.random.Random

class PasswordGenerator {
    private val vowels = "aeiou".toCharArray()
    private val consonants = "bchfghjklmnpqrstvwxyz".toCharArray()

    private val pairs = arrayListOf<String>()

    init {
        for (v in vowels) for (c in consonants) pairs.add("" + v + c)
    }
    fun get(length: Int): CharArray {
        val result = CharArray(length)
        for (i in 0 until (length / 2)) {
            val pair = pairs[Random.nextInt(pairs.size - 1)]
            result[i * 2] = pair[0]
            result[(i * 2) + 1] = pair[1]
        }
        val s5 = Random.nextBoolean()
        return if(s5) {
            val r = result.copyOfRange(0, result.size - 1)
            Arrays.fill(result, '0') // Clear the old array from memory
            r
        }
        else result
    }
}
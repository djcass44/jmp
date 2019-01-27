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

package com.django.jmp.api

import com.django.log2.logging.Log
import info.debatty.java.stringsimilarity.JaroWinkler
import java.util.*

class Similar(private val query: String, private val dict: ArrayList<String>, private val threshold: Double = 0.75) {
    fun compute(): ArrayList<String> {
        Log.d(javaClass, "Computing similar values for $query")
        val jw = JaroWinkler()
        val similarities = arrayListOf<String>()
        var best = ""
        var best_i = 0.0
        for (s in dict) {
            val metric = jw.similarity(query, s)
            if(metric > threshold)
                similarities.add(s)
            if(metric > best_i) {
                best_i = metric
                best = s
            }
            Log.v(javaClass, "s: $s, metric: $metric")
        }
        return if(similarities.size == 0)
            arrayListOf(best)
        else
            similarities
    }
}
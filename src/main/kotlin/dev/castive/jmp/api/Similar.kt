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

package dev.castive.jmp.api

import dev.castive.jmp.db.dao.JumpData
import dev.castive.jmp.util.url
import info.debatty.java.stringsimilarity.JaroWinkler

/**
 * Do a fuzzy search against a list of jumps
 */
class Similar(private val threshold: Double = 0.75) {
    fun compute(dict: ArrayList<JumpData>, query: String): ArrayList<JumpData> {
        val results = checkForDuplicates(dict, query)
        // if we have exact matches, return them straight away
        if(results.isNotEmpty()) return results
        val jw = JaroWinkler()
        var best: Pair<JumpData?, Double> = null to 0.0
        for (s in dict) {
            // generate the similarity metric
            val metric = jw.similarity(query, s.name)
            if (metric > threshold)
                // add it if it crosses the threshold
                results.add(s)
            // track 'okay' values as a fallback
            if (metric > 0.65 && metric > best.second)
                best = s to metric
        }
        // if we got no good results, fallback to the best 'okay' value
        if (results.size == 0 && best.first != null) {
            results.clear()
            results.add(best.first!!)
        }
        return results
    }
    fun computeNames(dict: ArrayList<JumpData>, query: String): List<String> {
        val results = compute(dict, query)
        // return the id suffix so that the webextension can skip the /similar hop
        return results.map { "${it.location.url()?.host}?id=${it.id}" }
    }

    // Check to see if any Jumps are exact matches
    // See #70
    private fun checkForDuplicates(dict: ArrayList<JumpData>, query: String): ArrayList<JumpData> {
        val results = arrayListOf<JumpData>()
        for (j in dict) {
            if(j.name == query) results.add(j)
        }
        return results
    }
}
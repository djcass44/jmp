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

package dev.castive.jmp.service

import dev.castive.jmp.entity.Jump
import dev.dcas.util.extend.asArrayList
import dev.dcas.util.extend.url
import info.debatty.java.stringsimilarity.JaroWinkler
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class SimilarityService {

	@Value("\${jmp.similarity.threshold:0.7}")
	private val threshold: Double = 0.70

	private val jw = JaroWinkler()

	fun forSearch(dict: List<Jump>, query: String): List<Jump> = dict.filter {
		// get the similarity
		val metrics = arrayListOf<Double>()
		metrics.add(jw.similarity(query, it.name))
		metrics.add(jw.similarity(query, it.location))
		// this may be null so default to 0 (no match)
		it.title?.let { s ->
			// basic tokenisation of something we know is probably a sentence
			metrics.addAll(s.split(" ").map { d ->
				jw.similarity(query, d)
			})
		}
		return@filter metrics.any { d ->
			d >= threshold
		}
	}

	/**
	 * Fuzzy search against a list of jumps
	 * If any match exactly, there will be the only results
	 */
	fun forJumping(dict: ArrayList<Jump>, query: String): ArrayList<Jump> {
		val results = checkForDuplicates(dict, query)
		// if we have exact matches, return them straight away
		if(results.isNotEmpty()) return results
		var best: Pair<Jump?, Double> = null to 0.0
		dict.forEach {
			// generate the similarity metric
			val metric = jw.similarity(query, it.name)
			if (metric > threshold)
			// add it if it crosses the threshold
				results.add(it)
			// track 'okay' values as a fallback
			if (metric > 0.65 && metric > best.second)
				best = it to metric
		}
		// if we got no good results, fallback to the best 'okay' value
		if (results.size == 0 && best.first != null) {
			results.clear()
			results.add(best.first!!)
		}
		return results
	}

	/**
	 * return the id suffix so that the webextension can skip the /similar hop
	 */
	fun forSuggesting(dict: ArrayList<Jump>, query: String): List<String> = forJumping(dict, query).map {
		"${it.location.url()?.host}&id=${it.id}"
	}

	// Check to see if any Jumps are exact matches
	// See #70
	private fun checkForDuplicates(dict: ArrayList<Jump>, query: String): ArrayList<Jump> = dict.filter {
		it.name == query
	}.asArrayList()
}

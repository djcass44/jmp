/*
 *    Copyright 2020 Django Cass
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

import dev.castive.jmp.entity.Alias
import dev.castive.jmp.entity.Jump
import dev.castive.jmp.repo.JumpRepo
import dev.castive.log2.logd
import dev.castive.log2.loge
import dev.castive.log2.logi
import dev.castive.log2.logw
import org.hibernate.search.jpa.Search
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.util.concurrent.Future
import javax.annotation.PostConstruct
import javax.persistence.EntityManagerFactory

@Service
class SearchService(
	entityManagerFactory: EntityManagerFactory,
	private val jumpRepo: JumpRepo
) {
	private val fullTextEntityManager = Search.getFullTextEntityManager(entityManagerFactory.createEntityManager())
	private var index: Future<*>? = null

	@PostConstruct
	fun init() {
		// build indices
		index = fullTextEntityManager.createIndexer().start()
		"Starting Lucene index builder asynchronously...".logi(javaClass)
	}

	private fun searchAliases(term: String): List<Jump> {
		if(index == null || !index!!.isDone) {
			"[Alias] Cannot perform search as indices haven't been built".logw(javaClass)
			return emptyList()
		}
		val queryBuilder = fullTextEntityManager.searchFactory
			.buildQueryBuilder()
			.forEntity(Alias::class.java)
			.get()
		val query = queryBuilder
			.bool()
			.should(queryBuilder
				.keyword()
				.wildcard()
				.onField("name")
				.matching("*$term*")
				.createQuery()
			)
			.createQuery()
		val jpaQuery = fullTextEntityManager.createFullTextQuery(query, Alias::class.java)
		val results = mutableSetOf<Jump>()
		jpaQuery.resultList.forEach {
			kotlin.runCatching {
				// convert the result back into our entity
				jumpRepo.findByIdOrNull((it as Alias).parent)?.let(results::add)
			}.onFailure {
				"[Alias] Failed to load Jump from Alias, searchTerm: $term".loge(javaClass, it)
			}
		}
		"[Alias] Executed full-text-search for '${term}': returned ${results.size} aliases".logd(javaClass)
		return results.toList()
	}

	fun search(term: String): List<Jump> {
		if(index == null || !index!!.isDone) {
			"[Jump] Cannot perform search as indices haven't been built".logw(javaClass)
			return emptyList()
		}
		val queryBuilder = fullTextEntityManager.searchFactory
			.buildQueryBuilder()
			.forEntity(Jump::class.java)
			.get()
		val query = queryBuilder
			.bool()
			.should(queryBuilder
				.keyword()
				.wildcard()
				.onFields("name", "location", "title")
				.matching("*$term*")
				.createQuery()
			)
			.createQuery()
		val jpaQuery = fullTextEntityManager.createFullTextQuery(query, Jump::class.java)
		// ensure there are no duplicates by mapping the id
		val results = hashMapOf<Int, Jump>()
		jpaQuery.resultList.forEach {
			kotlin.runCatching {
				// convert the result back into our entity
				val j = it as Jump
				results[j.id] = j
			}.onFailure {
				"[Jump] Failed to cast JpaQuery object to ${Jump::class.java.name}: searchTerm: $term".loge(javaClass, it)
			}
		}
		"[Jump] Executed full-text-search for '$term': returned ${results.size} items".logd(javaClass)
		val aliases = searchAliases(term)
		aliases.forEach {
			if(!results.containsKey(it.id))
				results[it.id] = it
		}
		"[Jump] Total results for '$term': ${results.size}".logd(javaClass)
		return results.values.toList()
	}
}

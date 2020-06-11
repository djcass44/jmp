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

import dev.castive.jmp.entity.Jump
import dev.castive.log2.*
import org.apache.lucene.search.Query
import org.hibernate.search.jpa.Search
import org.hibernate.search.query.dsl.QueryBuilder
import org.springframework.stereotype.Service
import java.util.concurrent.Future
import javax.annotation.PostConstruct
import javax.persistence.EntityManagerFactory

@Service
class SearchService(
	entityManagerFactory: EntityManagerFactory
) {
	private val fullTextEntityManager = Search.getFullTextEntityManager(entityManagerFactory.createEntityManager())
	private var index: Future<*>? = null

	private val queryBuilder: QueryBuilder = fullTextEntityManager.searchFactory
		.buildQueryBuilder()
		.forEntity(Jump::class.java)
		.get()

	@PostConstruct
	fun init() {
		// build indices
		index = fullTextEntityManager.createIndexer().start()
		"Starting Lucene index builder asynchronously...".logi(javaClass)
	}

	/**
	 * Generates a query with wildcarding.
	 * This query is useful for a basic gimme-everything search
	 */
	private fun wildcardQuery(term: String): Query = queryBuilder
		.bool()
		.should(queryBuilder
			.keyword()
			.wildcard()
			.onFields("name", "location", "title", "alias.name")
			.matching("*$term*")
			.createQuery()
		)
		.createQuery()

	fun search(term: String, query: Query = wildcardQuery(term)): List<Jump> {
		if(index == null || !index!!.isDone) {
			"[Jump] Cannot perform search as indices haven't been built".logw(javaClass)
			return emptyList()
		}
		val jpaQuery = fullTextEntityManager.createFullTextQuery(query, Jump::class.java)
		// ensure there are no duplicates by mapping the id
		val results = hashMapOf<Int, Jump>()
		if(jpaQuery.resultSize == 0 || jpaQuery.resultList == null) {
			"Got no results or resultset was null".logv(javaClass)
			return emptyList() // catch npe before the ::forEach hits it
		}
		jpaQuery.resultList.forEach {
			kotlin.runCatching {
				// convert the result back into our entity
				val j = it as Jump
				results[j.id] = j
			}.onFailure {
				"Failed to cast JpaQuery object to ${Jump::class.java.name}: searchTerm: $term".loge(javaClass, it)
			}
		}
		"Executed full-text-search for '$term': returned ${results.size} items".logd(javaClass)
		return results.values.toList()
	}
}

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

import dev.castive.log2.logd
import dev.castive.log2.loge
import dev.castive.log2.logi
import dev.castive.log2.logw
import org.hibernate.search.jpa.Search
import org.springframework.stereotype.Service
import java.util.concurrent.Future
import javax.annotation.PostConstruct
import javax.persistence.EntityManagerFactory

@Service
class SearchService(entityManagerFactory: EntityManagerFactory) {
	private val fullTextEntityManager = Search.getFullTextEntityManager(entityManagerFactory.createEntityManager())
	private var index: Future<*>? = null

	@PostConstruct
	fun init() {
		// build indices
		index = fullTextEntityManager.createIndexer().start()
		"Starting Lucene index builder asynchronously...".logi(javaClass)
	}

	fun <T> search(entityType: Class<T>, term: String): List<T> {
		if(index == null || !index!!.isDone) {
			"Cannot perform search as indices haven't been built".logw(javaClass)
			return emptyList()
		}
		val queryBuilder = fullTextEntityManager.searchFactory
			.buildQueryBuilder()
			.forEntity(entityType)
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
		val jpaQuery = fullTextEntityManager.createFullTextQuery(query, entityType)
		val results = arrayListOf<T>()
		jpaQuery.resultList.forEach {
			kotlin.runCatching {
				// convert the result back into our entity
				results.add(it as T)
			}.onFailure {
				"Failed to cast JpaQuery object to ${entityType.name}: searchTerm: $term".loge(javaClass)
			}
		}
		"Executed FTS for '${term}': returned ${results.size} items".logd(javaClass)
		return results
	}
}

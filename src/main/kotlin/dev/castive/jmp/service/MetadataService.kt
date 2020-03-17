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

import dev.castive.jmp.data.FSA
import dev.castive.jmp.data.FaviconPayload
import dev.castive.jmp.prop.AppMetadataProps
import dev.castive.jmp.repo.JumpRepo
import dev.castive.jmp.util.broadcast
import dev.castive.log2.logd
import dev.castive.log2.loge
import dev.castive.log2.logi
import dev.castive.log2.logv
import dev.dcas.util.extend.isESNullOrBlank
import dev.dcas.util.extend.safe
import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.utils.io.core.use
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.springframework.stereotype.Service
import java.net.URI

@Service
class MetadataService(
	private val jumpRepo: JumpRepo,
	private val metadataProps: AppMetadataProps
) {
	private val tagTitle = "<title>"

	/**
	 * Get a new HttpClient
	 * This is mostly here for easy test mocking
	 */
	internal fun getClient(): HttpClient = HttpClient(Apache)

	internal fun getProbableUrl(address: String): String {
		return kotlin.runCatching {
			val uri = URI(address)
			"https://${uri.host}"
		}.onFailure {
			"Failed to mangle address: $address".loge(javaClass, it)
		}.getOrNull() ?: address
	}

	internal suspend fun getTitle(address: String): String? {
		try {
			val host = getProbableUrl(address)
			val content = getClient().use {
				it.get<String>(host)
			}
			if (content.isESNullOrBlank()) {
				"Got null or blank data for address: $host".logi(MetadataService::class.java)
				return null
			}
			val start = content.indexOf(tagTitle) + tagTitle.length
			val end = content.indexOf("</title>", start)
			val title = content.substring(start, end)
			if (title.isESNullOrBlank()) {
				"Got null or blank title for address: $host".logi(MetadataService::class.java)
				return null
			}
			"Got title for address: $address, $title".logd(MetadataService::class.java)
			return title
		}
		catch (e: Exception) {
			"Failed to load title metadata for address: $address".loge(MetadataService::class.java, e)
			return null
		}
	}

	fun updateTitle(address: String): String? {
		if (!metadataProps.title.enabled) {
			"Refusing to scrape title metadata: egress is disabled".logv(MetadataService::class.java)
			return null
		}
		if (!address.startsWith("https://")) {
			"Refusing to process non-HTTPS domain: $address".logi(MetadataService::class.java)
			return null
		}
		// attempt to get the title or silently return (this::getTitle does the logging)
		val title = runBlocking {
			getTitle(address)
		} ?: return null
		// update all the jumps which use this url
		"Updating jumps with address: $address to use new title: '$title'".logd(MetadataService::class.java)
		jumpRepo.updateTitleWithAddress(address, title)
		jumpRepo.findAllByLocation(address).forEach {
			if (it.title == null || it.title != title) {
				"Updating title for ${it.name}, was: ${it.title}, now: $title".logv(MetadataService::class.java)
			}
			FSA(FSA.EVENT_UPDATE_TITLE, FaviconPayload(it.id, title)).broadcast()
		}
		return title
	}

	/**
	 * Warms the image service
	 */
	fun warmIconService(address: String): Job = GlobalScope.launch {
		if(!metadataProps.icon.enabled) {
			"Refusing to scrape image metadata: egress is disabled".logv(MetadataService::class.java)
			return@launch
		}
		if(metadataProps.icon.url.isESNullOrBlank()) {
			"Cannot scrape image metadata: blank or null url set".logv(MetadataService::class.java)
			return@launch
		}
		val destUrl = "${metadataProps.icon.url}/icon?site=${address.safe()}"
		kotlin.runCatching {
			getClient().use {
				val response = it.get<HttpResponse>(destUrl)
				"Got response when warming icon service: ${response.status.description}".logv(javaClass)
			}
		}.onFailure {
			"Caught exception when warming icon service: $address, $destUrl".loge(MetadataService::class.java, it)
		}
	}
}

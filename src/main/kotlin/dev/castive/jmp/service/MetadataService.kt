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

import dev.castive.jmp.component.SocketHandler
import dev.castive.jmp.data.FSA
import dev.castive.jmp.data.FaviconPayload
import dev.castive.jmp.repo.JumpRepo
import dev.castive.log2.loge
import dev.castive.log2.logi
import dev.castive.log2.logv
import dev.dcas.util.extend.safe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jsoup.Jsoup
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.getForEntity
import java.net.URI

@Service
class MetadataService @Autowired constructor(
	private val jumpRepo: JumpRepo,
	private val restTemplate: RestTemplate
) {
	@Value("\${jmp.metadata.title.enabled}")
	private val allowTitle: Boolean = true

	@Value("\${jmp.metadata.icon.enabled}")
	private val allowImage: Boolean = true

	@Value("\${jmp.metadata.icon.url}")
	private lateinit var iconUrl: String

	fun getTitle(address: String) = GlobalScope.launch(Dispatchers.IO) {
		if(!allowTitle) {
			"Refusing to scrape title metadata: egress is disabled".logv(MetadataService::class.java)
			return@launch
		}
		if(!address.startsWith("https://")) {
			"Refusing to process non-HTTPS domain: $address".logi(MetadataService::class.java)
			return@launch
		}
		try {
			val uri = URI(address)
			val host = "https://${uri.host}"
			val document = Jsoup.connect(host).get()
			// get the <title/> text
			val title = document.head().getElementsByTag("title").text()
			jumpRepo.saveAll(jumpRepo.findAllByLocation(address).map {
				if(it.title == null || it.title != title) {
					"Updating title for ${it.name}, was: ${it.title}, now: $title".logv(MetadataService::class.java)
				}
				it.title = title
				SocketHandler.broadcast(FSA(FSA.EVENT_UPDATE_TITLE, FaviconPayload(it.id, title)))
				return@map it
			})
		}
		catch (e: Exception) {
			"Failed to load title for $address, $e".loge(MetadataService::class.java)
		}
	}

	fun getImage(address: String) = GlobalScope.launch(Dispatchers.IO) {
		if(!allowImage) {
			"Refusing to scrape image metadata: egress is disabled".logv(MetadataService::class.java)
			return@launch
		}
		val destUrl = "$iconUrl/icon?site=${address.safe()}"
		try {
			restTemplate.getForEntity<String>(destUrl)
			jumpRepo.saveAll(jumpRepo.findAllByLocation(address).map {
				it.image = destUrl
				SocketHandler.broadcast(FSA(FSA.EVENT_UPDATE_FAVICON, FaviconPayload(it.id, destUrl)))
				return@map it
			})
		}
		catch (e: Exception) {
			"Failed favicon metadata location request: $address, $e".loge(MetadataService::class.java)
		}
	}
}

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
import dev.castive.jmp.repo.JumpRepo
import dev.castive.jmp.util.broadcast
import dev.castive.log2.loge
import dev.castive.log2.logi
import dev.castive.log2.logv
import dev.dcas.util.extend.isESNullOrBlank
import dev.dcas.util.extend.safe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.jsoup.Jsoup
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.net.URI

@Service
class MetadataService @Autowired constructor(
	private val jumpRepo: JumpRepo
) {
	@Value("\${jmp.metadata.title.enabled:true}")
	private val allowTitle: Boolean = true

	@Value("\${jmp.metadata.icon.enabled:false}")
	private val allowImage: Boolean = true

	@Value("\${jmp.metadata.icon.url:}")
	private lateinit var iconUrl: String

	fun getTitle(address: String): Job = GlobalScope.launch(Dispatchers.IO) {
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
			val title: String? = document.head().getElementsByTag("title").text()
			if(title.isNullOrBlank()) {
				"Got null or blank title for address: $address".logi(MetadataService::class.java)
				return@launch
			}
			jumpRepo.updateTitleWithAddress(address, title)
			jumpRepo.findAllByLocation(address).forEach {
				if(it.title == null || it.title != title) {
					"Updating title for ${it.name}, was: ${it.title}, now: $title".logv(MetadataService::class.java)
				}
				FSA(FSA.EVENT_UPDATE_TITLE, FaviconPayload(it.id, title)).broadcast()
			}
		}
		catch (e: Exception) {
			"Failed to load title for $address, $e".loge(MetadataService::class.java)
		}
	}

	fun getImage(address: String): String? {
		if(!allowImage) {
			"Refusing to scrape image metadata: egress is disabled".logv(MetadataService::class.java)
			return null
		}
		if(!this::iconUrl.isInitialized || iconUrl.isESNullOrBlank()) {
			"Cannot scrape image metadata: blank or null url set".logv(MetadataService::class.java)
			return null
		}
		val destUrl = "$iconUrl/icon?site=${address.safe()}"
		GlobalScope.launch {
			try {
				jumpRepo.updateIconWithAddress(address, destUrl)
				jumpRepo.findAllByLocation(address).forEach {
					FSA(FSA.EVENT_UPDATE_FAVICON, FaviconPayload(it.id, destUrl)).broadcast()
				}
			}
			catch (e: Exception) {
				"Failed favicon metadata location request: $address, $destUrl".loge(MetadataService::class.java, e)
			}
		}
		return destUrl
	}
}

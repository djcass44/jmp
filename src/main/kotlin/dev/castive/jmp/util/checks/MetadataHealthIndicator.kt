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

package dev.castive.jmp.util.checks

import dev.castive.jmp.prop.AppMetadataProps
import dev.castive.log2.loge
import dev.castive.log2.logi
import dev.castive.log2.logv
import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.HealthIndicator
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.getForEntity

@Component
class MetadataHealthIndicator(
	private val restTemplate: RestTemplate,
	private val appMetadataProps: AppMetadataProps
): HealthIndicator {

	override fun health(): Health {
		val iconUrl = appMetadataProps.icon.url
		if(iconUrl.isBlank()) {
			"Icon loader doesn't appear to be configured".logi(javaClass)
			return Health.unknown().withDetail("Reason", "Not configured").build()
		}
		if(!appMetadataProps.icon.enabled) {
			"Unable to perform icon loader health check due to egress policy".logi(javaClass)
			return Health.unknown().withDetail("Reason", "Egress policy").build()
		}

		"Running health check @ $iconUrl/ping".logv(javaClass)
		// do a standard http health check
		val httpCheck = kotlin.runCatching {
			restTemplate.getForEntity<String>("$iconUrl/ping")
		}.getOrNull()
		"Got response from $iconUrl: ${httpCheck?.statusCodeValue}".logv(javaClass)
		if(httpCheck == null || httpCheck.statusCode != HttpStatus.OK) {
			"Got non-OK response from $iconUrl, ${httpCheck?.statusCodeValue ?: -1}".loge(javaClass)
			// return 200 anyway so that our app isn't killed
			return Health.unknown().withDetail("Status Code", httpCheck?.statusCodeValue ?: -1).build()
		}
		if(!httpCheck.hasBody()) {
			"No body in response from $iconUrl".loge(javaClass)
			Health.unknown().withDetail("Reason", "No body returned").build()
		}
		// consider a 200 good enough
		return Health.up().build()
	}
}

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

package dev.castive.jmp.config

import dev.castive.jmp.component.SocketHandler
import dev.castive.jmp.prop.AppSecurityProps
import dev.castive.log2.loga
import dev.castive.log2.logi
import org.springframework.context.annotation.Configuration
import org.springframework.web.socket.config.annotation.EnableWebSocket
import org.springframework.web.socket.config.annotation.WebSocketConfigurer
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry

@Configuration
@EnableWebSocket
class WebSocketConfig(
	private val appSecurityConfig: AppSecurityProps,
	private val socketHandler: SocketHandler
): WebSocketConfigurer {

	override fun registerWebSocketHandlers(registry: WebSocketHandlerRegistry) {
		registry.addHandler(socketHandler, "/ws2").apply {
			if(appSecurityConfig.allowCors) {
				"Enabling CORS requests for WebSocket resources".loga(javaClass)
				setAllowedOrigins("*")
			}
			else {
				"Using baseUrl: ${appSecurityConfig.baseUrl}".logi(javaClass)
				setAllowedOrigins(appSecurityConfig.baseUrl)
			}
		}
	}
}

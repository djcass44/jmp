/*
 *    Copyright [2019 Django Cass
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

import dev.castive.jmp.util.forSocket
import dev.castive.log2.loge
import dev.castive.log2.logi
import dev.castive.log2.logv
import dev.dcas.castive_utilities.extend.isESNullOrBlank
import dev.dcas.castive_utilities.extend.parse
import io.javalin.websocket.WsMessageContext

/**
 * Handles incoming WebSocket messages and ferries them to be processed
 * Non-trivial processing should be done within coroutines so that the handler is not held-up
 */
class SocketHandler {
	fun handle(messageContext: WsMessageContext) {
		val data = messageContext.message()
		if(data.isESNullOrBlank()) {
			"Got unreadable or null message from client: ${messageContext.sessionId}".logi(javaClass)
			// send the message back in case the ui wants to do forensics
			messageContext.send((Socket.WS_SEND_FAILURE to "Data is null or undefined").forSocket(true, data))
			return
		}
		val fsa = try {
			val fsa = data.parse(Socket.Payload::class.java)
			// may not be senseless if GSON is setting the value to null
			@Suppress("SENSELESS_COMPARISON")
			if(fsa.type == null)
				throw NullPointerException("${Socket.Payload::class.java.name}::type cannot be null")
			fsa
		}
		catch (e: Exception) {
			"Failed to unmarshal message from client: [from: ${messageContext.sessionId}, data: $data]".loge(javaClass)
			// send the message back in case the ui wants to do forensics
			messageContext.send((Socket.WS_SEND_FAILURE to "Data may not comply with FSA").forSocket(true, data))
			return
		}
		when(fsa.type) {
			Socket.EVENT_PING -> {
				// respond with a simple message to let the client know we are still connected
				messageContext.send((Socket.EVENT_PING to "PONG").forSocket())
			}
		}
		"Handling socket request for: ${fsa.type} for ${messageContext.sessionId}".logv(javaClass)
	}
}

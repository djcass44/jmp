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

package dev.castive.jmp.component

import dev.castive.jmp.api.App
import dev.castive.jmp.data.FSA
import dev.castive.jmp.util.send
import dev.castive.log2.loge
import dev.castive.log2.logv
import dev.dcas.util.extend.isESNullOrBlank
import dev.dcas.util.extend.parse
import org.springframework.context.annotation.Bean
import org.springframework.scheduling.TaskScheduler
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler
import org.springframework.stereotype.Component
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.TextWebSocketHandler
import java.util.concurrent.ConcurrentHashMap

@Component
class SocketHandler: TextWebSocketHandler() {
	companion object {
		val sessions = ConcurrentHashMap<String, WebSocketSession>()

		/**
		 * Send a socket message to a session with a specific id
		 */
		fun sendTo(id: String, data: FSA) {
			sessions.forEach { (t: String, u: WebSocketSession) ->
				if(t == id) {
					u.send(data)
					return
				}
			}
		}

		/**
		 * Send a socket message to all connected sessions
		 */
		fun broadcast(data: FSA) {
			sessions.forEach { (_: String, u: WebSocketSession) ->
				u.send(data)
			}
		}
	}

	override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
		val data = message.payload
		if(data.isESNullOrBlank()) {
			"Got unreadable or null message from client: ${session.id}".logv(javaClass)
			// send the message back ins case the ui wants to do forensics
			session.send(FSA(FSA.WS_SEND_FAILURE, "Data is null or undefined", true, data))
			return
		}
		val fsa = try {
			val fsa = data.parse(FSA::class.java)
			// may not be useless if GSON is setting the value to null
			@Suppress("UselessCallOnNotNull")
			if(fsa.type.isNullOrBlank())
				throw NullPointerException("${FSA::class.java.name}::type cannot be null")
			fsa
		}
		catch (e: Exception) {
			"Failed to unmarshal message from client: ${session.id}, data: $data".loge(javaClass)
			session.send(FSA(FSA.WS_SEND_FAILURE, "Data may not comply with FSA", true, data))
			return
		}
		when(fsa.type) {
			FSA.TYPE_PING -> {
				// response with a simple message to keep the client connected
				session.send(FSA(FSA.TYPE_PING, "PONG"))
			}
		}
		"Handling socket request for: ${fsa.type} for ${session.id}".logv(javaClass)
	}

	override fun afterConnectionEstablished(session: WebSocketSession) {
		super.afterConnectionEstablished(session)
		sessions[session.id] = session
		"Established websocket connection with ${session.id}".logv(javaClass)
		// send init message
		session.send(FSA(FSA.INIT_APP, App.id))
	}

	override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
		super.afterConnectionClosed(session, status)
		sessions.remove(session.id)
		"Lost websocket connection with ${session.id}".logv(javaClass)
	}

	/**
	 * Send a keep-alive message to all listening clients every 10 seconds
	 * This ensures that the browser keeps the socket open
	 */
	@Scheduled(fixedDelay = 10_000)
	fun socketHeartbeat() {
		broadcast(FSA(FSA.TYPE_PING, "ping"))
	}

	@Bean
	fun taskScheduler(): TaskScheduler = ThreadPoolTaskScheduler().apply {
		poolSize = 2
		setThreadNamePrefix("scheduled-task-${javaClass.module.name}")
		isDaemon = true
	}
}

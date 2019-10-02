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
import dev.castive.jmp.util.json
import io.javalin.websocket.WsMessageContext
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*

class SocketHandlerTest {
	@Test
	fun `test ping`() {
		val msgCtx = mock(WsMessageContext::class.java)
		`when`(msgCtx.message()).thenReturn(Socket.Payload(
			Socket.EVENT_PING,
			null
		).json())

		// this is what we expect back
		val expected = Socket.Payload(Socket.EVENT_PING, "PONG")

		val handler = SocketHandler()
		handler.handle(msgCtx)
		verify(msgCtx, times(1)).send(expected)
	}

	@Test
	fun `test null or blank`() {
		val data = "null"

		val msgCtx = mock(WsMessageContext::class.java)
		`when`(msgCtx.message()).thenReturn(
			data
		)

		// this is what we expect back
		val expected = (Socket.WS_SEND_FAILURE to "Data is null or undefined").forSocket(true, data)

		val handler = SocketHandler()
		handler.handle(msgCtx)
		verify(msgCtx, times(1)).send(expected)
	}

	@Test
	fun `test not FSA`() {
		val data = ("test" to 0).json()

		val msgCtx = mock(WsMessageContext::class.java)
		`when`(msgCtx.message()).thenReturn(
			data
		)

		// this is what we expect back
		val expected = (Socket.WS_SEND_FAILURE to "Data may not comply with FSA").forSocket(true, data)

		val handler = SocketHandler()
		handler.handle(msgCtx)
		verify(msgCtx, times(1)).send(expected)
	}
}
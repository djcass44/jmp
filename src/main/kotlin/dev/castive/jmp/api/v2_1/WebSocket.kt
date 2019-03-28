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

package dev.castive.jmp.api.v2_1

import com.django.log2.logging.Log
import dev.castive.jmp.api.Runner
import io.javalin.apibuilder.ApiBuilder.ws
import io.javalin.apibuilder.EndpointGroup
import io.javalin.websocket.WsSession

class WebSocket: EndpointGroup {
    companion object {
        const val EVENT_UPDATE = "EVENT_UPDATE"
        const val EVENT_UPDATE_USER = "EVENT_UPDATE_USER"
        const val EVENT_UPDATE_GROUP = "EVENT_UPDATE_GROUP"
    }

    private val sessions = arrayListOf<WsSession>()

    override fun addEndpoints() {
        ws("${Runner.BASE}/ws") { ws ->
            ws.onConnect {
                Log.d(javaClass, "WebSocket connected: ${it.host()}")
                sessions.add(it)
            }
            ws.onClose { session, statusCode, reason ->
                Log.d(javaClass, "WebSocket disconnected: ${session.host()}, $statusCode, $reason")
                sessions.remove(session)
            }
            ws.onMessage { session, msg -> Log.d(javaClass, "WebSocket message: $msg from ${session.host()}") }
        }
    }

    fun fire(msg: String) {
        Log.v(javaClass, "Firing $msg to ${sessions.size} listeners")
        sessions.forEach { it.send(msg) }
    }
}
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

import com.corundumstudio.socketio.Configuration
import com.corundumstudio.socketio.SocketConfig
import com.corundumstudio.socketio.SocketIOServer
import dev.castive.jmp.db.Util
import dev.castive.log2.Log
import io.javalin.apibuilder.EndpointGroup

class WebSocket : EndpointGroup {
    companion object {
        const val EVENT_UPDATE = "EVENT_UPDATE"
        const val EVENT_UPDATE_USER = "${EVENT_UPDATE}_USER"
        const val EVENT_UPDATE_GROUP = "${EVENT_UPDATE}_GROUP"
        const val EVENT_UPDATE_FAVICON = "${EVENT_UPDATE}_FAVICON"
        const val EVENT_UPDATE_TITLE = "${EVENT_UPDATE}_TITLE"

        val allowSockets = Util.getEnv("SOCKET_ENABLED", "true").toBoolean()
    }
    private val server = SocketIOServer(Configuration().apply {
        hostname = Util.getEnv("SOCKET_HOST", "0.0.0.0")
        port = (Util.getEnv("SOCKET_PORT", "7001").toLongOrNull() ?: 7001).toInt()
        socketConfig = SocketConfig().apply { isReuseAddress = true }
    })

    override fun addEndpoints() {
        server.apply {
            addConnectListener {
                Log.d(javaClass, "WebSocket connected: ${it.remoteAddress}")
            }
            addDisconnectListener {
                Log.d(javaClass, "WebSocket disconnected: ${it.remoteAddress}")
            }
        }.startAsync()
    }

    fun fire(tag: String, data: Any) {
        if(!allowSockets) {
            Log.i(javaClass, "Unable to broadcast as Sockets are disabled")
        }
        Log.v(javaClass, "Broadcasting $tag event to ${server.allClients.size} listeners")
        server.allClients.forEach { it.sendEvent(tag, data) }
    }
}
data class FaviconPayload(val id: Int, val url: String)

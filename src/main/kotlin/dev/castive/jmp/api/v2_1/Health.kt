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

import dev.castive.jmp.api.Runner
import com.django.log2.logging.Log
import io.javalin.apibuilder.ApiBuilder.get
import io.javalin.apibuilder.ApiBuilder.ws
import io.javalin.apibuilder.EndpointGroup
import io.javalin.websocket.WsSession
import org.eclipse.jetty.http.HttpStatus
import kotlin.concurrent.fixedRateTimer

class Health: EndpointGroup {
    private val userSessions = arrayListOf<WsSession>()

    override fun addEndpoints() {
        get("${Runner.BASE}/v2_1/health", { ctx ->
            ctx.status(HttpStatus.OK_200).result("OK")
        }, dev.castive.jmp.api.Auth.defaultRoleAccess)
        ws("${Runner.BASE}/ws/2_1/health") { ws ->
            ws.onConnect { session ->
                // User has connected
                userSessions.add(session)
                Log.v(javaClass, "ws | ${session.host()} has connected [/ws/2_1/health]")
            }
            ws.onClose { session, statusCode, reason ->
                // User has disconnected
                userSessions.remove(session)
                Log.v(javaClass, "ws | ${session.host()} has disconnected [/ws/2_1/health, code: $statusCode, reason: $reason]")
            }
            ws.onMessage { session, msg -> session.send(msg) } // If anything is sent, send it back
        }
    }

    fun startHeartbeat() = fixedRateTimer(javaClass.name, true, 0, 5000) {
        Log.v(javaClass, "Heartbeat firing...")
        userSessions.forEach {
            it.send("OK") // Inform each session that we are okay
        }
    }
}
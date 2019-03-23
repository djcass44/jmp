package com.django.jmp.api.v2_1

import com.django.jmp.api.Auth
import com.django.jmp.api.Runner
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
        }, Auth.defaultRoleAccess)
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
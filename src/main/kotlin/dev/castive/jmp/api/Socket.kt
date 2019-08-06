package dev.castive.jmp.api

import dev.castive.jmp.api.actions.AuthAction
import dev.castive.jmp.api.v2_1.WebSocket
import dev.castive.log2.Log
import io.javalin.apibuilder.ApiBuilder
import io.javalin.apibuilder.EndpointGroup
import io.javalin.websocket.WsContext
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

class Socket: EndpointGroup {
	data class Payload(
		val tag: String,
		val data: Any?
	)

	private val sessions = ConcurrentHashMap<String, WsContext>()

	override fun addEndpoints() {
		ApiBuilder.ws("/ws2", { ws ->
			ws.onConnect { ctx ->
				Log.v(javaClass, "Socket connection established: ${ctx.sessionId}, ${ctx.session.remoteAddress}")
				sessions[ctx.sessionId] = ctx
				onConnect(ctx)
			}
			ws.onClose { ctx ->
				Log.v(javaClass, "Socket connection established: ${ctx.sessionId}, ${ctx.session.remoteAddress}")
				sessions.remove(ctx.sessionId)
			}
		}, Auth.openAccessRole)
	}

	private fun onConnect(ctx: WsContext) {
		ctx.send(Payload(WebSocket.INIT_APP, AuthAction.cacheLayer.getMisc("appId")))
	}

	/**
	 * Broadcasts a message to all listening sessions
	 */
	fun fire(tag: String, data: Any?) = GlobalScope.launch {
		Log.v(javaClass, "Broadcasting $tag event to ${sessions.size} listeners")
		var invalid = 0
		sessions.values.forEach {
			if(it.session.isOpen) it.send(Payload(tag, data))
			else invalid++
		}
		Log.i(javaClass, "Unable to send to $invalid closed sessions")
	}
}
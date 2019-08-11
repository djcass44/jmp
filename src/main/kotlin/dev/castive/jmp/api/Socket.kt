package dev.castive.jmp.api

import dev.castive.jmp.Runner
import dev.castive.jmp.cache.BaseCacheLayer
import dev.castive.log2.Log
import io.javalin.apibuilder.ApiBuilder
import io.javalin.apibuilder.EndpointGroup
import io.javalin.websocket.WsContext
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

class Socket(private val cache: BaseCacheLayer): EndpointGroup {
	companion object {
		const val INIT_APP = "INIT_APP"
		const val EVENT_UPDATE = "EVENT_UPDATE"
		const val EVENT_UPDATE_USER = "${EVENT_UPDATE}_USER"
		const val EVENT_UPDATE_GROUP = "${EVENT_UPDATE}_GROUP"
		const val EVENT_UPDATE_FAVICON = "${EVENT_UPDATE}_FAVICON"
		const val EVENT_UPDATE_TITLE = "${EVENT_UPDATE}_TITLE"
	}

	data class FaviconPayload(val id: Int, val url: String)
	// Complies with Flux Standard Action (https://github.com/redux-utilities/flux-standard-action)
	data class Payload(
		val type: String,
		val payload: Any?,
		val error: Boolean = false,
		val meta: Any? = null
	)

	private val sessions = ConcurrentHashMap<String, WsContext>()

	override fun addEndpoints() {
		ApiBuilder.ws("${Runner.BASE}/ws2", { ws ->
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
		ctx.send(Payload(INIT_APP, cache.get("appId")))
	}

	/**
	 * Broadcasts a message to all listening sessions
	 */
	fun fire(tag: String, data: Any?) {
		GlobalScope.launch {
			Log.v(javaClass, "Broadcasting $tag event to ${sessions.size} listeners")
			var invalid = 0
			sessions.values.forEach {
				if(it.session.isOpen) it.send(Payload(tag, data))
				else invalid++
			}
			Log.i(javaClass, "Unable to send to $invalid closed sessions")
		}
	}
}
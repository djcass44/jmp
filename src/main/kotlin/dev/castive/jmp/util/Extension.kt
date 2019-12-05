package dev.castive.jmp.util

import com.amdelamar.jhash.Hash
import dev.castive.jmp.component.SocketHandler
import dev.castive.jmp.data.FSA
import dev.castive.log2.logv
import dev.dcas.util.extend.json
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession

/**
 * Convert a List to kotlin.collections.ArrayList
 */
fun <T> List<T>.asArrayList(): ArrayList<T> = ArrayList(this)

fun WebSocketSession.send(data: FSA) {
	if(isOpen) {
		synchronized(this) {
			sendMessage(TextMessage(data.json()))
		}
	}
	else
		"Unable to send message to closed socket: $id".logv(javaClass)
}
fun FSA.broadcast() = SocketHandler.broadcast(this)

fun String.hash(): String = Hash.password(this.toCharArray()).create()

fun String.ellipsize(after: Int = length): String = "${substring(0, after)}..."

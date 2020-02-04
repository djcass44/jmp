package dev.castive.jmp.util

import dev.castive.jmp.component.SocketHandler
import dev.castive.jmp.data.FSA
import dev.castive.log2.loge
import dev.castive.log2.logv
import dev.dcas.jmp.security.shim.entity.User
import dev.dcas.jmp.spring.security.model.UserPrincipal
import dev.dcas.util.extend.json
import dev.dcas.util.spring.responses.UnauthorizedResponse
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession


fun WebSocketSession.send(data: FSA) {
	if(isOpen) {
		synchronized(this) {
			sendMessage(TextMessage(data.json()))
		}
	}
	else
		"Unable to send message to closed socket: $id".logv(javaClass)
}
fun FSA.broadcast(): Unit = SocketHandler.broadcast(this)

fun SecurityContext.user(): User? = kotlin.runCatching {
	if(SecurityContextHolder.getContext().authentication.principal == "anonymousUser")
		return null
	(SecurityContextHolder.getContext().authentication.principal as UserPrincipal).dao as User
}.onFailure {
	"Failed to extract user from SecurityContextHolder: ${SecurityContextHolder.getContext().authentication.principal}".loge(javaClass, it)
}.getOrNull()

fun SecurityContext.assertUser(): User = user() ?: throw UnauthorizedResponse()

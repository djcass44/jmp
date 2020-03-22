package dev.castive.jmp.util

import dev.castive.jmp.component.SocketHandler
import dev.castive.jmp.data.FSA
import dev.castive.jmp.entity.Jump
import dev.castive.log2.logv
import dev.dcas.jmp.security.shim.entity.User
import dev.dcas.util.extend.json
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

/**
 * Check whether a Jump is visible to a specific user
 */
fun Jump.isVisibleTo(user: User?): Boolean {
	// public jumps can be seen by anyone
	// any user can see jumps in public groups
	if((ownerGroup == null && owner == null) || ownerGroup?.public == true)
		return true
	// we need a user to check personal/grouped jumps
	if(user == null)
		return false
	return owner?.id == user.id || ownerGroup?.containsUser(user) ?: false
}

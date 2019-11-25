package dev.castive.jmp.db.dao

import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.UUIDTable
import java.util.*

object Sessions: UUIDTable() {
	val requestToken = text("requestToken").default("")
    val refreshToken = text("refreshToken")
    val ssoToken = text("ssoToken").nullable()
    val createdAt = long("createdAt").default(System.currentTimeMillis())

    val user = reference("user", Users)
    val active = bool("active").default(false)
}

class Session(id: EntityID<UUID>): UUIDEntity(id) {
    companion object: UUIDEntityClass<Session>(Sessions)

	var requestToken by Sessions.requestToken
    var refreshToken by Sessions.refreshToken
    var ssoToken by Sessions.ssoToken
    var createdAt by Sessions.createdAt

    var user by User referencedOn Sessions.user
    var active by Sessions.active
}

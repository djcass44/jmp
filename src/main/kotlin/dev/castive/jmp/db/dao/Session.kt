package dev.castive.jmp.db.dao

import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.UUIDTable
import java.util.*

object Sessions: UUIDTable() {
    val refreshToken = text("refreshToken")
    val createdAt = long("createdAt").default(System.currentTimeMillis())

    val user = reference("user", Users)
}

class Session(id: EntityID<UUID>): UUIDEntity(id) {
    companion object: UUIDEntityClass<Session>(Sessions)

    var refreshToken by Sessions.refreshToken
    var createdAt by Sessions.createdAt

    var user by User referencedOn Sessions.user
}
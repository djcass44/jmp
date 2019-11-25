package dev.castive.jmp.auth

import dev.castive.javalin_auth.auth.RequestUserLocator
import dev.castive.javalin_auth.auth.Roles.BasicRoles
import dev.castive.jmp.api.Responses
import dev.castive.jmp.db.dao.User
import dev.castive.jmp.db.dao.Users
import dev.castive.jmp.db.repo.findFirstByEntity
import io.javalin.core.security.AccessManager
import io.javalin.core.security.Role
import io.javalin.http.Context
import io.javalin.http.Handler
import org.eclipse.jetty.http.HttpStatus
import org.jetbrains.exposed.sql.transactions.transaction

class AccessManager(config: ConfigBuilder.JMPConfiguration) : AccessManager {
	companion object {
		const val attributeUser = "USER"
	}

	private val requestUserLocator = RequestUserLocator(UserLocation(), config.crowd)

	private fun getUser(ctx: Context): User? {
		// check the other providers (e.g. jwt, oauth2)
		val entity = requestUserLocator.getUser(ctx)
		return entity?.let {
			Users.findFirstByEntity(it)
		}
	}

	override fun manage(handler: Handler, ctx: Context, permittedRoles: MutableSet<Role>) {
		// get the user determination
		val user = getUser(ctx)
		val userRole = if(user == null) BasicRoles.ANYONE else transaction {
			BasicRoles.valueOf(user.role.name)
		}
		if(permittedRoles.contains(userRole)) {
			// Store user information for later handlers
			ctx.attribute(attributeUser, user)
			handler.handle(ctx)
		}
		else
			ctx.status(HttpStatus.UNAUTHORIZED_401).result(Responses.AUTH_NONE)
	}
}

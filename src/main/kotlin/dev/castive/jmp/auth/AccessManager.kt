package dev.castive.jmp.auth

import dev.castive.jmp.api.Auth
import dev.castive.jmp.api.Responses
import io.javalin.core.security.AccessManager
import io.javalin.core.security.Role
import io.javalin.http.Context
import io.javalin.http.Handler
import org.eclipse.jetty.http.HttpStatus
import org.jetbrains.exposed.sql.transactions.transaction

class AccessManager: AccessManager {
	companion object {
		const val attributeUser = "USER"
	}
	override fun manage(handler: Handler, ctx: Context, permittedRoles: MutableSet<Role>) {
		val user = ClaimConverter.getUser(ctx)
		val userRole = if(user == null) Auth.BasicRoles.ANYONE else transaction {
			Auth.BasicRoles.valueOf(user.role.name)
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
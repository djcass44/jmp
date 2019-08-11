package dev.castive.jmp.auth

import dev.castive.javalin_auth.auth.Roles.BasicRoles
import dev.castive.jmp.api.Responses
import dev.castive.jmp.cache.BaseCacheLayer
import io.javalin.core.security.AccessManager
import io.javalin.core.security.Role
import io.javalin.http.Context
import io.javalin.http.Handler
import org.eclipse.jetty.http.HttpStatus
import org.jetbrains.exposed.sql.transactions.transaction

class AccessManager(cache: BaseCacheLayer): AccessManager {
	companion object {
		const val attributeUser = "USER"
	}
	private val userUtils = UserUtils(cache)

	override fun manage(handler: Handler, ctx: Context, permittedRoles: MutableSet<Role>) {
		val user = ClaimConverter.getUser(ctx, userUtils)
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
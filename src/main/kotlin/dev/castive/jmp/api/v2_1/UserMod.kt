package dev.castive.jmp.api.v2_1

import dev.castive.javalin_auth.auth.provider.InternalProvider
import dev.castive.jmp.Runner
import dev.castive.jmp.api.Auth
import dev.castive.jmp.api.Responses
import dev.castive.jmp.auth.AccessManager
import dev.castive.jmp.db.dao.User
import dev.castive.log2.Log
import io.javalin.http.ForbiddenResponse
import io.javalin.http.UnauthorizedResponse
import io.javalin.apibuilder.ApiBuilder.get
import io.javalin.apibuilder.ApiBuilder.patch
import io.javalin.apibuilder.EndpointGroup
import org.eclipse.jetty.http.HttpStatus
import org.jetbrains.exposed.sql.transactions.transaction

class UserMod(private val auth: Auth): EndpointGroup {
    data class PasswdPayload(val currentPassword: String, val newPassword: String)
    override fun addEndpoints() {
        get("${Runner.BASE}/v3/user/modpw", { ctx ->
            val user: User = ctx.attribute(AccessManager.attributeUser) ?: throw UnauthorizedResponse(Responses.AUTH_INVALID)
            val res = transaction {
                // TODO check if LDAP is r/w
                return@transaction user.from == InternalProvider.SOURCE_NAME
            }
            ctx.status(HttpStatus.OK_200).result(res.toString())
        }, Auth.defaultRoleAccess)
        patch("${Runner.BASE}/v3/user/modpw", { ctx ->
            val user: User = ctx.attribute(AccessManager.attributeUser) ?: throw UnauthorizedResponse(Responses.AUTH_INVALID)
            val password = ctx.bodyAsClass(PasswdPayload::class.java)
            val currentHash = auth.computeHash(password.currentPassword.toCharArray())
            val nextHash = auth.computeHash(password.newPassword.toCharArray())
            transaction {
                if(user.from != InternalProvider.SOURCE_NAME) throw ForbiddenResponse("You must change your password via your external provider.")
                if(user.hash != currentHash) throw ForbiddenResponse("Incorrect password")
                user.hash = nextHash
                Log.v(javaClass, "${user.username} has changed their password [from: ${user.from}]")
            }
            ctx.status(HttpStatus.NO_CONTENT_204).json(nextHash) // Is this okay?
        }, Auth.defaultRoleAccess)
    }
}
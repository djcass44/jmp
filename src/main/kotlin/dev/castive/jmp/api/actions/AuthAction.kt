package dev.castive.jmp.api.actions

import dev.castive.javalin_auth.auth.Providers
import dev.castive.javalin_auth.auth.data.model.atlassian_crowd.Factor
import dev.castive.javalin_auth.auth.data.model.atlassian_crowd.ValidateRequest
import dev.castive.jmp.db.dao.Session
import dev.castive.jmp.db.dao.Sessions
import dev.castive.jmp.db.repo.findFirstBySsoToken
import dev.castive.log2.Log
import io.javalin.http.Context

object AuthAction {
	/**
	 * Check if the primary provider thinks that the token is valid
	 * Currently only support Atlassian Crowd
	 */
	fun isValidToken(token: String, ctx: Context): String {
		val valid = Providers.primaryProvider?.validate(token, ValidateRequest(arrayOf(Factor("remote_address", ctx.ip())))) ?: ""
		Log.d(javaClass, "Validation request returned: '$valid'")
		return valid
	}

	/**
	 * Check if a user HAD a specific token at any point
	 */
	fun userHadToken(token: String?): Session? {
		if(token == null) {
			Log.v(javaClass, "::userHadToken: provided null token")
			return null
		}
		return Sessions.findFirstBySsoToken(token)
	}
}
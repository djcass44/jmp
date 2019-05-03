/*
 *    Copyright 2019 Django Cass
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package dev.castive.jmp.api.v2_1

import dev.castive.javalin_auth.auth.connect.LDAPConfig
import dev.castive.jmp.Runner
import dev.castive.jmp.api.Auth
import dev.castive.jmp.util.checks.DatabaseCheck
import dev.castive.jmp.util.checks.LDAPCheck
import io.javalin.apibuilder.ApiBuilder.get
import io.javalin.apibuilder.EndpointGroup
import org.eclipse.jetty.http.HttpStatus

class Health(private val config: LDAPConfig): EndpointGroup {
    override fun addEndpoints() {
        get("${Runner.BASE}/v2_1/health", { ctx ->
            ctx.status(HttpStatus.OK_200).json(runChecks())
        }, Auth.openAccessRole)
    }
    private fun runChecks(): HealthPayload {
        val dbCheck = DatabaseCheck().runCheck()
        val ldapCheck = if(config.enabled) LDAPCheck(config).runCheck() else null
        val code = if(!dbCheck || ldapCheck == false) HttpStatus.INTERNAL_SERVER_ERROR_500 else HttpStatus.OK_200

        return HealthPayload(code, "OK", dbCheck, ldapCheck)
    }
}
data class HealthPayload(val code: Int = HttpStatus.OK_200, val http: String = "OK", val database: Boolean, val ldap: Boolean?)
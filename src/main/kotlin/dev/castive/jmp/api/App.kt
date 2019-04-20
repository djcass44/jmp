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

package dev.castive.jmp.api

import dev.castive.javalin_auth.actions.UserAction
import dev.castive.javalin_auth.auth.JWT
import dev.castive.javalin_auth.auth.Providers
import dev.castive.javalin_auth.auth.TokenProvider
import dev.castive.jmp.Arguments
import dev.castive.jmp.Version
import dev.castive.jmp.api.v1.Jump
import dev.castive.jmp.api.v2.*
import dev.castive.jmp.api.v2.Similar
import dev.castive.jmp.api.v2.User
import dev.castive.jmp.api.v2_1.*
import dev.castive.jmp.api.v2_1.Group
import dev.castive.jmp.audit.Logger
import dev.castive.jmp.auth.ClaimConverter
import dev.castive.jmp.auth.LDAPConfigBuilder
import dev.castive.jmp.auth.UserValidator
import dev.castive.jmp.auth.UserVerification
import dev.castive.jmp.db.ConfigStore
import dev.castive.jmp.db.Init
import dev.castive.jmp.db.dao.*
import dev.castive.log2.Log
import io.javalin.Javalin
import org.eclipse.jetty.http.HttpStatus
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.concurrent.TimeUnit

class App(val port: Int = 7000) {
    fun start(store: ConfigStore, arguments: Arguments, logger: Logger) {
        transaction {
            SchemaUtils.create(Jumps, Users, Roles, Groups, GroupUsers, Aliases) // Ensure that the tables are created
            Log.i(javaClass, "Running automated database upgrade (if required)")
            SchemaUtils.createMissingTablesAndColumns(Jumps, Users, Roles, Groups, GroupUsers, Aliases, Sessions)
            Init() // Ensure that the default admin/roles is created
        }
        val auth = Auth()
        val builder = LDAPConfigBuilder(store)
        Providers(builder.core, builder.extra).init(UserVerification(auth)) // Setup user authentication
        Providers.validator = UserValidator(auth, builder.extra)
        UserAction.verification = Providers.verification
        Javalin.create().apply {
            disableStartupBanner()
            port(port)
            if(arguments.enableCors) enableCorsForAllOrigins()
            enableCaseSensitiveUrls()
            accessManager { handler, ctx, permittedRoles ->
                val jwt = JWT.get().map(ctx)
                val user = if(TokenProvider.get().mayBeToken(jwt)) ClaimConverter.getUser(TokenProvider.get().verify(jwt!!, Providers.verification)) else null
                val userRole = if(user == null) Auth.BasicRoles.USER else transaction {
                    Auth.BasicRoles.valueOf(user.role.name)
                }
                if(permittedRoles.contains(userRole))
                    handler.handle(ctx)
                else
                    ctx.status(HttpStatus.UNAUTHORIZED_401).result("Unauthorised")
            }
            requestLogger { ctx, timeMs ->
                logger.add("${System.currentTimeMillis()} - ${ctx.method()} ${ctx.path()} took $timeMs ms")
            }
            routes {
                val ws = WebSocket()
                ws.addEndpoints()
                // General
                Info().addEndpoints()
                Props(builder).addEndpoints()

                // Jumping
                Jump(store, ws).addEndpoints()
                Similar().addEndpoints()

                // Users
                User(auth, ws, builder.extra).addEndpoints()

                // Group
                Group(ws).addEndpoints()
                GroupMod().addEndpoints()

                // Authentication
                Oauth(auth).addEndpoints()
                Verify(auth).addEndpoints()

                // Health
                Health().addEndpoints()
            }
            start()
        }
        println("       _ __  __ _____  \n" +
                "      | |  \\/  |  __ \\ \n" +
                "      | | \\  / | |__) |\n" +
                "  _   | | |\\/| |  ___/ \n" +
                " | |__| | |  | | |     \n" +
                "  \\____/|_|  |_|_|     \n" +
                "                       \n" +
                "JMP v${Version.getVersion()} is ready.")
    }
}
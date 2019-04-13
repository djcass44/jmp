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

import dev.castive.log2.Log
import dev.castive.jmp.Arguments
import dev.castive.jmp.api.v1.Jump
import dev.castive.jmp.api.v2.*
import dev.castive.jmp.api.v2.Similar
import dev.castive.jmp.api.v2.User
import dev.castive.jmp.api.v2_1.*
import dev.castive.jmp.api.v2_1.Group
import dev.castive.jmp.audit.Logger
import dev.castive.jmp.auth.JWT
import dev.castive.jmp.auth.Providers
import dev.castive.jmp.auth.TokenProvider
import dev.castive.jmp.db.ConfigStore
import dev.castive.jmp.db.Init
import dev.castive.jmp.db.dao.*
import io.javalin.Javalin
import org.eclipse.jetty.http.HttpStatus
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

class App {
    fun start(store: ConfigStore, arguments: Arguments, logger: Logger) {
        transaction {
            SchemaUtils.create(Jumps, Users, Roles, Groups, GroupUsers) // Ensure that the tables are created
            Log.i(javaClass, "Running automated database upgrade (if required)")
            SchemaUtils.createMissingTablesAndColumns(Jumps, Users, Roles, Groups, GroupUsers)
            Init() // Ensure that the default admin/roles is created
        }
        val auth = Auth()
        val providers = Providers(store, auth) // Setup user authentication
        Javalin.create().apply {
            disableStartupBanner()
            port(7000)
            if(arguments.enableCors) enableCorsForAllOrigins()
            enableCaseSensitiveUrls()
            accessManager { handler, ctx, permittedRoles ->
                val jwt = JWT.map(ctx)
                val user = if(TokenProvider.getInstance().mayBeToken(jwt)) TokenProvider.getInstance().verify(jwt!!) else null
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
                Props(providers).addEndpoints()

                // Jumping
                Jump(store, ws).addEndpoints()
                Similar().addEndpoints()

                // Users
                User(auth, providers, ws).addEndpoints()

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
                "JMP v${dev.castive.jmp.Version.getVersion()} is ready.")
    }
}
package com.django.jmp.api

import com.django.jmp.api.v1.Jump
import com.django.jmp.api.v2.Similar
import com.django.jmp.api.v2.User
import com.django.jmp.api.v2.Verify
import com.django.jmp.audit.Logger
import com.django.jmp.db.*
import com.django.log2.logging.Log
import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.get
import io.javalin.security.SecurityUtil.roles
import org.eclipse.jetty.http.HttpStatus
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import java.sql.Connection
import java.util.*


object Runner

fun main(args: Array<String>) {
    Log.v(Runner::class.java, Arrays.toString(args))
    val enableCors = args.contains("--enable-cors")
    if(enableCors) Log.w(Runner::class.java, "WARNING: CORS access is enable for ALL origins. DO NOT allow this in production: WARNING")
    val configLocation = if(args.size >= 2 && args[0] == "using") {
        args[1]
    }
    else
        "env"
    Log.i(Runner::class.java, "Using database path: $configLocation")
    val store = if(configLocation.isNotBlank() && configLocation != "env")
        Config().load(configLocation)
    else
        Config().loadEnv()
    Log.v(Runner::class.java, "Database config: [${store.url}, ${store.driver}]")
    Database.connect(store.url, store.driver)
    TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE // Fix required for SQLite

    transaction {
        addLogger(StdOutSqlLogger)
        SchemaUtils.create(Jumps, Users, Roles) // Ensure that the 'Jumps' table is created
        Init() // Ensure that the default admin is created
    }
    val auth = Auth()
    val logger = Logger(store.logRequestDir)
    val app = Javalin.create().apply {
        port(7000)
        enableStaticFiles("/public")
        if(enableCors) enableCorsForAllOrigins()
        enableCaseSensitiveUrls()
        accessManager { handler, ctx, permittedRoles ->
            val user = ctx.header(Auth.headerUser)
            val token = ctx.header(Auth.headerToken)
            val tokenUUID = if (token != null && token.isNotBlank() && token != "null") UUID.fromString(token) else null
            val userRole = if(tokenUUID != null && user != null) {
                auth.getUserRole(user, tokenUUID)
            }
            else auth.getUserRole(user)
            if(permittedRoles.contains(userRole))
                handler.handle(ctx)
            else
                ctx.status(HttpStatus.UNAUTHORIZED_401).result("Unauthorised")
        }
        requestLogger { ctx, timeMs ->
            logger.add("${ctx.method()} ${ctx.path()} took $timeMs ms")
        }
    }.start()
    app.routes {
        // Version/info
        get("/v2/info", { ctx ->
            ctx.status(HttpStatus.OK_200).result("v2.0")
        }, roles(Auth.BasicRoles.USER, Auth.BasicRoles.ADMIN))
        Jump(auth).addEndpoints()
        Similar(auth).addEndpoints()
        User(auth).addEndpoints()
        Verify(auth).addEndpoints()
    }
}
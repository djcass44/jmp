package com.django.jmp.api

import com.django.jmp.api.v1.Jump
import com.django.jmp.api.v2.*
import com.django.jmp.api.v2.Similar
import com.django.jmp.api.v2.User
import com.django.jmp.api.v2_1.Group
import com.django.jmp.audit.Logger
import com.django.jmp.auth.JWTContextMapper
import com.django.jmp.auth.TokenProvider
import com.django.jmp.db.Config
import com.django.jmp.db.ConfigStore
import com.django.jmp.db.Init
import com.django.jmp.db.dao.*
import com.django.log2.logging.Log
import io.javalin.Javalin
import org.eclipse.jetty.http.HttpStatus
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import java.sql.Connection
import java.util.*


object Runner {
    const val BASE = "/api"
    var START_TIME = 0L
    lateinit var store: ConfigStore
}

fun main(args: Array<String>) {
    Runner.START_TIME = System.currentTimeMillis()
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
    Runner.store = store
    Log.v(Runner::class.java, "Database config: [${store.url}, ${store.driver}]")
    Log.v(Runner::class.java, "Application config: [${store.BASE_URL}, ${store.logRequestDir}]")
    Database.connect(store.url, store.driver)
    TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE // Fix required for SQLite/Oracle DB

    transaction {
        addLogger(StdOutSqlLogger)
        SchemaUtils.create(Jumps, Users, Roles, Groups, GroupUsers) // Ensure that the tables are created
        Init() // Ensure that the default admin/roles is created
    }
    val auth = Auth()
    val logger = Logger(store.logRequestDir)
    val app = Javalin.create().apply {
        port(7000)
        if(enableCors) enableCorsForAllOrigins()
        enableCaseSensitiveUrls()
        accessManager { handler, ctx, permittedRoles ->
            val jwt = ctx.use(JWTContextMapper::class.java).tokenAuthCredentials(ctx)
            val user = if(jwt != null && jwt.isNotBlank() && jwt != "null") TokenProvider.getInstance().verify(jwt) else null
            val userRole = if(user == null) Auth.BasicRoles.USER else transaction {
                Auth.BasicRoles.valueOf(user.role.name)
            }
            if(permittedRoles.contains(userRole))
                handler.handle(ctx)
            else
                ctx.status(HttpStatus.UNAUTHORIZED_401).result("Unauthorised")
        }
        requestLogger { ctx, timeMs ->
            logger.add("${ctx.method()} ${ctx.path()} took $timeMs ms")
        }
    }.start()
    app.before {ctx ->
        ctx.register(JWTContextMapper::class.java, JWTContextMapper())
    }
    app.routes {
        Info().addEndpoints()
        Jump(auth, store).addEndpoints()
        Similar().addEndpoints()
        User(auth).addEndpoints()
        Group().addEndpoints()
        Oauth(auth).addEndpoints()
        Verify(auth).addEndpoints()
    }
}
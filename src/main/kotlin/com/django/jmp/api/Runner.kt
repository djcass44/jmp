package com.django.jmp.api

import com.django.jmp.Arguments
import com.django.jmp.Version
import com.django.jmp.api.v1.Jump
import com.django.jmp.api.v2.*
import com.django.jmp.api.v2.Similar
import com.django.jmp.api.v2.User
import com.django.jmp.api.v2_1.Group
import com.django.jmp.api.v2_1.GroupMod
import com.django.jmp.audit.Logger
import com.django.jmp.auth.JWTContextMapper
import com.django.jmp.auth.TokenProvider
import com.django.jmp.db.Config
import com.django.jmp.db.ConfigStore
import com.django.jmp.db.Init
import com.django.jmp.db.dao.*
import com.django.jmp.db.source.BasicAuthSource
import com.django.jmp.db.source.NoAuthSource
import com.django.jmp.except.InvalidSecurityConfigurationException
import com.django.log2.logging.Log
import io.javalin.Javalin
import org.eclipse.jetty.http.HttpStatus
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
    lateinit var args: Array<String>
}

fun main(args: Array<String>) {
    Runner.args = args
    Runner.START_TIME = System.currentTimeMillis()
    Log.v(Runner::class.java, Arrays.toString(args))
    val arguments = Arguments(args)
    if(arguments.enableCors) Log.w(Runner::class.java, "WARNING: CORS access is enable for ALL origins. DO NOT allow this in production: WARNING")
    Log.setPriorityLevel(arguments.debugLevel)
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
    // Do not allow CORS on an https url
    if(store.BASE_URL.startsWith("https") && arguments.enableCors) throw InvalidSecurityConfigurationException()
    val source = when {
        !store.tableUser.isNullOrBlank() && !store.tablePassword.isNullOrBlank() -> BasicAuthSource()
        else -> NoAuthSource()
    }
    source.connect(store)
    TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE // Fix required for SQLite/Oracle DB

    transaction {
        addLogger(StdOutSqlLogger)
        SchemaUtils.create(Jumps, Users, Roles, Groups, GroupUsers) // Ensure that the tables are created
        Log.i(javaClass, "Running automated database upgrade (if required)")
        SchemaUtils.createMissingTablesAndColumns(Jumps, Users, Roles, Groups, GroupUsers)
        Init() // Ensure that the default admin/roles is created
    }
    val auth = Auth()
    val logger = Logger(store.logRequestDir)
    Javalin.create().apply {
        disableStartupBanner()
        port(7000)
        if(arguments.enableCors) enableCorsForAllOrigins()
        enableCaseSensitiveUrls()
        accessManager { handler, ctx, permittedRoles ->
            val jwt = ctx.use(JWTContextMapper::class.java).tokenAuthCredentials(ctx)
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
        before { ctx ->
            ctx.register(JWTContextMapper::class.java, JWTContextMapper())
        }
        routes {
            // General
            Info().addEndpoints()

            // Jumping
            Jump(auth, store).addEndpoints()
            Similar().addEndpoints()

            // Users
            User(auth).addEndpoints()

            // Group
            Group().addEndpoints()
            GroupMod().addEndpoints()

            // Authentication
            Oauth(auth).addEndpoints()
            Verify(auth).addEndpoints()
        }
    }.start()
    println("       _ __  __ _____  \n" +
            "      | |  \\/  |  __ \\ \n" +
            "      | | \\  / | |__) |\n" +
            "  _   | | |\\/| |  ___/ \n" +
            " | |__| | |  | | |     \n" +
            "  \\____/|_|  |_|_|     \n" +
            "                       \n" +
            "JMP v${Version.getVersion()} is ready.")
}
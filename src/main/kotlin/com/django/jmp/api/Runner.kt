package com.django.jmp.api

import com.django.jmp.api.v1.Jump
import com.django.jmp.api.v2.Similar
import com.django.jmp.api.v2.User
import com.django.jmp.api.v2.Verify
import com.django.jmp.db.Config
import com.django.jmp.db.Init
import com.django.jmp.db.Jumps
import com.django.jmp.db.Users
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
    val configLocation = if(args.size == 2 && args[0] == "using") {
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
        SchemaUtils.create(Jumps, Users) // Ensure that the 'Jumps' table is created
        Init(store.super_name, store.super_key.toCharArray()) // Ensure that the default admin is created
    }
    val auth = Auth()
    val app = Javalin.create().apply {
        port(7000)
        enableStaticFiles("/public")
        enableCaseSensitiveUrls()
        accessManager { handler, ctx, permittedRoles ->
            val userRole = auth.getUserRole(ctx.header(Auth.headerUser))
            if(permittedRoles.contains(userRole))
                handler.handle(ctx)
            else
                ctx.status(HttpStatus.UNAUTHORIZED_401).result("Unauthorised")
        }
    }.start()
    app.routes {
        // Version/info
        get("/v2/info", { ctx ->
            ctx.result("v2.0")
            ctx.status(HttpStatus.OK_200)
        }, roles(Auth.BasicRoles.USER, Auth.BasicRoles.ADMIN))
        Jump(auth).addEndpoints()
        Similar().addEndpoints()
        User(auth).addEndpoints()
        Verify(auth).addEndpoints()
    }
}
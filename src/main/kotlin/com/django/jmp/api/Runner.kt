package com.django.jmp.api

import com.django.jmp.db.*
import com.django.jmp.except.EmptyPathException
import com.django.log2.logging.Log
import io.javalin.*
import io.javalin.apibuilder.ApiBuilder.*
import io.javalin.security.SecurityUtil.roles
import org.eclipse.jetty.http.HttpStatus
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import java.sql.Connection
import java.util.*


object Runner {
    fun jumpExists(name: String): Boolean {
        return transaction {
            val existing = Jump.find {
                Jumps.name.lowerCase() eq name.toLowerCase() and Jumps.token.isNull()
            }
            return@transaction !existing.empty()
        }
    }
    fun jumpExists(name: String, token: UUID?): Boolean {
        if(token == null)
            return jumpExists(name) // Fall back to v1 without a token
        return transaction {
            val existing = Jump.find {
                Jumps.name.lowerCase() eq name.toLowerCase() and Jumps.token.eq(token)
            }
            return@transaction if(existing.empty()) jumpExists(name) else !existing.empty() // Fall back to v1 if nothing found in v2
        }
    }
}

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
        // List all items in Json format
        get("/v1/jumps", { ctx ->
            val items = arrayListOf<JumpJson>()
            Log.i(Runner::class.java, "API:GET -> ${ctx.path()}")
            val token: String? = ctx.header(Auth.headerToken)
            Log.d(Runner::class.java, "Token: $token")
            val tokenUUID = if(token != null && token.isNotBlank() && token != "null") UUID.fromString(token) else null
            transaction {
                Log.d(javaClass, Jump.all().count().toString())
                Jump.all().forEach {
                    if(it.token == null || it.token!! == tokenUUID)
                        items.add(JumpJson(it))
                }
            }
            ctx.json(items)
        }, roles(Auth.BasicRoles.USER, Auth.BasicRoles.ADMIN))
        // Redirect to $location (if it exists)
        get("/v1/jump/:target", { ctx ->
            try {
                val target = ctx.pathParam("target")
                if(target.isBlank())
                    throw EmptyPathException()
                Log.d(Runner::class.java, "Target: $target")
                val token: String? = ctx.header(Auth.headerToken)
                if(token == null || token.isBlank()) {
                    ctx.redirect("/tokcheck.html?query=$target")
                    ctx.status(HttpStatus.FOUND_302)
                    return@get
                }
                var foundV2 = false
                if(token.isNotBlank() && token != "global" && token != "null") { // Request has a token, search user-jumps first
                    val tokenUUID = UUID.fromString(token)
                    transaction {
                        val dbtarget = Jump.find {
                            Jumps.name.lowerCase() eq target.toLowerCase() and Jumps.token.isNotNull() and Jumps.token.eq(tokenUUID)
                        }
                        if(!dbtarget.empty()) {
                            val location = dbtarget.elementAt(0).location
                            Log.v(javaClass, "v2: moving to user point: $location")
                            foundV2 = true
                            ctx.redirect(location, HttpStatus.FOUND_302)
                        }
                    }
                }
                if(foundV2) // Jump was found in personal collection, don't need to check global
                    return@get
                transaction {
                    val dbtarget = Jump.find {
                        Jumps.name.lowerCase() eq target.toLowerCase() and Jumps.token.isNull()
                    }
                    if(!dbtarget.empty()) {
                        val location = dbtarget.elementAt(0).location
                        Log.v(javaClass, "Redirecting to $location")
                        ctx.redirect(location, HttpStatus.FOUND_302)
                    }
                    else
                        ctx.redirect("/similar.html?query=$target")
                }
            }
            catch (e: IndexOutOfBoundsException) {
                Log.e(Runner::class.java, "Invalid target: ${ctx.path()}")
                throw BadRequestResponse()
            }
            catch (e: EmptyPathException) {
                Log.e(Runner::class.java, "Empty target")
                throw NotFoundResponse()
            }
        }, roles(Auth.BasicRoles.USER, Auth.BasicRoles.ADMIN))
        // Add a jump point
        put("/v1/jumps/add", { ctx ->
            val add = ctx.bodyAsClass(JumpJson::class.java)
            val token: String? = ctx.header(Auth.headerToken)
            val tokenUUID = if(token != null && token.isNotBlank() && token != "null") UUID.fromString(token) else null
            // Block valid users with invalid tokens
            if(tokenUUID != null && !auth.validateUserToken(tokenUUID)) throw UnauthorizedResponse()
            if(!Runner.jumpExists(add.name, tokenUUID)) {
                transaction {
                    Jump.new {
                        name = add.name
                        location = add.location
                        if(tokenUUID != null && add.personal)
                            this.token = tokenUUID
                    }
                }
                ctx.status(HttpStatus.CREATED_201)
            }
            else
                throw ConflictResponse()
        }, roles(Auth.BasicRoles.USER, Auth.BasicRoles.ADMIN))
        // Edit a jump point
        patch("/v1/jumps/edit", { ctx ->
            val update = ctx.bodyAsClass(EditJumpJson::class.java)
            transaction {
                if(update.lastName != update.name && Runner.jumpExists(update.name)) {
                    throw ConflictResponse()
                }
                else {
                    val existing = Jump.find {
                        Jumps.name eq update.lastName
                    }
                    if(!existing.empty()) {
                        val item = existing.elementAt(0)
                        item.name = update.name
                        item.location = update.location
                        ctx.status(HttpStatus.NO_CONTENT_204)
                    }
                    else
                        throw NotFoundResponse()
                }
            }
        }, roles(Auth.BasicRoles.USER, Auth.BasicRoles.ADMIN))
        // Delete a jump point
        delete("/v1/jumps/rm/:name", { ctx ->
            val name = ctx.pathParam("name")
            transaction {
                Jumps.deleteWhere { Jumps.name eq name }
            }
            ctx.status(HttpStatus.NO_CONTENT_204)
        }, roles(Auth.BasicRoles.USER, Auth.BasicRoles.ADMIN))
        // Find similar jumps
        get("/v2/similar/:query", { ctx ->
            try {
                val token: String? = ctx.header(Auth.headerToken)
                val tokenUUID = if(token != null && token.isNotBlank() && token != "null") UUID.fromString(token) else null
                val query = ctx.pathParam("query")
                if (query.isBlank())
                    throw EmptyPathException()
                val names = arrayListOf<String>()
                transaction {
                    Jump.all().forEach {
                        if(it.token == null || it.token == tokenUUID)
                            names.add(it.name)
                    }
                }
                val similar = Similar(query, names)
                ctx.json(similar.compute())
            }
            catch (e: EmptyPathException) {
                Log.e(Runner::class.java, "Empty target")
                throw NotFoundResponse()
            }
        }, roles(Auth.BasicRoles.USER, Auth.BasicRoles.ADMIN))
        // Add a user
        put("/v2/user/add", { ctx ->
            val credentials = Auth.BasicAuth(ctx.bodyAsClass(Auth.BasicAuth.Insecure::class.java))
            auth.createUser(credentials.username, credentials.password)
        }, roles(Auth.BasicRoles.ADMIN))
        // Get a users token
        post("/v2/user/auth", { ctx ->
            val credentials = Auth.BasicAuth(ctx.bodyAsClass(Auth.BasicAuth.Insecure::class.java))
            val token = auth.getUserToken(credentials.username, credentials.password)
            if(token != null)
                ctx.json(token)
            else
                throw NotFoundResponse()
        }, roles(Auth.BasicRoles.USER, Auth.BasicRoles.ADMIN))
        // Verify a users token is still valid
        get("/v2/verify/token", { ctx ->
            val token: String? = ctx.header(Auth.headerToken)
            val tokenUUID = if(token != null && token.isNotBlank() && token != "null") UUID.fromString(token) else null
            if(tokenUUID == null)
                throw BadRequestResponse()
            else {
                ctx.result(auth.validateUserToken(tokenUUID).toString())
                ctx.status(HttpStatus.OK_200)
            }
        }, roles(Auth.BasicRoles.USER, Auth.BasicRoles.ADMIN))
        // Verify a user still exists
        get("/v2/verify/user/:name", { ctx ->
            val name = ctx.pathParam("name")
            if(name.isBlank()) { // TODO never send 'null'
                Log.v(Runner::class.java, "User made null/empty request")
                throw BadRequestResponse()
            }
            transaction {
                val result = User.find {
                    Users.username eq name
                }.elementAtOrNull(0)
                if(result == null) {
                    Log.w(javaClass, "User: $name failed verification [IP: ${ctx.ip()}, UA: ${ctx.userAgent()}]")
                    throw BadRequestResponse()
                }
                else {
                    if(auth.validateUserToken(result.token)) {
                        ctx.status(HttpStatus.OK_200)
                        ctx.result(name)
                    }
                    else {
                        Log.w(javaClass, "User: $name exists, however their token is invalid [IP: ${ctx.ip()}, UA: ${ctx.userAgent()}")
                        throw BadRequestResponse()
                    }
                }
            }
        }, roles(Auth.BasicRoles.USER, Auth.BasicRoles.ADMIN))
    }
}
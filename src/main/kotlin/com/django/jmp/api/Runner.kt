package com.django.jmp.api

import com.django.jmp.db.*
import com.django.jmp.except.EmptyPathException
import com.django.log2.logging.Log
import io.javalin.BadRequestResponse
import io.javalin.ConflictResponse
import io.javalin.Javalin
import io.javalin.NotFoundResponse
import io.javalin.apibuilder.ApiBuilder.*
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
                Jumps.name.lowerCase() eq name.toLowerCase()
            }
            return@transaction !existing.empty()
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
    }
    val app = Javalin.create().apply {
        port(7000)
        enableStaticFiles("/public")
        enableCaseSensitiveUrls()
    }.start()
    app.routes {
        // List all items in Json format
        get("/v1/jumps") { ctx ->
            val items = arrayListOf<JumpJson>()
            Log.i(Runner::class.java, "API:GET -> ${ctx.path()}")
            val token = ctx.queryParam("token", "")
            val tokenUUID = if(token != null && token.isNotBlank()) UUID.fromString(token) else null
            transaction {
                Log.d(javaClass, Jump.all().count().toString())
                Jump.all().forEach {
                    if(it.token == null || it.token!! == tokenUUID)
                        items.add(JumpJson(it))
                }
            }
            ctx.json(items)
        }
        // Redirect to $location (if it exists)
        get("/v1/jump/:target") { ctx ->
            try {
                val target = ctx.pathParam("target")
                if(target.isBlank())
                    throw EmptyPathException()
                Log.d(Runner::class.java, "Target: $target")
                val token = ctx.queryParam("token", "")
                if(token == null || token.isBlank()) {
                    ctx.redirect("/tokcheck.html?query=$target")
                    ctx.status(HttpStatus.FOUND_302)
                    return@get
                }
                var foundV2 = false
                if(token.isNotBlank() && token != "global") { // Request has a token, search user-jumps first
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
        }
        // Add a jump point
        put("/v1/jumps/add") { ctx ->
            val add = ctx.bodyAsClass(JumpJson::class.java)
            val token = ctx.queryParam("token", "")
            val tokenUUID = if(token != null && token.isNotBlank()) UUID.fromString(token) else null
            if(!Runner.jumpExists(add.name)) {
                transaction {
                    Jump.new {
                        name = add.name
                        location = add.location
                        if(tokenUUID != null)
                            this.token = tokenUUID
                    }
                }
                ctx.status(HttpStatus.CREATED_201)
            }
            else
                throw ConflictResponse()
        }
        // Edit a jump point
        patch("/v1/jumps/edit") { ctx ->
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
        }
        // Delete a jump point
        delete("/v1/jumps/rm/:name") { ctx ->
            val name = ctx.pathParam("name")
            transaction {
                Jumps.deleteWhere { Jumps.name eq name }
            }
            ctx.status(HttpStatus.NO_CONTENT_204)
        }
        // Find similar jumps
        get("/v2/similar/:query") { ctx ->
            try {
                val token = ctx.queryParam("token", "")
                val tokenUUID = if(token != null && token.isNotBlank()) UUID.fromString(token) else null
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
        }
        // Add a user
        put("/v2/user/add") { ctx ->
            val credentials = Auth.BasicAuth(ctx.bodyAsClass(Auth.BasicAuth.Insecure::class.java))
            val auth = Auth()
            auth.createUser(credentials.username, credentials.password)
        }
        // Get a users token
        post("/v2/user/auth") { ctx ->
            val credentials = Auth.BasicAuth(ctx.bodyAsClass(Auth.BasicAuth.Insecure::class.java))
            val auth = Auth()
            val token = auth.getUserToken(credentials.username, credentials.password)
            if(token != null)
                ctx.json(token)
            else
                throw NotFoundResponse()
        }
    }
}
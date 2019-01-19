package com.django.jmp.api

import com.django.jmp.db.Jump
import com.django.jmp.db.JumpJson
import com.django.jmp.db.Jumps
import com.django.jmp.except.EmptyPathException
import com.django.log2.logging.Log
import io.javalin.BadRequestResponse
import io.javalin.ConflictResponse
import io.javalin.Javalin
import io.javalin.NotFoundResponse
import io.javalin.apibuilder.ApiBuilder.*
import org.eclipse.jetty.http.HttpStatus
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import java.sql.Connection
import java.util.*

// TODO get by config.yml or environment variables
const val version = "v1"

const val dbClass = "jdbc:sqlite:"
const val dbDriver = "org.sqlite.JDBC"

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
    val dbLocation = if(args.size == 2 && args[0] == "using") {
        args[1]
    }
    else
        "/tmp/jmp.db"
    Log.i(Runner::class.java, "Using database path: $dbLocation")
    Database.connect(dbClass + dbLocation, dbDriver)
    TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE // Fix required for SQLite
    
    val app = Javalin.create().apply {
        port(7000)
        enableStaticFiles("/public")
        enableCaseSensitiveUrls()
    }.start()
    app.routes {
        // List all items in Json format
        get("/$version/jumps") { ctx ->
            val items = arrayListOf<JumpJson>()
            Log.i(Runner::class.java, "API:GET -> ${ctx.path()}")
            transaction {
                Log.d(javaClass, Jump.all().count().toString())
                Jump.all().forEach {
                    items.add(JumpJson(it))
                }
            }
            ctx.json(items)
        }
        // Redirect to $location (if it exists)
        get("/$version/jump/:target") { ctx ->
            try {
                val target = ctx.pathParam("target")
                if(target.isBlank())
                    throw EmptyPathException()
                Log.d(Runner::class.java, "Target: $target")
                transaction {
                    val dbtarget = Jump.find {
                        Jumps.name.lowerCase() eq target.toLowerCase()
                    }
                    if(!dbtarget.empty()) {
                        val location = dbtarget.elementAt(0).location
                        Log.v(javaClass::class.java, "Redirecting to $location")
                        ctx.redirect(location, HttpStatus.FOUND_302)
                    }
                    else
                        throw NotFoundResponse()
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
        put("/$version/jumps/add") { ctx ->
            val add = ctx.bodyAsClass(JumpJson::class.java)
            if(!Runner.jumpExists(add.name)) {
                transaction {
                    Jump.new {
                        name = add.name
                        location = add.location
                    }
                }
                ctx.status(HttpStatus.CREATED_201)
            }
            else
                throw ConflictResponse()
        }
        // Edit a jump point
        patch("/$version/jumps/edit") { ctx ->
            val update = ctx.bodyAsClass(Jump::class.java)
            transaction {
                val existing = Jump.findById(update.id)
                if(existing != null) {
                    existing.name = update.name
                    existing.location = update.location
                    ctx.status(HttpStatus.NO_CONTENT_204)
                }
                else
                    throw NotFoundResponse()
            }
        }
        // Delete a jump point
        delete("/$version/jumps/rm/:name") { ctx ->
            val name = ctx.pathParam("name")
            transaction {
                Jumps.deleteWhere { Jumps.name eq name }
            }
            ctx.status(HttpStatus.NO_CONTENT_204)
        }
    }
}
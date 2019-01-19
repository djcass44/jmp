package com.django.jmp.api

import com.django.jmp.db.Jump
import com.django.jmp.db.JumpJson
import com.django.jmp.db.Jumps
import com.django.jmp.except.EmptyPathException
import com.django.log2.logging.Log
import io.javalin.BadRequestResponse
import io.javalin.Javalin
import io.javalin.NotFoundResponse
import io.javalin.apibuilder.ApiBuilder.*
import org.eclipse.jetty.http.HttpStatus
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import java.sql.Connection

// TODO get by config.yml or environment variables
const val version = "v1"

const val dbClass = "jdbc:sqlite:/tmp/jmp.db"
const val dbDriver = "org.sqlite.JDBC"

fun main() {
    Database.connect(dbClass, dbDriver)
    TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE // Fix required for SQLite

    transaction {
        addLogger(StdOutSqlLogger)
        SchemaUtils.create(Jumps)
        val def = Jump.find {
            Jumps.name eq "Google"
        }
        if(def.empty()) {
            Jump.new {
                name = "Google"
                location = "https://google.com"
            }
        }
    }
    val app = Javalin.create().apply {
        port(7000)
//        enableStaticFiles("/public")
        enableCaseSensitiveUrls()
    }.start()
    app.routes {
        // List all items in Json format
        get("/$version/jumps") { ctx ->
            val items = arrayListOf<JumpJson>()
            Log.i(Javalin::class.java, "API:GET -> ${ctx.path()}")
            transaction {
                Log.d(javaClass, Jump.all().count().toString())
                Jump.all().forEach {
                    items.add(JumpJson(it))
                }
            }
            ctx.json(items)
        }
        // Redirect to $location (if it exists)
        get("/$version/jump/*") { ctx ->
            try {
                val target = ctx.path().split("/$version/jump/")[1]
                if(target.isBlank())
                    throw EmptyPathException()
                Log.d(Javalin::class.java, "Target: $target")
                transaction {
                    val dbtarget = Jump.find {
                        Jumps.name.lowerCase() eq target.toLowerCase()
                    }
                    if(!dbtarget.empty()) {
                        val location = dbtarget.elementAt(0).location
                        Log.v(Javalin::class.java, "Redirecting to $location")
                        ctx.redirect(location, HttpStatus.FOUND_302)
                    }
                    else
                        throw NotFoundResponse()
                }
            }
            catch (e: IndexOutOfBoundsException) {
                Log.e(Javalin::class.java, "Invalid target: ${ctx.path()}")
                throw BadRequestResponse()
            }
            catch (e: EmptyPathException) {
                Log.e(Javalin::class.java, "Empty target")
                throw NotFoundResponse()
            }
        }
        put("/$version/jumpAdd") { ctx ->
            val add = ctx.bodyAsClass(Jump::class.java)
            transaction {
                Jump.new {
                    name = add.name
                    location = add.location
                }
            }
            ctx.status(204)
        }
        delete("/$version/jumpDel") { ctx ->
            val rm = ctx.bodyAsClass(Jump::class.java) // TODO remove by name/id
            transaction {
                rm.delete()
            }
        }
    }
}
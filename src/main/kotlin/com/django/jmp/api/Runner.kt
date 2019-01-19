package com.django.jmp.api

import com.django.jmp.db.Jump
import com.django.jmp.db.JumpJson
import com.django.jmp.db.Jumps
import com.django.log2.logging.Log
import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
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
        get("/$version/jumps") { ctx ->
            val items = arrayListOf<JumpJson>()
            transaction {
                Log.i(javaClass, "API:GET -> ${ctx.path()}")
                Log.d(javaClass, Jump.all().count().toString())
                Jump.all().forEach {
                    items.add(JumpJson(it))
                }
            }
            ctx.json(items)
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
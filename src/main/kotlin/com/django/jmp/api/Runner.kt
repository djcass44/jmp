package com.django.jmp.api

import com.django.jmp.model.To
import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.*

fun main() {
    val version = "v1"
    val items = arrayListOf(To(title = "google", location = "https://www.google.com"))
    val app = Javalin.create().apply {
        port(7000)
//        enableStaticFiles("/public")
    }.start()
    app.routes {
        get("/$version/jumps") { ctx ->
            ctx.json(items)
        }
        put("/$version/jumpAdd") { ctx ->
            items.add(ctx.bodyAsClass(To::class.java))
            ctx.status(204)
        }
        delete("/$version/jumpDel") { ctx ->
            items.remove(ctx.bodyAsClass(To::class.java)) // TODO remove by name/id
        }
    }
}
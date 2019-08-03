package dev.castive.jmp.api.actions

import dev.castive.jmp.api.v2_1.FaviconPayload
import dev.castive.jmp.api.v2_1.WebSocket
import dev.castive.jmp.db.dao.Jump
import dev.castive.jmp.db.dao.Jumps
import dev.castive.jmp.except.InsecureDomainException
import dev.castive.log2.Log
import org.jetbrains.exposed.sql.transactions.transaction
import org.jsoup.Jsoup
import java.net.URI

class TitleAction(private val address: String, private val ws: (tag: String, data: Any) -> (Unit)) {
    fun get() = Thread {
        try {
            if(address.startsWith("http://")) throw InsecureDomainException()
            val uri = URI(address)
            // Get get the actual domain address (e.g. google.com/search?q=blahblah -> google.com)
            val host = "${uri.scheme}://${uri.host}"
            val document = Jsoup.connect(host).get()
            val title = document.head().getElementsByTag("title").text()
            transaction {
                val results = Jump.find { Jumps.location eq address }
                for (r in results) if (r.title == null || r.title != title) {
                    Log.v(javaClass, "Updating title for ${r.name} [previous: ${r.title}, new: $title]")
                    r.title = title
                    ws.invoke(WebSocket.EVENT_UPDATE_TITLE, FaviconPayload(r.id.value, title))
                }
            }
        }
        catch (e: Exception) {
            Log.e(javaClass, "Failed to load title: $e")
        }
    }.apply {
        isDaemon = true
        start()
    }
}
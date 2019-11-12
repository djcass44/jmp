package dev.castive.jmp.api.actions

import dev.castive.jmp.api.Socket
import dev.castive.jmp.db.dao.Jumps
import dev.castive.jmp.db.repo.findAllByLocation
import dev.castive.jmp.except.InsecureDomainException
import dev.castive.jmp.util.EnvUtil
import dev.castive.log2.Log
import dev.castive.log2.logv
import dev.dcas.castive_utilities.extend.env
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jetbrains.exposed.sql.transactions.transaction
import org.jsoup.Jsoup
import java.net.URI

class TitleAction(private val ws: (tag: String, data: Any) -> (Unit)) {
    private val allowed = EnvUtil.JMP_ALLOW_EGRESS.env("true").toBoolean()

    fun get(address: String) = GlobalScope.launch(context = Dispatchers.IO) {
        // check that we are allowed to make network requests
        if(!allowed) {
            "Unable to load title: blocked by JMP_ALLOW_EGRESS policy".logv(javaClass)
            return@launch
        }
        try {
            if(address.startsWith("http://")) throw InsecureDomainException()
            val uri = URI(address)
            // Get get the actual domain address (e.g. google.com/search?q=blahblah -> google.com)
            val host = "${uri.scheme}://${uri.host}"
            val document = Jsoup.connect(host).get()
            val title = document.head().getElementsByTag("title").text()
            transaction {
                val results = Jumps.findAllByLocation(address)
                for (r in results) if (r.title == null || r.title != title) {
                    Log.v(javaClass, "Updating title for ${r.name} [previous: ${r.title}, new: $title]")
                    r.title = title
                    ws.invoke(Socket.EVENT_UPDATE_TITLE, Socket.FaviconPayload(r.id.value, title))
                }
            }
        }
        catch (e: Exception) {
            Log.e(javaClass, "Failed to load title: $e")
        }
    }
}

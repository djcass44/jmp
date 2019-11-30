/*
 *    Copyright 2019 Django Cass
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package dev.castive.jmp.api.actions

import dev.castive.jmp.api.Socket
import dev.castive.jmp.db.dao.Jumps
import dev.castive.jmp.db.repo.findAllByLocation
import dev.castive.jmp.util.EnvUtil
import dev.castive.log2.loge
import dev.castive.log2.logv
import dev.dcas.util.extend.env
import dev.dcas.util.extend.safe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jetbrains.exposed.sql.transactions.transaction

class ImageAction(private val ws: (tag: String, data: Any) -> (Unit)) {
	private val favUrl = EnvUtil.FAV2_URL.env("http://localhost:8080")
	private val allowed = EnvUtil.JMP_ALLOW_EGRESS.env("true").toBoolean()
	private val client = OkHttpClient()

	fun get(address: String) = GlobalScope.launch(context = Dispatchers.IO) {
		// check that we are allowed to make network requests
		if(!allowed) {
			"Unable to load favicon: blocked by egress policy".logv(javaClass)
			return@launch
		}
		// Create the request for fav2
		val destUrl = "$favUrl/icon?site=${address.safe()}"
		val request = Request.Builder().url(destUrl).get().build()
		try {
			// make the request to warm fav2
			client.newCall(request).execute().close()
			transaction {
				// Get the jumps which may use this favicon
				val results = Jumps.findAllByLocation(address)
				for (r in results) if(r.image == null || r.image != destUrl) {
					// Update the image url to the new one
					"Updating icon for ${r.name} [previous: ${r.image}, new: $destUrl]".logv(javaClass)
					r.image = destUrl
					// Fire a websocket update to inform the clients
					ws.invoke(Socket.EVENT_UPDATE_FAVICON, Socket.FaviconPayload(r.id.value, destUrl))
				}
			}
		}
		catch (e: Exception) {
			"Fav2 POST request failed: $e".loge(javaClass)
		}
	}
}

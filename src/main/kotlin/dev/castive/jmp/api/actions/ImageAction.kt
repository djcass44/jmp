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
import dev.castive.jmp.db.dao.Jump
import dev.castive.jmp.db.dao.Jumps
import dev.castive.jmp.util.EnvUtil
import dev.castive.log2.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.jetbrains.exposed.sql.transactions.transaction
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class ImageAction(private val ws: (tag: String, data: Any) -> (Unit)) {
	private val favUrl = EnvUtil.getEnv(EnvUtil.FAV2_URL, "http://localhost:8080")
	private val client = OkHttpClient()

	/**
	 * Convert a string to be url safe
	 * This should probably be moved to a util package
	 */
	private fun safe(string: String) = URLEncoder.encode(string, StandardCharsets.UTF_8)

	fun get(address: String) {
		GlobalScope.launch(context = Dispatchers.IO) {
			// Create the request for fav2
			val request = Request.Builder().url("$favUrl/icon?site=${safe(address)}").post(RequestBody.create(null, "")).build()
			try {
				val response = client.newCall(request).execute()
				// If the response isn't OK, return
				if(!response.isSuccessful) {
					Log.e(javaClass, "Fav2 POST request failed: ${response.message()}")
					response.close()
					return@launch
				}
				// Try to get the body response
				val destUrl = response.body()?.string() ?: run {
					response.close()
					throw Exception("No body returned from request")
				}
				transaction {
					// Get the jumps which may use this favicon
					val results = Jump.find { Jumps.location eq address }
					for (r in results) if(r.image == null || r.image != destUrl) {
						// Update the image url to the new one
						Log.v(javaClass, "Updating icon for ${r.name} [previous: ${r.image}, new: $destUrl]")
						r.image = destUrl
						// Fire a websocket update to inform the clients
						ws.invoke(Socket.EVENT_UPDATE_FAVICON, Socket.FaviconPayload(r.id.value, destUrl))
					}
				}
			}
			catch (e: Exception) {
				Log.e(javaClass, "Fav2 POST request failed: $e")
			}
		}
	}
}

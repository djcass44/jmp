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

package dev.castive.jmp.data

data class FSA(
	val type: String,
	val payload: Any?,
	val error: Boolean = false,
	val meta: Any? = null
) {
	companion object {
		const val INIT_APP = "INIT_APP"
		const val TYPE_PING = "@API/PING"
		const val EVENT_UPDATE_JUMP = "EVENT_UPDATE"
		const val EVENT_UPDATE_USER = "EVENT_UPDATE_USER"
		const val EVENT_UPDATE_GROUP = "EVENT_UPDATE_GROUP"
		const val EVENT_UPDATE_TITLE = "EVENT_UPDATE_TITLE"
		const val EVENT_UPDATE_FAVICON = "EVENT_UPDATE_FAVICON"

		// message sent when we were unable to process a websocket message
		const val WS_SEND_FAILURE = "WS_SEND_FAILURE"
	}
}

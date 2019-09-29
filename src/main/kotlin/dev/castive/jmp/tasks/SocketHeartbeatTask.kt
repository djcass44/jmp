/*
 *    Copyright [2019 Django Cass
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

package dev.castive.jmp.tasks

import dev.castive.jmp.api.Socket
import dev.castive.log2.logi
import dev.castive.log2.logv
import dev.castive.log2.logw
import kotlin.concurrent.timer

/**
 * Starts a repeating alarm to send a message to all websocket clients every 10 seconds
 * This is used to ensure that they don't time out
 */
object SocketHeartbeatTask: Task {
	private const val delay = 10_000L
	lateinit var broadcaster: (tag: String, data: Any) -> (Unit)

	override fun start() {
		// start the task
		"Starting timer: ${javaClass.name}".logi(javaClass)
		timer(javaClass.name, true, 0, delay) {
			SocketHeartbeatTask::run.invoke()
		}
	}

	override fun run() {
		if(!this::broadcaster.isInitialized) {
			"Unable to execute ${javaClass.name}: broadcaster is not initialised".logw(javaClass)
			return
		}
		"Firing socket heartbeat at ${System.currentTimeMillis()}".logv(javaClass)
		// send a message to all listeners
		broadcaster.invoke(Socket.EVENT_PING, "PONG")
	}

}
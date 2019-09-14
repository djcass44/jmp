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

package dev.castive.jmp.db

import dev.castive.jmp.util.EnvUtil.getEnv
import dev.castive.log2.Log

data class ConfigStore(
	val url: String,
	val driver: String,
	val baseUrl: String,
	val tableUser: String? = "",
	val tablePassword: String? = "",
	val dataPath: String = "."
)

class Config {
	companion object {
		private const val defaultUrl = "jdbc:sqlite:jmp.db"
		private const val defaultDriver = "org.sqlite.JDBC"

		private const val envUrl = "DRIVER_URL"
		private const val envDriver = "DRIVER_CLASS"
		private const val envUser = "DRIVER_USER"
		private const val envKey = "DRIVER_PASSWORD"
		private const val envBaseUrl = "BASE_URL"
		private const val envDataPath = "JMP_HOME"
	}
	fun loadEnv(): ConfigStore {
		Log.i(javaClass, "Using environment for application/database configuration")
		return ConfigStore(
			getEnv(envUrl, defaultUrl),
			getEnv(envDriver, defaultDriver),
			getEnv(envBaseUrl, "http://localhost:8080"),
			getEnv(envUser, ""),
			getEnv(envKey, ""),
			getEnv(envDataPath, ".")
		)
	}
}
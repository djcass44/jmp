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

package com.django.jmp.db

import com.beust.klaxon.Klaxon
import com.django.log2.logging.Log

data class ConfigStore(val url: String, val driver: String, val logRequestDir: String, val BASE_URL: String, val tableUser: String? = "", val tablePassword: String? = "")

class Config {
    companion object {
        private const val defaultUrl = "jdbc:sqlite:jmp.db"
        private const val defaultDriver = "org.sqlite.JDBC"

        private const val envUrl = "DRIVER_URL"
        private const val envDriver = "DRIVER_CLASS"
        private const val envUser = "DRIVER_USER"
        private const val envKey = "DRIVER_PASSWORD"

        private const val logRequestDir = "ENV_LOG_REQUEST_DIRECTORY"
    }
    fun load(name: String): ConfigStore {
        val fileContent = Config::class.java.getResource("/$name").readText()
        return try {
            val store = Klaxon().parse<ConfigStore>(fileContent)
            store?: loadEnv()
        }
        catch (e: Exception) {
            e.printStackTrace()
            Log.e(javaClass, "Failed to parse config")
            loadEnv()
        }
    }
    fun loadEnv(): ConfigStore {
        Log.w(javaClass, "Trying to use ENV for database configuration (default SQLite)")
        return ConfigStore(
            Util.getEnv(envUrl, defaultUrl),
            Util.getEnv(envDriver, defaultDriver),
            Util.getEnv(logRequestDir, "."),
            Util.getEnv("BASE_URL", "http://localhost:8080"),
            Util.getEnv(envUser, ""),
            Util.getEnv(envKey, "")
        )
    }
}
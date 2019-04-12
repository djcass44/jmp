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

import com.django.log2.logging.Log
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import dev.castive.jmp.Runner
import dev.castive.jmp.db.source.HikariSource

class DatabaseHelper {
    fun start(store: ConfigStore) {
        Log.v(Runner::class.java, "Database config: [${store.url}, ${store.driver}]")
        Log.v(Runner::class.java, "Application config: [${store.BASE_URL}, ${store.logRequestDir}, ${store.dataPath}]")
        val ds = HikariDataSource(HikariConfig().apply {
            jdbcUrl = store.url
            driverClassName = store.driver
            username = store.tableUser
            password = store.tablePassword
        })
        HikariSource().connect(ds)
        Runtime.getRuntime().addShutdownHook(Thread {
            Log.w(Runner::class.java, "Running shutdown hook, DO NOT forcibly close the application")
            ds.close()
        })
    }
}
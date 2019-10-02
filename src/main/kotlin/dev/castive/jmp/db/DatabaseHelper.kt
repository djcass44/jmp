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

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import dev.castive.jmp.db.dao.*
import dev.castive.jmp.db.source.HikariSource
import dev.castive.jmp.tasks.GroupsTask
import dev.castive.jmp.util.EnvUtil
import dev.castive.jmp.util.asEnv
import dev.castive.log2.Log
import dev.castive.log2.logv
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

class DatabaseHelper {
    fun start() {
        val ds = HikariDataSource(HikariConfig().apply {
            jdbcUrl = EnvUtil.DRIVER_URL.asEnv("jdbc:sqlite:jmp.db")
            driverClassName = EnvUtil.DRIVER_CLASS.asEnv("org.sqlite.JDBC")
            username = EnvUtil.DRIVER_USER.asEnv()
            password = EnvUtil.DRIVER_PASSWORD.asEnv()
        })
        "Database configuration: [url: ${ds.jdbcUrl}, class: ${ds.driverClassName}]".logv(javaClass)
        HikariSource().connect(ds)
        Runtime.getRuntime().addShutdownHook(Thread {
            Log.w(javaClass, "Running shutdown hook, DO NOT forcibly close the application")
            ds.close()
        })
        setup()
    }
    private fun setup() {
        transaction {
            // Ensure that the tables are created
            Log.i(javaClass, "Checking for database drift")
            SchemaUtils.createMissingTablesAndColumns(Jumps, Users, Roles, Groups, GroupUsers, Aliases, Sessions)
            Init() // Ensure that the default admin/roles is created
        }
        // start the GroupsTask cron
        GroupsTask.start()
    }
}
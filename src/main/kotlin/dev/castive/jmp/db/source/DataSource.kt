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

package dev.castive.jmp.db.source

import dev.castive.log2.Log
import org.jetbrains.exposed.sql.transactions.TransactionManager
import java.sql.Connection

abstract class DataSource {
    open fun preConnect() {
        Log.v(javaClass, "Connecting to database")
    }
    open fun connect(url: String) {
        postConnect(url)
    }
    open fun postConnect(url: String) {
        if(url.contains("sqlite", true) || url.contains("oracle", true)) {
            // Database requires special transaction levels
            TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE
            Log.i(javaClass, "Compensating for SQLite/Oracle database")
        }
    }
}
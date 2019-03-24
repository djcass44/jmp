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

import dev.castive.jmp.db.dao.Group
import dev.castive.jmp.db.dao.Groups
import org.jetbrains.exposed.sql.transactions.transaction

class GroupAction {
    companion object {
        private lateinit var instance: GroupAction

        fun getInstance(): GroupAction {
            if(!this::instance.isInitialized)
                instance = GroupAction()
            return instance
        }
    }

    fun getGroupByName(name: String): Group? = transaction {
        Group.find {
            Groups.name eq name
        }.elementAtOrNull(0)
    }
}
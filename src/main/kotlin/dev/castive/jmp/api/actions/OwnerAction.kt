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

import dev.castive.jmp.db.dao.*
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

object OwnerAction {
    fun getUserVisibleJumps(user: User?): ArrayList<Jump> {
        val jumps = arrayListOf<Jump>()
        transaction {
            // STAGE 1 get global + jumps the user owns
            val res = Jump.find {
                (Jumps.owner.isNull() and Jumps.ownerGroup.isNull())
            }
            jumps.addAll(res)
            if(user == null)
                return@transaction
            jumps.addAll(Jump.find { Jumps.owner.eq(user.id) })
            // STAGE 2 get jumps in groups user belongs to
            val r2 = (Groups innerJoin GroupUsers innerJoin Users)
                .slice(Groups.columns)
                .select {
                    Users.id eq user.id
                }
                .withDistinct()
            Group.wrapRows(r2).toList().forEach {
                val j = Jump.find {
                    Jumps.ownerGroup eq it.id
                }
                jumps.addAll(j)
            }
        }
        return jumps
    }
    fun getJumpFromUser(user: User?, jump: String): ArrayList<Jump> {
        val jumps = getUserVisibleJumps(user)
        val matches = arrayListOf<Jump>()
        jumps.forEach {
            if(it.name == jump || it.alias.contains(jump))
                matches.add(it)
        }
        return matches
    }
}
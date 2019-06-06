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

package dev.castive.jmp.auth

import dev.castive.javalin_auth.auth.external.ValidUserClaim
import dev.castive.jmp.db.dao.User
import dev.castive.jmp.db.dao.Users
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction

object ClaimConverter {
    fun getUser(claim: ValidUserClaim?): User? = transaction {
        if (claim == null) return@transaction null
        return@transaction User.find {
            Users.username eq claim.username and(Users.requestToken.eq(claim.token))
        }.elementAtOrNull(0)
    }

    fun get(claim: ValidUserClaim): User = transaction {
        return@transaction User.find {
            Users.username eq claim.username and(Users.requestToken.eq(claim.token))
        }.elementAt(0)
    }
}
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

import dev.castive.javalin_auth.auth.external.UserVerification
import dev.castive.jmp.api.Auth

class UserVerification(private val auth: Auth): UserVerification {
    override fun verify(userClaim: String, tokenClaim: String): Boolean {
        return auth.getUser(userClaim, tokenClaim) != null
    }

    override fun getToken(uid: String): String {
        return auth.getUserTokenWithPrivilege(uid)
    }

    override fun getToken(uid: String, password: String): String {
        return auth.getUserToken(uid, password.toCharArray()) ?: ""
    }
}
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

import com.django.jmp.api.Auth
import com.django.log2.logging.Log
import org.jetbrains.exposed.sql.transactions.transaction

class Init(private val superPassword: CharArray) {
    init {
        val superName = "admin"
        transaction {
            if(User.all().empty()) {
                Auth().createUser(superName, superPassword, true)
                Log.w(javaClass, "Created superuser with access: [username: $superName, password: $superPassword]\nPlease change this password ASAP")
            }
            if(Role.all().empty()) {
                for (r in Auth.BasicRoles.values()) {
                    Role.new {
                        name = r.name
                    }
                }
            }
        }
    }
}
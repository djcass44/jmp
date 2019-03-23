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
import com.django.jmp.db.dao.Role
import com.django.jmp.db.dao.User
import com.django.log2.logging.Log
import com.django.securepass3.PasswordGenerator
import org.jetbrains.exposed.sql.transactions.transaction
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

class Init {
    init {
        val superName = "admin" // Hardcoded into FE, don't change
        transaction {
            if(User.all().empty()) {
                val password = PasswordGenerator.getInstance().get(16, true)
                Auth().createUser(superName, password, true)
                Log.w(javaClass, "Created superuser with access: [username: $superName]\nPlease change this ASAP!\nThis will also be stored in the current directory in 'initialAdminPassword'")
                Files.writeString(Path.of("initialAdminPassword"), String(password), StandardCharsets.UTF_8)
                for (c in password) // Probably useless if converted to a string above
                    print(c)
                println()
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
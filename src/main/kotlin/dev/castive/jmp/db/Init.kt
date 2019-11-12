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

import dev.castive.javalin_auth.auth.Roles
import dev.castive.jmp.api.Auth
import dev.castive.jmp.crypto.KeyProvider
import dev.castive.jmp.db.dao.*
import dev.castive.jmp.db.repo.findFirstByUsername
import dev.castive.jmp.util.EnvUtil
import dev.castive.log2.Log
import dev.castive.log2.logi
import dev.castive.log2.logok
import dev.castive.log2.logv
import dev.dcas.castive_utilities.extend.env
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

class Init {
    init {
        val superName = "admin" // Hardcoded into FE, don't change
	    val systemUser = "system"
	    val keygen = KeyProvider()
	    var system: User
        transaction {
            // roles must be created before the admin
            if(Role.all().empty()) {
                Roles.BasicRoles.values().forEach {
                    Role.new { name = it.name }
                }
                "Initialised user roles".logok(javaClass)
            }
	        "Located ${User.all().count()} existing users...".logv(javaClass)
            if(User.all().empty()) {
                val password = keygen.createKey()
                Auth().createUser(superName, password.toCharArray(), true, "Admin")
                Log.w(javaClass, "Created superuser with access: [username: $superName]\nPlease change this ASAP!\nThis will also be stored in the current directory in 'initialAdminPassword'")
                try {
	                var path = EnvUtil.JMP_HOME.env("/data/")
	                if(!path.endsWith(File.separatorChar)) path += File.separatorChar
                    Files.writeString(Path.of("${path}initialAdminPassword"), password, StandardCharsets.UTF_8)
                }
                catch (e: Exception) {
                    Log.e(javaClass, "Failed to save admin password: $e")
                }
                println("\n$password\n")
            }
        }
	    transaction {
		    // find the system user or create it
		    system = Users.findFirstByUsername(systemUser) ?: Auth().createUser(systemUser, keygen.createKey().toCharArray(), false, displayName = "System")
		    "Updating meta[*] to v2 format".logi(javaClass)
		    var count = 0
		    Jump.all().forEach {
			    // create v2 meta objects for jumps without them
			    if(it.meta == null) {
				    it.meta = Meta.new {
					    created = it.metaCreation
					    edited = it.metaUpdate
					    createdBy = system
					    editedBy = system
				    }
				    count++
			    }
		    }
		    "Generated v2 Meta format for $count Jumps".logv(javaClass)
	    }
    }
}

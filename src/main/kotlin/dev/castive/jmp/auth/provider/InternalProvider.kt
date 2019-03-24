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

package dev.castive.jmp.auth.provider

import dev.castive.jmp.db.dao.GroupData
import dev.castive.jmp.db.dao.UserData

class InternalProvider: BaseProvider {
    companion object {
        const val SOURCE_NAME = "local"
    }
    private val auth = dev.castive.jmp.api.Auth()

    override fun setup() {

    }

    override fun getUsers(): ArrayList<UserData> {
        return arrayListOf()
    }

    override fun getGroups(): ArrayList<GroupData> {
        return arrayListOf()
    }

    override fun tearDown() {

    }

    override fun getLogin(uid: String, password: String): String? {
        return auth.getUserToken(uid, password.toCharArray())
    }
}
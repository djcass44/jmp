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

package com.django.jmp.auth.provider

import com.django.log2.logging.Log
import org.junit.jupiter.api.Test

class LDAPProviderTest {
    @Test
    fun testGetUsers() {
        val provider = LDAPProvider()
        provider.setup()
        assert(provider.connected)
        val users = provider.getUsers()
        assert(users.size == 5)
        provider.tearDown()
    }
    @Test
    fun testLoginCorrect() {
        val provider = LDAPProvider()
        provider.setup()
        assert(provider.connected)
        val validUser = provider.getLogin("bahaaldineazarmi", "bazarmi")
        Log.d(javaClass, "Valid user: $validUser")
        provider.tearDown()
        assert(validUser)
    }
    @Test
    fun testLoginIncorrect() {
        val provider = LDAPProvider()
        provider.setup()
        assert(provider.connected)
        val validUser = provider.getLogin("bahaaldineazarmi", "password")
        Log.d(javaClass, "Valid user: $validUser")
        provider.tearDown()
        assert(!validUser)
    }
}
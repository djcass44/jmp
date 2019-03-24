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

import com.django.jmp.auth.connect.LDAPConnection
import com.django.log2.logging.Log
import org.junit.jupiter.api.Test

class LDAPProviderTest {
    @Test
    fun testGetUsers() {
        val provider = LDAPProvider("localhost", 389, "ou=Users,dc=elastic,dc=co", "cn=admin,dc=elastic,dc=co", "password")
        provider.setup()
        assert(provider.connected)
        val users = provider.getUsers()
        assert(users.size == 5)
        provider.tearDown()
    }
    @Test
    fun testLoginCorrect() {
        val connection = LDAPConnection("localhost", 389, "ou=Users,dc=elastic,dc=co", "cn=admin,dc=elastic,dc=co", "password")
        assert(connection.connected)
        val validUser = connection.checkUserAuth("bahaaldineazarmi", "bazarmi")
        Log.d(javaClass, "Valid user: $validUser")
        connection.close()
        assert(validUser)
    }
    @Test
    fun testLoginIncorrect() {
        val connection = LDAPConnection("localhost", 389, "ou=Users,dc=elastic,dc=co", "cn=admin,dc=elastic,dc=co", "password")
        assert(connection.connected)
        val validUser = connection.checkUserAuth("bahaaldineazarmi", "password")
        Log.d(javaClass, "Valid user: $validUser")
        connection.close()
        assert(!validUser)
    }
}
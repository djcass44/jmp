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

package com.django.jmp.auth.connect

import com.django.log2.logging.Log
import java.util.*
import javax.naming.Context
import javax.naming.NamingException
import javax.naming.directory.InitialDirContext
import javax.naming.directory.SearchControls
import javax.naming.directory.SearchResult

class LDAPConnection(private val server: String, private val port: Int = 389, private val contextDN: String, private val serviceUserDN: String, private val serviceUserPassword: String) {
    var connected = false
        private set
    private lateinit var connection: InitialDirContext

    init {
        connect()
    }

    /**
     * Attempt to open an LDAP connection
     * Note: if there is an existing connection, it will be closed first
     */
    private fun connect() {
        if(connected) {
            Log.w(javaClass, "Found existing LDAP connection, this will be closed...")
            close()
        }
        val env = Hashtable<String, String>()
        env[Context.INITIAL_CONTEXT_FACTORY] = "com.sun.jndi.ldap.LdapCtxFactory"
        env[Context.PROVIDER_URL] = "ldap://$server:$port/"
        env[Context.SECURITY_PRINCIPAL] = serviceUserDN
        env[Context.SECURITY_CREDENTIALS] = serviceUserPassword
        try {
            connection = InitialDirContext(env)
            Log.ok(javaClass, "LDAP Authentication success!")
            connected = true
        }
        catch (e: NamingException) {
            Log.e(javaClass, "LDAP Authentication failure: $e")
            connected = false
        }
    }

    /**
     * Attempt to close the LDAP connection
     */
    fun close() {
        try {
            if(!connected) {
                Log.i(javaClass, "There is no active connection to close!")
                return
            }
            connection.close()
            Log.i(javaClass, "LDAP Connection closed")
            connected = false
        }
        catch (e: NamingException) {
            Log.e(javaClass, "Failed to close LDAP connection")
        }
    }

    /**
     * Run an arbitrary search
     */
    fun searchFilter(filter: String): ArrayList<SearchResult> {
        val controls = SearchControls()
        controls.searchScope = SearchControls.SUBTREE_SCOPE
        val searchResults = connection.search(contextDN, filter, controls)
        return if(searchResults.hasMoreElements()) {
            val results = arrayListOf<SearchResult>()
            while (searchResults.hasMore()) results.add(searchResults.next())
            results
        }
        else arrayListOf()
    }

    fun checkUserAuth(uid: String, password: String): Boolean {
        val user = searchFilter("(uid=$uid)")
        Log.d(javaClass, "Found user: $user")
        assert(user.size == 1) // There must be only 1 user with a uid
        val dn = user[0].nameInNamespace

        // Open a new connection with the users creds
        val verifyConnection = LDAPConnection(server, port, contextDN, dn, password)
        val connect = verifyConnection.connected
        Log.i(javaClass, "User credential validation: $connect")

        verifyConnection.close()

        return connect
    }
}
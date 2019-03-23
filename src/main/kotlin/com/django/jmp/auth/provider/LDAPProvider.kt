package com.django.jmp.auth.provider

import biz.stillhart.profileManagement.utils.LDAPConnection
import com.django.jmp.db.dao.GroupData
import com.django.jmp.db.dao.UserData
import com.django.log2.logging.Log
import java.util.*
import javax.naming.AuthenticationException
import javax.naming.NamingException

class LDAPProvider: BaseProvider {
    companion object {
        const val SOURCE_NAME = "ldap"
    }
    private lateinit var connection: LDAPConnection

    var connected = false
        private set

    override fun setup() = try {
        connection = LDAPConnection("localhost", 389, "ou=Users,dc=elastic,dc=co", "cn=admin,dc=elastic,dc=co", "password")
        connected = connection.isConnected
        Log.i(javaClass, "LDAP connected: $connected")
    }
    catch (e: AuthenticationException) {
        connected = false
        Log.e(javaClass, "LDAP -> Wrong authentication")
    }
    catch (e: NamingException) {
        connected = false
        Log.e(javaClass, "LDAP -> Couldn't connect: $e")
    }

    override fun getUsers(): ArrayList<UserData> {
        val users = arrayListOf<UserData>()
        val result = connection.getResultByCustomFilter("(objectClass=inetOrgPerson)")
        for (r in result) {
            val username = r.attributes.get("uid").get(0).toString()
            val role = r.attributes.get("objectClass").get(0).toString()
            Log.d(javaClass, "Found user: $username, $role")
            users.add(UserData(UUID.randomUUID(), username, role, arrayListOf(), 0, 0, SOURCE_NAME))
        }
        return users
    }
    override fun getGroups(): ArrayList<GroupData> {
        throw NotImplementedError()
    }
    override fun tearDown() {
        connection.close()
    }
}
package dev.castive.jmp.util.checks

import dev.castive.javalin_auth.auth.connect.LDAPConfig
import dev.castive.javalin_auth.auth.connect.LDAPConnection

class LDAPCheck(private val config: LDAPConfig): StartupCheck("LDAP Connection") {
    override fun runCheck(): Boolean {
        // TODO test that this actually works
        val connection = LDAPConnection(config, false)
        return if(connection.connected) {
            onSuccess()
            true
        }
        else {
            onFail()
            false
        }
    }
}
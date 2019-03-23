package com.django.jmp.auth.provider

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
}
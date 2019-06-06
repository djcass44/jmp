package dev.castive.jmp.util.checks

import dev.castive.javalin_auth.auth.Providers

class ProviderCheck: StartupCheck("Identity Provider Connection") {
    override fun runCheck(): Boolean {
        return when {
            Providers.primaryProvider == null -> {
                onWarning()
                true
            }
            Providers.primaryProvider!!.connected() -> {
                onSuccess()
                true
            }
            else -> {
                onFail()
                false
            }
        }
    }
}
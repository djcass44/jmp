package dev.castive.jmp.util.checks

import dev.castive.jmp.db.dao.Users
import dev.castive.jmp.db.repo.count
import dev.castive.log2.Log

class DatabaseCheck: StartupCheck("Database connection") {
    override fun runCheck(): Boolean = try {
        // Assumes that initial db setup has already been executed
        assert(Users.count() > 0)
        onSuccess()
        true
    }
    catch (e: Exception) {
        Log.e(javaClass, "Database connection test failed: $e")
        onFail()
        false
    }
}
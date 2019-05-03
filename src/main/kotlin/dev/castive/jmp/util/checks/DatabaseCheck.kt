package dev.castive.jmp.util.checks

import dev.castive.jmp.db.dao.User
import dev.castive.log2.Log
import org.jetbrains.exposed.sql.transactions.transaction

class DatabaseCheck: StartupCheck("Database connection") {
    override fun runCheck(): Boolean = try {
        transaction {
            // Assumes that initial db setup has already been executed
            assert(User.all().count() > 0)
        }
        onSuccess()
        true
    }
    catch (e: Exception) {
        Log.e(javaClass, "Database connection test failed: $e")
        onFail()
        false
    }
}
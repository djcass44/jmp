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

package dev.castive.jmp

import dev.castive.jmp.api.App
import dev.castive.jmp.audit.Logger
import dev.castive.jmp.db.Config
import dev.castive.jmp.db.ConfigStore
import dev.castive.jmp.db.DatabaseHelper
import dev.castive.jmp.util.checks.AuditCheck
import dev.castive.jmp.util.checks.EntropyCheck
import dev.castive.jmp.util.checks.SecureConfigCheck
import dev.castive.log2.Log
import java.util.*


class Runner {
    companion object {
        const val BASE = "/api"
        var START_TIME = 0L
        lateinit var store: ConfigStore
        lateinit var args: Array<String>
    }
    private fun runInitialChecks(store: ConfigStore, arguments: Arguments) {
        Log.i(javaClass, "Checking security configuration")
        println("Running setup checks\n")
        val checks = arrayListOf(SecureConfigCheck(store.BASE_URL, arguments), EntropyCheck(), AuditCheck())
        var count = 0
        for (c in checks) {
            if(c.runCheck()) count++
        }
        println("Setup checks completed ($count/${checks.size} passed)\n")
    }
    fun start(args: Array<String>) {
        Runner.args = args
        START_TIME = System.currentTimeMillis()
        Log.v(javaClass, Arrays.toString(args))
        val arguments = Arguments(args)
        if(arguments.enableCors) Log.w(javaClass, "WARNING: CORS access is enable for ALL origins. DO NOT allow this in production: WARNING")
        Log.setPriorityLevel(arguments.debugLevel)
        val configLocation = if(args.size >= 2 && args[0] == "using") {
            args[1]
        }
        else
            "env"
        Log.i(javaClass, "Using database path: $configLocation")
        val store = if(configLocation.isNotBlank() && configLocation != "env")
            Config().load(configLocation)
        else
            Config().loadEnv()
        Runner.store = store
        val logger = Logger(store.logRequestDir)
        runInitialChecks(store, arguments)
        DatabaseHelper().start(store)
        App().start(store, arguments, logger)
    }
}

fun main(args: Array<String>) {
    Runner().start(args)
}
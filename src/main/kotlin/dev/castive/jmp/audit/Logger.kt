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

package dev.castive.jmp.audit

import dev.castive.jmp.io.NOutputStream
import com.django.log2.logging.Log
import java.io.File
import java.io.FileOutputStream
import java.io.PrintStream
import java.nio.charset.StandardCharsets

class Logger(logPath: String) {
    private val logDir = File(logPath, "logs")

    private val logRequest = File(logDir, "jmp.request.out")
    private val logOut = File(logDir, "jmp.std.out")
    private val logErr = File(logDir, "jmp.err.out")

    init {
        Log.v(javaClass, "Using context directory: ${logDir.absolutePath}")
        if(!logDir.exists()) {
            logDir.parentFile.mkdirs()
            logDir.mkdir()
        }
        if(!logRequest.exists()) {
            try {
                logRequest.createNewFile()
            }
            catch (e: Exception) {
                Log.e(javaClass, "Failed to setup log directory: $e")
            }
        }
        if(logDir.exists() && logDir.isDirectory) {
            // Copy content of STDOUT to file
            val stream = NOutputStream()
            stream.add(System.out)
            stream.add(PrintStream(FileOutputStream(logOut, true)))
            System.setOut(PrintStream(stream))
            // Copy contents of STDERR to file
            val errorStream = NOutputStream()
            errorStream.add(System.err)
            errorStream.add(PrintStream(FileOutputStream(logErr, true)))
            System.setErr(PrintStream(errorStream))
        }
        else Log.f(javaClass, "Logging directory is invalid, logging will be disabled")
    }
    fun add(text: String) = try {
        logRequest.appendText("$text\n", StandardCharsets.UTF_8)
    }
    catch (e: Exception) {
        Log.e(javaClass, "Failed to log: $e")
    }
}
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

package com.django.jmp.audit

import com.django.log2.logging.Log
import java.io.File
import java.nio.charset.StandardCharsets

class Logger(logPath: String) {
    private val logDir = File(logPath, "logs")

    private val logRequest = File(logDir, "jmp.request.out")
    init {
        Log.v(javaClass, "Using context directory: ${logDir.absolutePath}")
        if(!logDir.exists()) {
            logDir.parentFile.mkdirs()
            logDir.mkdir()
        }
        if(!logRequest.exists())
            logRequest.createNewFile()
    }
    fun add(text: String) = try {
        logRequest.appendText("$text\n", StandardCharsets.UTF_8)
    }
    catch (e: Exception) {
        Log.e(javaClass, "Failed to log: $e")
    }
}
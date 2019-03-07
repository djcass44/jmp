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

package com.django.jmp.api.actions

import java.lang.management.ManagementFactory
import java.util.concurrent.TimeUnit

class InfoAction {
    data class Info(val os: String, val jvm: String, val uptime: String, val version: String)
    fun get(): Info {
        val os = System.getProperty("os.name")
        val jvm = System.getProperty("java.vm.version")
        val version = "2.0"
        val uptime = ManagementFactory.getRuntimeMXBean().uptime

        val uptimeString = String.format("%02d min, %02d sec",
            TimeUnit.MILLISECONDS.toMinutes(uptime),
            TimeUnit.MILLISECONDS.toSeconds(uptime) -
                    TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(uptime))
        )
        return Info(os, jvm, uptimeString, version)
    }
}
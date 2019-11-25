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

package dev.castive.jmp.api.actions

import dev.castive.javalin_auth.auth.RequestUserLocator
import dev.castive.jmp.Arguments
import dev.castive.jmp.Runner
import dev.castive.jmp.db.dao.Group
import dev.castive.jmp.db.dao.Jump
import dev.castive.jmp.db.dao.Jumps
import dev.castive.jmp.db.dao.User
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction
import java.lang.management.ManagementFactory
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.pow

class InfoAction(private val arguments: Arguments) {
    data class SystemInfo(val osInfo: OSInfo, val cpus: Int, val javaInfo: JavaInfo, val kotlinInfo: KotlinInfo, val memoryInfo: MemoryInfo)
    data class AppInfo(val version: String, val apiLevel: Double, val users: Int, val groups: Int, val identityInfo: IdentityInfo, val jumpInfo: JumpInfo, val appUptime: String, val launchArgs: ArrayList<String>)

    data class JavaInfo(val name: String, val version: String, val specification: String, val vendor: String, val home: String, val uptime: String)
    data class KotlinInfo(val major: Int, val minor: Int, val patch: Int)
    data class OSInfo(val name: String, val version: String, val arch: String)
    data class MemoryInfo(val total: String, val max: String, val used: String)
    data class IdentityInfo(val providerName: String, val providerClass: String)
    fun getSystem(): SystemInfo {
        val osInfo = OSInfo(System.getProperty("os.name"), System.getProperty("os.version"), System.getProperty("os.arch"))

        val cpuCount = Runtime.getRuntime().availableProcessors()

        val jvmUptime = ManagementFactory.getRuntimeMXBean().uptime
        val uptimeString = timeSpan(jvmUptime)
        val javaInfo = JavaInfo(System.getProperty("java.vm.name"),
            System.getProperty("java.vm.version"),
            System.getProperty("java.vm.specification.version"),
            System.getProperty("java.vm.vendor"),
            System.getProperty("java.home"), uptimeString)
        val kotlinInfo = KotlinInfo(KotlinVersion.CURRENT.major, KotlinVersion.CURRENT.minor, KotlinVersion.CURRENT.patch)
        val totalMemory = getByteFormatted(Runtime.getRuntime().totalMemory())
        val maxMemory = getByteFormatted(Runtime.getRuntime().maxMemory())
        val usedMemory = getByteFormatted(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory())
        val memInfo = MemoryInfo(totalMemory, maxMemory, usedMemory)
        return SystemInfo(osInfo, cpuCount, javaInfo, kotlinInfo, memInfo)
    }
    data class JumpInfo(val total: Int, val global: Int, val personal: Int, val grouped: Int)
    /**
     * This information may be sensitive in nature, only allow access to ADMIN
     */
    fun getApp(): AppInfo = transaction {
        val version = dev.castive.jmp.Version.getVersion()
        val users = User.all().count()
        val groups = Group.all().count()
        val jumps = Jump.all().count()
        val globalJumps = Jump.find { Jumps.owner.isNull() and Jumps.ownerGroup.isNull() }.count()
        val personalJumps = Jump.find { Jumps.owner.isNotNull() }.count()
        val groupedJumps = Jump.find { Jumps.ownerGroup.isNotNull() }.count()
        val jumpInfo = JumpInfo(jumps, globalJumps, personalJumps, groupedJumps)
        val uptime = System.currentTimeMillis() - Runner.START_TIME
        val uptimeString = timeSpan(uptime)
        val identityInfo = IdentityInfo(
	        RequestUserLocator::class.java.name,
	        RequestUserLocator::class.java.name
        )
        return@transaction AppInfo(version, 2.1, users, groups, identityInfo, jumpInfo, uptimeString, ArrayList(arguments.args.toList()))
    }

    private fun slf(n: Double): String = floor(n).toLong().toString()
    private fun timeSpan(timeInMs: Long): String {
        val t = timeInMs.toDouble()
        if (t < 1000.0)
            return slf(t) + "ms"
        if (t < 60000.0)
            return slf(t / 1000.0) + "s " +
                    slf(t % 1000.0) + "ms"
        if (t < 3600000.0)
            return slf(t / 60000.0) + "m " +
                    slf(t % 60000.0 / 1000.0) + "s " +
                    slf(t % 1000.0) + "ms"
        return if (t < 86400000.0) slf(t / 3600000.0) + "h " +
                slf(t % 3600000.0 / 60000.0) + "m " +
                slf(t % 60000.0 / 1000.0) + "s " +
                slf(t % 1000.0) + "ms" else slf(t / 86400000.0) + "d " +
                slf(t % 86400000.0 / 3600000.0) + "h " +
                slf(t % 3600000.0 / 60000.0) + "m " +
                slf(t % 60000.0 / 1000.0) + "s " +
                slf(t % 1000.0) + "ms"
    }

    private fun getByteFormatted(bytes: Long, si: Boolean = true): String {
        val unit = if (si) 1000 else 1024
        if (bytes < unit) return "$bytes B"
        val exp = (ln(bytes.toDouble()) / ln(unit.toDouble())).toInt()
        val pre = (if (si) "kMGTPE" else "KMGTPE")[exp - 1] + if (si) "" else "i"
        return String.format("%.1f %sB", bytes / unit.toDouble().pow(exp.toDouble()), pre)
    }
}

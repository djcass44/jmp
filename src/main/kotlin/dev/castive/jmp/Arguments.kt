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

import dev.castive.log2.Log
import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.HelpFormatter
import org.apache.commons.cli.Options
import java.util.*
import kotlin.system.exitProcess

class Arguments(val args: Array<String>) {
    private val options = Options()
    var enableCors = false
    var enableDev = false

    init {
        options.addOption("h", "help", false, "Show help (you are probably here)")
        options.addOption(null, "enable-cors", false, "Allow Cross-Origin Resource Sharing. This should only be used for development purposes. Note: the application will suicide if this is enabled on an HTTPS url")
        options.addOption("dev", "enable-dev", false, "Allow development features. Implies --enable-cors. Note: the application will suicide if this is enabled on an HTTPS url")
        options.addOption("d", "debug-level", true, "Set the level of logs to be output (0 -> 6, lower is more).")

        val helpFormatter = HelpFormatter()
        val parser = DefaultParser()
        try {
            Log.v(javaClass, "Arguments parser will receive: ${Arrays.toString(args)}")
            val cl = parser.parse(options, args)
            if(cl.hasOption("h")) {
                helpFormatter.printHelp("jmp", "jmp-${Version.getVersion()}", options, "", true)
                Log.w(javaClass, "Printed help, application will now exit!")
                exitProcess(0)
            }
            enableDev = cl.hasOption("enable-dev")
            enableCors = cl.hasOption("enable-cors") || enableDev
        }
        catch (e: Exception) {
            Log.e(javaClass, e.toString())
        }
    }
}
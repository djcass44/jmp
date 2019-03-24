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

import com.django.log2.logging.Log
import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.HelpFormatter
import org.apache.commons.cli.Options
import java.util.*

class Arguments(args: Array<String>) {
    val options = Options()
    var enableCors = false
    var debugLevel = 0

    init {
        options.addOption("h", "help", false, "Show help (you are probably here)")
        options.addOption(null, "enable-cors", false, "Allow Cross-Origin Resource Sharing. This should only be used for development purposes. Note: the application will suicide if this is enabled on an HTTPS url")
        options.addOption("d", "debug-level", true, "Set the level of logs to be output (0 -> 6, lower is more).")

        val helpFormatter = HelpFormatter()
        val parser = DefaultParser()
        try {
            // args 0,1 are always using xxx
            val relevantArgs = when {
                args.size < 2 -> args
                args.size == 2 -> arrayOf()
                else -> args.copyOfRange(2, args.size)
            }
            Log.v(javaClass, "Arguments parser will receive: ${Arrays.toString(relevantArgs)}")
            val cl = parser.parse(options, relevantArgs)
            if(cl.hasOption("h")) {
                helpFormatter.printHelp("jmp", "jmp-${dev.castive.jmp.Version.getVersion()}", options, "", true)
                Log.w(javaClass, "Printed help, application will now exit!")
                System.exit(0)
            }
            enableCors = cl.hasOption("enable-cors")
            debugLevel = if(cl.hasOption("d")) cl.getOptionValue("d").toInt() else 2 // Set default to show info and above
        }
        catch (e: Exception) {
            Log.e(javaClass, e.toString())
        }
    }
}
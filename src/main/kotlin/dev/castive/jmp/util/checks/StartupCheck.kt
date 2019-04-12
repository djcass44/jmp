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

package dev.castive.jmp.util.checks

import dev.castive.jmp.util.LogUtil

abstract class StartupCheck(private val name: String) {
    abstract fun runCheck(): Boolean

    protected fun onFail() {
        println("$name.. ${LogUtil.ANSI_RED}FAILED${LogUtil.ANSI_RESET}")
    }
    protected fun onSuccess() {
        println("$name.. ${LogUtil.ANSI_GREEN}OK${LogUtil.ANSI_RESET}")
    }
    protected fun onWarning() {
        println("$name.. ${LogUtil.ANSI_YELLOW}WARNING${LogUtil.ANSI_RESET}")
    }
}
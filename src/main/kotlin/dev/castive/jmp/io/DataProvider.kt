/*
 *    Copyright [2019 Django Cass
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

package dev.castive.jmp.io

import dev.castive.jmp.util.EnvUtil
import dev.castive.log2.logv
import dev.castive.log2.logw
import dev.dcas.util.extend.env
import java.io.File

object DataProvider {
	private val dataPath = File(EnvUtil.JMP_HOME.env("/data"))

	private val allocations = hashMapOf<String, File>()

	init {
		if(!dataPath.canRead()) {
			"JMP does not have read access to the data directory, this will cause problems [dir: ${dataPath.absolutePath}]"
		}
		if(!dataPath.canRead()) {
			"JMP does not have write access to the data directory, this will cause problems [dir: ${dataPath.absolutePath}]"
		}
	}

	private fun allocate(name: String): File {
		"Received request for home data allocation: $name".logv(javaClass)
		val dir = allocations[name] ?: File(dataPath, name)
		if(allocations[name] != null)
			"Found existing allocation for $name, we will use that".logw(javaClass)
		allocations[name] = dir
		return dir
	}

	fun get(name: String): File? {
		val dir = allocate(name)

		if(dir.exists() && !dir.isFile)
			return null
		return dir
	}
	fun getDirectory(name: String): File? {
		val dir = allocate(name)

		if(dir.exists() && !dir.isDirectory)
			return null
		return dir
	}
}

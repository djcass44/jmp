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

package dev.castive.jmp.db

import dev.castive.jmp.config.DataConfig
import dev.castive.log2.loge
import dev.castive.log2.logi
import dev.castive.log2.logok
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import java.io.File

/**
 * Base class for running database tests
 * This class will handle overriding environment and SQLite file creation and destruction
 * It will also run the schema bootstrapper to create default data
 *
 * Uses SQLite because H2 fails with a Syntax error
 */
open class DatabaseTest {
	private val dbUrl = "jdbc:sqlite:/tmp/jmp-${System.currentTimeMillis()}.db"
	private val dbClass = "org.sqlite.JDBC"

	/**
	 * Creates a new database connection and starts initial bootstrapper
	 */
	@BeforeEach
	fun setUp() {
		"Test context [url: $dbUrl, class: $dbClass]".logi(javaClass)
		val config = DataConfig(
			dbUrl,
			dbClass
		)
		// bootstrap the database
		val helper = DatabaseHelper(config)
		helper.start()
	}

	/**
	 * Attempts to delete the SQLite file
	 */
	@AfterEach
	fun tearDown() {
		kotlin.runCatching {
			val db = File(dbUrl.split(":")[2])
			if(db.exists())
				db.delete()
		}.onFailure {
			"Unable to cleanup test database: $dbUrl".loge(javaClass)
		}.onSuccess {
			"Deleted test database: $dbUrl".logok(javaClass)
		}
	}
}

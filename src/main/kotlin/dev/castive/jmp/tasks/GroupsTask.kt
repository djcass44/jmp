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

package dev.castive.jmp.tasks

import dev.castive.jmp.db.dao.Group
import dev.castive.jmp.db.dao.User
import dev.castive.jmp.util.add
import dev.castive.log2.loga
import dev.castive.log2.logi
import dev.castive.log2.logv
import dev.castive.log2.logw
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.timer
import kotlin.system.measureTimeMillis

object GroupsTask: Task {
	private const val delay = 60_000L
	// use an atomic value to reduce weird async behaviour
	private val running = AtomicBoolean(false)
	/**
	 * Request that the task performs an update
	 * This will not always be actioned and serves as more of a recommendation
	 */
	fun update() {
		run()
	}

	override fun start() {
		// start the task
		"Starting timer: ${javaClass.name}".logi(javaClass)
		timer(javaClass.name, true, 0, delay) {
			GroupsTask::run.invoke()
		}
	}

	override fun run() {
		if(running.get()) {
			"Unable to action ${javaClass.simpleName} as there is already one running".loga(javaClass)
			return
		}
		running.set(true)
		"Starting ${javaClass.name} at ${System.currentTimeMillis()}".logv(javaClass)
		"Scanned group relations in ${measureTimeMillis {
			transaction {
				Group.all().forEach {
					// public groups take precedence over default groups
					if (it.public)
						addUsersToPublicGroups(it)
					else if (it.defaultFor != null)
						addUsersToDefaultGroup(it)
				}
			}
		}} ms".logv(javaClass)
		running.set(false)
	}

	/**
	 * Add all users to the group, assuming that it has been marked as public
	 * @param group: the group to add users to. Must be public
	 */
	private fun addUsersToPublicGroups(group: Group) {
		// the group MUST be public
		if(!group.public) {
			"Blocked call of 'addUsersToPublicGroups' to non-public group".logw(javaClass)
			return
		}
		var count = 0
		User.all().forEach {
			// if the user isn't in the group, add them
			if(!group.users.contains(it)) {
				group.users = group.users.add(it)
				count++
			}
		}
		"Added $count users to public group: ${group.name}".logi(javaClass)
	}

	/**
	 * Add all users from a specific source to the group
	 * @param group: the group to add relevant users to
	 */
	private fun addUsersToDefaultGroup(group: Group) {
		// the group MUST have a 'defaultFor' value set
		if(group.defaultFor == null) {
			"Blocked call of 'addUsersToDefaultGroup' to non-default group".logw(javaClass)
			return
		}
		var count = 0
		User.all().forEach {
			if(it.from == group.defaultFor && !group.users.contains(it)) {
				// if the user isn't in the group, add them
				group.users = group.users.add(it)
				count++
			}
		}
		"Added $count users from ${group.defaultFor} to default group: ${group.name}".logi(javaClass)
	}
}
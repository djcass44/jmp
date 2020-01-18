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

import dev.castive.jmp.entity.Group
import dev.castive.jmp.repo.GroupRepo
import dev.castive.jmp.repo.UserRepo
import dev.castive.log2.loga
import dev.castive.log2.logv
import dev.castive.log2.logw
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.atomic.AtomicBoolean
import javax.transaction.Transactional
import kotlin.system.measureTimeMillis

@Transactional
@Component
class GroupsTask @Autowired constructor(
	private val groupRepo: GroupRepo,
	private val userRepo: UserRepo
) {
	// use an atomic value to reduce weird async behaviour
	private val running = AtomicBoolean(false)

	@Scheduled(fixedDelay = 60 * 1000)
	fun run() {
		if(running.get()) {
			"Unable to action ${javaClass.simpleName} as there is already one running".loga(javaClass)
			return
		}
		running.set(true)
		"Starting ${javaClass.name} at ${System.currentTimeMillis()}".logv(javaClass)
		"Scanned group relations in ${measureTimeMillis {
			groupRepo.findAll().forEach {
				when {
					it.public -> addUsersToPublicGroups(it)
					it.defaultFor != null -> addUsersToDefaultGroup(it)
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
		userRepo.findAll().forEach {
			// if the user isn't in the group, add them
			if(!group.users.contains(it)) {
				group.users.add(it)
				count++
			}
		}
		groupRepo.save(group)
		"Added $count users to public group: ${group.name}".logv(javaClass)
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
		userRepo.findAll().forEach {
			if(it.source == group.defaultFor && !group.users.contains(it)) {
				// if the user isn't in the group, add them
				group.users.add(it)
				count++
			}
		}
		groupRepo.save(group)
		"Added $count users from ${group.defaultFor} to default group: ${group.name}".logv(javaClass)
	}
}

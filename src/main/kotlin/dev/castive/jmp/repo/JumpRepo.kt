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

package dev.castive.jmp.repo

import dev.castive.jmp.entity.Group
import dev.castive.jmp.entity.Jump
import dev.castive.jmp.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
interface JumpRepo: JpaRepository<Jump, Int> {
	fun findAllByLocation(location: String): List<Jump>
	fun findAllByOwnerGroup(ownerGroup: Group): List<Jump>
	fun findAllByOwnerIsNullAndOwnerGroupIsNull(): List<Jump>
	fun findAllByOwner(owner: User): List<Jump>

	@Modifying
	@Transactional
	@Query("UPDATE Jump SET image = :icon WHERE location = :address")
	fun updateIconWithAddress(address: String, icon: String)

	@Modifying
	@Transactional
	@Query("UPDATE Jump SET title = :title WHERE location = :address")
	fun updateTitleWithAddress(address: String, title: String)
}

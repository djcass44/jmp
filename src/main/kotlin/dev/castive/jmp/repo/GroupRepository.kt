package dev.castive.jmp.repo

import dev.castive.jmp.entity.Group
import dev.dcas.jmp.spring.security.model.entity.GroupEntity
import dev.dcas.jmp.spring.security.model.repo.GroupRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Repository
import java.util.*

@Repository
class GroupRepository @Autowired constructor(
	private val groupRepo: GroupRepo
): GroupRepository {
	override fun create(name: String, source: String, defaultFor: String?) {
		groupRepo.save(Group(
			UUID.randomUUID(),
			name,
			source,
			false,
			defaultFor,
			mutableSetOf()
		))
	}

	override fun findFirstByName(name: String): GroupEntity? = groupRepo.findFirstByName(name)
}

package dev.castive.jmp.service

import dev.castive.jmp.data.JumpDTO
import dev.castive.jmp.entity.Jump
import dev.castive.jmp.repo.AliasRepo
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class OwnerService @Autowired constructor(
	private val aliasRepo: AliasRepo
) {

	fun getDTO(jump: Jump): JumpDTO = JumpDTO(
		jump,
		jump.owner,
		jump.ownerGroup,
		aliasRepo.findAllByParent(jump.id).map { it.asDTO() }
	)
}

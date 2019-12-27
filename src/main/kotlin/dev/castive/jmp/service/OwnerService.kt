package dev.castive.jmp.service

import dev.castive.jmp.data.JumpDTO
import dev.castive.jmp.entity.Jump
import dev.castive.jmp.repo.AliasRepo
import dev.castive.jmp.repo.GroupRepo
import dev.castive.jmp.repo.UserRepo
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service

@Service
class OwnerService @Autowired constructor(
	private val userRepo: UserRepo,
	private val groupRepo: GroupRepo,
	private val aliasRepo: AliasRepo
) {

	fun getDTO(jump: Jump): JumpDTO = JumpDTO(
		jump,
		jump.owner?.let {userRepo.findByIdOrNull(jump.owner)},
		jump.ownerGroup?.let {groupRepo.findByIdOrNull(jump.ownerGroup)},
		aliasRepo.findAllByParent(jump.id)
	)
}

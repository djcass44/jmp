package dev.castive.jmp.service

import dev.castive.jmp.data.JumpDTO
import dev.castive.jmp.entity.Jump
import dev.castive.jmp.prop.AppMetadataProps
import dev.castive.jmp.repo.AliasRepo
import dev.dcas.util.extend.safe
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class OwnerService @Autowired constructor(
	private val aliasRepo: AliasRepo,
	private val metadataProps: AppMetadataProps
) {

	fun getDTO(jump: Jump): JumpDTO = JumpDTO(
		jump,
		"${metadataProps.icon.url}/icon?site=${jump.location.safe()}",
		jump.owner,
		jump.ownerGroup,
		aliasRepo.findAllByParent(jump.id).map { it.asDTO() }
	)
}

package dev.castive.jmp.data

import dev.castive.jmp.data.dto.AliasDTO
import dev.castive.jmp.entity.Jump
import dev.dcas.jmp.security.shim.entity.Group
import dev.dcas.jmp.security.shim.entity.Meta
import dev.dcas.jmp.security.shim.entity.User

data class JumpDTO(
	val id: Int,
	val name: String,
	val location: String,
	val title: String? = null,
	val owner: User? = null,
	val ownerGroup: Group? = null,
	val image: String? = null,
	val meta: Meta,
	val usage: Int = 0,
	val public: Boolean,
	val alias: List<AliasDTO>
) {
	constructor(jump: Jump, imageUrl: String, owner: User?, ownerGroup: Group?, alias: List<AliasDTO>): this(
		jump.id,
		jump.name,
		jump.location,
		jump.title,
		owner,
		ownerGroup,
		imageUrl,
		jump.meta, jump.usage,
		jump.isPublic(),
		alias
	)
}

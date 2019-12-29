package dev.castive.jmp.data

import dev.castive.jmp.data.dto.AliasDTO
import dev.castive.jmp.entity.Group
import dev.castive.jmp.entity.Jump
import dev.castive.jmp.entity.Meta
import dev.castive.jmp.entity.User

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
	constructor(jump: Jump, owner: User?, ownerGroup: Group?, alias: List<AliasDTO>): this(
		jump.id,
		jump.name,
		jump.location,
		jump.title,
		owner,
		ownerGroup,
		jump.image,
		jump.meta, jump.usage,
		jump.isPublic(),
		alias
	)
}

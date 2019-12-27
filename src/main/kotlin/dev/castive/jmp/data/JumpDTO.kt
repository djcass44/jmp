package dev.castive.jmp.data

import dev.castive.jmp.entity.*

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
	val isPublic: Boolean,
	val alias: List<Alias>
) {
	constructor(jump: Jump, owner: User?, ownerGroup: Group?, alias: List<Alias>): this(
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

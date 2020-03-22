/*
 *    Copyright 2020 Django Cass
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

package dev.castive.jmp.data.dto

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

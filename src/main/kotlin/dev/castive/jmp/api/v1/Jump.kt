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

package dev.castive.jmp.api.v1

import dev.castive.eventlog.EventLog
import dev.castive.eventlog.schema.Event
import dev.castive.eventlog.schema.EventType
import dev.castive.javalin_auth.auth.Roles.BasicRoles
import dev.castive.jmp.Runner
import dev.castive.jmp.api.App
import dev.castive.jmp.api.Auth
import dev.castive.jmp.api.Socket
import dev.castive.jmp.api.actions.ImageAction
import dev.castive.jmp.api.actions.OwnerAction
import dev.castive.jmp.api.actions.TitleAction
import dev.castive.jmp.db.dao.*
import dev.castive.jmp.db.dao.Jump
import dev.castive.jmp.util.*
import dev.castive.log2.Log
import dev.castive.log2.logi
import io.javalin.apibuilder.ApiBuilder.*
import io.javalin.apibuilder.EndpointGroup
import io.javalin.http.BadRequestResponse
import io.javalin.http.ConflictResponse
import io.javalin.http.ForbiddenResponse
import io.javalin.http.NotFoundResponse
import org.eclipse.jetty.http.HttpStatus
import org.jetbrains.exposed.sql.transactions.transaction

class Jump(private val ws: (tag: String, data: Any) -> (Unit)): EndpointGroup {
	private val caseSensitive = EnvUtil.getEnv(EnvUtil.CASE_SENSITIVE, "false").toBoolean()
	private val imageAction = ImageAction(ws)
	private val titleAction = TitleAction(ws)

	data class JumpResponse(val found: Boolean = true, val location: String)

	private fun jumpExists(add: JumpData, user: User?): Boolean {
		return transaction {
			/**
			 * Get Jumps for user
			 * Return TRUE if both name & location matches any
			 */
			val existing = OwnerAction.getJumpFromUser(user, add.name, caseSensitive)
			existing.forEach {
				if(it.name.equals(add.name, ignoreCase = !caseSensitive) && it.location == add.location)
					return@transaction true
			}
			return@transaction false
		}
	}

	override fun addEndpoints() {
		// List all items in Json format
		get("${Runner.BASE}/v1/jumps", { ctx ->
			// get the jumps visible to the user
			val jumps = OwnerAction.getUserVisibleJumps(ctx.user())
			// return in a presentable format
			ctx.ok().json(transaction {
				jumps.map { JumpData(it) }
			})
		}, Auth.openAccessRole)
		get("${Runner.BASE}/v2/jump/:target", { ctx ->
			val target = if(caseSensitive) ctx.pathParam("target") else ctx.pathParam("target").toLowerCase()
			val id = ctx.queryParam("id", Int::class.java).getOrNull()
			if(target.isBlank() && id == null) {
				"Received null or empty target request from ${ctx.ip()}".logi(javaClass)
				throw BadRequestResponse("Empty or null target: $target, id: $id")
			}
			val user = ctx.user()
			val jumps = if(id != null) {
				"Got request for specific jump: $id".logi(javaClass)
				OwnerAction.getJumpById(user, id)
			}
			else OwnerAction.getJumpFromUser(user, target, caseSensitive)
			"Got ${jumps.size} jumps as a response to $target, $id".logi(javaClass)
			val res = run {
				val found = jumps.size == 1
				return@run if(found) {
					transaction { jumps[0].metaUsage++ }
					true to jumps[0].location
				}
				else false to "/similar?query=$target"
			}
			ctx.ok().json(
				JumpResponse(res.first, res.second)
			)
		}, Auth.openAccessRole)
		// Add a jump point
		put("${Runner.BASE}/v1/jump", { ctx ->
			val add = ctx.bodyAsClass(JumpData::class.java)
			val groupID = (ctx.queryParam<String>("gid").getOrNull() ?: "").toUUID()
			val user: User = ctx.assertUser()
			// Block non-admin user from adding global jumps
			if (add.personal == JumpData.TYPE_GLOBAL && !App.auth.isAdmin(user)) throw ForbiddenResponse("Only an admin can create global Jumps")
			if (!jumpExists(add, user)) {
				transaction {
					val group = if(groupID != null) Group.findById(groupID) else null
					val jump = Jump.new {
						name = add.name
						location = add.location
						owner = if (add.personal == JumpData.TYPE_PERSONAL) user else null
						ownerGroup = group
						metaCreation = System.currentTimeMillis()
						metaUpdate = System.currentTimeMillis()
					}
					// Create aliases for newly added Jump
					add.alias.forEach {
						Alias.new {
							name = it.name
							parent = jump
						}
					}
					EventLog.post(Event(type = EventType.CREATE, resource = JumpData::class.java, causedBy = Jump::class.java))
					imageAction.get(add.location)
					titleAction.get(add.location)
				}
				ws.invoke(Socket.EVENT_UPDATE, Socket.EVENT_UPDATE)
				ctx.status(HttpStatus.CREATED_201).json(add)
			}
			else {
				EventLog.post(Event(type = "CONFLICT", resource = JumpData::class.java, causedBy = javaClass))
				throw ConflictResponse("${add.name} already exists!")
			}
		}, Auth.openAccessRole)
		// Edit a jump point
		patch("${Runner.BASE}/v1/jump", { ctx ->
			val update = ctx.bodyAsClass(EditJumpData::class.java)
			val user = ctx.assertUser()
			transaction {
				val existing = Jump.findById(update.id) ?: throw NotFoundResponse()

				// User can change personal jumps
				if(existing.owner == user || user.role.isEqual(BasicRoles.ADMIN) ||
					(OwnerAction.getJumpById(user, existing.id.value).isNotEmpty() && !OwnerAction.isPublic(existing))) {
					existing.apply {
						name = update.name
						location = update.location
						metaUpdate = System.currentTimeMillis()
					}
					EventLog.post(Event(type = EventType.UPDATE, resource = JumpData::class.java, causedBy = javaClass))
					// Add aliases
					val updated = arrayListOf<Int>()
					update.alias.forEach {
						val alias = Alias.findById(it.id)
						if(it.id == 0 || alias == null) {
							// Create the alias if it doesn't exist
							val a = Alias.new {
								name = it.name
								parent = existing
							}
							updated.add(a.id.value)
						}
						// Update the alias name (needs investigation into ability to abuse)
						else {
							alias.name = it.name
							updated.add(alias.id.value)
						}
					}
					// Remove any dangling aliases
					val matches = Alias.find { Aliases.parent eq existing.id }
					var gc = 0
					matches.forEach {
						if(!updated.contains(it.id.value)) {
							it.delete()
							gc++
						}
					}
					Log.a(javaClass, "GC cleaned up $gc orphaned aliases for ${existing.id.value}, ${existing.name}")
					imageAction.get(update.location)
					titleAction.get(update.location)
					ws.invoke(Socket.EVENT_UPDATE, Socket.EVENT_UPDATE)
					ctx.status(HttpStatus.NO_CONTENT_204).json(update)
				}
				else throw ForbiddenResponse()
			}
		}, Auth.defaultRoleAccess)
		// Delete a jump point
		delete("${Runner.BASE}/v1/jump/:id", { ctx ->
			val id = ctx.pathParam<Int>("id").getOrNull() ?: throw BadRequestResponse()
			val user: User = ctx.assertUser()
			transaction {
				val result = Jump.findById(id) ?: throw NotFoundResponse()
				// 403 if jump is global and user ISN'T admin
				if (result.owner == null && user.role.name != BasicRoles.ADMIN.name) throw ForbiddenResponse()
				// 403 if jump is personal and tokens don't match
				if (result.owner != null && result.owner!!.id != user.id) throw ForbiddenResponse()
				// Delete all aliased children so that they don't become orphans
				Alias.find { Aliases.parent eq result.id }.forEach { it.delete() }
				result.delete()
				EventLog.post(Event(type = EventType.DESTROY, resource = JumpData::class.java, causedBy = javaClass))
			}
			ws.invoke(Socket.EVENT_UPDATE, Socket.EVENT_UPDATE)
			ctx.status(HttpStatus.NO_CONTENT_204)
		}, Auth.defaultRoleAccess)
	}
}

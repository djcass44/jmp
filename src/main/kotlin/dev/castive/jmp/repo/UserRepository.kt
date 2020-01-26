package dev.castive.jmp.repo

import dev.castive.jmp.entity.Meta
import dev.castive.jmp.entity.Role
import dev.castive.jmp.entity.User
import dev.dcas.jmp.spring.security.model.UserProjection
import dev.dcas.jmp.spring.security.model.entity.UserEntity
import dev.dcas.jmp.spring.security.model.repo.UserRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Repository
import java.util.*

@Repository
class UserRepository @Autowired constructor(
	private val userRepo: UserRepo,
	private val metaRepo: MetaRepo
): UserRepository {
	override fun createWithData(source: String, username: String, data: UserProjection): UserEntity {
		val id = UUID.randomUUID()
		val meta = metaRepo.save(Meta.fromUser(id))
		return userRepo.save(User(
			id,
			username,
			data.displayName,
			data.avatarUrl,
			null,
			mutableListOf(Role.ROLE_USER),
			meta,
			source
		))
	}

	override fun existsByUsername(username: String): Boolean = userRepo.existsByUsername(username)

	override fun findFirstByUsername(username: String): UserEntity? = userRepo.findFirstByUsername(username)

	override fun findFirstByUsernameAndSource(username: String, source: String): UserEntity? = userRepo.findFirstByUsernameAndSource(username, source)

	override fun update(username: String, data: UserProjection): UserEntity {
		userRepo.findFirstByUsername(username)?.let {
			return userRepo.save(it.apply {
				displayName = data.displayName
				avatarUrl = data.avatarUrl
			})
		}
		error("Failed to locate user with username: $username")
	}
}

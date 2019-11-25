package dev.castive.jmp.api

import dev.castive.jmp.db.DatabaseTest
import dev.castive.jmp.db.dao.Users
import dev.castive.jmp.db.repo.existsByUsername
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AuthTest {
	private val dummyText = "This is a test!"

	@Test
	fun `identical hashes return true`() {
		val auth = Auth()
		val hash = auth.computeHash(dummyText.toCharArray())
		assertTrue(auth.hashMatches(dummyText.toCharArray(), hash))
	}
	@Test
	fun `different hashes return false`() {
		val auth = Auth()
		val hash = auth.computeHash(dummyText.toCharArray())
		assertFalse(auth.hashMatches("This is a test".toCharArray(), hash))
	}
}
class AuthUserTest: DatabaseTest() {
	private val auth = Auth()

	@Test
	fun `can create standard user`() {
		val username = "user1"
		val name = "John Smith"
		val password = "hunter2"
		val user = auth.createUser(username, password.toCharArray(), false, name)

		// user exists
		assertTrue(Users.existsByUsername(username))
		// user role is NOT admin
		assertFalse(auth.isAdmin(user))
		// user password hash matches `password` defined above
		assertTrue(auth.hashMatches(password.toCharArray(), user.hash))
	}

	@Test
	fun `can create admin user`() {
		val username = "user1"
		val name = "John Smith"
		val password = "hunter2"
		val user = auth.createUser(username, password.toCharArray(), true, name)

		// user exists
		assertTrue(Users.existsByUsername(username))
		// user role is admin
		assertTrue(auth.isAdmin(user))
		// user password hash matches `password` defined above
		assertTrue(auth.hashMatches(password.toCharArray(), user.hash))
	}
}

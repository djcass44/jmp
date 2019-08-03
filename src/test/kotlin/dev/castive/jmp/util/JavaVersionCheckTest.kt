package dev.castive.jmp.util

import dev.castive.jmp.util.checks.JavaVersionCheck
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@Suppress("unused")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JavaVersionCheckTest {
	companion object {
		@JvmStatic
		fun getVersions(): Stream<Arguments> = Stream.of(
			Arguments.of("12", 12),
			Arguments.of("11", 11),
			Arguments.of("10", 10),
			Arguments.of("9", 9),
			Arguments.of("1.8", 8),
			Arguments.of("1.7", 7)
		)
	}

	@ParameterizedTest
	@MethodSource("getVersions")
	fun `check valid java version`(data: String, expected: Int) {
		val version = JavaVersionCheck().getVersion(data)
		assertNotNull(version)
		assertEquals(expected, version)
	}
}
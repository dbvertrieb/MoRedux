package de.db.moredux.observation

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

class NotificationGuardTest {

    @Nested
    inner class AlwaysNotifyTest {

        private val sut = NotificationGuard.AlwaysNotify<Any?>()

        @Test
        fun `test shouldNotify always returns true`() {
            // Given
            val value = "someValue"

            // When
            val result = sut.shouldNotify(value)

            // Then
            assertThat(result).isTrue()
        }

        @Test
        fun `test shouldNotify returns true for null`() {
            // Given
            val value = null

            // When
            val result = sut.shouldNotify(value)

            // Then
            assertThat(result).isTrue()
        }

        @Test
        fun `test shouldNotify returns true for repeated equal value`() {
            // Given
            val value = "sameValue"

            // When
            sut.shouldNotify(value)
            val result = sut.shouldNotify(value)

            // Then
            assertThat(result).isTrue()
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class NoDuplicatesTest {

        private val sut = NotificationGuard.NoDuplicates<String?>()

        @Test
        fun `test shouldNotify returns true on first call`() {
            // Given
            val value = "firstValue"

            // When
            val result = sut.shouldNotify(value)

            // Then
            assertThat(result).isTrue()
        }

        @Test
        fun `test shouldNotify returns false for same value`() {
            // Given
            val value = "sameValue"
            sut.shouldNotify(value)

            // When
            val result = sut.shouldNotify(value)

            // Then
            assertThat(result).isFalse()
        }

        @Test
        fun `test shouldNotify returns true for changed value`() {
            // Given
            sut.shouldNotify("firstValue")

            // When
            val result = sut.shouldNotify("secondValue")

            // Then
            assertThat(result).isTrue()
        }

        @Test
        fun `test shouldNotify returns true when value changes to null`() {
            // Given
            sut.shouldNotify("someValue")

            // When
            val result = sut.shouldNotify(null)

            // Then
            assertThat(result).isTrue()
        }

        @Test
        fun `test shouldNotify returns true when value changes from null`() {
            // Given
            sut.shouldNotify(null)

            // When
            val result = sut.shouldNotify("someValue")

            // Then
            assertThat(result).isTrue()
        }

        @Test
        fun `test shouldNotify returns false for repeated null`() {
            // Given
            sut.shouldNotify(null)

            // When
            val result = sut.shouldNotify(null)

            // Then
            assertThat(result).isFalse()
        }

        @ParameterizedTest
        @MethodSource("valueSequences")
        fun `test shouldNotify detects changes in a sequence`(
            given: List<String?>,
            expected: List<Boolean>
        ) {
            // Given
            val results = mutableListOf<Boolean>()

            // When
            given.forEach { value -> results.add(sut.shouldNotify(value)) }

            // Then
            assertThat(results).isEqualTo(expected)
        }

        fun valueSequences(): List<Arguments> = listOf(
            Arguments.of(
                listOf("a", "a", "b", "b", "a"),
                listOf(true, false, true, false, true)
            ),
            Arguments.of(
                listOf(null, null, "x", null),
                listOf(true, false, true, true)
            ),
            Arguments.of(
                listOf("only"),
                listOf(true)
            )
        )
    }
}

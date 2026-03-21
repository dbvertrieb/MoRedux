/*
 * Copyright 2024, DB Vertrieb GmbH.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.db.moredux.observation

import com.google.common.truth.Truth.assertThat
import de.db.moredux.State
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class SelectorToStateFlowTest {

    private fun createSut(notificationGuard: NotificationGuard<String>): SelectorToStateFlow<SelectorState, String> =
        object : SelectorToStateFlow<SelectorState, String>(MutableStateFlow("INITIAL VALUE"), notificationGuard) {
            override fun map(state: SelectorState): String = state.bla?.uppercase().orEmpty()
        }

    @Nested
    inner class ObserveAndRemoveTest {

        @Test
        fun `test observeSelector notifies observer on state change`() {
            // Given
            val sut = createSut(NotificationGuard.AlwaysAllow())
            var observed = ""
            sut.observeSelector { value -> observed = value }

            // When
            sut.onStateChanged(SelectorState("new value"))

            // Then
            assertThat(observed).isEqualTo("NEW VALUE")
        }

        @Test
        fun `test removeAllSelectorObservers stops notifications`() {
            // Given
            val sut = createSut(NotificationGuard.AlwaysAllow())
            var observed = ""
            sut.observeSelector { value -> observed = value }
            sut.onStateChanged(SelectorState("new value"))

            // When
            sut.removeAllSelectorObservers()
            sut.onStateChanged(SelectorState("very new value"))

            // Then
            assertThat(observed).isEqualTo("NEW VALUE")
        }

        @Test
        fun `test onStateChanged updates StateFlow value`() {
            // Given
            val sut = createSut(NotificationGuard.AlwaysAllow())

            // When
            sut.onStateChanged(SelectorState("new value"))

            // Then
            assertThat(sut.value).isEqualTo("NEW VALUE")
        }
    }

    @Nested
    inner class NoDuplicatesGuardTest {

        @Test
        fun `test onStateChanged does not notify or update StateFlow for equal mapped value`() {
            // Given
            val sut = createSut(NotificationGuard.NoDuplicates())
            val observed = mutableListOf<String>()
            sut.observeSelector { value -> observed.add(value) }

            // When
            sut.onStateChanged(SelectorState("same"))
            sut.onStateChanged(SelectorState("same"))

            // Then
            assertThat(observed).hasSize(1)
            assertThat(sut.value).isEqualTo("SAME")
        }

        @Test
        fun `test onStateChanged notifies and updates StateFlow for different mapped values`() {
            // Given
            val sut = createSut(NotificationGuard.NoDuplicates())
            val observed = mutableListOf<String>()
            sut.observeSelector { value -> observed.add(value) }

            // When
            sut.onStateChanged(SelectorState("first"))
            sut.onStateChanged(SelectorState("second"))

            // Then
            assertThat(observed).containsExactly("FIRST", "SECOND").inOrder()
            assertThat(sut.value).isEqualTo("SECOND")
        }
    }

    @Nested
    inner class AlwaysAllowGuardTest {

        @Test
        fun `test onStateChanged always notifies and updates StateFlow even for equal mapped value`() {
            // Given
            val sut = createSut(NotificationGuard.AlwaysAllow())
            val observed = mutableListOf<String>()
            sut.observeSelector { value -> observed.add(value) }

            // When
            sut.onStateChanged(SelectorState("same"))
            sut.onStateChanged(SelectorState("same"))

            // Then
            assertThat(observed).hasSize(2)
            assertThat(observed).containsExactly("SAME", "SAME").inOrder()
            assertThat(sut.value).isEqualTo("SAME")
        }
    }

    // region helpers

    data class SelectorState(val bla: String? = null) : State {
        override fun clone(): State = this.copy()
    }

    // endregion
}
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
import org.junit.Test

class SelectorToStateFlowTest {

    private val sut = object : SelectorToStateFlow<SelectorState, String>(MutableStateFlow("INITIAL VALUE")) {
        override fun map(state: SelectorState): String = state.bla?.uppercase().orEmpty()
    }

    @Test
    fun `test observeSelector and removeAllSelectorObservers`() {
        // Given
        var observed = ""
        sut.observeSelector { value -> observed = value }

        // When
        sut.onStateChanged(SelectorState("new value"))

        // Then
        assertThat(observed).isEqualTo("NEW VALUE")

        // When
        sut.removeAllSelectorObservers()
        sut.onStateChanged(SelectorState("very new value"))

        // Then
        assertThat(observed).isEqualTo("NEW VALUE")
    }

    @Test
    fun `test StateFlow updates`() {

        // When
        sut.onStateChanged(SelectorState("new value"))

        // Then
        assertThat(sut).isInstanceOf(MutableStateFlow::class.java)
        assertThat(sut.value).isEqualTo("NEW VALUE")
    }

    data class SelectorState(val bla: String? = null) : State {
        override fun clone(): State = this.copy()
    }
}
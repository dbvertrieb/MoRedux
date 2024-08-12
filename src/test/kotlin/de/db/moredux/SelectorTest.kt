/*
 * Copyright 2023 the original author or authors.
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

package de.db.moredux

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SelectorTest {

    private val sut = object : Selector<SelectorState, String>() {
        override fun map(state: SelectorState): String = state.bla?.uppercase().orEmpty()
    }

    @Test
    fun `test observeSelector and removeAllSelectorObservers`() {
        // Given
        var observed = ""
        sut.observeSelector { value -> observed = value }

        // When
        sut.onStateChanged(SelectorState("neuer wert"))

        // Then
        assertThat(observed).isEqualTo("NEUER WERT")

        // When
        sut.removeAllSelectorObservers()
        sut.onStateChanged(SelectorState("noch neuerer wert"))

        // Then
        assertThat(observed).isEqualTo("NEUER WERT")
    }

    data class SelectorState(val bla: String? = null) : State {
        override fun clone(): State = this.copy()
    }
}
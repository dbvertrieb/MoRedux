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

package de.db.moredux

import com.google.common.truth.Truth.assertThat
import de.db.moredux.store.Dispatcher
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock

class EffectTest {
    @Test
    fun `test consuming execute`() {
        // Given
        var actualEffectProperty = "some random String"
        val sut = Effect<EffectState> { state, dispatcher -> actualEffectProperty = state.effectProperty }
        val dispatcher: Dispatcher = mock()

        // Pre-Then
        assertThat(sut.isConsumed).isFalse()

        // When
        sut.execute(EffectState("effect parameter value"), dispatcher)

        // Then
        assertThat(sut.isConsumed).isTrue()
        assertThat(actualEffectProperty).isEqualTo("effect parameter value")
    }

    // Basic state implementation for testing
    data class EffectState(val effectProperty: String) : State {
        override fun clone(): State = this.copy()
    }
}
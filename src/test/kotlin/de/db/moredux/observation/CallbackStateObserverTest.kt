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

package de.db.moredux.observation

import com.google.common.truth.Truth.assertThat
import de.db.moredux.State
import org.junit.jupiter.api.Test

class CallbackStateObserverTest {
    @Test
    fun `test onStateChanged`() {
        // Given
        var actualStatePropertyValue = "Some random inital value"
        val sut = CallbackStateObserver<CallbackStateObserverState> { state ->
            actualStatePropertyValue = state.dummyStateProperty
        }

        // When
        sut.onStateChanged(CallbackStateObserverState(dummyStateProperty = "some string"))

        // Then
        assertThat(actualStatePropertyValue).isEqualTo("some string")
    }

    // Basic state implementation for testing
    data class CallbackStateObserverState(val dummyStateProperty: String) : State {
        override fun clone(): State = this.copy()
    }
}
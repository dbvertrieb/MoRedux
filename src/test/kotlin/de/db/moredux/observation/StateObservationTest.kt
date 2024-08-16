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
import de.db.moredux.store.Store
import org.junit.jupiter.api.Test

class StateObservationTest {

    private val store: Store<StateObservationState> = Store.Builder<StateObservationState>()
        .withInitialState(StateObservationState())
        .build()

    @Test
    fun `test addStateObserver as StateObserver instance and onStateChanged`() {
        // Given
        val callbackState = mutableListOf<StateObservationState>()
        val stateObserver = object : StateObserver<StateObservationState> {
            override fun onStateChanged(state: StateObservationState) {
                callbackState.add(state)
            }
        }

        // When
        store.addStateObserver(processCurrentStateImmediately = false, stateObserver = stateObserver)
        store.observationManager.onStateChanged(
            currentDispatchCount = 0,
            state = StateObservationState(bla = "State counter 0")
        )

        // Then
        assertThat(callbackState).hasSize(1)
        assertThat(callbackState[0]).isEqualTo(StateObservationState(bla = "State counter 0"))
    }

    @Test
    fun `test addStateObserver as StateObserver instance with processCurrentStateImmediately`() {
        // Given
        val callbackState = mutableListOf<StateObservationState>()
        val stateObserver = object : StateObserver<StateObservationState> {
            override fun onStateChanged(state: StateObservationState) {
                callbackState.add(state)
            }
        }

        // When
        store.addStateObserver(processCurrentStateImmediately = true, stateObserver = stateObserver)
        store.observationManager.onStateChanged(
            currentDispatchCount = 0,
            state = StateObservationState(bla = "State counter 0")
        )

        // Then
        assertThat(callbackState).hasSize(2)
        assertThat(callbackState[0]).isEqualTo(StateObservationState(bla = null))
        assertThat(callbackState[1]).isEqualTo(StateObservationState(bla = "State counter 0"))
    }

    @Test
    fun `test addStateObserver as callback and onStateChanged`() {
        // Given
        val callbackState = mutableListOf<StateObservationState>()

        // When
        store.addStateObserver { state -> callbackState.add(state) }
        store.observationManager.onStateChanged(
            currentDispatchCount = 0,
            state = StateObservationState(bla = "State counter 0")
        )

        // Then
        assertThat(callbackState).hasSize(1)
        assertThat(callbackState[0]).isEqualTo(StateObservationState(bla = "State counter 0"))
    }

    @Test
    fun `test addStateObserver as callback with processCurrentStateImmediately`() {
        // Given
        val callbackState = mutableListOf<StateObservationState>()

        // When
        store.addStateObserver(processCurrentStateImmediately = true) { state -> callbackState.add(state) }
        store.observationManager.onStateChanged(
            currentDispatchCount = 0,
            state = StateObservationState(bla = "State counter 0")
        )

        // Then
        assertThat(callbackState).hasSize(2)
        assertThat(callbackState[0]).isEqualTo(StateObservationState(bla = null))
        assertThat(callbackState[1]).isEqualTo(StateObservationState(bla = "State counter 0"))
    }

    @Test
    fun `test multiple addStateObserver`() {
        // Given
        val callbackState = mutableListOf<StateObservationState>()

        // When
        store.addStateObserver(processCurrentStateImmediately = true) { state -> callbackState.add(state) }
        store.addStateObserver(processCurrentStateImmediately = true) { state -> callbackState.add(state) }
        store.observationManager.onStateChanged(
            currentDispatchCount = 0,
            state = StateObservationState(bla = "State counter 0")
        )

        // Then
        assertThat(callbackState).hasSize(4)
        assertThat(callbackState[0]).isEqualTo(StateObservationState(bla = null))
        assertThat(callbackState[1]).isEqualTo(StateObservationState(bla = null))
        assertThat(callbackState[2]).isEqualTo(StateObservationState(bla = "State counter 0"))
        assertThat(callbackState[3]).isEqualTo(StateObservationState(bla = "State counter 0"))
    }

    data class StateObservationState(val bla: String? = null) : State {
        override fun clone(): State = this.copy()
    }
}
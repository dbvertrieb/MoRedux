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
import de.db.moredux.store.Store
import org.junit.jupiter.api.Test

class StateObservationTest {

    private val store: Store<StateObservationState> = Store.Builder<StateObservationState>()
        .withInitialState(StateObservationState())
        .build()

    @Test
    fun `test removeObserver`() {
        // Given
        val callbackState1 = mutableListOf<StateObservationState>()
        val callbackState2 = mutableListOf<StateObservationState>()

        // When
        val stateObserver1 =
            store.addStateObserver(processCurrentStateImmediately = true) { state -> callbackState1.add(state) }
        store.addStateObserver(processCurrentStateImmediately = true) { state -> callbackState2.add(state) }
        store.observationManager.onStateChanged(
            currentDispatchCount = 0,
            state = StateObservationState(bla = "State counter 0")
        )
        store.removeObserver(stateObserver1)
        store.observationManager.onStateChanged(
            currentDispatchCount = 1,
            state = StateObservationState(bla = "State counter 1")
        )

        // Then
        assertThat(callbackState1).hasSize(2)
        assertThat(callbackState1[0]).isEqualTo(StateObservationState(bla = null))
        assertThat(callbackState1[1]).isEqualTo(StateObservationState(bla = "State counter 0"))
        assertThat(callbackState2).hasSize(3)
        assertThat(callbackState2[0]).isEqualTo(StateObservationState(bla = null))
        assertThat(callbackState2[1]).isEqualTo(StateObservationState(bla = "State counter 0"))
        assertThat(callbackState2[2]).isEqualTo(StateObservationState(bla = "State counter 1"))
    }

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

    @Test
    fun `test addSelector with processCurrentStateImmediately`() {
        // Given
        val callbackState = mutableListOf<String>()
        val selector = object : Selector<StateObservationState, String>() {
            override fun map(state: StateObservationState): String =
                state.bla?.uppercase() ?: "<empty string>"
        }
        selector.observeSelector { value -> callbackState.add(value) }
        store.addSelector(processCurrentStateImmediately = true, selector)

        // Initial Then
        assertThat(callbackState).hasSize(1)
        assertThat(callbackState.first()).isEqualTo("<empty string>")

        // When
        store.observationManager.onStateChanged(
            currentDispatchCount = 0,
            state = StateObservationState(bla = "State counter 0")
        )
        selector.removeAllSelectorObservers()
        store.observationManager.onStateChanged(
            currentDispatchCount = 0,
            state = StateObservationState(bla = "After all observers have been removed from the selector")
        )

        // Then
        assertThat(callbackState).hasSize(2)
        assertThat(callbackState[0]).isEqualTo("<empty string>")
        assertThat(callbackState[1]).isEqualTo("STATE COUNTER 0")
    }

    @Test
    fun `test addSelector with callback function and processCurrentStateImmediately`() {
        // Given
        val callbackState = mutableListOf<String>()
        val selector = store.addSelectorFromCallback(
            processCurrentStateImmediately = true,
            observer = { value -> callbackState.add(value) }
        ) { state ->
            state.bla?.uppercase() ?: "<empty string>"
        }

        // Initial Then
        assertThat(callbackState).hasSize(1)
        assertThat(callbackState.first()).isEqualTo("<empty string>")

        // When
        store.observationManager.onStateChanged(
            currentDispatchCount = 0,
            state = StateObservationState(bla = "State counter 0")
        )
        selector.removeAllSelectorObservers()
        store.observationManager.onStateChanged(
            currentDispatchCount = 0,
            state = StateObservationState(bla = "After all observers have been removed from the selector")
        )

        // Then
        assertThat(callbackState).hasSize(2)
        assertThat(callbackState[0]).isEqualTo("<empty string>")
        assertThat(callbackState[1]).isEqualTo("STATE COUNTER 0")
    }

    @Test
    fun `test addSelector with callback function and without processCurrentStateImmediately`() {
        // Given
        val callbackState = mutableListOf<String>()
        val selector = store.addSelectorFromCallback { state ->
            state.bla?.uppercase() ?: "<empty string>"
        }
        selector.observeSelector { callbackState.add(it) }

        // Initial Then
        assertThat(callbackState).isEmpty()

        // When
        store.observationManager.onStateChanged(
            currentDispatchCount = 0,
            state = StateObservationState(bla = "State counter 0")
        )
        selector.removeAllSelectorObservers()
        store.observationManager.onStateChanged(
            currentDispatchCount = 0,
            state = StateObservationState(bla = "After all observers have been removed from the selector")
        )

        // Then
        assertThat(callbackState).hasSize(1)
        assertThat(callbackState[0]).isEqualTo("STATE COUNTER 0")
    }

    @Test
    fun `test addSelectorToStateFlow and processCurrentStateImmediately`() {
        // Given
        val callbackState = mutableListOf<String>()
        val selector = store.addSelectorStateFlow(
            initialValue = "<initial value>",
            processCurrentStateImmediately = true
        ) { state ->
            state.bla?.uppercase() ?: "<empty string>"
        }
        selector.observeSelector { callbackState.add(it) }

        // Initial Then
        assertThat(callbackState).hasSize(0)
        assertThat(selector.value).isEqualTo("<empty string>")

        // When
        store.observationManager.onStateChanged(
            currentDispatchCount = 0,
            state = StateObservationState(bla = "State counter 0")
        )
        selector.removeAllSelectorObservers()
        store.observationManager.onStateChanged(
            currentDispatchCount = 0,
            state = StateObservationState(bla = "After all observers have been removed from the selector")
        )

        // Then
        assertThat(callbackState).hasSize(1)
        assertThat(callbackState[0]).isEqualTo("STATE COUNTER 0")
        assertThat(selector.value).isEqualTo("AFTER ALL OBSERVERS HAVE BEEN REMOVED FROM THE SELECTOR")
    }

    @Test
    fun `test addSelectorToStateFlow and without processCurrentStateImmediately`() {
        // Given
        val callbackState = mutableListOf<String>()
        val selector = store.addSelectorStateFlow(
            initialValue = "<initial value>",
            processCurrentStateImmediately = false
        ) { state -> state.bla?.uppercase() ?: "<empty string>" }

        // Then
        assertThat(callbackState).hasSize(0)
        assertThat(selector.value).isEqualTo("<initial value>")
    }

    data class StateObservationState(val bla: String? = null) : State {
        override fun clone(): State = this.copy()
    }
}
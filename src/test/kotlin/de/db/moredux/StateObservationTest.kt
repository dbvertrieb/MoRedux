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
import org.junit.jupiter.api.Test

class StateObservationTest {

    private val sut = StateObservation(StateObservationState())
// TODO rewrite to stateFlow
//
//    @Test
//    fun `test addSelector as Selector instance with no post and onStateChanged`() {
//        // Given
//        val selector = object : Selector<StateObservationState, String>(usePost = false) {
//            override fun map(state: StateObservationState): String =
//                state.bla ?: "Ist leer"
//        }
//
//        // When
//        sut.addSelector(false, selector)
//        sut.onStateChanged(0, StateObservationState(bla = "State counter 0"))
//
//        // Then
//        assertThat(selector.value).isEqualTo("State counter 0")
//    }
//
//    @Test
//    fun `test addSelector as callback with post and processCurrentStateImmediately`() {
//        // When
//        val selector = sut.addSelector(true) { state -> state.bla ?: "Ist leer" }
//
//        // Then
//        assertThat(selector.value).isEqualTo("Ist leer")
//    }
//
//    @Test
//    fun `test addSelector as callback with post and onStateChanged`() {
//        // When
//        val selector = sut.addSelector(false) { state -> state.bla ?: "Ist leer" }
//        sut.onStateChanged(0, StateObservationState(bla = "State counter 0"))
//
//        // Then
//        assertThat(selector.value).isEqualTo("State counter 0")
//    }

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
        sut.addStateObserver(false, stateObserver)
        sut.onStateChanged(0, StateObservationState(bla = "State counter 0"))

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
        sut.addStateObserver(true, stateObserver)
        sut.onStateChanged(0, StateObservationState(bla = "State counter 0"))

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
        sut.addStateObserver { state -> callbackState.add(state) }
        sut.onStateChanged(0, StateObservationState(bla = "State counter 0"))

        // Then
        assertThat(callbackState).hasSize(1)
        assertThat(callbackState[0]).isEqualTo(StateObservationState(bla = "State counter 0"))
    }

    @Test
    fun `test addStateObserver as callback with processCurrentStateImmediately`() {
        // Given
        val callbackState = mutableListOf<StateObservationState>()

        // When
        sut.addStateObserver(true) { state -> callbackState.add(state) }
        sut.onStateChanged(0, StateObservationState(bla = "State counter 0"))

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
        sut.addStateObserver(true) { state -> callbackState.add(state) }
        sut.addStateObserver(true) { state -> callbackState.add(state) }
        sut.onStateChanged(0, StateObservationState(bla = "State counter 0"))

        // Then
        assertThat(callbackState).hasSize(4)
        assertThat(callbackState[0]).isEqualTo(StateObservationState(bla = null))
        assertThat(callbackState[1]).isEqualTo(StateObservationState(bla = null))
        assertThat(callbackState[2]).isEqualTo(StateObservationState(bla = "State counter 0"))
        assertThat(callbackState[3]).isEqualTo(StateObservationState(bla = "State counter 0"))
    }

    @Test
    fun `test multiple removeStateObserver`() {
        // Given
        val callbackState = mutableListOf<StateObservationState>()
        val observer1 = sut.addStateObserver(true) { state -> callbackState.add(state) }
        sut.addStateObserver(true) { state -> callbackState.add(state) }

        // When
        sut.removeObserver(observer1)
        sut.onStateChanged(0, StateObservationState(bla = "State counter 0"))

        // Then
        assertThat(callbackState).hasSize(3)
        assertThat(callbackState[0]).isEqualTo(StateObservationState(bla = null))
        assertThat(callbackState[1]).isEqualTo(StateObservationState(bla = null))
        assertThat(callbackState[2]).isEqualTo(StateObservationState(bla = "State counter 0"))
    }

    @Test
    fun `test teardown`() {
        // Given
        val callbackState = mutableListOf<StateObservationState>()
        sut.addStateObserver(true) { state -> callbackState.add(state) }
        sut.addStateObserver(true) { state -> callbackState.add(state) }

        // When
        sut.teardown()
        sut.onStateChanged(0, StateObservationState(bla = "State counter 0"))

        // Then
        assertThat(callbackState).hasSize(2)
        assertThat(callbackState[0]).isEqualTo(StateObservationState(bla = null))
        assertThat(callbackState[1]).isEqualTo(StateObservationState(bla = null))
    }

    data class StateObservationState(val bla: String? = null) : State {
        override fun clone(): State = this.copy()
    }
}
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

class ObservationManagerTest {

    private val sut = ObservationManager(StateObservationState())

    @Test
    fun `test multiple removeStateObserver`() {
        // Given
        val callbackState = mutableListOf<StateObservationState>()
        val stateObserver1 = object : StateObserver<StateObservationState> {
            override fun onStateChanged(state: StateObservationState) {
                callbackState.add(state)
            }
        }
        val stateObserver2 = object : StateObserver<StateObservationState> {
            override fun onStateChanged(state: StateObservationState) {
                callbackState.add(state)
            }
        }
        val observer1 = sut.addStateObserver(true, stateObserver1)
        sut.addStateObserver(true, stateObserver2)

        // When
        sut.removeObserver(observer1)
        sut.onStateChanged(currentDispatchCount = 0, state = StateObservationState(bla = "State counter 0"))

        // Then
        assertThat(callbackState).hasSize(3)
        // first addStateObserver with processCurrentStateImmediately == true
        assertThat(callbackState[0]).isEqualTo(StateObservationState(bla = null))
        // second addStateObserver with processCurrentStateImmediately == true
        assertThat(callbackState[1]).isEqualTo(StateObservationState(bla = null))
        // onStateChanged execution
        assertThat(callbackState[2]).isEqualTo(StateObservationState(bla = "State counter 0"))
    }

    @Test
    fun `test teardown`() {
        // Given
        val callbackState = mutableListOf<StateObservationState>()
        val stateObserver1 = object : StateObserver<StateObservationState> {
            override fun onStateChanged(state: StateObservationState) {
                callbackState.add(state)
            }
        }
        val stateObserver2 = object : StateObserver<StateObservationState> {
            override fun onStateChanged(state: StateObservationState) {
                callbackState.add(state)
            }
        }
        sut.addStateObserver(true, stateObserver1)
        sut.addStateObserver(true, stateObserver2)

        // When
        sut.teardown()
        sut.onStateChanged(0, StateObservationState(bla = "State counter 0"))

        // Then
        assertThat(callbackState).hasSize(2)
        // first addStateObserver with processCurrentStateImmediately == true
        assertThat(callbackState[0]).isEqualTo(StateObservationState(bla = null))
        // second addStateObserver with processCurrentStateImmediately == true
        assertThat(callbackState[1]).isEqualTo(StateObservationState(bla = null))
    }

    data class StateObservationState(val bla: String? = null) : State {
        override fun clone(): State = this.copy()
    }
}
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

class StoreTest {

    @Test
    fun `test wants`() {
        // Given
        val store = Store.Builder<StoreState>()
            .withInitialState(StoreState())
            .registerReducer<TestAction1> { state, _ -> state }
            .registerReducer<TestAction2> { state, _ -> state }
            .build()

        // When & Then
        assertThat(store.wants(TestAction1)).isTrue()
        assertThat(store.wants(TestAction2)).isTrue()
        assertThat(store.wants(TestAction3)).isFalse()
    }

    @Test
    fun `test dispatch`() {
        // Given
        var testReducer1Executed = false
        var testReducer2Executed = false
        val store = Store.Builder<StoreState>()
            .withInitialState(StoreState())
            .registerReducer<TestAction1> { state, _ ->
                testReducer1Executed = true
                state.copy(bla = "Reducer 1")
            }
            .registerReducer<TestAction2> { state, _ ->
                testReducer2Executed = true
                state.copy(bla = "Reducer 2")
            }
            .build()

        val callbackState = mutableListOf<StoreState>()
        store.observation.addStateObserver { state -> callbackState.add(state) }

        // When
        val wasDispatched = store.dispatch(TestAction1)

        // Then
        assertThat(wasDispatched).isTrue()
        assertThat(testReducer1Executed).isTrue()
        assertThat(testReducer2Executed).isFalse()
        assertThat(callbackState).hasSize(1)
        assertThat(callbackState[0]).isEqualTo(StoreState().copy(bla = "Reducer 1"))
    }

    @Test
    fun `test republish`() {
        // Given
        val store = Store.Builder<StoreState>()
            .withInitialState(StoreState())
            .registerReducer<TestAction1> { state, _ -> state.copy(bla = "Reducer 1") }
            .registerReducer<TestAction2> { state, _ -> state.copy(bla = "Reducer 2") }
            .build()

        val callbackState = mutableListOf<StoreState>()
        store.observation.addStateObserver { state -> callbackState.add(state) }

        // When
        store.dispatch(TestAction1)
        store.republish()

        // Then
        assertThat(callbackState).hasSize(2)
        val expectedState = StoreState(bla = "Reducer 1")
        assertThat(callbackState[0]).isEqualTo(expectedState)
        assertThat(callbackState[1]).isEqualTo(expectedState)
    }

    @Test
    fun `test rehydrate different state`() {
        // Given
        val store = Store.Builder<StoreState>()
            .withInitialState(StoreState())
            .registerReducer<TestAction1> { state, _ -> state.copy(bla = "Reducer 1") }
            .registerReducer<TestAction2> { state, _ -> state.copy(bla = "Reducer 2") }
            .build()

        val callbackState = mutableListOf<StoreState>()
        store.observation.addStateObserver { state -> callbackState.add(state) }

        // When
        store.dispatch(TestAction1)
        store.rehydrate(StoreState(bla = "Wazzzuuuup"))

        // Then
        assertThat(callbackState).hasSize(2)
        assertThat(callbackState[0]).isEqualTo(StoreState(bla = "Reducer 1"))
        assertThat(callbackState[1]).isEqualTo(StoreState(bla = "Wazzzuuuup"))
    }

    @Test
    fun `test rehydrate same state`() {
        // Given
        val store = Store.Builder<StoreState>()
            .withInitialState(StoreState())
            .registerReducer<TestAction1> { state, _ -> state.copy(bla = "Reducer 1") }
            .registerReducer<TestAction2> { state, _ -> state.copy(bla = "Reducer 2") }
            .build()

        val callbackState = mutableListOf<StoreState>()
        store.observation.addStateObserver { state -> callbackState.add(state) }

        // When
        store.dispatch(TestAction1)
        store.rehydrate(StoreState(bla = "Reducer 1"))

        // Then
        assertThat(callbackState).hasSize(1)
        assertThat(callbackState[0]).isEqualTo(StoreState(bla = "Reducer 1"))
    }

    data class StoreState(val bla: String? = null) : State {
        override fun clone(): State = this.copy()
    }

    data object TestAction1 : Action
    data object TestAction2 : Action
    data object TestAction3 : Action
}
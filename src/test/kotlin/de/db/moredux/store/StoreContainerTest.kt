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

package de.db.moredux.store

import com.google.common.truth.Truth.assertThat
import de.db.moredux.Action
import de.db.moredux.State
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

class StoreContainerTest {


    @Test
    fun `test wants`() {
        // Given
        val storeContainer = StoreContainer.Builder()
            .addStore(createStore1())
            .build()

        // When & Then
        assertThat(storeContainer.wants(TestAction1)).isTrue()
        assertThat(storeContainer.wants(TestAction2)).isFalse()
    }

    @Test
    fun `no stores registered in storecontainer - no dispatch possible`() {
        // Given
        val storeContainer = StoreContainer.Builder().build()

        // When
        val wasDispatched = storeContainer.dispatch(mock())

        // Then
        assertThat(wasDispatched).isFalse()
    }

    @Test
    fun `stores registered but no one wants the action - no dispatch possible`() {
        // Given
        val storeContainer = StoreContainer.Builder()
            .addStore(createStore1())
            .addStore(createStore2())
            .build()

        // When
        val wasDispatched = storeContainer.dispatch(TestAction3)

        // Then
        assertThat(wasDispatched).isFalse()
    }

    @Test
    fun `stores registered and one store wants the action - dispatch successful`() {
        // Given
        val storeContainer = StoreContainer.Builder()
            .addStore(createStore1())
            .addStore(createStore2())
            .build()

        // When
        val wasDispatched = storeContainer.dispatch(TestAction1)

        // Then
        assertThat(wasDispatched).isTrue()
    }

    @Test
    fun `Builder - store has already been added to the builder - addStore is skipped`() {
        // Given
        val store1 = createStore1()
        val store2 = createStore2()

        // When
        val builder = StoreContainer.Builder()
            .addStore(store1)
            .addStore(store1)
            .addStore(store2)

        // Then
        assertThat(builder.stores).hasSize(2)
    }

    @Test
    fun `teardown calls teardown on each Store`() {
        // Given
        val store1 = mock<Store<StoreState1>>()
        val store2 = mock<Store<StoreState2>>()
        val storeContainer = StoreContainer.Builder()
            .addStore(store1)
            .addStore(store2)
            .build()

        // When
        storeContainer.teardown()

        // Then
        verify(store1).teardown()
        verify(store2).teardown()
    }

    @Test
    fun `DispatchCounter should be incremented twice on one storeContainer dispatch`() {
        // Given
        val storeContainer = StoreContainer.Builder()
            .addStore(createStore1())
            .build()

        // Pre Then
        assertThat(storeContainer.currentDispatchCount).isEqualTo(0)

        // When
        storeContainer.dispatch(TestAction1)

        // Then
        assertThat(storeContainer.currentDispatchCount).isEqualTo(2)
    }

    private fun createStore1() =
        Store.Builder<StoreState1>()
            .withInitialState(StoreState1())
            .registerReducerToState<TestAction1> { state, _ -> state.copy(bla = "Reducer 1") }
            .build()

    private fun createStore2() =
        Store.Builder<StoreState2>()
            .withInitialState(StoreState2())
            .registerReducerToState<TestAction2> { state, _ -> state.copy(bla = "Reducer 2") }
            .build()

    private data class StoreState1(val bla: String? = null) : State {
        override fun clone(): State = this.copy()
    }

    private data class StoreState2(val bla: String? = null) : State {
        override fun clone(): State = this.copy()
    }

    private data object TestAction1 : Action
    private data object TestAction2 : Action
    private data object TestAction3 : Action
}
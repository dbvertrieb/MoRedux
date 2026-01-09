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
import de.db.moredux.preferences.PreferencesAction
import de.db.moredux.preferences.PreferencesState
import de.db.moredux.preferences.ReducerSetDarkMode
import de.db.moredux.preferences.ReducerSetLightMode
import de.db.moredux.preferences.ReducerSetUsername
import de.db.moredux.todo.ReducerAddTodo
import de.db.moredux.todo.TodoAction
import de.db.moredux.todo.TodoState
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

class StoreContainerTest {


    @Test
    fun `test wants`() {
        // Given
        val storeContainer = StoreContainer.Builder()
                .addStore(createStoreTodo())
                .build()

        // When & Then
        assertThat(storeContainer.wants(TodoAction.Add("Bla"))).isTrue()
        assertThat(storeContainer.wants(UnknownAction)).isFalse()
    }

    @Test
    fun `no stores registered in storeContainer - no dispatch possible`() {
        // Given
        val storeContainer = StoreContainer.Builder().build()

        // When
        storeContainer.dispatch(mock())

        // Then
        assertThat(storeContainer.currentDispatchCount).isEqualTo(0)
    }

    @Test
    fun `stores registered but no one wants the action - no dispatch possible`() {
        // Given
        val storeContainer = StoreContainer.Builder()
                .addStore(createStoreTodo())
                .addStore(createStorePreferences())
                .build()

        // When
        storeContainer.dispatch(UnknownAction)

        // Then
        assertThat(storeContainer.currentDispatchCount).isEqualTo(0)
    }

    @Test
    fun `stores registered and one store wants the action - dispatch successful`() {
        // Given
        val storeContainer = StoreContainer.Builder()
                .addStore(createStoreTodo())
                .addStore(createStorePreferences())
                .build()

        // When
        storeContainer.dispatch(PreferencesAction.SetLightMode)

        // Then
        assertThat(storeContainer.currentDispatchCount).isEqualTo(1)
    }

    @Test
    fun `Builder - store has already been added to the builder - addStore is skipped`() {
        // Given
        val storeTodo = createStoreTodo()
        val storePreferences = createStorePreferences()

        // When
        val builder = StoreContainer.Builder()
                .addStore(storeTodo)
                .addStore(storeTodo)
                .addStore(storePreferences)

        // Then
        assertThat(builder.stores).hasSize(2)
    }

    @Test
    fun `teardown calls teardown on each Store`() {
        // Given
        val storeTodo = mock<Store<TodoState>>()
        val storePreferences = mock<Store<PreferencesState>>()
        val storeContainer = StoreContainer.Builder()
                .addStore(storeTodo)
                .addStore(storePreferences)
                .build()

        // When
        storeContainer.teardown()

        // Then
        verify(storeTodo).teardown()
        verify(storePreferences).teardown()
    }

    @Test
    fun `DispatchCounter should be incremented for each successful dispatch`() {
        // Given
        val storeContainer = StoreContainer.Builder()
                .addStore(createStoreTodo())
                .addStore(createStorePreferences())
                .build()

        // Pre Then
        assertThat(storeContainer.currentDispatchCount).isEqualTo(0)

        // When & Then
        storeContainer.dispatch(PreferencesAction.SetDarkMode)
        assertThat(storeContainer.currentDispatchCount).isEqualTo(1)

        // When & Then
        storeContainer.dispatch(TodoAction.Add("Deep-fry chocolate bar"))
        assertThat(storeContainer.currentDispatchCount).isEqualTo(2)

        // When & Then
        storeContainer.dispatch(UnknownAction)
        assertThat(storeContainer.currentDispatchCount).isEqualTo(2)
    }

    private fun createStoreTodo() =
        Store.Builder<TodoState>()
                .withInitialState(TodoState.INITIAL)
                .registerReducer<TodoAction.Add>(ReducerAddTodo())
                .registerReducerToState<TodoAction.SetDone> { state, action ->
                    state.copy(
                        done = state.done.toMutableList().also { it[action.index] = true }
                    )
                }
                .build()

    private fun createStorePreferences() =
        Store.Builder<PreferencesState>()
                .withInitialState(PreferencesState.INITIAL)
                .registerReducer<PreferencesAction.SetLightMode>(ReducerSetLightMode())
                .registerReducer<PreferencesAction.SetDarkMode>(ReducerSetDarkMode())
                .registerReducer<PreferencesAction.SetUsername>(ReducerSetUsername())
                .build()

    private data object UnknownAction : Action
}
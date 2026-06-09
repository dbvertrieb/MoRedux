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
import de.db.moredux.observation.addStateObserver
import de.db.moredux.reducer.Reducer
import de.db.moredux.reducer.ReducerResult
import de.db.moredux.settings.MoReduxSettings
import de.db.moredux.settings.MoReduxSettings.LogMode
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

class StoreTest {

    @Test
    fun `test teardown`() {
        // Given
        val store = Store.Builder<StoreState>()
            .withInitialState(StoreState())
            .registerReducerToState<TestAction1> { state, _ -> state }
            .registerReducerToState<TestAction2> { state, _ -> state }
            .build()
        val callbackState = mutableListOf<StoreState>()
        store.addStateObserver { state -> callbackState.add(state) }

        // When
        store.teardown()
        store.dispatch(TestAction1)

        // Then
        assertThat(store.wants(TestAction1)).isFalse()
        assertThat(store.wants(TestAction2)).isFalse()
        assertThat(callbackState).isEmpty()
    }

    @Test
    fun `test wants only with reducers`() {
        // Given
        val store = Store.Builder<StoreState>()
            .withInitialState(StoreState())
            .registerReducerToState<TestAction1> { state, _ -> state }
            .registerReducerToState<TestAction2> { state, _ -> state }
            .build()

        // When & Then
        assertThat(store.wants(TestAction1)).isTrue()
        assertThat(store.wants(TestAction2)).isTrue()
        assertThat(store.wants(TestAction3)).isFalse()
    }

    @Test
    fun `test wants only with middlewares`() {
        // Given
        val store = Store.Builder<StoreState>()
            .withInitialState(StoreState())
            .registerMiddleware { _, _, _, _ ->
                println("Dummy")
            }
            .build()

        // When & Then
        assertThat(store.wants(TestAction1)).isTrue()
        assertThat(store.wants(TestAction2)).isTrue()
        assertThat(store.wants(TestAction3)).isTrue()
    }

    @Test
    fun `test dispatch`() {
        // Given
        var testReducer1Executed = false
        var testReducer2Executed = false
        val store = Store.Builder<StoreState>()
            .withInitialState(StoreState())
            .registerReducerToState<TestAction1> { state, _ ->
                testReducer1Executed = true
                state.copy(bla = "Reducer 1")
            }
            .registerReducerToState<TestAction2> { state, _ ->
                testReducer2Executed = true
                state.copy(bla = "Reducer 2")
            }
            .build()

        val callbackState = mutableListOf<StoreState>()
        store.addStateObserver { state -> callbackState.add(state) }

        // When
        store.dispatch(TestAction1)

        // Then
        assertThat(testReducer1Executed).isTrue()
        assertThat(testReducer2Executed).isFalse()
        assertThat(callbackState).hasSize(1)
        assertThat(callbackState[0]).isEqualTo(StoreState().copy(bla = "Reducer 1"))
    }

    @ParameterizedTest
    @MethodSource("testSynchronizationParameters")
    fun `test dispatch parallel synchronized`(isSynchronized: Boolean, expectedBla: String) {
        // Given
        var testReducer1Executed = false
        var testReducer2Executed = false
        val store = Store.Builder<StoreState>()
            .withInitialState(StoreState())
            .withSynchronizedDispatch(isSynchronized)
            .registerReducerToState<TestAction1> { state, _ ->
                testReducer1Executed = true
                Thread.sleep(1000)
                state.copy(bla = "Reducer 1")
            }
            .registerReducerToState<TestAction2> { state, _ ->
                testReducer2Executed = true
                state.copy(bla = "Reducer 2")
            }
            .build()

        val callbackState = mutableListOf<StoreState>()
        store.addStateObserver { state -> callbackState.add(state) }

        // When
        val thread1 = Thread {
            store.dispatch(TestAction1)
        }
        val thread2 = Thread {
            store.dispatch(TestAction2)
        }

        thread1.start()
        thread2.start()

        thread1.join()
        thread2.join()

        // Then
        assertThat(testReducer1Executed).isTrue()
        assertThat(testReducer2Executed).isTrue()
        assertThat(callbackState).hasSize(2)
        assertThat(store.state.bla).isEqualTo(expectedBla)
    }

    @Test
    fun `test republish`() {
        // Given
        val store = Store.Builder<StoreState>()
            .withInitialState(StoreState())
            .registerReducerToState<TestAction1> { state, _ -> state.copy(bla = "Reducer 1") }
            .registerReducerToState<TestAction2> { state, _ -> state.copy(bla = "Reducer 2") }
            .build()

        val callbackState = mutableListOf<StoreState>()
        store.addStateObserver { state -> callbackState.add(state) }

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
            .registerReducerToState<TestAction1> { state, _ -> state.copy(bla = "Reducer 1") }
            .registerReducerToState<TestAction2> { state, _ -> state.copy(bla = "Reducer 2") }
            .build()

        val callbackState = mutableListOf<StoreState>()
        store.addStateObserver { state -> callbackState.add(state) }

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
            .registerReducerToState<TestAction1> { state, _ -> state.copy(bla = "Reducer 1") }
            .registerReducerToState<TestAction2> { state, _ -> state.copy(bla = "Reducer 2") }
            .build()

        val callbackState = mutableListOf<StoreState>()
        store.addStateObserver { state -> callbackState.add(state) }

        // When
        store.dispatch(TestAction1)
        store.rehydrate(StoreState(bla = "Reducer 1"))

        // Then
        assertThat(callbackState).hasSize(1)
        assertThat(callbackState[0]).isEqualTo(StoreState(bla = "Reducer 1"))
    }

    @Test
    fun `test reset`() {
        // Given
        val initialState = StoreState()
        val store = Store.Builder<StoreState>()
            .withInitialState(initialState)
            .registerReducer<TestAction1> { state, _ -> ReducerResult(state.copy(bla = "Reducer 1")) }
            .build()

        // When
        store.dispatch(TestAction1)
        assertThat(store.state.bla).isEqualTo("Reducer 1")

        // When
        store.reset()

        // Then
        assertThat(store.state).isEqualTo(initialState)
        assertThat(store.state.bla).isNull()
    }

    @Test
    fun `test isPartOfStoreContainer without adding an injected dispatcher`() {
        // Given
        val store = Store.Builder<StoreState>()
            .withInitialState(StoreState())
            .registerReducerToState<TestAction1> { state, _ -> state.copy(bla = "Reducer 1") }
            .build()

        // When
        val actual = store.isPartOfStoreContainer()

        // Then
        assertThat(actual).isFalse()
    }

    @Test
    fun `test isPartOfStoreContainer after injecting a dispatcher`() {
        // Given
        val store = Store.Builder<StoreState>()
            .withInitialState(StoreState())
            .registerReducerToState<TestAction1> { state, _ -> state.copy(bla = "Reducer 1") }
            .build()
        store.injectedDispatcher = mock()


        // When
        val actual = store.isPartOfStoreContainer()

        // Then
        assertThat(actual).isTrue()
    }

    @Test
    fun `storecontainer as dispatcher injected is used when follow up actions are processed`() {
        // Given
        val store = Store.Builder<StoreState>()
            .withInitialState(StoreState())
            .registerReducer<TestAction1>(
                object : Reducer<StoreState, TestAction1>() {
                    override fun reduce(state: StoreState, action: TestAction1): ReducerResult<StoreState> {
                        return ReducerResult(state, TestAction2)
                    }
                })
            .registerReducerToState<TestAction2> { state, _ -> state.copy(bla = "Reducer 2") }
            .build()
        val injectedDispatcher: Dispatcher = mock()
        store.injectedDispatcher = injectedDispatcher

        // When
        store.dispatch(TestAction1)

        // Then
        verify(injectedDispatcher).dispatch(TestAction2)
    }

    @Test
    fun `dispatch with middleware with action rewrite without breaking the chain`() {
        MoReduxSettings.logMode = LogMode.FULL
        // Given
        var preMiddlewareHasBeenProcessed = false
        var postMiddlewareHasBeenProcessed = false
        val store = Store.Builder<StoreState>()
            .withInitialState(StoreState())
            .registerMiddleware { _, _, _, next ->
                preMiddlewareHasBeenProcessed = true
                next(TestAction2)
                postMiddlewareHasBeenProcessed = true
            }
            .registerReducerToState<TestAction1> { state, _ -> state.copy(bla = "Reducer 1") }
            .registerReducerToState<TestAction2> { state, _ -> state.copy(bla = "Reducer 2") }
            .build()

        // When
        store.dispatch(TestAction1)

        // Then
        assertThat(store.state.bla).isEqualTo("Reducer 2")
        assertThat(preMiddlewareHasBeenProcessed).isTrue()
        assertThat(postMiddlewareHasBeenProcessed).isTrue()
    }

    @Test
    fun `dispatch with middleware that breaks the chain`() {
        // Given
        val store = Store.Builder<StoreState>()
            .withInitialState(StoreState())
            .registerMiddleware { _, _, action, next ->
                if (action == TestAction2) {
                    next(TestAction2)
                }
                // TestAction1 leads to breaking the execution change
            }
            .registerReducerToState<TestAction1> { state, _ -> state.copy(bla = "Reducer 1") }
            .registerReducerToState<TestAction2> { state, _ -> state.copy(bla = "Reducer 2") }
            .build()

        // When
        store.dispatch(TestAction1)

        // Then
        assertThat(store.state.bla).isNull()

        // When
        store.dispatch(TestAction2)

        // Then
        assertThat(store.state.bla).isEqualTo("Reducer 2")
    }

    @Test
    fun `dispatch with middleware for action that rewrites the action`() {
        // Given
        val store = Store.Builder<StoreState>()
            .withInitialState(StoreState())
            .registerMiddlewareForAction<TestAction1> { _, _, _, next -> next(TestAction3) }
            .registerMiddleware { _, _, action, next -> next(action) }
            .registerReducerToState<TestAction1> { state, _ -> state.copy(bla = "Reducer 1") }
            .registerReducerToState<TestAction2> { state, _ -> state.copy(bla = "Reducer 2") }
            .registerReducerToState<TestAction3> { state, _ -> state.copy(bla = "Reducer 3") }
            .build()

        // When
        store.dispatch(TestAction1)

        // Then
        assertThat(store.state.bla).isEqualTo("Reducer 3")

        // When
        store.dispatch(TestAction2)

        // Then
        assertThat(store.state.bla).isEqualTo("Reducer 2")

        // When
        store.dispatch(TestAction3)

        // Then
        assertThat(store.state.bla).isEqualTo("Reducer 3")
    }

    private data class StoreState(val bla: String? = null) : State {
        override fun clone(): State = this.copy()
    }

    private data object TestAction1 : Action
    private data object TestAction2 : Action
    private data object TestAction3 : Action

    companion object {
        @JvmStatic
        fun testSynchronizationParameters() = listOf(
            Arguments.of(true, "Reducer 2"),
            Arguments.of(false, "Reducer 1")
        )
    }
}
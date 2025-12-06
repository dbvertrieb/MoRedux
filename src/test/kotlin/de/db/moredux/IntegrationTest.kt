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
import de.db.moredux.IntegrationTest.TodoAction.Add
import de.db.moredux.IntegrationTest.TodoAction.SetDone
import de.db.moredux.observation.addSelectorStateFlow
import de.db.moredux.reducer.Reducer
import de.db.moredux.reducer.ReducerResult
import de.db.moredux.settings.MoReduxSettings
import de.db.moredux.store.Store
import org.junit.jupiter.api.Test

class IntegrationTest {

    // Define the state
    data class TodoState(
        val todos: List<String>,
        val done: List<Boolean>,
        val middlewareCounter: Int
    ) : State {
        override fun clone(): State = copy()
    }

    // Define the possible actions
    sealed class TodoAction : Action {
        data class Add(val todo: String) : TodoAction()
        data class SetDone(val index: Int) : TodoAction()
        data object IncrementMiddlewareCounter : TodoAction()
    }

    // Example of a reducer implemented as class extending teh Reducer interface
    class ReducerAddTodo : Reducer<TodoState, Add>() {
        override fun reduce(state: TodoState, action: Add): ReducerResult<TodoState> {
            val todos = state.todos.toMutableList()
            todos.add(action.todo)

            val done = state.done.toMutableList()
            done.add(false)

            return ReducerResult(
                state.copy(
                    todos = todos.toList(),
                    done = done.toList()
                )
            )
        }
    }

    @Test
    fun `test with none breaking middlewares`() {

        val log = mutableListOf<String>()
        MoReduxSettings.logDebug = { tag, message -> log.add("DEBUG - $tag: $message") }
        MoReduxSettings.logWarn = { tag, message -> log.add("WARN - $tag: $message") }

        // Build the store and register all reducers
        val store = Store.Builder<TodoState>()
                .withInitialState(TodoState(todos = emptyList(), done = emptyList(), middlewareCounter = 0))
                .registerReducer<Add>(ReducerAddTodo())
                .registerReducerToState<TodoAction.IncrementMiddlewareCounter> { state, _ ->
                    state.copy(middlewareCounter = state.middlewareCounter + 1)
                }
                .registerReducerToState<SetDone> { state, action ->
                    // Example of a reducer implemented as function that simply returns a new state
                    val done = state.done.toMutableList()
                    done[action.index] = true

                    state.copy(done = done.toList())
                }
                .registerMiddleware { store, action, next ->
                    // Make sure the same action is not processed twice - infinite recursion guard
                    if (action != TodoAction.IncrementMiddlewareCounter) {
                        store.dispatch(TodoAction.IncrementMiddlewareCounter)
                    }
                    next(action)
                }
                .build()

        // set up a selector only with todos that have not been done yet
        val unfinishedTodos = store.addSelectorStateFlow(emptyList()) { state ->
            state.todos.filterIndexed { index, _ -> !state.done[index] }
        }

        // Perform some actions - these would be actions trigger by user input
        store.dispatch(Add("Invite friends"))
        store.dispatch(Add("Cook dinner"))
        store.dispatch(SetDone(0))

        // Then
        assertThat(unfinishedTodos.value).isEqualTo(listOf("Cook dinner"))
        assertThat(store.state.middlewareCounter).isEqualTo(3)
//        println(log)
    }

    @Test
    fun `test with breaking middlewares`() {

        val log = mutableListOf<String>()
        MoReduxSettings.logDebug = { tag, message -> log.add("DEBUG - $tag: $message") }
        MoReduxSettings.logWarn = { tag, message -> log.add("WARN - $tag: $message") }

        // Build the store and register all reducers
        val store = Store.Builder<TodoState>()
                .withInitialState(TodoState(todos = emptyList(), done = emptyList(), middlewareCounter = 0))
                .registerReducer<Add>(ReducerAddTodo())
                .registerReducerToState<SetDone> { state, action ->
                    // Example of a reducer implemented as function that simply returns a new state
                    val done = state.done.toMutableList()
                    done[action.index] = true

                    state.copy(done = done.toList())
                }
                .registerMiddleware { _, _, _ ->
                    /* do nothing, do not call "next" callback */
                }
                .registerMiddleware { store, action, next ->
                    store.dispatch(TodoAction.IncrementMiddlewareCounter)
                    next(action)
                }
                .build()

        // set up a selector only with todos that have not been done yet
        val unfinishedTodos = store.addSelectorStateFlow(emptyList()) { state ->
            state.todos.filterIndexed { index, _ -> !state.done[index] }
        }

        // Perform some actions - these would be actions trigger by user input - all of them are not executed since the
        // the first middleware breaks
        store.dispatch(Add("Invite friends"))
        store.dispatch(Add("Cook dinner"))
        store.dispatch(SetDone(0))

        // Then
        assertThat(unfinishedTodos.value).isEmpty()
        assertThat(store.state.middlewareCounter).isEqualTo(0)
//        println(log)
    }
}
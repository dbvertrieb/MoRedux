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
import de.db.moredux.observation.addSelectorStateFlow
import de.db.moredux.settings.MoReduxSettings
import de.db.moredux.store.Store
import de.db.moredux.todo.ReducerAddTodo
import de.db.moredux.todo.TodoAction.Add
import de.db.moredux.todo.TodoAction.IncrementCounter
import de.db.moredux.todo.TodoAction.SetDone
import de.db.moredux.todo.TodoState
import org.junit.jupiter.api.Test

class IntegrationTest {

    @Test
    fun `test with none breaking middlewares`() {

        val log = mutableListOf<String>()
        MoReduxSettings.logDebug = { tag, message -> log.add("DEBUG - $tag: $message") }
        MoReduxSettings.logWarn = { tag, message -> log.add("WARN - $tag: $message") }

        // Build the store and register all reducers
        val store = Store.Builder<TodoState>()
                .withInitialState(TodoState(todos = emptyList(), done = emptyList(), counter = 0))
                .registerReducer<Add>(ReducerAddTodo())
                .registerReducerToState<IncrementCounter> { state, _ ->
                    state.copy(counter = state.counter + 1)
                }
                .registerReducerToState<SetDone> { state, action ->
                    // Example of a reducer implemented as function that simply returns a new state
                    val done = state.done.toMutableList()
                    done[action.index] = true

                    state.copy(done = done.toList())
                }
                .registerMiddleware { dispatcher, _, action, next ->
                    // Make sure the same action is not processed twice - infinite recursion guard
                    if (action != IncrementCounter) {
                        dispatcher.dispatch(IncrementCounter)
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
        assertThat(store.state.counter).isEqualTo(3)
    }

    @Test
    fun `test with breaking middlewares`() {

        val log = mutableListOf<String>()
        MoReduxSettings.logDebug = { tag, message -> log.add("DEBUG - $tag: $message") }
        MoReduxSettings.logWarn = { tag, message -> log.add("WARN - $tag: $message") }

        // Build the store and register all reducers
        val store = Store.Builder<TodoState>()
                .withInitialState(TodoState(todos = emptyList(), done = emptyList(), counter = 0))
                .registerReducer<Add>(ReducerAddTodo())
                .registerReducerToState<SetDone> { state, action ->
                    // Example of a reducer implemented as function that simply returns a new state
                    val done = state.done.toMutableList()
                    done[action.index] = true

                    state.copy(done = done.toList())
                }
                .registerMiddleware { _, _, _, _ ->
                    /* do nothing, do not call "next" callback */
                }
                .registerMiddleware { dispatcher, _, action, next ->
                    dispatcher.dispatch(IncrementCounter)
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
        assertThat(store.state.counter).isEqualTo(0)
    }
}
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


class ReducerCallbackToStateTest {

    @Test
    fun `test reduce`() {
        // Given
        val sut =
            ReducerCallbackToState<ReducerCallbackToStateState, ReducerCallbackToStateAction.Action1> { state, _ ->
                state.copy(bla = state.bla.lowercase())
            }

        // When
        val actual = sut.reduce(ReducerCallbackToStateState("ALTER WERT"), ReducerCallbackToStateAction.Action1)

        // Then
        val expected = ReducerResult(ReducerCallbackToStateState("alter wert"))
        assertThat(actual).isEqualTo(expected)
    }

    sealed class ReducerCallbackToStateAction : Action {
        data object Action1 : ReducerCallbackToStateAction()
    }

    data class ReducerCallbackToStateState(val bla: String) : State {
        override fun clone(): State = this.copy()
    }
}
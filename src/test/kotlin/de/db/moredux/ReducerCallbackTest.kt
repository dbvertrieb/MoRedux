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

class ReducerCallbackTest {

    @Test
    fun `test reduce`() {
        // Given
        val sut = ReducerCallback<ReducerCallbackState, ReducerCallbackAction.Action1> { state, _ ->
            ReducerResult(
                state.copy(bla = state.bla.uppercase()),
                action = ReducerCallbackAction.Action2
            )
        }

        // When
        val actual = sut.reduce(ReducerCallbackState("alter wert"), ReducerCallbackAction.Action1)

        // Then
        val expected = ReducerResult(
            ReducerCallbackState("ALTER WERT"),
            ReducerCallbackAction.Action2
        )
        assertThat(actual).isEqualTo(expected)
    }

    sealed class ReducerCallbackAction : Action {
        data object Action1 : ReducerCallbackAction()
        data object Action2 : ReducerCallbackAction()
    }

    data class ReducerCallbackState(val bla: String) : State {
        override fun clone(): State = this.copy()
    }
}
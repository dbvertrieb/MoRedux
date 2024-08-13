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

class ReducerTest {

    @Test
    fun `test wants`() {
        // Given
        val sut = object : Reducer<ReducerState, ReducerAction.Action1>() {
            override fun reduce(state: ReducerState, action: ReducerAction.Action1): ReducerResult<ReducerState> =
                ReducerResult(state)
        }
        sut.setActionKClass(ReducerAction.Action1::class)

        // When & Then
        assertThat(sut.wants(ReducerAction.Action1)).isTrue()
        assertThat(sut.wants(ReducerAction.Action2)).isFalse()
    }

    @Test
    fun `test reduce`() {
        // Given
        val sut = object : Reducer<ReducerState, ReducerAction.Action1>() {
            override fun reduce(state: ReducerState, action: ReducerAction.Action1): ReducerResult<ReducerState> =
                ReducerResult(
                    state.copy(bla = state.bla.uppercase()),
                    action = ReducerAction.Action2
                )
        }

        // When
        val actual = sut.reduce(ReducerState("alter wert"), ReducerAction.Action1)

        // Then
        val expected = ReducerResult(
            ReducerState("ALTER WERT"),
            ReducerAction.Action2
        )
        assertThat(actual).isEqualTo(expected)
    }

    sealed class ReducerAction : Action {
        data object Action1 : ReducerAction()
        data object Action2 : ReducerAction()
    }

    data class ReducerState(val bla: String) : State {
        override fun clone(): State = this.copy()
    }
}
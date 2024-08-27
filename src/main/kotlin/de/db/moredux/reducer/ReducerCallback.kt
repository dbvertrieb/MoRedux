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

package de.db.moredux.reducer

import de.db.moredux.Action
import de.db.moredux.State

/**
 * The ReducerCallback maps a callback function to a regular Reducer, so no special class extending the abstract Reducer
 * is necessary.
 * The return value type of the callback function is a ReducerResult, same as the return value type of the
 * Reducer.reduce method.
 *
 * This simply reduces boilerplate code.
 */
open class ReducerCallback<STATE : State, ACTION : Action>(
    val code: (STATE, ACTION) -> ReducerResult<STATE>
) : Reducer<STATE, ACTION>() {

    /**
     * @param action the action to process
     * @param state the input state with all data the action should process
     * @return ReducerResult of the processing
     */
    override fun reduce(state: STATE, action: ACTION): ReducerResult<STATE> =
        code(state, action)
}
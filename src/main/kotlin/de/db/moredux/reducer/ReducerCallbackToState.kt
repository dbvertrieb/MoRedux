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

package de.db.moredux.reducer

import de.db.moredux.Action
import de.db.moredux.State

/**
 * The ReducerCallback maps a callback function to a regular Reducer, so no special class extending the abstract Reducer
 * is necessary.
 * The return value type of the callback function is a State. This can be used in case no follow up Action or Effect
 * are returned as result of the Reducer.
 *
 * This reduces even more boilerplate code, than the ReducerCallback
 */
class ReducerCallbackToState<STATE : State, ACTION : Action>(
    val codeToState: (STATE, ACTION) -> STATE
) : ReducerCallback<STATE, ACTION>(
    code = { state: STATE, action: ACTION -> ReducerResult(codeToState.invoke(state, action)) }
)
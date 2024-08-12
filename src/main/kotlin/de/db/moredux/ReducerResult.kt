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

// TODO kdoc
data class ReducerResult<STATE : State>(
    /**
     * the resulting state after an action has processed the input state in the process method of a processor
     */
    val state: STATE,

    /**
     * a follow up action if there is any. The action will get a copy of [state]
     */
    val action: Action? = null,

    /**
     * An effect instance that should be processed as a consequence of the reduce method of a reducer.
     * The effect instance keeps track of whether it was already consumed or not. The effect will get a copy of [state].
     */
    val effect: Effect<STATE>? = null
)
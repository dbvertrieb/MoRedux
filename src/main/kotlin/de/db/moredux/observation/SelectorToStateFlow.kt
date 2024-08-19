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

package de.db.moredux.observation

import de.db.moredux.State
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * As soon as the Selector ist registered, onStateChanged() will be called with the state that is active at that moment.
 * The resulting value of map will be passed to the mutableStateFlow
 */
abstract class SelectorToStateFlow<STATE : State, VALUE>(
    mutableStateFlow: MutableStateFlow<VALUE>
) : Selector<STATE, VALUE>(),
    MutableStateFlow<VALUE> by mutableStateFlow {

    override fun onStateChanged(state: STATE) {
        val value = map(state)
        this.value = value
    }
}
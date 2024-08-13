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

/**
 * An Effect is potentially returned by a Reducer as part of a ReducerResult. When an Effect is executed, it processes
 * the current state, but does not return anything. It may dispatch actions via the passed dispatcher though.
 */
class Effect<STATE : State>(
    val code: (STATE, Dispatcher) -> Unit
) {
    /**
     * If false, this Effect has not been consumed/processed yet.
     * If true, this Effect has been consumed/processed and will not be processed again.
     */
    var isConsumed = false
        private set

    /**
     * @param state a copy of the state, that this Effect works on
     * @param dispatcher a dispatcher to dispatch further actions. The dispatcher is either the Store where the
     * Reducer is registered in that returned this current Effect, or the Stores parent StoreContainer
     */
    fun execute(state: STATE, dispatcher: Dispatcher) {
        if (!isConsumed) {
            code.invoke(state, dispatcher)
            isConsumed = true
            MoReduxLogger.d(this::class, MoReduxSettings.LogMode.MINIMAL, "Effect successfully consumed")
        } else {
            MoReduxLogger.w(
                this::class,
                MoReduxSettings.LogMode.MINIMAL,
                "Effect has already been consumed -> SKIP invocation"
            )
        }
    }
}
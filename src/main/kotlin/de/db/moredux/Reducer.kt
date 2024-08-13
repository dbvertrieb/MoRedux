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

import kotlin.reflect.KClass

/**
 * Abstract reducer that does some basic checking and execution of actions and an API.
 *
 * A Reducer "reduces" a State and an Action into a new State.
 * A Reducer operates on the main thread, so beware of heavy computing.
 */
abstract class Reducer<STATE : State, ACTION : Action> {

    private var actionKClass: KClass<ACTION>? = null

    /**
     * FOR INTERNAL USER ONLY
     *
     * This method has to be public to enable access for an inline method in Store.Builder
     */
    fun setActionKClass(actionKClass: KClass<ACTION>) {
        this.actionKClass = actionKClass
    }

    /**
     * @return if true this Reducer can process the passed [action]
     */
    fun wants(action: Action): Boolean = action::class == actionKClass

    /**
     * Same as [reduce], but its a wrapper for internal processes
     *
     * @param action the action to process
     * @param state the input state with all data the action should process
     * @return ReducerResult of the processing
     */
    internal fun reduceInternal(state: STATE, action: ACTION): ReducerResult<STATE> {
        check(wants(action)) {
            "Internal error. The reducer %s has not been asked whether it wants the action %s. " +
                    "The reduce method has been called illegally".format(
                        this::class.simpleName,
                        action
                    )
        }
        return reduce(state, action)
    }

    /**
     * Reduce the two inputs [state] and [action] to one instance of ReducerResult (the ReducerResult contains
     * the new state + some follow up actions/effects)
     *
     * @param state the state on which the [action] should be performed on
     * @param action the action to process
     * @return the result of this reducer
     */
    abstract fun reduce(state: STATE, action: ACTION): ReducerResult<STATE>

    /**
     * Code that is executed upon Store.teardown() so that only the Store is torn down and not every single component
     *
     * The default case is, that no teardown is needed.
     */
    open fun teardown() {
        // Do your teardown stuff here
    }
}
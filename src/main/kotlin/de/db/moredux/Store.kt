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

package de.db.moredux.de.db.moredux


import java.util.concurrent.atomic.AtomicInteger
import kotlin.reflect.KClass

class Store<STATE : State> private constructor(
    initialState: STATE,
    private val reducers: MutableMap<KClass<*>, Reducer<STATE, Action>>
) : Dispatcher {
    private var _state: STATE = initialState
    private val dispatchCounter = AtomicInteger()

    @Suppress("UNCHECKED_CAST")
    val state: STATE
        get() = _state.clone() as STATE

    val observation = StateObservation(state)

    /**
     * Teardown this store:
     * - teardown observation (clear all observers + remove all observations in selectors)
     * - teardown each reducer
     * - clear the reducer list
     */
    fun teardown() {
        observation.teardown()
        reducers.values.forEach { it.teardown() }
        reducers.clear()
    }

    /**
     * @param action the action to check whether any reducer "wants" it
     * @return if true one of the inherited reducers can process the passed [action]
     */
    fun wants(action: Action): Boolean = reducers.containsKey(action::class)

    /**
     * Dispatch [action] to the reducer who "wants" the action. If no reducer "wants" the [action], then nothing happens
     *
     * @param action the action to dispatch
     * @return if true the [action] could be dispatched, false if no reducer wanted the passed [action]
     */
    override fun dispatch(action: Action): Boolean {
        val currentDispatchCount = dispatchCounter.incrementAndGet()
        ReduxLogger.d(
            this::class,
            ReduxSettings.LogMode.MINIMAL,
            "%d - Dispatch action: %s".format(currentDispatchCount, action)
        )
        return reducers.get(action::class)
            ?.takeIf { reducer -> reducer.wants(action) }
            ?.also {
                ReduxLogger.d(
                    this::class,
                    ReduxSettings.LogMode.FULL,
                    "%d - Found reducer %s for action %s. Start reduction ...".format(
                        currentDispatchCount,
                        it::class.simpleName,
                        action
                    )
                )
            }
            ?.reduceInternal(state, action)
            ?.let { result ->
                ReduxLogger.d(
                    this::class,
                    ReduxSettings.LogMode.FULL,
                    "%d - Finished reduction of action %s".format(currentDispatchCount, action)
                )
                setNewState(currentDispatchCount, result)
                true
            }
            ?: false
    }

    /**
     * Take the current state and publish it to all registered observers
     */
    fun republish() {
        val currentDispatchCount = dispatchCounter.get()
        ReduxLogger.d(
            this::class,
            ReduxSettings.LogMode.FULL,
            "%d - republish current state".format(currentDispatchCount)
        )
        observation.onStateChanged(currentDispatchCount, state)
    }

    /**
     * Set a [state] from the outside and publish it to all registered observers.
     * Rehydration with a state that equals the current state, won't do anything though. Not even a republish.
     *
     * Use this to set a saved state into this store ... if you cannot to do this with the initialState on construction.
     */
    fun rehydrate(state: STATE) {
        val currentDispatchCount = dispatchCounter.incrementAndGet()
        ReduxLogger.d(
            this::class,
            ReduxSettings.LogMode.FULL,
            "%d - rehydrate state".format(currentDispatchCount)
        )
        setNewState(currentDispatchCount, ReducerResult(state))
    }

    /**
     * Process all steps when a new state is set/present - publishing, historical bookkeeping, effect execution
     */
    private fun setNewState(currentDispatchCount: Int, reducerResult: ReducerResult<STATE>) {
        if (_state != reducerResult.state) {
            ReduxLogger.d(
                this::class,
                ReduxSettings.LogMode.FULL,
                "%d - Store new state".format(currentDispatchCount)
            )
            _state = reducerResult.state
            observation.onStateChanged(currentDispatchCount, state)
        } else {
            ReduxLogger.d(
                this::class,
                ReduxSettings.LogMode.FULL,
                "%d - State has not changed -> Skip notifications".format(currentDispatchCount)
            )
        }

        reducerResult.action?.let { action ->
            ReduxLogger.d(
                this::class,
                ReduxSettings.LogMode.FULL,
                "%d - Follow up action %s detected -> pass to dispatch".format(currentDispatchCount, action)
            )
            dispatch(action)
        }
        reducerResult.effect?.let { effect ->
            ReduxLogger.d(
                this::class,
                ReduxSettings.LogMode.FULL,
                "%d - Effect %s detected -> start execution".format(currentDispatchCount, effect)
            )
            effect.execute(reducerResult.state, this)
        }
    }

    @Suppress("UNCHECKED_CAST")
    class Builder<STATE : State> {
        private var initialState: STATE? = null
        val reducers = mutableMapOf<KClass<*>, Reducer<STATE, Action>>()

        fun withInitialState(initialState: STATE): Builder<STATE> = also {
            this.initialState = initialState
        }

        /**
         * Register a reducer that processes [ACTION]
         */
        inline fun <reified ACTION : Action> registerReducer(
            noinline codeToState: (STATE, ACTION) -> STATE
        ): Builder<STATE> = also {
            val reducer = ReducerCallbackToState(codeToState = codeToState)
            registerReducer(reducer)
        }

        /**
         * Register a new reducer:
         * - a reducer may only be registered once
         * - a new reducer must not "want" actions, that are already "wanted" by another registered reducer
         */
        inline fun <reified ACTION : Action> registerReducer(reducer: Reducer<STATE, ACTION>): Builder<STATE> {
            // The same reducer must not be registered twice
            if (reducers.containsValue<KClass<*>, Reducer<STATE, out Action>>(reducer)) {
                ReduxLogger.w(
                    this::class,
                    ReduxSettings.LogMode.FULL,
                    "Reducer has already been registered -> Skipping registration"
                )
                return this
            }

            // Make sure, that the [reducer] processes only actions that no other reducer wants to process
            if (reducers.containsKey(ACTION::class)) {
                ReduxLogger.w(
                    this::class,
                    ReduxSettings.LogMode.FULL,
                    "Reducer wants action (%s) that is already wanted by an already registered reducer (%s) as well " +
                            "-> Skipping registration".format(
                                ACTION::class.simpleName,
                                reducers[ACTION::class]
                            )
                )
                return this
            }

            reducer.setActionKClass(ACTION::class)

            reducers[ACTION::class] = reducer as Reducer<STATE, Action>
            return this
        }

        fun build(): Store<STATE> = Store(
            checkNotNull(initialState) { "InitialState is not set" },
            reducers
        )
    }
}
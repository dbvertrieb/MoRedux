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

package de.db.moredux.store

import de.db.moredux.Action
import de.db.moredux.settings.MoReduxLogger
import de.db.moredux.settings.MoReduxSettings
import de.db.moredux.observation.ObservationManager
import de.db.moredux.reducer.Reducer
import de.db.moredux.reducer.ReducerCallbackToState
import de.db.moredux.reducer.ReducerResult
import de.db.moredux.State
import kotlin.reflect.KClass

/**
 * A Store manages the state (of type [STATE]) and all action dispatching throughout the reducers that are registered
 * at this store.
 */
class Store<STATE : State> private constructor(
    initialState: STATE,
    private val reducers: MutableMap<KClass<*>, Reducer<STATE, Action>>
) : Dispatcher {
    private var _state: STATE = initialState

    @Suppress("UNCHECKED_CAST")
    val state: STATE
        get() = _state.clone() as STATE

    internal val observationManager = ObservationManager(state)

    internal var injectedDispatcher: Dispatcher? = null

    /**
     * Provide a incremental number for logging. In case this Store has been added to a StoreContainer,
     * this dispatchCounter holds an injected DispatchCounter instance of the StoreContainer
     */
    internal var dispatchCounter: DispatchCounter = DispatchCounter()

    /**
     * @return if true, this store has been added to a StoreContainer
     */
    fun isPartOfStoreContainer(): Boolean = injectedDispatcher != null

    /**
     * Teardown this store:
     * - teardown observation (clear all observers + remove all observations in selectors)
     * - teardown each reducer
     * - clear the reducer list
     */
    fun teardown() {
        observationManager.teardown()
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
        MoReduxLogger.d(
            this::class,
            MoReduxSettings.LogMode.MINIMAL,
            "%s Dispatch action: %s".format(currentDispatchCount.createPrefix(), action)
        )
        return reducers.get(action::class)
            ?.takeIf { reducer -> reducer.wants(action) }
            ?.also {
                MoReduxLogger.d(
                    this::class,
                    MoReduxSettings.LogMode.FULL,
                    "%s Found reducer %s for action %s. Start reduction ...".format(
                        currentDispatchCount.createPrefix(),
                        it::class.simpleName,
                        action::class.simpleName
                    )
                )
            }
            // reduction
            ?.reduceInternal(state, action)
            // store new state
            ?.let { result ->
                MoReduxLogger.d(
                    this::class,
                    MoReduxSettings.LogMode.FULL,
                    "%s Finished reduction of action %s".format(
                        currentDispatchCount.createPrefix(),
                        action::class.simpleName
                    )
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
        MoReduxLogger.d(
            this::class,
            MoReduxSettings.LogMode.FULL,
            "%s republish current state".format(currentDispatchCount.createPrefix())
        )
        observationManager.onStateChanged(currentDispatchCount, state)
    }

    /**
     * Set a [state] from the outside and publish it to all registered observers.
     * Rehydration with a state that equals the current state, won't do anything though. Not even a republish.
     *
     * Use this to set a saved state into this store ... if you cannot to do this with the initialState on construction.
     */
    fun rehydrate(state: STATE) {
        val currentDispatchCount = dispatchCounter.incrementAndGet()
        MoReduxLogger.d(
            this::class,
            MoReduxSettings.LogMode.FULL,
            "%s rehydrate state".format(currentDispatchCount.createPrefix())
        )
        setNewState(currentDispatchCount, ReducerResult(state))
    }

    /**
     * Process all steps when a new state is set/present - publishing, historical bookkeeping, effect execution
     */
    private fun setNewState(currentDispatchCount: Int, reducerResult: ReducerResult<STATE>) {
        if (_state != reducerResult.state) {
            MoReduxLogger.d(
                this::class,
                MoReduxSettings.LogMode.FULL,
                "%s Store new state".format(currentDispatchCount.createPrefix())
            )
            _state = reducerResult.state
            observationManager.onStateChanged(currentDispatchCount, state)
        } else {
            MoReduxLogger.d(
                this::class,
                MoReduxSettings.LogMode.FULL,
                "%s State has not changed -> Skip notifications".format(currentDispatchCount.createPrefix())
            )
        }

        reducerResult.action?.let { action ->
            MoReduxLogger.d(
                this::class,
                MoReduxSettings.LogMode.FULL,
                "%s Follow up action %s detected -> pass to dispatch".format(
                    currentDispatchCount.createPrefix(),
                    action::class.simpleName
                )
            )
            resolveDispatcher(currentDispatchCount).dispatch(action)
        }
        reducerResult.effect?.let { effect ->
            MoReduxLogger.d(
                this::class,
                MoReduxSettings.LogMode.FULL,
                "%s Effect %s detected -> start execution".format(currentDispatchCount.createPrefix(), effect)
            )
            effect.execute(reducerResult.state, this)
        }
    }

    private fun resolveDispatcher(currentDispatchCount: Int): Dispatcher =
        injectedDispatcher
            ?.let {
                MoReduxLogger.d(
                    this::class,
                    MoReduxSettings.LogMode.FULL,
                    "%s Use injected dispatcher %s".format(
                        currentDispatchCount.createPrefix(),
                        it::class.simpleName
                    )
                )
                it
            }
            ?: run {
                MoReduxLogger.d(
                    this::class,
                    MoReduxSettings.LogMode.FULL,
                    "%s Use current store %s as dispatcher".format(
                        currentDispatchCount.createPrefix(),
                        this::class.simpleName
                    )
                )
                this
            }

    private fun Int.createPrefix(): String = "%d - Store for %s -".format(this, state::class.simpleName)

    @Suppress("UNCHECKED_CAST")
    class Builder<STATE : State> {
        private var initialState: STATE? = null

        /**
         * Must not be private, because it is used in the inlined registerReducer method below
         */
        val reducers: Map<KClass<*>, Reducer<STATE, Action>> = mutableMapOf()

        /**
         * @param initialState the initialState is mandatory. Without an initial state, the Builder.build() method will
         * throw an Exception
         * @return this Builder for chaining
         */
        fun withInitialState(initialState: STATE): Builder<STATE> = also {
            this.initialState = initialState
        }

        /**
         * Register a reducer that processes [ACTION] and returns a [STATE] (not a ReducerResult) without any
         * follow up actions or effects
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
                MoReduxLogger.w(
                    this::class,
                    MoReduxSettings.LogMode.MINIMAL,
                    "Reducer has already been registered -> Skipping registration"
                )
                return this
            }

            // Make sure, that the [reducer] processes only actions that no other reducer wants to process
            if (reducers.containsKey(ACTION::class)) {
                MoReduxLogger.w(
                    this::class,
                    MoReduxSettings.LogMode.MINIMAL,
                    "Reducer wants action (%s) that is also wanted by an already registered reducer (%s) as well " +
                            "-> Skipping registration".format(
                                ACTION::class.simpleName,
                                reducers[ACTION::class]
                            )
                )
                return this
            }

            reducer.setActionKClass(ACTION::class)

            (reducers as MutableMap<KClass<*>, Reducer<STATE, Action>>)[ACTION::class] =
                reducer as Reducer<STATE, Action>
            return this
        }

        /**
         * @return the built Store
         * @throws IllegalStateException in case the initialState is not set
         */
        fun build(): Store<STATE> = Store(
            checkNotNull(initialState) { "InitialState is not set" },
            reducers as MutableMap<KClass<*>, Reducer<STATE, Action>>
        )
    }
}
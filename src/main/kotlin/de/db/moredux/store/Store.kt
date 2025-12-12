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

package de.db.moredux.store

import de.db.moredux.Action
import de.db.moredux.State
import de.db.moredux.middleware.Middleware
import de.db.moredux.middleware.MiddlewareManager
import de.db.moredux.observation.ObservationManager
import de.db.moredux.reducer.Reducer
import de.db.moredux.reducer.ReducerCallback
import de.db.moredux.reducer.ReducerCallbackToState
import de.db.moredux.reducer.ReducerResult
import de.db.moredux.settings.MoReduxLogger
import de.db.moredux.settings.MoReduxSettings
import kotlin.reflect.KClass

/**
 * A Store manages the state (of type [STATE]) and all action dispatching throughout the reducers that are registered
 * at this store.
 */
class Store<STATE : State> private constructor(
    private val initialState: STATE,
    private val reducers: MutableMap<KClass<*>, Reducer<STATE, Action>>,
    middlewareManagerBuilder: MiddlewareManager.Companion.Builder<STATE>
) : Dispatcher {
    private var _state: STATE = initialState

    @Suppress("UNCHECKED_CAST")
    val state: STATE
        get() = _state.clone() as STATE

    internal val observationManager = ObservationManager(state)

    internal var injectedDispatcher: Dispatcher? = null

    private val middlewareManager: MiddlewareManager<STATE> = middlewareManagerBuilder
            .withStore(this)
            .build()

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
        middlewareManager.teardown()

        reducers.values.forEach { it.teardown() }
        reducers.clear()
    }

    /**
     * @param action the action to check whether any reducer "wants" it
     * @return if true one of the inherited reducers can process the passed [action]
     */
    fun wants(action: Action): Boolean = reducers.containsKey(action::class)

    /**
     * 1. Let the [action] pass through all registered middlewares in the [middlewareManager]
     * 2. Dispatch the [action] to the reducer who "wants" the action.
     *    If no reducer "wants" the [action], then nothing happens
     *
     * @param action the action to dispatch
     */
    override fun dispatch(action: Action) {
        val currentDispatchCount = dispatchCounter.incrementAndGet()
        val logStore = LogStore(currentDispatchCount, state::class)
        logStore.d("Dispatch action: %s".format(action), MoReduxSettings.LogMode.MINIMAL)

        if (middlewareManager.hasMiddleware()) {
            middlewareManager.execute(
                dispatcher = resolveDispatcher(currentDispatchCount),
                state = state,
                action = action,
                currentDispatchCount = currentDispatchCount,
                startReduction = { finalAction -> dispatchReducers(currentDispatchCount, finalAction) }
            )
        } else {
            dispatchReducers(currentDispatchCount, action)
        }
    }

    private fun findReducerForAction(currentDispatchCount: Int, action: Action): Reducer<STATE, Action>? {
        val logStore = LogStore(currentDispatchCount, state::class)
        return reducers[action::class]
                       ?.takeIf { reducer -> reducer.wants(action) }
                       ?.also {
                           logStore.d(
                               "Found reducer %s for action %s.".format(
                                   it::class.simpleName,
                                   action::class.simpleName
                               )
                           )
                       }
               ?: run {
                   logStore.d(
                       "Could not find reducer for action %s -> Quit dispatching".format(action::class.simpleName)
                   )
                   return null
               }
    }

    internal fun dispatchReducers(
        currentDispatchCount: Int,
        action: Action
    ) {
        val logStore = LogStore(currentDispatchCount, state::class)
        findReducerForAction(currentDispatchCount, action)?.let { reducer ->
            // reduction
            reducer.reduceInternal(state, action)
                    // store new state
                    .let { reducerResult ->
                        logStore.d("Finished reduction of action %s".format(action::class.simpleName))
                        setReducerResult(currentDispatchCount, reducerResult)
                    }
        }
    }

    /**
     * Take the current state and publish it to all registered observers
     */
    fun republish() {
        val currentDispatchCount = dispatchCounter.get()
        val logStore = LogStore(currentDispatchCount, state::class)
        logStore.d("republish current state")

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
        val logStore = LogStore(currentDispatchCount, state::class)
        logStore.d("rehydrate state")

        setReducerResult(currentDispatchCount, ReducerResult(state))
    }

    /**
     * Reset the store to its initial state (the one set in the withInitialState of the Builder).
     * All registered reducers will be left untouched.
     */
    fun reset() {
        this._state = initialState
    }

    /**
     * Process all steps when a new state is set/present - publishing, historical bookkeeping, effect execution
     */
    private fun setReducerResult(currentDispatchCount: Int, reducerResult: ReducerResult<STATE>) {
        val logStore = LogStore(currentDispatchCount, state::class)

        if (_state != reducerResult.state) {
            logStore.d("Store new state")
            _state = reducerResult.state
            logStore.d("Publish state change")
            observationManager.onStateChanged(currentDispatchCount, reducerResult.state)
        } else {
            logStore.d("State has not changed -> Skip notifications")
        }

        reducerResult.action?.let { action ->
            logStore.d("Follow up action %s detected -> pass to dispatch".format(action::class.simpleName))
            resolveDispatcher(currentDispatchCount).dispatch(action)
        }
        reducerResult.effect?.let { effect ->
            logStore.d("Effect %s detected -> start execution".format(effect))
            effect.execute(reducerResult.state, this)
        }
    }

    private fun resolveDispatcher(currentDispatchCount: Int): Dispatcher {
        val logStore = LogStore(currentDispatchCount, state::class)

        return injectedDispatcher
                       ?.let {
                           logStore.d("Use injected dispatcher %s".format(it::class.simpleName))
                           it
                       }
               ?: run {
                   logStore.d("Use current store %s as dispatcher".format(this::class.simpleName))
                   this
               }
    }

    @Suppress("UNCHECKED_CAST")
    class Builder<STATE : State> {
        private var initialState: STATE? = null

        private val middlewareManagerBuilder = MiddlewareManager.Companion.Builder<STATE>()

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
        inline fun <reified ACTION : Action> registerReducerToState(
            noinline codeToState: (STATE, ACTION) -> STATE
        ): Builder<STATE> = also {
            val reducer = ReducerCallbackToState(codeToState = codeToState)
            registerReducer(reducer)
        }

        /**
         * Register a reducer that processes [ACTION] and produces a ReducerResult
         */
        inline fun <reified ACTION : Action> registerReducer(
            noinline code: (STATE, ACTION) -> ReducerResult<STATE>
        ): Builder<STATE> = also {
            val reducer = ReducerCallback(code = code)
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
                    clazz = this::class,
                    logMode = MoReduxSettings.LogMode.MINIMAL,
                    message = "Reducer has already been registered -> Skipping registration"
                )
                return this
            }

            // Make sure, that the [reducer] processes only actions that no other reducer wants to process
            if (reducers.containsKey(ACTION::class)) {
                MoReduxLogger.w(
                    clazz = this::class,
                    logMode = MoReduxSettings.LogMode.MINIMAL,
                    message = "Reducer wants action (%s) that is also wanted by an already registered reducer (%s) as well " +
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
         * Register a middleware. Do whatever you want within the middleware, but remember to execute the 'next' lambda
         * that is passed to the middleware. If the 'next' lambda is not executed, the chain of execution and the
         * dispatching will stop.
         *
         * Use this e.g. to do some data loading, logging, rewriting actions or whatever
         */
        fun registerMiddleware(middleware: Middleware<STATE>): Builder<STATE> = also {
            middlewareManagerBuilder.registerMiddleware(middleware)
        }

        /**
         * @return the built Store
         * @throws IllegalStateException in case the initialState is not set
         */
        fun build(): Store<STATE> = Store(
            initialState = checkNotNull(initialState) { "InitialState is not set" },
            reducers = reducers as MutableMap<KClass<*>, Reducer<STATE, Action>>,
            middlewareManagerBuilder = middlewareManagerBuilder
        )
    }
}
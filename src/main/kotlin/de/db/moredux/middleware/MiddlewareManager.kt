package de.db.moredux.middleware

import de.db.moredux.Action
import de.db.moredux.State
import de.db.moredux.settings.MoReduxLogger
import de.db.moredux.settings.MoReduxSettings
import de.db.moredux.store.Dispatcher
import de.db.moredux.store.LogMiddleware
import kotlin.reflect.KClass

internal class MiddlewareManager<STATE : State>(
    private val stateClazz: KClass<out STATE>,
    private val middlewares: MutableList<Container<STATE>>
) {
    /**
     * The middlewares match the [action] whenever
     * - at least one middleware is not bound to any action
     * - or at least one middleware is bound to the passed [action]
     *
     * @param action the action to whether a middleware wants to process the action or not
     * @return if true, the middlewares want to process the action
     */
    internal fun wants(action: Action): Boolean =
        middlewares.any {
            it.actionClazz == null ||
                    it.actionClazz == action::class
        }

    /**
     * Remove all links to the registered middlewares at the end
     */
    internal fun teardown() {
        middlewares.forEach { it.middleware.teardown() }
        middlewares.clear()
    }

    /**
     * Start the execution of the middlewares processing the passed [action]
     *
     * @param dispatcher the dispatcher passed to the middleware for further use
     * @param state the state that was current when the action dispatching started in the parent store
     * @param action the action that is processed
     * @param currentDispatchCount a counter used within logging to make one dispatching process visible in the logs
     * @param startReduction a callback, that will be executed, when the last middleware finished with a "next(…)"
     * execution and reduction starts (execution of the according reducer in the store)
     * @
     */
    internal fun execute(
        dispatcher: Dispatcher,
        state: STATE,
        action: Action,
        currentDispatchCount: Int,
        startReduction: (Action) -> Unit
    ) {
        executeInternal(dispatcher, state, action, currentDispatchCount, 0, startReduction)
    }

    private fun <ACTION : Action> executeInternal(
        dispatcher: Dispatcher,
        state: STATE,
        action: ACTION,
        currentDispatchCount: Int,
        middlewareIndex: Int,
        startReduction: (Action) -> Unit
    ) {
        val logMiddleware = LogMiddleware(currentDispatchCount, middlewareIndex, stateClazz)
        val container = middlewares.getOrNull(middlewareIndex)
        when {
            container == null -> {
                logMiddleware.d("No middleware with index: $middlewareIndex -> Proceed with reducer dispatching")
                startReduction(action)
            }

            container.actionClazz != null && container.actionClazz != action::class -> {
                logMiddleware.d(
                    "Middleware with index: $middlewareIndex is registered for action ${container.actionClazz.simpleName} " +
                            "and does not match action ${action::class.simpleName} " +
                            "-> Continue with next middleware"
                )

                val nextMiddlewareIndex = middlewareIndex + 1
                logMiddleware.d("Pass to middleware with index: $nextMiddlewareIndex")
                executeInternal(
                    dispatcher = dispatcher,
                    state = state,
                    action = action,
                    currentDispatchCount = currentDispatchCount,
                    middlewareIndex = nextMiddlewareIndex,
                    startReduction = startReduction
                )
                logMiddleware.d("Return from middleware with index: $nextMiddlewareIndex")
            }

            container.actionClazz == action::class -> {
                logMiddleware.d("Start execution for action: ${container.actionClazz} ...")

                @Suppress("UNCHECKED_CAST")
                val containerMiddleware = middlewares[middlewareIndex].middleware as MiddlewareForAction<STATE, ACTION>
                containerMiddleware(
                    dispatcher = dispatcher,
                    state = state,
                    action = action,
                    next = { nextAction ->
                        val nextMiddlewareIndex = middlewareIndex + 1
                        logMiddleware.d("Pass to middleware with index: $nextMiddlewareIndex")
                        executeInternal(
                            dispatcher = dispatcher,
                            state = state,
                            action = nextAction,
                            currentDispatchCount = currentDispatchCount,
                            middlewareIndex = nextMiddlewareIndex,
                            startReduction = startReduction
                        )
                        logMiddleware.d("Return from middleware with index: $nextMiddlewareIndex")
                    }
                )
                logMiddleware.d("Finish execution for action: ${container.actionClazz} ...")
            }

            else -> {
                logMiddleware.d("Start execution ...")

                val containerMiddleware = middlewares[middlewareIndex].middleware as Middleware<STATE>
                containerMiddleware(
                    dispatcher = dispatcher,
                    state = state,
                    action = action,
                    next = { nextAction ->
                        val nextMiddlewareIndex = middlewareIndex + 1
                        logMiddleware.d("Pass to middleware with index: $nextMiddlewareIndex")
                        executeInternal(
                            dispatcher = dispatcher,
                            state = state,
                            action = nextAction,
                            currentDispatchCount = currentDispatchCount,
                            middlewareIndex = nextMiddlewareIndex,
                            startReduction = startReduction
                        )
                        logMiddleware.d("Return from middleware with index: $nextMiddlewareIndex")
                    }
                )
                logMiddleware.d("Finish execution ...")
            }
        }
    }

    /**
     * Internal model to manage Middlewares and the action classes they are responsible for, if any
     */
    internal data class Container<STATE : State>(
        val middleware: MiddlewareParent<STATE>,
        val actionClazz: KClass<*>?
    )

    companion object {
        /**
         * Builder for the MiddlewareManager, responsible for all registrations and the action processing
         */
        class Builder<STATE : State> {

            private lateinit var stateClazz: KClass<out STATE>

            private val middlewares = mutableListOf<Container<STATE>>()

            internal fun withState(stateClazz: KClass<out STATE>): Builder<STATE> =
                also { this.stateClazz = stateClazz }

            /**
             * Register a middleware for all actions
             */
            internal fun registerMiddleware(middleware: Middleware<STATE>): Builder<STATE> = also {
                if (!doesMiddlewareExist(middleware)) {
                    middlewares.add(Container(middleware, null))
                }
            }

            /**
             * Register a middleware for exactly one action
             */
            internal fun <ACTION : Action> registerMiddlewareForAction(
                actionClazz: KClass<*>,
                middleware: MiddlewareForAction<STATE, ACTION>
            ): Builder<STATE> = also {
                if (!doesMiddlewareExist(middleware)) {
                    middlewares.add(Container(middleware, actionClazz))
                }
            }

            /**
             * Register a middleware for exactly one action
             */
            private fun doesMiddlewareExist(middleware: MiddlewareParent<STATE>): Boolean =
                if (middlewares.any { it.middleware == middleware }) {
                    MoReduxLogger.w(
                        clazz = this::class,
                        logMode = MoReduxSettings.LogMode.MINIMAL,
                        message = "Middleware has already been registered -> Skipping registration"
                    )
                    true
                } else {
                    false
                }

            fun build(): MiddlewareManager<STATE> {
                assert(this::stateClazz.isInitialized) {
                    "MiddlewareBuilder stateClazz has not been initialized. " +
                            "Building a MiddlewareManager is impossible."
                }
                return MiddlewareManager(stateClazz, middlewares)
            }
        }
    }
}
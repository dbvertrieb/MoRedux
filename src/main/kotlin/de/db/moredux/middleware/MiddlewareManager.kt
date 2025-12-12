package de.db.moredux.middleware

import de.db.moredux.Action
import de.db.moredux.State
import de.db.moredux.settings.MoReduxLogger
import de.db.moredux.settings.MoReduxSettings
import de.db.moredux.store.Dispatcher
import de.db.moredux.store.LogMiddleware
import de.db.moredux.store.Store

internal class MiddlewareManager<STATE : State>(
    private val store: Store<STATE>,
    private val middlewares: MutableList<Middleware<STATE>>
) {
    internal fun teardown() {
        middlewares.clear()
    }

    internal fun hasMiddleware() = middlewares.isNotEmpty()

    internal fun execute(
        dispatcher: Dispatcher,
        state: STATE,
        action: Action,
        currentDispatchCount: Int
    ) {
        executeInternal(dispatcher, state, action, currentDispatchCount, 0)
    }

    private fun executeInternal(
        dispatcher: Dispatcher,
        state: STATE,
        action: Action,
        currentDispatchCount: Int,
        middlewareIndex: Int
    ) {
        val logMiddleware = LogMiddleware(currentDispatchCount, middlewareIndex, store.state::class)
        if (middlewareIndex < middlewares.size) {
            logMiddleware.d("Start execution ...")
            middlewares[middlewareIndex](
                dispatcher = dispatcher,
                state = state,
                action = action,
                next = { nextAction ->
                    val nextMiddlewareIndex = middlewareIndex + 1
                    logMiddleware.d("Pass to middleware with index: $nextMiddlewareIndex")
                    executeInternal(dispatcher, state, nextAction, currentDispatchCount, nextMiddlewareIndex)
                    logMiddleware.d("Return from middleware with index: $nextMiddlewareIndex")
                }
            )
            logMiddleware.d("Finish execution ...")
        } else {
            logMiddleware.d("No middleware with index: $middlewareIndex -> Proceed with reducer dispatching")
            store.dispatchReducers(currentDispatchCount, action)
        }
    }

    companion object {
        class Builder<STATE : State>() {

            private lateinit var store: Store<STATE>

            private val middlewares = mutableListOf<Middleware<STATE>>()

            internal fun withStore(store: Store<STATE>): Builder<STATE> = also { this.store = store }

            /**
             * Register a middleware. Do whatever you want within the middleware, but remember to execute the callback
             * that is passed to the middleware. If the callback is not executed, the chain of execution and the dispatching
             * will stop.
             *
             * Use this e.g. to do some data loading, logging, rewriting actions or whatever
             */
            internal fun registerMiddleware(middleware: Middleware<STATE>): Builder<STATE> = also {
                if (middlewares.contains(middleware)) {
                    MoReduxLogger.w(
                        clazz = this::class,
                        logMode = MoReduxSettings.LogMode.MINIMAL,
                        message = "Middleware has already been registered -> Skipping registration"
                    )
                } else {
                    middlewares.add(middleware)
                }
            }

            fun build(): MiddlewareManager<STATE> = MiddlewareManager(store, middlewares)
        }
    }
}
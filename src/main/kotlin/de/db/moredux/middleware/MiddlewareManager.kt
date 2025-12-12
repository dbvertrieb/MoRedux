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
    private val middlewares: MutableList<Middleware<STATE>>
) {
    internal fun teardown() {
        middlewares.clear()
    }

    internal fun hasMiddleware() =
        middlewares.isNotEmpty()

    internal fun execute(
        dispatcher: Dispatcher,
        state: STATE,
        action: Action,
        currentDispatchCount: Int,
        startReduction: (Action) -> Unit
    ) {
        executeInternal(dispatcher, state, action, currentDispatchCount, 0, startReduction)
    }

    private fun executeInternal(
        dispatcher: Dispatcher,
        state: STATE,
        action: Action,
        currentDispatchCount: Int,
        middlewareIndex: Int,
        startReduction: (Action) -> Unit
    ) {
        val logMiddleware = LogMiddleware(currentDispatchCount, middlewareIndex, stateClazz)
        if (middlewareIndex < middlewares.size) {
            logMiddleware.d("Start execution ...")
            middlewares[middlewareIndex](
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
        } else {
            logMiddleware.d("No middleware with index: $middlewareIndex -> Proceed with reducer dispatching")
            startReduction(action)
        }
    }

    companion object {
        class Builder<STATE : State>() {

            private lateinit var stateClazz: KClass<out STATE>

            private val middlewares = mutableListOf<Middleware<STATE>>()

            internal fun withState(stateClazz: KClass<out STATE>): Builder<STATE> = also { this.stateClazz = stateClazz }

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

            fun build(): MiddlewareManager<STATE> = MiddlewareManager(stateClazz, middlewares)
        }
    }
}
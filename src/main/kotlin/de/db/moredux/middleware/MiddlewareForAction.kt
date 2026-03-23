package de.db.moredux.middleware

import de.db.moredux.Action
import de.db.moredux.State
import de.db.moredux.store.Dispatcher

interface MiddlewareForAction<STATE : State, ACTION : Action> : MiddlewareParent<STATE> {
    /**
     * Same as [Middleware], but this Middleware is only processed/executed in case the [ACTION] is being dispatched.
     * For every other Action, this MiddlewareForAction is skipped.
     *
     * @param dispatcher a dispatcher to dispatch new actions. It's either the store, where this Middleware is
     * registered, or the dispatcher that has been injected in that store.
     * @param state the state as it was at the time, the action has been originally dispatched to the store
     * where this Middleware is registered
     * @param action the action that is currently processed
     * @param next the function or callback that passes an action ([action] or some replacement) to the next middleware
     * IMPORTANT: You can call [next] multiple times, but then the stores dispatch counter will not be incremented +
     * the action will just be processed in the store the middleware has been registered in.
     * If you want proper dispatch counters, or you want to dispatch actions via the stores
     * injected dispatcher, use the passed [dispatcher].
     */
    operator fun invoke(dispatcher: Dispatcher, state: STATE, action: ACTION, next: (Action) -> Unit)
}
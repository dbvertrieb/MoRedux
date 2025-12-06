package de.db.moredux.middleware

import de.db.moredux.Action
import de.db.moredux.State
import de.db.moredux.store.Store

fun interface Middleware<STATE : State> {
    /**
     * @param store the matching store to the middleware - contributes the dispatcher and the state
     * @param action the action that is currently processed
     * @param next the method that passes an action ([action] or some replacement) to the next middleware
     */
    operator fun invoke(store: Store<STATE>, action: Action, next: (Action) -> Any)
}
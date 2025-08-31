package de.db.moredux.middleware

import de.db.moredux.Action
import de.db.moredux.State

fun interface Middleware<STATE : State> {
    /**
     * @param state the current state
     * @param action the action that will be dispatched afterward
     * @param callback execute this callback when your middleware has finished and pass the state.
     * It may be altered in the processing of your middleware
     * @return a MiddlewareResult telling the middleware processing loop how to
     * proceed with the processing loop and what to do with a changed state and
     */
    operator fun invoke(state: STATE, action: Action): MiddlewareResult<STATE>
}
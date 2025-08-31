package de.db.moredux.middleware

import de.db.moredux.State

sealed interface MiddlewareResult<STATE : State> {

    /**
     * Continue processing the next middleware or finish the middleware processing by executing the reducer.
     */
    data class Continue<STATE : State>(
        /**
         * If [state] is set, it will be used for the next middleware or the final reducer execution
         * NOTE: The state will not be directly set as the stores state.
         */
        val state: STATE? = null
    ) : MiddlewareResult<STATE>

    /**
     * Stop processing the middlewares and do not execute the reducer
     */
    data class Break<STATE : State>(
        /**
         * Sorry for the shitty workaround. This is needed to ensure a Middleware<STATE> type safety on
         * middleware registration. The state here is not used anywhere
         */
        val state: STATE? = null
    ) : MiddlewareResult<STATE>
}
package de.db.moredux.preferences

import de.db.moredux.reducer.Reducer
import de.db.moredux.reducer.ReducerResult

class ReducerSetUsername : Reducer<PreferencesState, PreferencesAction.SetUsername>() {

    override fun reduce(
        state: PreferencesState,
        action: PreferencesAction.SetUsername
    ): ReducerResult<PreferencesState> =
        ReducerResult(state = state.copy(username = action.username))
}
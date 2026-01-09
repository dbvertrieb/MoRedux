package de.db.moredux.preferences

import de.db.moredux.reducer.Reducer
import de.db.moredux.reducer.ReducerResult

class ReducerSetDarkMode : Reducer<PreferencesState, PreferencesAction.SetDarkMode>() {

    override fun reduce(
        state: PreferencesState,
        action: PreferencesAction.SetDarkMode
    ): ReducerResult<PreferencesState> =
        ReducerResult(state = state.copy(lightMode = false))
}
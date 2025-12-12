package de.db.moredux.preferences

import de.db.moredux.reducer.Reducer
import de.db.moredux.reducer.ReducerResult

class ReducerSetLightMode : Reducer<PreferencesState, PreferencesAction.SetLightMode>() {

    override fun reduce(
        state: PreferencesState,
        action: PreferencesAction.SetLightMode
    ): ReducerResult<PreferencesState> =
        ReducerResult(state = state.copy(lightMode = true))
}
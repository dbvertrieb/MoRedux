package de.db.moredux.preferences

import de.db.moredux.State

data class PreferencesState(
    val username: String?,
    val lightMode: Boolean
) : State {
    override fun clone(): State = this.copy()

    companion object {
        val INITIAL = PreferencesState(
            username = null,
            lightMode = true
        )
    }
}

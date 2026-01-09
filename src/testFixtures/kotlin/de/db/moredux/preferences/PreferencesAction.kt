package de.db.moredux.preferences

import de.db.moredux.Action

sealed class PreferencesAction: Action {
    data object SetLightMode : PreferencesAction()
    data object SetDarkMode : PreferencesAction()
    data class SetUsername(val username: String) : PreferencesAction()
}
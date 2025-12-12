package de.db.moredux.middleware

import com.google.common.truth.Truth.assertThat
import de.db.moredux.preferences.PreferencesAction
import de.db.moredux.preferences.PreferencesState
import de.db.moredux.preferences.ReducerSetDarkMode
import de.db.moredux.preferences.ReducerSetLightMode
import de.db.moredux.preferences.ReducerSetUsername
import de.db.moredux.store.Store
import org.junit.jupiter.api.Test

class MiddlewareManagerTest {

    @Test
    fun `test simple passthrough middleware`() {
        // Given
        val store = createStorePreferences(lightMode = false)

        // When
        store.dispatch(PreferencesAction.SetLightMode)

        // Then
        assertThat(store.dispatchCounter.get()).isEqualTo(1)
        assertThat(store.state.lightMode).isTrue()
    }

    @Test
    fun `test middleware creates additional action`() {
        // Given
        val store = createStorePreferences(lightMode = true)

        // When
        store.dispatch(PreferencesAction.SetDarkMode)

        // Then
        assertThat(store.dispatchCounter.get()).isEqualTo(2)
        assertThat(store.state.lightMode).isFalse()
        assertThat(store.state.username).isEqualTo("WEREWOLF")
    }

    @Test
    fun `test middleware modifies action and dispatches again via store`() {
        // Given
        val store = createStorePreferences(lightMode = true)

        // When
        store.dispatch(PreferencesAction.SetUsername("vampire"))

        // Then
        assertThat(store.dispatchCounter.get()).isEqualTo(1)
        assertThat(store.state.username).isEqualTo("VAMPIRE")
    }

    /**
     * Middlewares:
     * - Action: SetLightMode -> no modification, no additional action
     * - Action: SetDarkMode -> no modification, additional action that changes the username to "WEREWOLF"
     * - Action: SetUsername -> username is always set the ALL CAPS, no additional action
     */
    private fun createStorePreferences(lightMode: Boolean): Store<PreferencesState> =
        Store.Builder<PreferencesState>()
                .withInitialState(PreferencesState.INITIAL.copy(lightMode = lightMode))
                .registerReducer<PreferencesAction.SetLightMode>(ReducerSetLightMode())
                .registerReducer<PreferencesAction.SetDarkMode>(ReducerSetDarkMode())
                .registerReducer<PreferencesAction.SetUsername>(ReducerSetUsername())
                .registerMiddleware { _, _, action, next ->
                    val newAction = if (action is PreferencesAction.SetUsername) {
                        PreferencesAction.SetUsername(action.username.uppercase())
                    } else {
                        action
                    }
                    next(newAction)
                }
                .registerMiddleware { dispatcher, _, action, next ->
                    next(action)
                    if (action is PreferencesAction.SetDarkMode) {
                        dispatcher.dispatch(PreferencesAction.SetUsername("WEREWOLF"))
                    }
                }
                .registerMiddleware { _, _, action, next ->
                    next(action)
                    if (action is PreferencesAction.SetUsername && action.username.lowercase() == "illegal") {
                        next(action)
                    }
                }
                .build()
}
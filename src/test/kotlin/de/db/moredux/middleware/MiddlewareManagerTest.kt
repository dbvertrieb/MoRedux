package de.db.moredux.middleware

import com.google.common.truth.Truth.assertThat
import de.db.moredux.Action
import de.db.moredux.preferences.PreferencesAction
import de.db.moredux.preferences.PreferencesState
import de.db.moredux.preferences.ReducerSetDarkMode
import de.db.moredux.preferences.ReducerSetLightMode
import de.db.moredux.preferences.ReducerSetUsername
import de.db.moredux.store.Dispatcher
import de.db.moredux.store.Store
import de.db.moredux.todo.TodoAction
import org.junit.jupiter.api.Test

class MiddlewareManagerTest {

    @Test
    fun `test simple passthrough middleware`() {
        // Given
        val store = Store.Builder<PreferencesState>()
                .withInitialState(PreferencesState.INITIAL)
                .registerReducer<PreferencesAction.SetLightMode>(ReducerSetLightMode())
                .registerMiddleware { _, _, action, next -> next(action) }
                .build()

        // When
        store.dispatch(PreferencesAction.SetLightMode)

        // Then
        assertThat(store.dispatchCounter.get()).isEqualTo(1)
        assertThat(store.state.lightMode).isTrue()
    }

    @Test
    fun `test middleware creates additional action`() {
        // Given
        val store = Store.Builder<PreferencesState>()
                .withInitialState(PreferencesState.INITIAL)
                .registerReducer<PreferencesAction.SetDarkMode>(ReducerSetDarkMode())
                .registerReducer<PreferencesAction.SetUsername>(ReducerSetUsername())
                .registerMiddleware { dispatcher, _, action, next ->
                    next(action)
                    if (action is PreferencesAction.SetDarkMode) {
                        dispatcher.dispatch(PreferencesAction.SetUsername("WEREWOLF"))
                    }
                }
                .build()

        // When
        store.dispatch(PreferencesAction.SetDarkMode)

        // Then
        assertThat(store.dispatchCounter.get()).isEqualTo(2)
        assertThat(store.state.lightMode).isFalse()
        assertThat(store.state.username).isEqualTo("WEREWOLF")
    }

    @Test
    fun `test middleware modifies action and dispatches again via next function`() {
        // Given
        val store = Store.Builder<PreferencesState>()
                .withInitialState(PreferencesState.INITIAL)
                .registerReducer<PreferencesAction.SetUsername>(ReducerSetUsername())
                .registerMiddleware { _, _, action, next ->
                    val newAction = if (action is PreferencesAction.SetUsername) {
                        PreferencesAction.SetUsername(action.username.uppercase())
                    } else {
                        action
                    }
                    next(newAction)
                }
                .build()

        // When
        store.dispatch(PreferencesAction.SetUsername("vampire"))

        // Then
        assertThat(store.dispatchCounter.get()).isEqualTo(1)
        assertThat(store.state.username).isEqualTo("VAMPIRE")
    }

    @Test
    fun `test middleware that is registered for just one action`() {
        // Given
        val store = Store.Builder<PreferencesState>()
                .withInitialState(PreferencesState.INITIAL)
                .registerReducer<PreferencesAction.SetUsername>(ReducerSetUsername())
                .registerMiddleware { _, _, action, next ->
                    val newAction = if (action is PreferencesAction.SetUsername) {
                        PreferencesAction.SetUsername(action.username.uppercase())
                    } else {
                        action
                    }
                    next(newAction)
                }
                .registerMiddlewareForAction<PreferencesAction.SetUsername> { _, _, action, next ->
                    val newAction = if (action is PreferencesAction.SetUsername) {
                        PreferencesAction.SetUsername(action.username + " modified")
                    } else {
                        action
                    }
                    next(newAction)
                }
                .registerMiddlewareForAction(PreferencesAction.SetLightMode::class) { _, _, _, next ->
                    next(PreferencesAction.SetDarkMode)
                }
                .build()

        // When
        store.dispatch(PreferencesAction.SetUsername("vampire"))

        // Then
        assertThat(store.dispatchCounter.get()).isEqualTo(1)
        assertThat(store.state.username).isEqualTo("VAMPIRE modified")
        assertThat(store.state.lightMode).isTrue()
    }

    @Test
    fun `test wants with a general middleware`() {
        // Given
        val middlewareManager = MiddlewareManager.Companion.Builder<PreferencesState>()
                .withState(PreferencesState::class)
                .registerMiddleware { _, _, _, _ -> println("Dummy") }
                .build()

        // When & Then
        assertThat(middlewareManager.wants(PreferencesAction.SetLightMode)).isTrue()
        assertThat(middlewareManager.wants(PreferencesAction.SetDarkMode)).isTrue()
        assertThat(middlewareManager.wants(TodoAction.IncrementCounter)).isTrue()
        assertThat(middlewareManager.wants(object : Action {})).isTrue()
    }

    @Test
    fun `test wants with an action specific middleware`() {
        // Given
        val middlewareManager = MiddlewareManager.Companion.Builder<PreferencesState>()
                .withState(PreferencesState::class)
                .registerMiddlewareForAction<PreferencesAction.SetLightMode> { _, _, _, _ ->
                    println("Dummy")
                }
                .build()

        // When & Then
        assertThat(middlewareManager.wants(PreferencesAction.SetLightMode)).isTrue()
        assertThat(middlewareManager.wants(PreferencesAction.SetDarkMode)).isFalse()
        assertThat(middlewareManager.wants(TodoAction.IncrementCounter)).isFalse()
        assertThat(middlewareManager.wants(object : Action {})).isFalse()
    }

    @Test
    fun `test with multiple middleware alternating between general and for a single action`() {
        // Given
        val path = mutableListOf<String>()
        val store = Store.Builder<PreferencesState>()
                .withInitialState(PreferencesState.INITIAL)
                .registerReducer<PreferencesAction.SetLightMode>(ReducerSetLightMode())
                .registerMiddlewareForAction<PreferencesAction.SetUsername> { _, _, action, next ->
                    path.add("SetUsername 1 ${action.username}")
                    next(action)
                }
                .registerMiddleware { _, _, action, next ->
                    path.add("General 1")
                    next(action)
                }
                .registerMiddlewareForAction<PreferencesAction.SetUsername> { _, _, action, next ->
                    path.add("SetUsername 2 ${action.username}")
                    next(action)
                }
                .registerMiddleware { _, _, action, next ->
                    path.add("General 2")
                    next(action)
                }
                .build()

        // When
        store.dispatch(PreferencesAction.SetUsername("Henrik"))

        // Then
        assertThat(store.state.lightMode).isTrue()
        val expected = mutableListOf("SetUsername 1 Henrik", "General 1", "SetUsername 2 Henrik", "General 2")
        assertThat(path).isEqualTo(expected)
    }

}
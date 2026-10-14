/*
 * Copyright 2026, DB Vertrieb GmbH.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.db.moredux.observation

import com.google.common.truth.Truth.assertThat
import de.db.moredux.State
import de.db.moredux.store.Store
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

class StateObservationEventChannelTest {

    private val store: Store<StateObservationEventChannelState> = Store.Builder<StateObservationEventChannelState>()
        .withInitialState(StateObservationEventChannelState())
        .build()
/*
    @Test
    fun `test addSelectorEventChannel with NotificationGuard NoDuplicates drops duplicate values`() = runBlocking {
        // Given
        val flow = store.addSelectorEventChannel(
            notificationGuard = NotificationGuard.NoDuplicates(),
        ) { state -> state.bla }

        // When - the second state change maps to the same value as the first one
        store.observationManager.onStateChanged(currentDispatchCount = 0, state = StateObservationEventChannelState(bla = "State counter 0"))
        store.observationManager.onStateChanged(currentDispatchCount = 1, state = StateObservationEventChannelState(bla = "State counter 0"))
        store.observationManager.onStateChanged(currentDispatchCount = 2, state = StateObservationEventChannelState(bla = "State counter 2"))

        // Then
        val values = flow.take(2).toList()
        assertThat(values).containsExactly("State counter 0", "State counter 2").inOrder()
    }
    */

/*
    @Test
    fun `test addSelectorEventChannel with NotificationGuard AlwaysNotify emits duplicate values`() = runBlocking {
        // Given
        val flow = store.addSelectorEventChannel(
            notificationGuard = NotificationGuard.AlwaysNotify(),
        ) { state -> state.bla }

        // When - the second state change maps to the same value as the first one
        store.observationManager.onStateChanged(currentDispatchCount = 0, state = StateObservationEventChannelState(bla = "State counter 0"))
        store.observationManager.onStateChanged(currentDispatchCount = 1, state = StateObservationEventChannelState(bla = "State counter 0"))

        // Then
        val values = flow.take(2).toList()
        assertThat(values).containsExactly("State counter 0", "State counter 0").inOrder()
    }
*/
    @Test
    fun `test addSelectorEventChannel with default conflated capacity behaves like a single live event`() = runBlocking {
        // Given
        val flow = store.addSelectorEventChannel { state -> state.bla }

        // When - two values are sent before anyone collects from the flow
        store.observationManager.onStateChanged(currentDispatchCount = 0, state = StateObservationEventChannelState(bla = "State counter 0"))
        store.observationManager.onStateChanged(currentDispatchCount = 1, state = StateObservationEventChannelState(bla = "State counter 1"))

        // Then - only the most recently sent value survives, the stale one got conflated away
        val value = flow.take(1).toList().first()
        assertThat(value).isEqualTo("State counter 1")
    }

    @Test
    fun `test addSelectorEventChannel with capacity other than conflated keeps all values`() = runBlocking {
        // Given
        val flow = store.addSelectorEventChannel(capacity = Channel.UNLIMITED) { state -> state.bla }

        // When - two values are sent before anyone collects from the flow
        store.observationManager.onStateChanged(currentDispatchCount = 0, state = StateObservationEventChannelState(bla = "State counter 0"))
        store.observationManager.onStateChanged(currentDispatchCount = 1, state = StateObservationEventChannelState(bla = "State counter 1"))

        // Then - both values are still available, nothing got conflated away
        val values = flow.take(2).toList()
        assertThat(values).containsExactly("State counter 0", "State counter 1").inOrder()
    }

    @Test
    fun `test addSelectorEventChannel emits a value on a simple state change`() = runBlocking {
        // Given
        val flow = store.addSelectorEventChannel { state -> state.bla }

        // When
        store.observationManager.onStateChanged(currentDispatchCount = 0, state = StateObservationEventChannelState(bla = "State counter 0"))

        // Then
        val value = flow.take(1).toList().first()
        assertThat(value).isEqualTo("State counter 0")
    }
/*
    @Test
    fun `test addSelectorEventChannel with observer invokes observer for every mapped value`() = runBlocking {
        // Given
        val observedValues = mutableListOf<String?>()
        val flow = store.addSelectorEventChannel(
            notificationGuard = NotificationGuard.AlwaysNotify(),
            observer = { value -> observedValues.add(value) },
        ) { state -> state.bla }

        // When - the second state change maps to the same value as the first one, but the observer is
        // still notified both times, independent of the channel's NotificationGuard
        store.observationManager.onStateChanged(currentDispatchCount = 0, state = StateObservationEventChannelState(bla = "State counter 0"))
        store.observationManager.onStateChanged(currentDispatchCount = 1, state = StateObservationEventChannelState(bla = "State counter 0"))

        // Then
        assertThat(observedValues).containsExactly("State counter 0", "State counter 0").inOrder()
        val values = flow.take(2).toList()
        assertThat(values).containsExactly("State counter 0", "State counter 0").inOrder()
    }
*/
    /*
    @Test
    fun `test addSelectorEventChannel with observer and NotificationGuard NoDuplicates drops duplicate channel values`() = runBlocking {
        // Given
        val observedValues = mutableListOf<String?>()
        val flow = store.addSelectorEventChannel(
            notificationGuard = NotificationGuard.NoDuplicates(),
            observer = { value -> observedValues.add(value) },
        ) { state -> state.bla }

        // When - the second state change maps to the same value as the first one
        store.observationManager.onStateChanged(currentDispatchCount = 0, state = StateObservationEventChannelState(bla = "State counter 0"))
        store.observationManager.onStateChanged(currentDispatchCount = 1, state = StateObservationEventChannelState(bla = "State counter 0"))
        store.observationManager.onStateChanged(currentDispatchCount = 2, state = StateObservationEventChannelState(bla = "State counter 2"))

        // Then - the observer only sees distinct values since it's fed by the same selector as the channel
        assertThat(observedValues).containsExactly("State counter 0", "State counter 2").inOrder()
        val values = flow.take(2).toList()
        assertThat(values).containsExactly("State counter 0", "State counter 2").inOrder()
    }
*/
    data class StateObservationEventChannelState(val bla: String? = null) : State {
        override fun clone(): State = this.copy()
    }
}

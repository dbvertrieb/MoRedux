/*
 * Copyright 2023 the original author or authors.
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

import de.db.moredux.settings.MoReduxLogger
import de.db.moredux.settings.MoReduxSettings
import de.db.moredux.State

/**
 * The ObservationManager class handles all StateObservers and notifications upon state changes
 */
internal class ObservationManager<STATE : State>(
    /**
     * This state is needed in case a registration below demands an immediate processing of the current state
     */
    private var state: STATE
) {

    private val stateObservers = mutableListOf<StateObserver<STATE>>()

    /**
     * Register a [stateObserver] that will be notified everytime the state changes. A StateObserver may only
     * be registered once.
     *
     * @param processCurrentStateImmediately if true, the current state will be pushed into the passed [stateObserver]
     * @param stateObserver the StateObserver instance that listens upon onStateChanged
     * @return the [stateObserver]
     */
    internal fun addStateObserver(
        processCurrentStateImmediately: Boolean = false,
        stateObserver: StateObserver<STATE>
    ): StateObserver<STATE> {
        if (!stateObservers.contains(stateObserver)) {
            stateObservers.add(stateObserver)
        }
        if (processCurrentStateImmediately) {
            stateObserver.onStateChanged(state)
        }
        return stateObserver
    }

    /**
     * Remove the passed [stateObserver] from the observer list. May be a StateObserver instance including Selectors
     */
    internal fun removeObserver(stateObserver: StateObserver<STATE>) {
        stateObservers.remove(stateObserver)
    }

    /**
     * - Remove all observers of the registered Selectors
     * - Clear all StateObservers from the list
     */
    internal fun teardown() {
        stateObservers.forEach { if (it is Selector<*, *>) it.removeAllSelectorObservers() }
        stateObservers.clear()
    }

    internal fun onStateChanged(currentDispatchCount: Int, state: STATE) {
        this.state = state
        stateObservers.takeIf { it.isNotEmpty() }
            ?.also {
                MoReduxLogger.d(
                    this::class,
                    MoReduxSettings.LogMode.FULL,
                    "%d - Start notifying %d observers".format(currentDispatchCount, it.size)
                )
            }
            ?.forEach { it.onStateChanged(state) }
            ?: MoReduxLogger.d(
                this::class,
                MoReduxSettings.LogMode.FULL,
                "%d - No observers present -> Skip notifications".format(currentDispatchCount)
            )
    }
}
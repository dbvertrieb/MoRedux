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

package de.db.moredux

/**
 * The StateObservation class handles all StateObservers and notifications upon state changes
 */
// TODO refactor - move all none basic add / remove into Extensions (extending Store)
class StateObservation<STATE : State>(
    /**
     * This state is needed in case a registration below demands an immediate processing of the current state
     */
    private var state: STATE
) {

    private val stateObservers = mutableListOf<StateObserver<STATE>>()

    /**
     * Register a [selector] that will be notified everytime the state changes.
     * The [selector] will be treated like any other registered StateObserver
     *
     * @param processCurrentStateImmediately if true, the current state will be pushed into the passed [selector]
     * @param selector the Selector instance that is registered as StateObserver
     * @return the [selector] instance
     */
    fun <VALUE> addSelector(
        processCurrentStateImmediately: Boolean,
        selector: Selector<STATE, VALUE>
    ): Selector<STATE, VALUE> {
        addStateObserver(processCurrentStateImmediately, selector)
        return selector
    }

    /**
     * Register a [select] as a Selector. The [select] will be treated like any other Selector.
     *
     * @param processCurrentStateImmediately if true, the current state will be pushed into the passed [select]
     * @param select the [select] function maps the state [STATE] to [VALUE]. The [VALUE] is published to the Selector
     * @return the Selector instance that was constructed out of [select]
     * @see addSelector
     */
    fun <VALUE> addSelector(
        processCurrentStateImmediately: Boolean = false,
        select: (STATE) -> VALUE
    ): Selector<STATE, VALUE> {
        val selector = object : Selector<STATE, VALUE>() {
            override fun map(state: STATE): VALUE = select(state)
        }
        return addSelector(processCurrentStateImmediately, selector)
    }

    /**
     * Register a [stateObserver] that will be notified everytime the state changes. A StateObserver may only
     * be registered once.
     *
     * @param processCurrentStateImmediately if true, the current state will be pushed into the passed [stateObserver]
     * @param stateObserver the StateObserver instance that listens upon onStateChanged
     * @return the [stateObserver]
     */
    fun addStateObserver(
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
     * Register a [callback] as a StateObserver. The [callback] will be treated like any other StateObserver
     *
     * @param processCurrentStateImmediately if true, the current state will be pushed into the passed [callback]
     * @param callback the callback function with which a StateObserver is constructed
     * @return the StateObserver instance that was constructed out of [callback]
     * @see addStateObserver
     */
    fun addStateObserver(
        processCurrentStateImmediately: Boolean = false,
        callback: (STATE) -> Unit
    ): StateObserver<STATE> =
        addStateObserver(processCurrentStateImmediately, CallbackStateObserver(callback))

    /**
     * Remove the passed [stateObserver] from the observer list. May be a StateObserver instance including Selectors
     */
    fun removeObserver(stateObserver: StateObserver<STATE>) {
        stateObservers.remove(stateObserver)
    }

    /**
     * - Remove all observers of the registered Selectors
     * - Clear all StateObservers from the list
     */
    fun teardown() {
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
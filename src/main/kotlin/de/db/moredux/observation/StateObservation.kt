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

import de.db.moredux.State
import de.db.moredux.settings.MoReduxLogger
import de.db.moredux.settings.MoReduxSettings
import de.db.moredux.store.Store
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Register a [stateObserver] that will be notified everytime the state changes. A StateObserver may only
 * be registered once.
 *
 * @param processCurrentStateImmediately if true, the current state will be pushed into the passed [stateObserver]
 * @param stateObserver the StateObserver instance that listens upon onStateChanged
 * @return the [stateObserver]
 */
fun <STATE : State> Store<STATE>.addStateObserver(
    processCurrentStateImmediately: Boolean = false,
    stateObserver: StateObserver<STATE>
): StateObserver<STATE> =
    this.observationManager.addStateObserver(processCurrentStateImmediately, stateObserver)

/**
 * Register a [callback] as a StateObserver. The [callback] will be treated like any other StateObserver
 *
 * @param processCurrentStateImmediately if true, the current state will be pushed into the passed [callback]
 * @param callback the callback function with which a StateObserver is constructed
 * @return the StateObserver instance that was constructed out of [callback]
 * @see ObservationManager.addStateObserver
 */
fun <STATE : State> Store<STATE>.addStateObserver(
    processCurrentStateImmediately: Boolean = false,
    callback: (STATE) -> Unit
): StateObserver<STATE> =
    this.observationManager.addStateObserver(processCurrentStateImmediately, CallbackStateObserver(callback))

/**
 * Register a [selector] that will be notified everytime the state changes.
 * The [selector] will be treated like any other registered StateObserver
 *
 * @param processCurrentStateImmediately if true, the current state will be pushed into the passed [selector]
 * @param selector the Selector instance that is registered as StateObserver
 * @return the [selector] instance
 */
fun <STATE : State, VALUE> Store<STATE>.addSelector(
    processCurrentStateImmediately: Boolean,
    selector: Selector<STATE, VALUE>
): Selector<STATE, VALUE> {
    this.observationManager.addStateObserver(processCurrentStateImmediately, selector)
    return selector
}

/**
 * Register a [map] as a Selector. The [map] will be treated like any other Selector.
 *
 * @param processCurrentStateImmediately if true, the current state will be pushed into the passed [map]
 * @param observer in order to be able to process the current state immediately, an observer function has
 * to be passed as argument here. The Selector is created inside this function, which makes it impossible to set any
 * observer function before processing the current state. If [processCurrentStateImmediately] is true anf [observer]
 * is null, then you will simply loose the current state. Every follow up state will be observed correctly though.
 * @param map the [map] function maps the state [STATE] to [VALUE]. The [VALUE] is published to the Selector
 * @return the Selector instance that was constructed out of [map]
 * @see addSelector
 */
fun <STATE : State, VALUE> Store<STATE>.addSelector(
    processCurrentStateImmediately: Boolean,
    observer: ((VALUE) -> Unit)? = null,
    map: (STATE) -> VALUE
): Selector<STATE, VALUE> {
    val selector = object : Selector<STATE, VALUE>() {
        override fun map(state: STATE): VALUE = map(state)
    }

    if (observer == null && processCurrentStateImmediately) {
        MoReduxLogger.w(
            ObservationManager::class,
            MoReduxSettings.LogMode.MINIMAL,
            "Adding selector with processCurrentStateImmediately set, but without an observer function -> " +
                    "The current state will not be published anywhere by this Selector."
        )
    }

    observer?.let { selector.observeSelector { value -> observer(value) } }
    return addSelector(processCurrentStateImmediately, selector)
}

/**
 * Register a [map] as a Selector that extends a Kotlin coroutines MutableStateFlow.
 * The [map] will be treated like any other Selector.
 *
 * @param initialValue the initial value o the created StateFlow
 * @param processCurrentStateImmediately if true, the current state will be pushed into the passed [map]
 * @param map the [map] function maps the state [STATE] to [VALUE]. The [VALUE] is published to the created MutableStateFlow
 * @return the SelectorToStateFlow instance that was constructed out of [map] - it's a MutableStateFlow under the hood
 * @see addSelector
 */
fun <STATE : State, VALUE> Store<STATE>.addSelectorStateFlow(
    initialValue: VALUE,
    processCurrentStateImmediately: Boolean = false,
    map: (STATE) -> VALUE
): SelectorToStateFlow<STATE, VALUE> {
    val mutableStateFlow = MutableStateFlow(initialValue)
    val selector = object : SelectorToStateFlow<STATE, VALUE>(mutableStateFlow) {
        override fun map(state: STATE): VALUE = map(state)
    }
    addSelector(processCurrentStateImmediately, selector)
    return selector
}

/**
 * Remove the passed [stateObserver] from the observer list. This may be any StateObserver instance including Selectors
 */
fun <STATE : State> Store<STATE>.removeObserver(stateObserver: StateObserver<STATE>) {
    this.observationManager.removeObserver(stateObserver)
}


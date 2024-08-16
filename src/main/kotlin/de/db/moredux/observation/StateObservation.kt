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
import de.db.moredux.store.Store

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
 * Register a [select] as a Selector. The [select] will be treated like any other Selector.
 *
 * @param processCurrentStateImmediately if true, the current state will be pushed into the passed [select]
 * @param select the [select] function maps the state [STATE] to [VALUE]. The [VALUE] is published to the Selector
 * @return the Selector instance that was constructed out of [select]
 * @see addSelector
 */
// TODO test
fun <STATE : State, VALUE> Store<STATE>.addSelector(
    processCurrentStateImmediately: Boolean = false,
    select: (STATE) -> VALUE
): Selector<STATE, VALUE> {
    val selector = object : Selector<STATE, VALUE>() {
        override fun map(state: STATE): VALUE = select(state)
    }
    return addSelector(processCurrentStateImmediately, selector)
}

/**
 * Remove the passed [stateObserver] from the observer list. May be a StateObserver instance including Selectors
 */
// TODO test
fun <STATE : State> Store<STATE>.removeObserver(stateObserver: StateObserver<STATE>) {
    this.observationManager.removeObserver(stateObserver)
}


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

/**
 * A Selector is a special StateObserver, that is also an observable (see [observeSelector] and
 * [removeAllSelectorObservers]). Using the [map] method, the Selector provides a fraction of data of the state,
 * whenever the state changes. Which part of the state and whether that part of the state is further modified is up to
 * the developer/use case.
 *
 * Example:
 * A State holds a list of Todos. One Selector provides a list Todos that have not been finished. Another Selector
 * provides the count of Todos that have been finished according to the current state.
 */
abstract class Selector<STATE : State, VALUE> : StateObserver<STATE> {

    private val observerList = mutableListOf<(VALUE) -> Unit>()

    abstract fun map(state: STATE): VALUE

    override fun onStateChanged(state: STATE) {
        val value = map(state)
        notifyObservers(value)
    }

    protected fun notifyObservers(value:VALUE) {
        observerList.forEach { observer -> observer.invoke(value) }
    }

    fun observeSelector(observer: (VALUE) -> Unit) {
        observerList.add(observer)
    }

    fun removeAllSelectorObservers() {
        observerList.clear()
    }
}
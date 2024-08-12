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

// TODO Move the MutableLiveData dependent Selector to another OS project, that provides Android Livecycle Utilities
// TODO Same with Compose State support

// TODO kdoc still fine?
/**
 * As soon as the Selector ist registered, onStateChanged() will be called with the state that is active at that moment
 */
abstract class Selector<STATE : State, VALUE> : StateObserver<STATE> {

    private val observerList = mutableListOf<(VALUE) -> Unit>()

    abstract fun map(state: STATE): VALUE

    override fun onStateChanged(state: STATE) {
        val value = map(state)
        observerList.forEach { observer -> observer.invoke(value) }
    }

    fun observeSelector(observer: (VALUE) -> Unit) {
        observerList.add(observer)
    }

    fun removeAllSelectorObservers() {
        observerList.clear()
    }
}
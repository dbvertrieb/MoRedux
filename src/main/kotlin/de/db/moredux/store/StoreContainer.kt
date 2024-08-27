/*
 * Copyright 2024, DB Vertrieb GmbH.
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

package de.db.moredux.store

import de.db.moredux.Action
import de.db.moredux.settings.MoReduxLogger
import de.db.moredux.settings.MoReduxSettings
import de.db.moredux.State

/**
 * A StoreContainer is a container for multiple dispatchers (Store + StoreContainer).
 * An Action passed to the StoreContainers dispatch method is passed on to the first dispatcher,
 * that wants that action. As of now, dispatching a single action to multiple Dispatcher is forbidden.
 */
class StoreContainer private constructor(
    private val stores: MutableList<Store<*>> = mutableListOf()
) : Dispatcher {

    private var dispatchCounter = DispatchCounter()
    val currentDispatchCount: Int
        get() = dispatchCounter.get()

    init {
        stores.forEach {
            it.injectedDispatcher = this
            it.dispatchCounter = dispatchCounter
        }
    }

    /**
     * @return if true, than one of the stores contained in this StoreContainer, wants the passed
     * [action]
     */
    fun wants(action: Action): Boolean = stores.any { it.wants(action) }

    /**
     * actions and effects will be executed immediately by the store it is registered in
     *
     * @return true, if at least one store returned a successful dispatch
     */
    override fun dispatch(action: Action): Boolean {
        var wasDispatched = false
        stores.filter { it.wants(action) }
            .forEach {
                MoReduxLogger.d(
                    this::class,
                    MoReduxSettings.LogMode.FULL,
                    "%d - Store for %s wants action %s -> START dispatching".format(
                        dispatchCounter.incrementAndGet(),
                        it.state::class.simpleName,
                        action::class.simpleName
                    )
                )
                wasDispatched = it.dispatch(action) || wasDispatched
            }
        return wasDispatched
    }

    /**
     * Teardown all stores and clear the list of registered stores
     */
    fun teardown() {
        stores.forEach { it.teardown() }
        stores.clear()
    }

    class Builder {
        /**
         * Must not be private, because it is used in the inlined registerReducer method below
         */
        val stores: List<Store<*>> = mutableListOf()

        /**
         * @param store add this store to the StoreContainer
         * @return this Builder for chaining
         */
        inline fun <reified STATE : State> addStore(store: Store<STATE>) = also {
            when {
                stores.contains(store) ->
                    MoReduxLogger.w(
                        this::class,
                        MoReduxSettings.LogMode.MINIMAL,
                        "Store for %s has already been added to the store list -> Skipping add"
                            .format(STATE::class.simpleName)
                    )

                store.isPartOfStoreContainer() ->
                    MoReduxLogger.w(
                        this::class,
                        MoReduxSettings.LogMode.MINIMAL,
                        "Store for %s has already been added to another StoreContainer -> Skipping add"
                            .format(STATE::class.simpleName)
                    )

                else -> {
                    (stores as MutableList<Store<*>>).add(store)
                }
            }
        }

        /**
         * @return the built StoreContainer
         */
        fun build(): StoreContainer {
            if (stores.isEmpty()) {
                MoReduxLogger.w(
                    this::class,
                    MoReduxSettings.LogMode.MINIMAL,
                    "No stores set in Builder -> Create empty StoreContainer"
                )
            }
            return StoreContainer(stores as MutableList<Store<*>>)
        }
    }
}
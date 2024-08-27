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

package de.db.moredux

/**
 * All MoRedux stores need a state, that implements this State interface. This gives the MoRedux states a namespace
 * and ensures, that a State can be copied/cloned at anytime.
 *
 * Make sure the State implementation contains only immutable members!
 */
interface State {
    /**
     * This clone method has to be implemented by the state, because there is no way to enforce the usage of a data
     * class that offers a copy() method
     */
    fun clone(): State
}
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

package de.db.moredux.de.db.moredux

import java.util.logging.Level
import java.util.logging.Logger
import kotlin.reflect.KClass

// TODO kdoc

object ReduxLogger {

    private const val NO_NAME_CLASS = "<No name class>"

    fun d(clazz: KClass<*>, logMode: ReduxSettings.LogMode, message: String) {
        if (isAllowed(logMode)) {
            ReduxSettings.logDebug
                ?.let { it(clazz.simpleName, message) }
                ?: getLogger(clazz).log(Level.FINE, message)
        }
    }

    fun w(clazz: KClass<*>, logMode: ReduxSettings.LogMode, message: String) {
        if (isAllowed(logMode)) {
            ReduxSettings.logWarn
                ?.let { it(clazz.simpleName, message) }
                ?: getLogger(clazz).log(Level.WARNING, message)
        }
    }

    private fun isAllowed(logMode: ReduxSettings.LogMode): Boolean =
        logMode == ReduxSettings.LogMode.FULL ||
                logMode == ReduxSettings.LogMode.MINIMAL && ReduxSettings.logMode == ReduxSettings.LogMode.MINIMAL

    private fun getLogger(clazz: KClass<*>) = Logger.getLogger(clazz.simpleName ?: NO_NAME_CLASS)
}
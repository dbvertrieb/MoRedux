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

import java.util.logging.Level
import java.util.logging.Logger
import kotlin.reflect.KClass

/**
 * This logging helper ensures independence from platform specific logging frameworks and provides a
 * fallback logging, that uses native Java logging mechanisms.
 */
internal object MoReduxLogger {

    /**
     * Default logger name / log tag
     */
    private const val NO_NAME_CLASS = "<No name class>"

    /**
     * Log with "debug" severity (Java log level: FINE)
     */
    fun d(clazz: KClass<*>, logMode: MoReduxSettings.LogMode, message: String) {
        if (isAllowed(logMode)) {
            MoReduxSettings.logDebug
                ?.let { customLogger -> customLogger(getLogTag(clazz), message) }
                ?: getLogger(clazz).log(Level.FINE, message)
        }
    }

    /**
     * Log with "warning" severity (Java log level: WARNING)
     */
    fun w(clazz: KClass<*>, logMode: MoReduxSettings.LogMode, message: String) {
        if (isAllowed(logMode)) {
            MoReduxSettings.logWarn
                ?.let { customLogger -> customLogger(getLogTag(clazz), message) }
                ?: getLogger(clazz).log(Level.WARNING, message)
        }
    }

    /**
     * Log level guard. Allows all, only a fraction or no logs at all.
     */
    private fun isAllowed(logMode: MoReduxSettings.LogMode): Boolean =
        logMode == MoReduxSettings.LogMode.FULL ||
                logMode == MoReduxSettings.LogMode.MINIMAL && MoReduxSettings.logMode == MoReduxSettings.LogMode.MINIMAL

    private fun getLogger(clazz: KClass<*>) = Logger.getLogger(getLogTag(clazz))

    private fun getLogTag(clazz: KClass<*>): String = clazz.simpleName ?: NO_NAME_CLASS
}
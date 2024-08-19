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

package de.db.moredux.settings

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.MethodSource

class MoReduxLoggerTest {

    @BeforeEach
    fun setup() {
        MoReduxSettings.logMode = MoReduxSettings.LogMode.FULL
    }

    @AfterEach
    fun tearDown() {
        MoReduxSettings.logMode = MoReduxSettings.LogMode.MINIMAL
    }

    @ParameterizedTest
    @MethodSource("testDataLogModesDebug")
    fun `test debug logging with all logModes`(
        logModeInSettings: MoReduxSettings.LogMode,
        logModeOnLog: MoReduxSettings.LogMode,
        wasLogged: Boolean
    ) {
        // Given
        var actualTag: String? = null
        var actualMessage: String? = null
        MoReduxSettings.logDebug = { tag, message ->
            actualTag = tag
            actualMessage = message
        }
        MoReduxSettings.logMode = logModeInSettings

        // When
        MoReduxLogger.d(MoReduxLogger::class, logModeOnLog, "Message text")

        // Then
        if (wasLogged) {
            assertThat(actualTag).isEqualTo("MoRedux.MoReduxLogger")
            assertThat(actualMessage).isEqualTo("Message text")
        } else {
            assertThat(actualTag).isNull()
            assertThat(actualMessage).isNull()
        }
    }

    @ParameterizedTest
    @MethodSource("testDataLogModesWarning")
    fun `test warn logging with all logModes`(
        logModeInSettings: MoReduxSettings.LogMode,
        logModeOnLog: MoReduxSettings.LogMode,
        wasLogged: Boolean
    ) {
        // Given
        var actualTag: String? = null
        var actualMessage: String? = null
        MoReduxSettings.logWarn = { tag, message ->
            actualTag = tag
            actualMessage = message
        }
        MoReduxSettings.logMode = logModeInSettings

        // When
        MoReduxLogger.w(MoReduxLogger::class, logModeOnLog, "Message text")

        // Then
        if (wasLogged) {
            assertThat(actualTag).isEqualTo("MoRedux.MoReduxLogger")
            assertThat(actualMessage).isEqualTo("Message text")
        } else {
            assertThat(actualTag).isNull()
            assertThat(actualMessage).isNull()
        }
    }

    companion object {
        @JvmStatic
        fun testDataLogModesDebug() = listOf(
            arguments(MoReduxSettings.LogMode.FULL, MoReduxSettings.LogMode.FULL, true),
            arguments(MoReduxSettings.LogMode.FULL, MoReduxSettings.LogMode.MINIMAL, true),
            arguments(MoReduxSettings.LogMode.MINIMAL, MoReduxSettings.LogMode.FULL, false),
            arguments(MoReduxSettings.LogMode.MINIMAL, MoReduxSettings.LogMode.MINIMAL, true),
            arguments(MoReduxSettings.LogMode.DISABLED, MoReduxSettings.LogMode.FULL, false),
            arguments(MoReduxSettings.LogMode.DISABLED, MoReduxSettings.LogMode.MINIMAL, false)
        )

        @JvmStatic
        fun testDataLogModesWarning() = listOf(
            arguments(MoReduxSettings.LogMode.FULL, MoReduxSettings.LogMode.FULL, true),
            arguments(MoReduxSettings.LogMode.FULL, MoReduxSettings.LogMode.MINIMAL, true),
            arguments(MoReduxSettings.LogMode.MINIMAL, MoReduxSettings.LogMode.FULL, false),
            arguments(MoReduxSettings.LogMode.MINIMAL, MoReduxSettings.LogMode.MINIMAL, true),
            arguments(MoReduxSettings.LogMode.DISABLED, MoReduxSettings.LogMode.FULL, false),
            arguments(MoReduxSettings.LogMode.DISABLED, MoReduxSettings.LogMode.MINIMAL, false)
        )
    }
}
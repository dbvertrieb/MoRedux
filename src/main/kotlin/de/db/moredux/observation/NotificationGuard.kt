package de.db.moredux.observation

interface NotificationGuard<VALUE> {
    fun shouldNotify(value: VALUE): Boolean

    class AlwaysNotify<VALUE> : NotificationGuard<VALUE> {
        override fun shouldNotify(value: VALUE): Boolean = true
    }

    class NoDuplicates<VALUE> : NotificationGuard<VALUE> {
        /**
         * Holds the previously mapped value to detect changes between state updates.
         * Initialized with [UNINITIALIZED] as a sentinel object to distinguish between
         * an actual `null` value and an unset state.
         */
        private var previousValue: Any? = UNINITIALIZED

        override fun shouldNotify(value: VALUE): Boolean {
            val hasValueChanged = value != previousValue

            // remember the new value after the has-changed-check
            previousValue = value

            return hasValueChanged
        }

        private companion object {
            private val UNINITIALIZED = Any()
        }
    }
}
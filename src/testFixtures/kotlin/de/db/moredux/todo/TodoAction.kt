package de.db.moredux.todo

import de.db.moredux.Action

// Define the possible actions
sealed class TodoAction : Action {
    /**
     * Add another todo to the list
     */
    data class Add(val todo: String) : TodoAction()

    /**
     * Set the todo with [index] as "Done"
     */
    data class SetDone(val index: Int) : TodoAction()

    /**
     * Increment the counter
     */
    data object IncrementCounter : TodoAction()
}
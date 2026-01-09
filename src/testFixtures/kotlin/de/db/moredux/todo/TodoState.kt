package de.db.moredux.todo

import de.db.moredux.State

/**
 * Testing State that holds
 * - a list of todos
 * - a list that denote whether the todo of the same index has been done or not
 * - a counter that should be incremented whenever a todo ist added or is set to done
 */
data class TodoState(
    val todos: List<String>,
    val done: List<Boolean>,
    val counter: Int
) : State {
    override fun clone(): State = copy()

    companion object {
        val INITIAL = TodoState(
            todos = emptyList(),
            done = emptyList(),
            counter = 0
        )
    }
}
package de.db.moredux.todo

import de.db.moredux.reducer.Reducer
import de.db.moredux.reducer.ReducerResult
import de.db.moredux.todo.TodoAction.Add


/**
 * A reducer that adds a new todo to the list
 */
class ReducerAddTodo : Reducer<TodoState, Add>() {
    override fun reduce(state: TodoState, action: Add): ReducerResult<TodoState> {
        val todos = state.todos.toMutableList()
        todos.add(action.todo)

        val done = state.done.toMutableList()
        done.add(false)

        return ReducerResult(
            state.copy(
                todos = todos.toList(),
                done = done.toList()
            )
        )
    }
}

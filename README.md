![Publish release artifacts](https://github.com/dbvertrieb/MoRedux/actions/workflows/publish-release-artifacts.yml/badge.svg)
![Verify code quality](https://github.com/dbvertrieb/MoRedux/actions/workflows/verify-code-quality.yml/badge.svg)

# MoRedux

Redux framework in Kotlin, inspired by the JS Redux framework (https://redux.js.org/).

MoRedux is a predictable state management method. It centralizes the state of an application or of a part of an application and enforces strict rules on how the state can be changes. In MoRedux actions are dispatched, reducers determine how the state should be changed and updated and the state is made available to the entire application through the store, that contains and manages the state.

# Download

Replace ```$VERSION$``` with the released version of your choice. The recommended version is the latest.

Visit [the release page](https://github.com/dbvertrieb/MoRedux/releases) for a detailed list of all releases and their corresponding changelogs.

## gradle

```
# on build.gradle.kts on project level
repositories {
    mavenCentral()
}

# wherever you declare your dependencies (project / module level)
dependencies {
    implementation("io.github.dbvertrieb:moredux:$VERSION$")
}
```

## Maven

```
<dependency>
    <groupId>io.github.dbvertrieb</groupId>
    <artifactId>moredux</artifactId>
    <version>$VERSION$</version>
</dependency>
```
# When to use MoRedux

The Redux method ensures that state changes are predictable and easy to track, which is crucial in larger applications. If your application has a lot of shared state that needs to be accessible across multiple components or the state contains a lot of data,especially when the state is changed in bigger user interfaces, the Redux method in general helps to manage user interactions and state changes.

# Basic Usage

## Components

* State
* Action
* Reducer
* Effect / Followup action
* Store
* StoreContainer
* Logging

## State

The state

## Action

A plain Kotlin object e.g. a data class or data object, that describes an event in the application. Actions my include a
payload with additional data. They are dispatched to update the state in the store.

## Reducer

A reducer is a piece of code that takes the current state and an action as arguments and returns a new state. Reducers
specify how the state changes in response to an action. A Reducer always operates synchronously / on the main thread.

## Effect

Effects are possible additional results of a reducer. An effect is a function that takes the current state as an
argument and is executed after a reducer finishes. An effect does not create a new state, but it may dispatch further
actions. The dispatch process is identical to a regular action dispatch

## Followup actions

Followup actions are possible additional results of a reducer. A follow-up action is not different from regular actions.
They are executed / redispatched right after the reducer finishes.

## StateObserver

A stateobserver is a function that is executed upon state changed. Whenever a reducer finishes its work, all registeres
stateobservers are notified. Stateobservers (and Selectors - see below) are registered in a store.

## Selectors

Selectors are special types of stateobservers. Selectors are functions that extract specific pieces of the state from
the store upon state change.They allow you to get the information
from the state, without accessing the state of the store directly. A selector usually also applies some additional
transformation to the piece of information it extracts from the state, e.g. a state contains a persons name and adress
all in lower case letters and a selector could now extract the family name and make sure the first letter is always a
capital letter.

## Store

The single source of truth where the state of the application is stored and managed. The store is also responsible for
management of reducers and action dispatching. A dispatch takes an action as an argument and forwards it togetehr with
the current state to the reducer responsible for that action.

## StoreContainer

A storecontainer does not contain a state, but contains one or more stores. It dispatches actions passed to the
storecontainer to the store that contains the reducer that is responsible for the action.

## Logging

The "logging component" is nothing Redux specific, but a simple tool to help you integrate MoRedux into your code. You can redirect all logging of MoRedux to the log mechanism of your choice (see ModReduxSettings). In addition, you can specify the amount of logs that are logged out. The possible settings are FULL, MINIMAL and DISABLED.

# Examples

```kotlin
// Define the state
data class TodoState(
    val todos: List<String>,
    val done: List<Boolean>
) : State {
    override fun clone(): State = copy()
}

// Define the possible actions
sealed class TodoAction : Action {
    data class Add(val todo: String) : TodoAction()
    data class SetDone(val index: Int) : TodoAction()
}

// Example of a reducer implemented as class extending teh Reducer interface
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

fun main() {

    // inject some custom logging
    val log = mutableListOf<String>()
    MoReduxSettings.logDebug = { tag, message -> log.add("DEBUG - $tag: $message") }
    MoReduxSettings.logWarn = { tag, message -> log.add("WARN - $tag: $message") }

    // Build the store and register all reducers
    val store = Store.Builder<TodoState>()
        .withInitialState(TodoState(todos = emptyList(), done = emptyList()))
        .registerReducer<Add>(ReducerAddTodo())
        .registerReducerToState<SetDone> { state, action ->
            // Example of a reducer implemented as function that simply returns a new state
            val done = state.done.toMutableList()
            done[action.index] = true

            state.copy(done = done.toList())
        }
        .build()

    // set up a selector only with todos that have not been done yet
    val unfinishedTodos = store.addSelectorToStateFlow(emptyList()) { state ->
        state.todos.filterIndexed { index, _ -> !state.done[index] }
    }

    // Perform some actions - these would be actions trigger by user input
    store.dispatch(Add("Invite friends"))
    store.dispatch(Add("Cook dinner"))
    store.dispatch(SetDone(0))
    
    // the unfinishedtodos StateFlow contains a list with the only value "Cook dinner"
    println(unfinishedTodos.value)
}
```
# License

The content of this repository is licensed under the [Apache License, Version 2.0](LICENSE.txt).

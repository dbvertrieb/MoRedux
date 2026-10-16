package de.db.moredux.observation

import de.db.moredux.State
import de.db.moredux.store.Store
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * Registers a selector on this store and exposes its mapped values as a [Flow].
 *
 * The returned flow emits a value whenever the selected state value changes according to the
 * selector's notification rules. By default, the channel is conflated, so a slow collector only
 * receives the most recent pending value. Values are sent to the channel without suspending; a
 * value can therefore be dropped if the channel is full.
 *
 * @param processCurrentStateImmediately if `true`, emits the value mapped from the current state
 * immediately after the selector is registered
 * @param notificationGuard the [notificationGuard] is used to determine whether a mapped value should be published
 * to the Selector or not. If you want to receive every mapped value, use [NotificationGuard.AlwaysNotify].
 * If you only want to receive mapped values that are different from the previous mapped value,
 * use [NotificationGuard.NoDuplicates].
 * @param capacity the capacity and buffering strategy of the underlying channel
 * @param map maps each store state to the value emitted by the returned flow
 * @return a flow backed by the selector's event channel
 */
fun <STATE : State, VALUE> Store<STATE>.addSelectorEventChannel(
    processCurrentStateImmediately: Boolean = false,
    notificationGuard: NotificationGuard<VALUE> = NotificationGuard.NoDuplicates(),
    capacity: Int = Channel.CONFLATED,
    map: (STATE) -> VALUE,
): Flow<VALUE> {
    val channel = Channel<VALUE>(capacity)
    addSelectorFromCallback(
        map = map,
        processCurrentStateImmediately = processCurrentStateImmediately,
        notificationGuard = notificationGuard,
        observer = { value -> value?.let { channel.trySend(it) } }
    )
    return channel.receiveAsFlow()
}

/**
 * Registers a selector on this store, invokes [observer] for every mapped value, and exposes the
 * mapped values as a [Flow].
 *
 * The channel keeps the same non-suspending [Channel.trySend] behavior as
 * [addSelectorEventChannel]. Consequently, a value is not emitted to the channel when it is
 * `null`, or when the channel cannot accept it. The [observer] is invoked independently for every
 * mapped value.
 *
 * @param processCurrentStateImmediately if `true`, processes the current state immediately after
 * the selector is registered
 * @param notificationGuard the [notificationGuard] is used to determine whether a mapped value should be published
 * to the Selector or not. If you want to receive every mapped value, use [NotificationGuard.AlwaysNotify].
 * If you only want to receive mapped values that are different from the previous mapped value,
 * use [NotificationGuard.NoDuplicates].
 * @param capacity the capacity and buffering strategy of the underlying channel
 * @param observer callback invoked for every mapped value
 * @param map maps each store state to the value emitted by the returned flow
 * @return a flow backed by the selector's event channel
 */
fun <STATE : State, VALUE> Store<STATE>.addSelectorEventChannel(
    processCurrentStateImmediately: Boolean = false,
    notificationGuard: NotificationGuard<VALUE> = NotificationGuard.NoDuplicates(),
    capacity: Int = Channel.CONFLATED,
    observer: (VALUE) -> Unit,
    map: (STATE) -> VALUE,
): Flow<VALUE> {
    val channel = Channel<VALUE>(capacity)
    addSelectorFromCallback(
        map = map,
        processCurrentStateImmediately = processCurrentStateImmediately,
        notificationGuard = notificationGuard,
        observer = { value ->
            observer(value)
            value?.let { channel.trySend(it) }
        }
    )
    return channel.receiveAsFlow()
}

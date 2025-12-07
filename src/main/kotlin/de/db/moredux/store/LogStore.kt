package de.db.moredux.store

import de.db.moredux.State
import de.db.moredux.settings.MoReduxLogger
import de.db.moredux.settings.MoReduxSettings
import kotlin.reflect.KClass

open class LogStore<STATE : State>(
    open val currentDispatchCount: Int,
    val clazz: KClass<STATE>
) {
    val prefix = currentDispatchCount.createPrefix()

    protected fun Int.createPrefix(): String = "%d - Store for %s".format(this, clazz.simpleName)

    open fun d(message: String, logMode: MoReduxSettings.LogMode = MoReduxSettings.LogMode.FULL) {
        MoReduxLogger.d(
            clazz = this::class,
            logMode = logMode,
            message = "$prefix - $message"
        )
    }
}

class LogMiddleware<STATE : State>(
    override val currentDispatchCount: Int,
    middlewareIndex: Int,
    clazz: KClass<STATE>
) : LogStore<STATE>(currentDispatchCount, clazz) {

    val middlewarePrefix = "%s Middleware #%d".format(
        currentDispatchCount.createPrefix(),
        middlewareIndex
    )

    override fun d(message: String, logMode: MoReduxSettings.LogMode) {
        MoReduxLogger.d(
            clazz = this::class,
            logMode = logMode,
            message = "$middlewarePrefix - $message"
        )
    }
}

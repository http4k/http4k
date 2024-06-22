package org.http4k.sse

import org.http4k.core.Request

abstract class PushAdaptingSse(override val connectRequest: Request) : Sse {

    private val closeHandlers: MutableList<() -> Unit> = mutableListOf()

    fun triggerClose() = closeHandlers.forEach { it.invoke() }

    override fun onClose(fn: () -> Unit) {
        closeHandlers.add(fn)
    }
}

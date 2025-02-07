package org.http4k.sse

import org.http4k.core.Request

abstract class PushAdaptingSse(override val connectRequest: Request) : Sse {

    private val closeHandlers = mutableListOf<() -> Unit>()

    fun triggerClose() = apply { closeHandlers.forEach { it() } }

    override fun close() {
        triggerClose()
    }

    override fun onClose(fn: () -> Unit) = apply {
        closeHandlers.add(fn)
    }
}

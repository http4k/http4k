package org.http4k.sse

abstract class PushAdaptingSse : Sse {

    private val closeHandlers: MutableList<() -> Unit> = mutableListOf()

    fun triggerClose() = closeHandlers.forEach { it.invoke() }

    override fun onClose(fn: () -> Unit) {
        closeHandlers.add(fn)
    }
}

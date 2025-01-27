package org.http4k.hotreload

import java.nio.file.Path

/**
 * Responsible for watching a set of paths for changes and acting on the changes.
 */
interface PathWatcher : AutoCloseable {
    fun onChange(fn: (List<Path>) -> Unit): PathWatcher
    fun onSuccess(fn: () -> Unit): PathWatcher
    fun onFailure(fn: (String) -> Unit): PathWatcher
    fun watch(newPaths: List<Path>)
    fun start()
    fun stop()
    override fun close() = stop()
}

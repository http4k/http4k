package org.http4k.hotreload

import org.http4k.hotreload.ProjectCompiler.Companion.Gradle
import org.http4k.hotreload.ProjectCompiler.Companion.Result.Failed
import org.http4k.hotreload.ProjectCompiler.Companion.Result.Ok
import java.nio.file.FileSystems
import java.nio.file.FileVisitResult
import java.nio.file.FileVisitResult.CONTINUE
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.StandardWatchEventKinds.ENTRY_CREATE
import java.nio.file.StandardWatchEventKinds.ENTRY_DELETE
import java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY
import java.nio.file.attribute.BasicFileAttributes
import java.time.Duration
import kotlin.concurrent.thread
import kotlin.io.path.exists

/**
 * Watches for changes on the classpath source and rebuilds the project when changes are detected.
 */
class ProjectCompilingPathWatcher(
    private val projectCompiler: ProjectCompiler = Gradle(),
    // This is the default set of directories to watch for changes
    private val watchedDirs: Set<String> = setOf("src/main", "src/test", "build/classes"),
    private val downtimeSleep: () -> Unit = { Thread.sleep(Duration.ofSeconds(1)) }
) : PathWatcher {
    private val watchService = FileSystems.getDefault().newWatchService()

    private val pathsToWatch = mutableSetOf<Path>()
    private val changeListeners = mutableListOf<(List<Path>) -> Unit>()
    private val successListeners = mutableListOf<() -> Unit>()
    private val failureListeners = mutableListOf<(String) -> Unit>()

    override fun onChange(fn: (List<Path>) -> Unit) = apply { changeListeners.add(fn) }

    override fun onSuccess(fn: () -> Unit) = apply { successListeners.add(fn) }

    override fun onFailure(fn: (String) -> Unit) = apply { failureListeners.add(fn) }

    override fun watch(newPaths: List<Path>) {
        pathsToWatch += newPaths.filter { it.exists() }
    }

    override fun start() {
        pathsToWatch
            .forEach {
                Files.walkFileTree(it, object : SimpleFileVisitor<Path>() {
                    override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
                        if (shouldWatchDirectory(dir)) {
                            dir.register(watchService, ENTRY_MODIFY, ENTRY_CREATE, ENTRY_DELETE)
                        }
                        return CONTINUE
                    }
                })
            }

        var isRebuilding = false

        thread {
            while (true) {
                val key = watchService.take()
                val fileChanges = key.pollEvents().mapNotNull { it.context() as? Path }

                if (!isRebuilding && fileChanges.any { it.toString().contains(".") }) {
                    isRebuilding = true
                    try {
                        changeListeners.forEach { it(fileChanges) }

                        when (val result = projectCompiler()) {
                            Ok -> successListeners.forEach { it() }
                            is Failed -> failureListeners.forEach { it(result.error) }
                        }

                        downtimeSleep()
                    } finally {
                        isRebuilding = false
                    }
                }
                key.reset()
            }
        }
    }

    override fun stop() {
        watchService.close()
    }

    private fun shouldWatchDirectory(dir: Path) = watchedDirs.any { dir.toString().contains(it) }
}

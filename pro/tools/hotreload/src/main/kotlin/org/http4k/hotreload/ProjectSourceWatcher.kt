package org.http4k.hotreload

import org.http4k.hotreload.CompileProject.Companion.Gradle
import org.http4k.hotreload.CompileProject.Companion.Result.Failed
import org.http4k.hotreload.CompileProject.Companion.Result.Ok
import java.nio.file.FileSystems
import java.nio.file.FileVisitResult
import java.nio.file.FileVisitResult.CONTINUE
import java.nio.file.Files.walkFileTree
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.StandardWatchEventKinds.ENTRY_CREATE
import java.nio.file.StandardWatchEventKinds.ENTRY_DELETE
import java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY
import java.nio.file.attribute.BasicFileAttributes
import java.time.Duration
import kotlin.concurrent.thread
import kotlin.io.path.exists

// This is the default set of directories to watch for changes
val DEFAULT_WATCH_PATTERNS = setOf("src/main", "src/test", "build/classes")

/**
 * Watches for changes on the classpath source and rebuilds the project when changes are detected.
 */
class ProjectSourceWatcher(
    private val compileProject: CompileProject = Gradle(),
    private val watchedDirs: Set<String> = DEFAULT_WATCH_PATTERNS,
    private val rebuildBackoff: Duration = Duration.ofSeconds(1)
) {
    private val changeListeners = mutableListOf<(List<Path>) -> Unit>()

    fun onChange(fn: (List<Path>) -> Unit) = apply { changeListeners.add(fn) }

    private val successListeners = mutableListOf<() -> Unit>()

    fun onSuccess(fn: () -> Unit) = apply { successListeners.add(fn) }

    private val failureListeners = mutableListOf<(String) -> Unit>()

    fun onFailure(fn: (String) -> Unit) = apply { failureListeners.add(fn) }

    fun watch(sourcePaths: List<Path>) {
        val watchService = FileSystems.getDefault().newWatchService()

        sourcePaths
            .filter { it.exists() }
            .forEach {
                walkFileTree(it, object : SimpleFileVisitor<Path>() {
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

                        when (val result = compileProject()) {
                            Ok -> successListeners.forEach { it() }
                            is Failed -> result.errorStream.reader().readText()
                                .also { stream -> failureListeners.forEach { it(stream) } }
                        }
                        Thread.sleep(rebuildBackoff)
                    } finally {
                        isRebuilding = false
                    }
                }
                key.reset()
            }
        }
    }

    private fun shouldWatchDirectory(dir: Path) = watchedDirs.any { dir.toString().contains(it) }
}

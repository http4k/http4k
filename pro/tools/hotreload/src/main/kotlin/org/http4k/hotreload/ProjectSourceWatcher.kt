package org.http4k.hotreload

import org.http4k.hotreload.CompileProject.Companion.Result.Failed
import org.http4k.hotreload.CompileProject.Companion.Result.Ok
import java.io.InputStream
import java.nio.file.FileSystems
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.SimpleFileVisitor
import java.nio.file.StandardWatchEventKinds
import java.nio.file.attribute.BasicFileAttributes
import java.time.Duration
import kotlin.concurrent.thread
import kotlin.io.path.exists

class ProjectSourceWatcher(
    private val watchedDirs: Set<String>,
    private val compileProject: CompileProject,
    private val rebuildBackoff: Duration,
    private val onChange: () -> Unit,
    private val onSuccess: () -> Unit,
    private val onError: (InputStream) -> Unit
) {
    fun watchFiles(classpathRoots: List<String>) {
        val watchService = FileSystems.getDefault().newWatchService()

        classpathRoots
            .map { Paths.get(it.substringBefore("build/classes")).resolve("src") }
            .filter { it.exists() }
            .forEach {
                Files.walkFileTree(it, object : SimpleFileVisitor<Path>() {
                    override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
                        if (shouldWatchDirectory(dir)) {
                            dir.register(
                                watchService,
                                StandardWatchEventKinds.ENTRY_MODIFY,
                                StandardWatchEventKinds.ENTRY_CREATE,
                                StandardWatchEventKinds.ENTRY_DELETE
                            )
                        }
                        return FileVisitResult.CONTINUE
                    }
                })
            }

        var isRebuilding = false

        thread {
            while (true) {
                val key = watchService.take()
                val events = key.pollEvents()

                if (!isRebuilding && events.any { ((it.context() as? Path)?.toString() ?: "").contains(".") }) {
                    isRebuilding = true
                    try {
                        onChange()

                        when (val result = compileProject()) {
                            Ok -> onSuccess()
                            is Failed -> onError(result.errorStream)
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

package org.http4k.hotreload

import org.http4k.core.HttpHandler
import org.http4k.hotreload.CompileProject.Companion.Gradle
import org.http4k.hotreload.CompileProject.Companion.Result.Failed
import org.http4k.hotreload.CompileProject.Companion.Result.Ok
import org.http4k.server.Http4kServer
import org.http4k.server.PolyHandler
import org.http4k.server.PolyServerConfig
import org.http4k.server.ServerConfig
import org.http4k.server.SunHttp
import org.http4k.server.asServer
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.FileVisitResult
import java.nio.file.FileVisitResult.CONTINUE
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths.get
import java.nio.file.SimpleFileVisitor
import java.nio.file.StandardWatchEventKinds.ENTRY_CREATE
import java.nio.file.StandardWatchEventKinds.ENTRY_DELETE
import java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY
import java.nio.file.attribute.BasicFileAttributes
import java.time.Duration
import java.time.Duration.ofMillis
import java.time.Duration.ofSeconds
import kotlin.concurrent.thread
import kotlin.io.path.exists

/**
 * Hot reloading server. Watches for changes on the classpath source and rebuilds the project when changes are detected.
 * Uses an event source connection script injected into the HTML pages to trigger a reload when the project is rebuilt.
 */
object HotReloadServer {

    // This is the default set of directories to watch for changes
    val DEFAULT_WATCH_SET = setOf("src/main", "src/test", "build/classes")

    const val DEFAULT_PORT = 8000

    /**
     * Create a hot-reloading HTTP server. Defaults to SunHttp on port 8000.
     *
     *  Note that some servers do not support hot-reloading correctly due to quirks around reloading.
     *  We suggest using SunHttp (the default) for speed.
     */
    @Suppress("UNCHECKED_CAST")
    inline fun <reified H : HotReloadable<HttpHandler>> http(
        serverConfig: ServerConfig = SunHttp(DEFAULT_PORT),
        watchedDirs: Set<String> = DEFAULT_WATCH_SET,
        compileProject: CompileProject = Gradle(),
        runner: TaskRunner = TaskRunner.retry(5, ofMillis(100)),
        rebuildBackoff: Duration = ofSeconds(1),
        crossinline log: (String) -> Unit = ::println
    ) = invoke<H, HttpHandler>(watchedDirs, compileProject, runner, rebuildBackoff, log) {
        it.asServer(serverConfig)
    }

    /**
     * Create a hot-reloading Multi-protocol server.
     *
     *  Note that some servers do not support hot-reloading correctly due to quirks around reloading.
     *  We suggest using Jetty as a default when WS or SSE is required.
     */
    inline fun <reified H : HotReloadable<PolyHandler>> poly(
        serverConfig: PolyServerConfig,
        watchedDirs: Set<String> = DEFAULT_WATCH_SET,
        compileProject: CompileProject = Gradle(),
        runner: TaskRunner = TaskRunner.retry(5, ofMillis(100)),
        rebuildBackoff: Duration = ofSeconds(1),
        crossinline log: (String) -> Unit = ::println
    ) = invoke<H, PolyHandler>(watchedDirs, compileProject, runner, rebuildBackoff, log) {
        it.asServer(serverConfig)
    }

    inline operator fun <reified T : HotReloadable<H>, H> invoke(
        watchedDirs: Set<String>,
        compileProject: CompileProject,
        runner: TaskRunner,
        rebuildBackoff: Duration = ofSeconds(1),
        crossinline log: (String) -> Unit,
        crossinline toServer: (H) -> Http4kServer
    ) = object : Http4kServer {
        override fun port() = currentServer?.port() ?: error("Server is not started!")

        private var currentServer: Http4kServer? = null
        private var watchThread: Thread? = null
        private var isRebuilding = false

        override fun start(): Http4kServer {
            watchFiles()
            return startServer()
        }

        override fun stop() = apply { currentServer?.stop() }

        fun startServer(): Http4kServer {
            runCatching { currentServer?.stop() }

            val classpathUrls = allProjectClasspathRoots()
                .map { get(it).toUri().toURL() }
                .toTypedArray()

            val classLoader = HotReloadClassLoader(classpathUrls)
            Thread.currentThread().contextClassLoader = classLoader

            val appClass = Class.forName(T::class.qualifiedName, true, classLoader)

            runner {
                currentServer = toServer(
                    appClass.getDeclaredMethod("create").invoke(appClass.getDeclaredConstructor().newInstance())
                    as H
                ).start()
            }

            log("\uD83D\uDE80 http4k started at http://localhost:${port()} \uD83D\uDE80")

            return currentServer as Http4kServer
        }

        private fun watchFiles() {
            val watchService = FileSystems.getDefault().newWatchService()

            allProjectClasspathRoots()
                .map { get(it.substringBefore("build/classes")).resolve("src") }
                .filter { it.exists() }
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

            watchThread = thread {
                while (true) {
                    val key = watchService.take()
                    val events = key.pollEvents()

                    if (!isRebuilding && events.any { ((it.context() as? Path)?.toString() ?: "").contains(".") }) {
                        isRebuilding = true
                        try {
                            log("\uD83D\uDEA7 Rebuilding... \uD83D\uDEA7")

                            when (val result = compileProject()) {
                                Ok -> startServer()
                                is Failed -> {
                                    result.errorStream.copyTo(System.err)
                                    log("\uD83D\uDEAB Rebuilding failed... \uD83D\uDEAB")
                                }
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

        private fun allProjectClasspathRoots() = System.getProperty("java.class.path")
            .split(File.pathSeparator)
            .filter { it.contains("build/classes") }

        private fun shouldWatchDirectory(dir: Path) = watchedDirs.any { dir.toString().contains(it) }
    }
}

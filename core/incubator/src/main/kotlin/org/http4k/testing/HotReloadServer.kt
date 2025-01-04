package org.http4k.testing

import HotReloadClassLoader
import org.http4k.core.HttpHandler
import org.http4k.server.Http4kServer
import org.http4k.server.PolyHandler
import org.http4k.server.PolyServerConfig
import org.http4k.server.ServerConfig
import org.http4k.server.SunHttp
import org.http4k.server.asServer
import org.http4k.testing.CompileProject.Companion.Gradle
import org.http4k.testing.CompileProject.Companion.Result.Failed
import org.http4k.testing.CompileProject.Companion.Result.Ok
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
import kotlin.concurrent.thread
import kotlin.io.path.exists

/**
 * Extreme Spike hot-reloading server. Probably only works on simple projects right now
 */
object HotReloadServer {

    /**
     * Create a hot-reloading HTTP server. Defaults to SunHttp on port 8000.
     */
    @Suppress("UNCHECKED_CAST")
    inline fun <reified T : HttpAppProvider> http(
        serverConfig: ServerConfig = SunHttp(8000),
        watchedDirs: Set<String> = STANDARD_WATCH_SET,
        compileProject: CompileProject = Gradle(),
        runner: TaskRunner = TaskRunner.retry(5, Duration.ofMillis(100)),
        crossinline log: (String) -> Unit = ::println
    ) = invoke<T>(watchedDirs, compileProject, runner, log) { (it as HttpHandler).asServer(serverConfig) }

    /**
     * Create a hot-reloading Multi-protocol server.
     */
    inline fun <reified T : PolyAppProvider> poly(
        serverConfig: PolyServerConfig,
        watchedDirs: Set<String> = STANDARD_WATCH_SET,
        compileProject: CompileProject = Gradle(),
        runner: TaskRunner = TaskRunner.retry(5, Duration.ofMillis(100)),
        crossinline log: (String) -> Unit = ::println
    ) = invoke<T>(watchedDirs, compileProject, runner, log) { (it as PolyHandler).asServer(serverConfig) }

    val STANDARD_WATCH_SET = setOf("src/main", "src/test", "build/classes")

    inline operator fun <reified T> invoke(
        watchedDirs: Set<String>,
        compileProject: CompileProject,
        runner: TaskRunner,
        crossinline log: (String) -> Unit,
        crossinline toServer: (Any) -> Http4kServer
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
            currentServer?.stop()

            val classpathUrls = allProjectClasspathRoots()
                .map { get(it).toUri().toURL() }
                .toTypedArray()

            val classLoader = HotReloadClassLoader(classpathUrls)
            Thread.currentThread().contextClassLoader = classLoader

            val appClass = Class.forName(T::class.qualifiedName, true, classLoader)

            runner {
                currentServer = toServer(
                    appClass.getDeclaredMethod("invoke").invoke(appClass.getDeclaredConstructor().newInstance())
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
                            Thread.sleep(1000)
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

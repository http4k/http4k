package org.http4k.hotreload

import org.http4k.core.HttpHandler
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.hotreload.CompileProject.Companion.Gradle
import org.http4k.server.Http4kServer
import org.http4k.server.PolyHandler
import org.http4k.server.PolyServerConfig
import org.http4k.server.ServerConfig
import org.http4k.server.SunHttp
import org.http4k.server.asServer
import java.io.File
import java.net.URL
import java.nio.file.Paths.get
import java.time.Duration
import java.time.Duration.ofMillis
import java.time.Duration.ofSeconds

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
    inline fun <reified H : HotReloadable<HttpHandler>> http(
        serverConfig: ServerConfig = SunHttp(DEFAULT_PORT),
        watchedDirs: Set<String> = DEFAULT_WATCH_SET,
        compileProject: CompileProject = Gradle(),
        rebuildBackoff: Duration = ofSeconds(1),
        runner: TaskRunner = TaskRunner.retry(5, ofMillis(100)),
        noinline log: (String) -> Unit = ::println
    ) = invoke<H, HttpHandler>(watchedDirs, compileProject, rebuildBackoff, runner, log) {
        HotReloadRoutes(it).asServer(serverConfig)
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
        rebuildBackoff: Duration = ofSeconds(1),
        runner: TaskRunner = TaskRunner.retry(5, ofMillis(100)),
        noinline log: (String) -> Unit = ::println
    ) = invoke<H, PolyHandler>(watchedDirs, compileProject, rebuildBackoff, runner, log) {
        it.run { PolyHandler(HotReloadRoutes(http ?: { Response(OK) }), ws, sse) }
            .asServer(serverConfig)
    }

    @Suppress("UNCHECKED_CAST")
    inline operator fun <reified T : HotReloadable<H>, H> invoke(
        watchedDirs: Set<String>,
        compileProject: CompileProject,
        rebuildBackoff: Duration,
        runner: TaskRunner,
        noinline log: (String) -> Unit,
        crossinline toServer: (H) -> Http4kServer
    ) = object : Http4kServer {
        override fun port() = currentServer?.port() ?: error("Server is not started!")

        private var currentServer: Http4kServer? = null

        private val watcher =
            ProjectSourceWatcher(watchedDirs, compileProject, rebuildBackoff,
                { log("\uD83D\uDEA7 Rebuilding... \uD83D\uDEA7") },
                { startServer() },
                {
                    it.copyTo(System.err)
                    log("\uD83D\uDEAB Rebuilding failed... \uD83D\uDEAB")
                })

        private val classpathRoots = System.getProperty("java.class.path")
            .split(File.pathSeparator)
            .filter { it.contains("build/classes") }

        override fun start(): Http4kServer = apply {
            watcher
                .watchFiles(classpathRoots)

            startServer()
        }

        override fun stop() = apply { currentServer?.stop() }

        fun startServer(): Http4kServer {
            runCatching { currentServer?.stop() }

            runner { currentServer = toServer(reloadAppFrom(classpathRoots)).start() }

            log("\uD83D\uDE80 http4k started at http://localhost:${port()} \uD83D\uDE80")

            return currentServer as Http4kServer
        }

        private fun reloadAppFrom(classpathRoots: List<String>): H {
            val classLoader = HotReloadClassLoader(classpathRoots.map { get(it).toUri().toURL() }.toTypedArray<URL>())
                .also { Thread.currentThread().contextClassLoader = it }

            val appClass = Class.forName(T::class.qualifiedName, true, classLoader)

            return Class.forName(T::class.qualifiedName, true, classLoader)
                .getDeclaredMethod("create")
                .invoke(appClass.getDeclaredConstructor().newInstance()) as H
        }
    }
}

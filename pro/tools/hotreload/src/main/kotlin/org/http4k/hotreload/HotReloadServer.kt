package org.http4k.hotreload

import org.http4k.core.HttpHandler
import org.http4k.core.PolyHandler
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.hotreload.ProjectCompiler.Companion.Gradle
import org.http4k.hotreload.Reload.Companion.Classpath
import org.http4k.hotreload.TaskRunner.Companion.retry
import org.http4k.server.Http4kServer
import org.http4k.server.PolyServerConfig
import org.http4k.server.ServerConfig
import org.http4k.server.SunHttp
import org.http4k.server.asServer
import java.io.File
import java.nio.file.Paths
import java.nio.file.Paths.get
import java.time.Duration.ofMillis

/**
 * Hot reloading server. Watches for changes on the classpath source and rebuilds the project when changes are detected.
 * Uses an event source connection script injected into the HTML pages to trigger a reload when the project is rebuilt.
 */
object HotReloadServer {
    /**
     * Create a hot-reloading HTTP server. Defaults to SunHttp on port 8000.
     *
     *  Note that some servers do not support hot-reloading correctly due to quirks around reloading.
     *  We suggest using SunHttp (the default) for speed.
     */
    inline fun <reified H : HotReloadable<HttpHandler>> http(
        serverConfig: ServerConfig = SunHttp(8000),
        watcher: PathWatcher = ProjectCompilingPathWatcher(Gradle()),
        taskRunner: TaskRunner = retry(5, ofMillis(100)),
        noinline log: (String) -> Unit = ::println,
        noinline error: (String) -> Unit = System.err::println
    ) = hotReload<H, HttpHandler>(watcher, taskRunner, Classpath(), log, error) {
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
        watcher: PathWatcher = ProjectCompilingPathWatcher(Gradle()),
        taskRunner: TaskRunner = retry(5, ofMillis(100)),
        noinline log: (String) -> Unit = ::println,
        noinline error: (String) -> Unit = System.err::println
    ) = hotReload<H, PolyHandler>(watcher, taskRunner, Classpath(), log, error) {
        it.run { PolyHandler(HotReloadRoutes(http ?: { Response(OK) }), ws, sse) }.asServer(serverConfig)
    }

    @Suppress("UNCHECKED_CAST")
    inline fun <reified T : HotReloadable<H>, H> hotReload(
        watcher: PathWatcher,
        taskRunner: TaskRunner,
        reload: Reload<H>,
        noinline log: (String) -> Unit,
        noinline err: (String) -> Unit,
        crossinline toServer: (H) -> Http4kServer
    ) = object : Http4kServer {

        override fun port() = currentServer?.port() ?: error("Server is not started!")

        private var currentServer: Http4kServer? = null

        private val classpathRoots = System.getProperty("java.class.path")
            .split(File.pathSeparator)
            .filter { it.contains("build/classes") }

        init {
            watcher
                .onChange { log("\uD83D\uDEA7 Rebuilding... \uD83D\uDEA7") }
                .onSuccess { startServer() }
                .onFailure {
                    err(it)
                    log("\uD83D\uDEAB Rebuilding failed... \uD83D\uDEAB")
                }
                .watch(classpathRoots
                    .map { get(it.substringBefore("build/classes")).resolve("src") })
        }

        override fun start(): Http4kServer = apply {
            watcher.start()
            startServer()
        }

        override fun stop() = apply {
            watcher.stop()
            currentServer?.stop()
        }

        fun startServer(): Http4kServer {
            runCatching { currentServer?.stop() }

            taskRunner {
                currentServer = toServer(reload(T::class.qualifiedName!!, classpathRoots.map(Paths::get))).start()
            }

            log("\uD83D\uDE80 http4k started at http://localhost:${port()} \uD83D\uDE80")

            return currentServer as Http4kServer
        }
    }
}


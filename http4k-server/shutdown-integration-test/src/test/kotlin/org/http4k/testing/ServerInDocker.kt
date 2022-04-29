package org.http4k.testing

import com.github.dockerjava.api.async.ResultCallback
import com.github.dockerjava.api.command.BuildImageResultCallback
import com.github.dockerjava.api.model.*
import com.github.dockerjava.core.DefaultDockerClientConfig
import com.github.dockerjava.core.DockerClientImpl
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient
import com.github.dockerjava.transport.DockerHttpClient
import okhttp3.internal.toImmutableList
import org.http4k.core.Uri
import org.http4k.core.extend
import org.http4k.events.Event
import org.http4k.events.Events
import org.http4k.server.ServerConfig
import org.http4k.util.inIntelliJOnly
import org.junit.jupiter.api.fail
import java.io.File
import java.nio.file.Files
import java.time.Duration
import java.time.Instant
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit.SECONDS

class ServerInDocker(private val events: Events = PrintEventsInIntelliJ()) {
    private val basePath by lazy {
        val workingDir = File(".").absolutePath
        val projectDir = workingDir.removeSuffix(workingDir.substringAfter("/http4k/"))
        val modulePath = "/http4k-server/shutdown-integration-test"
        Uri.of("$projectDir$modulePath")
    }
    private val dockerWorkspace = basePath.extend(Uri.of("build/docker"))
    private fun dockerWorkspace(subPath: String) = File(dockerWorkspace.extend(Uri.of(subPath)).path)
    private fun project(subPath: String) = File(basePath.extend(Uri.of(subPath)).path)

    private val config = DefaultDockerClientConfig.createDefaultConfigBuilder().build()
    private val http: DockerHttpClient = ApacheDockerHttpClient.Builder()
        .dockerHost(config.dockerHost)
        .sslConfig(config.sslConfig)
        .maxConnections(100)
        .connectionTimeout(Duration.ofSeconds(30))
        .responseTimeout(Duration.ofSeconds(45))
        .build()

    private val dockerClient = DockerClientImpl.getInstance(config, http)

    init {
        try {
            dockerClient.pingCmd().exec()
        } catch (e: Exception) {
            error("Docker unavailable (${e.message})")
        }
    }

    fun start(backend: ServerBackend, stopMode: ServerConfig.StopMode): ContainerId {
        events(DockerEvent.ServerStartedRequested)
        dockerWorkspace("/").apply {
            deleteRecursively()
            mkdirs()
        }

        Files.copy(
            project("/src/main/resources/Dockerfile").toPath(),
            dockerWorkspace("Dockerfile").toPath()
        )

        val serverPackage = project("/build/distributions/http4k-server-shutdown-integration-test-LOCAL.zip")

        if (!serverPackage.exists()) {
            fail(
                "Server package not found. To create run:\n" +
                    "./gradlew :http4k-server-shutdown-integration-test:distZip"
            )
        }

        Files.copy(
            serverPackage.toPath(),
            dockerWorkspace("http4k-server-shutdown-integration-test-LOCAL.zip").toPath()
        )

        val imageId = dockerClient.buildImageCmd(dockerWorkspace("Dockerfile"))
            .withTags(setOf("http4k-server-shutdown-integration-test"))
            .exec(BuildImageResultCallback())
            .awaitImageId(10, SECONDS)

        events(DockerEvent.WorkspacePrepared)

        dockerClient.listContainersCmd()
            .withShowAll(true)
            .exec()
            .filter { it.names.contains("/http4k-server-shutdown-integration-test") }
            .also { events(DockerEvent.RelevantContainersFound(it.map { it.id to it.state })) }
            .map {
                if (it.state == "running") {
                    dockerClient.killContainerCmd(it.id).exec()
                    events(DockerEvent.ContainerKilled(it.id))
                }
                dockerClient.removeContainerCmd(it.id).withForce(true).exec()
                events(DockerEvent.ContainerRemoved(it.id))
            }

        val exposedPort = ExposedPort.tcp(8000)
        val portBindings = Ports().apply {
            bind(exposedPort, Ports.Binding.bindPort(8000))
        }

        events(DockerEvent.StartedCreatingContainer)
        val containerId = dockerClient.createContainerCmd(imageId)
            .withName("http4k-server-shutdown-integration-test")
            .withEnv(
                listOf(
                    "BACKEND=$backend",
                    "STOP_MODE=${stopMode.javaClass.simpleName}"
                )
            )
            .withExposedPorts(exposedPort)
            .withHostConfig(
                HostConfig.newHostConfig()
                    .withLogConfig(LogConfig(LogConfig.LoggingType.JSON_FILE))
                    .withPortBindings(portBindings)
            )
            .exec().id.let(::ContainerId)

        dockerClient.startContainerCmd(containerId.value).exec()

        events(DockerEvent.ContainerCreated)
        waitForEvent(containerId, TestServerEvent.ServerStarted(backend.name, stopMode::class.java.simpleName))
        events(DockerEvent.ServerReady)
        return containerId
    }

    fun eventsFor(containerId: ContainerId): List<ContainerEvent> {
        val list = mutableListOf<ContainerEvent>()
        dockerClient.logContainerCmd(containerId.value)
            .withStdOut(true)
            .withStdErr(true)
            .withTailAll()
            .withSince(0)
            .exec(object : ResultCallback.Adapter<Frame>() {
                override fun onNext(frame: Frame) {
                    val stringPayload = String(frame.payload)
                    val tokens = stringPayload.split("container_event=")
                    if (tokens.size == 2) {
                        list += ContainerEventsJackson.asA<ContainerEvent>(tokens[1])
                    }
                }
            }).awaitCompletion()
        return list.toImmutableList()
    }

    fun waitForEvent(containerId: ContainerId, event: TestServerEvent) {
        val countdown = CountDownLatch(1)
        Thread {
            while (!eventsFor(containerId).map(ContainerEvent::event).contains(event)) {
                Thread.sleep(1000)
            }
            countdown.countDown()
        }.start()
        val succeeded = countdown.await(30, SECONDS)
        if (!succeeded) {
            val logs = StringBuilder()
            dockerClient.logContainerCmd(containerId.value)
                .withStdOut(true)
                .withStdErr(true)
                .withTailAll()
                .withSince(0)
                .exec(object : ResultCallback.Adapter<Frame>() {
                    override fun onNext(frame: Frame) {
                        val stringPayload = String(frame.payload)
                        logs.append(stringPayload)
                    }
                }).awaitCompletion()
            fail("Timed out waiting for event: $event. Latest logs: \n\n$logs")
        }
    }

    fun stop(containerId: ContainerId) {
        events(DockerEvent.ServerStopRequested(containerId.value))
        dockerClient.stopContainerCmd(containerId.value).exec()
        events(DockerEvent.ContainerStopped(containerId.value))
    }

    sealed class DockerEvent : Event {
        object ServerStartedRequested : DockerEvent()
        object WorkspacePrepared : DockerEvent()
        data class RelevantContainersFound(val ids: List<Pair<String, String>>) : DockerEvent()
        object StartedCreatingContainer : DockerEvent()
        object ContainerCreated : DockerEvent()
        data class ContainerKilled(val id:String) : DockerEvent()
        data class ServerStopRequested(val id:String) : DockerEvent()
        data class ContainerStopped(val id:String) : DockerEvent()
        data class ContainerRemoved(val id:String) : DockerEvent()
        object ServerReady : DockerEvent()
    }
}

class PrintEventsInIntelliJ : Events {
    data class DebugEvent(val timestamp: Instant, val event: Event)

    override fun invoke(event: Event) {
        inIntelliJOnly { println(DebugEvent(Instant.now(), event)) }
    }
}

data class ContainerId(val value: String)

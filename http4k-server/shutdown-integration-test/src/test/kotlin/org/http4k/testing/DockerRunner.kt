package org.http4k.testing

import com.github.dockerjava.api.async.ResultCallback.Adapter
import com.github.dockerjava.api.command.BuildImageResultCallback
import com.github.dockerjava.api.model.ExposedPort
import com.github.dockerjava.api.model.Frame
import com.github.dockerjava.api.model.HostConfig.newHostConfig
import com.github.dockerjava.api.model.LogConfig
import com.github.dockerjava.api.model.LogConfig.LoggingType.JSON_FILE
import com.github.dockerjava.api.model.Ports
import com.github.dockerjava.core.DefaultDockerClientConfig
import com.github.dockerjava.core.DockerClientImpl
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient
import com.github.dockerjava.transport.DockerHttpClient
import org.http4k.core.Uri
import org.http4k.core.extend
import java.io.File
import java.nio.file.Files
import java.time.Duration
import java.util.concurrent.TimeUnit


object ProjectFiles {
    private val basePath = Uri.of("http4k-server/shutdown-integration-test")

    private val dockerWorkspace = basePath.extend(Uri.of("build/docker"))
    fun dockerWorkspace(subPath: String) = File(dockerWorkspace.extend(Uri.of(subPath)).path)
    fun project(subPath: String) = File(basePath.extend(Uri.of(subPath)).path)
}

fun main() {
    val config = DefaultDockerClientConfig.createDefaultConfigBuilder().build()

    val http: DockerHttpClient = ApacheDockerHttpClient.Builder()
        .dockerHost(config.dockerHost)
        .sslConfig(config.sslConfig)
        .maxConnections(100)
        .connectionTimeout(Duration.ofSeconds(30))
        .responseTimeout(Duration.ofSeconds(45))
        .build()

    val dockerClient = DockerClientImpl.getInstance(config, http)

    ProjectFiles.dockerWorkspace("/").apply {
        deleteRecursively()
        mkdirs()
    }

    Files.copy(
        ProjectFiles.project("/src/main/resources/Dockerfile").toPath(),
        ProjectFiles.dockerWorkspace("Dockerfile").toPath()
    )

    Files.copy(
        ProjectFiles.project("/build/distributions/http4k-server-shutdown-integration-test-LOCAL.zip").toPath(),
        ProjectFiles.dockerWorkspace("http4k-server-shutdown-integration-test-LOCAL.zip").toPath()
    )

    val imageId = dockerClient.buildImageCmd(ProjectFiles.dockerWorkspace("Dockerfile"))
        .withTags(setOf("http4k-server-shutdown-integration-test"))
        .exec(BuildImageResultCallback())
        .awaitImageId(10, TimeUnit.SECONDS)

    dockerClient.listContainersCmd()
        .withShowAll(true)
        .exec()
        .find { it.names.contains("/http4k-server-shutdown-integration-test") }
        ?.let {
            if (it.state == "running") {
                dockerClient.killContainerCmd(it.id).exec()
            }
            dockerClient.removeContainerCmd(it.id).exec()
        }

    val exposedPort = ExposedPort.tcp(8000)
    val portBindings = Ports().apply {
        bind(exposedPort, Ports.Binding.bindPort(8000))
    }

    val containerId = dockerClient.createContainerCmd(imageId)
        .withName("http4k-server-shutdown-integration-test")
        .withExposedPorts(exposedPort)
        .withHostConfig(newHostConfig()
            .withLogConfig(LogConfig(JSON_FILE))
            .withPortBindings(portBindings))
        .exec().id

    dockerClient.startContainerCmd(containerId).exec()

    dockerClient.logContainerCmd(containerId)
        .withStdOut(true)
        .withStdErr(true)
        .withTailAll()
        .withSince(0)
        .exec(object : Adapter<Frame>() {
            override fun onNext(frame: Frame) {
                println(frame)
            }
        }).awaitCompletion()
}

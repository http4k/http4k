package org.http4k.testing

import com.github.dockerjava.api.async.ResultCallback.Adapter
import com.github.dockerjava.api.command.BuildImageResultCallback
import com.github.dockerjava.api.model.Frame
import com.github.dockerjava.api.model.HostConfig
import com.github.dockerjava.api.model.LogConfig
import com.github.dockerjava.api.model.LogConfig.LoggingType.JSON_FILE
import com.github.dockerjava.core.DefaultDockerClientConfig
import com.github.dockerjava.core.DockerClientImpl
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient
import com.github.dockerjava.transport.DockerHttpClient
import java.io.File
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import java.util.concurrent.TimeUnit

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


    File("http4k-server/shutdown-integration-test/build/docker").deleteRecursively()
    File("http4k-server/shutdown-integration-test/build/docker").mkdirs()
    Files.copy(File("http4k-server/shutdown-integration-test/src/main/resources/Dockerfile").toPath(), File("http4k-server/shutdown-integration-test/build/docker/Dockerfile").toPath())
    Files.copy(File("http4k-server/shutdown-integration-test/build/distributions/http4k-server-shutdown-integration-test-LOCAL.zip").toPath(), File("http4k-server/shutdown-integration-test/build/docker/http4k-server-shutdown-integration-test-LOCAL.zip").toPath())

    val imageId = dockerClient.buildImageCmd(File("http4k-server/shutdown-integration-test/build/docker/Dockerfile"))
        .withTags(setOf("http4k-server-shutdown-integration-test"))
        .exec(BuildImageResultCallback())
        .awaitImageId(10, TimeUnit.SECONDS)

    dockerClient.listContainersCmd()
        .withShowAll(true)
        .exec()
        .find { it.names.contains("/http4k-server-shutdown-integration-test") }
        ?.let {
            if(it.state == "running"){
                dockerClient.killContainerCmd(it.id).exec()
            }
            dockerClient.removeContainerCmd(it.id).exec()
        }

    val containerId = dockerClient.createContainerCmd(imageId)
        .withPortSpecs("8000:8000")
        .withName("http4k-server-shutdown-integration-test")
        .withHostConfig(HostConfig.newHostConfig().withLogConfig(LogConfig(JSON_FILE)))
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

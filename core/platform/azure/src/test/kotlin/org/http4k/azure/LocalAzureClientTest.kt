package org.http4k.azure

import com.azure.core.util.BinaryData
import com.azure.storage.blob.BlobContainerClientBuilder
import com.azure.storage.blob.BlobServiceClientBuilder
import com.azure.storage.common.sas.AccountSasPermission
import com.azure.storage.common.sas.AccountSasResourceType
import com.azure.storage.common.sas.AccountSasService
import com.azure.storage.common.sas.AccountSasSignatureValues
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.client.JavaHttpClient
import org.http4k.util.PortBasedTest
import org.http4k.util.assumeDockerDaemonRunning
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.testcontainers.azure.AzuriteContainer
import org.testcontainers.utility.DockerImageName
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit

@Disabled("waiting for azurite to be updated to support the newest API version")
class LocalAzureClientTest : PortBasedTest {

    init {
        assumeDockerDaemonRunning()
    }

    val container = AzuriteContainer(DockerImageName.parse("mcr.microsoft.com/azure-storage/azurite:3.35.0"))

    @BeforeEach
    fun start() = container.start()

    @AfterEach
    fun tearDown() = container.stop()

    @Test
    fun `should connect to microsoft azure`() {
        val serviceClient =
            BlobServiceClientBuilder().connectionString(container.connectionString).buildClient()

        val containerName = serviceClient.createBlobContainer("test").blobContainerName

        val sasToken =
            serviceClient.generateAccountSas(
                AccountSasSignatureValues(
                    OffsetDateTime.now().plus(10, ChronoUnit.DAYS),
                    AccountSasPermission().setCreatePermission(true),
                    AccountSasService().setBlobAccess(true),
                    AccountSasResourceType().setObject(true),
                )
            )

        val filename = "test-file"
        val contents = buildString { repeat(1025) { append("Hello\n") } }

        BlobContainerClientBuilder()
            .httpClient(AzureHttpClient(JavaHttpClient()))
            .endpoint("http://${container.host}:${container.getMappedPort(10000)}/devstoreaccount1")
            .containerName(containerName)
            .sasToken(sasToken)
            .buildClient().getBlobClient(filename)
            .blockBlobClient.upload(BinaryData.fromString(contents), false)

        val client = serviceClient.getBlobContainerClient(containerName)
        assertThat(client.listBlobs().map { it.name }, equalTo(listOf(filename)))
        assertThat(client.getBlobClient(filename).downloadContent().toString(), equalTo(contents))
    }
}

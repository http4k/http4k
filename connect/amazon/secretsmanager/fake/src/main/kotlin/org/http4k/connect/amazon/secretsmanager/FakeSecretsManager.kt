package org.http4k.connect.amazon.secretsmanager

import org.http4k.aws.AwsCredentials
import org.http4k.chaos.ChaoticHttpHandler
import org.http4k.chaos.start
import org.http4k.connect.amazon.AwsJsonFake
import org.http4k.connect.amazon.core.model.AwsService
import org.http4k.connect.amazon.core.model.Region
import org.http4k.connect.amazon.secretsmanager.model.VersionId
import org.http4k.connect.model.Base64Blob
import org.http4k.connect.model.Timestamp
import org.http4k.connect.storage.InMemory
import org.http4k.connect.storage.Storage
import org.http4k.core.Method.POST
import org.http4k.routing.bind
import org.http4k.routing.routes
import java.time.Clock

data class StoredSecretValue(
    val versionId: VersionId,
    val createdAt: Timestamp,
    val updatedAt: Timestamp,
    val secretString: String? = null,
    val secretBinary: Base64Blob? = null
)

class FakeSecretsManager(
    private val secrets: Storage<StoredSecretValue> = Storage.InMemory(),
    private val clock: Clock = Clock.systemUTC()
) : ChaoticHttpHandler() {

    private val api = AwsJsonFake(SecretsManagerMoshi, AwsService.of("secretsmanager"))

    override val app = routes(
        "/" bind POST to routes(
            api.createSecret(secrets, clock),
            api.deleteSecret(secrets),
            api.getSecret(secrets),
            api.listSecrets(secrets),
            api.putSecret(secrets, clock),
            api.updateSecret(secrets, clock)
        )
    )

    /**
     * Convenience function to get SecretsManager client
     */
    fun client() = SecretsManager.Http(Region.of("ldn-north-1"), { AwsCredentials("accessKey", "secret") }, this, clock)
}

fun main() {
    FakeSecretsManager().start()
}

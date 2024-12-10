package org.http4k.connect.amazon.secretsmanager

import org.http4k.connect.amazon.AwsJsonFake
import org.http4k.connect.amazon.core.model.ARN
import org.http4k.connect.amazon.core.model.AwsAccount
import org.http4k.connect.amazon.core.model.Region
import org.http4k.connect.amazon.secretsmanager.action.CreateSecret
import org.http4k.connect.amazon.secretsmanager.action.CreatedSecret
import org.http4k.connect.amazon.secretsmanager.action.DeleteSecret
import org.http4k.connect.amazon.secretsmanager.action.DeletedSecret
import org.http4k.connect.amazon.secretsmanager.action.GetSecretValue
import org.http4k.connect.amazon.secretsmanager.action.ListSecrets
import org.http4k.connect.amazon.secretsmanager.action.PutSecretValue
import org.http4k.connect.amazon.secretsmanager.action.SecretValue
import org.http4k.connect.amazon.secretsmanager.action.Secrets
import org.http4k.connect.amazon.secretsmanager.action.UpdateSecret
import org.http4k.connect.amazon.secretsmanager.action.UpdatedSecret
import org.http4k.connect.amazon.secretsmanager.action.UpdatedSecretValue
import org.http4k.connect.amazon.secretsmanager.model.Secret
import org.http4k.connect.amazon.secretsmanager.model.SecretId
import org.http4k.connect.amazon.secretsmanager.model.VersionId
import org.http4k.connect.model.Timestamp
import org.http4k.connect.storage.Storage
import java.time.Clock
import java.util.UUID


fun AwsJsonFake.createSecret(
    secrets: Storage<StoredSecretValue>, clock: Clock
) = route<CreateSecret> { req ->
    val versionId = VersionId.of(UUID.randomUUID().toString())
    val createdAt = Timestamp.of(clock.instant().toEpochMilli() / 1000)
    secrets[req.Name] = StoredSecretValue(
        versionId,
        createdAt, createdAt,
        req.SecretString, req.SecretBinary
    )
    CreatedSecret(SecretId.of(req.Name).toArn(), req.Name, versionId)
}

fun AwsJsonFake.deleteSecret(secrets: Storage<StoredSecretValue>) = route<DeleteSecret> { req ->
    val secretId = req.SecretId.resourceId()

    secrets[secretId.value]
        ?.let {
            secrets.remove(secretId.value)
            DeletedSecret(secretId.value, secretId.toArn(), Timestamp.of(0))
        }
}

fun AwsJsonFake.getSecret(secrets: Storage<StoredSecretValue>) = route<GetSecretValue> { req ->
    val secretId = req.SecretId.resourceId()

    secrets.keySet(secretId.value).firstOrNull()
        ?.let { secrets[it] }
        ?.let {
            SecretValue(
                secretId.toArn(),
                it.createdAt,
                secretId.value,
                it.secretBinary,
                it.secretString,
                it.versionId,
                emptyList()
            )
        }
}

fun AwsJsonFake.listSecrets(secrets: Storage<StoredSecretValue>) = route<ListSecrets> {
    Secrets(secrets.keySet("").map {
        Secret(SecretId.of(it).toArn(), it)
    })
}

fun AwsJsonFake.putSecret(
    secrets: Storage<StoredSecretValue>, clock: Clock
) = route<PutSecretValue> { req ->
    val secretId = req.SecretId.resourceId()
    secrets[secretId.value]
        ?.let {
            val versionId = VersionId.of(UUID.randomUUID().toString())
            secrets[secretId.value] = StoredSecretValue(
                versionId,
                it.createdAt,
                Timestamp.of(clock.instant().toEpochMilli() / 1000),
                req.SecretString, req.SecretBinary
            )

            UpdatedSecretValue(secretId.toArn(), secretId.value, versionId)
        }
}

fun AwsJsonFake.updateSecret(
    secrets: Storage<StoredSecretValue>, clock: Clock
) = route<UpdateSecret> { req ->
    val secretId = req.SecretId.resourceId()

    secrets[secretId.value]
        ?.let {
            val versionId = VersionId.of(UUID.randomUUID().toString())
            secrets[secretId.value] = StoredSecretValue(
                versionId,
                it.createdAt,
                Timestamp.of(clock.instant().toEpochMilli() / 1000),
                req.SecretString, req.SecretBinary
            )

            UpdatedSecret(secretId.toArn(), secretId.value, versionId)
        }
}

private fun SecretId.toArn() = ARN.of(
    SecretsManager.awsService,
    Region.of("us-east-1"),
    AwsAccount.of("0"),
    "secret", this
)

fun SecretId.resourceId() = SecretId.of(
    when {
        value.startsWith("arn") -> value.split(":").last()
        else -> value
    }
)

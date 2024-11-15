package org.http4k.connect.amazon.secretsmanager

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.present
import dev.forkhandles.result4k.failureOrNull
import org.http4k.connect.amazon.AwsContract
import org.http4k.connect.amazon.secretsmanager.model.SecretId
import org.http4k.connect.successValue
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.junit.jupiter.api.Test
import java.util.UUID

interface SecretsManagerContract : AwsContract {
    private val sm get() = SecretsManager.Http(aws.region, { aws.credentials }, http)

    val nameOrArn: String
    private val secretValue get() = uuid(1).toString()
    private val updatedValue get() = uuid(2).toString()
    private val finalValue get() = uuid(3).toString()
    val propogateTime: Long get() = 0

    @Test
    fun `secret lifecycle`() {
        try {
            val lookupNothing = sm.getSecretValue(SecretId.of(nameOrArn)).failureOrNull()
            assertThat(lookupNothing?.status, equalTo(BAD_REQUEST))

            val Name = SecretId.of(nameOrArn).resourceId().value
            val creation = sm.createSecret(Name, UUID.randomUUID(), secretValue).successValue()
            assertThat(creation.Name, equalTo(Name))

            Thread.sleep(propogateTime)

            val list = sm.listSecrets().successValue()
            assertThat(list.SecretList.any { it.ARN == creation.ARN }, equalTo(true))

            val lookupCreated = sm.getSecretValue(SecretId.of(nameOrArn)).successValue()
            assertThat(lookupCreated.SecretString, present(equalTo(secretValue)))

            val updated = sm.updateSecret(SecretId.of(nameOrArn), UUID.randomUUID(), updatedValue).successValue()
            assertThat(updated.Name, present(equalTo(Name)))

            val putValue = sm.putSecretValue(SecretId.of(nameOrArn), UUID.randomUUID(), finalValue).successValue()
            assertThat(putValue.Name, present(equalTo(Name)))

            val lookupUpdated = sm.getSecretValue(SecretId.of(nameOrArn)).successValue()
            assertThat(lookupUpdated.SecretString, present(equalTo(finalValue)))

            val deleted = sm.deleteSecret(SecretId.of(nameOrArn)).successValue()
            assertThat(deleted.ARN, present(equalTo(updated.ARN)))

            val lookupDeleted = sm.getSecretValue(SecretId.of(nameOrArn)).failureOrNull()
            assertThat(lookupDeleted?.status, equalTo(BAD_REQUEST))
        } finally {
            sm.deleteSecret(SecretId.of(nameOrArn))
        }
    }
}

fun SecretId.resourceId() = SecretId.of(
    when {
        value.startsWith("arn") -> value.split(":").last()
        else -> value
    }
)

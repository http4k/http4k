package org.http4k.connect.amazon.kms

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.present
import org.http4k.connect.amazon.FakeAwsContract
import org.http4k.connect.storage.InMemory
import org.http4k.connect.storage.Storage
import org.http4k.connect.successValue
import org.junit.jupiter.api.Test
import java.security.SecureRandom

class FakeKMSTest : KMSContract, FakeAwsContract {
    override val http = FakeKMS()

    @Test
    fun `keys can be deterministic`() {
        val kms1Storage = Storage.InMemory<StoredCMK>()
        val kms1 = FakeKMS(kms1Storage, SecureRandom.getInstance("SHA1PRNG").apply { setSeed(42) })
        val key1 = kms1.client().createKey().successValue().KeyMetadata.Arn

        val kms2Storage = Storage.InMemory<StoredCMK>()
        val kms2 = FakeKMS(kms2Storage, SecureRandom.getInstance("SHA1PRNG").apply { setSeed(42) })
        val key2 = kms2.client().createKey().successValue().KeyMetadata.Arn

        assertThat(key1, present())
        assertThat(key1, equalTo(key2))

        assertThat(kms1Storage[key1.toString()], present())
        assertThat(kms1Storage[key1.toString()], equalTo(kms2Storage[key2.toString()]))
    }
}

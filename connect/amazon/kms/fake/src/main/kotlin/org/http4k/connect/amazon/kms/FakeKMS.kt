package org.http4k.connect.amazon.kms

import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.http4k.aws.AwsCredentials
import org.http4k.chaos.ChaoticHttpHandler
import org.http4k.chaos.start
import org.http4k.connect.amazon.AwsJsonFake
import org.http4k.connect.amazon.core.model.AwsService
import org.http4k.connect.amazon.core.model.Region
import org.http4k.connect.storage.InMemory
import org.http4k.connect.storage.Storage
import org.http4k.core.Method.POST
import org.http4k.routing.bind
import org.http4k.routing.routes

class FakeKMS(val keys: Storage<StoredCMK> = Storage.InMemory()) : ChaoticHttpHandler() {

    private val api = AwsJsonFake(KMSMoshi, AwsService.of("TrentService"))
    private val crypto = BouncyCastleProvider()

    override val app = routes(
        "/" bind POST to routes(
            api.createKey(keys, crypto),
            api.describeKey(keys),
            api.decrypt(keys),
            api.encrypt(keys),
            api.getPublicKey(keys),
            api.listKeys(keys),
            api.scheduleKeyDeletion(keys),
            api.sign(keys, crypto),
            api.verify(keys, crypto)
        )
    )

    /**
     * Convenience function to get a KMS client
     */
    fun client() = KMS.Http(Region.of("ldn-north-1"), { AwsCredentials("accessKey", "secret") }, this)
}

fun main() {
    FakeKMS().start()
}


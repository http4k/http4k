package org.http4k.connect.amazon.firehose

import org.http4k.aws.AwsCredentials
import org.http4k.chaos.ChaoticHttpHandler
import org.http4k.chaos.start
import org.http4k.connect.amazon.AwsJsonFake
import org.http4k.connect.amazon.core.model.AwsService
import org.http4k.connect.amazon.core.model.Region
import org.http4k.connect.amazon.model.Record
import org.http4k.connect.storage.InMemory
import org.http4k.connect.storage.Storage
import org.http4k.core.Method.POST
import org.http4k.routing.bind
import org.http4k.routing.routes

class FakeFirehose(val records: Storage<List<Record>> = Storage.InMemory()) : ChaoticHttpHandler() {

    private val api = AwsJsonFake(FirehoseMoshi, AwsService.of("Firehose_20150804"))

    override val app = "/" bind POST to routes(
        api.createDeliveryStream(records),
        api.listDeliveryStreams(records),
        api.deleteDeliveryStream(records),
        api.putRecord(records),
        api.putRecordBatch(records)
    )

    /**
     * Convenience function to get a Firehose client
     */
    fun client() = Firehose.Http(Region.of("ldn-north-1"), { AwsCredentials("accessKey", "secret") }, this)
}

fun main() {
    FakeFirehose().start()
}

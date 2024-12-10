package org.http4k.connect.amazon.apprunner

import org.http4k.aws.AwsCredentials
import org.http4k.chaos.ChaoticHttpHandler
import org.http4k.chaos.start
import org.http4k.connect.amazon.AwsJsonFake
import org.http4k.connect.amazon.apprunner.action.Service
import org.http4k.connect.amazon.apprunner.endpoints.createService
import org.http4k.connect.amazon.apprunner.endpoints.deleteService
import org.http4k.connect.amazon.apprunner.endpoints.listServices
import org.http4k.connect.amazon.core.model.AwsService
import org.http4k.connect.amazon.core.model.Region
import org.http4k.connect.storage.InMemory
import org.http4k.connect.storage.Storage
import org.http4k.routing.routes
import java.time.Clock

class FakeAppRunner(val records: Storage<Service> = Storage.InMemory()) : ChaoticHttpHandler() {

    private val api = AwsJsonFake(AppRunnerMoshi, AwsService.of("AppRunner"))

    override val app = routes(
        api.createService(records),
        api.deleteService(records),
        api.listServices(records)
    )

    /**
     * Convenience function to get AppRunner client
     */
    fun client() = AppRunner.Http(
        Region.of("ldn-north-1"),
        { AwsCredentials("accessKey", "secret") }, this, Clock.systemUTC()
    )
}

fun main() {
    FakeAppRunner().start()
}

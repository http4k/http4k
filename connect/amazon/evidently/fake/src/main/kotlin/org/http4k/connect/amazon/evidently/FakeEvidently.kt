package org.http4k.connect.amazon.evidently

import org.http4k.aws.AwsCredentials
import org.http4k.chaos.ChaoticHttpHandler
import org.http4k.chaos.start
import org.http4k.connect.amazon.AwsRestJsonFake
import org.http4k.connect.amazon.core.model.AwsAccount
import org.http4k.connect.amazon.core.model.AwsService
import org.http4k.connect.amazon.core.model.Region

import org.http4k.connect.storage.InMemory
import org.http4k.connect.storage.Storage
import org.http4k.routing.routes
import java.time.Clock

class FakeEvidently(
    projects: Storage<StoredProject> = Storage.InMemory(),
    features: Storage<StoredFeature> = Storage.InMemory(),
    clock: Clock = Clock.systemUTC(),
    region: Region = Region.US_EAST_1,
    account: AwsAccount = AwsAccount.of("1")
) : ChaoticHttpHandler() {

    private val api =
        AwsRestJsonFake(EvidentlyMoshi, AwsService.of("evidently"), region, account)

    override val app = routes(
        api.createProject(clock, projects, features),
        api.createFeature(clock, projects, features),
        api.updateFeature(clock, projects, features),
        api.evaluateFeature(projects, features),
        api.batchEvaluateFeature(projects, features),
        api.deleteFeature(projects, features),
        api.deleteProject(projects, features)
    )

    /**
     * Convenience function to get a KMS client
     */
    fun client() = Evidently.Http(api.region, { AwsCredentials("accessKey", "secret") }, this)
}

fun main() {
    FakeEvidently().start()
}


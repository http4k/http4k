package org.http4k.connect.amazon.cognitoidentity

import org.http4k.aws.AwsCredentials
import org.http4k.chaos.ChaoticHttpHandler
import org.http4k.chaos.start
import org.http4k.connect.amazon.AwsJsonFake
import org.http4k.connect.amazon.JsonError
import org.http4k.connect.amazon.cognitoidentity.action.GetCredentialsForIdentity
import org.http4k.connect.amazon.cognitoidentity.action.GetId
import org.http4k.connect.amazon.cognitoidentity.action.Identity
import org.http4k.connect.amazon.cognitoidentity.action.IdentityCredentials
import org.http4k.connect.amazon.cognitoidentity.action.TemporaryCredentials
import org.http4k.connect.amazon.cognitoidentity.model.IdentityId
import org.http4k.connect.amazon.core.model.AccessKeyId
import org.http4k.connect.amazon.core.model.AwsService
import org.http4k.connect.amazon.core.model.Region
import org.http4k.connect.amazon.core.model.SecretAccessKey
import org.http4k.connect.amazon.core.model.SessionToken
import org.http4k.connect.model.Timestamp
import org.http4k.connect.storage.InMemory
import org.http4k.connect.storage.Storage
import org.http4k.routing.routes
import java.time.Clock
import java.time.Duration
import java.util.UUID

class FakeCognitoIdentity(
    val identities: Storage<StoredIdentity> = Storage.InMemory(),
    private val region: Region = Region.of("ldn-north-1"),
    private val clock: Clock = Clock.systemUTC(),
    private val expiry: Duration = Duration.ofHours(1),
) : ChaoticHttpHandler() {

    private val api = AwsJsonFake(CognitoIdentityMoshi, AwsService.of("AWSCognitoIdentityService"))

    override val app = routes(
        api.route<GetId> { getId(it) },
        api.route<GetCredentialsForIdentity> { getCredentialsForIdentity(it) },
    )

    private fun getId(request: GetId): Identity {
        val logins = request.Logins.orEmpty()

        synchronized(identities) {
            identities.keySet()
                .mapNotNull { identities[it] }
                .firstOrNull { it.identityPoolId == request.IdentityPoolId && it.logins == logins }
                ?.let { return Identity(it.identityId) }

            val identityId = IdentityId.of("$region:${UUID.randomUUID()}")

            identities[identityId.value] = StoredIdentity(
                identityId = identityId,
                identityPoolId = request.IdentityPoolId,
                logins = logins,
                creationDate = Timestamp.of(clock.instant()),
            )

            return Identity(identityId)
        }
    }

    private fun getCredentialsForIdentity(request: GetCredentialsForIdentity): Any {
        identities[request.IdentityId.value]
            ?: return JsonError("ResourceNotFoundException", "Identity '${request.IdentityId.value}' not found.")

        return IdentityCredentials(
            IdentityId = request.IdentityId,
            Credentials = TemporaryCredentials(
                AccessKeyId = AccessKeyId.of("ASIAFAKEACCESSKEY"),
                SecretKey = SecretAccessKey.of("secret"),
                SessionToken = SessionToken.of("token"),
                Expiration = Timestamp.of(clock.instant().plus(expiry)),
            ),
        )
    }

    fun client() = CognitoIdentity.Http(region, { AwsCredentials("accessKey", "secret") }, this)
}

fun main() {
    FakeCognitoIdentity().start()
}

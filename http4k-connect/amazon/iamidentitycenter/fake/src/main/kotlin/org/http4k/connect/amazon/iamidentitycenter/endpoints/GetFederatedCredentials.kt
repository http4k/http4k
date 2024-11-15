package org.http4k.connect.amazon.iamidentitycenter.endpoints

import org.http4k.connect.amazon.core.model.AccessKeyId
import org.http4k.connect.model.Timestamp
import org.http4k.connect.amazon.core.model.SecretAccessKey
import org.http4k.connect.amazon.core.model.SessionToken
import org.http4k.connect.amazon.iamidentitycenter.IAMIdentityCenterMoshi
import org.http4k.connect.amazon.iamidentitycenter.sso.action.FederatedCredentials
import org.http4k.connect.amazon.iamidentitycenter.sso.action.RoleCredentials
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.with
import org.http4k.lens.Header
import org.http4k.routing.bind
import java.time.Clock

fun getFederatedCredentials(clock: Clock) = "/federation/credentials" bind Method.GET to { req: Request ->
    val token = Header.required("x-amz-sso_bearer_token")(req)
    Response(Status.OK)
        .with(
            IAMIdentityCenterMoshi.autoBody<Any>().toLens() of FederatedCredentials(
                RoleCredentials(
                    AccessKeyId.of("accessKeyId"),
                    SecretAccessKey.of("secretAccessKey"),
                    SessionToken.of(token.reversed()),
                    Timestamp.of(clock.instant().plusSeconds(3600))
                )
            )
        )
}

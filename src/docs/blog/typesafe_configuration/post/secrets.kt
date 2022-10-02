package blog.typesafe_configuration.post

import org.http4k.client.OkHttp
import org.http4k.cloudnative.env.Environment
import org.http4k.cloudnative.env.EnvironmentKey
import org.http4k.cloudnative.env.Secret
import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.then
import org.http4k.filter.ServerFilters
import org.http4k.lens.Lens
import org.http4k.lens.secret

// export USER_PASSWORD=12345
val accessToken: Lens<Environment, Secret> =
    EnvironmentKey.secret().required("USER_PASSWORD")

val secret: Secret = accessToken(Environment.ENV)

val authFilter: Filter = secret.use { value: String ->
    ServerFilters.BearerAuth(value)
}

val authedHttp: HttpHandler = authFilter.then(OkHttp())

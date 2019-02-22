package blog.typesafe_configuration.post

import org.http4k.cloudnative.env.Environment
import org.http4k.cloudnative.env.EnvironmentKey
import org.http4k.core.Filter
import org.http4k.filter.ServerFilters
import org.http4k.lens.secret

val accessToken = EnvironmentKey.secret().required("USER_PASSWORD")

val secret = (accessToken(Environment.ENV))

val auth: Filter = secret.use { ServerFilters.BearerAuth(it) }

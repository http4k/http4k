package server.extensive

import org.http4k.config.EnvironmentKey
import org.http4k.lens.basicCredentials
import org.http4k.lens.of
import org.http4k.lens.uri

object Settings {
    val BASE_URL by EnvironmentKey.uri().of().required()
    val CREDENTIALS by EnvironmentKey.basicCredentials().of().required()
}

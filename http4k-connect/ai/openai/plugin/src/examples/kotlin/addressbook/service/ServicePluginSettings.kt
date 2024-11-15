package addressbook.service

import org.http4k.config.EnvironmentKey
import org.http4k.connect.openai.model.Email
import org.http4k.connect.openai.model.VerificationToken
import org.http4k.lens.int
import org.http4k.lens.of
import org.http4k.lens.uri
import org.http4k.lens.value
import org.http4k.security.AccessToken

/**
 * Defines the settings which should exist in the Environment at runtime
 */
object ServicePluginSettings {
    val PORT by EnvironmentKey.int().of().defaulted(9000)
    val PLUGIN_BASE_URL by EnvironmentKey.uri().of().required()
    val EMAIL by EnvironmentKey.value(Email).of().required()
    val OPENAI_API_KEY by EnvironmentKey.map({ AccessToken(it) }, AccessToken::value).of().required()
    val OPENAI_VERIFICATION_TOKEN by EnvironmentKey.value(VerificationToken).of().required()
}

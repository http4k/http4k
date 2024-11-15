package addressbook.oauth

import addressbook.shared.credentials
import org.http4k.config.EnvironmentKey
import org.http4k.connect.openai.auth.OpenAIPluginId
import org.http4k.connect.openai.model.Email
import org.http4k.connect.openai.model.VerificationToken
import org.http4k.core.Uri
import org.http4k.lens.composite
import org.http4k.lens.int
import org.http4k.lens.of
import org.http4k.lens.uri
import org.http4k.lens.value

/**
 * Defines the settings which should exist in the Environment at runtime
 */
object OAuthPluginSettings {
    val PORT by EnvironmentKey.int().of().required()
    val PLUGIN_BASE_URL by EnvironmentKey.uri().of().required()
    val EMAIL by EnvironmentKey.value(Email).of().required()
    val COOKIE_DOMAIN by EnvironmentKey.of().required()
    val OPENAI_PLUGIN_ID by EnvironmentKey.value(OpenAIPluginId).of().required()
    val OPENAI_CLIENT_CREDENTIALS by EnvironmentKey.credentials().of().required()
    val OPENAI_VERIFICATION_TOKEN by EnvironmentKey.value(VerificationToken).of().required()
    val REDIRECTION_URLS = EnvironmentKey.uri().multi.defaulted("REDIRECTION_URLS",
        EnvironmentKey.composite {
            listOf(Uri.of("https://chat.openai.com/aip/plugin-${OPENAI_PLUGIN_ID(it)}/oauth/callback"))
        }
    )
}

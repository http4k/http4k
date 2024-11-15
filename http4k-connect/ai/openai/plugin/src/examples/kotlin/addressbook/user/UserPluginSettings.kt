package addressbook.user

import org.http4k.config.EnvironmentKey
import org.http4k.connect.openai.model.Email
import org.http4k.core.Uri
import org.http4k.lens.int
import org.http4k.lens.of
import org.http4k.lens.uri
import org.http4k.lens.value

/**
 * Defines the settings which should exist in the Environment at runtime
 */
object UserPluginSettings {
    val PORT by EnvironmentKey.int().of().defaulted(9000)
    val PLUGIN_BASE_URL by EnvironmentKey.uri().of().defaulted(Uri.of("http://localhost:9000"))
    val EMAIL by EnvironmentKey.value(Email).of().defaulted(Email.of("foo@bar"))
}

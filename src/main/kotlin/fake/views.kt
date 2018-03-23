package fake

import org.http4k.core.Uri
import org.http4k.template.ViewModel

data class Index(val serviceName: String) : ViewModel

data class OAuthLogin(val serviceName: String, val callbackUri: Uri, val error: String? = null) : ViewModel
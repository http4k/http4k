package org.http4k.testing

import org.http4k.core.Uri
import org.http4k.template.ViewModel

data class OAuthIndex(val serviceName: String) : ViewModel

data class OAuthLogin(val serviceName: String, val callbackUri: Uri, val error: String? = null) : ViewModel

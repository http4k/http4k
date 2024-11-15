package org.http4k.connect.openai.auth.oauth

import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.lens.LensExtractor

/**
 * Handles the method by which the Principal for the plugin is authenticated. Typically this is a login
 * page, but could be a biometric challenge.
 */
interface PrincipalChallenge<Principal> : LensExtractor<Request, Principal> {
    val challenge: HttpHandler
    val handleChallenge: Filter
}

package org.http4k.playwright

import com.microsoft.playwright.Page
import org.http4k.core.Uri
import org.http4k.core.extend

/**
 * Custom Page implementation to add convenience functions to the standard Playwright model
 */
class HttpPage(delegate: Page, private val baseUri: Uri) : Page by delegate {

    /**
     * Navigate to an http4k application route, based on the base Uri of our application.
     */
    fun navigate(uri: Uri = baseUri) = super.navigate(baseUri.extend(uri).toString())
}

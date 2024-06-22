package org.http4k.playwright

import com.microsoft.playwright.Page
import com.microsoft.playwright.Response
import org.http4k.core.Uri
import org.http4k.core.extend

/**
 * Custom Page implementation to add convenience functions to the standard Playwright model
 */
class HttpPage(delegate: Page, private val baseUri: Uri) : Page by delegate {

    /**
     * Navigates to the base URL of the http4k application
     */
    fun navigateHome() = navigate(baseUri)

    /**
     * Navigates to an arbitrary URL
     */
    fun navigate(uri: Uri) = navigate(uri.toString())

    /**
     * Navigate to a URL. If the scheme is not set, we use the base URL of the http4k application
     */
    override fun navigate(uri: String): Response = super.navigate(
        when (Uri.of(uri).scheme) {
            "" -> baseUri.extend(Uri.of(uri)).toString()
            else -> uri
        }
    )
}

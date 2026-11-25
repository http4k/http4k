package org.http4k.playwright

import com.microsoft.playwright.CDPSession
import com.microsoft.playwright.Page
import com.microsoft.playwright.Response
import org.http4k.core.Uri
import org.http4k.core.extend

/**
 * Custom Page implementation to add convenience functions to the standard Playwright model.
 */
class HttpPage(private val playwrightPage: Page, private val baseUri: Uri) : Page by playwrightPage {

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
    ) ?: error("No response from call to: $uri")

    /**
     * Opens a Chrome DevTools Protocol session on this page (Chromium only)
     */
    fun devTools(): CDPSession = context().newCDPSession(playwrightPage)
}

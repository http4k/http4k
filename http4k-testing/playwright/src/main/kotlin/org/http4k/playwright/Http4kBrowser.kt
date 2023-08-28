package org.http4k.playwright

import com.microsoft.playwright.Browser
import org.http4k.core.Uri

/**
 * Custom Page implementation to add convenience functions to the standard Playwright model
 */
class Http4kBrowser(delegate: Browser, val baseUri: Uri) : Browser by delegate {
    override fun newPage(): HttpPage = HttpPage(super.newPage(), baseUri)
}

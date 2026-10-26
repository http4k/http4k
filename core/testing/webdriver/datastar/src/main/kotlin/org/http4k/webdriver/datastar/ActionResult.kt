package org.http4k.webdriver.datastar

import org.http4k.core.Method
import org.http4k.core.Status
import org.http4k.core.Uri

/**
 * A backend action the page fired, as the browser's network log would show it.
 */
data class ActionResult(val method: Method, val uri: Uri, val status: Status)

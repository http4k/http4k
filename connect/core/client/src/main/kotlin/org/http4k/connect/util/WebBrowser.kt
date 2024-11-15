package org.http4k.connect.util

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import org.http4k.core.Uri

class WebBrowser(
    private val open: OpenBrowser = { Runtime.getRuntime().exec(it.split(" ").toTypedArray()) },
    private val operatingSystemName: String = System.getProperty("os.name")
) : Browser {
    override fun navigateTo(url: Uri) =
        try {
            when {
                operatingSystemName.startsWith("Mac OS") -> open("open $url")
                operatingSystemName.startsWith("Windows") -> open("rundll32 url.dll,FileProtocolHandler $url")
                else -> linuxBrowser(url)
            }

            Success(Unit)
        } catch (e: Exception) {
            Failure(Exception("Could not open system browser"))
        }

    private fun linuxBrowser(url: Uri) {
        var browser: String? = null
        for (b in Browser.entries) {
            if (browser == null && open("which ${b.name}").inputStream.read() != -1) {
                open("${b.name.also { browser = b.name }} $url")
            }
        }
        if (browser == null) throw Exception("No web browser found")
    }

    private enum class Browser {
        `google-chrome`,
        firefox, mozilla, epiphany,
        konqueror, netscape, opera, links, lynx, chromium,
        `brave-browser`
    }
}


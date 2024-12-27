package org.http4k.connect.amazon.iamidentitycenter

import org.http4k.connect.util.WebBrowser
import org.http4k.core.Uri
import org.http4k.server.ServerConfig
import org.http4k.server.ServerConfig.StopMode.Graceful
import org.http4k.server.SunHttp
import java.time.Duration

sealed interface SSOLogin {
    companion object {
        fun enabled(
            openBrowser: (Uri) -> Any = WebBrowser()::navigateTo,
            waitFor: (Long) -> Unit = Thread::sleep,
            serverConfig: ServerConfig = SunHttp(0, stopMode = Graceful(Duration.ofSeconds(2))),
            forceRefresh: Boolean = true
        ) = SSOLoginEnabled(openBrowser, waitFor, serverConfig, forceRefresh)

        val disabled = SSOLoginDisabled
    }
}

class SSOLoginEnabled(
    val openBrowser: (Uri) -> Any,
    val waitFor: (Long) -> Unit,
    val serverConfig: ServerConfig,
    val forceRefresh: Boolean
) : SSOLogin

data object SSOLoginDisabled : SSOLogin

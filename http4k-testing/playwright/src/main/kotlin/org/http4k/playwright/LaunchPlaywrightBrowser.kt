package org.http4k.playwright

import com.microsoft.playwright.Browser
import com.microsoft.playwright.BrowserType
import com.microsoft.playwright.BrowserType.LaunchOptions
import com.microsoft.playwright.Playwright
import com.microsoft.playwright.Playwright.create
import org.http4k.core.HttpHandler
import org.http4k.core.Uri
import org.http4k.server.ServerConfig
import org.http4k.server.SunHttp
import org.http4k.server.asServer
import org.junit.jupiter.api.extension.AfterTestExecutionCallback
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver
import java.util.concurrent.atomic.AtomicReference

class LaunchPlaywrightBrowser @JvmOverloads constructor(
    http: HttpHandler,
    private val browserType: Playwright.() -> BrowserType = Playwright::chromium,
    private val launchOptions: LaunchOptions = LaunchOptions(),
    private val createPlaywright: () -> Playwright = ::create,
    serverFn: (Int) -> ServerConfig = ::SunHttp
) : ParameterResolver, BeforeTestExecutionCallback, AfterTestExecutionCallback {

    override fun supportsParameter(pc: ParameterContext, ec: ExtensionContext) =
        pc.parameter.parameterizedType.typeName == Browser::class.java.name ||
            pc.parameter.parameterizedType.typeName == Http4kBrowser::class.java.name

    private val playwright = AtomicReference<Playwright>()

    private val server = http.asServer(serverFn(0))

    override fun resolveParameter(pc: ParameterContext, ec: ExtensionContext) = Http4kBrowser(
        browserType(playwright.get()).launch(launchOptions),
        Uri.of("http://localhost:${server.port()}")
    )

    override fun beforeTestExecution(context: ExtensionContext?) {
        playwright.set(createPlaywright())
        server.start()
    }

    override fun afterTestExecution(context: ExtensionContext) {
        server.stop()
        playwright.get().close()
    }
}


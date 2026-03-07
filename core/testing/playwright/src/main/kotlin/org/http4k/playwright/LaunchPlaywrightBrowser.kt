package org.http4k.playwright

import com.microsoft.playwright.Browser
import com.microsoft.playwright.BrowserType
import com.microsoft.playwright.BrowserType.LaunchOptions
import com.microsoft.playwright.Playwright
import com.microsoft.playwright.Playwright.create
import org.http4k.core.HttpHandler
import org.http4k.core.PolyHandler
import org.http4k.core.Uri
import org.http4k.server.Http4kServer
import org.http4k.server.PolyServerConfig
import org.http4k.server.ServerConfig
import org.http4k.server.SunHttp
import org.http4k.server.asServer
import org.http4k.server.uri
import org.junit.jupiter.api.extension.AfterTestExecutionCallback
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver
import java.util.concurrent.atomic.AtomicReference

class LaunchPlaywrightBrowser @JvmOverloads constructor(
    private val server: Http4kServer,
    private val browserType: Playwright.() -> BrowserType = Playwright::chromium,
    private val launchOptions: LaunchOptions = LaunchOptions(),
    private val createPlaywright: () -> Playwright = ::create,
) : ParameterResolver, BeforeTestExecutionCallback, AfterTestExecutionCallback {

    @JvmOverloads
    constructor(
        http: HttpHandler,
        browserType: Playwright.() -> BrowserType = Playwright::chromium,
        launchOptions: LaunchOptions = LaunchOptions(),
        createPlaywright: () -> Playwright = ::create,
        serverFn: (Int) -> ServerConfig = ::SunHttp
    ) : this(http.asServer(serverFn(0)), browserType, launchOptions, createPlaywright)

    @JvmOverloads
    constructor(
        poly: PolyHandler,
        serverFn: (Int) -> PolyServerConfig,
        browserType: Playwright.() -> BrowserType = Playwright::chromium,
        launchOptions: LaunchOptions = LaunchOptions(),
        createPlaywright: () -> Playwright = ::create,
    ) : this(poly.asServer(serverFn(0)), browserType, launchOptions, createPlaywright)

    override fun supportsParameter(pc: ParameterContext, ec: ExtensionContext) =
        pc.parameter.parameterizedType.typeName == Browser::class.java.name ||
            pc.parameter.parameterizedType.typeName == Http4kBrowser::class.java.name

    private val playwright = AtomicReference<Playwright>()

    override fun resolveParameter(pc: ParameterContext, ec: ExtensionContext) = Http4kBrowser(
        browserType(playwright.get()).launch(launchOptions),
        server.uri()
    )

    override fun beforeTestExecution(context: ExtensionContext) {
        playwright.set(createPlaywright())
        server.start()
    }

    override fun afterTestExecution(context: ExtensionContext) {
        server.stop()
        playwright.get().close()
    }
}


package org.http4k.webdriver

import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Status
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.ClientFilters
import org.http4k.filter.cookie.CookieStorage
import org.http4k.filter.cookie.LocalCookie
import org.openqa.selenium.Alert
import org.openqa.selenium.By
import org.openqa.selenium.Cookie
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebDriver.Navigation
import org.openqa.selenium.WebElement
import org.openqa.selenium.WindowType
import java.net.URL
import java.nio.file.Paths
import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset.UTC
import java.util.*
import org.http4k.core.cookie.Cookie as HCookie

typealias Navigate = (Request) -> Unit
typealias GetURL = () -> String?

interface Http4KNavigation : Navigation {
    fun to(uri: Uri)
}

class Http4kWebDriver(initialHandler: HttpHandler, clock: Clock = Clock.systemDefaultZone()) : WebDriver {
    val handler =
        Filter { next -> { request -> next(request.header("host", latestHost)) } }
            .then(ClientFilters.FollowRedirects())
            .then(ClientFilters.Cookies(clock, cookieStorage()))
            .then(Filter { next -> { request -> latestUri = request.uri.toString(); next(request) } })
            .then(initialHandler)

    private var current: Page? = null
    private var activeElement: WebElement? = null
    private val siteCookies = mutableMapOf<String, StoredCookie>()
    private var latestUri: String = ""
    private var latestHost: String? = null

    private fun navigateTo(request: Request) {
        val normalizedPath = request.uri(request.uri.path(normalized(request.uri.path)))
        val host = request.uri.host + (request.uri.port?.let { ":$it" } ?: "")
        if (host.isNotEmpty()) latestHost = host
        val response = handler(normalizedPath)
        current = Page(
            response.status,
            this::navigateTo,
            { currentUrl },
            UUID.randomUUID(),
            normalized(latestUri),
            response.bodyString(),
            current
        )
    }

    private fun normalized(path: String) = when {
        Regex("http[s]?://.*").matches(path) -> path
        else -> {
            val newPath = when {
                path.startsWith("/") -> path
                else -> {
                    val currentPath = currentUrl?.let {
                        Uri.of(it).path.let { it.ifEmpty { "/" } }
                    } ?: "/"
                    when {
                        currentPath.endsWith("/") -> currentPath.appendToPath(path)
                        else -> {
                            val pathParts = Paths.get(currentPath).toList()
                            val newPathParts = Paths.get(path).toList()
                            val newPath = when {
                                path.isEmpty() -> pathParts
                                else -> pathParts.dropLast(1) + newPathParts
                            }
                            newPath.joinToString(separator = "/")
                        }
                    }
                }
            }
            newPath.normalizePath()
        }
    }

    private fun String.appendToPath(pathToAppend: String): String {
        val newPath = StringBuilder(this)
        if (!this.endsWith("/") && !pathToAppend.startsWith("/")) newPath.append("/")
        newPath.append(pathToAppend)
        return newPath.toString()
    }

    private fun String.normalizePath(): String {
        if (this == "/") return this
        val pathParts = this.split("/").filter { part -> part != "." }
        val newPathParts = mutableListOf<String>()
        pathParts.forEachIndexed { index, part ->
            if ((index < pathParts.lastIndex && pathParts[index + 1] != "..") || index == pathParts.lastIndex)
                if (part != "..") newPathParts.add(part)
        }
        var normalizedPath = newPathParts.joinToString(separator = "/")
        if (!normalizedPath.startsWith("/")) normalizedPath = "/$normalizedPath"
        return normalizedPath
    }

    private fun HCookie.toWebDriver(): Cookie = Cookie(
        name, value, domain, path,
        expires?.let { Date.from(it.atZone(ZoneId.systemDefault()).toInstant()) }, secure, httpOnly
    )

    private fun LocalCookie.toWebDriver(): StoredCookie = StoredCookie(cookie.toWebDriver(), this)

    override fun get(url: String) {
        navigateTo(Request(GET, url).body(""))
    }

    fun get(uri: Uri) = get(uri.toString())

    override fun getCurrentUrl(): String? = current?.url

    override fun getTitle(): String? = current?.title

    val status: Status?
        get() = current?.status

    override fun findElements(by: By): List<WebElement>? = current?.findElements(by)

    override fun findElement(by: By): WebElement? = current?.findElements(by)?.firstOrNull()

    override fun getPageSource(): String? = current?.contents

    override fun close() {
        current = null
    }

    override fun quit() {
        current = null
    }

    override fun getWindowHandles(): Set<String> = current?.let { setOf(it.handle.toString()) } ?: emptySet()

    override fun getWindowHandle(): String? = windowHandles.firstOrNull()

    override fun switchTo(): WebDriver.TargetLocator = object : WebDriver.TargetLocator {
        override fun frame(index: Int): WebDriver = throw FeatureNotImplementedYet

        override fun frame(nameOrId: String?): WebDriver = throw FeatureNotImplementedYet

        override fun frame(frameElement: WebElement?): WebDriver = throw FeatureNotImplementedYet

        override fun parentFrame(): WebDriver = throw FeatureNotImplementedYet

        override fun alert(): Alert = throw FeatureNotImplementedYet

        override fun activeElement(): WebElement = activeElement ?: current?.firstElement()
        ?: throw NoSuchElementException("no page loaded!")

        override fun window(nameOrHandle: String?): WebDriver =
            if (current?.handle?.toString() != nameOrHandle) throw NoSuchElementException("window with handle$nameOrHandle") else this@Http4kWebDriver

        override fun newWindow(typeHint: WindowType?) = throw FeatureNotImplementedYet

        override fun defaultContent(): WebDriver = this@Http4kWebDriver
    }

    override fun navigate(): Http4KNavigation = object : Http4KNavigation {
        override fun to(url: String) = get(url)

        override fun to(url: URL) = get(url.toString())

        override fun forward() {
            current?.next?.let { current = it }
        }

        override fun refresh() {
            current?.let {
                current = it.copy(contents = handler(Request(GET, it.url)).bodyString())
            }
        }

        override fun back() {
            current?.previous?.let { current = it.copy(next = current) }
        }

        override fun to(uri: Uri) = get(uri.toString())
    }

    override fun manage() = object : WebDriver.Options {
        override fun addCookie(cookie: Cookie) {
            siteCookies[cookie.name] = StoredCookie(
                cookie,
                LocalCookie(HCookie(cookie.name, cookie.value), LocalDateTime.now().toInstant(UTC))
            )
        }

        override fun getCookies() = siteCookies.values.map { it.cookie }.toSet()

        override fun deleteCookieNamed(name: String?) {
            siteCookies.remove(name)
        }

        override fun getCookieNamed(name: String) = siteCookies[name]?.cookie

        override fun deleteAllCookies() {
            siteCookies.clear()
        }

        override fun deleteCookie(cookie: Cookie) {
            siteCookies.remove(cookie.name)
        }

        override fun logs() = throw FeatureNotImplementedYet

        override fun timeouts() = throw FeatureNotImplementedYet

        override fun window() = throw FeatureNotImplementedYet
    }

    private fun cookieStorage() = object : CookieStorage {
        override fun store(cookies: List<LocalCookie>) {
            cookies.forEach { siteCookies[it.cookie.name] = it.toWebDriver() }
        }

        override fun remove(name: String) {
            siteCookies.remove(name)
        }

        override fun retrieve(): List<LocalCookie> = siteCookies.entries.map { it.value.localCookie }
    }

    private data class StoredCookie(val cookie: Cookie, val localCookie: LocalCookie)
}

/**
 * DSL-helper so we can use this webdriver in a lambda-with-receiver context
 */
operator fun Http4kWebDriver.invoke(fn: Http4kWebDriver.() -> Unit): Http4kWebDriver = apply(fn)

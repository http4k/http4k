package org.http4k.webdriver

import org.openqa.selenium.By
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement

class HtmxHttp4kWebDriver(val driver: Http4kWebDriver) : WebDriver {

    private fun toHtmx(element: WebElement): HtmxJsoupWebElement =
        when (element) {
            is JSoupWebElement -> HtmxJsoupWebElement(element, driver.handler)
            else -> throw RuntimeException("could not convert $element to HtmxJsoupWebElement")
        }

    override fun findElements(by: By): List<WebElement>? = driver.findElements(by)?.map { toHtmx(it) }

    override fun findElement(by: By): WebElement? = driver.findElement(by)?.let { toHtmx(it) }

    override fun get(url: String) = driver.get(url)

    override fun getCurrentUrl(): String? = driver.getCurrentUrl()

    override fun getTitle(): String? = driver.getTitle()

    override fun getPageSource(): String? = driver.getPageSource()

    override fun close() = driver.close()

    override fun quit() = driver.quit()

    override fun getWindowHandles(): Set<String> = driver.getWindowHandles()

    override fun getWindowHandle(): String? = driver.getWindowHandle()

    override fun switchTo(): WebDriver.TargetLocator = driver.switchTo()

    override fun navigate(): WebDriver.Navigation = driver.navigate()

    override fun manage(): WebDriver.Options = driver.manage()
}

fun Http4kWebDriver.withHtmx(): HtmxHttp4kWebDriver = HtmxHttp4kWebDriver(this)

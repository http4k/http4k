import org.http4k.client.ApacheClient
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.ClientFilters
import org.http4k.filter.RequestFilters
import org.http4k.webdriver.Http4kWebDriver
import org.openqa.selenium.By

sealed class SpiderReport

data class Ok(val original: String, val sublinks: List<SpiderReport>) : SpiderReport()
data class Unreachable(val original: String) : SpiderReport()

fun main() {
    val handler = ClientFilters.SetHostFrom(Uri.of("https://www.http4k.org"))
        .then(RequestFilters.Tap { println(it.uri) })
        .then(ClientFilters.FollowRedirects())
        .then(ApacheClient())
    val driver = Http4kWebDriver(handler)

    fun spider(url: String, visited: List<String> = emptyList()): SpiderReport = try {
        val actualUrl = "/${url.trimStart('/')}"
        println("visiting: $actualUrl")
        driver.get(actualUrl)
        val nowVisited = visited.plus(actualUrl)

        val sublinks = driver.findElements(By.tagName("a"))
            ?.map { it.getAttribute("href") }
            ?.filter {
                it != "." &&
                    !it.startsWith("#") &&
                    !it.startsWith("http") &&
                    !nowVisited.contains(it)
            }
            ?.map { spider(it, nowVisited) } ?: emptyList()

        Ok(actualUrl, sublinks)
    } catch (e: Exception) {
        Unreachable(url)
    }

    println(spider("/"))
}

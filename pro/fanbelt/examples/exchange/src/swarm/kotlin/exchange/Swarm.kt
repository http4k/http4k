package exchange

import com.microsoft.playwright.Browser.NewContextOptions
import com.microsoft.playwright.BrowserType.LaunchOptions
import com.microsoft.playwright.Page
import com.microsoft.playwright.Playwright
import exchange.domain.SYMBOLS
import java.util.Random
import java.util.concurrent.atomic.AtomicLong
import kotlin.concurrent.thread

private val BOTS = System.getenv("BOTS")?.toInt() ?: 30
private val BROWSERS = System.getenv("BROWSERS")?.toInt() ?: 3
private val NODES = System.getenv("NODES")?.toInt() ?: 3
private val HEADED = System.getenv("HEADED") == "true"
private val PAUSE = System.getenv("PAUSE_MS")?.toLong() ?: if (HEADED) 2500 else 0
private val ACCEPT_ENCODING: String? = System.getenv("ACCEPT_ENCODING")?.takeIf(String::isNotBlank)

private val actions = AtomicLong()
private val failures = AtomicLong()
private val wire = AtomicLong()
private val decoded = AtomicLong()

fun main() {
    (0 until BROWSERS).map { browser ->
        thread(name = "browser-$browser") { swarm(browser, (0 until BOTS).filter { it % BROWSERS == browser }) }
    }
    val started = System.nanoTime()
    while (true) {
        Thread.sleep(10_000)
        val seconds = (System.nanoTime() - started) / 1e9
        println("%d bots: %d actions (%.1f/s), %d failures, %d KB on the wire for %d KB of content".format(BOTS, actions.get(), actions.get() / seconds, failures.get(), wire.get() / 1024, decoded.get() / 1024))
    }
}

private fun swarm(browser: Int, ids: List<Int>) {
    val random = Random(browser.toLong())
    Playwright.create().use { playwright ->
        val window = listOf("--window-position=${browser % 2 * 1275},${browser / 2 % 2 * 720}", "--window-size=1275,720")
        val chromium = playwright.chromium().launch(LaunchOptions().setHeadless(!HEADED).setArgs(window))
        val bots = ids.map { id ->
            val context = chromium.newContext(NewContextOptions().setBaseURL("http://localhost:${9000 + id % NODES}/venues/demo/").setViewportSize(null))
            if (ACCEPT_ENCODING != null) context.setExtraHTTPHeaders(mapOf("Accept-Encoding" to ACCEPT_ENCODING))
            val page = context.newPage()
            val network = context.newCDPSession(page)
            network.send("Network.enable")
            network.on("Network.dataReceived") { event ->
                wire.addAndGet(event["encodedDataLength"].asLong)
                decoded.addAndGet(event["dataLength"].asLong)
            }
            Bot(id, page, random)
        }
        bots.forEach { it.page.setDefaultTimeout(3000.0) }
        while (true) {
            bots.forEach { bot ->
                runCatching { bot.act() }.onSuccess { actions.incrementAndGet() }.onFailure { failures.incrementAndGet(); System.err.println("bot ${bot.id}: ${it.message?.lines()?.first()}") }
            }
            if (PAUSE > 0) Thread.sleep(PAUSE / 2 + random.nextLong(PAUSE))
        }
    }
}

internal enum class Strategy { MarketMaker, Noise, Momentum }

private class Bot(val id: Int, val page: Page, private val random: Random) {
    private val strategy = Strategy.entries[id % Strategy.entries.size]
    private var symbol = SYMBOLS[random.nextInt(SYMBOLS.size)]
    private var nextHop = hop()

    private fun hop() = System.currentTimeMillis() + 20_000 + random.nextInt(20_000)

    fun act() {
        if (page.url() == "about:blank") page.navigate("s/$symbol")
        if (System.currentTimeMillis() > nextHop) {
            symbol = (SYMBOLS - symbol)[random.nextInt(SYMBOLS.size - 1)]
            page.click("#ticker a[href=\"s/$symbol\"]")
            page.waitForURL("**/s/$symbol")
            nextHop = hop()
        }
        val mark = page.textContent("#ticker a[href=\"s/$symbol\"] .flash").orEmpty().trim().split(" ").first().toDoubleOrNull() ?: return
        when (strategy) {
            Strategy.MarketMaker -> {
                if (page.locator("button.cancel").count() > 6) page.locator("button.cancel").last().click()
                val spread = mark * (0.001 + random.nextDouble() * 0.004)
                order("buy", "%.2f".format(mark - spread), 5 + random.nextInt(15))
                order("sell", "%.2f".format(mark + spread), 5 + random.nextInt(15))
            }
            Strategy.Noise -> order(if (random.nextBoolean()) "buy" else "sell", "", 1 + random.nextInt(10))
            Strategy.Momentum -> {
                val prints = page.locator("#tape tr td:nth-child(2)")
                if (prints.count() == 0) return
                val last = prints.first().textContent()?.toDoubleOrNull() ?: return
                if (random.nextInt(3) == 0) order(if (last > mark) "buy" else "sell", "", 1 + random.nextInt(8))
            }
        }
    }

    private fun order(side: String, price: String, quantity: Int) {
        page.fill("[data-bind\\:price]", price)
        page.fill("[data-bind\\:quantity]", quantity.toString())
        page.click("button.$side")
    }
}
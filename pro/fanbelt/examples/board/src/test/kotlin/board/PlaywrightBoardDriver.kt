package board

import board.domain.BoardId
import board.domain.BuildStatus
import board.domain.CardText
import board.domain.Column
import board.domain.Commit
import board.domain.Direction
import board.domain.Role
import board.domain.Team
import board.domain.Viewer
import board.domain.ViewerId
import board.store.Teams
import board.web.viewerCookie
import com.microsoft.playwright.Locator
import com.microsoft.playwright.Page
import com.microsoft.playwright.TimeoutError
import org.http4k.fanbelt.infrastructure.AppInfra
import org.http4k.fanbelt.infrastructure.AppRuntime
import org.http4k.testing.toHttpHandler
import org.http4k.fanbelt.store.DocumentStorage
import org.http4k.fanbelt.store.memory.InMemoryDocumentStorage
import org.http4k.fanbelt.tenancy.TenantId
import org.http4k.fanbelt.testing.RealBrowser
import org.http4k.fanbelt.testing.Verdict
import org.http4k.fanbelt.testing.posting
import org.http4k.fanbelt.testing.provesServerRefuses
import org.http4k.fanbelt.testing.start
import org.http4k.fanbelt.testing.textAs
import org.http4k.fanbelt.testing.verdict
import org.http4k.server.Jetty
import org.http4k.server.uri
import org.jsoup.Jsoup
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable

class PlaywrightBoardDriver(private val storage: DocumentStorage = InMemoryDocumentStorage()) : BoardDriver, AutoCloseable {
    private val app = App(AppRuntime(), { AppInfra(it, storage) })
    private val server = app.asServer(Jetty(0)).start()
    private val browser = RealBrowser(server.uri()).apply { start() }

    init {
        Teams(storage).create(TenantId.of("acme"), Team(start))
    }

    override fun member(name: String): BoardMember = PlaywrightMember(viewerNamed(name))

    override fun ciReports(board: BoardId, status: String, commit: String) = app.handler.toHttpHandler().reportsBuild(board, status, commit)

    override fun close() {
        browser.close()
        server.stop()
    }

    private inner class PlaywrightMember(override val id: ViewerId) : BoardMember {
        override val name = Viewer(id).name
        private var page: Page? = null
        private var on: BoardId? = null

        override fun opens(board: BoardId) {
            visit(board)
        }

        override fun watches(board: BoardId) {
            visit(board)
        }

        override fun adds(board: BoardId, text: String): Verdict {
            val page = visit(board)
            val add = page.locator("form.add input")
            try {
                add.waitFor(Locator.WaitForOptions().setTimeout(2000.0))
            } catch (_: TimeoutError) {
                return page.provesServerRefuses("/teams/acme/b/${board.value}/cards", """{"text":"$text"}""")
            }
            return page.verdict("form.add .error") {
                add.fill(text)
                add.press("Enter")
            }
        }

        override fun moves(board: BoardId, text: CardText, direction: Direction) {
            visit(board).posting { card(board, text).locator("button[aria-label='Move ${direction.name.lowercase()}']").click() }
        }

        override fun deletes(board: BoardId, text: CardText) {
            visit(board).posting { card(board, text).locator("button[aria-label='Delete']").click() }
        }

        override fun makesEditor(board: BoardId, other: BoardMember) {
            val page = visit(board)
            page.posting { page.locator("#viewer-${other.id} button").click() }
        }

        override fun sees(board: BoardId): BoardSeen {
            val page = visit(board)
            return BoardSeen(
                page.locator("#cards section").all().associate { column ->
                    column.locator("h2").textAs<Column>() to column.locator("article > span:not(.controls)").all().map { it.textAs(CardText) }
                },
                page.locator("form.add").count() > 0,
                page.locator("#viewers li").all().associate {
                    it.locator("span").first().innerText().removeSuffix(" (you)") to it.locator("em").textAs<Role>()
                },
                Jsoup.parse(page.locator("#builds").innerHTML()).select("li").map { BuildSeen(BuildStatus.valueOf(it.select(".status").text()), Commit.of(it.select("code").text())) },
            )
        }

        private fun visit(board: BoardId): Page {
            val current = page
            if (current != null && on == board) return current
            val opened = current ?: browser.blank(viewerCookie(id))
            opened.posting { opened.navigate("/teams/acme/b/${board.value}") }
            opened.locator("#viewer-$id").waitFor()
            page = opened
            on = board
            return opened
        }

        private fun card(board: BoardId, text: CardText) =
            visit(board).locator("article:has(> span:text-is(\"${text.value}\"))")
    }
}

@EnabledIfEnvironmentVariable(named = "FANBELT_TEST_BROWSER", matches = ".+")
class BoardPlaywrightTest : BoardContract() {
    override val driver = PlaywrightBoardDriver()

    @AfterEach
    fun close() = driver.close()
}
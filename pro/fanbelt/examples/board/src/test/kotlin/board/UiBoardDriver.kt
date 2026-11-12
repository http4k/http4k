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
import dev.forkhandles.time.DeterministicScheduler
import org.http4k.fanbelt.infrastructure.AppInfra
import org.http4k.fanbelt.infrastructure.Node
import org.http4k.testing.toHttpHandler
import org.http4k.fanbelt.store.DocumentStorage
import org.http4k.fanbelt.store.memory.InMemoryDocumentStorage
import org.http4k.fanbelt.tenancy.TenantId
import org.http4k.fanbelt.testing.TestRuntime
import org.http4k.fanbelt.testing.Verdict
import org.http4k.fanbelt.testing.browser
import org.http4k.fanbelt.testing.provesServerRefuses
import org.http4k.fanbelt.testing.start
import org.http4k.fanbelt.testing.textAs
import org.http4k.fanbelt.testing.verdict
import org.openqa.selenium.By
import org.openqa.selenium.WebElement

class UiBoardDriver(
    private val scheduler: DeterministicScheduler = DeterministicScheduler(start),
    private val storage: DocumentStorage = InMemoryDocumentStorage(),
    private val nodes: List<Node> = listOf(App(TestRuntime(scheduler), { AppInfra(it, storage) })),
) : BoardDriver {
    init {
        nodes.forEach(Node::start)
        Teams(storage).create(TenantId.of("acme"), Team(start))
    }

    private var arrivals = 0

    override fun member(name: String): BoardMember = UiMember(viewerNamed(name), nodes[arrivals++ % nodes.size])

    override fun ciReports(board: BoardId, status: String, commit: String): Verdict {
        val verdict = nodes.first().handler.toHttpHandler().reportsBuild(board, status, commit)
        scheduler.runUntilIdle()
        return verdict
    }

    private inner class UiMember(override val id: ViewerId, private val node: Node) : BoardMember {
        override val name = Viewer(id).name
        private val browser = browser(node, scheduler, viewerCookie(id))
        private var on: BoardId? = null

        override fun opens(board: BoardId) = visit(board)

        override fun watches(board: BoardId) = visit(board)

        override fun adds(board: BoardId, text: String): Verdict {
            visit(board)
            val form = browser.findElements(By.cssSelector("form.add")).firstOrNull()
                ?: return node.provesServerRefuses("/teams/acme/b/${board.value}/cards", viewerCookie(id), """{"text":"$text"}""")
            form.findElement(By.tagName("input")).run {
                clear()
                sendKeys(text)
            }
            form.submit()
            return browser.verdict(By.cssSelector("form.add .error"))
        }

        override fun moves(board: BoardId, text: CardText, direction: Direction) =
            card(board, text).findElement(By.cssSelector("button[aria-label='Move ${direction.name.lowercase()}']")).click()

        override fun deletes(board: BoardId, text: CardText) =
            card(board, text).findElement(By.cssSelector("button[aria-label='Delete']")).click()

        override fun makesEditor(board: BoardId, other: BoardMember) {
            visit(board)
            browser.findElement(By.cssSelector("#viewer-${other.id} button")).click()
        }

        override fun sees(board: BoardId): BoardSeen {
            visit(board)
            return BoardSeen(
                browser.findElements(By.cssSelector("#cards section")).associate { column ->
                    column.findElement(By.tagName("h2")).textAs<Column>() to column.findElements(By.cssSelector("article > span:not(.controls)")).map { it.textAs(CardText) }
                },
                browser.findElements(By.cssSelector("form.add")).isNotEmpty(),
                browser.findElements(By.cssSelector("#viewers li")).associate {
                    it.findElement(By.tagName("span")).text.removeSuffix(" (you)") to it.findElement(By.tagName("em")).textAs<Role>()
                },
                browser.findElements(By.cssSelector("#builds li")).map { BuildSeen(it.findElement(By.className("status")).textAs<BuildStatus>(), it.findElement(By.tagName("code")).textAs(Commit)) },
            )
        }

        private fun visit(board: BoardId) {
            if (on == board) return
            browser.get("http://localhost/teams/acme/b/${board.value}")
            on = board
        }

        private fun card(board: BoardId, text: CardText): WebElement {
            visit(board)
            return browser.findElements(By.tagName("article")).first { it.findElement(By.tagName("span")).text == text.value }
        }
    }
}
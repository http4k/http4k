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
import org.http4k.core.then
import org.http4k.fanbelt.infrastructure.AppInfra
import org.http4k.fanbelt.store.DocumentStorage
import org.http4k.fanbelt.store.memory.InMemoryDocumentStorage
import org.http4k.fanbelt.tenancy.TenantId
import org.http4k.fanbelt.testing.TestRuntime
import org.http4k.fanbelt.testing.Verdict
import org.http4k.fanbelt.testing.provesServerRefuses
import org.http4k.fanbelt.testing.textAs
import org.http4k.fanbelt.testing.toFanBeltBrowser
import org.http4k.fanbelt.testing.verdict
import org.http4k.filter.ClientFilters
import org.http4k.testing.toHttpHandler
import org.openqa.selenium.By
import org.openqa.selenium.WebElement
import java.time.Instant.ofEpochMilli

class UiBoardDriver(storage: DocumentStorage = InMemoryDocumentStorage()) : BoardDriver {
    private val scheduler = DeterministicScheduler(ofEpochMilli(0))
    private val node = App(TestRuntime(scheduler), { AppInfra(it, storage) }).apply { start() }

    init {
        Teams(storage).create(TenantId.of("acme"), Team(ofEpochMilli(0)))
    }

    override fun member(name: String): BoardMember = UiMember(viewerNamed(name))

    override fun ciReports(board: BoardId, status: String, commit: String): Verdict {
        val verdict = node.handler.toHttpHandler().reportsBuild(board, status, commit)
        scheduler.runUntilIdle()
        return verdict
    }

    private inner class UiMember(override val id: ViewerId) : BoardMember {
        override val name = Viewer(id).name
        private val browser = ClientFilters.SetCookie(viewerCookie(id)).then(node.handler.toHttpHandler()).toFanBeltBrowser(scheduler)
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

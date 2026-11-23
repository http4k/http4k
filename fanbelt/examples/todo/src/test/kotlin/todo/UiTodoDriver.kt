package todo

import dev.forkhandles.time.DeterministicScheduler
import org.http4k.fanbelt.infrastructure.AppInfra
import org.http4k.fanbelt.store.DocumentStorage
import org.http4k.fanbelt.store.memory.InMemoryDocumentStorage
import org.http4k.fanbelt.tenancy.TenantId
import org.http4k.fanbelt.testing.TestRuntime
import org.http4k.fanbelt.testing.Verdict
import org.http4k.fanbelt.testing.browser
import org.http4k.fanbelt.testing.start
import org.http4k.fanbelt.testing.verdict
import org.openqa.selenium.By
import org.openqa.selenium.WebElement
import todo.domain.TodoList
import todo.domain.TodoTitle
import todo.store.Lists

class UiTodoDriver(private val storage: DocumentStorage = InMemoryDocumentStorage()) : TodoDriver {
    private val scheduler = DeterministicScheduler(start)
    private val app = App(TestRuntime(scheduler)) { AppInfra(it, storage) }.apply {
        start()
        Lists(storage).create(TenantId.of("acme"), TodoList(start))
    }

    override fun user(name: String): User = UiUser()

    private inner class UiUser : User {
        private val browser = browser(app, scheduler)
        private var visited = false
        private val correcting = mutableMapOf<TodoTitle, String>()

        override fun watches() = visit()

        override fun adds(title: String): Verdict {
            visit()
            browser.findElement(By.cssSelector("input.new")).run {
                clear()
                sendKeys(title)
                submit()
            }
            return browser.verdict(By.cssSelector(".error"))
        }

        override fun toggles(title: TodoTitle) = item(title).findElement(By.cssSelector("input[type=checkbox]")).click()

        override fun renames(title: TodoTitle, to: String): Verdict {
            val item = correcting[title]?.let { browser.findElement(By.id(it)) } ?: item(title)
            item.findElement(By.cssSelector("input.title")).run {
                clear()
                sendKeys(to)
            }
            val outcome = browser.verdict(By.cssSelector(".error"))
            if (outcome is Verdict.Rejected) correcting[title] = item.getAttribute("id").orEmpty() else correcting.remove(title)
            return outcome
        }

        override fun deletes(title: TodoTitle) = item(title).findElement(By.cssSelector("button[aria-label='Delete']")).click()

        override fun sees(): ListSeen {
            visit()
            return ListSeen(
                browser.findElements(By.cssSelector("#todos li")).map { it.title() to it.getAttribute("class").orEmpty().contains("done") },
                browser.findElement(By.cssSelector("#todos .remaining")).text.removeSuffix(" left").toInt(),
            )
        }

        private fun visit() {
            if (visited) return
            browser.get("http://localhost/lists/acme/")
            visited = true
        }

        private fun item(title: TodoTitle): WebElement {
            visit()
            return browser.findElements(By.cssSelector("#todos li")).first { it.title() == title.value }
        }

        private fun WebElement.title() = findElement(By.cssSelector("input.title")).getAttribute("value").orEmpty()
    }
}
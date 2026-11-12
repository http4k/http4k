package sl4k

import dev.forkhandles.time.DeterministicScheduler
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.cookie.Cookie
import org.http4k.core.cookie.cookie
import org.http4k.fanbelt.infrastructure.AppInfra
import org.http4k.testing.toHttpHandler
import org.http4k.fanbelt.store.DocumentStorage
import org.http4k.fanbelt.store.memory.InMemoryDocumentStorage
import org.http4k.fanbelt.tenancy.TenantId
import org.http4k.fanbelt.testing.TestRuntime
import org.http4k.fanbelt.testing.start
import sl4k.domain.ChannelName
import sl4k.domain.MessageText
import sl4k.store.ChatTables
import sl4k.store.Teams

private val acme = TenantId.of("acme")

fun datastarPost(path: String, cookie: Cookie, body: String) =
    Request(POST, "/teams/acme/$path").header("datastar-request", "true").header("content-type", "application/json").cookie(cookie).body(body)

fun <R> DocumentStorage.chat(block: (ChatTables) -> R) = read { block(ChatTables(it.tables(acme))) }

fun DocumentStorage.messageNamed(text: MessageText) = chat { tables -> tables.messages.page().items.first { it.text == text } }

fun DocumentStorage.attachmentNamed(name: String) = chat { tables -> tables.attachments.page().items.firstOrNull { it.name.value == name } }

fun DocumentStorage.bookmarkNamed(title: String) = chat { tables -> tables.bookmarks.page().items.first { it.title.value == title } }

class ChatApp(sites: HttpHandler = fakeSites) {
    val storage = InMemoryDocumentStorage()
    val scheduler = DeterministicScheduler(start)
    val node = App(TestRuntime(scheduler, http = sites)) { AppInfra(it, storage) }
    val http = node.handler.toHttpHandler()

    init {
        Teams(storage).create(acme, TestRuntime(scheduler).clock, ChannelName.of("general"))
    }

    fun post(path: String, cookie: Cookie, body: String) = http(datastarPost(path, cookie, body))

    fun get(path: String, cookie: Cookie) = http(Request(GET, "/teams/acme/$path").cookie(cookie))
}

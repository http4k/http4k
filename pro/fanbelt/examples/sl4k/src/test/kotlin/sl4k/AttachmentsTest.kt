package sl4k

import org.http4k.connect.model.Base64Blob
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.core.cookie.cookie
import org.http4k.fanbelt.testing.TestTenant
import org.http4k.fanbelt.testing.Verdict
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import sl4k.domain.ChannelName
import sl4k.domain.Draft
import sl4k.domain.Edit
import sl4k.domain.Viewer
import sl4k.domain.MessageRef
import sl4k.domain.PAGE
import sl4k.domain.Upload
import sl4k.domain.Window
import sl4k.feature.ChatFeature
import sl4k.store.ChatTables
import sl4k.store.Teams
import sl4k.web.viewerCookie

class AttachmentsTest {
    private val general = ChannelName.of("general")
    private val ann = Viewer(viewerNamed("ann"))
    private val hello = Base64Blob.encode("hello").value

    @Test
    fun `a file that isn't base64, has no contents or has no name is refused`() {
        val tenant = TestTenant()
        val chat = ChatFeature(tenant.clock, tenant.random)
        Teams(tenant.infra.storage).create(tenant.scope.tenant, tenant.clock, general)

        assertEquals(Verdict.Rejected("That file can't be read"), tenant.execute(chat.post, ann, Draft(general, "Hi", listOf(Upload("a.txt", "text/plain", "%%%")))))
        assertEquals(Verdict.Rejected("That file can't be read"), tenant.execute(chat.post, ann, Draft(general, "Hi", listOf(Upload("a.txt", "text/plain", "")))))
        assertEquals(Verdict.Rejected("Give the file a name"), tenant.execute(chat.post, ann, Draft(general, "Hi", listOf(Upload("  ", "text/plain", hello)))))
    }

    @Test
    fun `deleting a message leaves no file behind`() {
        val tenant = TestTenant()
        val chat = ChatFeature(tenant.clock, tenant.random)
        Teams(tenant.infra.storage).create(tenant.scope.tenant, tenant.clock, general)
        tenant.execute(chat.post, ann, Draft(general, "Hi", listOf(Upload("a.txt", "text/plain", hello))))
        val message = tenant.read(chat.history, Window(general, PAGE))!!.messages.single().id

        tenant.execute(chat.delete, ann, MessageRef(general, message))

        val (attachments, contents) = tenant.infra.storage.read { tenants ->
            val tables = ChatTables(tenants.tables(tenant.scope.tenant))
            tables.attachments.count() to tables.attachmentContents.count()
        }
        assertEquals(0 to 0, attachments to contents)
    }

    @Test
    fun `a file can't be fetched through a channel it isn't in`() {
        val app = ChatApp()
        val cookie = viewerCookie(ann.id)
        app.post("channels", cookie, """{"name":"secret","private":true}""")
        app.post("c/secret/messages", cookie, """{"text":"Hi","upload":[{"name":"a.txt","mime":"text/plain","contents":"$hello"}]}""")
        val id = app.storage.chat { it.attachments.page().items.single().id }

        assertEquals(OK, app.get("c/secret/files/$id", cookie).status)
        assertEquals(NOT_FOUND, app.get("c/general/files/$id", cookie).status)
        assertEquals(NOT_FOUND, app.get("c/general/files/$id", viewerCookie(viewerNamed("bob"))).status)
    }

    @Test
    fun `a message deleted while you were changing it says so`() {
        val tenant = TestTenant()
        val chat = ChatFeature(tenant.clock, tenant.random)
        Teams(tenant.infra.storage).create(tenant.scope.tenant, tenant.clock, general)
        tenant.execute(chat.post, ann, Draft(general, "Hi", emptyList()))
        val message = tenant.read(chat.history, Window(general, PAGE))!!.messages.single().id
        tenant.execute(chat.delete, ann, MessageRef(general, message))

        assertEquals(Verdict.Rejected("No such message"), tenant.execute(chat.edit, ann, Edit(general, message, "Better")))
        assertEquals(Verdict.Rejected("No such message"), tenant.execute(chat.delete, ann, MessageRef(general, message)))
    }

    @Test
    fun `a download can't run in our origin, and its name can't break out of the header`() {
        val app = ChatApp()
        val cookie = viewerCookie(ann.id)
        val body = """{"text":"Hi","upload":[{"name":"evil\"; x=\r\n.html","mime":"text/html","contents":"$hello"}]}"""
        app.post("c/general/messages", cookie, body)
        val id = app.storage.chat { it.attachments.page().items.single().id }

        val response = app.get("c/general/files/$id", cookie)

        assertEquals(OK, response.status)
        assertEquals("application/octet-stream", response.header("content-type"))
        assertEquals("""attachment; filename="evil___x___.html"""", response.header("content-disposition"))
        assertEquals("nosniff", response.header("x-content-type-options"))
        assertEquals("sandbox", response.header("content-security-policy"))
    }
}

package features.grid

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import features.App
import features.viewerCookie
import features.viewerNamed
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.body.form
import org.http4k.core.cookie.cookie
import org.http4k.fanbelt.testing.infrastructure.TestRuntime
import org.junit.jupiter.api.Test

class GridExportTest {
    private val http = App(TestRuntime()).apply { start() }.handler.http!!
    private val ann = viewerCookie(viewerNamed("ann"))

    init {
        http(Request(POST, "/spaces").form("space", "acme"))
    }

    private fun set(cell: String, text: String) =
        http(Request(POST, "/spaces/acme/cells/$cell").cookie(ann).header("content-type", "application/json").body("""{"edit":"$text"}"""))

    @Test
    fun `the grid downloads as CSV, one line per cell that has something in it`() {
        set("B2", "1200")
        set("A1", "Rent, monthly")

        val csv = http(Request(GET, "/spaces/acme/grid.csv").cookie(ann))

        assertThat(csv.header("content-type"), equalTo("text/csv"))
        assertThat(csv.bodyString(), equalTo("cell,text\nA1,\"Rent, monthly\"\nB2,1200\n"))
    }
}

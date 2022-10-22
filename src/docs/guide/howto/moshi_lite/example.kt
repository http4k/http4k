package guide.howto.moshi_lite

import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.with
import java.util.UUID

data class MoshiCat(val id: UUID, val name: String)

fun main() {
    val cat = MoshiCat(UUID.randomUUID(), "Tigger")

    // serialize
    val string = MoshiLite.asFormatString(cat)
        .also(::println)

    // deserialize
    MoshiLite.asA<MoshiCat>(string)
        .also(::println)

    // make a lens
    val lens = MoshiLite.autoBody<MoshiCat>().toLens()
    Request(GET, "foo").with(lens of cat)
}

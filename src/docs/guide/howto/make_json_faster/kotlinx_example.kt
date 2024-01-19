package guide.howto.make_json_faster

import kotlinx.serialization.Serializable
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.with
import org.http4k.format.KotlinxSerialization
import java.util.UUID

// You must annotate your class to generate the adapter
@Serializable
data class KotlinXCat(
    val id: String, // UUID not supported
    val name: String
)

// use the builtin http4k module
private val json = KotlinxSerialization

fun main() {
    val cat = KotlinXCat(UUID.randomUUID().toString(), "Kratos")

    // serialize
    val string = json.asFormatString(cat)
        .also(::println)

    // deserialize
    json.asA<KotlinXCat>(string)
        .also(::println)

    // make a lens
    val lens = json.autoBody<KotlinXCat>().toLens()
    Request(Method.GET, "foo").with(lens of cat)
}


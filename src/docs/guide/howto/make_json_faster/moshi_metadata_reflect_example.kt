package guide.howto.make_json_faster

import com.squareup.moshi.Moshi
import dev.zacsweers.moshix.reflect.MetadataKotlinJsonAdapterFactory
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.with
import org.http4k.format.ConfigurableMoshi
import org.http4k.format.EventAdapter
import org.http4k.format.ListAdapter
import org.http4k.format.MapAdapter
import org.http4k.format.ThrowableAdapter
import org.http4k.format.asConfigurable
import org.http4k.format.withStandardMappings
import se.ansman.kotshi.JsonSerializable
import java.util.UUID

private val json = ConfigurableMoshi(
    Moshi.Builder()
        .addLast(EventAdapter)
        .addLast(ThrowableAdapter)
        .addLast(ListAdapter)
        .addLast(MapAdapter)
        .asConfigurable(MetadataKotlinJsonAdapterFactory()) // <-- moshi-metadata-reflect
        .withStandardMappings()
        .done()
)

data class MoshiCat(val id: UUID, val name: String)

fun main() {
    val cat = MoshiCat(UUID.randomUUID(), "Kratos")

    // serialize
    val string = json.asFormatString(cat)
        .also(::println)

    // deserialize
    json.asA<MoshiCat>(string)
        .also(::println)

    // make a lens
    val lens = json.autoBody<MoshiCat>().toLens()
    Request(Method.GET, "foo").with(lens of cat)
}

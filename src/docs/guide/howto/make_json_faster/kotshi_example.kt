package guide.howto.make_json_faster

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
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
import se.ansman.kotshi.KotshiJsonAdapterFactory
import java.util.UUID

// Annotation is required to generate the adapter
@JsonSerializable
data class KotshiCat(val id: UUID, val name: String)

// Build the kotshi adapter
@KotshiJsonAdapterFactory
private object ExampleJsonAdapterFactory : JsonAdapter.Factory by
    KotshiExampleJsonAdapterFactory // this class will be generated during compile

private val json = ConfigurableMoshi(
    Moshi.Builder()
        .add(ExampleJsonAdapterFactory) // inject kotshi here
        .addLast(EventAdapter)
        .addLast(ThrowableAdapter)
        .addLast(ListAdapter)
        .addLast(MapAdapter)
        .asConfigurable()
        .withStandardMappings()
        .done()
)

fun main() {
    val cat = KotshiCat(UUID.randomUUID(), "Kratos")

    // serialize
    val string = json.asFormatString(cat)
        .also(::println)

    // deserialize
    json.asA<KotshiCat>(string)
        .also(::println)

    // make a lens
    val lens = json.autoBody<KotshiCat>().toLens()
    Request(Method.GET, "foo").with(lens of cat)
}

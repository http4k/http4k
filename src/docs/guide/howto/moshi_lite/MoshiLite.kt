package guide.howto.moshi_lite

import com.squareup.moshi.Moshi
import dev.zacsweers.moshix.reflect.MetadataKotlinJsonAdapterFactory
import org.http4k.format.ConfigurableMoshi
import org.http4k.format.EventAdapter
import org.http4k.format.ListAdapter
import org.http4k.format.MapAdapter
import org.http4k.format.ThrowableAdapter
import org.http4k.format.asConfigurable
import org.http4k.format.withStandardMappings

object MoshiLite: ConfigurableMoshi(
    Moshi.Builder()
        .addLast(EventAdapter)
        .addLast(ThrowableAdapter)
        .addLast(ListAdapter)
        .addLast(MapAdapter)
        .asConfigurable(MetadataKotlinJsonAdapterFactory()) // <-- moshi-metadata-reflect
        .withStandardMappings()
        .done()
)

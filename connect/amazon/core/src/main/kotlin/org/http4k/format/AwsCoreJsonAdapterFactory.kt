package org.http4k.format

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import org.http4k.connect.amazon.KotshiJsonErrorJsonAdapter
import org.http4k.connect.amazon.core.model.KotshiCredentialsJsonAdapter
import org.http4k.connect.amazon.core.model.KotshiMessageFieldsDtoJsonAdapter
import org.http4k.connect.amazon.core.model.KotshiTagJsonAdapter
import se.ansman.kotshi.InternalKotshiApi
import se.ansman.kotshi.KotshiJsonAdapterFactory

@OptIn(InternalKotshiApi::class)
class AwsCoreJsonAdapterFactory(
    vararg typesToAdapters: Pair<String, (Moshi) -> JsonAdapter<*>>
) : SimpleMoshiAdapterFactory(
    *(
        typesToAdapters.toList()
            + adapter { KotshiTagJsonAdapter() }
            + adapter { KotshiJsonErrorJsonAdapter() }
            + adapter { moshi -> KotshiMessageFieldsDtoJsonAdapter(moshi) }
            + adapter { moshi -> KotshiCredentialsJsonAdapter(moshi) }
        )
        .toTypedArray()
)

@KotshiJsonAdapterFactory
object CoreAdapterFactory : JsonAdapter.Factory by KotshiCoreAdapterFactory

package org.http4k.connect.plugin

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import dev.forkhandles.result4k.Result
import org.http4k.connect.Http4kConnectApiClient
import org.http4k.connect.Paged
import org.http4k.connect.RemoteFailure
import org.http4k.connect.plugin.bar.BarAction
import org.http4k.connect.plugin.foo.FooAction
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.http4k.format.ConfigurableMoshi
import org.http4k.format.asConfigurable
import se.ansman.kotshi.JsonSerializable
import se.ansman.kotshi.KotshiJsonAdapterFactory

@Http4kConnectApiClient
interface TestClient {
    operator fun <R> invoke(action: FooAction<R>): Result<R, RemoteFailure>
    operator fun <R> invoke(action: BarAction<R>): Result<R, RemoteFailure>

    companion object
}

fun TestClient.Companion.Impl() = object : TestClient {
    override fun <R> invoke(action: FooAction<R>) = action.toResult(Response(OK).body("[]"))
    override fun <R> invoke(action: BarAction<R>) = action.toResult(Response(OK))
}

object TestMoshi : ConfigurableMoshi(
    Moshi.Builder()
        .add(TestJsonFactory)
        .asConfigurable()
        .done()
)

@JsonSerializable
data class TestBean(val value: String)

@KotshiJsonAdapterFactory
object TestJsonFactory : JsonAdapter.Factory by KotshiTestJsonFactory

data class TestPaged(val token: String?) : Paged<Uri, String> {
    override fun token() = token?.let { Uri.of(it) }

    override val items = emptyList<String>()
}

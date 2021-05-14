package org.http4k.serverless

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestStreamHandler
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.format.Jackson.asA
import org.http4k.format.Jackson.asFormatString
import org.http4k.format.Jackson.asInputStream
import java.io.ByteArrayOutputStream
import java.lang.reflect.Proxy

fun assertOutput(
    app: ApiGatewayFnLoader,
    request: Map<String, Any>,
    response: Map<String, Any>
) {
    val env = emptyMap<String, String>()

    assertThat(
        asFormatString(
            asA(
                app(env)(
                    asFormatString(request).byteInputStream(),
                    proxy()
                )
            )
        ), equalTo(asFormatString(response))
    )
}

fun RequestStreamHandler.handleRequest(request: Map<String, Any>, ctx: Context): Map<String, Any> =
    ByteArrayOutputStream().run {
        handleRequest(asInputStream(request), this, ctx)
        asA(toString())
    }

fun proxy(): Context = Proxy.newProxyInstance(
    Context::class.java.classLoader,
    arrayOf(Context::class.java)
) { _, _, _ -> TODO("not implemented") } as Context

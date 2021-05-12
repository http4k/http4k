package org.http4k.serverless

import org.http4k.format.AutoMarshalling
import java.io.InputStream


inline fun <Ctx, reified In, Out : Any> AutoMarshallingFnLoader(
    autoMarshalling: AutoMarshalling,
    crossinline makeHandler: (Map<String, String>) -> FnHandler<In, Ctx, Out>
) = object : FnLoader<Ctx> {
    override fun invoke(env: Map<String, String>): FnHandler<InputStream, Ctx, InputStream> {
        val receiver = makeHandler(env)
        return FnHandler { inputStream, ctx: Ctx ->
            autoMarshalling
                .asFormatString(receiver(autoMarshalling.asA(inputStream), ctx))
                .trimStart('"')
                .trimEnd('"')
                .byteInputStream()
        }
    }
}

package org.http4k.testing

import org.http4k.core.HttpMessage
import java.io.InputStream

/**
 * Determines which parts of the HttpMessage will be compared.
 */
interface ApprovalContent {
    operator fun invoke(input: InputStream): InputStream
    operator fun invoke(input: HttpMessage): InputStream

    companion object {
        fun EntireHttpMessage() = object : ApprovalContent {
            override fun invoke(input: InputStream) = input

            override fun invoke(input: HttpMessage) = input.toString().byteInputStream()
        }

        fun HttpTextBody(formatter: (String) -> String = { it }) = object : ApprovalContent {
            override fun invoke(input: InputStream) = input
            override fun invoke(input: HttpMessage) = formatter(input.bodyString()).byteInputStream()
        }

        fun HttpBinaryBody() = object : ApprovalContent {
            override fun invoke(input: InputStream): InputStream = input
            override fun invoke(input: HttpMessage): InputStream = invoke(input.body.stream)
        }

        fun HttpTextMessage(formatBody: (String) -> String) = object : ApprovalContent {
            override fun invoke(input: InputStream) = input
            override fun invoke(input: HttpMessage) =
                input.body(formatBody(input.bodyString())).toString().byteInputStream()
        }
    }
}

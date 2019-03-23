package org.http4k.testing

import org.http4k.core.HttpMessage
import java.io.InputStream

interface ApprovalContent {
    operator fun invoke(input: InputStream): InputStream
    operator fun invoke(input: HttpMessage): InputStream

    companion object {
        val EntireMessage = object : ApprovalContent {
            override fun invoke(input: InputStream) = input

            override fun invoke(input: HttpMessage) = input.toString().byteInputStream()
        }

        val BodyOnly = object : ApprovalContent {
            override fun invoke(input: InputStream) = input

            override fun invoke(input: HttpMessage) = input.body.stream
        }
    }
}
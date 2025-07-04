package org.http4k.connect.amazon.route53.model

import org.http4k.connect.amazon.core.firstChild
import org.http4k.connect.amazon.core.firstChildText
import org.w3c.dom.Document
import java.util.UUID

data class ErrorResponse(
    val requestId: UUID,
    val error: Error,
) {
    data class Error(
        val type: String,
        val code: String,
        val message: String?
    )

    fun toXml() = buildString {
        append("""<ErrorResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">""")
        append("<Error>")
        append("<Type>${error.type}</Type>")
        append("<Code>${error.code}</Code>")
        if (error.message != null) append("<Message>${error.message}</Message>")
        append("</Error>")
        append("<RequestId>$requestId</RequestId>")
        append("</ErrorResponse>")
    }

    companion object {
        fun parse(document: Document) = document
            .getElementsByTagName("ErrorResponse")
            .item(0)!!.let { response ->
                ErrorResponse(
                    requestId = UUID.fromString(response.firstChildText("RequestId")!!),
                    error = response.firstChild("Error")!!.let {
                        Error(
                            type = it.firstChildText("Type")!!,
                            code = it.firstChildText("Code")!!,
                            message = it.firstChildText("Message"),
                        )
                    }
                )
            }
    }
}

package org.http4k.multipart

import org.http4k.multipart.part.Part

data class MultipartForm(val parts: List<Part>)

fun main(args: Array<String>) {

    val builder = ValidMultipartFormBuilder("bob".toByteArray())
    builder.part("content")
//    builder.attachment()
}
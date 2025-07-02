package org.http4k.connect.amazon.route53.model

fun ChangeInfo.toXml() = buildString {
    append("<ChangeInfo>")
    append("<Id>$id</Id>")
    append("<Status>${status}</Status>")
    append("<SubmittedAt>$submittedAt</SubmittedAt>")
    if (comment != null) append("<Comment>$comment</Comment>")
    append("</ChangeInfo>")
}

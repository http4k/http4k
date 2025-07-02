package org.http4k.connect.amazon.route53.model

import org.http4k.connect.amazon.core.firstChild
import org.w3c.dom.Node
import java.time.Instant

data class ChangeInfo(
    val id: String,
    val status: Status,
    val submittedAt: Instant,
    val comment: String?,
) {
    companion object {
        fun parse(node: Node) = ChangeInfo(
            comment = node.firstChild("Comment")?.textContent!!,
            id = node.firstChild("Id")?.textContent!!,
            status = Status.valueOf(node.firstChild("Status")?.textContent!!),
            submittedAt = Instant.parse(node.firstChild("SubmittedAt")?.textContent!!)
        )
    }

    enum class Status { PENDING, INSYNC }
}

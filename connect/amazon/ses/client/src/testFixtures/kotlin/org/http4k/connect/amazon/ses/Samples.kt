package org.http4k.connect.amazon.ses

import org.http4k.connect.amazon.ses.model.EmailAddress

fun sampleMimeMessage(from: EmailAddress, to: Set<EmailAddress> = emptySet(), cc: Set<EmailAddress> = emptySet()) = (
    """From: Some One <${from.value}>${to.joinToString { "\nTo: ${it.value}" }}${cc.joinToString { "\nCc: ${it.value}" }}
MIME-Version: 1.0
Content-Type: multipart/mixed;
        boundary="XXXXboundary text"

This is a multipart message in MIME format.

--XXXXboundary text
Content-Type: text/plain

this is the body text

--XXXXboundary text
Content-Type: text/plain;
Content-Disposition: attachment;
        filename="test.txt"

this is the attachment text

--XXXXboundary text--"""
    )

package org.http4k.connect.amazon.ses

import org.http4k.connect.amazon.ses.model.EmailAddress

fun sampleMimeMessage(from: EmailAddress) = (
    """From: Some One <${from.value}>
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

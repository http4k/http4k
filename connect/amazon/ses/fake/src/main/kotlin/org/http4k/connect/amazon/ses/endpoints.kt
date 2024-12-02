@file:Suppress("FunctionName")

package org.http4k.connect.amazon.ses

import org.http4k.connect.amazon.ses.model.EmailAddress
import org.http4k.connect.amazon.ses.model.Message
import org.http4k.connect.amazon.ses.model.SESMessageId
import org.http4k.connect.amazon.ses.model.Subject
import org.http4k.connect.storage.Storage
import org.http4k.core.Body
import org.http4k.core.ContentType.Companion.APPLICATION_XML
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.body.form
import org.http4k.core.with
import org.http4k.lens.FormField
import org.http4k.lens.Validator
import org.http4k.lens.WebForm
import org.http4k.lens.value
import org.http4k.lens.webForm
import org.http4k.routing.asPredicate
import org.http4k.routing.bind
import org.http4k.template.PebbleTemplates
import org.http4k.template.viewModel
import org.http4k.connect.amazon.ses.model.Body as SESBody

private val source = FormField.value(EmailAddress).required("Source")
private val sendForm = Body.webForm(Validator.Strict, source).toLens()

fun SendEmail(messagesBySender: Storage<List<EmailMessage>>) = { r: Request -> r.form("Action") == "SendEmail" }
    .asPredicate() bind {

    val webform = sendForm(it)

    val existing = messagesBySender[source(webform).value] ?: emptyList()

    val emailMessage = EmailMessage(
        source(webform),
        webform.valuesFrom("Destination.ToAddresses.member", EmailAddress::of),
        webform.valuesFrom("Destination.CcAddresses.member", EmailAddress::of),
        webform.valuesFrom("Destination.BccAddresses.member", EmailAddress::of),
        Message(
            webform.valuesFrom("Message.Subject.Data", Subject::of).first(),
            webform.valuesFrom("Message.Body.Html.Data", SESBody::of).firstOrNull(),
            webform.valuesFrom("Message.Body.Text.Data", SESBody::of).firstOrNull()
        )
    )

    messagesBySender[source(webform).value] = existing + emailMessage

    Response(OK).with(viewModelLens of SendEmailResponse(SESMessageId.of(emailMessage.toString())))
}

private fun <T> WebForm.valuesFrom(prefix: String, transform: (String) -> T) = fields
    .filterKeys { it.startsWith(prefix) }
    .values
    .flatMap { it.map(transform) }
    .toSet()

val viewModelLens by lazy {
    Body.viewModel(PebbleTemplates().CachingClasspath(), APPLICATION_XML).toLens()
}

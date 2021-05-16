package howto.html_forms

import org.http4k.core.Body
import org.http4k.core.ContentType
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.with
import org.http4k.lens.FormField
import org.http4k.lens.Header
import org.http4k.lens.LensFailure
import org.http4k.lens.Validator
import org.http4k.lens.WebForm
import org.http4k.lens.int
import org.http4k.lens.webForm

data class Name(val value: String)

fun main() {

    // define fields using the standard lens syntax
    val ageField = FormField.int().required("age")
    val nameField = FormField.map(::Name, Name::value).optional("name")

    // add fields to a form definition, along with a validator
    val strictFormBody = Body.webForm(Validator.Strict, nameField, ageField).toLens()
    val feedbackFormBody = Body.webForm(Validator.Feedback, nameField, ageField).toLens()

    val invalidRequest = Request(GET, "/")
        .with(Header.CONTENT_TYPE of ContentType.APPLICATION_FORM_URLENCODED)

    // the "strict" form rejects (throws a LensFailure) because "age" is required
    try {
        strictFormBody(invalidRequest)
    } catch (e: LensFailure) {
        println(e.message)
    }

    // the "feedback" form doesn't throw, but collects errors to be reported later
    val invalidForm = feedbackFormBody(invalidRequest)
    println(invalidForm.errors)

    // creating valid form using "with()" and setting it onto the request
    val webForm = WebForm().with(ageField of 55, nameField of Name("rita"))
    val validRequest = Request(GET, "/").with(strictFormBody of webForm)

    // to extract the contents, we first extract the form and then extract the fields from it using the lenses
    val validForm = strictFormBody(validRequest)
    val age = ageField(validForm)
    println(age)
}

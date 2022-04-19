package org.http4k.format

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Body
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with
import org.http4k.format.Klaxon.auto
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import com.beust.klaxon.Klaxon as KKlaxon

class KlaxonAutoTest : AutoMarshallingJsonContract(Klaxon) {
    override val expectedAutoMarshallingResult = """{"bool":false,"child":{"bool":true,"child":null,"numbers":[1],"string":"world"},"numbers":[],"string":"hello"}"""

    override val expectedAutoMarshallingResultPrimitives = """{"duration":"PT1S", "instant":"1970-01-01T00:00:00Z", "localDate":"2000-01-01", "localDateTime":"2000-01-01T01:01:01", "localTime":"01:01:01", "offsetDateTime":"2000-01-01T01:01:01Z", "offsetTime":"01:01:01Z", "status":200, "uri":"http://uri:8000", "url":"http://url:9000", "uuid":"1a448854-1687-4f90-9562-7d527d64383c", "zonedDateTime":"2000-01-01T01:01:01Z[UTC]"}"""

    override val expectedAutoMarshallingZonesAndLocale = """{"locale":"en-CA","zoneId":"America/Toronto","zoneOffset":"-04:00"}"""

    override fun customMarshaller() = object : ConfigurableKlaxon(KKlaxon().asConfigurable().customise()) {}
    override fun customMarshallerProhibitStrings()= object : ConfigurableKlaxon(KKlaxon().asConfigurable().prohibitStrings().customise()) {}

    @Test
    fun `write interface implementation to body`() {
        assertThat(Response(OK).with(
            Body.auto<Interface>().toLens() of InterfaceImpl()
        ).bodyString(), equalTo("""{"subValue" : "123", "value" : "hello"}"""))
    }

    @Test
    fun `write list of interface implementation to body`() {
        assertThat(Response(OK).with(
            Body.auto<List<Interface>>().toLens() of listOf(InterfaceImpl())
        ).bodyString(), equalTo("""[{"subValue" : "123", "value" : "hello"}]"""))
    }

    @Test
    fun `writes using non-sealed parent type`() {
        val nonSealedChild = NonSealedChild("hello")
        assertThat(Response(OK).with(Body.auto<NotSealedParent>().toLens() of nonSealedChild).bodyString(), equalTo("""{"something" : "hello"}"""))
    }

    @Disabled("not supported by Klaxon")
    override fun `roundtrip custom value`() {
    }

    @Disabled("not supported by Klaxon")
    override fun `roundtrip custom number`() {
    }

    @Disabled("not supported by Klaxon")
    override fun `roundtrip custom decimal`() {
    }

    @Disabled("not supported by Klaxon")
    override fun `roundtrip custom boolean`() {
    }
}

class KlaxonAutoEventsTest : AutoMarshallingEventsContract(Klaxon)

package org.http4k.format

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.ubertob.kondor.json.*
import com.ubertob.kondor.json.jsonnode.JsonNodeObject
import org.http4k.core.ContentType
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.with
import org.http4k.hamkrest.hasContentType
import org.http4k.lens.BiDiMapping
import org.http4k.lens.StringBiDiMappings
import org.http4k.lens.StringBiDiMappings.bigDecimal
import org.http4k.lens.StringBiDiMappings.duration
import org.http4k.lens.StringBiDiMappings.instant
import org.http4k.lens.StringBiDiMappings.localDate
import org.http4k.lens.StringBiDiMappings.localDateTime
import org.http4k.lens.StringBiDiMappings.localTime
import org.http4k.lens.StringBiDiMappings.locale
import org.http4k.lens.StringBiDiMappings.offsetDateTime
import org.http4k.lens.StringBiDiMappings.offsetTime
import org.http4k.lens.StringBiDiMappings.throwable
import org.http4k.lens.StringBiDiMappings.uri
import org.http4k.lens.StringBiDiMappings.url
import org.http4k.lens.StringBiDiMappings.uuid
import org.http4k.lens.StringBiDiMappings.zoneId
import org.http4k.lens.StringBiDiMappings.zoneOffset
import org.http4k.lens.StringBiDiMappings.zonedDateTime
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.util.*

class KondorJsonAutoMarshallingJsonTest : AutoMarshallingJsonContract(
    KondorJson {
        register(JArbObject)
        register(JStringHolder)
        register(JMapHolder)
        register(JCommonJdkPrimitives)
        register(JRegexHolder)
        register(JZonesAndLocale)
        register(JExceptionHolder)
        register(throwable().asJConverter())
        register(JMap(JString))
    }
) {
    override fun customMarshaller() = KondorJson {
        register(JArbObject)
        register(JInOnlyHolder)
        register(JOutOnlyHolder)
        register(JHolderHolder)
        register(JMyValueHolder)
        register(BiDiMapping(::BooleanHolder, BooleanHolder::value).asJConverter())
        register(BiDiMapping(::BigDecimalHolder, BigDecimalHolder::value).asJConverter(JBigDecimal))
        register(bigDecimal().map(::MappedBigDecimalHolder, MappedBigDecimalHolder::value).asJConverter())
        register(BiDiMapping(::BigIntegerHolder, BigIntegerHolder::value).asJConverter(JBigInteger))
    }

    override fun strictMarshaller() = throw UnsupportedOperationException()
    override fun customMarshallerProhibitStrings() = throw UnsupportedOperationException()

    @Test
    @Disabled("not supported")
    override fun `roundtrip arbitrary array`() {
        super.`roundtrip arbitrary array`()
    }

    @Test
    @Disabled("not supported")
    override fun `roundtrip arbitrary map`() {
        super.`roundtrip arbitrary map`()
    }

    @Test
    @Disabled("not supported")
    override fun `roundtrip arbitrary set`() {
        super.`roundtrip arbitrary set`()
    }

    @Test
    @Disabled("not supported")
    override fun `prohibit strings`() {
        super.`prohibit strings`()
    }

    @Test
    @Disabled("not supported")
    override fun `fails decoding when a extra key found`() {
        super.`fails decoding when a extra key found`()
    }

    @Test
    override fun `roundtrip custom value`() {
        val marshaller = customMarshaller()

        val wrapper = MyValueHolder(MyValue("foobar"))
        assertThat(marshaller.asFormatString(wrapper), equalTo("{\"value\":\"foobar\"}"))
        assertThat(marshaller.asA("{\"value\":\"foobar\"}", MyValueHolder::class), equalTo(wrapper))
        assertThat(marshaller.asA("{\"value\":null}", MyValueHolder::class), equalTo(MyValueHolder(null)))
    }

    @Test
    fun `roundtrip arbitrary object to and from body`() {
        val body = customMarshaller().autoBody<ArbObject>().toLens()

        val obj = ArbObject("hello", ArbObject("world", null, listOf(1), true), emptyList(), false)

        assertThat(body(Response(Status.OK).with(body of obj)), equalTo(obj))
    }

    @Test
    fun `default content type`() {
        val body = customMarshaller().autoBody<ArbObject>().toLens()

        val obj = ArbObject("hello", ArbObject("world", null, listOf(1), true), emptyList(), false)

        assertThat(Request(Method.POST, "/").with(body of obj), hasContentType(ContentType.APPLICATION_JSON))
    }

    @Test
    fun `custom content type`() {
        val marshaller = KondorJson(ContentType.Text("application/some-custom+json")) {
            register(JArbObject)
        }
        val body = marshaller.autoBody<ArbObject>().toLens()

        val obj = ArbObject("hello", ArbObject("world", null, listOf(1), true), emptyList(), false)

        assertThat(Request(Method.POST, "/").with(body of obj), hasContentType(ContentType.Text("application/some-custom+json")))
    }
}

private object JInOnly : JStringRepresentable<InOnly>() {
    override val cons: (String) -> InOnly = ::InOnly
    override val render: (InOnly) -> String
        get() = throw IllegalArgumentException()
}

private object JInOnlyHolder : JAny<InOnlyHolder>() {
    val value by str(JInOnly, InOnlyHolder::value)

    override fun JsonNodeObject.deserializeOrThrow() = InOnlyHolder(value = +value)
}

private object JOutOnly : JStringRepresentable<OutOnly>() {
    override val cons: (String) -> OutOnly
        get() = throw IllegalArgumentException()
    override val render: (OutOnly) -> String = OutOnly::value
}

private object JOutOnlyHolder : JAny<OutOnlyHolder>() {
    val value by str(JOutOnly, OutOnlyHolder::value)

    override fun JsonNodeObject.deserializeOrThrow() = OutOnlyHolder(value = +value)
}

private object JArbObject : JAny<ArbObject>() {
    val string by str(ArbObject::string)
    val child by obj(JArbObject, ArbObject::child)
    val numbers by array(JInt, ArbObject::numbers)
    val bool by bool(ArbObject::bool)

    override fun JsonNodeObject.deserializeOrThrow() = ArbObject(
        string = +string,
        child = +child,
        numbers = +numbers,
        bool = +bool
    )
}

private object JStringHolder : JAny<StringHolder>() {
    val value by str(StringHolder::value)

    override fun JsonNodeObject.deserializeOrThrow() = StringHolder(value = +value)
}

private object JMapHolder : JAny<MapHolder>() {
    val value by obj(JMap(JString), MapHolder::value)

    override fun JsonNodeObject.deserializeOrThrow() = MapHolder(value = +value)
}

private object JHolderHolder : JAny<HolderHolder>() {
    val value by JField(HolderHolder::value, bigDecimal().map(::MappedBigDecimalHolder, MappedBigDecimalHolder::value).asJConverter())

    override fun JsonNodeObject.deserializeOrThrow() = HolderHolder(value = +value)
}

private object JMyValue : JStringRepresentable<MyValue>() {
    override val cons: (String) -> MyValue = { MyValue.of(it) }
    override val render: (MyValue) -> String = { MyValue.show(it) }
}

private object JMyValueHolder : JAny<MyValueHolder>() {
    val value by str(JMyValue, MyValueHolder::value)

    override fun JsonNodeObject.deserializeOrThrow() = MyValueHolder(value = +value)
}

private object JCommonJdkPrimitives : JAny<CommonJdkPrimitives>() {
    val duration by str(duration().asJConverter(), CommonJdkPrimitives::duration)
    val localDate by str(localDate().asJConverter(), CommonJdkPrimitives::localDate)
    val localTime by str(localTime().asJConverter(), CommonJdkPrimitives::localTime)
    val localDateTime by str(localDateTime().asJConverter(), CommonJdkPrimitives::localDateTime)
    val zonedDateTime by str(zonedDateTime().asJConverter(), CommonJdkPrimitives::zonedDateTime)
    val offsetTime by str(offsetTime().asJConverter(), CommonJdkPrimitives::offsetTime)
    val offsetDateTime by str(offsetDateTime().asJConverter(), CommonJdkPrimitives::offsetDateTime)
    val instant by str(instant().asJConverter(), CommonJdkPrimitives::instant)
    val uuid by str(uuid().asJConverter(), CommonJdkPrimitives::uuid)
    val uri by str(uri().asJConverter(), CommonJdkPrimitives::uri)
    val url by str(url().asJConverter(), CommonJdkPrimitives::url)
    val status by JField(CommonJdkPrimitives::status, BiDiMapping({ Status(it, "") }, Status::code).asJConverter(JInt))

    override fun JsonNodeObject.deserializeOrThrow() =
        CommonJdkPrimitives(
            duration = +duration,
            localDate = +localDate,
            localTime = +localTime,
            localDateTime = +localDateTime,
            zonedDateTime = +zonedDateTime,
            offsetTime = +offsetTime,
            offsetDateTime = +offsetDateTime,
            instant = +instant,
            uuid = +uuid,
            uri = +uri,
            url = +url,
            status = +status
        )
}

private object JRegexHolder : JAny<RegexHolder>() {
    val regex by str(StringBiDiMappings.regexObject().asJConverter(), RegexHolder::regex)

    override fun JsonNodeObject.deserializeOrThrow() = RegexHolder(regex = +regex)
}

private object JZonesAndLocale : JAny<ZonesAndLocale>() {
    val zoneId by str(zoneId().asJConverter(), ZonesAndLocale::zoneId)
    val zoneOffset by str(zoneOffset().asJConverter(), ZonesAndLocale::zoneOffset)
    val locale by str(locale().asJConverter(), ZonesAndLocale::locale)

    override fun JsonNodeObject.deserializeOrThrow() =
        ZonesAndLocale(
            zoneId = +zoneId,
            zoneOffset = +zoneOffset,
            locale = +locale
        )
}

private object JExceptionHolder : JAny<ExceptionHolder>() {
    val value by str(throwable().asJConverter(), ExceptionHolder::value)

    override fun JsonNodeObject.deserializeOrThrow() = null
}

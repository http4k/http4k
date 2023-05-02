package org.http4k.format

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.ubertob.kondor.json.*
import com.ubertob.kondor.json.jsonnode.JsonNodeObject
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.with
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.util.*

class KondorJsonAutoMarshallingJsonTest : AutoMarshallingJsonContract(ConfigurableKondorJson({
    asConfigurable()
        .withStandardMappings()
        .register(JArbObject)
        .register(JStringHolder)
        .register(JMapHolder)
        .register(JCommonJdkPrimitives(this))
        .register(JRegexHolder(this))
        .register(JZonesAndLocale(this))
        .register(JExceptionHolder(this))
        .register(JMap(JString))
        .done()
})) {
    override fun customMarshaller() = ConfigurableKondorJson({
        asConfigurable()
            .customise()
            .register(JArbObject)
            .register(JInOnlyHolder(this))
            .register(JOutOnlyHolder(this))
            .register(JHolderHolder(this))
            .register(JMyValueHolder(this))
            .done()
    })

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
}

private class JInOnlyHolder(resolve: JConverterResolver) : JAny<InOnlyHolder>() {
    val value by JField(InOnlyHolder::value, resolve())

    override fun JsonNodeObject.deserializeOrThrow() = InOnlyHolder(value = +value)
}

private class JOutOnlyHolder(resolve: JConverterResolver) : JAny<OutOnlyHolder>() {
    val value by JField(OutOnlyHolder::value, resolve())

    override fun JsonNodeObject.deserializeOrThrow() = OutOnlyHolder(value = +value)
}

private object JArbObject : JAny<ArbObject>() {
    val string by str(ArbObject::string)
    val child by JFieldMaybe(ArbObject::child, JArbObject)
    val numbers by JField(ArbObject::numbers, JList(JInt))
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
    val value by JField(MapHolder::value, JMap(JString))

    override fun JsonNodeObject.deserializeOrThrow() = MapHolder(value = +value)
}

private class JHolderHolder(resolve: JConverterResolver) : JAny<HolderHolder>() {
    val value by JField(HolderHolder::value, resolve())

    override fun JsonNodeObject.deserializeOrThrow() = HolderHolder(value = +value)
}

private class JMyValueHolder(resolve: JConverterResolver) : JAny<MyValueHolder>() {
    val value by JFieldMaybe(MyValueHolder::value, resolve())

    override fun JsonNodeObject.deserializeOrThrow() = MyValueHolder(value = +value)
}

private class JCommonJdkPrimitives(resolve: JConverterResolver) : JAny<CommonJdkPrimitives>() {
    val duration by JField(CommonJdkPrimitives::duration, resolve())
    val localDate by JField(CommonJdkPrimitives::localDate, resolve())
    val localTime by JField(CommonJdkPrimitives::localTime, resolve())
    val localDateTime by JField(CommonJdkPrimitives::localDateTime, resolve())
    val zonedDateTime by JField(CommonJdkPrimitives::zonedDateTime, resolve())
    val offsetTime by JField(CommonJdkPrimitives::offsetTime, resolve())
    val offsetDateTime by JField(CommonJdkPrimitives::offsetDateTime, resolve())
    val instant by JField(CommonJdkPrimitives::instant, resolve())
    val uuid by JField(CommonJdkPrimitives::uuid, resolve())
    val uri by JField(CommonJdkPrimitives::uri, resolve())
    val url by JField(CommonJdkPrimitives::url, resolve())
    val status by JField(CommonJdkPrimitives::status, resolve())

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

private class JRegexHolder(resolve: JConverterResolver) : JAny<RegexHolder>() {
    val regex by JField(RegexHolder::regex, resolve())

    override fun JsonNodeObject.deserializeOrThrow() = RegexHolder(regex = +regex)
}

private class JZonesAndLocale(resolve: JConverterResolver) : JAny<ZonesAndLocale>() {
    val zoneId by JField(ZonesAndLocale::zoneId, resolve())
    val zoneOffset by JField(ZonesAndLocale::zoneOffset, resolve())
    val locale by JField(ZonesAndLocale::locale, resolve())

    override fun JsonNodeObject.deserializeOrThrow() =
        ZonesAndLocale(
            zoneId = +zoneId,
            zoneOffset = +zoneOffset,
            locale = +locale
        )
}

private class JExceptionHolder(lookup: JConverterResolver) : JAny<ExceptionHolder>() {
    val value by JField(ExceptionHolder::value, lookup[Throwable::class])

    override fun JsonNodeObject.deserializeOrThrow() = null
}

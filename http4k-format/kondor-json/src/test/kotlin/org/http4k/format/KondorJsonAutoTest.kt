package org.http4k.format

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.ubertob.kondor.json.JAny
import com.ubertob.kondor.json.JField
import com.ubertob.kondor.json.JFieldMaybe
import com.ubertob.kondor.json.JInt
import com.ubertob.kondor.json.JList
import com.ubertob.kondor.json.JMap
import com.ubertob.kondor.json.JNumRepresentable
import com.ubertob.kondor.json.JString
import com.ubertob.kondor.json.JStringRepresentable
import com.ubertob.kondor.json.bool
import com.ubertob.kondor.json.datetime.JDuration
import com.ubertob.kondor.json.jsonnode.JsonNodeObject
import com.ubertob.kondor.json.*
import com.ubertob.kondor.json.datetime.*
import org.http4k.core.Status
import org.http4k.core.Uri
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.net.URL
import java.time.OffsetDateTime
import java.time.OffsetTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.Locale
import java.util.UUID

class KondorJsonAutoTest : AutoMarshallingJsonContract(
    object : ConfigurableKondorJson({
        asConfigurable()
            .register(JArbObject)
            .register(JStringHolder)
            .register(JMapHolder)
            .register(JCommonJdkPrimitives)
            .register(JRegexHolder)
            .register(JZonesAndLocale)
            .register(JExceptionHolder)
            .register(JThrowable)
            .register(JMap(JString))
            .done()
    }) {}
) {
    override fun customMarshaller(): AutoMarshalling = object : ConfigurableKondorJson({
        asConfigurable()
            .register(JInOnlyHolder)
            .register(JOutOnlyHolder)
            .register(JHolderHolder)
            .register(JMyValueHolder)
            .customise()
    }) {}

    override fun strictMarshaller(): AutoMarshalling = throw UnsupportedOperationException()
    override fun customMarshallerProhibitStrings(): AutoMarshalling = throw UnsupportedOperationException()

    override val expectedCustomWrappedNumber = """{"value":1.01}"""

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
}

private object JInOnly : JStringRepresentable<InOnly>() {
    override val cons: (String) -> InOnly = ::InOnly
    override val render: (InOnly) -> String = { throw UnsupportedOperationException() }
}

private object JInOnlyHolder : JAny<InOnlyHolder>() {
    val value by str(JInOnly, InOnlyHolder::value)

    override fun JsonNodeObject.deserializeOrThrow() = InOnlyHolder(value = +value)
}

private object JOutOnly : JStringRepresentable<OutOnly>() {
    override val cons: (String) -> OutOnly = { throw UnsupportedOperationException() }
    override val render: (OutOnly) -> String = OutOnly::value
}

private object JOutOnlyHolder : JAny<OutOnlyHolder>() {
    val value by str(JOutOnly, OutOnlyHolder::value)

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

private object JMappedBigDecimalHolder : JNumRepresentable<MappedBigDecimalHolder>() {
    override val cons: (BigDecimal) -> MappedBigDecimalHolder = ::MappedBigDecimalHolder
    override val render: (MappedBigDecimalHolder) -> BigDecimal = MappedBigDecimalHolder::value
}

private object JHolderHolder : JAny<HolderHolder>() {
    val value by JField(HolderHolder::value, JMappedBigDecimalHolder)

    override fun JsonNodeObject.deserializeOrThrow() = HolderHolder(value = +value)
}

private object JMyValue : JStringRepresentable<MyValue>() {
    override val cons: (String) -> MyValue = ::MyValue
    override val render: (MyValue) -> String = MyValue::value
}

private object JMyValueHolder : JAny<MyValueHolder>() {
    val value by str(JMyValue, MyValueHolder::value)

    override fun JsonNodeObject.deserializeOrThrow() = MyValueHolder(value = +value)
}

private object JZonedDateTime : JStringRepresentable<ZonedDateTime>() {
    override val cons: (String) -> ZonedDateTime = ZonedDateTime::parse
    override val render: (ZonedDateTime) -> String = ZonedDateTime::toString
}

private object JOffsetTime : JStringRepresentable<OffsetTime>() {
    override val cons: (String) -> OffsetTime = OffsetTime::parse
    override val render: (OffsetTime) -> String = OffsetTime::toString
}

private object JOffsetDateTime : JStringRepresentable<OffsetDateTime>() {
    override val cons: (String) -> OffsetDateTime = OffsetDateTime::parse
    override val render: (OffsetDateTime) -> String = OffsetDateTime::toString
}

private object JUuid : JStringRepresentable<UUID>() {
    override val cons: (String) -> UUID = UUID::fromString
    override val render: (UUID) -> String = UUID::toString
}

private object JUri : JStringRepresentable<Uri>() {
    override val cons: (String) -> Uri = Uri.Companion::of
    override val render: (Uri) -> String = Uri::toString
}

private object JUrl : JStringRepresentable<URL>() {
    override val cons: (String) -> URL = ::URL
    override val render: (URL) -> String = URL::toString
}

private object JStatus : JNumRepresentable<Status>() {
    override val cons: (BigDecimal) -> Status = { Status(it.intValueExact(), null) }
    override val render: (Status) -> BigDecimal = { it.code.toBigDecimal() }
}

private object JCommonJdkPrimitives : JAny<CommonJdkPrimitives>() {
    val duration by str(JDuration, CommonJdkPrimitives::duration)
    val localDate by str(CommonJdkPrimitives::localDate)
    val localTime by str(CommonJdkPrimitives::localTime)
    val localDateTime by str(CommonJdkPrimitives::localDateTime)
    val zonedDateTime by str(JZonedDateTime, CommonJdkPrimitives::zonedDateTime)
    val offsetTime by str(JOffsetTime, CommonJdkPrimitives::offsetTime)
    val offsetDateTime by str(JOffsetDateTime, CommonJdkPrimitives::offsetDateTime)
    val instant by str(JInstant, CommonJdkPrimitives::instant)
    val uuid by str(JUuid, CommonJdkPrimitives::uuid)
    val uri by str(JUri, CommonJdkPrimitives::uri)
    val url by str(JUrl, CommonJdkPrimitives::url)
    val status by JField(CommonJdkPrimitives::status, JStatus)

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

private object JRegex : JStringRepresentable<Regex>() {
    override val cons: (String) -> Regex = ::Regex
    override val render: (Regex) -> String = Regex::pattern
}

private object JRegexHolder : JAny<RegexHolder>() {
    val regex by str(JRegex, RegexHolder::regex)

    override fun JsonNodeObject.deserializeOrThrow() = RegexHolder(regex = +regex)
}

private object JZoneOffset : JStringRepresentable<ZoneOffset>() {
    override val cons: (String) -> ZoneOffset = ZoneOffset::of
    override val render: (ZoneOffset) -> String = ZoneOffset::toString
}

private object JLocale : JStringRepresentable<Locale>() {
    override val cons: (String) -> Locale = Locale::forLanguageTag
    override val render: (Locale) -> String = Locale::toLanguageTag
}

private object JZonesAndLocale : JAny<ZonesAndLocale>() {
    val zoneId by str(JZoneId, ZonesAndLocale::zoneId)
    val zoneOffset by str(JZoneOffset, ZonesAndLocale::zoneOffset)
    val locale by str(JLocale, ZonesAndLocale::locale)

    override fun JsonNodeObject.deserializeOrThrow() =
        ZonesAndLocale(
            zoneId = +zoneId,
            zoneOffset = +zoneOffset,
            locale = +locale
        )
}

private object JThrowable : JStringRepresentable<Throwable>() {
    override val cons: (String) -> Throwable = ::Throwable
    override val render: (Throwable) -> String = Throwable::toString
}

private object JExceptionHolder : JAny<ExceptionHolder>() {
    val value by str(JThrowable, ExceptionHolder::value)

    override fun JsonNodeObject.deserializeOrThrow() = null
}

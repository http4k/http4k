package demo

import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

fun TestClock(year: Int, month: Int, day: Int): Clock =
    Clock.fixed(LocalDate.of(year, month, day).atStartOfDay().toInstant(ZoneOffset.UTC), ZoneId.of("UTC"))

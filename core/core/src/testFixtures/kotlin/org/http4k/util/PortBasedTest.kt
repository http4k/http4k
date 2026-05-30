package org.http4k.util

import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Timeout
import java.util.concurrent.TimeUnit.MINUTES

@Timeout(1, unit = MINUTES)
@Tag("slow")
interface PortBasedTest

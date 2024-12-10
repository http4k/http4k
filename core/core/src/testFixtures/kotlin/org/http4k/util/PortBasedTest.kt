package org.http4k.util

import org.junit.jupiter.api.Timeout
import java.util.concurrent.TimeUnit.MINUTES

@Timeout(4, unit = MINUTES)
interface PortBasedTest

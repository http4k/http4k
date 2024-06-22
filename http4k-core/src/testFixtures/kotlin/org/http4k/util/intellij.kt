package org.http4k.util

import java.lang.management.ManagementFactory.getRuntimeMXBean

fun inIntelliJOnly(action: () -> Unit) =
    if (getRuntimeMXBean().inputArguments.find { it.contains("idea", true) } != null)
        action()
    else Unit

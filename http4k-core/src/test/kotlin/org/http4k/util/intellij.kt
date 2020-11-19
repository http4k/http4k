package org.http4k.util

import java.lang.management.ManagementFactory

fun inIntelliJOnly(action: () -> Unit) =
    if (ManagementFactory.getRuntimeMXBean().inputArguments.find { it.contains("idea", true) } != null)
        action()
    else Unit

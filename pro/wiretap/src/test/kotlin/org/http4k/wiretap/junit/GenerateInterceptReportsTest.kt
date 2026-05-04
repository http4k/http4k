/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.wiretap.junit

import dev.forkhandles.mock4k.mock
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.testing.ApprovalTest
import org.http4k.testing.Approver
import org.http4k.testing.assertApproved
import org.http4k.util.FixedClock
import org.http4k.wiretap.junit.RenderMode.Always
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.RegisterExtension
import java.io.File
import java.util.Optional
import kotlin.io.path.createTempDirectory

@ExtendWith(ApprovalTest::class)
class GenerateInterceptReportsTest {

    private val reportDir: File = createTempDirectory().toFile()

    @RegisterExtension
    @JvmField
    val intercept = Intercept.http(Always, clock = FixedClock, reportDir = reportDir) { { Response(OK).body("hello") } }

    @Test
    fun `generates html`(approver: Approver, http: HttpHandler) {
        http(Request(GET, "/"))

        intercept.afterTestExecution(FakeEC)

        approver.assertApproved(File(reportDir, "java/lang/String.toString.html").readText())
    }

    @Test
    fun `generates markdown`(approver: Approver, http: HttpHandler) {
        http(Request(GET, "/"))

        intercept.afterTestExecution(FakeEC)

        approver.assertApproved(File(reportDir, "java/lang/String.toString.md").readText())
    }
}

private object FakeEC : ExtensionContext by mock() {

    override fun getRequiredTestClass() = String::class.java

    override fun publishReportEntry(map: Map<String, String>) {}

    override fun getExecutionException() = Optional.empty<Throwable>()

    override fun getTestMethod() = Optional.of(String::class.java.getMethod("toString"))
}


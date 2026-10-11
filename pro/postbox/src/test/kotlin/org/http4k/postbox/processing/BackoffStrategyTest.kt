/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.postbox.processing

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test
import java.time.Duration.ofSeconds

class BackoffStrategyTest {

    @Test
    fun `default backoff strategy`() {
        val randomSource: RandomSource = { 7 }
        assertThat(defaultBackoffStrategy(0, randomSource), equalTo(ofSeconds(12)))
        assertThat(defaultBackoffStrategy(1, randomSource), equalTo(ofSeconds(17)))
        assertThat(defaultBackoffStrategy(2, randomSource), equalTo(ofSeconds(27)))
        assertThat(defaultBackoffStrategy(3, randomSource), equalTo(ofSeconds(47)))
        assertThat(defaultBackoffStrategy(4, randomSource), equalTo(ofSeconds(87)))
        assertThat(defaultBackoffStrategy(5, randomSource), equalTo(ofSeconds(167)))
    }
}

package org.reekwest.http.core

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.Test

class FilterTest {

    private val repeat = Filter.mk { str: String, svc: (String) -> Int -> svc(str + str) / 2 }

    @Test
    fun `can manipulate value on way in and out of service`() {
        val svc = repeat.andThen({ it.toInt() })
        assertThat(svc("4"), equalTo(22))
    }

    @Test
    fun `can stack filters value on way in and out of service`() {
        val svc = repeat.andThen(repeat).andThen({ it.toInt() })
        assertThat(svc("4"), equalTo(1111))
    }

    @Test
    fun `noop`() {
        val svc = Filter.noOp<String, Int>().andThen({ it.toInt() })
        assertThat(svc("4"), equalTo(4))
    }

}
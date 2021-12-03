package org.http4k.security

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Uri
import org.junit.jupiter.api.Test

class FragmentParametersTest {

    @Test
    fun `can correctly add fragment parameter to uri`() {
        assertThat(Uri.of("http://localhost").fragmentParameter("foo", "bar").toString(), equalTo("http://localhost#foo=bar"))
    }

    @Test
    fun `can correctly add fragment parameter to uri, when one exists`() {
        assertThat(Uri.of("http://localhost#code=12345").fragmentParameter("foo", "bar").toString(), equalTo("http://localhost#code=12345&foo=bar"))
        assertThat(Uri.of("http://localhost#code=12345").fragmentParameter("code", "some different code").toString(), equalTo("http://localhost#code=12345&code=some+different+code"))
    }

    @Test
    fun `can remove parameter from uri`() {
        assertThat(Uri.of("http://localhost#code=12345").removeFragmentParameter("code").toString(), equalTo("http://localhost"))
        assertThat(Uri.of("http://localhost#code=12345").removeFragmentParameter("foo").toString(), equalTo("http://localhost#code=12345"))
        assertThat(Uri.of("http://localhost").removeFragmentParameter("foo").toString(), equalTo("http://localhost"))
    }

    @Test
    fun `can retrieve form parameter from request`() {
        assertThat(Request(GET, "http://localhost#code=12345").fragmentParameter("code"), equalTo("12345"))
        assertThat(Request(GET, "http://localhost#code=12345&code=some+different+code").fragmentParameter("code"), equalTo("12345"))
        assertThat(Request(GET, "http://localhost#code=12345").fragmentParameter("foo"), absent())
        assertThat(Request(GET, "http://localhost#onlyAKey").fragmentParameter("foo"), absent())
        assertThat(Request(GET, "http://localhost#onlyAKey=").fragmentParameter("foo"), absent())
        assertThat(Request(GET, "http://localhost#onlyAKey").fragmentParameter("onlyAKey"), absent())
        assertThat(Request(GET, "http://localhost#onlyAKey=").fragmentParameter("onlyAKey"), equalTo(""))
    }

    @Test
    fun `can retrieve multiple form parameters from request`() {
        assertThat(Request(GET, "http://localhost#code=12345").fragmentParameters("code"), equalTo(listOf<String?>("12345")))
        assertThat(Request(GET, "http://localhost#code=12345&code=some+different+code").fragmentParameters("code"), equalTo(listOf<String?>("12345", "some different code")))
        assertThat(Request(GET, "http://localhost#code=12345").fragmentParameters("foo"), equalTo(emptyList()))
    }

    @Test
    fun `can add form parameter to request`() {
        assertThat(Request(GET, "http://localhost").fragmentParameter("code", "12345"), equalTo(Request(GET, "http://localhost#code=12345")))
        assertThat(Request(GET, "http://localhost#code=12345").fragmentParameter("code", "some different code"), equalTo(Request(GET, "http://localhost#code=12345&code=some+different+code")))
        assertThat(Request(GET, "http://localhost#code=12345").fragmentParameter("foo", null), equalTo(Request(GET, "http://localhost#code=12345&foo")))
    }
}

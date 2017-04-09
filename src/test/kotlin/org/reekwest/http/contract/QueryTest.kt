package org.reekwest.http.contract

class QueryTest {
//    private val request = Request(GET, uri("/?hello=world&hello=world2"))
//
//    @Test
//    fun `value present`() {
//        assertThat(request[Query.optional("hello")], equalTo("world"))
//        assertThat(request[Query.required("hello")], equalTo("world"))
//        assertThat(request[Query.required("hello").map { it.length }], equalTo(5))
//        assertThat(request[Query.optional("hello").map { it.length }], equalTo(5))
//
//        val expected: List<String?> = listOf("world", "world2")
//        assertThat(request[Query.multi.required("hello")], equalTo(expected))
//        assertThat(request[Query.multi.optional("hello")], equalTo(expected))
//    }
//
//    @Test
//    fun `value missing`() {
//        assertThat(request[Query.optional("world")], absent())
//        assertThat({ request[Query.required("world")] }, throws<Missing>())
//
//        assertThat(request[Query.multi.optional("world")], equalTo(emptyList()))
//        assertThat({ request[Query.multi.required("world")] }, throws<Missing>())
//    }
//
//    @Test
//    fun `invalid value`() {
//        assertThat({ request[Query.required("hello").map { it.toInt() }] }, throws<Invalid>())
//        assertThat({ request[Query.optional("hello").map { it.toInt() }] }, throws<Invalid>())
//
//        assertThat({ request[Query.multi.required("hello").map { it.map { it?.toInt() } }] }, throws<Invalid>())
//        assertThat({ request[Query.multi.optional("hello").map { it.map { it?.toInt() } }] }, throws<Invalid>())
//    }
//
//    @Test
//    fun `toString is ok`() {
//        assertThat(Query.required("hello").toString(), equalTo("Required query 'hello'"))
//        assertThat(Query.optional("hello").toString(), equalTo("Optional query 'hello'"))
//        assertThat(Query.multi.required("hello").toString(), equalTo("Required query 'hello'"))
//        assertThat(Query.multi.optional("hello").toString(), equalTo("Optional query 'hello'"))
//    }
}
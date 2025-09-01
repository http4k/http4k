package org.http4k.core

import org.http4k.core.AcceptableParsingTest.ExampleValue.a
import org.http4k.core.AcceptableParsingTest.ExampleValue.b
import org.http4k.core.AcceptableParsingTest.ExampleValue.c
import org.http4k.core.Method.GET
import org.http4k.lens.Header
import org.http4k.lens.LensFailure
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.fail

class AcceptableParsingTest {
    enum class ExampleValue {
        a, b, c
    }
    
    private val valueParser: (String, Map<String, String>) -> ExampleValue =
        { s, _ -> ExampleValue.valueOf(s) }
    
    @Test
    fun `parse single value`() {
        val parsed = PriorityList.fromHeader("a", valueParser)
        assertEquals(PriorityList(listOf(Weighted(a, 1.0))), parsed)
    }
    
    @Test
    fun `parse multiple values`() {
        val parsed = PriorityList.fromHeader("a,b,c", valueParser)
        assertEquals(
            PriorityList(
                listOf(
                    Weighted(a, 1.0),
                    Weighted(b, 1.0),
                    Weighted(c, 1.0),
                )
            ),
            parsed
        )
    }
    
    @Test
    fun `parse single qualified value`() {
        val parsed = PriorityList.fromHeader("a;q=0.625", valueParser)
        assertEquals(PriorityList(listOf(Weighted(a, 0.625))), parsed)
    }
    
    @Test
    fun `parse mixed qualified and unqualified values`() {
        val parsed = PriorityList.fromHeader("a,b;q=0.625,c;q=0.25", valueParser)
        assertEquals(PriorityList(listOf(Weighted(a, 1.0), Weighted(b, 0.625), Weighted(c, 0.25))), parsed)
    }
    
    @Test
    fun `returns in preference order`() {
        val parsed = PriorityList.fromHeader("a;q=0.25,b,c;q=0.625", valueParser)
        assertEquals(PriorityList(listOf(Weighted(b, 1.0), Weighted(c, 0.625), Weighted(a, 0.25))), parsed)
    }
    
    @Test
    fun `allows optional whitespace`() {
        val parsed = PriorityList.fromHeader("a ; q=0.25 , b , c ; q=0.625", valueParser)
        assertEquals(PriorityList(listOf(Weighted(b, 1.0), Weighted(c, 0.625), Weighted(a, 0.25))), parsed)
    }
    
    @Test
    fun `lens converts exception to LensFailure when parsing fails`() {
        class ExampleException(msg: String) : Exception(msg)
        
        val lens = Header
            .map<PriorityList<Int>>(
                nextIn = { PriorityList.fromHeader(it, { _, _ -> throw ExampleException("test failure") }) },
                nextOut = { it.toHeader { _ -> fail("should not be called") } }
            )
            .optional("Example-Header")
        
        val request = Request(GET, "/example").header("Example-Header", "x")
        
        assertThrows<LensFailure> {
            lens(request)
        }
    }
}

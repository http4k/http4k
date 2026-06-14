package org.http4k.webdriver.datastar

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.junit.jupiter.api.Test

class DatastarExpressionTest {

    private val store = SignalStore()
    private val actions = mutableListOf<Action>()

    private fun eval(expression: String): Any? = DatastarExpression.parse(expression).evaluate(store, actions::add)

    @Test
    fun literals() {
        assertThat(eval("1"), equalTo(1.0))
        assertThat(eval("1.5"), equalTo(1.5))
        assertThat(eval("'hello'"), equalTo("hello"))
        assertThat(eval("\"world\""), equalTo("world"))
        assertThat(eval("true"), equalTo(true))
        assertThat(eval("false"), equalTo(false))
        assertThat(eval("null"), absent())
        assertThat(eval("'it\\'s'"), equalTo("it's"))
    }

    @Test
    fun `signal read and write`() {
        eval($$"$count = 5")
        assertThat(store["count"], equalTo(5.0))
        assertThat(eval($$"$count"), equalTo(5.0))

        eval($$"$user.name = 'bob'")
        assertThat(store["user.name"], equalTo("bob"))
    }

    @Test
    fun `unset signal reads as null`() {
        assertThat(eval($$"$missing"), absent())
    }

    @Test
    fun `arithmetic with precedence`() {
        assertThat(eval("1 + 2 * 3"), equalTo(7.0))
        assertThat(eval("(1 + 2) * 3"), equalTo(9.0))
        assertThat(eval("10 % 3"), equalTo(1.0))
        assertThat(eval("10 - 2 - 3"), equalTo(5.0))
        assertThat(eval("-5 + 1"), equalTo(-4.0))
    }

    @Test
    fun `string concatenation`() {
        assertThat(eval("'a' + 'b'"), equalTo("ab"))
        eval($$"$count = 2")
        assertThat(eval($$"'total: ' + $count"), equalTo("total: 2"))
    }

    @Test
    fun `comparisons and equality`() {
        assertThat(eval("1 < 2"), equalTo(true))
        assertThat(eval("2 <= 2"), equalTo(true))
        assertThat(eval("3 > 4"), equalTo(false))
        assertThat(eval("1 == 1"), equalTo(true))
        assertThat(eval("1 == '1'"), equalTo(true))
        assertThat(eval("1 === '1'"), equalTo(false))
        assertThat(eval("'a' != 'b'"), equalTo(true))
    }

    @Test
    fun `boolean logic short circuits and returns the deciding operand`() {
        assertThat(eval("true && false"), equalTo(false))
        assertThat(eval("true || false"), equalTo(true))
        assertThat(eval("'a' && 'b'"), equalTo("b"))
        assertThat(eval("null || 'fallback'"), equalTo("fallback"))
        assertThat(eval($$"!$open"), equalTo(true))
    }

    @Test
    fun ternary() {
        eval($$"$count = 5")
        assertThat(eval($$"$count > 3 ? 'big' : 'small'"), equalTo("big"))
        assertThat(eval($$"$count > 10 ? 'big' : 'small'"), equalTo("small"))
    }

    @Test
    fun `toggle idiom`() {
        eval($$"$open = false")
        eval($$"$open = !$open")
        assertThat(store["open"], equalTo(true))
    }

    @Test
    fun `increment and decrement`() {
        eval($$"$count = 1")
        assertThat(eval($$"$count++"), equalTo(1.0))
        assertThat(store["count"], equalTo(2.0))
        assertThat(eval($$"++$count"), equalTo(3.0))
        assertThat(eval($$"$count--"), equalTo(3.0))
        assertThat(store["count"], equalTo(2.0))
    }

    @Test
    fun `compound assignment`() {
        eval($$"$count = 10")
        eval($$"$count += 5")
        assertThat(store["count"], equalTo(15.0))
        eval($$"$count -= 3")
        assertThat(store["count"], equalTo(12.0))
        eval($$"$count *= 2")
        assertThat(store["count"], equalTo(24.0))
        eval($$"$count /= 4")
        assertThat(store["count"], equalTo(6.0))
    }

    @Test
    fun `multiple statements run in order`() {
        eval($$"$a = 1; $b = $a + 1; $c = $b * 2;")
        assertThat(store["c"], equalTo(4.0))
    }

    @Test
    fun `object and array literals`() {
        assertThat(eval("{foo: 1, 'bar': 'baz'}"), equalTo<Any?>(mapOf("foo" to 1.0, "bar" to "baz")))
        assertThat(eval("{nested: {x: true}}"), equalTo<Any?>(mapOf("nested" to mapOf("x" to true))))
        assertThat(eval("[1, 'two', false]"), equalTo<Any?>(listOf(1.0, "two", false)))
    }

    @Test
    fun `action calls are dispatched`() {
        eval("@get('/foo')")
        assertThat(actions, equalTo(listOf(Action(GET, "/foo"))))
    }

    @Test
    fun `action url can be an expression`() {
        eval($$"$id = 42")
        eval($$"@post('/items/' + $id)")
        assertThat(actions, equalTo(listOf(Action(POST, "/items/42"))))
    }

    @Test
    fun `action call combined with signal updates`() {
        eval($$"$saving = true; @post('/save')")
        assertThat(store["saving"], equalTo(true))
        assertThat(actions, equalTo(listOf(Action(POST, "/save"))))
    }

    @Test
    fun `only the chosen ternary branch action is dispatched`() {
        eval($$"$open = true")
        eval($$"$open ? @post('/close') : @post('/open')")
        assertThat(actions, equalTo(listOf(Action(POST, "/close"))))
    }

    @Test
    fun `string literals may contain statement and expression delimiters`() {
        eval($$"$a = 'one;two'; $b = 'a ? b : c'")
        assertThat(store["a"], equalTo("one;two"))
        assertThat(store["b"], equalTo("a ? b : c"))
    }

    @Test
    fun `nested signal paths support increment`() {
        eval($$"$basket.count = 1")
        eval($$"$basket.count++")
        assertThat(store["basket.count"], equalTo(2.0))
    }

    @Test
    fun `local signals use underscore prefix`() {
        eval($$"$_local = 1")
        assertThat(store["_local"], equalTo(1.0))
    }

    @Test
    fun `invalid expressions parse to null`() {
        assertThat(DatastarExpression.parseOrNull("if (x) {"), absent())
        assertThat(DatastarExpression.parseOrNull(""), absent())
        assertThat(DatastarExpression.parseOrNull("function() {}"), absent())
    }
}

package org.http4k.webdriver.datastar

import org.http4k.webdriver.datastar.DatastarExpr.ActionCall
import org.http4k.webdriver.datastar.DatastarExpr.ArrayLiteral
import org.http4k.webdriver.datastar.DatastarExpr.Assignment
import org.http4k.webdriver.datastar.DatastarExpr.Binary
import org.http4k.webdriver.datastar.DatastarExpr.EventKey
import org.http4k.webdriver.datastar.DatastarExpr.EventTarget
import org.http4k.webdriver.datastar.DatastarExpr.IncDec
import org.http4k.webdriver.datastar.DatastarExpr.Literal
import org.http4k.webdriver.datastar.DatastarExpr.ObjectLiteral
import org.http4k.webdriver.datastar.DatastarExpr.SignalRef
import org.http4k.webdriver.datastar.DatastarExpr.Statements
import org.http4k.webdriver.datastar.DatastarExpr.Ternary
import org.http4k.webdriver.datastar.DatastarExpr.Unary
import org.jsoup.nodes.Element

/**
 * Evaluates datastar expressions against a SignalStore, with JS-flavoured semantics:
 * truthiness, loose equality, string concatenation with +, and short-circuiting && / ||
 * which return the deciding operand. @action calls are sent to the dispatcher.
 */
internal data class DomEvent(val target: Element, val key: String? = null)

internal fun DatastarExpr.evaluate(store: SignalStore, event: DomEvent? = null, dispatch: (Action) -> Unit = {}): Any? = when (this) {
    is Literal -> value

    is ObjectLiteral -> entries.associateTo(linkedMapOf()) { (key, value) -> key to value.evaluate(store, event, dispatch) }

    is ArrayLiteral -> items.map { it.evaluate(store, event, dispatch) }

    is SignalRef -> store[path]

    is EventKey -> eventKey(event)

    is EventTarget -> eventTarget(event)

    is Assignment -> assign(store, value.evaluate(store, event, dispatch))

    is IncDec -> incDec(store)

    is Unary -> unary(operand.evaluate(store, event, dispatch))

    is Binary -> binary { it.evaluate(store, event, dispatch) }

    is Ternary -> branch(condition.evaluate(store, event, dispatch)).evaluate(store, event, dispatch)

    is ActionCall -> {
        dispatch(Action(method, stringify(url.evaluate(store, event, dispatch))))
        null
    }

    is Statements -> statements.fold(null as Any?) { _, statement -> statement.evaluate(store, event, dispatch) }
}

private fun eventKey(event: DomEvent?) =
    event?.key ?: throw UnsupportedDatastarExpression("an attribute without a key event", "evt.key")

private fun EventTarget.eventTarget(event: DomEvent?): Any? {
    val element = event?.target ?: throw UnsupportedDatastarExpression("an attribute without an event", "evt.target.$property")
    return when (property) {
        "checked" -> element.hasAttr("checked")
        else -> element.domValue()
    }
}

private fun Assignment.assign(store: SignalStore, rhs: Any?): Any? {
    val result = when (op) {
        "=" -> rhs
        "+=" -> plus(store[path], rhs)
        "-=" -> asNumber(store[path]) - asNumber(rhs)
        "*=" -> asNumber(store[path]) * asNumber(rhs)
        "/=" -> asNumber(store[path]) / asNumber(rhs)
        else -> error("unsupported assignment operator $op")
    }
    store[path] = result
    return result
}

private fun IncDec.incDec(store: SignalStore): Double {
    val old = asNumber(store[path])
    store[path] = old + delta
    return if (postfix) old else old + delta
}

private fun Ternary.branch(condition: Any?) = if (truthy(condition)) ifTrue else ifFalse

private fun Unary.unary(value: Any?): Any = when (op) {
    "!" -> !truthy(value)
    "-" -> -asNumber(value)
    else -> error("unsupported unary operator $op")
}

private fun Binary.binary(eval: (DatastarExpr) -> Any?): Any? = when (op) {
    "&&" -> eval(left).let { if (!truthy(it)) it else eval(right) }
    "||" -> eval(left).let { if (truthy(it)) it else eval(right) }
    else -> applyOperator(op, eval(left), eval(right))
}

private fun applyOperator(op: String, l: Any?, r: Any?): Any? = when (op) {
    "+" -> plus(l, r)
    "-" -> asNumber(l) - asNumber(r)
    "*" -> asNumber(l) * asNumber(r)
    "/" -> asNumber(l) / asNumber(r)
    "%" -> asNumber(l) % asNumber(r)
    else -> compare(op, l, r)
}

private fun compare(op: String, l: Any?, r: Any?): Boolean = when (op) {
    "==" -> looseEquals(l, r)
    "!=" -> !looseEquals(l, r)
    "===" -> l == r
    "!==" -> l != r
    "<" -> compareValues(l, r) < 0
    ">" -> compareValues(l, r) > 0
    "<=" -> compareValues(l, r) <= 0
    ">=" -> compareValues(l, r) >= 0
    else -> error("unsupported operator $op")
}

internal fun truthy(value: Any?): Boolean = when (value) {
    null -> false
    is Boolean -> value
    is Number -> value.toDouble() != 0.0 && !value.toDouble().isNaN()
    is String -> value.isNotEmpty()
    else -> true
}

internal fun stringify(value: Any?): String = when (value) {
    null -> ""
    is String -> value
    is Number -> renderNumber(value.toDouble())
    is Boolean -> value.toString()
    else -> renderJson(value)
}

private fun asNumber(value: Any?): Double = when (value) {
    null -> 0.0
    is Number -> value.toDouble()
    is Boolean -> if (value) 1.0 else 0.0
    is String -> value.toDoubleOrNull() ?: Double.NaN
    else -> Double.NaN
}

private fun plus(left: Any?, right: Any?): Any? =
    if (left is String || right is String) {
        stringify(left) + stringify(right)
    } else {
        asNumber(left) + asNumber(right)
    }

private fun looseEquals(left: Any?, right: Any?): Boolean = when {
    left is Number && right is Number -> left.toDouble() == right.toDouble()
    left is Number || right is Number -> asNumber(left) == asNumber(right)
    else -> left == right
}

private fun compareValues(left: Any?, right: Any?): Int =
    if (left is String && right is String) {
        left.compareTo(right)
    } else {
        asNumber(left).compareTo(asNumber(right))
    }

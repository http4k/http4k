package org.http4k.webdriver.datastar

import org.http4k.webdriver.datastar.DatastarExpr.ActionCall
import org.http4k.webdriver.datastar.DatastarExpr.ArrayLiteral
import org.http4k.webdriver.datastar.DatastarExpr.Assignment
import org.http4k.webdriver.datastar.DatastarExpr.Binary
import org.http4k.webdriver.datastar.DatastarExpr.IncDec
import org.http4k.webdriver.datastar.DatastarExpr.Literal
import org.http4k.webdriver.datastar.DatastarExpr.ObjectLiteral
import org.http4k.webdriver.datastar.DatastarExpr.SignalRef
import org.http4k.webdriver.datastar.DatastarExpr.Statements
import org.http4k.webdriver.datastar.DatastarExpr.Ternary
import org.http4k.webdriver.datastar.DatastarExpr.Unary

/**
 * Evaluates datastar expressions against a SignalStore, with JS-flavoured semantics:
 * truthiness, loose equality, string concatenation with +, and short-circuiting && / ||
 * which return the deciding operand. @action calls are sent to the dispatcher.
 */
internal fun DatastarExpr.evaluate(store: SignalStore, dispatch: (Action) -> Unit = {}): Any? = when (this) {
    is Literal -> value
    is ObjectLiteral -> entries.associateTo(linkedMapOf()) { (key, value) -> key to value.evaluate(store, dispatch) }
    is ArrayLiteral -> items.map { it.evaluate(store, dispatch) }
    is SignalRef -> store[path]
    is Assignment -> {
        val rhs = value.evaluate(store, dispatch)
        val result = when (op) {
            "=" -> rhs
            "+=" -> plus(store[path], rhs)
            "-=" -> asNumber(store[path]) - asNumber(rhs)
            "*=" -> asNumber(store[path]) * asNumber(rhs)
            "/=" -> asNumber(store[path]) / asNumber(rhs)
            else -> error("unsupported assignment operator $op")
        }
        store[path] = result
        result
    }

    is IncDec -> {
        val old = asNumber(store[path])
        store[path] = old + delta
        if (postfix) old else old + delta
    }

    is Unary -> when (op) {
        "!" -> !truthy(operand.evaluate(store, dispatch))
        "-" -> -asNumber(operand.evaluate(store, dispatch))
        else -> error("unsupported unary operator $op")
    }

    is Binary -> {
        when (op) {
            "&&" -> left.evaluate(store, dispatch).let { if (!truthy(it)) it else right.evaluate(store, dispatch) }
            "||" -> left.evaluate(store, dispatch).let { if (truthy(it)) it else right.evaluate(store, dispatch) }
            else -> {
                val l = left.evaluate(store, dispatch)
                val r = right.evaluate(store, dispatch)
                when (op) {
                    "+" -> plus(l, r)
                    "-" -> asNumber(l) - asNumber(r)
                    "*" -> asNumber(l) * asNumber(r)
                    "/" -> asNumber(l) / asNumber(r)
                    "%" -> asNumber(l) % asNumber(r)
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
            }
        }
    }

    is Ternary ->
        if (truthy(condition.evaluate(store, dispatch))) ifTrue.evaluate(store, dispatch)
        else ifFalse.evaluate(store, dispatch)

    is ActionCall -> {
        dispatch(Action(method, stringify(url.evaluate(store, dispatch))))
        null
    }

    is Statements -> statements.fold(null as Any?) { _, statement -> statement.evaluate(store, dispatch) }
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
    if (left is String || right is String) stringify(left) + stringify(right)
    else asNumber(left) + asNumber(right)

private fun looseEquals(left: Any?, right: Any?): Boolean = when {
    left is Number && right is Number -> left.toDouble() == right.toDouble()
    left is Number || right is Number -> asNumber(left) == asNumber(right)
    else -> left == right
}

private fun compareValues(left: Any?, right: Any?): Int =
    if (left is String && right is String) left.compareTo(right)
    else asNumber(left).compareTo(asNumber(right))

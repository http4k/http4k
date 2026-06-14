package org.http4k.webdriver.datastar

import org.http4k.core.Method

/**
 * AST for the pragmatic subset of the datastar expression language: signal reads/writes,
 * literals, boolean logic, comparisons, arithmetic, ternaries, increments and @action calls.
 */
internal sealed interface DatastarExpr {
    data class Literal(val value: Any?) : DatastarExpr
    data class ObjectLiteral(val entries: List<Pair<String, DatastarExpr>>) : DatastarExpr
    data class ArrayLiteral(val items: List<DatastarExpr>) : DatastarExpr
    data class SignalRef(val path: String) : DatastarExpr
    data class Assignment(val path: String, val op: String, val value: DatastarExpr) : DatastarExpr
    data class IncDec(val path: String, val delta: Double, val postfix: Boolean) : DatastarExpr
    data class Unary(val op: String, val operand: DatastarExpr) : DatastarExpr
    data class Binary(val op: String, val left: DatastarExpr, val right: DatastarExpr) : DatastarExpr
    data class Ternary(val condition: DatastarExpr, val ifTrue: DatastarExpr, val ifFalse: DatastarExpr) : DatastarExpr
    data class ActionCall(val method: Method, val url: DatastarExpr) : DatastarExpr
    data class Statements(val statements: List<DatastarExpr>) : DatastarExpr
}

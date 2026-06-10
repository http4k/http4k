package org.http4k.storyboard.datastar

import org.http4k.core.Method

/**
 * AST for the pragmatic subset of the datastar expression language: signal reads/writes,
 * literals, boolean logic, comparisons, arithmetic, ternaries, increments and @action calls.
 */
internal sealed interface Expr {
    data class Literal(val value: Any?) : Expr
    data class ObjectLiteral(val entries: List<Pair<String, Expr>>) : Expr
    data class ArrayLiteral(val items: List<Expr>) : Expr
    data class SignalRef(val path: String) : Expr
    data class Assignment(val path: String, val op: String, val value: Expr) : Expr
    data class IncDec(val path: String, val delta: Double, val postfix: Boolean) : Expr
    data class Unary(val op: String, val operand: Expr) : Expr
    data class Binary(val op: String, val left: Expr, val right: Expr) : Expr
    data class Ternary(val condition: Expr, val ifTrue: Expr, val ifFalse: Expr) : Expr
    data class ActionCall(val method: Method, val url: Expr) : Expr
    data class Statements(val statements: List<Expr>) : Expr
}

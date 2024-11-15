package org.http4k.connect.amazon.dynamodb.grammar

import org.http4k.connect.amazon.dynamodb.model.AttributeValue
import parser4k.Parser
import parser4k.asBinary
import parser4k.commonparsers.token
import parser4k.inOrder
import parser4k.map
import parser4k.mapLeftAssoc

fun interface Expr {
    fun eval(item: ItemWithSubstitutions): Any
}

internal const val NULLMARKER = "__*NULL*__"

internal fun Any.asString(): Any =
    with(this as AttributeValue) {
        when {
            B != null -> B!!.value
            BOOL != null -> BOOL!!.toString()
            BS != null -> BS!!.map { it.value }
            L != null -> L!!.map(Any::asString)
            M != null -> M!!.map { it.value.asString() }
            N != null -> N!!
            NS != null -> NS!!
            S != null -> S!!
            SS != null -> SS!!
            else -> NULLMARKER
        }
    }

fun binaryExpr(parser: Parser<Expr>, tokenString: String, f: (Expr, Expr) -> Expr) =
    inOrder(parser, token(tokenString), parser).mapLeftAssoc(f.asBinary())

fun unaryExpr(parser: Parser<Expr>, tokenString: String, f: (Expr) -> Expr) =
    inOrder(token(tokenString), parser).map { (_, it) -> f(it) }

internal fun ItemWithSubstitutions.comparable(expr: Expr) = expr.eval(this).asString().toString().padStart(200)

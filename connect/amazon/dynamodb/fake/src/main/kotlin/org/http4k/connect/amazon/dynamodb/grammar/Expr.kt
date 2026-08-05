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

    /**
     * Check the expression against the request's substitutions, before any of it is evaluated.
     *
     * DynamoDB rejects an expression naming an undefined or malformed expression attribute value as a
     * *request* validation error, whatever the item holds - so the check cannot ride on [eval], where
     * `AND`/`OR` short-circuiting decides whether a branch is reached at all. The default is a no-op; the
     * nodes which resolve a substitution, and the combinators which have to pass the walk on to their
     * sub-expressions, override it.
     */
    fun validate(item: ItemWithSubstitutions) {}
}

internal fun expr(vararg children: Expr, eval: (ItemWithSubstitutions) -> Any): Expr = object : Expr {
    override fun eval(item: ItemWithSubstitutions) = eval(item)

    override fun validate(item: ItemWithSubstitutions) = item.validateAll(*children)
}

/** Passes the [Expr.validate] walk on to the sub-expressions of a combinator. */
internal fun ItemWithSubstitutions.validateAll(vararg exprs: Expr) = exprs.forEach { it.validate(this) }

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

internal fun ItemWithSubstitutions.comparable(expr: Expr) = expr.eval(this) as AttributeValue

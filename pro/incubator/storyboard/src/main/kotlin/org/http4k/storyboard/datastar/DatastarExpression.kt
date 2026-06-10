package org.http4k.storyboard.datastar

import org.http4k.core.Method
import parser4k.OutputCache
import parser4k.Parser
import parser4k.anyCharExcept
import parser4k.asBinary
import parser4k.char
import parser4k.commonparsers.Tokens
import parser4k.commonparsers.joinedWith
import parser4k.commonparsers.token
import parser4k.inOrder
import parser4k.map
import parser4k.mapLeftAssoc
import parser4k.nestedPrecedence
import parser4k.oneOf
import parser4k.oneOfWithPrecedence
import parser4k.optional
import parser4k.parseWith
import parser4k.ref
import parser4k.reset
import parser4k.skipFirst
import parser4k.skipWrapper
import parser4k.str
import parser4k.with
import parser4k.zeroOrMore

/**
 * parser4k grammar for the pragmatic subset of datastar expressions used in data-* attributes.
 */
internal object DatastarExpression {

    fun parse(expression: String): Expr = expression.parseWith(program)

    fun parseOrNull(expression: String): Expr? = runCatching { parse(expression) }.getOrNull()

    private val cache = OutputCache<Expr>()

    private fun <T> Parser<T>.asToken(): Parser<T> =
        inOrder(zeroOrMore(Tokens.whitespace), this, zeroOrMore(Tokens.whitespace)).skipWrapper()

    private val identifier: Parser<String> =
        inOrder(
            oneOf(oneOf('a'..'z'), oneOf('A'..'Z'), char('_')),
            zeroOrMore(oneOf(oneOf('a'..'z'), oneOf('A'..'Z'), oneOf('0'..'9'), char('_')))
        ).map { (first, rest) -> first + rest.joinToString("") }

    private val signalPath: Parser<String> =
        inOrder(char('$'), identifier.joinedWith(char('.'))).skipFirst().map { it.joinToString(".") }

    private val escapedChar: Parser<String> = inOrder(char('\\'), anyCharExcept()).skipFirst().map {
        when (it) {
            'n' -> "\n"
            'r' -> "\r"
            't' -> "\t"
            else -> it.toString()
        }
    }

    private fun quotedString(quote: Char): Parser<String> =
        inOrder(char(quote), zeroOrMore(oneOf(escapedChar, anyCharExcept(quote, '\\').map(Char::toString))), char(quote))
            .skipWrapper().map { it.joinToString("") }

    private val stringLiteral: Parser<String> = oneOf(quotedString('\''), quotedString('"'))

    private val literal: Parser<Expr> = oneOf(
        Tokens.number.map { Expr.Literal(it.toDouble()) },
        stringLiteral.map { Expr.Literal(it) },
        str("true").map { Expr.Literal(true) },
        str("false").map { Expr.Literal(false) },
        str("null").map { Expr.Literal(null) },
    ).asToken()

    private val incDecOp: Parser<String> = oneOf(str("++"), str("--"))

    private val postfixIncDec: Parser<Expr> = inOrder(signalPath, incDecOp)
        .map { (path, op) -> Expr.IncDec(path, if (op == "++") 1.0 else -1.0, postfix = true) }.asToken()

    private val prefixIncDec: Parser<Expr> = inOrder(incDecOp, signalPath)
        .map { (op, path) -> Expr.IncDec(path, if (op == "++") 1.0 else -1.0, postfix = false) }.asToken()

    private val signalRef: Parser<Expr> = signalPath.map { Expr.SignalRef(it) }.asToken()

    private val actionCall: Parser<Expr> = inOrder(
        oneOf("@get", "@post", "@put", "@patch", "@delete").asToken(),
        token("("),
        ref { expr },
        optional(inOrder(token(","), ref { expr })),
        token(")")
    ).map { (method, _, url, _, _) -> Expr.ActionCall(Method.valueOf(method.drop(1).uppercase()), url) }

    private val objectKey: Parser<String> = oneOf(identifier, stringLiteral).asToken()

    private val objectLiteral: Parser<Expr> = inOrder(
        token("{"),
        optional(inOrder(objectKey, token(":"), ref { expr }).map { (key, _, value) -> key to value }.joinedWith(token(","))),
        token("}")
    ).skipWrapper().map { Expr.ObjectLiteral(it ?: emptyList()) }

    private val arrayLiteral: Parser<Expr> = inOrder(
        token("["),
        optional(ref { expr }.joinedWith(token(","))),
        token("]")
    ).skipWrapper().map { Expr.ArrayLiteral(it ?: emptyList()) }

    private fun binary(op: String): Parser<Expr> =
        inOrder(ref { expr }, token(op), ref { expr })
            .mapLeftAssoc({ left: Expr, right: Expr -> Expr.Binary(op, left, right) }.asBinary())
            .with(cache)

    private fun unary(op: String): Parser<Expr> =
        inOrder(token(op), ref { expr }).skipFirst().map { Expr.Unary(op, it) }.with(cache)

    private val ternary: Parser<Expr> =
        inOrder(ref { expr }, token("?"), ref { expr }, token(":"), ref { expr })
            .map { (condition, _, ifTrue, _, ifFalse) -> Expr.Ternary(condition, ifTrue, ifFalse) }
            .with(cache)

    private val paren: Parser<Expr> = inOrder(token("("), ref { expr }, token(")")).skipWrapper().with(cache)

    private val expr: Parser<Expr> = oneOfWithPrecedence(
        ternary,
        binary("||"),
        binary("&&"),
        oneOf(binary("==="), binary("=="), binary("!=="), binary("!=")),
        oneOf(binary("<="), binary(">="), binary("<"), binary(">")),
        oneOf(binary("+"), binary("-")),
        oneOf(binary("*"), binary("/"), binary("%")),
        oneOf(unary("!"), unary("-")),
        paren.nestedPrecedence(),
        actionCall.with(cache).nestedPrecedence(),
        objectLiteral.with(cache).nestedPrecedence(),
        arrayLiteral.with(cache).nestedPrecedence(),
        oneOf(
            literal.with(cache),
            postfixIncDec.with(cache),
            prefixIncDec.with(cache),
            signalRef.with(cache)
        )
    ).reset(cache)

    private val assignOp: Parser<String> =
        oneOf(str("+="), str("-="), str("*="), str("/="), str("=")).asToken()

    private val assignment: Parser<Expr> = inOrder(signalPath.asToken(), assignOp, ref { statement })
        .map { (path, op, value) -> Expr.Assignment(path, op, value) }

    private val statement: Parser<Expr> = oneOf(assignment, expr)

    private val program: Parser<Expr> =
        inOrder(statement, zeroOrMore(inOrder(token(";"), statement).skipFirst()), optional(token(";")))
            .map { (first, rest, _) -> if (rest.isEmpty()) first else Expr.Statements(listOf(first) + rest) }
}

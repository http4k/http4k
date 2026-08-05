package org.http4k.connect.amazon.dynamodb.grammar

import org.http4k.connect.amazon.dynamodb.model.AttributeValue
import org.http4k.connect.amazon.dynamodb.model.DynamoDataType
import parser4k.Parser
import parser4k.commonparsers.Tokens
import parser4k.commonparsers.token
import parser4k.inOrder
import parser4k.map
import parser4k.oneOf
import parser4k.ref
import parser4k.skipFirst
import parser4k.skipWrapper

/**
 * `attribute_type(path, type)`.
 *
 * Real DynamoDB accepts the type operand only as an expression attribute value - `attribute_type(#a, :t)`
 * with `:t` bound to a string holding the type name - so that form is parsed here and resolved through the
 * request's `ExpressionAttributeValues`. The bare-name form (`attribute_type(a, S)`), which this fake has
 * always accepted and real DynamoDB rejects, still parses so existing expressions keep working.
 *
 * An unresolvable or non-type operand is reported as a [DynamoDbConditionError], which the endpoints map
 * onto the `ValidationException` the real service answers with. DynamoDB treats that as a *request* error
 * rather than a data-dependent result, so the operand is resolved in [Expr.validate] as well as in `eval`:
 * without it `attribute_not_exists(#a) OR attribute_type(#a, :missing)` would quietly succeed whenever the
 * left operand happened to be true, and the bad token would never be reported.
 */
object AttributeType : ExprFactory {
    override operator fun invoke(parser: () -> Parser<Expr>): Parser<Expr> =
        inOrder(
            token("attribute_type"),
            token("("),
            ref(parser),
            token(","),
            typeOperand,
            token(")")
        ).skipWrapper()
            .map { (_, attr, _, dynamoType) ->
                object : Expr {
                    override fun eval(item: ItemWithSubstitutions): Any {
                        val value = attr.eval(item)
                        return AttributeValue::class.java.methods
                            .find { it.name == "get" + dynamoType(item).name }
                            ?.invoke(value) != null
                    }

                    override fun validate(item: ItemWithSubstitutions) {
                        dynamoType(item)
                        item.validateAll(attr)
                    }
                }
            }
}

/**
 * The type operand of `attribute_type`, resolved against the item's substitutions: either a `:token`
 * naming a string [AttributeValue] in the `ExpressionAttributeValues`, or a bare type name.
 */
private val typeOperand: Parser<(ItemWithSubstitutions) -> DynamoDataType> = oneOf(
    inOrder(oneOf(':'), Tokens.identifier).skipFirst().map { valueToken ->
        { item: ItemWithSubstitutions ->
            val value = item.values[":$valueToken"]
                ?: throw DynamoDbConditionError(
                    "An expression attribute value used in expression is not defined; attribute value: :$valueToken"
                )
            (value.S ?: throw invalidTypeName(value)).asDynamoDataType()
        }
    },
    Tokens.identifier.map { name -> { _: ItemWithSubstitutions -> name.asDynamoDataType() } }
)

private fun String.asDynamoDataType() = DynamoDataType.entries.find { it.name == this } ?: throw invalidTypeName(this)

private fun invalidTypeName(type: Any) = DynamoDbConditionError(
    "Invalid attribute type name found; type: $type, valid types: {${DynamoDataType.entries.joinToString(",")}}"
)

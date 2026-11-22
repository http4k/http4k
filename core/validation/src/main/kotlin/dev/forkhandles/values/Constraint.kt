package dev.forkhandles.values

@JvmInline
value class Constraint(override val value: String) : Value<String> {
    companion object {
        val NotNull = Constraint("NotNull")
        val Null = Constraint("Null")
        val NotBlank = Constraint("NotBlank")
        val NotEmpty = Constraint("NotEmpty")
        val Size = Constraint("Size")
        val Email = Constraint("Email")
        val Pattern = Constraint("Pattern")
        val Min = Constraint("Min")
        val Max = Constraint("Max")
        val DecimalMin = Constraint("DecimalMin")
        val DecimalMax = Constraint("DecimalMax")
        val Positive = Constraint("Positive")
        val PositiveOrZero = Constraint("PositiveOrZero")
        val Negative = Constraint("Negative")
        val NegativeOrZero = Constraint("NegativeOrZero")
        val Digits = Constraint("Digits")
        val AssertTrue = Constraint("AssertTrue")
        val AssertFalse = Constraint("AssertFalse")
        val Past = Constraint("Past")
        val PastOrPresent = Constraint("PastOrPresent")
        val Future = Constraint("Future")
        val FutureOrPresent = Constraint("FutureOrPresent")
        val Missing = Constraint("Missing")
        val Invalid = Constraint("Invalid")
        val Unsupported = Constraint("Unsupported")
        val Problems = Constraint("Problems")
    }
}

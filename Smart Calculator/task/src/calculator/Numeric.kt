package calculator

import java.math.BigInteger

class Numeric(rawValue: String) : Element(rawValue) {
    constructor(rawValue: BigInteger) : this(rawValue.toString())

    override fun toString(): String {
        return "$rawValue"
    }

    fun numericValue(): BigInteger {
        return rawValue.toBigInteger()
    }

}
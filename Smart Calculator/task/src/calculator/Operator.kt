package calculator

import kotlin.math.pow

class Operator(rawValue: String) : Element(rawValue) {
    private val regexAllMinus = Regex("-+")
    private val regexAllPlus = Regex("\\++")

    override fun toString(): String {
        return "(Operator) $rawValue"
    }

    public fun getMathFunction(): (Int, Int) -> Int {
        when (rawValue[0]) {
            '+' -> return getActualOperator()
            '-' -> return getActualOperator()
            '*' -> return Math::multiplyExact
            '/' -> return Math::floorDiv
            '^' -> return {x, y -> x.toDouble().pow(y.toDouble()).toInt()}
            else -> throw Exception("Invalid Operator: $rawValue")
        }
    }

    private fun getActualOperator(): (x: Int, y: Int) -> Int {
        val isAllMinus = rawValue.matches(regexAllMinus)
        val isAllPlus = rawValue.matches(regexAllPlus)

        return when {
            isAllPlus -> Math::addExact
            isAllMinus && rawValue.length % 2 == 1 -> Math::subtractExact
            isAllMinus && rawValue.length % 2 == 0 -> Math::addExact
            else -> throw Exception("Invalid Operator: $rawValue")
        }
    }

}